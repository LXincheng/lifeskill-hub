# Contributing to LifeSkill Hub

感谢你参与 LifeSkill Hub。提交代码前，请先阅读：

1. [`AGENTS.md`](./AGENTS.md)：产品边界与 AI 协作规则
2. [`docs/ENGINEERING_STANDARDS.md`](./docs/ENGINEERING_STANDARDS.md)：命名、模块边界、质量与安全规范
3. [`docs/DEVELOPMENT_PLAN.md`](./docs/DEVELOPMENT_PLAN.md)：当前里程碑与验收标准

## 基本流程

1. 从一个可演示的纵向切片开始，避免一次创建大量空抽象。
2. 修改前检查现有测试和工作区状态，不覆盖无关改动。
3. 为新行为补测试，并执行前后端对应的检查命令。
4. 若产品范围、API、数据模型或架构边界改变，同步更新 `docs/`。
5. 提交信息遵循 Conventional Commits，例如 `feat(skill): confirm skill draft`。

## 合并前最低检查

```powershell
cd frontend
npm run typecheck
npm run build

cd ..\backend
.\mvnw.cmd test
```

真实密钥、个人数据、生成产物和本地配置不得提交到仓库。

