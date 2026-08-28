import { useState } from 'react'

import { Icon } from '../components/Icon'
import type { ConversationState } from './useConversation'

export function ConversationDrawer({ state, onClose }: { state: ConversationState; onClose: () => void }) {
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null)

  return <div className="drawer-scrim" onMouseDown={onClose}>
    <aside className="workspace-drawer conversation-drawer" onMouseDown={(event) => event.stopPropagation()}>
      <header>
        <div><small>CONVERSATIONS</small><h2>对话记录</h2><p>研究任务、学习计划与长期关注都从这里恢复。</p></div>
        <button aria-label="关闭" className="icon-button" onClick={onClose}><Icon name="x" /></button>
      </header>
      <button className="drawer-primary-action" onClick={() => { void state.startNewConversation(); onClose() }}>
        <Icon name="plus" size={17} />新建对话
      </button>
      <div className="conversation-history-list">
        {state.conversations.map((item) => <article className={state.conversation?.id === item.id ? 'active' : ''} key={item.id}>
          <button className="conversation-history-open" onClick={() => { void state.selectConversation(item.id); onClose() }}>
            <span><strong>{item.title}</strong><small>{item.messageCount} 条消息 · {new Intl.DateTimeFormat('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(item.updatedAt))}</small></span>
            <Icon name="chevron-right" size={16} />
          </button>
          <button aria-label="删除对话" className={deleteTarget === item.id ? 'history-delete armed' : 'history-delete'} onClick={() => {
            if (deleteTarget !== item.id) { setDeleteTarget(item.id); return }
            void state.removeConversation(item.id)
            setDeleteTarget(null)
          }}><Icon name="trash" size={14} /></button>
        </article>)}
        {!state.conversations.length && <div className="drawer-empty"><Icon name="message" /><span>还没有历史对话</span></div>}
      </div>
    </aside>
  </div>
}
