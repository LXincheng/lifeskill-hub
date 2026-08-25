import { useState } from 'react'
import type { FormEvent, KeyboardEvent } from 'react'

import { Icon } from '../components/Icon'
import type { IconName } from '../components/Icon'
import type { SkillDraft } from './conversationApi'
import type { ConversationState } from './useConversation'

type ChatPageProps = {
  state: ConversationState
}

const starterPrompts: Array<{ icon: IconName; label: string; prompt: string }> = [
  { icon: 'activity', label: '建立持续关注', prompt: '每周五整理 Java Agent 前沿，并核对官方来源' },
  { icon: 'book', label: '拆解学习目标', prompt: '帮我制定四周的 Spring AI 学习路线' },
  { icon: 'target', label: '推进手头任务', prompt: '把一个模糊目标拆成今天能开始的步骤' },
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

export function ChatPage({ state }: ChatPageProps) {
  const [input, setInput] = useState('')
  const conversation = state.conversation

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
      <div className="chat-scroll">
        <div className="chat-content">
          {state.isLoading && !conversation && <div className="chat-status" role="status">正在恢复对话历史…</div>}

          {!state.isLoading && conversation?.messages.length === 0 && (
            <div className="chat-empty">
              <span className="chat-eyebrow"><Icon name="sparkles" size={16} />对话，是所有能力的起点</span>
              <h1>今天，想推进什么？</h1>
              <p>描述一个目标、任务或持续兴趣。LifeSkill 会保存上下文，再把它逐步转成可执行、可确认的能力。</p>
              <div className="starter-grid">
                {starterPrompts.map((starter) => (
                  <button key={starter.label} type="button" onClick={() => setInput(starter.prompt)}>
                    <span className="starter-icon"><Icon name={starter.icon} size={19} /></span>
                    <span><strong>{starter.label}</strong><small>{starter.prompt}</small></span>
                    <Icon name="arrow-right" size={17} />
                  </button>
                ))}
              </div>
              <span className="trust-note"><Icon name="shield" size={15} />对话自动保存，长期任务创建前会先请你确认</span>
            </div>
          )}

          {conversation && conversation.messages.length > 0 && (
            <div className="message-thread">
              <div className="date-separator"><i /><span>{formatDate(conversation.messages[0]?.createdAt)}</span><i /></div>
              {conversation.messages.map((message) => (
                <article className={message.role === 'USER' ? 'conversation-message user' : 'conversation-message assistant'} key={message.id}>
                  <header>
                    <span className="message-avatar">{message.role === 'USER' ? 'L' : 'AI'}</span>
                    <span>{message.role === 'USER' ? formatTime(message.createdAt) : `LifeSkill · ${formatTime(message.createdAt)}`}</span>
                  </header>
                  <p>{message.content}</p>
                </article>
              ))}

              {conversation.skillDrafts.map((draft) => (
                <article className="skill-draft" key={draft.id}>
                  <i className="skill-draft-accent" />
                  <header className="skill-draft-header">
                    <span className="skill-draft-icon"><Icon name="zap" size={16} /></span>
                    <span><strong>{draft.title}</strong><small>Skill 草案 · 待你确认</small></span>
                    <em>尚未创建</em>
                  </header>
                  <dl>
                    <div><dt>目标</dt><dd>{draft.objective}</dd></div>
                    <div><dt>频率</dt><dd>{formatSchedule(draft)}</dd></div>
                    <div><dt>状态</dt><dd>等待用户确认</dd></div>
                  </dl>
                  <footer><Icon name="shield" size={15} />只有确认后，系统才会创建并运行这个 Skill</footer>
                </article>
              ))}
            </div>
          )}

          {state.isSending && <div className="sending-status" role="status">正在保存并分析消息…</div>}
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
