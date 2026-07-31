# 阶段 0 详细设计：基础设施搭建

> **配套文档**：[architecture.md](./architecture.md) · [implementation-plan.md](./implementation-plan.md)
>
> **目标**：搭建同步层骨架——配置模型、pig HTTP 客户端、OAuth2 token 管理、生命周期挂钩——为后续 5 个阶段提供统一基础。
>
> **预估工期**：2-3 天 · **依赖**：无

---

## 目录

- [1. 模块结构](#1-模块结构)
- [2. 配置模型设计](#2-配置模型设计)
- [3. 异常体系](#3-异常体系)
- [4. pig HTTP 客户端](#4-pig-http-客户端)
- [5. OAuth2 Token 管理](#5-oauth2-token-管理)
- [6. 生命周期管理器](#6-生命周期管理器)
- [7. app lifespan 接入](#7-app-lifespan-接入)
- [8. 验收标准](#8-验收标准)

---

## 1. 模块结构

```
src/qwenpaw/sync/
  ├── __init__.py          # 公共接口导出
  ├── errors.py            # 异常定义
  ├── client.py            # PigClient — pig HTTP 客户端
  ├── auth_adapter.py      # PigTokenProvider — OAuth2 token 管理
  └── lifecycle.py         # SyncLifecycle — 生命周期挂钩
```

依赖关系：

```
lifecycle.py
  ├── client.py (PigClient)
  │     └── auth_adapter.py (PigTokenProvider)
  └── 后续阶段: memory_sync / credential_sync / skill_sync / mcp_sync
```

---

## 2. 配置模型设计

### 2.1 Pydantic 模型定义

**文件**：`src/qwenpaw/config/config.py`

在 `SecurityConfig`（第 2340 行）之后新增以下模型：

```python
from typing import Literal, Optional


class PigAuthConfig(BaseModel):
    """pig OAuth2 认证配置。

    所有 pig 连接参数集中在此，client_secret 复用现有 Fernet 加密
    （ENC: 前缀，由 secret_store.py 透明处理）。
    """
    model_config = ConfigDict(extra="ignore")

    base_url: str = Field(
        default="",
        description="pig 服务端基础 URL，如 https://pig.example.com",
    )
    client_id: str = Field(
        default="qwenpaw",
        description="OAuth2 client_id，在 pig 管理后台注册",
    )
    client_secret: str = Field(
        default="",
        description=(
            "OAuth2 client_secret，加密存储。写入 config.yaml 时使用 "
            "ENC: 前缀，运行时由 secret_store.decrypt() 透明解密。"
        ),
    )
    token_endpoint: str = Field(
        default="/oauth2/token",
        description="令牌端点（授权码换 token / 刷新 token）",
    )
    introspect_endpoint: str = Field(
        default="/oauth2/introspect",
        description="令牌内省端点（可选，JWT 本地验签时不用）",
    )
    jwks_endpoint: str = Field(
        default="/oauth2/jwks",
        description="JWKS 公钥端点（JWT 本地验签用）",
    )
    authorize_endpoint: str = Field(
        default="/oauth2/authorize",
        description="授权端点（前端跳转 SSO 登录页）",
    )
    redirect_uri: str = Field(
        default="",
        description=(
            "OAuth2 回调地址，如 http://localhost:8000/api/sync/oauth/callback。"
            "需与 pig 管理后台注册的回调一致。"
        ),
    )
    scopes: str = Field(
        default="openid profile",
        description="请求的 OAuth2 scope",
    )


class MemorySyncConfig(BaseModel):
    """记忆同步配置。"""
    model_config = ConfigDict(extra="ignore")

    sync_enabled: bool = Field(
        default=True,
        description="是否启用记忆同步（上行 + 下行）",
    )
    batch_size: int = Field(
        default=50,
        ge=1,
        le=500,
        description="上行批量大小，每批推送的记忆条数",
    )
    retry_times: int = Field(
        default=3,
        ge=0,
        le=10,
        description="上行失败重试次数",
    )
    flush_on_exit: bool = Field(
        default=True,
        description="退出时是否 flush 上行队列",
    )
    pull_batch_size: int = Field(
        default=500,
        ge=1,
        le=5000,
        description="下行拉取每批条数",
    )


class SkillSyncConfig(BaseModel):
    """技能同步配置。"""
    model_config = ConfigDict(extra="ignore")

    auto_pull_enterprise: bool = Field(
        default=True,
        description="登录时是否自动拉取企业技能",
    )


class CredentialSyncConfig(BaseModel):
    """凭据同步配置。"""
    model_config = ConfigDict(extra="ignore")

    cache_enterprise: bool = Field(
        default=True,
        description="是否缓存企业凭据到本地",
    )
    sync_interval_minutes: int = Field(
        default=30,
        ge=5,
        le=1440,
        description="企业凭据定时同步间隔（分钟）",
    )


class McpSyncConfig(BaseModel):
    """MCP 模板同步配置。"""
    model_config = ConfigDict(extra="ignore")

    auto_pull_templates: bool = Field(
        default=True,
        description="登录时是否自动拉取企业 MCP 模板",
    )


class SyncConfig(BaseModel):
    """顶层 sync 配置段。

    enabled=false 时完全退化为纯本地模式，零影响。
    """
    model_config = ConfigDict(extra="ignore")

    enabled: bool = Field(
        default=False,
        description=(
            "总开关。false 时所有同步功能关闭，完全本地模式。"
            "可由环境变量 QWENPAW_SYNC_ENABLED 覆盖。"
        ),
    )
    auth_mode: Literal["local", "pig"] = Field(
        default="local",
        description="认证模式：local=本地 HMAC，pig=pig OAuth2",
    )
    pig: PigAuthConfig = Field(default_factory=PigAuthConfig)
    memory: MemorySyncConfig = Field(default_factory=MemorySyncConfig)
    skill: SkillSyncConfig = Field(default_factory=SkillSyncConfig)
    credential: CredentialSyncConfig = Field(default_factory=CredentialSyncConfig)
    mcp: McpSyncConfig = Field(default_factory=McpSyncConfig)
```

### 2.2 挂载到顶层 Config

在顶层 `Config` 模型中追加字段：

```python
class Config(BaseModel):
    # ... 现有字段 ...
    security: SecurityConfig = Field(default_factory=SecurityConfig)
    # 新增
    sync: SyncConfig = Field(default_factory=SyncConfig)
```

### 2.3 环境变量覆盖

在 `src/qwenpaw/constant.py` 的 `EnvVarLoader` 体系中无需额外代码——`SyncConfig.enabled` 可通过现有环境变量加载机制覆盖。实施时在 `config/utils.py` 的配置加载流程中增加：

```python
# config/utils.py — load_config() 中
if EnvVarLoader.get_bool("QWENPAW_SYNC_ENABLED"):
    config.sync.enabled = True
```

### 2.4 config.yaml 示例

```yaml
sync:
  enabled: true
  auth_mode: pig
  pig:
    base_url: https://pig.example.com
    client_id: qwenpaw
    client_secret: ENC:gAAAAABm...  # 复用 Fernet 加密
    redirect_uri: http://localhost:8000/api/sync/oauth/callback
    scopes: "openid profile"
  memory:
    sync_enabled: true
    batch_size: 50
    retry_times: 3
  skill:
    auto_pull_enterprise: true
  credential:
    cache_enterprise: true
    sync_interval_minutes: 30
  mcp:
    auto_pull_templates: true
```

---

## 3. 异常体系

**文件**：`src/qwenpaw/sync/errors.py`

```python
# -*- coding: utf-8 -*-
"""同步层异常定义。

所有异常继承 SyncError，同步失败永不阻塞本地主流程——
调用方应 catch SyncError 并记日志，不向上抛出。
"""
from __future__ import annotations


class SyncError(Exception):
    """同步层基础异常。"""


class SyncConfigError(SyncError):
    """配置错误（如 pig base_url 未配置但 enabled=true）。"""


class SyncAuthError(SyncError):
    """认证错误（token 过期、刷新失败、pig 返回 401）。"""


class SyncNetworkError(SyncError):
    """网络错误（连接超时、pig 不可达）。

    此异常可重试。
    """
    def __init__(self, message: str, *, retryable: bool = True):
        super().__init__(message)
        self.retryable = retryable


class SyncServerError(SyncError):
    """pig 服务端返回错误（5xx 或业务错误码）。"""

    def __init__(self, message: str, *, status_code: int, body: str = ""):
        super().__init__(message)
        self.status_code = status_code
        self.body = body


class SyncConflictError(SyncError):
    """同步冲突（数据版本不一致且无法自动解决）。"""
```

---

## 4. pig HTTP 客户端

**文件**：`src/qwenpaw/sync/client.py`

### 4.1 设计要点

- 基于 `httpx.AsyncClient`（项目已依赖 `httpx>=0.27.0`）
- Bearer token 从 `PigTokenProvider` 获取，401 时触发一次刷新后重试
- 重试：仅对 `SyncNetworkError` 重试，指数退避，最多 `max_retries` 次
- 超时：默认 30s，下载场景可传 `timeout=` 覆盖
- 单例：整个 app 生命周期共享一个 `PigClient` 实例

### 4.2 类定义

```python
# -*- coding: utf-8 -*-
"""pig 服务端 HTTP 客户端。

封装 Bearer token 注入、自动刷新、重试退避。
所有同步模块（记忆/凭据/技能/MCP）共用此客户端。
"""
from __future__ import annotations

import asyncio
import logging
from pathlib import Path
from typing import Any, Optional

import httpx

from ..config import load_config
from ..config.config import PigAuthConfig
from .auth_adapter import PigTokenProvider
from .errors import (
    SyncAuthError,
    SyncConfigError,
    SyncNetworkError,
    SyncServerError,
)

logger = logging.getLogger(__name__)

_DEFAULT_TIMEOUT = 30.0
_DOWNLOAD_TIMEOUT = 120.0
_RETRY_BASE_DELAY = 1.0  # 秒
_RETRY_MAX_DELAY = 30.0


class PigClient:
    """pig 服务端 HTTP 客户端。

    生命周期与 app 一致，由 SyncLifecycle 持有。
    """

    def __init__(
        self,
        pig_config: PigAuthConfig,
        token_provider: PigTokenProvider,
    ) -> None:
        if not pig_config.base_url:
            raise SyncConfigError(
                "sync.pig.base_url is required when sync is enabled",
            )
        self._config = pig_config
        self._token_provider = token_provider
        self._client: httpx.AsyncClient | None = None

    @property
    def base_url(self) -> str:
        return self._config.base_url.rstrip("/")

    async def _ensure_client(self) -> httpx.AsyncClient:
        """惰性创建 httpx client，复用连接池。"""
        if self._client is None or self._client.is_closed:
            self._client = httpx.AsyncClient(
                base_url=self.base_url,
                timeout=httpx.Timeout(_DEFAULT_TIMEOUT),
                limits=httpx.Limits(
                    max_connections=10,
                    max_keepalive_connections=5,
                ),
            )
        return self._client

    async def _request(
        self,
        method: str,
        path: str,
        *,
        json: dict | None = None,
        params: dict | None = None,
        timeout: float | None = None,
        max_retries: int = 3,
        _retry_on_auth: bool = True,
    ) -> httpx.Response:
        """发送请求，自动注入 Bearer token，401 时刷新重试。

        Args:
            max_retries: 网络错误重试次数（指数退避）。
            _retry_on_auth: 401 时是否刷新 token 重试（防止无限循环）。
        """
        client = await self._ensure_client()
        token = await self._token_provider.get_access_token()

        headers = {"Authorization": f"Bearer {token}"}
        to = httpx.Timeout(timeout) if timeout else None

        last_exc: Exception | None = None
        for attempt in range(max_retries + 1):
            try:
                resp = await client.request(
                    method,
                    path,
                    json=json,
                    params=params,
                    headers=headers,
                    timeout=to,
                )
                # 401: 尝试刷新 token 后重试一次
                if resp.status_code == 401 and _retry_on_auth:
                    logger.debug(
                        "pig returned 401, attempting token refresh",
                    )
                    refreshed = await self._token_provider.refresh()
                    if refreshed:
                        token = await self._token_provider.get_access_token()
                        headers["Authorization"] = f"Bearer {token}"
                        return await self._request(
                            method,
                            path,
                            json=json,
                            params=params,
                            timeout=timeout,
                            max_retries=max_retries,
                            _retry_on_auth=False,  # 只重试一次
                        )
                    raise SyncAuthError(
                        "Token refresh failed; re-login required",
                    )

                # 5xx: 服务端错误
                if resp.status_code >= 500:
                    raise SyncServerError(
                        f"pig server error: {resp.status_code}",
                        status_code=resp.status_code,
                        body=resp.text,
                    )

                # 4xx (非401): 客户端错误，不重试
                if resp.status_code >= 400:
                    raise SyncServerError(
                        f"pig client error: {resp.status_code}",
                        status_code=resp.status_code,
                        body=resp.text,
                    )

                return resp

            except httpx.ConnectError as exc:
                last_exc = exc
                logger.debug(
                    "pig connect error (attempt %d/%d): %s",
                    attempt + 1,
                    max_retries + 1,
                    exc,
                )
            except httpx.TimeoutException as exc:
                last_exc = exc
                logger.debug(
                    "pig timeout (attempt %d/%d): %s",
                    attempt + 1,
                    max_retries + 1,
                    exc,
                )

            # 指数退避
            if attempt < max_retries:
                delay = min(
                    _RETRY_BASE_DELAY * (2 ** attempt),
                    _RETRY_MAX_DELAY,
                )
                await asyncio.sleep(delay)

        raise SyncNetworkError(
            f"pig request failed after {max_retries + 1} attempts: {last_exc}",
        )

    async def get(
        self,
        path: str,
        params: dict | None = None,
        *,
        timeout: float | None = None,
    ) -> dict:
        """GET 请求，返回解析后的 JSON。"""
        resp = await self._request(
            "GET", path, params=params, timeout=timeout,
        )
        return resp.json()

    async def post(
        self,
        path: str,
        json: dict | None = None,
        *,
        timeout: float | None = None,
    ) -> dict:
        """POST 请求，返回解析后的 JSON。"""
        resp = await self._request(
            "POST", path, json=json, timeout=timeout,
        )
        return resp.json()

    async def download(self, path: str, dest: Path) -> None:
        """下载文件到指定路径（流式写入）。

        用于技能包下载，超时 120s。
        """
        client = await self._ensure_client()
        token = await self._token_provider.get_access_token()
        headers = {"Authorization": f"Bearer {token}"}

        try:
            async with client.stream(
                "GET",
                path,
                headers=headers,
                timeout=httpx.Timeout(_DOWNLOAD_TIMEOUT),
            ) as resp:
                if resp.status_code >= 400:
                    raise SyncServerError(
                        f"download failed: {resp.status_code}",
                        status_code=resp.status_code,
                        body=await resp.aread(),
                    )
                dest.parent.mkdir(parents=True, exist_ok=True)
                with open(dest, "wb") as f:
                    async for chunk in resp.aiter_bytes(chunk_size=65536):
                        f.write(chunk)
        except httpx.ConnectError as exc:
            raise SyncNetworkError(f"download connect error: {exc}") from exc
        except httpx.TimeoutException as exc:
            raise SyncNetworkError(f"download timeout: {exc}") from exc

    async def upload(self, path: str, file: Path) -> dict:
        """上传文件（multipart）。

        用于个人技能云备份，超时 120s。
        """
        client = await self._ensure_client()
        token = await self._token_provider.get_access_token()
        headers = {"Authorization": f"Bearer {token}"}

        try:
            with open(file, "rb") as f:
                resp = await client.post(
                    path,
                    files={"file": (file.name, f, "application/octet-stream")},
                    headers=headers,
                    timeout=httpx.Timeout(_DOWNLOAD_TIMEOUT),
                )
            if resp.status_code >= 400:
                raise SyncServerError(
                    f"upload failed: {resp.status_code}",
                    status_code=resp.status_code,
                    body=resp.text,
                )
            return resp.json()
        except httpx.ConnectError as exc:
            raise SyncNetworkError(f"upload connect error: {exc}") from exc
        except httpx.TimeoutException as exc:
            raise SyncNetworkError(f"upload timeout: {exc}") from exc

    async def close(self) -> None:
        """关闭 httpx client。"""
        if self._client and not self._client.is_closed:
            await self._client.aclose()
```

### 4.3 设计决策说明

| 决策 | 理由 |
|------|------|
| 惰性创建 client | 避免 `sync.enabled=false` 时创建无用的连接池 |
| 401 只重试一次 | 防止 token 持续无效时的无限刷新循环 |
| 5xx 抛异常不重试 | `SyncServerError` 由调用方决定是否重试（记忆上行有独立重试逻辑） |
| 网络错误指数退避 | 避免 pig 短暂不可达时打满连接 |
| download/upload 独立超时 | 技能包可能较大，30s 不够 |

---

## 5. OAuth2 Token 管理

**文件**：`src/qwenpaw/sync/auth_adapter.py`

### 5.1 设计要点

- token 持久化到 `SECRET_DIR/pig_tokens.json`（加密，复用 `secret_store.py`）
- 过期前 60 秒自动刷新
- 刷新失败标记 `needs_relogin = True`，前端感知后重新跳转登录
- 线程安全：`asyncio.Lock` 保护并发刷新

### 5.2 类定义

```python
# -*- coding: utf-8 -*-
"""pig OAuth2 token 生命周期管理。

token 持久化到 SECRET_DIR/pig_tokens.json（加密），
过期前自动刷新，刷新失败时标记需重新登录。
"""
from __future__ import annotations

import asyncio
import json
import logging
import time
from pathlib import Path
from typing import Any, Optional

import httpx

from ..config.config import PigAuthConfig
from ..constant import SECRET_DIR
from ..security.secret_store import decrypt, encrypt, is_encrypted
from .errors import SyncAuthError, SyncNetworkError

logger = logging.getLogger(__name__)

_TOKEN_FILE = SECRET_DIR / "pig_tokens.json"
_REFRESH_MARGIN = 60  # 过期前 60 秒刷新
_TOKEN_TIMEOUT = 15.0


class PigTokenProvider:
    """管理 pig OAuth2 access_token / refresh_token。

    生命周期与 PigClient 一致，由 SyncLifecycle 持有。
    """

    def __init__(self, pig_config: PigAuthConfig) -> None:
        self._config = pig_config
        self._lock = asyncio.Lock()
        self._tokens: dict[str, Any] = {}
        self._needs_relogin: bool = False
        self._load_tokens()

    # --- 持久化 ---

    def _load_tokens(self) -> None:
        """从磁盘加载 token（解密）。"""
        if not _TOKEN_FILE.is_file():
            return
        try:
            raw = _TOKEN_FILE.read_text(encoding="utf-8")
            data = json.loads(raw)
            # access_token / refresh_token 加密存储
            for key in ("access_token", "refresh_token"):
                val = data.get(key, "")
                if val and is_encrypted(val):
                    data[key] = decrypt(val)
            self._tokens = data
        except (json.JSONDecodeError, OSError) as exc:
            logger.warning("Failed to load pig tokens: %s", exc)

    def _save_tokens(self) -> None:
        """持久化 token 到磁盘（加密）。"""
        _TOKEN_FILE.parent.mkdir(parents=True, exist_ok=True)
        data = dict(self._tokens)
        for key in ("access_token", "refresh_token"):
            val = data.get(key, "")
            if val and not is_encrypted(val):
                data[key] = encrypt(val)
        _TOKEN_FILE.write_text(
            json.dumps(data, indent=2, ensure_ascii=False),
            encoding="utf-8",
        )
        try:
            _TOKEN_FILE.chmod(0o600)
        except OSError:
            pass

    # --- 公共接口 ---

    @property
    def needs_relogin(self) -> bool:
        """是否需要重新登录（refresh 失败）。"""
        return self._needs_relogin

    def set_tokens(self, access_token: str, refresh_token: str,
                   expires_in: int) -> None:
        """OAuth2 授权码流程成功后，由回调路由调用。"""
        self._tokens = {
            "access_token": access_token,
            "refresh_token": refresh_token,
            "expires_at": int(time.time()) + expires_in,
        }
        self._needs_relogin = False
        self._save_tokens()
        logger.info("pig OAuth2 tokens updated")

    def clear(self) -> None:
        """注销时清除 token。"""
        self._tokens = {}
        self._needs_relogin = False
        try:
            _TOKEN_FILE.unlink(missing_ok=True)
        except OSError:
            pass

    async def get_access_token(self) -> str:
        """获取有效的 access_token，必要时自动刷新。

        Raises:
            SyncAuthError: 无 token 或刷新失败。
        """
        if self._needs_relogin:
            raise SyncAuthError("Re-login required (token refresh failed)")

        access = self._tokens.get("access_token", "")
        if not access:
            raise SyncAuthError("No pig token; please login via pig SSO")

        expires_at = self._tokens.get("expires_at", 0)
        if expires_at - time.time() > _REFRESH_MARGIN:
            return access

        # 即将过期，刷新
        refreshed = await self.refresh()
        if not refreshed:
            raise SyncAuthError("Token refresh failed")
        return self._tokens["access_token"]

    async def refresh(self) -> bool:
        """使用 refresh_token 刷新 access_token。

        Returns:
            True 刷新成功，False 刷新失败（需重新登录）。
        """
        async with self._lock:  # 防止并发刷新
            # double-check: 可能在等锁期间已被其他协程刷新
            expires_at = self._tokens.get("expires_at", 0)
            if expires_at - time.time() > _REFRESH_MARGIN:
                return True

            refresh_token = self._tokens.get("refresh_token", "")
            if not refresh_token:
                self._needs_relogin = True
                return False

            try:
                async with httpx.AsyncClient(
                    timeout=httpx.Timeout(_TOKEN_TIMEOUT),
                ) as client:
                    resp = await client.post(
                        self._config.base_url.rstrip("/")
                        + self._config.token_endpoint,
                        data={
                            "grant_type": "refresh_token",
                            "refresh_token": refresh_token,
                            "client_id": self._config.client_id,
                            "client_secret": decrypt_if_needed(
                                self._config.client_secret,
                            ),
                        },
                        headers={
                            "Content-Type": "application/x-www-form-urlencoded",
                        },
                    )

                if resp.status_code != 200:
                    logger.warning(
                        "pig token refresh failed: %d %s",
                        resp.status_code,
                        resp.text,
                    )
                    self._needs_relogin = True
                    return False

                data = resp.json()
                self._tokens = {
                    "access_token": data["access_token"],
                    "refresh_token": data.get(
                        "refresh_token",
                        refresh_token,  # 部分 OAuth2 不返回新 refresh
                    ),
                    "expires_at": int(time.time())
                    + int(data.get("expires_in", 3600)),
                }
                self._save_tokens()
                logger.info("pig token refreshed successfully")
                return True

            except (httpx.ConnectError, httpx.TimeoutException) as exc:
                logger.warning("pig token refresh network error: %s", exc)
                # 网络错误不标记 relogin，下次重试
                return False
            except (KeyError, json.JSONDecodeError) as exc:
                logger.error("pig token refresh parse error: %s", exc)
                self._needs_relogin = True
                return False


def decrypt_if_needed(value: str) -> str:
    """如果值是 ENC: 加密的则解密，否则原样返回。"""
    if value and is_encrypted(value):
        return decrypt(value)
    return value
```

### 5.3 token 文件格式

```json
// ~/.qwenpaw.secret/pig_tokens.json (权限 0600)
{
  "access_token": "ENC:gAAAAABm...",
  "refresh_token": "ENC:gAAAAABm...",
  "expires_at": 1756684800
}
```

---

## 6. 生命周期管理器

**文件**：`src/qwenpaw/sync/lifecycle.py`

### 6.1 设计要点

- 统一管理所有同步模块的启动/关闭
- 登录成功后并发拉取（阶段 1-5 填充）
- 退出时 flush 记忆上行队列
- 定时同步（凭据/技能变更检查）
- 所有操作 catch 异常，不阻塞 app 启动

### 6.2 类定义

```python
# -*- coding: utf-8 -*-
"""同步层生命周期管理。

挂载到 app lifespan，统一管理所有同步模块的启动/关闭。
所有操作 catch 异常，不阻塞 app 主流程。
"""
from __future__ import annotations

import asyncio
import logging
from typing import Any, Optional

from ..config import load_config
from ..config.config import SyncConfig
from .auth_adapter import PigTokenProvider
from .client import PigClient
from .errors import SyncError

logger = logging.getLogger(__name__)


class SyncLifecycle:
    """同步层生命周期管理器。

    由 app.state.sync_lifecycle 持有，贯穿整个 app 生命周期。
    """

    def __init__(self) -> None:
        self._pig_client: Optional[PigClient] = None
        self._token_provider: Optional[PigTokenProvider] = None
        # 各阶段同步管理器（后续阶段填充）
        self._memory_sync: Any = None  # 阶段2: MemorySyncManager
        self._credential_sync: Any = None  # 阶段3: EnterpriseCredentialCache
        self._skill_sync: Any = None  # 阶段4: SkillSyncManager
        self._mcp_sync: Any = None  # 阶段5: McpTemplateSync
        self._periodic_task: Optional[asyncio.Task] = None

    @property
    def pig_client(self) -> Optional[PigClient]:
        return self._pig_client

    @property
    def token_provider(self) -> Optional[PigTokenProvider]:
        return self._token_provider

    @property
    def is_enabled(self) -> bool:
        """同步是否启用。"""
        try:
            cfg = load_config()
            return cfg.sync.enabled
        except Exception:
            return False

    # --- 启动 ---

    async def on_startup(self) -> None:
        """app 启动时调用。

        创建 PigClient 和 TokenProvider。
        不在此拉取数据——数据拉取在登录成功后触发（on_login_success）。
        """
        if not self.is_enabled:
            logger.debug("Sync layer disabled, skipping startup")
            return

        try:
            cfg = load_config()
            if not cfg.sync.pig.base_url:
                logger.warning(
                    "Sync enabled but pig.base_url not configured; "
                    "sync layer will be inactive",
                )
                return

            self._token_provider = PigTokenProvider(cfg.sync.pig)
            self._pig_client = PigClient(
                cfg.sync.pig,
                self._token_provider,
            )
            logger.info(
                "Sync layer initialized (pig: %s)",
                cfg.sync.pig.base_url,
            )
        except SyncError as exc:
            logger.warning("Sync layer init failed: %s", exc)
        except Exception:
            logger.warning(
                "Sync layer init encountered unexpected error",
                exc_info=True,
            )

    # --- 登录成功 ---

    async def on_login_success(self, user: str) -> None:
        """pig OAuth2 登录成功后调用。

        并发拉取：记忆增量 + 企业凭据 + 企业技能 + MCP模板。
        任何一项失败不影响其他项。
        """
        if not self._pig_client:
            return

        cfg = load_config()
        tasks: list[tuple[str, asyncio.Task]] = []

        # 各阶段填充具体拉取逻辑
        if cfg.sync.memory.sync_enabled and self._memory_sync:
            tasks.append((
                "memory",
                asyncio.create_task(
                    self._memory_sync.pull_incremental(),
                ),
            ))
        if cfg.sync.credential.cache_enterprise and self._credential_sync:
            tasks.append((
                "credentials",
                asyncio.create_task(
                    self._credential_sync.pull(),
                ),
            ))
        if cfg.sync.skill.auto_pull_enterprise and self._skill_sync:
            tasks.append((
                "skills",
                asyncio.create_task(
                    self._skill_sync.pull_enterprise_skills(),
                ),
            ))
        if cfg.sync.mcp.auto_pull_templates and self._mcp_sync:
            tasks.append((
                "mcp",
                asyncio.create_task(
                    self._mcp_sync.pull_templates(),
                ),
            ))

        if not tasks:
            return

        # 并发拉取，各自独立 catch
        results = await asyncio.gather(
            *[t[1] for t in tasks],
            return_exceptions=True,
        )
        for (name, _), result in zip(tasks, results):
            if isinstance(result, Exception):
                logger.warning(
                    "Sync pull '%s' failed on login: %s",
                    name,
                    result,
                )
            else:
                logger.debug("Sync pull '%s' completed", name)

    # --- 定时同步 ---

    def start_periodic_sync(self) -> None:
        """启动定时同步任务（凭据/技能变更检查）。"""
        if not self._pig_client:
            return
        self._periodic_task = asyncio.create_task(self._periodic_loop())

    async def _periodic_loop(self) -> None:
        """每 30 分钟检查凭据/技能变更。"""
        cfg = load_config()
        interval = cfg.sync.credential.sync_interval_minutes * 60

        while True:
            await asyncio.sleep(interval)
            try:
                if self._credential_sync:
                    await self._credential_sync.pull()
            except Exception:
                logger.warning(
                    "Periodic credential sync failed",
                    exc_info=True,
                )

    # --- 关闭 ---

    async def on_shutdown(self) -> None:
        """app 关闭时调用。

        flush 记忆上行队列，关闭 pig client。
        """
        # 取消定时任务
        if self._periodic_task and not self._periodic_task.done():
            self._periodic_task.cancel()

        # flush 记忆上行
        if self._memory_sync:
            try:
                await self._memory_sync.flush_and_stop()
            except Exception:
                logger.warning(
                    "Memory sync flush failed on shutdown",
                    exc_info=True,
                )

        # 关闭 pig client
        if self._pig_client:
            try:
                await self._pig_client.close()
            except Exception:
                logger.warning("PigClient close failed", exc_info=True)

    # --- 各阶段注册器 ---

    def register_memory_sync(self, manager: Any) -> None:
        """阶段2 注册记忆同步管理器。"""
        self._memory_sync = manager

    def register_credential_sync(self, cache: Any) -> None:
        """阶段3 注册凭据同步。"""
        self._credential_sync = cache

    def register_skill_sync(self, manager: Any) -> None:
        """阶段4 注册技能同步管理器。"""
        self._skill_sync = manager

    def register_mcp_sync(self, sync: Any) -> None:
        """阶段5 注册 MCP 同步。"""
        self._mcp_sync = sync
```

---

## 7. app lifespan 接入

**文件**：`src/qwenpaw/app/_app.py`

### 7.1 启动时初始化

在 `lifespan()` 的 `_background_startup()` 中，skill pool auto-update sync（第 642-653 行）之后追加：

```python
# ---- Sync layer init (pig integration) ----
try:
    from ..sync.lifecycle import SyncLifecycle
    sync_lc = SyncLifecycle()
    app.state.sync_lifecycle = sync_lc
    await sync_lc.on_startup()
except Exception:
    logger.warning("Sync layer init skipped", exc_info=True)
```

### 7.2 关闭时清理

在 `lifespan()` 的 `finally` 块（第 671-678 行）中，`_bg_task` 取消之后追加：

```python
# Flush sync layer on shutdown
sync_lc = getattr(app.state, "sync_lifecycle", None)
if sync_lc:
    await sync_lc.on_shutdown()
```

### 7.3 __init__.py 导出

**文件**：`src/qwenpaw/sync/__init__.py`

```python
# -*- coding: utf-8 -*-
"""pig 服务端同步层。

本地优先架构：所有读写优先走本地，同步层是后台旁路。
sync.enabled=false 时完全退化为基础本地模式。
"""
from .client import PigClient
from .auth_adapter import PigTokenProvider
from .lifecycle import SyncLifecycle
from .errors import (
    SyncError,
    SyncAuthError,
    SyncNetworkError,
    SyncServerError,
    SyncConfigError,
    SyncConflictError,
)

__all__ = [
    "PigClient",
    "PigTokenProvider",
    "SyncLifecycle",
    "SyncError",
    "SyncAuthError",
    "SyncNetworkError",
    "SyncServerError",
    "SyncConfigError",
    "SyncConflictError",
]
```

---

## 8. 验收标准

### 功能验收

- [ ] `config.yaml` 配置 `sync.enabled: true` 时，app 启动日志出现 `Sync layer initialized`
- [ ] `config.yaml` 不配置 `sync` 段或 `sync.enabled: false` 时，零行为变化
- [ ] `pig.base_url` 为空但 `enabled: true` 时，启动日志 warning 但不崩溃
- [ ] `PigClient` 能对 pig `/oauth2/jwks` 发起 GET 请求（即使返回 401）
- [ ] `PigTokenProvider.set_tokens()` 后 `pig_tokens.json` 生成在 `SECRET_DIR/`，权限 0600
- [ ] `PigTokenProvider.get_access_token()` 在 token 有效期内返回明文 token
- [ ] `PigTokenProvider.refresh()` 在无 refresh_token 时返回 False 且 `needs_relogin=True`
- [ ] app 关闭时 `PigClient.close()` 被调用，无资源泄漏

### 异常验收

- [ ] pig 不可达时，`PigClient.get()` 抛出 `SyncNetworkError`，不无限重试
- [ ] pig 返回 500 时，`PigClient.get()` 抛出 `SyncServerError`
- [ ] pig 返回 401 且 refresh 成功时，请求自动重试成功
- [ ] pig 返回 401 且 refresh 失败时，抛出 `SyncAuthError`
- [ ] 任何同步异常不传播到 app 主流程

### 配置验收

- [ ] `QWENPAW_SYNC_ENABLED=true` 环境变量可覆盖 config.yaml 的 `sync.enabled`
- [ ] `SyncConfig` 各子配置的默认值合理（见 2.1 节 Field default）
- [ ] `client_secret` 在 config.yaml 中以 `ENC:` 前缀存储，运行时透明解密

---

*文档创建日期：2026-07-30*
*配套文档：[architecture.md](./architecture.md) · [implementation-plan.md](./implementation-plan.md)*
