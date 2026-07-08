# PostgreSQL 镜像 (youming 业务库 + 种子数据)
# 对应原 db/Dockerfile (MySQL), 迁移到 PG 后替代
# 用法: docker compose -f docker-compose-boot.yml up -d
FROM postgres:18-alpine

LABEL maintainer="youming"

ENV TZ=Asia/Shanghai
ENV LANG=en_US.utf8

RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 初始化脚本 (容器首次启动时自动执行, 按文件名排序)
# 1. 业务表结构  2. Quartz 表  3. 种子数据
COPY ./postgresql/V1__init_schema.sql /docker-entrypoint-initdb.d/01-schema.sql
COPY ./postgresql/quartz/tables_postgres.sql /docker-entrypoint-initdb.d/02-quartz.sql
COPY ./postgresql/V1__init_data.sql /docker-entrypoint-initdb.d/03-data.sql
