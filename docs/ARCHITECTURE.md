# 架构说明

## 1. 总体架构

下图是模块边界。当前已实现 `conversation`、`skill`、`learning`、`pulse`、`agent` 与 Evidence/Claim 可信管道；通知和其他外部工具仍按里程碑逐步实现。

```text
React Web / PWA
        ↓ REST / SSE
Spring Boot 模块化单体
├── conversation   已实现：对话、意图、执行收据
├── skill          已实现：定义、版本、确认、状态、手动运行与每周调度
├── learning       已实现：文件夹、内容 CRUD 和基础浏览
├── pulse          已实现：可靠动态、来源数量、推荐原因与证据入口
├── integration    已实现：DeepSeek 与 Spring AI 官方 GitHub Release Adapter
├── agent          已实现：Runtime、角色、Harness、SSE、Policy Gate
├── evidence       已实现：不可变来源、Claim、关联与独立核验
├── plan           后续：计划与任务
└── notification   后续：应用内/Web Push/外部渠道
        ↓
PostgreSQL / DeepSeek API / 外部事实 API
```

## 2. Agent 设计

MVP 不部署多个自治服务。Planner、Researcher、Verifier、Composer 是同一 Java Harness 中的角色化步骤；只有需要隔离上下文或独立核验时才发起额外模型调用，不能为了展示效果虚构多个 Agent。

```text
RECEIVED → PLANNING → COLLECTING → VERIFYING
         → COMPOSING → POLICY_CHECK → COMPLETED
                                      ↘ BLOCKED / FAILED
```

### 2.1 DeepSeek 负责

- 意图理解和计划草案
- 选择被允许的工具
- 多来源语义整理
- Claim 草案与学习内容生成
- 个性化学习计划生成；其结果标记为 `AI_GENERATED`，与基于 Evidence 的 `VERIFIED` 内容分开
- 独立上下文的语义核验

### 2.2 Java Harness 负责

- 工具注册、参数校验和真实执行
- 最大步骤、超时、重试和费用预算
- 来源策略和证据持久化
- JSON Schema 与业务规则校验
- 幂等、审计和失败恢复
- 外部写操作的用户确认
- 最终发布、降级或拦截

### 2.3 外部工具边界

- Source Adapter 只读采集官方文档、GitHub Release、RSS 等允许来源。
- 行情、库存、场次等易变化数据必须在代码侧标注采集时间并在使用前重新确认。
- 购票、通知、下单等外部写操作使用单独 Tool；查询结果不等于预订成功，最终写操作必须人工确认。
- 未注册 Tool 不可由模型临时访问；工具失败时保存安全摘要，不把失败伪装成完成。

### 2.4 行情、购票与通知的扩展契约

- 行情属于只读 `Source Adapter`：当前 World Gold Council 适配器提供官方研究而非实时价格；未来实时适配器必须返回来源、标的、报价时间、采集时间和原始响应哈希，确定性代码重算涨跌与时效，模型只能解释。没有实时适配器时不得发布实时行情结论。
- 购票分成只读查询 Adapter 与写操作 Tool。场次、库存和价格每次使用前重新采集；登录、锁座、下单和支付属于外部写操作，必须在参数明确后展示确认草案，并在最后一步再次人工确认。
- 通知属于单独的写操作 Tool。动态发布不等于已发送通知；通知需要渠道权限、免打扰规则、幂等键和发送审计，失败不能回写为动态失败。
- 这些扩展复用 AgentRun、Evidence、Claim 和 Policy Gate，但不会让模型获得任意网络或支付权限，也不需要拆成微服务。

## 3. 数据可信管道

```text
Source Adapter
  → Raw Evidence（不可变）
  → Normalization / Deduplication
  → Researcher 生成 Claim + evidenceIds
  → Deterministic Validator
  → Verifier 独立核验
  → Policy Gate
  → Content / Pulse / Notification
```

禁止模型直接构造行情数字、来源 URL 或发布时间。来源不足、链接失效或 Claim 无 Evidence 时不得标为已核验。

## 4. 数据表

Flyway 已创建第一批核心表，但存在数据表不等于对应运行能力已经完成：

- `conversation`
- `message`
- `skill`
- `skill_version`
- `skill_run`
- `agent_step`
- `evidence`
- `claim`
- `claim_evidence`
- `learning_folder`
- `content_item`
- `learning_attempt`
- `pulse_item`
- `notification_rule`

具体字段由 Flyway 迁移维护。JSONB 只保存易变配置和内容块 Payload，关键状态、时间、关系和审计字段使用普通列。

## 5. API 原则

- 路径前缀 `/api`。
- 聊天流式事件后续使用 SSE；普通配置使用 REST。
- 所有长期写操作必须设计幂等键；当前 SkillDraft 确认已实现，后续运行、生成和通知接口接入时分别补齐。
- 学习进度采用追加式 `learning_attempt` 保存，不重写原始 ContentItem。这样既能恢复最新进度，也保留测验历史；读取文件夹时由应用服务聚合最新一次尝试。
- 错误返回统一 Problem Details。
- API DTO 与 JPA Entity 分离。
- 外部模型和来源通过端口/适配器隔离，领域层不依赖具体 SDK。

## 6. 安全与隐私

- API Key 仅来自环境变量或部署平台 Secret。
- 日志默认不记录完整 Prompt、个人资料和 Tool 凭据。
- 任意外部写操作必须有明确工具白名单和用户确认。
- Skill 默认只能读取其声明的来源，不能访问任意本地文件或网络。
- AgentRun、ToolCall、Evidence 和发布决策需要审计 ID。

## 7. 部署

开发期可使用 Docker Compose 或本机 PostgreSQL，前后端分别热更新。首个线上版本优先选择静态前端 + 单个 Java 服务 + 托管 PostgreSQL；上线前必须补齐 Secret、CORS、健康检查、数据库备份和迁移验证，不做 Kubernetes。
