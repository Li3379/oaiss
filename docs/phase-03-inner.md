# Phase 3：内层迭代

**R1-R5 请以 `verifiability.md` 为唯一准绳。**

## 核心思想

**Prompt 固定，代码或文档持续优化。** Inner loop 不关心 Prompt 是怎么生成的，只关心最终产物是否越来越好。

## 实体产物要求（phase 3 细则）

**R1、R2、R4、R5 以 `verifiability.md` 为准。** 这里补充 phase 3 专属要求：

```text
- 命名约定：tracks/prompt-{id}/r{round}.{ext}
  例如：tracks/prompt-a/r01.html、tracks/prompt-a/r02.html

- 每个文件头部（HTML 注释或代码注释）必须包含：
  - Scheme id
  - Round number
  - 本轮只改了什么（1 句话）
  - 本轮分维度得分
  - 上一轮最大问题（1 句话）

- 文件之间必须可 clean diff：
  - 相邻轮次的 diff 只能体现“本轮声明的那一个改动”
  - 如果实际改了多个区域，则该轮评分无效，必须回退重做

- 如果存在单独的“交付文件”（例如根目录 index.html）：
  - 也必须满足 R2；验证口径遵循 R4/R5
```

## 迭代流程

```text
Round 1：先生成首版（目标是“能工作”，不是“完美”）
  -> 对照 tracks/phase-01-acceptance.md 检查能验证的项
  -> 评分（第 1 轮上限 7 分，仅作参考）
  -> 记录日志

Round 2 及以后：
  -> 优先把还没勾选的 acceptance 项变成已勾选（但仍然遵守“每轮只改一个点”）
  -> 重新检查 acceptance
  -> 重新评分（仅作参考）
  -> 如果分数提高，则保留（若与 acceptance 冲突，以 acceptance 为准）
  -> 如果分数下降，则回退
  -> 记录日志

单条轨道的终止条件：满足以下任一项即可停止

  1. 主终止条件：tracks/phase-01-acceptance.md 中所有条目都已勾选完成，并且能追溯到验证过程
  2. 上限：最多 5 轮；如果 5 轮后仍未全绿，则该轨道不能作为最终交付来源，phase 4 必须列出缺口
  3. 仅供参考：自评分走势不能替代第 1 条，例如“>=8 分”不能单独作为终止依据
```

**说明**：自评分存在偏差，因此必须用 **phase 1 锁定的 acceptance checklist** 来防止目标漂移。后续如果换独立评审，也应以这份清单作为最低门槛。

## 评分客观性（硬规则）

> 作者和评分者是同一个模型时，容易高估，因此 **acceptance checklist 才是真正的硬门槛**。

**规则 1：必须按维度评分，并写出具体扣分理由**

```text
Score: 6.5/10
- Correctness (x/x): ...
- Presentation (x/x): ...
- Code quality (x/x): ...
- Robustness (x/x): ...
- Maintainability (x/x): ...
```

**无效评分示例：**

- “感觉不错，8/10”
- “功能完整、代码不错，8.5/10”，但没有写扣分依据

**规则 2：第一轮最高只能打 7 分**

第一版默认应该是“粗糙可用”，必须天然保留优化空间。

**规则 3：每轮前后都要写明判断**

```text
Before:
  Biggest issue: ...
  Plan: ...
  Score before: 6.5/10
  Expectation: ...
After:
  Score after: 7.8/10
  Delta: +1.3
  Decision: keep
```

**规则 4：禁止自我合理化**

- “第一版已经够好了” -> 不允许
- “功能 X 不在 spec 里” -> 如果 phase 1 规格写了，它就是必做项

## 评分细则：代码类产物（0-10）

| 维度 | 权重 | 检查点 |
|---|---|---|
| Correctness | 25% | 核心路径可用、边界合理、无明显 bug |
| Presentation | 25% | 层级清晰、节奏合理、风格匹配、反馈明确 |
| Code quality | 20% | 结构、复用、命名、依赖控制 |
| Robustness | 15% | 错误处理、校验、可访问性、资源处理 |
| Maintainability | 15% | token/CSS 变量、模块化、可扩展性 |

**防漂移规则：**

```text
- 每轮的“最大问题”必须来自当前最低分维度（或并列最低）
- 不能连续 3 轮只修改同一个维度
- 某个维度高分，不能掩盖另一个维度明显失败
```

## 评分细则：文档 / 方案类产物（0-10）

| 维度 | 权重 | 检查点 |
|---|---|---|
| Completeness | 40% | 是否回答完整，关键点是否齐全 |
| Accuracy | 30% | 论据正确，无明显错误 |
| Clarity | 20% | 结构清晰，表达简洁 |
| Actionability | 10% | 是否给出可执行下一步 |

## 其他内循环规则

- 每轮只能改一个点；如果要改 3 个点，就拆成 3 轮
- 分数低于 6 时，不要追求“优雅”，先修 correctness
- 每一轮都必须有分数
- “还可以更好”不算改动，必须明确写出具体怎么改
- 每条轨道最多 **5** 轮

## 每个方案的汇总输出

```text
Scheme: [id or short name]
Dimension: [1-5]
Final score: X/10
Rounds: N
Start score: Y/10
Key changes: [2-3 best]
Log:
  1: Y -> [change] -> Z
  2: Z -> [change] -> W
  ...
```

## 闸门

```text
- tracks/phase-01-acceptance.md 已存在（见 phase-01-spec.md）
- 每个 scheme 至少跑 2 轮
- 若声称某轨道“可交付”，则该轨道的 acceptance 必须全部勾选完成
- 每轮都有可追溯的分维度评分（仅作参考，遵循 R3）
- 每轮都有实体文件：tracks/prompt-{id}/r{round}.{ext}（R1）
- 相邻 diff 与本轮声明的单一改动一致
- 缺失文件或 diff 不成立，则该 scheme 的迭代视为无效
```
