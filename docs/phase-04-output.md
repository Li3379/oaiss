# Phase 4：对比、择优、交付

**R3（先看 acceptance，自评分只作参考）请以 `verifiability.md` 为准。**

## 对比前检查（硬闸门）

```text
- 每个 scheme 都已按 phase-03 的终止条件完成 inner loop
- tracks/phase-01-acceptance.md 已存在，且至少有一条轨道的最终产物已把所有验收项全部勾选完成，否则不能有最终交付
- 每个 scheme 都有可追溯的迭代链路（默认是每轮文件头中的记录）
- 所有 scheme 仍然与 phase 2 的范围保持一致
```

**说明**：“完整日志”在这里的意思是，每一轮要求写入的头部字段都能追溯到；除非维护者另有要求，否则 **不强制额外再写单独 `.md` 日志**。

如果某个 scheme 在中途自己长出了额外功能：

- 评分时忽略这些额外功能，或者直接标记为 **不可比较** 并排除。

## 如何定义“最佳”

**前提条件**：只在那些 **`tracks/phase-01-acceptance.md` 已全部勾选完成** 的轨道中进行比较（见 **R3**）。如果 **没有任何轨道全绿**，则 **不能** 产出最终交付。

在所有通过 acceptance 的轨道中，按照以下优先级选出 **最佳代码** 与 **最佳 Prompt**：

```text
1. 最终自评分（权重最高，但仅用于相对比较）
   - 在 acceptance 已通过的轨道中，最终分最高者胜出
   - 该轨道对应的 Prompt 也成为最佳 Prompt 候选

2. 起始分（当前几乎打平时再看）
   - 起始分更高，说明 Prompt 更容易落地出第一版

3. 提升幅度（起点与终点都接近时再看）
   - 提升越大，说明这条轨道越有可迭代空间

4. 稳定性（仍然接近时再看）
   - 轮次间回退更少者，说明稳定性更强
```

## 选择策略

**遵循 `verifiability.md` 的 R3。** 总结如下：

```text
至少有一条轨道 acceptance 全绿 -> 在这些轨道中选择自评分最高者，输出代码 + Prompt
没有任何轨道 acceptance 全绿 -> 闸门失败，不得给出“获胜方案”作为最终交付
```

## 当没有任何轨道通过 acceptance（硬规则）

**仍然遵循 `verifiability.md` 的 R3。**

常见原因包括：acceptance 过于严苛、phase 1 需求写得不清晰，或者 inner loop 提前停得太早。

**如果所有轨道都未通过完整 acceptance：**

```text
- 不得宣称“最佳方案”并直接交付
- 必须明确告诉用户：
  1. 每条轨道具体未通过哪些 acceptance 项（对照 phase-01-acceptance.md）
  2. 自评分与这些缺口之间的关系（仅作解释）
  3. 建议：放宽或拆分验收项、补充可检查条件，或继续迭代
  4. 询问是要调整 phase 1 还是继续往下做
```

## 分析输出模板

```text
Prompt optimization analysis:
  Best scheme: [id]
  Dimension: [1-5]
  Best score: X/10
  Worst scheme: [id]
  Worst score: Y/10

  Why best beats original:
  - [diff 1]
  - [diff 2]

  Suggestions:
  - Original was missing [X]; adding it helps ~Z%
  - Effective pattern: [...]
```

---

## 最终输出格式

完成双层运行后，返回给用户的内容必须遵守以下原则：

**不要重新生成一份新结果**，而是从现有工作中 **选择已有产物**：

- **Best Prompt**：遵守 **R3**。只有在 **至少一条轨道 acceptance 全绿** 时，才能从这些轨道里选出 **自评分最高的 Prompt 原文**。如果 **没有任何轨道全绿**，就 **不能** 把任何方案标成可交付的“最佳方案”。
- **Best code**：应来自同一条胜出轨道的 **最终 inner-loop 文件**，且该轨道的终止过程必须合法（acceptance 全绿，并满足 `phase-03-inner.md`）。

关于“交付文件”与“最终轨道文件”的一致性，以及“same / verified”等说法，必须遵守 **`verifiability.md` 的 R2、R4、R5**。

```markdown
## Optimization result

**Final score**: X/10 (baseline Y/10, +Z%)
**Delivery source**: tracks/prompt-{id}/r{round}.{ext}
**Consistency note** (R4): class A 需附命令与输出；class B 只能写“请本地验证”，不得冒充已验证

### Best Prompt

[获胜方案的 Prompt 原文]

### Final artifact

[inner loop 中的最终文件原文]
[如果交付文件与轨道文件不同，必须说明差异]

### Prompt improvement notes

[为什么这个表述优于原始表述]
[以后描述类似需求时应如何表达]
```
