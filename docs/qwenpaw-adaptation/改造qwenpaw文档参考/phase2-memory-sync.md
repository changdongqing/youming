# 阶段 2 详细设计：记忆同步

> **配套文档**：[architecture.md](./architecture.md) · [implementation-plan.md](./implementation-plan.md) · [phase0-infrastructure.md](./phase0-infrastructure.md) · [phase1-auth-oauth2.md](./phase1-auth-oauth2.md)
>
> **目标**：登录时从服务端拉取记忆增量到本地 SQLite，工作中后台异步上行新增记忆，断网可正常工作，联网后自动补传。
>
> **预估工期**：5-7 天 · **依赖**：阶段 0 + 阶段 1

---

## 目录

- [1. 整体架构](#1-整体架构)
- [2. 数据模型](#2-数据模型)
- [3. HistoryStore 改造](#3-historystore-改造)
- [4. MemorySyncManager 设计](#4-memorysyncmanager-设计)
- [5. 下行拉取（登录时）](#5-下行拉取登录时)
- [6. 上行推送（工作中）](#6-上行推送工作中)
- [7. 对账机制](#7-对账机制)
- [8. HistoryStore 实例化注入](#8-historystore-实例化注入)
- [9. pig 服务端接口约定](#9-pig-服务端接口约定)
- [10. 验收标准](#10-验收标准)

---

## 1. 整体架构

### 1.1 数据流

```
┌──────────────────────────────────────────────────────────────┐
│                        QwenPaw 本地                           │
│                                                              │
│   Agent ──► HistoryStore.append() ──► SQLite (本地母版)       │
│                 │                              │             │
│                 │ sync_hook (非阻塞)            │             │
│                 ▼                              │             │
│          ┌──────────────┐                      │             │
│          │ asyncio.Queue│                      │             │
│          │ (上行缓冲)    │                      │             │
│          └──────┬───────┘                      │             │
│                 │                               │             │
│          ┌──────▼───────┐               ┌──────▼──────┐      │
│          │ UplinkWorker │               │ PullManager │      │
│          │ (批量上行)    │               │ (登录拉取)   │      │
│          └──────┬───────┘               └──────┬──────┘      │
│                 │                              │             │
└─────────────────┼──────────────────────────────┼─────────────┘
                  │ POST /api/sync/memory         │ GET /api/sync/memory
                  │ (上行)                        │ ?since={seq}
                  ▼                               ▼
            ┌─────────────────────────────────────────┐
            │              pig 服务端                   │
            │   conversation_history (PostgreSQL)      │
            │   UNIQUE(session_id, dedup_key) 幂等     │
            └─────────────────────────────────────────┘
```

### 1.2 核心设计原则

| 原则 | 说明 |
|------|------|
| **本地优先** | `HistoryStore.append()` 写入 SQLite 是唯一阻塞操作，sync_hook 非阻塞 |
| **幂等上行** | 利用现有 `dedup_key`，服务端 `ON CONFLICT DO NOTHING` |
| **断网容错** | 上行队列堆积在内存，联网后批量补传 |
| **增量游标** | `sync_meta.last_sync_seq` 记录已上行到的 seq |
| **登录对账** | 每次登录全量对账，修复上次断网丢失的上行 |

---

## 2. 数据模型

### 2.1 SQLite 新增同步元数据表

**文件**：`src/qwenpaw/agents/context/scroll/history.py`

在 `_init_schema()`（第 145 行）中追加：

```python
def _init_schema(self) -> None:
    with self._conn:
        # ... 现有 conversation_history 表 ...

        # 新增：同步元数据表
        self._conn.execute(
            """
            CREATE TABLE IF NOT EXISTS sync_meta (
                key   TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
            """,
        )

        # 新增：上行队列表（持久化，防进程崩溃丢失）
        # 仅当 sync 启用时使用，不影响现有逻辑
        self._conn.execute(
            """
            CREATE TABLE IF NOT EXISTS sync_uplink_queue (
                seq          INTEGER PRIMARY KEY,
                session_id   TEXT NOT NULL,
                agent_id     TEXT,
                kind         TEXT,
                role         TEXT,
                name         TEXT,
                content      TEXT,
                tool_call_id TEXT,
                tool_input   TEXT,
                tool_state   TEXT,
                headline     TEXT,
                blocks       TEXT,
                metadata     TEXT,
                created_at   TEXT,
                dedup_key    TEXT,
                attempts     INTEGER DEFAULT 0,
                queued_at    TEXT
            )
            """,
        )
```

### 2.2 同步元数据读写

```python
# history.py 新增

def get_sync_meta(self, key: str, default: str = "") -> str:
    """读取同步元数据。"""
    with self._lock:
        row = self._conn.execute(
            "SELECT value FROM sync_meta WHERE key = ?",
            (key,),
        ).fetchone()
        return row["value"] if row else default

def set_sync_meta(self, key: str, value: str) -> None:
    """写入同步元数据。"""
    with self._lock, self._conn:
        self._conn.execute(
            "INSERT INTO sync_meta (key, value) VALUES (?, ?) "
            "ON CONFLICT(key) DO UPDATE SET value = excluded.value",
            (key, value),
        )
```

### 2.3 上行队列持久化操作

```python
# history.py 新增

def enqueue_uplink(self, row: dict) -> None:
    """将一条记忆加入上行队列（持久化到 SQLite）。"""
    with self._lock, self._conn:
        self._conn.execute(
            """
            INSERT OR IGNORE INTO sync_uplink_queue
            (seq, session_id, agent_id, kind, role, name, content,
             tool_call_id, tool_input, tool_state, headline, blocks,
             metadata, created_at, dedup_key, queued_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                row["seq"], row["session_id"], row.get("agent_id"),
                row.get("kind"), row.get("role"), row.get("name"),
                row.get("content"), row.get("tool_call_id"),
                row.get("tool_input"), row.get("tool_state"),
                row.get("headline"), row.get("blocks"),
                row.get("metadata"), row.get("created_at"),
                row.get("dedup_key"),
                datetime.now(timezone.utc).isoformat(),
            ),
        )

def dequeue_uplink_batch(self, batch_size: int) -> list[dict]:
    """取出一批待上行记录。"""
    with self._lock, self._conn:
        rows = self._conn.execute(
            "SELECT * FROM sync_uplink_queue "
            "ORDER BY seq ASC LIMIT ?",
            (batch_size,),
        ).fetchall()
        return [dict(r) for r in rows]

def remove_uplinked(self, seqs: list[int]) -> None:
    """上行成功后从队列删除。"""
    if not seqs:
        return
    placeholders = ", ".join("?" for _ in seqs)
    with self._lock, self._conn:
        self._conn.execute(
            f"DELETE FROM sync_uplink_queue WHERE seq IN ({placeholders})",
            seqs,
        )

def increment_uplink_attempts(self, seqs: list[int]) -> None:
    """上行失败时增加重试计数。"""
    if not seqs:
        return
    placeholders = ", ".join("?" for _ in seqs)
    with self._lock, self._conn:
        self._conn.execute(
            f"UPDATE sync_uplink_queue SET attempts = attempts + 1 "
            f"WHERE seq IN ({placeholders})",
            seqs,
        )

def uplink_queue_size(self) -> int:
    """返回上行队列长度。"""
    with self._lock:
        row = self._conn.execute(
            "SELECT COUNT(*) AS n FROM sync_uplink_queue",
        ).fetchone()
        return int(row["n"])
```

---

## 3. HistoryStore 改造

### 3.1 构造函数增加 sync_hook

**文件**：`src/qwenpaw/agents/context/scroll/history.py`

```python
class HistoryStore:
    def __init__(
        self,
        db_path: str | Path,
        sync_hook: "Callable[[int, str, LogEntry, str | None, str | None], None] | None" = None,
    ) -> None:
        self._path = Path(db_path).expanduser()
        self._path.parent.mkdir(parents=True, exist_ok=True)
        self._lock = threading.Lock()
        self.quarantined_to: Path | None = None
        self.degraded = False
        self.write_failures = 0
        self._closed = False
        self._sync_hook = sync_hook  # 新增
        try:
            self._open_and_init()
        except sqlite3.DatabaseError as exc:
            self._quarantine(exc)
            self._open_and_init()
```

### 3.2 append() 增加 hook 调用

在 `append()`（第 261 行）末尾，`return seq` 之前追加：

```python
def append(
    self,
    *,
    session_id: str,
    entry: LogEntry,
    agent_id: str | None = None,
    dedup_key: str | None = None,
) -> int:
    row = self._insert_row(session_id, agent_id, entry, dedup_key)
    with self._lock, self._conn:
        cur = self._conn.execute(
            f"INSERT INTO conversation_history "
            f"({', '.join(_INSERT_COLUMNS)}) VALUES ({placeholders}) "
            f"ON CONFLICT(session_id, dedup_key) DO NOTHING",
            row,
        )
        if cur.rowcount == 0:
            existing = self._conn.execute(
                "SELECT seq FROM conversation_history "
                "WHERE session_id = ? AND dedup_key = ?",
                (session_id, dedup_key),
            ).fetchone()
            return int(existing["seq"]) if existing else 0
        seq = int(cur.lastrowid or 0)
        if self._fts and entry.name not in _RECALL_TOOL_NAMES:
            self._conn.execute(
                "INSERT INTO conversation_history_fts(rowid, content) "
                "VALUES (?, ?)",
                (seq, entry.content or ""),
            )

    # 新增：同步 hook（仅对新插入的行，非阻塞）
    if seq > 0 and self._sync_hook:
        try:
            self._sync_hook(seq, session_id, entry, dedup_key, agent_id)
        except Exception:
            logger.debug("sync hook failed, ignored", exc_info=True)

    return seq
```

> **关键**：hook 在 `with self._lock, self._conn` 块**外部**调用，不持有数据库锁，不影响写入性能。hook 本身只是入队操作（非阻塞 `put_nowait`）。

### 3.3 append_many() 同理

在 `append_many()`（第 306 行）中，对新增行触发 hook：

```python
def append_many(
    self,
    *,
    session_id: str,
    entries: Sequence[tuple[LogEntry, str | None]],
    agent_id: str | None = None,
) -> int:
    # ... 现有逻辑 ...
    inserted_seqs: list[tuple[int, LogEntry, str | None]] = []

    with self._lock, self._conn:
        for entry, dedup_key in entries:
            row = self._insert_row(session_id, agent_id, entry, dedup_key)
            cur = self._conn.execute(sql, row)
            if cur.rowcount == 0:
                continue
            inserted += 1
            seq = int(cur.lastrowid or 0)
            if self._fts and entry.name not in _RECALL_TOOL_NAMES:
                self._conn.execute(
                    "INSERT INTO conversation_history_fts(rowid, content) "
                    "VALUES (?, ?)",
                    (seq, entry.content or ""),
                )
            if self._sync_hook:
                inserted_seqs.append((seq, entry, dedup_key))

    # 新增：批量触发 hook（锁外部）
    if self._sync_hook:
        for seq, entry, dedup_key in inserted_seqs:
            try:
                self._sync_hook(seq, session_id, entry, dedup_key, agent_id)
            except Exception:
                logger.debug("sync hook failed, ignored", exc_info=True)

    return inserted
```

---

## 4. MemorySyncManager 设计

**新建文件**：`src/qwenpaw/sync/memory_sync.py`

### 4.1 类定义

```python
# -*- coding: utf-8 -*-
"""记忆双向同步管理器。

- 上行：接收 HistoryStore hook，批量异步推送到 pig
- 下行：登录时从 pig 拉取增量记忆到本地 SQLite
- 对账：每次登录全量对账，修复断网丢失

核心原则：所有操作不阻塞本地主流程。
"""
from __future__ import annotations

import asyncio
import logging
from typing import Any, TYPE_CHECKING

from ..config.config import MemorySyncConfig
from .client import PigClient
from .errors import SyncError, SyncNetworkError, SyncServerError

if TYPE_CHECKING:
    from ..agents.context.scroll.history import HistoryStore

logger = logging.getLogger(__name__)

_LAST_SYNC_SEQ_KEY = "pig_memory_last_sync_seq"
_MAX_PULL_ROUNDS = 100  # 防止无限拉取


class MemorySyncManager:
    """记忆双向同步管理器。

    生命周期：由 SyncLifecycle 持有，登录后 start()，退出时 flush_and_stop()。
    """

    def __init__(
        self,
        pig_client: PigClient,
        history: "HistoryStore",
        config: MemorySyncConfig,
    ) -> None:
        self._pig = pig_client
        self._history = history
        self._config = config
        self._uplink_worker: asyncio.Task | None = None
        self._stopping = False

    # ================================================================
    # 上行（本地 → 服务端）
    # ================================================================

    def on_history_appended(
        self,
        seq: int,
        session_id: str,
        entry: Any,  # LogEntry
        dedup_key: str | None,
        agent_id: str | None,
    ) -> None:
        """HistoryStore sync_hook 回调。

        非阻塞：将记录持久化到 sync_uplink_queue 表。
        UplinkWorker 会异步批量推送。
        """
        if not self._config.sync_enabled:
            return
        try:
            row = {
                "seq": seq,
                "session_id": session_id,
                "agent_id": agent_id,
                "kind": entry.kind,
                "role": entry.role,
                "name": entry.name,
                "content": entry.content,
                "tool_call_id": entry.tool_call_id,
                "tool_input": _safe_json(entry.tool_input),
                "tool_state": entry.tool_state,
                "headline": entry.headline,
                "blocks": _safe_json(entry.blocks),
                "metadata": _safe_json(entry.metadata),
                "created_at": entry.created_at,
                "dedup_key": dedup_key,
            }
            self._history.enqueue_uplink(row)
        except Exception:
            logger.debug(
                "enqueue_uplink failed for seq=%d, ignored",
                seq,
                exc_info=True,
            )

    async def _uplink_worker(self) -> None:
        """后台 worker：从队列取批量上行到 pig。

        循环：取一批 → 上传 → 成功删除 / 失败增加重试计数 → 等待 → 重复
        """
        while not self._stopping:
            try:
                batch = self._history.dequeue_uplink_batch(
                    self._config.batch_size,
                )
                if not batch:
                    await asyncio.sleep(2.0)  # 无数据时等待
                    continue

                await self._upload_batch(batch)

            except asyncio.CancelledError:
                break
            except Exception:
                logger.warning(
                    "uplink worker error, will retry in 5s",
                    exc_info=True,
                )
                await asyncio.sleep(5.0)

    async def _upload_batch(self, batch: list[dict]) -> None:
        """上传一批记忆到 pig。

        成功的从队列删除，失败的记录重试次数。
        超过 retry_times 的记录也删除（放弃，下次对账修复）。
        """
        seqs = [r["seq"] for r in batch]

        # 序列化为 pig 接口格式
        entries = [_row_to_uplink_payload(r) for r in batch]

        try:
            result = await self._pig.post(
                "/api/sync/memory",
                json={"entries": entries},
            )
            accepted = result.get("accepted", 0)
            logger.debug(
                "uplink batch: %d entries, %d accepted",
                len(batch),
                accepted,
            )
            # 全部成功，从队列删除
            self._history.remove_uplinked(seqs)

            # 更新已同步游标
            max_seq = max(seqs)
            current = int(self._history.get_sync_meta(
                _LAST_SYNC_SEQ_KEY, "0",
            ))
            if max_seq > current:
                self._history.set_sync_meta(
                    _LAST_SYNC_SEQ_KEY, str(max_seq),
                )

        except (SyncNetworkError, SyncServerError) as exc:
            logger.warning(
                "uplink batch failed (%d entries): %s",
                len(batch),
                exc,
            )
            # 增加重试计数
            self._history.increment_uplink_attempts(seqs)
            # 检查是否超过最大重试次数
            self._purge_exhausted()
            # 等待后由 worker 循环重试
            await asyncio.sleep(3.0)

        except SyncError as exc:
            logger.error(
                "uplink batch unrecoverable error: %s",
                exc,
            )
            # 不可恢复的错误，放弃这批
            self._history.remove_uplinked(seqs)

    def _purge_exhausted(self) -> None:
        """删除超过最大重试次数的记录（下次对账修复）。"""
        # 直接删除 attempts > retry_times 的记录
        # 实施时在 HistoryStore 增加 purge_exhausted_uplink(max_attempts) 方法
        pass

    # ================================================================
    # 下行（服务端 → 本地）
    # ================================================================

    async def pull_incremental(self) -> int:
        """登录时从 pig 拉取增量记忆到本地。

        1. 读取本地 last_sync_seq
        2. 分页 GET /api/sync/memory?since={seq}
        3. 用 history.append_many() 写入（dedup_key 幂等）
        4. 更新 last_sync_seq

        Returns:
            本次拉取的记录数。
        """
        if not self._config.sync_enabled:
            return 0

        total_pulled = 0
        since = int(self._history.get_sync_meta(
            _LAST_SYNC_SEQ_KEY, "0",
        ))

        try:
            for _ in range(_MAX_PULL_ROUNDS):
                resp = await self._pig.get(
                    "/api/sync/memory",
                    params={
                        "since": since,
                        "limit": self._config.pull_batch_size,
                    },
                )
                entries = resp.get("entries", [])
                next_seq = resp.get("next_seq", since)
                has_more = resp.get("has_more", False)

                if not entries:
                    break

                # 转换为 LogEntry + dedup_key，批量写入
                log_entries = [
                    _payload_to_log_entry(e) for e in entries
                ]
                dedup_keys = [e.get("dedup_key") for e in entries]

                self._history.append_many(
                    session_id="",  # 从 payload 中取
                    entries=log_entries,
                )

                # 更新游标
                since = next_seq
                self._history.set_sync_meta(
                    _LAST_SYNC_SEQ_KEY, str(since),
                )
                total_pulled += len(entries)

                if not has_more:
                    break

            logger.info(
                "memory pull: %d entries, cursor now at %d",
                total_pulled,
                since,
            )
        except SyncError as exc:
            logger.warning("memory pull failed: %s", exc)
        except Exception:
            logger.warning(
                "memory pull unexpected error",
                exc_info=True,
            )

        return total_pulled

    # ================================================================
    # 对账
    # ================================================================

    async def reconcile(self) -> None:
        """全量对账：确保本地与服务端一致。

        1. 先 flush 上行队列
        2. 查询服务端 max_seq
        3. 如本地 last_sync_seq < max_seq，增量拉取
        """
        # 先推完上行队列
        await self._flush_uplink()

        try:
            meta = await self._pig.get("/api/sync/memory/meta")
            server_max_seq = int(meta.get("max_seq", 0))
            local_seq = int(self._history.get_sync_meta(
                _LAST_SYNC_SEQ_KEY, "0",
            ))

            if server_max_seq > local_seq:
                logger.info(
                    "reconcile: pulling %d entries "
                    "(local=%d, server=%d)",
                    server_max_seq - local_seq,
                    local_seq,
                    server_max_seq,
                )
                await self.pull_incremental()
            else:
                logger.debug(
                    "reconcile: up to date (local=%d, server=%d)",
                    local_seq,
                    server_max_seq,
                )
        except SyncError as exc:
            logger.warning("reconcile failed: %s", exc)

    # ================================================================
    # 生命周期
    # ================================================================

    async def start(self) -> None:
        """启动上行 worker。"""
        if self._uplink_worker is None or self._uplink_worker.done():
            self._stopping = False
            self._uplink_worker = asyncio.create_task(
                self._uplink_worker_loop(),
            )
            logger.info("MemorySyncManager uplink worker started")

    async def _uplink_worker_loop(self) -> None:
        """worker 主循环（含启动时对账）。"""
        # 启动时先对账一次
        await self.reconcile()
        # 然后进入上行循环
        await self._uplink_worker()

    async def flush_and_stop(self) -> None:
        """退出时 flush 上行队列后停止。"""
        self._stopping = True
        if self._uplink_worker and not self._uplink_worker.done():
            self._uplink_worker.cancel()
            try:
                await asyncio.wait_for(
                    self._uplink_worker, timeout=10.0,
                )
            except (asyncio.CancelledError, asyncio.TimeoutError):
                pass

        if self._config.flush_on_exit:
            await self._flush_uplink()

    async def _flush_uplink(self) -> None:
        """将队列中剩余记录全部上行。"""
        for _ in range(_MAX_PULL_ROUNDS):
            batch = self._history.dequeue_uplink_batch(
                self._config.batch_size,
            )
            if not batch:
                break
            try:
                await self._upload_batch(batch)
            except Exception:
                logger.warning("flush uplink batch failed", exc_info=True)
                break


# --- 序列化辅助 ---

def _safe_json(value: Any) -> str | None:
    """安全 JSON 序列化，失败返回 None。"""
    import json
    if value is None:
        return None
    try:
        return json.dumps(value, ensure_ascii=False, default=str)
    except (TypeError, ValueError):
        return str(value)


def _row_to_uplink_payload(row: dict) -> dict:
    """将 SQLite 行转换为 pig 上行接口格式。"""
    return {
        "seq": row["seq"],
        "session_id": row["session_id"],
        "agent_id": row.get("agent_id"),
        "kind": row.get("kind"),
        "role": row.get("role"),
        "name": row.get("name"),
        "content": row.get("content"),
        "tool_call_id": row.get("tool_call_id"),
        "tool_input": row.get("tool_input"),
        "tool_state": row.get("tool_state"),
        "headline": row.get("headline"),
        "blocks": row.get("blocks"),
        "metadata": row.get("metadata"),
        "created_at": row.get("created_at"),
        "dedup_key": row.get("dedup_key"),
    }


def _payload_to_log_entry(payload: dict) -> tuple:
    """将 pig 下行 payload 转换为 (LogEntry, dedup_key) 元组。

    实施时需对照 LogEntry 的字段定义。
    """
    from ..agents.context.scroll.types import LogEntry
    import json

    entry = LogEntry(
        kind=payload.get("kind", ""),
        role=payload.get("role"),
        name=payload.get("name"),
        content=payload.get("content"),
        tool_call_id=payload.get("tool_call_id"),
        tool_input=_safe_loads(payload.get("tool_input")),
        tool_state=payload.get("tool_state"),
        headline=payload.get("headline"),
        blocks=_safe_loads(payload.get("blocks")),
        metadata=_safe_loads(payload.get("metadata")),
        created_at=payload.get("created_at"),
    )
    dedup_key = payload.get("dedup_key")
    return (entry, dedup_key)


def _safe_loads(value: str | None) -> Any:
    """安全 JSON 反序列化。"""
    import json
    if not value:
        return None
    try:
        return json.loads(value)
    except (json.JSONDecodeError, TypeError):
        return None
```

---

## 5. 下行拉取（登录时）

### 5.1 触发时机

**文件**：`src/qwenpaw/sync/lifecycle.py`

`on_login_success()` 中已预留 memory sync 的调用（阶段 0 设计）。实际实现：

```python
async def on_login_success(self, user: str) -> None:
    if not self._pig_client:
        return

    cfg = load_config()

    # 记忆同步
    if cfg.sync.memory.sync_enabled:
        # 找到活动的 HistoryStore 实例
        history = self._get_active_history()
        if history:
            if not self._memory_sync:
                self._memory_sync = MemorySyncManager(
                    self._pig_client,
                    history,
                    cfg.sync.memory,
                )
                self.register_memory_sync(self._memory_sync)

            # 登录时拉取
            pulled = await self._memory_sync.pull_incremental()
            # 启动上行 worker
            await self._memory_sync.start()
```

### 5.2 获取活动 HistoryStore

需要从 workspace 上下文中获取当前活动的 `HistoryStore` 实例。

**方案**：在 `ScrollContextManager` 创建 `HistoryStore` 时注册到全局注册表。

```python
# src/qwenpaw/agents/context/__init__.py 第186行附近

# 全局注册表（agent_id → HistoryStore）
_history_registry: dict[str, "HistoryStore"] = {}

def get_history_store(agent_id: str) -> "HistoryStore | None":
    """获取指定 agent 的 HistoryStore。"""
    return _history_registry.get(agent_id)
```

在 `HistoryStore` 构造后注册：

```python
history = HistoryStore(db_path, sync_hook=_get_sync_hook())
_history_registry[agent_id] = history
```

> `SyncLifecycle._get_active_history()` 遍历注册表，返回第一个（单用户场景只有一个 agent）。

---

## 6. 上行推送（工作中）

### 6.1 数据流

```
HistoryStore.append()
    │
    │ seq > 0 and sync_hook
    ▼
sync_hook = MemorySyncManager.on_history_appended()
    │
    │ 非阻塞 enqueue_uplink() → SQLite sync_uplink_queue 表
    ▼
UplinkWorker (asyncio.Task, 后台循环)
    │
    │ dequeue_uplink_batch(batch_size=50)
    ▼
PigClient.post("/api/sync/memory", {entries: [...]})
    │
    ├─ 成功 → remove_uplinked(seqs) + 更新 last_sync_seq
    ├─ 网络失败 → increment_uplink_attempts(seqs) + 等3秒重试
    └─ 不可恢复 → remove_uplinked(seqs) + 记日志
```

### 6.2 性能保障

| 措施 | 说明 |
|------|------|
| sync_hook 在锁外调用 | 不影响 `append()` 的写入延迟 |
| enqueue 用 `INSERT OR IGNORE` | 幂等，重复 hook 不产生重复队列项 |
| 批量上行（50条/批） | 减少 HTTP 请求次数 |
| 无数据时 sleep 2s | 避免 CPU 空转 |
| 上行失败 sleep 3s | 避免对 pig 的压力 |

---

## 7. 对账机制

### 7.1 何时对账

| 时机 | 触发 | 动作 |
|------|------|------|
| **登录后 worker 启动** | `_uplink_worker_loop()` 开头 | flush 上行 → 查服务端 max_seq → 增量拉取 |
| **退出时** | `flush_and_stop()` | 仅 flush 上行（不拉取） |
| **手动触发** | API 端点（可选） | 完整 reconcile |

### 7.2 对账逻辑

```python
async def reconcile(self) -> None:
    # 1. 先推完上行队列（本地有、服务端无的）
    await self._flush_uplink()

    # 2. 查服务端最新 seq
    meta = await self._pig.get("/api/sync/memory/meta")
    server_max_seq = meta["max_seq"]

    # 3. 比对游标
    local_seq = self._history.get_sync_meta(_LAST_SYNC_SEQ_KEY, "0")
    if server_max_seq > local_seq:
        # 服务端有本地没有的，拉取
        await self.pull_incremental()
```

### 7.3 多设备场景

```
设备A写入 seq=100 → 上行到服务端 → 服务端 max_seq=100
设备B登录 → 本地 last_sync_seq=80
           → 对账发现 server_max_seq(100) > local(80)
           → 拉取 seq 81-100 到本地
```

dedup_key 保证即使设备B也有部分相同记录，不会重复插入。

---

## 8. HistoryStore 实例化注入

### 8.1 所有实例化点

经代码搜索，`HistoryStore` 有 3 个实例化点：

| 位置 | 文件 | 行号 | 用途 |
|------|------|------|------|
| ① | `src/qwenpaw/agents/context/__init__.py` | 186 | **主路径**：ScrollContextManager 创建时 |
| ② | `src/qwenpaw/agents/context/scroll/sync.py` | 550 | 文件→DB 同步（一次性迁移） |
| ③ | `src/qwenpaw/agents/command_handler.py` | 577 | 命令处理器中的历史查询 |

### 8.2 注入策略

**主路径（①）**需要注入 sync_hook：

```python
# src/qwenpaw/agents/context/__init__.py 第186行

def _get_sync_hook():
    """获取记忆同步 hook（如果 sync 启用）。"""
    try:
        from ...config import load_config
        from ...sync.lifecycle import get_sync_lifecycle
        cfg = load_config()
        if not (cfg.sync.enabled and cfg.sync.memory.sync_enabled):
            return None
        sync_lc = get_sync_lifecycle()
        if sync_lc and sync_lc._memory_sync:
            return sync_lc._memory_sync.on_history_appended
    except Exception:
        return None
    return None

# 创建 HistoryStore 时注入
history = HistoryStore(db_path, sync_hook=_get_sync_hook())
```

**迁移路径（②）**和**查询路径（③）**不需要注入 sync_hook（它们是一次性操作或只读操作）。

### 8.3 时序问题

`HistoryStore` 在 agent 启动时创建，而 `MemorySyncManager` 在登录成功后创建。存在时序窗口：

```
app启动 → HistoryStore创建(sync_hook=None) → ... → 登录成功 → MemorySyncManager创建
```

**解决方案**：`HistoryStore` 的 `sync_hook` 支持运行时设置：

```python
class HistoryStore:
    def set_sync_hook(self, hook) -> None:
        """运行时设置 sync_hook（登录后调用）。"""
        self._sync_hook = hook
```

`MemorySyncManager.start()` 中：

```python
async def start(self) -> None:
    # 注入 hook 到已存在的 HistoryStore
    self._history.set_sync_hook(self.on_history_appended)
    # 启动 worker
    ...
```

---

## 9. pig 服务端接口约定

### 9.1 下行拉取

```
GET /api/sync/memory?since={seq}&limit=500

Response 200:
{
  "entries": [
    {
      "seq": 81,
      "session_id": "abc123",
      "agent_id": "default",
      "kind": "turn",
      "role": "assistant",
      "name": "agent",
      "content": "Hello!",
      "tool_call_id": null,
      "tool_input": null,
      "tool_state": null,
      "headline": "Greeting",
      "blocks": "[{...}]",
      "metadata": null,
      "created_at": "2026-07-30T10:00:00Z",
      "dedup_key": "msg-uuid-123"
    }
  ],
  "next_seq": 81,
  "has_more": false
}
```

### 9.2 上行推送

```
POST /api/sync/memory
Content-Type: application/json

Body:
{
  "entries": [
    { ... 同上格式 ... }
  ]
}

Response 200:
{
  "accepted": 50,
  "deduped": 3
}
```

服务端处理：对每条记录 `INSERT ... ON CONFLICT(session_id, dedup_key) DO NOTHING`。

### 9.3 对账元数据

```
GET /api/sync/memory/meta

Response 200:
{
  "max_seq": 100
}
```

### 9.4 pig 服务端实现要点（Java/Spring）

```java
// pig 侧需新建表
CREATE TABLE qwenpaw_conversation_history (
    seq          BIGSERIAL PRIMARY KEY,  -- 全局自增
    session_id   VARCHAR(255) NOT NULL,
    agent_id     VARCHAR(255),
    kind         VARCHAR(50) NOT NULL,
    role         VARCHAR(50),
    name         VARCHAR(255),
    content      TEXT,
    tool_call_id VARCHAR(255),
    tool_input   TEXT,
    tool_state   VARCHAR(50),
    headline     VARCHAR(500),
    blocks       TEXT,
    metadata     TEXT,
    created_at   TIMESTAMP,
    dedup_key    VARCHAR(255),
    user_id      VARCHAR(255) NOT NULL,  -- pig 用户ID
    UNIQUE(session_id, dedup_key, user_id)
);
CREATE INDEX idx_qch_user_seq ON qwenpaw_conversation_history(user_id, seq);
```

> `user_id` 从 JWT 的 `sub` claim 提取，确保多用户数据隔离。

---

## 10. 验收标准

### 功能验收

- [ ] 登录后本地 SQLite 包含服务端全部记忆（增量拉取）
- [ ] 拉取日志显示 `memory pull: N entries, cursor now at M`
- [ ] 工作中新产生的记忆进入 `sync_uplink_queue` 表
- [ ] 后台 worker 批量上行到 pig，日志显示 `uplink batch: N entries, M accepted`
- [ ] 上行成功后记录从 `sync_uplink_queue` 删除
- [ ] `sync_meta` 表中 `pig_memory_last_sync_seq` 正确更新

### 断网容错验收

- [ ] 断网时本地记忆读写完全正常（sync_hook 仍入队，worker 重试等待）
- [ ] 断网期间产生的记忆堆积在 `sync_uplink_queue` 表
- [ ] 联网后 worker 自动批量补传
- [ ] 补传完成后 `sync_uplink_queue` 清空
- [ ] 超过 `retry_times` 的记录被放弃（下次对账修复）

### 幂等验收

- [ ] 同一 `dedup_key` 记忆上行多次，服务端只存一条
- [ ] 拉取时本地已存在的 `dedup_key` 不重复插入
- [ ] 进程重启后 worker 从 `sync_uplink_queue` 恢复未上行记录

### 对账验收

- [ ] 登录后 worker 启动时自动对账
- [ ] 本地有、服务端无的记忆被上行补传
- [ ] 服务端有、本地无的记忆被拉取
- [ ] 多设备场景下记忆最终一致

### 性能验收

- [ ] `append()` 延迟不受 sync_hook 影响（hook 在锁外，非阻塞）
- [ ] 无记忆数据时 worker sleep 2s，CPU 占用极低
- [ ] 批量上行 50 条/次，HTTP 请求不过频

### 开关验收

- [ ] `memory.sync_enabled=false` 时 `sync_hook=None`，零行为变化
- [ ] `sync.enabled=false` 时不创建 `sync_uplink_queue` 表相关逻辑
- [ ] pig 不可达时不影响本地任何操作

---

*文档创建日期：2026-07-30*
*配套文档：[architecture.md](./architecture.md) · [implementation-plan.md](./implementation-plan.md) · [phase0-infrastructure.md](./phase0-infrastructure.md) · [phase1-auth-oauth2.md](./phase1-auth-oauth2.md)*
