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

- 采用 Figma 新版原型的紧凑工作台结构，减少无意义留白，保留清晰阅读层级和无气泡消息。
- 不使用 Claude 陶土色；采用中性纸白/墨色与少量钴蓝、青绿状态色。
- Agent 流程是细线、圆点和短文本，默认折叠，运行时才出现。
- 避免统计仪表盘、渐变装饰和大面积装饰图标；首屏状态只能来自真实会话数据。
- 桌面只保留 76px 左侧主导航，移动端使用底部导航；字号、间距、圆角和 Lucide 图标统一由 `docs/DESIGN_SYSTEM.md` 约束。
- 学习页采用文件夹、文件、内容三级结构，支持学习路径时间线、文章代码块和交互测验视图。

## 首个业务切片

建议先实现“Java Agent Weekly”：

- 聊天创建每周关注 Skill。
- 使用 GitHub/官方文档适配器收集来源。
- 输出 5 分钟简报、学习卡片和一道实践题。
- 重要结论经过二次核验。
- 在动态页展示，并可进入 Java Agent 学习文件夹。

## 当前工程状态（2026-08-27）

- M0 本地骨架和 M1.1 真实对话骨架已完成：对话与消息领域模型、PostgreSQL 迁移、REST API、React 接入和刷新历史恢复。
- 前端 typecheck/生产构建和后端领域、应用、HTTP/持久化测试均已通过；后端测试使用 H2 隔离本地数据库差异。
- GitHub 公开仓库与首次推送已完成；本机暂未安装 Docker，因此 PostgreSQL 联调仍待验证。
- M1.2 代码已完成：Model Port 隔离 DeepSeek，支持三类意图、结构化 Schema 校验、SkillDraft 业务校验与持久化，并在模型不可用时安全降级。
- 当前环境没有 `DEEPSEEK_API_KEY`，真实 DeepSeek 联调仍待完成；模型默认关闭，启用需同时设置密钥和 `LIFESKILL_MODEL_ENABLED=true`。
- 下一开发切片是 M2：接入首个可靠 Source Adapter，建立 Evidence、Claim 与 Policy Gate 闭环。
- 所有命名、模块边界、Agent 安全和开发后讲解遵循 `docs/ENGINEERING_STANDARDS.md`。
- 新版前端工作台已经按 `docs/DESIGN_SYSTEM.md` 重构，对话、学习和动态共用导航、字体、图标及响应式规则。
- M1.3 已实现 SkillDraft 幂等确认、Skill/SkillVersion 落库，以及 Skill 暂停、恢复和修改 API；聊天页可以完成确认。
- 学习页已接入真实文件夹与文档 CRUD，可创建文章、笔记和行动清单并在刷新后恢复。
- 动态页已改为读取真实 `pulse_item` 数据；在 M2 可靠来源链路完成前保持可信空状态，不再展示样例核验数据。
- 根目录 `.env` 是本地 DeepSeek 与数据库配置入口，文件被 Git 忽略。
- 2026-08-27 根据最新版 Figma 原型重做前端信息结构：移除聊天右侧面板，新增真实状态与四个任务入口；学习页升级为桌面三栏和手机三级栈；动态分类改用分段控件和分类色条。

## 新窗口建议提示词

```text
请先阅读 AGENTS.md、docs/PROJECT_CONTEXT.md、docs/PRODUCT.md、
docs/ARCHITECTURE.md、docs/ENGINEERING_STANDARDS.md 和
docs/DEVELOPMENT_PLAN.md、docs/DESIGN_SYSTEM.md，然后检查 git status。
继续当前里程碑，不扩展非目标功能，并在修改后运行相关测试。
```
