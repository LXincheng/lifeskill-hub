import { useState } from 'react'
import type { FormEvent } from 'react'

import type { ConversationState } from './useConversation'

type ChatPageProps = {
  state: ConversationState
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

  return (
    <section className="chat-page" aria-busy={state.isLoading || state.isSending}>
      {state.isLoading && !state.conversation && (
        <div className="chat-status" role="status">正在恢复对话历史…</div>
      )}

      {!state.isLoading && state.conversation?.messages.length === 0 && (
        <div className="chat-empty">
          <span className="assistant-mark">✦</span>
          <h1>从一个真实需求开始</h1>
          <p>消息会保存到对话历史。下一阶段再由模型理解意图并生成可确认的 Skill 草案。</p>
        </div>
      )}

      {state.conversation && state.conversation.messages.length > 0 && (
        <div className="message-list">
          {state.conversation.messages.map((message) => (
            <article
              className={message.role === 'USER' ? 'user-message' : 'assistant-message'}
              key={message.id}
            >
              {message.role !== 'USER' && <span className="assistant-mark">✦</span>}
              <p>{message.content}</p>
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
          placeholder="描述一个任务、学习目标或想持续关注的主题…"
          disabled={!state.conversation || state.isLoading}
        />
        <div>
          <span className="composer-hint">{input.length}/4000</span>
          <button
            className="send"
            disabled={!state.conversation || state.isLoading || state.isSending || !input.trim()}
            aria-label="发送"
          >↑</button>
        </div>
      </form>
    </section>
  )
}
