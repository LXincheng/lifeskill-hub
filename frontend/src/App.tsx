import { useState } from 'react'

import { Icon } from './components/Icon'
import { ChatPage } from './conversation/ChatPage'
import { useConversation } from './conversation/useConversation'
import { LearningPage } from './learning/LearningPage'
import { PrimaryNavigation } from './layout/PrimaryNavigation'
import type { PrimaryView } from './layout/PrimaryNavigation'
import { PulsePage } from './pulse/PulsePage'

function App() {
  const [view, setView] = useState<PrimaryView>('chat')
  const conversationState = useConversation()
  const conversation = conversationState.conversation

  function handleNewConversation() {
    setView('chat')
    void conversationState.startNewConversation()
  }

  return (
    <div className="app-shell">
      <PrimaryNavigation
        activeView={view}
        isBusy={conversationState.isLoading || conversationState.isSending}
        onNavigate={setView}
        onNewConversation={handleNewConversation}
      />

      <main className="app-main">
        {view === 'chat' && (
          <section className="chat-view">
            <header className="chat-header">
              <div className="chat-header-title">
                <strong>对话</strong>
                <i />
                <span>{conversation?.title ?? '新对话'}</span>
              </div>
              <div className="chat-header-actions">
                <span className={conversationState.error ? 'connection-status error' : 'connection-status'}>
                  <i />
                  {conversationState.isLoading ? '正在连接' : conversationState.error ? '连接中断' : '历史已同步'}
                </span>
                <button
                  aria-label="新对话"
                  className="header-icon-button mobile-new-chat"
                  disabled={conversationState.isLoading || conversationState.isSending}
                  onClick={handleNewConversation}
                ><Icon name="plus" size={18} /></button>
              </div>
            </header>

            <div className="chat-layout">
              <ChatPage state={conversationState} />
              <aside className="conversation-panel">
                <section>
                  <div className="panel-heading"><span>会话状态</span><Icon name="activity" size={14} /></div>
                  <dl className="conversation-facts">
                    <div><dt>消息</dt><dd>{conversation?.messages.length ?? 0}</dd></div>
                    <div><dt>待确认草案</dt><dd>{conversation?.skillDrafts.filter((draft) => draft.status === 'PENDING_CONFIRMATION').length ?? 0}</dd></div>
                    <div><dt>存储</dt><dd className={conversationState.error ? '' : 'success'}>{conversationState.isLoading ? '连接中' : conversationState.error ? '同步失败' : '已同步'}</dd></div>
                  </dl>
                </section>
                <section>
                  <div className="panel-heading"><span>能力边界</span><Icon name="shield" size={14} /></div>
                  <div className="boundary-list">
                    <p><Icon name="check-circle" size={15} /><span><strong>对话会保存</strong><small>刷新后继续当前上下文</small></span></p>
                    <p><Icon name="check-circle" size={15} /><span><strong>草案需确认</strong><small>不会静默创建长期 Skill</small></span></p>
                    <p className="muted"><Icon name="clock" size={15} /><span><strong>来源检索待接入</strong><small>不展示未经核验的搜索结论</small></span></p>
                  </div>
                </section>
              </aside>
            </div>
          </section>
        )}

        {view === 'learning' && <LearningPage />}
        {view === 'pulse' && <PulsePage onAsk={() => setView('chat')} onLearn={() => setView('learning')} />}
      </main>
    </div>
  )
}

export default App
