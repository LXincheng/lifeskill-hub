# LifeSkill Hub Agent Guide

本文件是新 Codex 窗口和所有自动化开发任务的首要项目上下文。

## 开始工作

修改代码前必须阅读：

1. `docs/PROJECT_CONTEXT.md`
2. `docs/PRODUCT.md`
3. `docs/ARCHITECTURE.md`
4. `docs/ENGINEERING_STANDARDS.md`
5. `docs/DEVELOPMENT_PLAN.md`

然后检查 `git status`，保留用户已有改动，不覆盖无关文件。

## 已确定的产品原则

- 主入口只有：对话、学习、动态。
- 聊天是创建计划、Skill、关注规则和发起搜索的主要入口。
- 长期任务或推送必须展示结构化草案并由用户确认，不能静默创建。
- Agent 运行过程只在聊天中按需展示可验证事件，不设独立一级运行记录页。
- 展示工具、来源、状态和耗时；不得向用户暴露模型原始思维链。
- 动态频道由用户 Skill、计划和兴趣生成，禁止写死金融、GitHub 等固定频道。
- 学习内容组织为文件夹，并包含卡片、文章、测验、代码实践和外部资料。
- 重要结论必须关联 Evidence；来源不足时降级或不推送。

## 已确定的技术原则

- 核心后端使用 Java 21 + Spring Boot，保持模块化单体，不拆微服务。
- AI 使用 Spring AI 接入 DeepSeek，模型只是推理层；工具执行、权限、预算和发布决策由 Java Harness 控制。
- 前端使用 React + TypeScript；视觉强调留白、排版和内容，不复刻 Claude 配色。
- PostgreSQL 是唯一主数据库；Flyway 管理结构迁移。
- MVP 不引入 Kafka、Elasticsearch、向量数据库、LangGraph 或自治多 Agent 框架。

## 工程约束

- 命名、模块边界、错误处理、测试与 Agent 开发必须遵循 `docs/ENGINEERING_STANDARDS.md`。
- 后端包按业务能力组织，而不是堆在 `controller/service/repository` 全局目录。
- 外部来源先落为 Evidence，模型生成 Claim 时必须引用 Evidence ID。
- 数字、日期、URL、阈值等由确定性代码校验，不能依赖模型自检。
- 所有长期执行都必须具备暂停、超时、最大步骤、预算和审计字段。
- 新功能必须写测试；架构或产品边界变化需要同步更新 `docs/`。
- 密钥只从环境变量读取，不能进入代码、日志、测试快照或提交历史。

## 开发完成后的交付方式

- 先说明用户可感知的结果，再说明实现；不要只列修改文件。
- 用精炼、有重点的方式讲解一个产品判断和一个可迁移的技术设计。
- 指出最多三个值得用户阅读的代码入口，并解释调用关系。
- 明确验证结果、已知限制和最值得继续的下一步。
- 详细格式遵循 `docs/ENGINEERING_STANDARDS.md` 的“每次开发后的成长交付”。

## 当前阶段

当前目标是完成 `docs/DEVELOPMENT_PLAN.md` 中的 M0/M1。不要提前扩展社区、Skill 市场、3D 地球、复杂知识编辑器或微服务基础设施。
