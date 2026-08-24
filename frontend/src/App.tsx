import { useState } from 'react'
import type { FormEvent } from 'react'

type View = 'chat' | 'learning' | 'pulse'
type LearningType = 'cards' | 'article' | 'quiz'

type AgentEvent = {
  stage: string
  label: string
  status: string
}

type SkillDraft = {
  name: string
  schedule: string
  outputLength: string
  sourcePolicy: string[]
  requiresConfirmation: boolean
}

type PreviewResponse = {
  intent: string
  assistantMessage: string
  events: AgentEvent[]
  skillDraft: SkillDraft | null
}

const initialPreview: PreviewResponse = {
  intent: 'RECURRING_SKILL',
  assistantMessage:
    '这是一个持续性需求。我已确认可用来源，并生成一个可编辑的 Skill 草案。只有你确认后，它才会开始定时执行。',
  events: [
    { stage: 'PLANNING', label: '拆分 GitHub、官方文档与版本动态', status: 'COMPLETED' },
    { stage: 'COLLECTING', label: '找到 8 个候选来源', status: 'COMPLETED' },
    { stage: 'VERIFYING', label: '保留 3 个一手来源，排除聚合转载', status: 'COMPLETED' },
    { stage: 'PERSISTING', label: '生成结构化配置，等待确认', status: 'WAITING' },
  ],
  skillDraft: {
    name: 'Java Agent Weekly',
    schedule: 'FRIDAY 18:00',
    outputLength: '5_MIN_READ',
    sourcePolicy: ['PREFER_PRIMARY_SOURCE', 'VERIFY_IMPORTANT_CLAIMS'],
    requiresConfirmation: true,
  },
}

const navItems: Array<{ id: View; label: string }> = [
  { id: 'chat', label: '对话' },
  { id: 'learning', label: '学习' },
  { id: 'pulse', label: '动态' },
]

function App() {
  const [view, setView] = useState<View>('chat')
  const [learningType, setLearningType] = useState<LearningType>('cards')
  const [input, setInput] = useState('')
  const [userMessage, setUserMessage] = useState(
    '每周五整理 Java Agent 和 Spring AI 的重要变化，5 分钟读完，重要结论必须核对官方来源。',
  )
  const [preview, setPreview] = useState<PreviewResponse>(initialPreview)
  const [skillCreated, setSkillCreated] = useState(false)
  const [loading, setLoading] = useState(false)

  async function submitMessage(event: FormEvent) {
    event.preventDefault()
    const message = input.trim()
    if (!message || loading) return

    setUserMessage(message)
    setInput('')
    setLoading(true)
    setSkillCreated(false)

    try {
      const response = await fetch('/api/conversations/preview', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message }),
      })
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      setPreview((await response.json()) as PreviewResponse)
    } catch {
      setPreview({
        intent: 'BACKEND_UNAVAILABLE',
        assistantMessage: '暂时无法连接 Java 后端。请启动 backend 后再次发送，界面与本地编辑仍可继续。',
        events: [],
        skillDraft: null,
      })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand"><span className="brand-mark">L</span><span>LifeSkill</span></div>
        <button className="new-chat" onClick={() => setView('chat')}>＋ 新对话</button>
        <nav>
          {navItems.map((item) => (
            <button
              className={view === item.id ? 'nav-item active' : 'nav-item'}
              key={item.id}
              onClick={() => setView(item.id)}
            >
              {item.label}
            </button>
          ))}
        </nav>
        <div className="recent"><span>最近</span><button>Java Agent 入职准备</button><button>创建贵金属关注</button></div>
        <div className="profile"><span>S</span><small>水手</small></div>
      </aside>

      <main className="main">
        <header className="topbar"><strong>{view === 'chat' ? 'Java Agent 入职准备' : view === 'learning' ? '学习空间' : '你的动态'}</strong><span>⌕　···</span></header>
        {view === 'chat' && (
          <section className="chat-page">
            <div className="date">今天</div>
            <div className="user-message">{userMessage}</div>
            <div className="assistant-message">
              <span className="assistant-mark">✦</span>
              <div>
                <p>{loading ? '正在理解你的需求…' : preview.assistantMessage}</p>
                {preview.events.length > 0 && (
                  <details className="agent-flow" open>
                    <summary>搜索与核对过程</summary>
                    {preview.events.map((item) => (
                      <div className="agent-step" key={item.stage}>
                        <span className={item.status === 'COMPLETED' ? 'step-dot done' : 'step-dot'} />
                        <span><strong>{item.stage}</strong> · {item.label}</span>
                        <small>{item.status}</small>
                      </div>
                    ))}
                  </details>
                )}
                {preview.skillDraft && (
                  <article className="skill-draft">
                    <div className="skill-heading"><h2>{preview.skillDraft.name}</h2><span>{skillCreated ? '已创建并落库' : '待确认'}</span></div>
                    <dl>
                      <dt>执行时间</dt><dd>{preview.skillDraft.schedule}</dd>
                      <dt>输出长度</dt><dd>{preview.skillDraft.outputLength}</dd>
                      <dt>来源策略</dt><dd>{preview.skillDraft.sourcePolicy.join(' · ')}</dd>
                    </dl>
                    <div className="draft-actions"><button>调整设置</button><button className="primary" disabled={skillCreated} onClick={() => setSkillCreated(true)}>{skillCreated ? '已创建' : '创建 Skill'}</button></div>
                  </article>
                )}
              </div>
            </div>
            <form className="composer" onSubmit={submitMessage}>
              <textarea value={input} onChange={(event) => setInput(event.target.value)} placeholder="继续提问，或描述一个想持续关注的主题…" />
              <div><button type="button">＋</button><button className="send" disabled={loading} aria-label="发送">↑</button></div>
            </form>
          </section>
        )}

        {view === 'learning' && (
          <section className="learning-page">
            <aside className="folders"><h2>文件夹</h2>{['Java Agent', 'Spring AI', 'Agent 工程', '力量训练', '贵金属基础'].map((folder, index) => <button className={index === 0 ? 'selected' : ''} key={folder}>▱ {folder}</button>)}</aside>
            <div className="learning-content"><h1>Java Agent</h1><p>12 项内容 · 最近学习：工具调用控制</p><div className="content-tabs">{(['cards', 'article', 'quiz'] as LearningType[]).map((type) => <button className={learningType === type ? 'active' : ''} key={type} onClick={() => setLearningType(type)}>{type === 'cards' ? '学习卡片' : type === 'article' ? '文章阅读' : '测验'}</button>)}</div>
              {learningType === 'cards' && <div className="learning-list">{[['概念卡', 'premain 与 agentmain'], ['流程卡', '工具调用为什么由应用执行'], ['代码实践', '实现一个受控 ToolCallback'], ['复习', 'Instrumentation 本周复习']].map(([type, title]) => <button key={title}><span>{type}</span><strong>{title}</strong><small>打开 →</small></button>)}</div>}
              {learningType === 'article' && <article className="reader"><small>12 分钟阅读 · 2 个官方来源</small><h2>从一次工具调用理解 Agent Harness</h2><p>语言模型返回工具名称与参数，由应用校验并执行，再把结果送回模型。这个边界决定了权限、预算、超时和审计应该放在哪里。</p><p>LifeSkill 会保存每次工具调用的证据与发布决策，而不是让模型自行声明内容可靠。</p><footer>Spring AI Tool Calling · DeepSeek Tool Calls</footer></article>}
              {learningType === 'quiz' && <div className="quiz"><small>理解检查 · 第 1 / 3 题</small><h2>为什么不能让模型直接执行任意工具？</h2>{['模型生成速度不够快', '应用需要控制权限、参数、预算与审计', 'Java 不支持工具调用', '工具结果不能返回模型'].map((choice) => <button key={choice}>{choice}</button>)}</div>}
            </div>
          </section>
        )}

        {view === 'pulse' && (
          <section className="pulse-page"><h1>动态</h1><p>来自你的 Skill、学习计划和主动关注。</p><div className="pulse-tabs">{['全部', '技术前沿', '市场观察', '训练', '稍后阅读'].map((tab, index) => <button className={index === 0 ? 'active' : ''} key={tab}>{tab}</button>)}</div>{[
            ['Java Agent Weekly', 'Spring AI 工具执行边界发生变化', '3 个一手来源 · 核验通过'],
            ['贵金属基础', '黄金波动扩大，驱动因素仍存在来源冲突', '部分确认 · 已降低推送强度'],
            ['力量训练计划', '深蹲技术复习已准备好', '3 张动作卡 · 可信教学视频'],
          ].map(([source, title, meta]) => <article className="pulse-item" key={title}><small>{source}　·　{meta}</small><h2>{title}</h2><p>与你当前计划相关，可继续询问、查看证据或进入学习。</p><div><button onClick={() => setView('chat')}>询问</button><button onClick={() => setView('learning')}>学习</button></div></article>)}</section>
        )}
      </main>
    </div>
  )
}

export default App
