---
status: complete
created: 2026-05-14
author: claude
source: UltraQA API Testing Cycle 1
---

# SPEC-007: MinIO端口配置不一致

## 问题描述

MinIO Docker容器将内部端口9000映射到宿主机端口9002，但后端应用配置连接localhost:9000，导致文件相关功能全部失败。

### 影响

- `/file/list` 返回1000 "获取文件列表失败"
- `/file/info`、`/file/exists`、`/file/presigned-url` 等端点均不可用
- 文件上传/下载功能完全不可用

### 根因分析

`docker-compose.yml` 中MinIO端口映射为 `9002:9000`，但 `application.yml` 中 MinIO endpoint 配置为 `http://localhost:9000`。

## 修复方案

修改 `docker-compose.yml` 或 `application.yml` 使端口一致：

**方案A**（推荐）：修改docker-compose.yml端口映射为 `9000:9000`
**方案B**：修改application.yml中minio.endpoint为 `http://localhost:9002`

## 变更文件

| 文件 | 变更 |
|------|------|
| `docker-compose.yml` 或 `application.yml` | 统一MinIO端口配置 |

## 验证结果

| 场景 | 修复前 | 修复后 |
|------|--------|--------|
| GET /file/list | 1000 连接失败 | 200 成功 |
