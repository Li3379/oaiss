# External Evidence Archive

本目录用于归档 OAISS CHAIN 远程 `staging` / `production` 发布阶段的真实执行证据。

这些文件的作用不是替代执行，而是把外部执行结果沉淀为：

- 可复核
- 可签收
- 可追溯
- 可用于重新判断 acceptance 是否可勾选

## 1. 使用规则

1. 每一次真实外部执行都生成独立文件，不要把多个不同日期或不同环境的执行结果混写在同一份记录里。
2. 文件内容应基于真实执行结果填写，不要预先勾选未完成项。
3. 不要把真实 secret、私钥、明文密码写入仓库。
4. 如需附截图、日志、导出文本，请在文档中记录相对路径或外部链接。
5. 只有当证据足够支撑 `tracks/phase-01-acceptance.md` 中的开放项时，才可把对应 acceptance 项从未完成改为完成。

## 2. 推荐命名

- `staging-deploy-YYYY-MM-DD.md`
- `staging-rehearsal-YYYY-MM-DD.md`
- `production-deploy-YYYY-MM-DD.md`
- `production-observation-YYYY-MM-DD.md`
- `production-rollback-YYYY-MM-DD.md`

## 3. 推荐流程

1. 先复制 `docs/external-execution-evidence-template.md`
2. 保存为本目录下带日期的新文件
3. 填写 GitHub Actions Run、镜像 tag、健康检查、业务验收、观察窗口、回滚等真实结果
4. 由执行人和复核人共同确认
5. 再决定是否更新 acceptance 清单中的开放项

补充要求：

- 请保留模板第 11 节中的 acceptance 原文，不要改写条目文本
- `node scripts/closure-audit.mjs` 会按这些原文与 `tracks/phase-01-acceptance.md` 做交叉校验

## 4. 当前状态说明

截至当前仓库状态：

- 仓库侧发布与闭环能力已具备
- 外部执行 acceptance 项仍未完成
- 本目录中的骨架文件仅是待填写模板，不构成任何“已完成”证据
- `closure-audit.mjs` 会区分 skeleton 文件与真实 evidence 文件

## 5. 相关文档

- [external-execution-evidence-template.md](C:/Users/LiShuai/Desktop/OAISS%20CHAIN/docs/external-execution-evidence-template.md)
- [remote-staging-first-deploy-checklist.md](C:/Users/LiShuai/Desktop/OAISS%20CHAIN/docs/remote-staging-first-deploy-checklist.md)
- [remote-staging-rehearsal.md](C:/Users/LiShuai/Desktop/OAISS%20CHAIN/docs/remote-staging-rehearsal.md)
- [production-observation-window.md](C:/Users/LiShuai/Desktop/OAISS%20CHAIN/docs/production-observation-window.md)
- [go-live-gate-matrix.md](C:/Users/LiShuai/Desktop/OAISS%20CHAIN/docs/go-live-gate-matrix.md)
