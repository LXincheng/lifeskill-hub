# 工程与命名规范

这份文档是 LifeSkill Hub 的可执行开发契约。目标不是追求抽象数量，而是让每次迭代保持清晰、可靠、易替换。

## 1. 名称体系

同一个产品在不同场景使用不同形式：

| 场景 | 形式 | 示例 |
| --- | --- | --- |
| 产品品牌、页面标题 | 自然品牌名 | `LifeSkill Hub` |
| GitHub 仓库、本地目录、部署服务 | 小写 kebab-case | `lifeskill-hub` |
| Java 类型、React 组件、TypeScript 类型 | PascalCase | `SkillDraft`, `LearningCard` |
| Java/TypeScript 函数、方法、变量 | lowerCamelCase | `confirmSkillDraft`, `evidenceCount` |
| 常量、环境变量 | UPPER_SNAKE_CASE | `MAX_AGENT_STEPS`, `DEEPSEEK_API_KEY` |

仓库名不使用 `LifeSkillHub`。小写 kebab-case 更符合 GitHub URL、命令行、包管理和部署服务的通用习惯；PascalCase 留给代码中的类型，语义更清楚。

## 2. 文件与符号命名

### Java

- 包名全小写，并按业务能力组织：`com.lifeskillhub.skill.application`。
- 类、record、enum 使用 PascalCase；一个公开顶级类型对应一个文件。
- 接口用业务角色命名，如 `EvidenceRepository`，不加 `I` 前缀。
- API 入参与出参使用 `Request`、`Response` 后缀；持久化实体仅在需要区分时使用 `Entity` 后缀。
- 测试文件使用 `*Test.java`；集成测试使用 `*IntegrationTest.java`。

### React 与 TypeScript

- React 组件文件使用 PascalCase：`AgentProcessFlow.tsx`。
- Hook 使用 `use` 前缀：`useConversation.ts`。
- 非组件模块使用 lowerCamelCase：`conversationApi.ts`、`formatEvidence.ts`。
- 类型使用 PascalCase，不加 `I` 前缀；Props 使用 `ComponentNameProps`。
- 布尔量以 `is`、`has`、`can` 或 `should` 开头，事件处理函数以 `handle` 开头。
- CSS 类使用带组件或功能前缀的 kebab-case，避免无边界的 `.title`、`.card`。

### API、数据库与文档

- REST 资源使用复数名词和 kebab-case 路径：`/api/skill-runs/{id}`。
- JSON 字段使用 lowerCamelCase；时间使用带时区的 ISO 8601 字符串。
- PostgreSQL 表、列、索引使用 snake_case；外键写作 `<entity>_id`。
- Flyway 文件只新增、不回写已共享迁移；名称说明业务变化。
- 根级标准文档保留约定名称，如 `README.md`、`AGENTS.md`；`docs/` 内契约文档使用 UPPER_SNAKE_CASE。

## 3. 模块边界：低耦合、高内聚

后端采用按业务能力拆分的模块化单体。每个能力内部遵循：

```text
api → application → domain
          ↓
    infrastructure
```

- `domain` 保存业务规则和状态，不依赖 Spring、数据库、DeepSeek 或具体 Web 协议。
- `application` 编排用例、事务、权限和 Agent Harness，通过端口调用外部能力。
- `api` 只负责协议转换、输入校验和响应映射，不承载业务决策。
- `infrastructure` 实现仓储、模型、搜索、通知等端口；替换供应商不应改动领域规则。
- 跨模块只通过公开应用服务、事件或明确端口协作，不直接访问对方内部 repository。
- 前端按功能聚合视图、状态、API 与类型；共享层只接收出现两次以上且语义稳定的能力。

先写清楚一个真实用例，再抽象第二个实现。禁止创建含糊的 `CommonService`、`Utils`、`Manager` 大杂烩。

## 4. 函数、状态与错误

- 一个函数只表达一个意图；出现多层分支时优先提取有业务名称的函数。
- 优先不可变值、Java record 和只读 TypeScript 数据；状态修改必须有明确入口。
- 依赖使用构造器注入；禁止 Service Locator 和隐藏的全局可变状态。
- 在系统边界校验输入，在领域层校验不变量。错误返回可识别的错误码和安全信息。
- 不静默吞异常，不用宽泛 `catch` 掩盖失败；日志包含关联 ID，不记录密钥或敏感正文。
- Java `Optional` 主要用于返回值，不用于实体字段和方法参数；TypeScript 明确区分缺失与空值。
- 数据库事务放在应用用例边界，外部网络调用避免长时间占用事务。

## 5. Agent 与 AI 工程规范

模型是可替换的推理组件，不是系统控制器。

- DeepSeek 通过 `ModelPort` 一类端口接入，领域与应用代码不依赖供应商 DTO。
- 模型输出必须经过结构化 Schema 校验；数字、时间、URL、来源数量和权限由确定性代码复核。
- 搜索结果先标准化为 `Evidence`，面向用户的事实 `Claim` 必须引用 Evidence ID。
- Harness 控制最大步骤、超时、预算、可调用工具、重试、幂等键和人工确认点。
- 创建长期 Skill、发送外部通知、修改计划等有持续影响的动作必须先展示草案并确认。
- 过程流只展示步骤、工具、来源、状态、耗时和错误摘要，不保存或暴露模型原始思维链。
- Prompt 与输出 Schema 需要版本号；关键 Agent 路径使用固定样例做回归评测。
- 多 Agent 仅在角色隔离或并行收益可验证时引入，MVP 优先使用单 Harness 下的显式步骤。

## 6. API、数据与安全

- API 先定义请求、响应、错误和幂等语义，再连接界面。
- 更新操作明确所有权；高风险操作保留审计字段与可追踪事件。
- 数据迁移向前兼容，先扩展再收缩；删除字段前确认读取方已迁移。
- 密钥只来自环境变量或部署平台 Secret；示例值必须不可用。
- 默认最小权限。外部 URL、文件和模型输出都视为不可信输入。
- 用户内容日志化遵循最少原则；未来接入认证后，查询必须带用户数据边界。

## 7. 测试与交付门槛

- 领域规则写快速单元测试；数据库、HTTP、模型适配器写边界集成测试；核心闭环保留少量端到端测试。
- 修复缺陷时先增加能复现问题的测试。
- 时间、UUID、模型和外部来源通过可替换端口控制，避免脆弱测试。
- 前端至少通过 typecheck 与生产构建；后端至少通过 Maven 测试。
- 提交遵循 Conventional Commits；一次提交只表达一个可回滚意图。
- 任何 API、数据模型、产品边界或关键技术决策变化都同步更新文档。

## 8. 每次开发后的成长交付

Codex 每次完成实质开发后，最终说明必须简洁包含：

1. **完成了什么**：用户现在能感知到的结果，而非文件流水账。
2. **产品要点**：本次功能为什么属于核心闭环，避免了什么产品噪音。
3. **技术要点**：讲清一个值得迁移到大厂项目的设计选择及其取舍。
4. **你应掌握的代码入口**：最多三个关键文件或调用链，便于快速接管。
5. **验证与下一步**：检查结果、已知限制和最值得继续的一个切片。

讲解默认控制在 5–10 个要点内：不堆术语，不逐文件复述，不省略风险；遇到新概念时用“它是什么 → 为什么这样做 → 在本项目哪里体现”说明。

