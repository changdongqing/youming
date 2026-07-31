-- ============================================================
-- V23__qwenpaw_client_seed.sql
-- QwenPaw 客户端 OAuth2 注册（sys_oauth_client_details 种子）
-- 对应《qwenpaw-adaptation.md》§二 OAuth2 认证配置
-- grant_types: authorization_code,refresh_token
-- token 格式: opaque（pig-auth 默认 OAuth2TokenFormat.REFERENCE，非 JWT）
-- ============================================================

-- ---------- sys_oauth_client_details 种子（17 字段按位置 INSERT） ----------
-- 字段顺序：id, client_id, resource_ids, client_secret, scope,
--           authorized_grant_types, web_server_redirect_uri, authorities,
--           access_token_validity, refresh_token_validity, additional_information,
--           autoapprove, del_flag, create_by, update_by, create_time, update_time
-- client_secret 明文存储，运行时由 PigRemoteRegisteredClientRepository 加 {noop} 前缀
INSERT INTO sys_oauth_client_details VALUES (
    9,
    'qwenpaw',
    NULL,
    'qwenpaw',
    'server',
    'authorization_code,refresh_token',
    'http://localhost:8000/api/sync/oauth/callback',
    NULL,
    3600,
    604800,
    '{"enc_flag":"0","captcha_flag":"0"}',
    'true',
    '0',
    'admin',
    ' ',
    NULL,
    NULL
);
