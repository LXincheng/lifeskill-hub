import { useEffect, useRef, useState } from 'react'
import type { FormEvent, KeyboardEvent } from 'react'

import { Icon } from '../components/Icon'
import type { IconName } from '../components/Icon'
import type { SkillDraft } from './conversationApi'
import type { ConversationState } from './useConversation'
import { ResearchRunPanel } from './ResearchRunPanel'
import { workspaceCopy } from '../copy'

type ChatPageProps = {
  state: ConversationState
  onManageSkill: (skillId: string) => void
  onOpenReport: (contentId: string) => void
}

const starterPrompts: Array<{ icon: IconName; tone: string; label: string; description: string; prompt: string }> = [
  { icon: 'search', tone: 'orange', label: '生成黄金报告', description: '读取 Goldhub 官方研究并生成专业报告', prompt: '帮我做一份9月份黄金市场研究报告，说明核心驱动、关键数据、风险和后续观察点' },
  { icon: 'globe', tone: 'green', label: '追踪信息动态', description: '持续关注主题，先生成可确认草案', prompt: '每周整理 Java Agent 前沿动态，关注 LangChain4j 和 Spring AI' },
  { icon: 'route', tone: 'blue', label: '创建学习路径', description: '把目标拆成循序渐进的学习步骤', prompt: '为我创建一个 Java Agent 开发学习路径' },
  { icon: 'zap', tone: 'purple', label: '规划购票辅助', description: '先核对真实工具与人工确认边界', prompt: '帮我盯着抢购上海正大乐影城9.6《奥德赛》的皇帝座电影票' },
]

const dayLabels: Record<string, string> = {
  MONDAY: '周一',
  TUESDAY: '周二',
  WEDNESDAY: '周三',
  THURSDAY: '周四',
  FRIDAY: '周五',
  SATURDAY: '周六',
  SUNDAY: '周日',
}

function formatSchedule(draft: SkillDraft) {
  return `每${dayLabels[draft.dayOfWeek] ?? draft.dayOfWeek} ${draft.time} · ${draft.timezone}`
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date(value))
}

function formatDate(value?: string) {
  const date = value ? new Date(value) : new Date()
  return new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'short' }).format(date)
}

export function ChatPage({ state, onManageSkill, onOpenReport }: ChatPageProps) {
  const [input, setInput] = useState('')
  const [waitSeconds, setWaitSeconds] = useState(0)
  const inputRef = useRef<HTMLTextAreaElement>(null)
  const scrollRef = useRef<HTMLDivElement>(null)
  const conversation = state.conversation
  const visibleMessages = conversation
    ? [...conversation.messages, ...(state.pendingMessage ? [state.pendingMessage] : [])]
    : []
  const pendingDraftCount = conversation?.skillDrafts.filter((draft) => draft.status === 'PENDING_CONFIRMATION').length ?? 0
  const hour = new Date().getHours()
  const greeting = hour < 12 ? '早上好' : hour < 18 ? '下午好' : '晚上好'

  useEffect(() => {
    if (!state.isSending) { setWaitSeconds(0); return }
    const startedAt = Date.now()
    const timer = window.setInterval(() => setWaitSeconds(Math.floor((Date.now() - startedAt) / 1000)), 1000)
    return () => window.clearInterval(timer)
  }, [state.isSending])

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }, [visibleMessages.length, state.isSending])

  function handleStarter(prompt: string) {
    setInput(prompt)
    requestAnimationFrame(() => inputRef.current?.focus())
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const content = input.trim()
    if (!content) return

    if (await state.sendMessage(content)) setInput('')
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
      event.preventDefault()
      event.currentTarget.form?.requestSubmit()
    }
  }

  return (
    <section className="chat-thread" aria-busy={state.isLoading || state.isSending}>
      <div className="chat-scroll" ref={scrollRef}>
        <div className="chat-content">
          {state.isLoading && !conversation && <div className="chat-status" role="status">正在恢复对话历史…</div>}

          {!state.isLoading && conversation && visibleMessages.length === 0 && (
            <div className="chat-empty">
              <header className="chat-greeting">
                <span>{workspaceCopy.consoleEyebrow} · {formatDate()}</span>
                <h1>{greeting}。{workspaceCopy.consoleTitle}</h1>
                <p>{workspaceCopy.consoleDescription}</p>
              </header>

              <div className="capability-console">{workspaceCopy.capabilities.map((capability) => <div key={capability.label}><span className={capability.tone}><i />{capability.status}</span><strong>{capability.label}</strong><small>{capability.detail}</small></div>)}</div>

              <div className="chat-stats" aria-label="当前会话状态">
                <div><strong>{conversation.messages.length}</strong><span>条消息</span></div>
                <div><strong>{pendingDraftCount}</strong><span>个待确认草案</span></div>
                <div><strong className={state.error ? 'danger' : 'success'}>{state.error ? '异常' : '已同步'}</strong><span>本地工作区</span></div>
              </div>

              <div className="starter-grid">
                {starterPrompts.map((starter) => (
                  <button key={starter.label} type="button" onClick={() => handleStarter(starter.prompt)}>
                    <span className={`starter-icon ${starter.tone}`}><Icon name={starter.icon} size={20} /></span>
                    <span><strong>{starter.label}</strong><small>{starter.description}</small></span>
                    <Icon name="arrow-right" size={17} />
                  </button>
                ))}
              </div>
            </div>
          )}

          {conversation && visibleMessages.length > 0 && (
            <div className="message-thread">
              <div className="date-separator"><i /><span>{formatDate(conversation.messages[0]?.createdAt)}</span><i /></div>
              {visibleMessages.map((message) => (
                <article className={`${message.role === 'USER' ? 'conversation-message user' : 'conversation-message assistant'}${message.id.startsWith('pending-') ? ' pending' : ''}`} key={message.id}>
                  <header>
                    <span className="message-avatar">{message.role === 'USER' ? 'L' : 'AI'}</span>
                    <span>{message.role === 'USER' ? formatTime(message.createdAt) : `LifeSkill · ${formatTime(message.createdAt)}`}</span>
                  </header>
                  <p>{message.content}</p>
                  {message.processingSteps.length > 0 && (
                    <details className="agent-receipt">
                      <summary><Icon name="activity" size={15} />执行过程 · {message.durationMs ?? 0} ms</summary>
                      <ol>{message.processingSteps.map((step, index) => (
                        <li className={step.status.toLowerCase()} key={`${step.stage}-${index}`}>
                          <i>{step.status === 'COMPLETED' ? <Icon name="check" size={12} /> : step.status === 'BLOCKED' ? <Icon name="shield" size={12} /> : <Icon name="alert-circle" size={12} />}</i>
                          <span><strong>{step.label}</strong><small>{step.detail}{step.durationMs > 0 ? ` · ${step.durationMs} ms` : ''}</small></span>
                        </li>
                      ))}</ol>
                    </details>
                  )}
                  {message.agentRunId && <ResearchRunPanel runId={message.agentRunId} onOpenReport={onOpenReport} />}
                </article>
              ))}

              {conversation.skillDrafts.map((draft) => (
                <article className="skill-draft" key={draft.id}>
                  <i className="skill-draft-accent" />
                  <header className="skill-draft-header">
                    <span className="skill-draft-icon"><Icon name="zap" size={16} /></span>
                    <span><strong>{draft.title}</strong><small>Skill 草案 · {draft.status === 'CONFIRMED' ? '已确认落库' : '待你确认'}</small></span>
                    <em className={draft.status === 'CONFIRMED' ? 'confirmed' : ''}>
                      {draft.status === 'CONFIRMED' ? '已创建' : '尚未创建'}
                    </em>
                  </header>
                  <dl>
                    <div><dt>目标</dt><dd>{draft.objective}</dd></div>
                    <div><dt>频率</dt><dd>{formatSchedule(draft)}</dd></div>
                    <div><dt>状态</dt><dd>{draft.status === 'CONFIRMED' ? 'Skill 已启用' : '等待用户确认'}</dd></div>
                  </dl>
                  {draft.status === 'PENDING_CONFIRMATION' ? (
                    <div className="skill-draft-actions">
                      <span><Icon name="shield" size={15} />确认后才会创建，不会重复落库</span>
                      <button
                        disabled={state.confirmingDraftId !== null}
                        onClick={() => void state.confirmDraft(draft.id)}
                        type="button"
                      >{state.confirmingDraftId === draft.id ? '正在确认…' : '确认创建 Skill'}</button>
                    </div>
                  ) : (
                    <footer className="confirmed"><span><Icon name="check-circle" size={15} />已创建 Skill · 版本 1 · 当前为启用状态</span>{draft.confirmedSkillId && <button className="secondary-button" onClick={() => onManageSkill(draft.confirmedSkillId!)}>查看并运行</button>}</footer>
                  )}
                </article>
              ))}
            </div>
          )}

          {state.isSending && <div className="sending-status" role="status"><span className="sending-pulse" /><span><strong>等待服务返回</strong><small>消息已提交，正在进行结构化分析 · {waitSeconds} 秒</small></span></div>}
          {state.error && (
            <div className="chat-error" role="alert"><span>{state.error}</span><button type="button" onClick={() => void state.reloadConversation()}>重试</button></div>
          )}
        </div>
      </div>

      <div className="composer-dock">
        <form className="composer" onSubmit={handleSubmit}>
          <textarea
            aria-label="消息内容"
            disabled={!conversation || state.isLoading}
            maxLength={4000}
            onChange={(event) => setInput(event.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="描述你的学习目标、研究任务或信息需求……"
            ref={inputRef}
            rows={1}
            value={input}
          />
          <button
            aria-label="发送"
            className="send"
            disabled={!conversation || state.isLoading || state.isSending || !input.trim()}
          ><Icon name="send" size={17} /></button>
        </form>
        <p>Ctrl + Enter 发送 · 可创建学习计划、长期 Skill 或一次性研究任务 · {input.length}/4000</p>
      </div>
    </section>
  )
}
