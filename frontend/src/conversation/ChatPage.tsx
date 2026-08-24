import { useState } from 'react'
import type { FormEvent, KeyboardEvent } from 'react'

import { Icon } from '../components/Icon'
import type { ConversationState } from './useConversation'
import type { SkillDraft } from './conversationApi'

type ChatPageProps = {
  state: ConversationState
}

const starterPrompts = [
  {
    icon: 'activity' as const,
    label: '建立持续关注',
    prompt: '每周五整理 Java Agent 前沿，并核对官方来源',
  },
  {
    icon: 'book' as const,
    label: '拆解学习目标',
    prompt: '帮我制定四周的 Spring AI 学习路线',
  },
  {
    icon: 'compass' as const,
    label: '推进手头任务',
    prompt: '把一个模糊目标拆成今天能开始的步骤',
  },
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

export function ChatPage({ state }: ChatPageProps) {
  const [input, setInput] = useState('')

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const content = input.trim()
    if (!content) return

    if (await state.sendMessage(content)) {
      setInput('')
    }
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
      event.preventDefault()
      event.currentTarget.form?.requestSubmit()
    }
  }

  return (
    <section className="chat-page" aria-busy={state.isLoading || state.isSending}>
      {state.isLoading && !state.conversation && (
        <div className="chat-status" role="status">正在恢复对话历史…</div>
      )}

      {!state.isLoading && state.conversation?.messages.length === 0 && (
        <div className="chat-empty">
          <div className="chat-eyebrow"><Icon name="sparkles" size={15} /> 对话，是所有能力的起点</div>
          <h1>今天，想推进什么？</h1>
          <p className="chat-intro">描述一个目标、任务或持续兴趣。LifeSkill 会先记住上下文，再逐步把它变成可执行、可确认的能力。</p>
          <div className="starter-grid">
            {starterPrompts.map((starter) => (
              <button key={starter.label} type="button" onClick={() => setInput(starter.prompt)}>
                <span className="starter-icon"><Icon name={starter.icon} size={18} /></span>
                <span><strong>{starter.label}</strong><small>{starter.prompt}</small></span>
                <Icon name="arrow-right" size={16} />
              </button>
            ))}
          </div>
          <div className="trust-note"><Icon name="check" size={15} /> 对话历史自动保存，长期任务创建前会先请你确认</div>
        </div>
      )}

      {state.conversation && state.conversation.messages.length > 0 && (
        <div className="message-list">
          {state.conversation.messages.map((message) => (
            <article
              className={message.role === 'USER' ? 'user-message' : 'assistant-message'}
              key={message.id}
            >
              {message.role !== 'USER' && <span className="assistant-mark"><Icon name="sparkles" size={16} /></span>}
              <p>{message.content}</p>
            </article>
          ))}
          {state.conversation.skillDrafts.map((draft) => (
            <article className="skill-draft" key={draft.id}>
              <header>
                <span className="skill-draft-kicker"><i /> Skill 草案</span>
                <span>尚未创建</span>
              </header>
              <h2>{draft.title}</h2>
              <p>{draft.objective}</p>
              <dl>
                <div><dt>执行频率</dt><dd>{formatSchedule(draft)}</dd></div>
                <div><dt>当前状态</dt><dd>等待你确认</dd></div>
              </dl>
              <footer><Icon name="check" size={14} /> 只有确认后，系统才会创建并运行这个 Skill</footer>
            </article>
          ))}
        </div>
      )}

      {state.isSending && <div className="sending-status" role="status">正在保存消息…</div>}

      {state.error && (
        <div className="chat-error" role="alert">
          <span>{state.error}</span>
          <button type="button" onClick={() => void state.reloadConversation()}>重试</button>
        </div>
      )}

      <form className="composer" onSubmit={handleSubmit}>
        <textarea
          aria-label="消息内容"
          maxLength={4000}
          value={input}
          onChange={(event) => setInput(event.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="描述一个任务、学习目标或想持续关注的主题…"
          disabled={!state.conversation || state.isLoading}
        />
        <div>
          <span className="composer-hint">Ctrl + Enter 发送 · {input.length}/4000</span>
          <button
            className="send"
            disabled={!state.conversation || state.isLoading || state.isSending || !input.trim()}
            aria-label="发送"
          ><Icon name="send" size={16} strokeWidth={1.9} /></button>
        </div>
      </form>
    </section>
  )
}
