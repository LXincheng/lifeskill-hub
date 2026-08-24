# Project Context / 新窗口接续说明

## 一句话定义

LifeSkill Hub 是一个以聊天为入口，将用户意图转化为计划、可复用 Skill、可靠动态和结构化学习的个人能力中枢。

## 当前共识

- 产品形态：响应式 Web，后续升级 PWA；暂不做小程序端。
- 一级入口：对话、学习、动态。
- 后端：Java 21 + Spring Boot + Spring AI。
- 模型：DeepSeek API。
- 前端：React + TypeScript + Vite。
- 数据库：PostgreSQL + Flyway。
- 架构：模块化单体，Java 侧实现 Agent Runtime/Harness。
- 开源：GitHub 公开仓库。

## 核心用户闭环

1. 用户在聊天中描述一次性任务、学习目标或持续兴趣。
2. 系统判断意图；需要搜索时展示轻量 Agent 过程流。
3. Researcher 获取来源，Verifier 核对 Claim 与 Evidence。
4. 对持续性需求，系统展示 SkillDraft，用户确认后才落库。
5. Skill 定时或事件触发执行，生成动态卡片和可选通知。
6. 用户从聊天或动态进入学习文件夹，完成卡片、文章、测验或实践。
7. 系统保存进度、反馈和复习任务，后续复盘推送质量。

## UI 方向

- Claude 风格的留白、阅读排版、无气泡 AI 消息和轻量用户气泡。
- 不使用 Claude 陶土色；采用中性纸白/墨色与少量钴蓝、青绿状态色。
- Agent 流程是细线、圆点和短文本，默认折叠，运行时才出现。
- 避免统计仪表盘、渐变装饰、过多圆角卡片和大面积图标。

## 首个业务切片

建议先实现“Java Agent Weekly”：

- 聊天创建每周关注 Skill。
- 使用 GitHub/官方文档适配器收集来源。
- 输出 5 分钟简报、学习卡片和一道实践题。
- 重要结论经过二次核验。
- 在动态页展示，并可进入 Java Agent 学习文件夹。

## 当前工程状态（2026-08-24）

- M0 本地骨架已完成：React 页面骨架、Spring Boot API、Flyway 初始迁移、CI 与 Apache-2.0 License。
- 前端 typecheck/生产构建和后端测试均已通过；后端测试使用 H2 隔离本地数据库差异。
- GitHub 公开仓库与首次推送正在完成；本机暂未安装 Docker，因此 PostgreSQL 联调仍待验证。
- 下一开发切片是 M1“聊天创建 Skill”：先完成对话持久化，再接 DeepSeek 结构化草案，最后由用户确认落库。
- 所有命名、模块边界、Agent 安全和开发后讲解遵循 `docs/ENGINEERING_STANDARDS.md`。

## 新窗口建议提示词

```text
请先阅读 AGENTS.md、docs/PROJECT_CONTEXT.md、docs/PRODUCT.md、
docs/ARCHITECTURE.md、docs/ENGINEERING_STANDARDS.md 和
docs/DEVELOPMENT_PLAN.md，然后检查 git status。
继续当前里程碑，不扩展非目标功能，并在修改后运行相关测试。
```
