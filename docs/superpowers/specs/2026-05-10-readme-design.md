# README.md 设计规格文档

> 本文档定义 OAISS CHAIN 项目 README.md 的完整结构和内容

---

## 文档元数据

- **创建日期**: 2026-05-10
- **目标受众**: 开发者
- **内容深度**: 全面详细
- **语言**: 简体中文

---

## 设计决策

### 选择方案 A：经典技术文档结构

**理由**：
- 开发者受众需要清晰的技术信息层级
- 经典结构便于快速定位所需信息
- 适合作为项目主入口文档

### 内容来源

基于项目代码探索获取的真实数据：
- 16 个 REST 控制器
- 19 个业务服务
- 22 个 JPA 实体
- 21 张数据库表
- 17 个前端 API 模块
- 27+ Vue 页面组件

---

## 文档结构

### 1. 项目概述

**项目简介**：OAISS CHAIN 是基于区块链的碳资产数字化管理平台。

**核心特性**：
- 碳核算管理
- 碳交易系统（P2P + 双向拍卖）
- 信用评分体系
- 碳币体系
- 碳中和项目
- 数字签名
- 区块链集成
- 多角色权限

---

### 2. 技术栈

**后端技术表**：
- Java 17
- Spring Boot 3.2.5
- Spring Security
- Spring Data JPA
- MySQL 8.0
- Redis 7.0
- MinIO
- JWT (jjwt 0.12.5)
- Flyway
- SpringDoc OpenAPI 2.5.0
- MapStruct 1.5.5
- Testcontainers 1.19.7
- JaCoCo 0.8.11 (90%+)

**前端技术表**：
- Vue 3.5.32
- TypeScript 6.0.3
- Vite 8.0.10
- Element Plus 2.13.7
- Pinia 3.0.4
- Vue Router 5.0.6
- ECharts 6.0.0
- vue-i18n 11.4.0
- Vitest 4.1.5
- Playwright 1.59.1

**基础设施表**：
- MySQL (3306)
- Redis (6379)
- MinIO (9000/9001)
- Backend (8080)
- Frontend (5173)

---

### 3. 项目结构

展示后端目录结构：
- controller/
- service/
- repository/
- entity/
- dto/
- config/
- annotation/
- aop/
- security/
- enums/
- constant/
- exception/
- util/

展示前端目录结构：
- api/
- views/ (enterprise, admin, auditor, authenticator, third-party)
- store/
- router/
- i18n/
- components/
- layout/
- utils/

---

### 4. 快速开始

**环境要求**：
- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 7.0+
- Docker & Docker Compose

**方式一：Docker Compose（推荐）**
- 克隆项目
- 配置环境变量
- 启动服务
- 访问地址

**方式二：本地开发**
- 后端启动命令
- 前端启动命令

**运行测试**：
- 后端单元测试
- 后端集成测试
- 前端单元测试
- 前端 E2E 测试

---

### 5. 用户角色与权限

**角色表**：
- ENTERPRISE（企业）
- REVIEWER（审核员）
- AUTHENTICATOR（认证机构）
- ADMIN（管理员）
- THIRD_PARTY（第三方机构）

**权限控制**：
- 后端：@PreAuthorize 注解
- 前端：Vue Router meta.roles

---

### 6. 核心功能模块

详细描述 8 大业务模块：
1. 碳核算管理
2. 碳交易系统
3. 信用评分体系
4. 碳币体系
5. 碳中和项目
6. 数字签名
7. 区块链集成
8. 系统管理

---

### 7. API 概览

**基础信息**：
- Base URL: /api/v1
- 认证方式: JWT Bearer Token + CSRF Cookie
- 响应格式: ApiResponse<T>

**响应结构 JSON 示例**

**主要端点表**：
- 认证 /api/v1/auth/*
- 用户 /api/v1/users/*
- 碳核算 /api/v1/carbon/*
- 交易 /api/v1/trade/*
- 拍卖 /api/v1/auction/*
- 碳币 /api/v1/carbon-coin/*
- 信用 /api/v1/credit/*
- 项目 /api/v1/carbon-neutral/*
- 签名 /api/v1/signature/*
- 区块链 /api/v1/blockchain/*

---

### 8. 数据库设计

**核心表结构（21 张表）**：
- 用户体系（6 表）
- 碳核算（2 表）
- 碳交易（3 表）
- 信用体系（2 表）
- 碳币体系（2 表）
- 碳中和（1 表）
- 安全（1 表）
- 权限（2 表）
- 审核（1 表）
- 日志（1 表）

**数据库迁移**：Flyway 版本管理

---

### 9. 开发指南

**后端开发规范**：
- 代码格式化
- 静态分析
- 测试运行
- 覆盖率查看

**前端开发规范**：
- 类型检查
- 代码检查
- 单元测试
- E2E 测试

**Git 提交规范**：Conventional Commits

---

### 10. 许可证与联系方式

- 许可证声明
- 贡献指南
- 联系方式

---

## 实现注意事项

1. 使用 Markdown 标准语法
2. 表格对齐规范
3. 代码块指定语言
4. 链接使用相对路径
5. 适当使用 emoji 增强可读性
6. 保持简体中文表述准确

---

## 验收标准

- [ ] 所有技术版本号准确
- [ ] 所有路径和命令可执行
- [ ] 文档结构清晰，层级合理
- [ ] 代码示例语法正确
- [ ] 表格数据完整
