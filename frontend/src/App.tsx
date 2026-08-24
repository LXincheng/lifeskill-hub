import { useState } from 'react'

import { Icon } from './components/Icon'
import { ChatPage } from './conversation/ChatPage'
import { ConversationContextPanel } from './conversation/ConversationContextPanel'
import { useConversation } from './conversation/useConversation'

type View = 'chat' | 'learning' | 'pulse'
type LearningType = 'cards' | 'article' | 'quiz'

const navItems: Array<{ id: View; label: string; icon: 'message' | 'book' | 'activity' }> = [
  { id: 'chat', label: '对话', icon: 'message' },
  { id: 'learning', label: '学习', icon: 'book' },
  { id: 'pulse', label: '动态', icon: 'activity' },
]

function App() {
  const [view, setView] = useState<View>('chat')
  const [learningType, setLearningType] = useState<LearningType>('cards')
  const conversationState = useConversation()

  function handleNewConversation() {
    setView('chat')
    void conversationState.startNewConversation()
  }

  const pageTitle = view === 'chat'
    ? conversationState.conversation?.title ?? '对话'
    : view === 'learning' ? '学习空间' : '你的动态'

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand" title="LifeSkill Hub"><span className="brand-mark"><Icon name="sparkles" size={17} /></span></div>
        <button
          className="new-chat"
          aria-label="新对话"
          disabled={conversationState.isLoading || conversationState.isSending}
          onClick={handleNewConversation}
        ><Icon name="plus" size={18} /><span>新建</span></button>
        <nav>
          {navItems.map((item) => (
            <button
              className={view === item.id ? 'nav-item active' : 'nav-item'}
              key={item.id}
              onClick={() => setView(item.id)}
            >
              <Icon name={item.icon} size={18} /> <span>{item.label}</span>
            </button>
          ))}
        </nav>
        <div className="profile" title="本地空间"><span>L</span></div>
      </aside>

      <main className="main">
        <header className="topbar">
          <div className="page-identity"><strong>{view === 'chat' ? '对话' : pageTitle}</strong>{view === 'chat' && <><i /> <span>{pageTitle}</span></>}</div>
          <span className={conversationState.error ? 'sync-status error' : 'sync-status'}>
            <i />{conversationState.isLoading ? '正在连接' : conversationState.error ? '连接中断' : '历史已同步'}
          </span>
        </header>
        {view === 'chat' && (
          <div className="chat-workspace">
            <ChatPage state={conversationState} />
            <ConversationContextPanel state={conversationState} />
          </div>
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
