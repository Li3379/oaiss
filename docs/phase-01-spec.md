# Phase 1：需求定义

## 要做什么

把一个模糊需求拆成 **五个** 明确元素（前四项属于经典规格定义，**第五项是强制性的验收清单**）：

```text
1. 功能列表：产品到底要做什么？（列出全部功能）
2. 技术栈：语言、框架、数据库等
3. 产出形态：SPA？API？文档？
4. 质量标准：什么叫“足够好”（核心路径可用？错误处理到位？）
5. 验收清单：在什么条件下，任何人都可以说“这一轮可以停 / 可以交付”？
   - 每一条都必须可检查：优先使用脚本、grep 或简短人工测试
   - 禁止写成无法核验的空话，比如“看起来专业”；如果是主观项，必须定义验证方式，例如“与附图对比”
```

## 默认落盘文件

- 在 phase 1 结束前，必须创建或更新 **`tracks/phase-01-acceptance.md`**。
- 使用 Markdown task list，便于人和脚本同时检查，例如：

```markdown
# Acceptance checklist (locked in phase 1)

> Inner loop must not silently delete items; scope changes go back to phase 1/2 with trace.

## Structure / machine-checkable

- [ ] Deliverable is single-file HTML with root `<html lang="...">`
- [ ] ...

## Behavior / manual or scripted

- [ ] ...
```

- **闸门要求**：功能列表 **不能为空**，并且 **`tracks/phase-01-acceptance.md` 至少包含一行** `- [ ]` 或 `- [x]`。在进入 phase 2 前，这些项不要求全部完成；但在 phase 3 中结束某条轨道，以及在 phase 4 中最终交付时，**必须全部勾选完成**（详见 `phase-03-inner.md` 与 `verifiability.md` 的 R3）。

## 示例

用户说：“帮我做一个记账工具。”

完成 phase 1 后应得到：

```text
功能：录入收入/支出、月度统计、按分类筛选
技术栈：Next.js + SQLite + Tailwind
产出：单页 Web 应用
质量：核心流程可用、输入有校验、界面整洁
验收（节选，且必须落盘到 tracks/phase-01-acceptance.md）：
  - [ ] 页面存在 form id="txn-form"
  - [ ] 提交后，列表中出现一条金额匹配的新记录
  - [ ] ...
```

## 闸门

只有当规格已经写明、功能列表 **非空**，并且 **`tracks/phase-01-acceptance.md` 已存在且至少包含一条验收项** 时，才能进入 phase 2。否则不得进入下一阶段。
