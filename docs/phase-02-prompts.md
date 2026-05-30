# Phase 2：外层方案设计

## 核心思想

**需求相同，表述方式不同。** 每一个 Prompt 都必须描述 **同一个产品**；变化的只是“怎么描述”，而不是“做什么”。

## “不同表述”的五个维度

从下列维度中选择 **3-4** 个；每个维度产出 **一个** Prompt 方案，或者按你的计划分配。

**维度 1：抽象层级**

```text
L1 单行：      "Build an expense tracker"
L2 带功能：    "Build an expense tracker with income/expense, monthly stats, category filter"
L3 带技术栈：  "Build an expense tracker with ... stack Next.js + SQLite ..."
L4 带边界：    "... handle empty input, negatives, insufficient balance ..."
```

通常比较相邻两个层级，例如 L1 与 L3。

**维度 2：质量强调点**

```text
简洁型：       "Minimal code, no over-engineering"
稳健型：       "Every edge case and error handled gracefully"
性能型：       "Prefer speed and memory"
可维护型：     "Clear structure for future changes"
```

**维度 3：示例驱动**

```text
抽象描述：     "Add income and expense; monthly stats"
示例驱动：     "Input 'lunch 35' -> line '-35 lunch'; 'this month' -> stats"
```

覆盖等级：

```text
Level 1: 1-2 个正常输入输出示例
Level 2: 加上主要场景
Level 3: 再加边界与错误场景
```

**维度 4：角色视角**

```text
"You are a senior engineer with 10 years ..."
"You are a PM focused on UX ..."
"You are a minimalist (YAGNI) ..."
```

**维度 5：拆解方式**

```text
A 按功能拆：ledger、stats、filter 分成子提示
B 按角色拆：user 与 admin
C 不拆：一个 Prompt 覆盖全部
```

## 方案生成规则

- 第一个方案必须保留用户的 **原始表达**，作为 baseline。
- 后续方案应在不同维度上选取不同层级或风格。
- 每个方案都必须覆盖规格中的 **全部功能**，禁止暗中增删范围。

## 一致性检查（强制）

> 常见失败点：不同方案在范围上已经漂移，导致“得分更高”其实是“多做了功能”，而不是“表述更优”。

在所有方案生成完成后，**必须** 执行以下检查：

**检查项：**

```text
- 功能范围一致
  抽取每个方案的功能列表，必须完全一致
  错误示例：A 只有“生成+润色”，B 却额外加了“语气 + 版本管理”

- 技术栈一致
- 输出类型一致（SPA、API 等）
- 验收清单一致（如果 phase-1 文件已存在）
  所有方案都必须满足同一份 acceptance，不允许每个方案自己删项

- 差异只能来自表述方式
  允许：抽象层级、示例、角色、强调点不同
  不允许：功能数量、技术栈、架构不同
```

**必须显式输出如下结构：**

```text
## Scheme consistency check

Feature list: [all features]
Stack: [technologies]

Scheme 1 features: [...] -> match / mismatch [note]
Scheme 2 features: [...] -> match / mismatch [note]
Scheme 3 features: [...] -> match / mismatch [note]

Result: pass / fail (fixed)
```

**如果检查失败：**

- 不得进入 phase 3
- 必须回到 phase 1 规格重新修正方案
- 修正后重新执行一致性检查，直到通过

## 闸门

只有一致性检查 **通过** 才能进入 phase 3。否则 **不得** 进入下一阶段。

## 默认落盘记录

除对话输出外，还必须把完整的 **Scheme consistency check** 内容写入 **`tracks/phase-02-consistency-check.md`**，包括标题、功能列表、技术栈、逐方案比对行和 **result**，并同时记录 **scheme-维度映射表**，之后才能进入 phase 3。细则见 **`execution.md`** 中的“多方案内循环独立性”。

> **可由维护者调整**：这些默认要求可以通过根目录 `SKILL.md` 或宿主规则放宽。
