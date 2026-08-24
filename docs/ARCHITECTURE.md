# 架构说明

## 1. 总体架构

```text
React Web / PWA
        ↓ REST / SSE
Spring Boot 模块化单体
├── conversation   对话、意图和流式事件
├── plan           计划与任务
├── skill          Skill 定义、版本和调度
├── agent          Runtime、角色、Harness、Policy Gate
├── evidence       来源、Claim 和核验
├── learning       文件夹、内容块、测验和进度
├── pulse          动态聚合与用户标签
├── notification   应用内/Web Push/外部渠道
└── integration    DeepSeek、GitHub、RSS、行情等适配器
        ↓
PostgreSQL / DeepSeek API / 外部事实 API
```

## 2. Agent 设计

MVP 不部署多个自治服务。Planner、Researcher、Verifier、Composer 是同一 Runtime 中的角色化步骤，可以使用同一个 DeepSeek 模型的独立调用和上下文。

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
- 独立上下文的语义核验

### 2.2 Java Harness 负责

- 工具注册、参数校验和真实执行
- 最大步骤、超时、重试和费用预算
- 来源策略和证据持久化
- JSON Schema 与业务规则校验
- 幂等、审计和失败恢复
- 外部写操作的用户确认
- 最终发布、降级或拦截

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

## 4. 建议数据表

MVP 第一批：

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
- 所有长期写操作支持幂等键。
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

开发期使用 Docker Compose 启动 PostgreSQL，前后端分别热更新。首个线上版本优先选择简单平台：静态前端 + 单个 Java 服务 + 托管 PostgreSQL，不做 Kubernetes。
