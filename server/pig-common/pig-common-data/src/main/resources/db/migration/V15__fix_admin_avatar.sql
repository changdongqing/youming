-- V15: 修复 admin 用户头像路径
-- 原种子数据(V2)中 avatar 引用了不存在的文件路径 /admin/sys-file/local/xxx.png
-- 后端 SysFileController 仅有 /sys-file/local/file/{fileName} 和 /sys-file/oss/file?fileName= 两个路由
-- /sys-file/local/{fileName} 不存在，导致前端图片请求返回 424 (Failed Dependency)
-- 且该文件名在 sys_file 表中无对应记录，清空 avatar 字段即可
UPDATE sys_user SET avatar = '' WHERE user_id = 1 AND avatar LIKE '/admin/sys-file/local/%';
