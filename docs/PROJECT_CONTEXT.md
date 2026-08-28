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

## 目标用户闭环（并非全部已实现）

1. 用户在聊天中描述一次性任务、学习目标或持续兴趣。
2. 系统判断意图；需要搜索时展示轻量 Agent 过程流。
3. Researcher 获取来源，Verifier 核对 Claim 与 Evidence。
4. 对持续性需求，系统展示 SkillDraft，用户确认后才落库。
5. Skill 手动或定时触发受控运行，通过 Policy Gate 后生成动态卡片。
6. 用户从聊天或动态进入学习文件夹，完成卡片、文章、测验或实践。
7. 后续阶段再保存学习进度、反馈和复习任务，并评估推送质量。

## 当前真实能力边界

已可用：真实 PostgreSQL、DeepSeek 普通问答与结构化 SkillDraft、确认创建与管理 Skill、消息执行收据、受控 AgentRun、Spring AI 官方 Release 与 World Gold Council 官方研究采集、Evidence/Claim/Verifier/Policy Gate、可靠动态、一次性专业研究报告、从动态或对话生成学习文件夹与文章/路径/测验，以及学习进度与测验记录。

尚未可用：通知、实时行情适配器、购票查询与写操作 Tool、复杂在线编辑器和跨来源冲突处理。界面与 Agent 不得把这些能力描述为已经执行。

## UI 方向

- 采用 Figma 新版原型的紧凑工作台结构，减少无意义留白，保留清晰阅读层级和无气泡消息。
- 工作台采用石墨黑控制层、柔和紫色操作强调和青绿可信状态；长报告和学习正文使用独立暖白纸张阅读层与克制的赭红章节强调色。
- Agent 流程是细线、圆点和短文本，默认折叠，运行时才出现。
- 避免统计仪表盘、渐变装饰和大面积装饰图标；首屏状态只能来自真实会话数据。
- 桌面使用 236px 语义侧栏，中等屏幕收为 82px，移动端使用底部导航；字号、间距、圆角和 Lucide 图标统一由 `docs/DESIGN_SYSTEM.md` 约束。
- 学习页采用文件夹、文件、内容三级结构，支持学习路径时间线、文章代码块和交互测验视图。

## 当前 M2 业务切片

M2 保持两条窄而真实的闭环，不铺设空壳工具：

- 聊天创建每周关注 Skill。
- 使用 GitHub/官方文档适配器收集来源。
- 支持手动运行 Skill，输出带来源的简报，并生成学习路径、文章和测验。
- 重要结论经过二次核验。
- 在动态页展示，并可进入 Java Agent 学习文件夹。
- 在聊天中提出一次性黄金研究后，确定性路由到 World Gold Council 官方适配器，经相同可信管道生成专业报告并在学习文件夹保存。

完成这条闭环后，行情研究、购票辅助等能力通过新的 Source Adapter 或受控 Tool 扩展。涉及实时价格、库存、账号、下单或支付时，必须重新校验数据并要求用户在最终外部写操作前确认。

## 当前工程状态（2026-08-28）

- M0 本地骨架和 M1.1 真实对话骨架已完成：对话与消息领域模型、PostgreSQL 迁移、REST API、React 接入和刷新历史恢复。
- 前端 typecheck/生产构建和后端领域、应用、HTTP/持久化测试均已通过；后端测试使用 H2 隔离本地数据库差异。
- GitHub 公开仓库与首次推送已完成；本机没有 Docker，但已使用本机 PostgreSQL 18 完成真实建库、Flyway 迁移和持久化联调。
- M1.2 代码已完成：Model Port 隔离 DeepSeek，支持三类意图、结构化 Schema 校验、SkillDraft 业务校验与持久化，并在模型不可用时安全降级。
- 真实 DeepSeek 联调已完成；模型结构化契约升级为始终返回非空 Draft 外壳，避免普通问答因 nullable Schema 反复重试。
- 当前开发切片是 M2：接入首个可靠 Source Adapter，建立 Evidence、Claim、Policy Gate、动态与学习内容生成闭环。
- 所有命名、模块边界、Agent 安全和开发后讲解遵循 `docs/ENGINEERING_STANDARDS.md`。
- 新版前端工作台已经按 `docs/DESIGN_SYSTEM.md` 重构，对话、学习和动态共用导航、字体、图标及响应式规则。
- M1.3 已实现 SkillDraft 幂等确认、Skill/SkillVersion 落库，以及 Skill 暂停、恢复和修改 API；聊天页可以完成确认。
- 学习页已接入真实文件夹与文档 CRUD，可创建文章、笔记和行动清单并在刷新后恢复。
- 动态页已改为读取真实 `pulse_item` 数据；在 M2 可靠来源链路完成前保持可信空状态，不再展示样例核验数据。
- 根目录 `.env` 是本地 DeepSeek 与数据库配置入口，文件被 Git 忽略。
- 2026-08-27 根据最新版 Figma 原型重做前端信息结构：移除聊天右侧面板，新增真实状态与四个任务入口；学习页升级为桌面三栏和手机三级栈；动态分类改用分段控件和分类色条。
- 对话发送采用乐观显示，等待期间展示请求耗时；助手消息保存可核验的处理收据，不展示思维链。学习编辑器支持编辑/预览切换。
- M2 纵向闭环已完成：`Java Agent Weekly` 可手动或按周运行；Harness 以 8 步上限和 120 秒超时编排 Planner、Researcher、Verifier、Composer，SSE 只发送可核验事件。
- 首个 Source Adapter 固定读取 `spring-projects/spring-ai` 官方 GitHub Releases；Evidence 保存发布时间、采集时间、原始内容和 SHA-256，Claim 必须引用 Evidence ID。
- Java Policy Gate 在发布动态和生成学习内容前重复检查 Evidence、官方 URL、独立核验状态和置信阈值；学习生成具有数据库级幂等边界。
- 根目录 `start.cmd` / `start.ps1` 提供 Windows 一键启动与健康检查，`stop.ps1` 只停止当前仓库的前后端进程。
- 对话历史现可列表、切换和删除；学习目标会进入 Planner → Curriculum Designer → Java Learning Gate 的受控运行，并保存 AI 生成内容。学习进度采用追加式尝试记录，刷新后恢复。
- 一次性黄金报告不依赖模型意图分类决定是否执行：Java 先匹配已接入能力，再由 Researcher、Verifier 和 Composer 使用 DeepSeek；报告完成后可从聊天直接打开并在刷新后恢复。
- 电影票请求会被 Capability/Policy 边界拦截；接入官方场次、实时库存、登录授权、锁座和下单 Tool 前不创建虚假 Skill，最终外部写操作与支付仍需人工确认。

## 产品形态说明

“世界控制台式 All-in-One”描述的是信息密度、全局掌控感和跨能力闭环，不是新增地图、行情大盘或大量一级页面。所有新增能力仍应进入对话、学习、动态三个入口。
