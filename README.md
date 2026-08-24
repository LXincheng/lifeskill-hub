# LifeSkill Hub

LifeSkill Hub 是一个以聊天为入口的个人能力中枢。用户通过自然语言创建计划、执行任务、沉淀 Skill、持续关注感兴趣的动态，并把可靠信息转化为学习卡片、文章、测验和实践。

当前仓库已完成 M1.2 结构化 Skill 草案代码，正在沿下面的 MVP 闭环迭代：

```text
聊天表达需求
  → 搜索与来源核验
  → 用户确认创建 Skill
  → 定时执行与动态卡片
  → 进入结构化学习
  → 记录进度与复习
```

## 技术栈

- Web：React 19、TypeScript、Vite
- Backend：Java 21、Spring Boot 4、Spring AI 2
- Model：DeepSeek API
- Database：PostgreSQL、Flyway
- Delivery：Docker、PWA（后续迭代）

核心后端使用 Java，以贴合实际团队技术栈并训练企业级后端与 Agent Harness 能力。TypeScript 负责 Web/PWA 和后续移动端体验。

## 仓库结构

```text
lifeskill-hub/
├── AGENTS.md                 # Codex/新窗口必须先读的项目约束
├── docs/
│   ├── PRODUCT.md           # 产品目标、核心功能、非目标
│   ├── ARCHITECTURE.md      # 系统、Agent 与数据架构
│   ├── ENGINEERING_STANDARDS.md # 命名、边界与交付规范
│   ├── DEVELOPMENT_PLAN.md  # MVP 迭代路径和验收标准
│   └── PROJECT_CONTEXT.md   # 新对话快速接续上下文
├── frontend/                # React + TypeScript
├── backend/                 # Spring Boot + Spring AI
└── compose.yaml             # 本地 PostgreSQL
```

## 本地启动

### 1. 数据库

```bash
docker compose up -d postgres
```

### 2. 后端

Windows：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

后端默认运行在 `http://localhost:8080`，状态接口为 `GET /api/status`。

当前对话 API：

- `POST /api/conversations`：创建对话
- `POST /api/conversations/{id}/messages`：保存消息、识别意图并按需生成 Skill 草案
- `GET /api/conversations/{id}`：读取对话历史和待确认 Skill 草案

### 3. 前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，开发服务器会把 `/api` 代理到 Java 后端。

## 环境变量

复制 `.env.example` 并按需配置：

- `DEEPSEEK_API_KEY`：DeepSeek API Key
- `LIFESKILL_MODEL_ENABLED`：设置为 `true` 后启用 DeepSeek；默认关闭并使用安全降级
- `DB_URL`：JDBC 数据库地址
- `DB_USERNAME`：数据库用户名
- `DB_PASSWORD`：数据库密码

不要提交真实密钥。

## 开始开发前

新成员或新 Codex 对话应按顺序阅读：

1. [AGENTS.md](./AGENTS.md)
2. [docs/PROJECT_CONTEXT.md](./docs/PROJECT_CONTEXT.md)
3. [docs/PRODUCT.md](./docs/PRODUCT.md)
4. [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md)
5. [docs/ENGINEERING_STANDARDS.md](./docs/ENGINEERING_STANDARDS.md)
6. [docs/DEVELOPMENT_PLAN.md](./docs/DEVELOPMENT_PLAN.md)

## License

[Apache License 2.0](./LICENSE)
