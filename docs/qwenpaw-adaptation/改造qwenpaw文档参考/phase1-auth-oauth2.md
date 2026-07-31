# 阶段 1 详细设计：授权对接 pig OAuth2

> **配套文档**：[architecture.md](./architecture.md) · [implementation-plan.md](./implementation-plan.md) · [phase0-infrastructure.md](./phase0-infrastructure.md)
>
> **目标**：用户可通过 pig SSO 登录 QwenPaw，pig 签发的 JWT 可本地验签，保留本地 auth 作为离线 fallback。
>
> **预估工期**：3-5 天 · **依赖**：阶段 0

---

## 目录

- [1. 整体流程](#1-整体流程)
- [2. 后端：JWT 本地验签](#2-后端jwt-本地验签)
- [3. 后端：OAuth2 授权码路由](#3-后端oauth2-授权码路由)
- [4. 后端：auth status 扩展](#4-后端auth-status-扩展)
- [5. 后端：AuthMiddleware 改造](#5-后端authmiddleware-改造)
- [6. 前端：登录页适配](#6-前端登录页适配)
- [7. 前端：API 层适配](#7-前端api-层适配)
- [8. pig 服务端配置要求](#8-pig-服务端配置要求)
- [9. 验收标准](#9-验收标准)

---

## 1. 整体流程

### 1.1 pig 模式登录流程（OAuth2 授权码模式）

```
用户              QwenPaw前端         QwenPaw后端          pig-auth
 │                   │                   │                   │
 │  访问应用          │                   │                   │
 │──────────────────►│                   │                   │
 │                   │ GET /auth/status  │                   │
 │                   │──────────────────►│                   │
 │                   │  {auth_mode:"pig",│                   │
 │                   │   pig_authorize_url}                   │
 │                   │◄──────────────────│                   │
 │  显示"pig登录"按钮 │                   │                   │
 │◄──────────────────│                   │                   │
 │  点击登录          │                   │                   │
 │──────────────────►│                   │                   │
 │                   │ 跳转 authorize_url│                   │
 │                   │──────────────────────────────────────►│
 │                   │                   │  pig SSO 登录页    │
 │  输入账密          │                   │◄──────────────────│
 │──────────────────────────────────────────────────────────►│
 │                   │                   │  回调带 code       │
 │                   │  /api/sync/oauth/callback?code=xxx     │
 │                   │──────────────────►│                   │
 │                   │                   │  code 换 token     │
 │                   │                   │──────────────────►│
 │                   │                   │  access_token      │
 │                   │                   │  refresh_token     │
 │                   │                   │◄──────────────────│
 │                   │                   │ 持久化 token       │
 │                   │  重定向前端带token │                   │
 │                   │◄──────────────────│                   │
 │  进入应用          │                   │                   │
 │◄──────────────────│                   │                   │
 │                   │                   │                   │
 │  后续API请求       │  Bearer <jwt>     │                   │
 │──────────────────►│──────────────────►│                   │
 │                   │                   │ 本地JWKS验签       │
 │                   │                   │   (不请求pig)      │
 │                   │  200 OK           │                   │
 │                   │◄──────────────────│                   │
```

### 1.2 本地模式（fallback）

当 `auth_mode=local` 或 pig 不可达时，完全使用现有 `auth.py` 的 HMAC token 流程，**零改动**。

---

## 2. 后端：JWT 本地验签

### 2.1 JWKS 公钥缓存

**文件**：`src/qwenpaw/app/auth.py`

pig 基于 Spring Authorization Server，JWT 使用 RS256 签名，公钥通过 JWKS 端点发布。本地验签需要缓存公钥。

新增 JWKS 缓存管理（参考现有 `_get_config_cached` 的 mtime 缓存模式）：

```python
# auth.py 新增

import asyncio
from typing import Any

# JWKS 公钥缓存
_jwks_cache: dict[str, Any] = {"keys": None, "fetched_at": 0.0}
_JWKS_TTL = 3600  # 公钥缓存 1 小时
_jwks_lock = asyncio.Lock()


async def _fetch_pig_jwks_async() -> dict | None:
    """从 pig jwks_endpoint 拉取公钥集，带 TTL 缓存。

    使用 asyncio.Lock 防止并发重复拉取。
    失败时返回上次缓存的值（如果有），否则 None。
    """
    import time as _time

    now = _time.time()
    if (
        _jwks_cache["keys"] is not None
        and now - _jwks_cache["fetched_at"] < _JWKS_TTL
    ):
        return _jwks_cache["keys"]

    async with _jwks_lock:
        # double-check
        now = _time.time()
        if (
            _jw_cache["keys"] is not None
            and now - _jwks_cache["fetched_at"] < _JWKS_TTL
        ):
            return _jwks_cache["keys"]

        try:
            from ..config import load_config
            import httpx

            cfg = load_config()
            if not cfg.sync.enabled or cfg.sync.auth_mode != "pig":
                return None

            jwks_url = (
                cfg.sync.pig.base_url.rstrip("/")
                + cfg.sync.pig.jwks_endpoint
            )
            async with httpx.AsyncClient(
                timeout=httpx.Timeout(10.0),
            ) as client:
                resp = await client.get(jwks_url)
                if resp.status_code == 200:
                    keys = resp.json()
                    _jwks_cache["keys"] = keys
                    _jwks_cache["fetched_at"] = now
                    logger.info("pig JWKS fetched and cached")
                    return keys
                else:
                    logger.warning(
                        "pig JWKS fetch failed: %d",
                        resp.status_code,
                    )
        except Exception as exc:
            logger.warning("pig JWKS fetch error: %s", exc)

        # 返回过期缓存（降级）
        return _jwks_cache["keys"]
```

### 2.2 JWT 验签函数

```python
# auth.py 新增

def _verify_pig_jwt_sync(token: str) -> str | None:
    """同步验签 pig JWT（使用缓存的 JWKS 公钥）。

    如果 JWKS 缓存为空（首次启动或 pig 不可达），返回 None。
    调用方（AuthMiddleware）应在验签失败时尝试异步刷新 JWKS。

    Returns:
        username (sub claim) 或 None。
    """
    import base64
    import json

    keys_data = _jwks_cache.get("keys")
    if not keys_data or not keys_data.get("keys"):
        return None

    try:
        # 解码 JWT header 获取 kid
        parts = token.split(".")
        if len(parts) != 3:
            return None
        header_b64 = parts[0]
        # 补 padding
        padding = 4 - len(header_b64) % 4
        if padding != 4:
            header_b64 += "=" * padding
        header = json.loads(
            base64.urlsafe_b64decode(header_b64),
        )
        kid = header.get("kid")

        # 查找匹配的公钥
        signing_key = None
        for key in keys_data["keys"]:
            if key.get("kid") == kid:
                signing_key = key
                break
        if not signing_key:
            # kid 不匹配，可能 JWKS 已更新
            logger.debug(
                "pig JWT kid %s not found in cached JWKS",
                kid,
            )
            return None

        # 验证签名（RS256）
        # 使用 cryptography 库（项目已依赖）
        from cryptography.hazmat.primitives.asymmetric import rsa
        from cryptography.hazmat.primitives import serialization
        import jwt as pyjwt  # 需要 PyJWT

        # 从 JWK 构建 public key
        public_key = pyjwt.PyJWK(signing_key).key

        # 验证 token
        payload = pyjwt.decode(
            token,
            public_key,
            algorithms=["RS256"],
            options={"verify_aud": False},
        )

        # 检查过期
        exp = payload.get("exp", 0)
        if exp < time.time():
            return None

        return payload.get("sub") or payload.get("user_name")

    except Exception as exc:
        logger.debug("pig JWT verification failed: %s", exc)
        return None


async def _verify_pig_jwt_async(token: str) -> str | None:
    """异步验签 pig JWT。

    先尝试用缓存 JWKS 验签，失败时刷新 JWKS 后重试一次。
    """
    # 第一次尝试（用缓存）
    result = _verify_pig_jwt_sync(token)
    if result is not None:
        return result

    # 缓存未命中或 kid 不匹配，刷新 JWKS
    await _fetch_pig_jwks_async()

    # 第二次尝试
    return _verify_pig_jwt_sync(token)
```

### 2.3 依赖说明

JWT 验签需要 `PyJWT` 库。在 `pyproject.toml` 的 `dependencies` 中新增：

```toml
"PyJWT>=2.8.0",
```

`PyJWT` 自带 `cryptography` 支持（项目已有 `cryptography`），可直接处理 RS256 + JWK。

### 2.4 模式判断

```python
# auth.py 新增

def is_pig_auth_mode() -> bool:
    """检查是否启用了 pig 认证模式。

    True 当且仅当 sync.enabled=true 且 sync.auth_mode="pig"。
    """
    try:
        from ..config import load_config
        cfg = load_config()
        return cfg.sync.enabled and cfg.sync.auth_mode == "pig"
    except Exception:
        return False
```

---

## 3. 后端：OAuth2 授权码路由

### 3.1 路由定义

**新建文件**：`src/qwenpaw/app/routers/sync_auth.py`

```python
# -*- coding: utf-8 -*-
"""pig OAuth2 授权码流程路由。

- /oauth/authorize: 重定向到 pig 授权页
- /oauth/callback: 授权码回调，换 token 后重定向前端
- /oauth/logout: 注销 pig token
- /oauth/status: 检查 pig token 状态
"""
from __future__ import annotations

import logging
import secrets
from urllib.parse import urlencode

import httpx
from fastapi import APIRouter, HTTPException, Request
from fastapi.responses import RedirectResponse
from pydantic import BaseModel

from ...config import load_config
from ...constant import SECRET_DIR

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/sync/oauth", tags=["sync-auth"])

# 授权码流程的 state 参数缓存（防 CSRF）
# 单用户场景下用内存即可；state 有效期 10 分钟
_pending_states: dict[str, float] = {}
_STATE_TTL = 600


def _get_token_provider():
    """从 app.state 获取 PigTokenProvider。"""
    from .._app import get_app
    # 实际实现中通过 request.app.state 获取
    ...


@router.get("/authorize")
async def oauth_authorize(request: Request):
    """重定向到 pig 授权页。

    生成 state 参数防 CSRF，记录到内存。
    """
    cfg = load_config()
    if not cfg.sync.enabled or cfg.sync.auth_mode != "pig":
        raise HTTPException(403, "pig auth mode is not enabled")

    pig = cfg.sync.pig
    state = secrets.token_urlsafe(32)
    import time
    _pending_states[state] = time.time()

    params = {
        "response_type": "code",
        "client_id": pig.client_id,
        "redirect_uri": pig.redirect_uri,
        "scope": pig.scopes,
        "state": state,
    }
    authorize_url = (
        pig.base_url.rstrip("/")
        + pig.authorize_endpoint
        + "?"
        + urlencode(params)
    )
    return RedirectResponse(url=authorize_url)


@router.get("/callback")
async def oauth_callback(
    code: str,
    state: str,
    request: Request,
    error: str | None = None,
):
    """pig OAuth2 授权码回调。

    1. 验证 state（防 CSRF）
    2. 用 code 换 access_token + refresh_token
    3. 持久化 token 到 PigTokenProvider
    4. 重定向到前端，带 token 参数
    """
    import time

    # 错误回调（用户拒绝授权等）
    if error:
        frontend_url = _frontend_url(request)
        return RedirectResponse(
            url=f"{frontend_url}?oauth_error={error}",
        )

    # 验证 state
    created_at = _pending_states.pop(state, 0)
    if not created_at or time.time() - created_at > _STATE_TTL:
        raise HTTPException(400, "Invalid or expired OAuth2 state")

    cfg = load_config()
    pig = cfg.sync.pig

    # 用 code 换 token
    try:
        async with httpx.AsyncClient(
            timeout=httpx.Timeout(15.0),
        ) as client:
            resp = await client.post(
                pig.base_url.rstrip("/") + pig.token_endpoint,
                data={
                    "grant_type": "authorization_code",
                    "code": code,
                    "redirect_uri": pig.redirect_uri,
                    "client_id": pig.client_id,
                    "client_secret": _decrypt_secret(pig.client_secret),
                },
                headers={
                    "Content-Type": "application/x-www-form-urlencoded",
                },
            )
    except (httpx.ConnectError, httpx.TimeoutException) as exc:
        raise HTTPException(502, f"pig unreachable: {exc}") from exc

    if resp.status_code != 200:
        logger.error(
            "pig token exchange failed: %d %s",
            resp.status_code,
            resp.text,
        )
        raise HTTPException(
            502,
            f"pig token exchange failed: {resp.status_code}",
        )

    token_data = resp.json()
    access_token = token_data.get("access_token", "")
    refresh_token = token_data.get("refresh_token", "")
    expires_in = int(token_data.get("expires_in", 3600))

    if not access_token:
        raise HTTPException(502, "pig did not return access_token")

    # 持久化 token
    sync_lc = getattr(request.app.state, "sync_lifecycle", None)
    if sync_lc and sync_lc.token_provider:
        sync_lc.token_provider.set_tokens(
            access_token, refresh_token, expires_in,
        )

    # 触发登录后同步拉取（阶段 0 的 on_login_success）
    if sync_lc:
        import asyncio
        # 从 JWT 中提取 username（sub claim）
        username = _extract_sub_from_jwt(access_token) or "pig_user"
        asyncio.create_task(sync_lc.on_login_success(username))

    # 重定向到前端，带 token
    frontend_url = _frontend_url(request)
    return RedirectResponse(
        url=f"{frontend_url}?oauth_token={access_token}",
    )


@router.post("/logout")
async def oauth_logout(request: Request):
    """注销：清除本地 pig token。

    可选：调用 pig 的 token revocation 端点。
    """
    sync_lc = getattr(request.app.state, "sync_lifecycle", None)
    if sync_lc and sync_lc.token_provider:
        sync_lc.token_provider.clear()
    return {"message": "Logged out from pig"}


@router.get("/status")
async def oauth_status(request: Request):
    """检查 pig token 状态。"""
    sync_lc = getattr(request.app.state, "sync_lifecycle", None)
    if not sync_lc or not sync_lc.token_provider:
        return {"authenticated": False, "needs_relogin": True}

    tp = sync_lc.token_provider
    return {
        "authenticated": bool(tp._tokens.get("access_token")),
        "needs_relogin": tp.needs_relogin,
    }


# --- 辅助函数 ---

def _frontend_url(request: Request) -> str:
    """推断前端 URL（用于 OAuth2 回调重定向）。

    优先从配置读取，否则从请求推断。
    """
    cfg = load_config()
    # 可增加 sync.pig.frontend_url 配置项
    # 这里从请求头推断
    host = request.headers.get("host", "localhost:8000")
    scheme = request.headers.get("x-forwarded-proto", "http")
    return f"{scheme}://{host}/login"


def _decrypt_secret(value: str) -> str:
    """解密 client_secret（如果加密存储）。"""
    from ...security.secret_store import decrypt, is_encrypted
    if value and is_encrypted(value):
        return decrypt(value)
    return value


def _extract_sub_from_jwt(token: str) -> str | None:
    """从 JWT 中提取 sub claim（不验签，仅解码）。"""
    import base64
    import json
    try:
        parts = token.split(".")
        if len(parts) != 3:
            return None
        payload_b64 = parts[1]
        padding = 4 - len(payload_b64) % 4
        if padding != 4:
            payload_b64 += "=" * padding
        payload = json.loads(base64.urlsafe_b64decode(payload_b64))
        return payload.get("sub") or payload.get("user_name")
    except Exception:
        return None
```

### 3.2 路由注册

**文件**：`src/qwenpaw/app/_app.py`

在 router 注册区域追加：

```python
from .routers.sync_auth import router as sync_auth_router
# ...
app.include_router(sync_auth_router)
```

### 3.3 公开路径放行

**文件**：`src/qwenpaw/app/auth.py`

在 `_PUBLIC_PATHS`（第 53 行）中追加 OAuth2 回调路径：

```python
_PUBLIC_PATHS: frozenset[str] = frozenset(
    {
        "/api/auth/login",
        "/api/auth/status",
        "/api/auth/register",
        "/api/desktop/shutdown",
        "/api/version",
        "/api/settings/language",
        "/api/settings/upload-limit",
        "/api/frontend_plugin",
        # 新增：pig OAuth2 回调（无需认证即可访问）
        "/api/sync/oauth/authorize",
        "/api/sync/oauth/callback",
        "/api/sync/oauth/status",
    },
)
```

> `/api/sync/oauth/logout` 需要认证，不放行。

---

## 4. 后端：auth status 扩展

### 4.1 响应模型扩展

**文件**：`src/qwenpaw/app/routers/auth.py`

```python
class AuthStatusResponse(BaseModel):
    enabled: bool
    has_users: bool
    auth_mode: str = "local"  # "local" | "pig"
    pig_authorize_url: str = ""  # pig 模式下的授权页跳转 URL
    pig_authenticated: bool = False  # pig 模式下是否已登录
```

### 4.2 status 端点改造

```python
@router.get("/status")
async def auth_status(request: Request):
    """检查认证状态。"""
    from ..auth import is_pig_auth_mode, has_registered_users, is_auth_enabled

    if is_pig_auth_mode():
        # pig 模式
        sync_lc = getattr(request.app.state, "sync_lifecycle", None)
        pig_authenticated = False
        needs_relogin = True
        if sync_lc and sync_lc.token_provider:
            tp = sync_lc.token_provider
            pig_authenticated = bool(tp._tokens.get("access_token"))
            needs_relogin = tp.needs_relogin

        # 构造授权 URL
        from ...config import load_config
        from urllib.parse import urlencode
        import secrets
        import time

        cfg = load_config()
        pig = cfg.sync.pig
        state = secrets.token_urlsafe(32)
        _pending_states_external = request.app.state.setdefault(
            "_oauth_states", {},
        )
        _pending_states_external[state] = time.time()

        authorize_url = (
            pig.base_url.rstrip("/")
            + pig.authorize_endpoint
            + "?"
            + urlencode({
                "response_type": "code",
                "client_id": pig.client_id,
                "redirect_uri": pig.redirect_uri,
                "scope": pig.scopes,
                "state": state,
            })
        )

        return AuthStatusResponse(
            enabled=True,
            has_users=pig_authenticated,
            auth_mode="pig",
            pig_authorize_url=authorize_url,
            pig_authenticated=pig_authenticated and not needs_relogin,
        )

    # 本地模式（现有逻辑）
    return AuthStatusResponse(
        enabled=is_auth_enabled(),
        has_users=has_registered_users(),
        auth_mode="local",
    )
```

### 4.3 verify 端点适配

```python
@router.get("/verify")
async def verify(request: Request):
    """验证当前 token 是否有效。"""
    from ..auth import is_pig_auth_mode, verify_token

    if is_pig_auth_mode():
        auth_header = request.headers.get("Authorization", "")
        token = auth_header[7:] if auth_header.startswith("Bearer ") else ""
        if not token:
            raise HTTPException(401, "No token provided")

        # pig 模式：异步验签
        from ..auth import _verify_pig_jwt_async
        username = await _verify_pig_jwt_async(token)
        if username is None:
            raise HTTPException(401, "Invalid or expired token")
        return {"valid": True, "username": username}

    # 本地模式（现有逻辑）
    if not is_auth_enabled():
        return {"valid": True, "username": ""}
    auth_header = request.headers.get("Authorization", "")
    token = auth_header[7:] if auth_header.startswith("Bearer ") else ""
    if not token:
        raise HTTPException(401, "No token provided")
    username = verify_token(token)
    if username is None:
        raise HTTPException(401, "Invalid or expired token")
    return {"valid": True, "username": username}
```

---

## 5. 后端：AuthMiddleware 改造

### 5.1 dispatch 方法改造

**文件**：`src/qwenpaw/app/auth.py`

`AuthMiddleware.dispatch()`（第 693 行）需要支持异步 JWT 验签：

```python
class AuthMiddleware(BaseHTTPMiddleware):
    """Middleware that checks Bearer token on protected routes.

    支持两种模式：
    - local: 本地 HMAC token 验签（同步）
    - pig: pig JWT 验签（异步，使用缓存的 JWKS 公钥）
    """

    async def dispatch(self, request: Request, call_next):
        if self._should_skip_auth(request):
            return await call_next(request)

        token = self._extract_token(request)
        if not token:
            return Response(
                content='{"detail":"Not authenticated"}',
                status_code=401,
                media_type="application/json",
            )

        # 根据模式选择验签方式
        if is_pig_auth_mode():
            user = await _verify_pig_jwt_async(token)
        else:
            user = verify_token(token)

        if user is None:
            return Response(
                content='{"detail":"Invalid or expired token"}',
                status_code=401,
                media_type="application/json",
            )

        request.state.user = user
        return await call_next(request)
```

### 5.2 _should_skip_auth 适配

现有 `_should_skip_auth`（第 717 行）在 pig 模式下的行为：

```python
@staticmethod
def _should_skip_auth(request: Request) -> bool:
    # pig 模式下也检查公开路径
    if is_pig_auth_mode():
        path = request.url.path
        if (
            request.method == "OPTIONS"
            or path in _PUBLIC_PATHS
            or any(path.startswith(p) for p in _PUBLIC_PREFIXES)
            or not path.startswith("/api/")
        ):
            return True
        return False  # pig 模式下所有 /api/ 路径都需要认证

    # 本地模式（现有逻辑不变）
    if not is_auth_enabled() or not has_registered_users():
        return True
    # ... 现有逻辑 ...
```

> pig 模式下不使用 `allow_no_auth_hosts`（localhost 免认证），因为 pig 管理用户身份。如需保留 localhost 免认证，可在 pig 模式下也检查此配置。

### 5.3 JWKS 后台预热

**文件**：`src/qwenpaw/app/_app.py`

在 `_background_startup()` 中，sync layer init 之后追加 JWKS 预热：

```python
# pig 模式下预热 JWKS 公钥
if sync_lc and sync_lc.is_enabled:
    try:
        from ..config import load_config
        cfg = load_config()
        if cfg.sync.auth_mode == "pig":
            from .auth import _fetch_pig_jwks_async
            await _fetch_pig_jwks_async()
    except Exception:
        logger.debug("JWKS prefetch skipped", exc_info=True)
```

---

## 6. 前端：登录页适配

### 6.1 Login 页面改造

**文件**：`console/src/pages/Login/index.tsx`

```tsx
import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Button, Form, Input, Spin } from "antd";
import { useAppMessage } from "../../hooks/useAppMessage";
import { LockOutlined, UserOutlined } from "@ant-design/icons";
import { authApi } from "../../api/modules/auth";
import { setAuthToken } from "../../api/config";
import { useTheme } from "../../contexts/ThemeContext";

interface AuthStatus {
  enabled: boolean;
  has_users: boolean;
  auth_mode: string;     // "local" | "pig"
  pig_authorize_url: string;
  pig_authenticated: boolean;
}

export default function LoginPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { isDark } = useTheme();
  const [loading, setLoading] = useState(false);
  const [isRegister, setIsRegister] = useState(false);
  const [hasUsers, setHasUsers] = useState(true);
  const [authMode, setAuthMode] = useState<string>("local");
  const [pigAuthorizeUrl, setPigAuthorizeUrl] = useState<string>("");
  const { message } = useAppMessage();

  // 检查 OAuth2 回调 token
  useEffect(() => {
    const oauthToken = searchParams.get("oauth_token");
    const oauthError = searchParams.get("oauth_error");
    if (oauthToken) {
      setAuthToken(oauthToken);
      // 清除 URL 参数
      navigate("/chat", { replace: true });
      return;
    }
    if (oauthError) {
      message.error(`pig 登录失败: ${oauthError}`);
    }
  }, [searchParams, navigate, message]);

  // 检查认证状态
  useEffect(() => {
    authApi
      .getStatus()
      .then((res: AuthStatus) => {
        if (!res.enabled) {
          navigate("/chat", { replace: true });
          return;
        }
        setAuthMode(res.auth_mode);
        setHasUsers(res.has_users);
        setPigAuthorizeUrl(res.pig_authorize_url);

        // pig 模式下已认证，直接进入
        if (res.auth_mode === "pig" && res.pig_authenticated) {
          navigate("/chat", { replace: true });
          return;
        }
        // 本地模式无用户，进入注册
        if (res.auth_mode === "local" && !res.has_users) {
          setIsRegister(true);
        }
      })
      .catch(() => {});
  }, [navigate]);

  // pig 登录：跳转授权页
  const handlePigLogin = () => {
    if (pigAuthorizeUrl) {
      window.location.href = pigAuthorizeUrl;
    }
  };

  // 本地登录（现有逻辑）
  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true);
    try {
      const raw = searchParams.get("redirect") || "/chat";
      const redirect =
        raw.startsWith("/") && !raw.startsWith("//") ? raw : "/chat";

      if (isRegister) {
        const res = await authApi.register(values.username, values.password);
        if (res.token) {
          setAuthToken(res.token);
          message.success(t("login.registerSuccess"));
          navigate(redirect, { replace: true });
        }
      } else {
        const res = await authApi.login(values.username, values.password);
        if (res.token) {
          setAuthToken(res.token);
          navigate(redirect, { replace: true });
        }
      }
    } catch (err) {
      message.error(err instanceof Error ? err.message : t("login.failed"));
    } finally {
      setLoading(false);
    }
  };

  // pig 模式：显示 SSO 登录按钮
  if (authMode === "pig") {
    return (
      <div style={{
        height: "100vh", display: "flex",
        alignItems: "center", justifyContent: "center",
        background: isDark
          ? "linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%)"
          : "linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)",
      }}>
        <div style={{
          width: 400, padding: 32, borderRadius: 12,
          background: isDark ? "#1f1f1f" : "#fff",
          boxShadow: isDark
            ? "0 4px 24px rgba(0,0,0,0.4)"
            : "0 4px 24px rgba(0,0,0,0.1)",
          textAlign: "center",
        }}>
          <img
            src={isDark ? "/logo-dark.svg" : "/logo-light.svg"}
            alt="QwenPaw"
            style={{ height: 48, marginBottom: 12 }}
          />
          <h2 style={{ margin: 0, fontWeight: 600, fontSize: 20, marginBottom: 24 }}>
            {t("login.title")}
          </h2>
          <Button
            type="primary"
            onClick={handlePigLogin}
            loading={loading}
            block
            style={{ height: 44, borderRadius: 8, fontWeight: 500 }}
          >
            使用 pig 登录
          </Button>
        </div>
      </div>
    );
  }

  // 本地模式：现有表单（不变）
  return (
    // ... 现有 JSX 完全保留 ...
  );
}
```

### 6.2 全局 token 失效处理

**文件**：`console/src/api/authHeaders.ts` 或 API 拦截层

在 API 响应拦截中，401 时检查是否 pig 模式并触发重新登录：

```tsx
// console/src/api/index.ts 或统一的 fetch 封装中
async function apiFetch(url: string, options: RequestInit) {
  const response = await fetch(url, options);
  if (response.status === 401) {
    // 清除失效 token
    setAuthToken("");
    // 跳转登录页
    window.location.href = "/login";
  }
  return response;
}
```

> 实施时检查现有 API 层是否已有 401 拦截逻辑，避免重复。

---

## 7. 前端：API 层适配

### 7.1 auth API 类型扩展

**文件**：`console/src/api/modules/auth.ts`

```typescript
export interface AuthStatusResponse {
  enabled: boolean;
  has_users: boolean;
  auth_mode?: string;         // "local" | "pig"
  pig_authorize_url?: string;
  pig_authenticated?: boolean;
}

export const authApi = {
  // 现有方法不变...

  getStatus: async (): Promise<AuthStatusResponse> => {
    const res = await fetch(getApiUrl("/auth/status"));
    if (!res.ok) throw new Error("Failed to check auth status");
    return res.json();
  },

  // 新增：pig 注销
  pigLogout: async (): Promise<void> => {
    const headers = buildAuthHeaders();
    const res = await fetch(getApiUrl("/sync/oauth/logout"), {
      method: "POST",
      headers,
    });
    if (!res.ok) throw new Error("pig logout failed");
  },
};
```

---

## 8. pig 服务端配置要求

### 8.1 OAuth2 客户端注册

在 pig 管理后台注册 OAuth2 客户端：

| 配置项 | 值 |
|--------|-----|
| client_id | `qwenpaw` |
| client_secret | （生成后填入 config.yaml，加密存储） |
| grant_types | `authorization_code`, `refresh_token` |
| redirect_uri | `http://localhost:8000/api/sync/oauth/callback`（按实际部署调整） |
| scopes | `openid profile` |
| token_format | JWT (RS256) |
| access_token有效期 | 3600s（1小时） |
| refresh_token有效期 | 604800s（7天） |

### 8.2 pig 需暴露的端点

| 端点 | 说明 | Spring Authorization Server 自带 |
|------|------|------|
| `/oauth2/authorize` | 授权页 | ✅ |
| `/oauth2/token` | 令牌端点 | ✅ |
| `/oauth2/jwks` | JWKS 公钥 | ✅ |
| `/oauth2/introspect` | 令牌内省（可选） | ✅ |

> Spring Authorization Server 默认提供以上端点，无需额外开发。

### 8.3 JWT Claims 约定

pig 签发的 JWT 需包含以下 claim：

| claim | 说明 |
|-------|------|
| `sub` | 用户标识（用户名或用户ID） |
| `exp` | 过期时间戳 |
| `iat` | 签发时间戳 |
| `iss` | 签发方（pig base_url） |

> Spring Authorization Server 默认包含这些 claim。`user_name` claim 也可作为用户名来源（代码中已兼容）。

---

## 9. 验收标准

### 功能验收

- [ ] `auth_mode=pig` 时，前端登录页显示"使用 pig 登录"按钮
- [ ] 点击按钮跳转 pig SSO 登录页
- [ ] pig 登录成功后回调 QwenPaw，URL 带 `oauth_token` 参数
- [ ] 前端获取 token 存入 localStorage，跳转 `/chat`
- [ ] 后续 API 请求带 `Bearer <jwt>`，`AuthMiddleware` 验签通过
- [ ] JWKS 公钥缓存 1 小时，过期后自动刷新
- [ ] kid 不匹配时自动刷新 JWKS 重试验签
- [ ] JWT 过期时返回 401，前端跳转登录页
- [ ] pig 注销后 token 清除，再次访问需重新登录

### Fallback 验收

- [ ] `auth_mode=local` 时登录流程与当前完全一致
- [ ] `sync.enabled=false` 时所有行为不变
- [ ] pig 不可达时，本地模式仍可正常使用
- [ ] pig 不可达时 JWKS 缓存为空，JWT 验签返回 None（不崩溃）

### 安全验收

- [ ] OAuth2 state 参数防 CSRF，10 分钟过期
- [ ] client_secret 在 config.yaml 中 ENC: 加密存储
- [ ] pig_tokens.json 权限 0600
- [ ] 回调重定向只允许前端 URL（不开放重定向）

### 前端验收

- [ ] pig 模式登录页样式与本地模式一致
- [ ] OAuth2 回调错误（用户拒绝授权）有友好提示
- [ ] 401 响应自动跳转登录页
- [ ] pig 模式下已认证用户直接进入应用

---

*文档创建日期：2026-07-30*
*配套文档：[architecture.md](./architecture.md) · [implementation-plan.md](./implementation-plan.md) · [phase0-infrastructure.md](./phase0-infrastructure.md)*
