# LifeSkill Console

LifeSkill Console（仓库名保留 `lifeskill-hub`）是一个以聊天为入口的个人研究与学习控制台。用户通过自然语言创建计划、执行任务、沉淀 Skill、持续关注感兴趣的动态，并把可靠信息转化为学习路径、文章、测验和实践记录。

当前仓库已完成两条可用的 M2 纵向闭环：`Java Agent Weekly` 从 Spring AI 官方 Release 生成可靠动态与学习内容；一次性黄金研究从 World Gold Council 官方研究生成带 Evidence ID 的核验报告。电影票库存、锁座和支付尚未接入，系统会明确拦截而不会伪装执行。

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
│   ├── DESIGN_SYSTEM.md      # 视觉令牌、图标和响应式约束
│   └── PROJECT_CONTEXT.md   # 新对话快速接续上下文
├── frontend/                # React + TypeScript
├── backend/                 # Spring Boot + Spring AI
└── compose.yaml             # 本地 PostgreSQL
```

## 本地启动

Windows 可直接双击根目录 `start.cmd`。在 VS Code 中打开仓库后，也可以打开集成终端（PowerShell）直接运行：

```powershell
.\start.ps1
```

启动器会检查 PostgreSQL、`.env`、DeepSeek 配置，自动执行 Flyway、按需启动前后端并打开浏览器。看到 `LifeSkill Hub is ready: http://localhost:5173` 即可使用。日志保存在被 Git 忽略的 `.lifeskill-runtime/`。停止开发服务可运行 `.\stop.ps1`。

下面仍保留分步启动方式，便于排查单个服务。

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

核心 API：

- `POST /api/conversations`：创建对话
- `GET /api/conversations`、`DELETE /api/conversations/{id}`：恢复和管理对话记录
- `POST /api/conversations/{id}/messages`：保存消息、识别意图并按需生成 Skill 草案
- `GET /api/conversations/{id}`：读取对话历史和待确认 Skill 草案
- `GET /api/skills`、`PATCH /api/skills/{id}`：管理、暂停、恢复和修改 Skill
- `POST /api/skills/{id}/runs`：异步启动受控 AgentRun
- `GET /api/skill-runs/{id}`、`GET /api/skill-runs/{id}/events`：读取结果和 SSE 事件
- `GET /api/pulse-items`、`GET /api/pulse-items/{id}/evidence`：读取可靠动态和 Evidence
- `POST /api/pulse-items/{id}/learning-folder`：幂等生成学习文件夹、路径、文章和测验
- `GET /api/learning-folders/{id}/progress`：读取学习文件夹进度
- `POST /api/content-items/{id}/attempts`：保存路径进度、阅读完成或测验结果
- `GET/POST/DELETE /api/content-items/{id}/annotations`：保存重点与反馈
- `POST /api/content-items/{id}/regenerations`：根据已保存反馈重生成 AI 学习内容

### 3. 前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，开发服务器会把 `/api` 代理到 Java 后端。

## 环境变量

仓库根目录已经预留本地 `.env`（Git 会忽略它）。如需重新创建，可复制 `.env.example`。填写：

- `DEEPSEEK_API_KEY`：DeepSeek API Key
- `LIFESKILL_MODEL_ENABLED`：填写 Key 后改为 `true` 才会启用 DeepSeek；默认关闭并使用安全降级
- `DB_URL`：JDBC 数据库地址
- `DB_USERNAME`：数据库用户名
- `DB_PASSWORD`：数据库密码

不要提交真实密钥。

启用真实对话前还需要：

1. 安装并启动 Docker Desktop，然后在仓库根目录执行 `docker compose up -d postgres`。
2. 在根目录 `.env` 中填写 `DEEPSEEK_API_KEY`，并设置 `LIFESKILL_MODEL_ENABLED=true`。
3. 分别启动后端和前端。后端会自动读取根目录 `.env`，Flyway 会自动创建或升级 PostgreSQL 表。

不填写 Key 时，对话和数据 CRUD 仍可运行，但模型会安全降级，不会生成真实 AI 回答或 Skill 草案。

## 开始开发前

新成员或新 Codex 对话应按顺序阅读：

1. [AGENTS.md](./AGENTS.md)
2. [docs/PROJECT_CONTEXT.md](./docs/PROJECT_CONTEXT.md)
3. [docs/PRODUCT.md](./docs/PRODUCT.md)
4. [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md)
5. [docs/ENGINEERING_STANDARDS.md](./docs/ENGINEERING_STANDARDS.md)
6. [docs/DEVELOPMENT_PLAN.md](./docs/DEVELOPMENT_PLAN.md)
7. [docs/DESIGN_SYSTEM.md](./docs/DESIGN_SYSTEM.md)

## License

[Apache License 2.0](./LICENSE)
