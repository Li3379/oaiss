<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-05-11 | Updated: 2026-05-30 -->

# docs/ 文档目录说明

本目录用于存放 OAISS CHAIN 双碳链动平台的参考资料、规格说明、计划文档与验收文档。

## 内容结构

### 顶层文件

| 文件 | 说明 |
|------|------|
| `DATABASE_SCHEMA.md` | 数据库结构说明 |
| `execution.md` | 执行规则说明 |
| `FIX-SPEC.md` | 规格驱动修复说明 |
| `ONBOARDING.md` | 开发者入门指南 |
| `phase-01-spec.md` | Phase 1 规格定义 |
| `phase-02-prompts.md` | Phase 2 Prompt 方案设计 |
| `phase-03-inner.md` | Phase 3 内层迭代细则 |
| `phase-04-output.md` | Phase 4 输出与择优规则 |
| `product-specification.md` | 产品说明书 |
| `verifiability.md` | 可验证性规则 |

### 子目录

| 目录 | 说明 |
|------|------|
| `raw/` | 原始中文需求文档（5 份） |
| `specs/` | 技术规范文档，包括 AI 模块、区块链集成、碳核算模型、差距分析 |
| `superpowers/` | 带日期的设计文档，内部含 `plans/` 与 `specs/` 子目录（2026-05-03 至 2026-05-10） |

### `raw/` 原始需求

该目录保存中文原始项目文档：

1. `01-项目需求分析.md`：项目需求分析
2. `02-项目概要介绍.md`：项目概要说明
3. `03-项目详细方案.md`：项目详细方案
4. `04-碳核算模型介绍文档.md`：碳核算模型说明
5. `05-项目测试文档.md`：项目测试文档

### `specs/` 技术规范

| 文件 | 说明 |
|------|------|
| `AI-MODULE-SPEC.md` | AI 模块技术规范 |
| `BLOCKCHAIN-INTEGRATION-SPEC.md` | 区块链集成技术规范 |
| `CARBON-CALCULATION-SPEC.md` | 碳核算模型技术规范 |
| `GAP-ANALYSIS.md` | 需求与实现差距分析 |
| `README.md` | `specs` 目录索引 |

## 对 AI Agent 的要求

### 在本目录中工作时

这些文档主要作为参考材料使用。修改文档时，应保留 `raw/` 与 `tracks/` 目录中的中文原始内容，不要误覆盖为其他形式。

### 测试要求

本目录不包含可执行代码，无需运行单元测试，但应检查文档引用路径是否有效。

### 编写规范

- 使用 Markdown 格式
- 需要时可以补充图示
- 中文与英文内容尽量分文件或分明确段落维护

## 依赖关系

- **内部依赖**：引用项目中的实体、服务、控制器及其他代码实现
- **外部依赖**：无
