import { useState } from 'react'

import { ChatPage } from './conversation/ChatPage'
import { useConversation } from './conversation/useConversation'
import { LearningPage } from './learning/LearningPage'
import { PrimaryNavigation } from './layout/PrimaryNavigation'
import type { PrimaryView } from './layout/PrimaryNavigation'
import { PulsePage } from './pulse/PulsePage'
import { SkillsDrawer } from './skills/SkillsDrawer'

function App() {
  const [view, setView] = useState<PrimaryView>('chat')
  const [managedSkillId, setManagedSkillId] = useState<string | null | undefined>(undefined)
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
                <span>{conversation?.title ?? '新对话'}</span>
              </div>
              <div className="chat-header-actions">
                <button className="header-skill-button" onClick={() => setManagedSkillId(null)}>Skills</button>
                <span className={conversationState.error ? 'connection-status error' : 'connection-status'}>
                  <i />
                  {conversationState.isLoading ? '正在连接' : conversationState.error ? '连接中断' : '历史已同步'}
                </span>
              </div>
            </header>
            <ChatPage state={conversationState} onManageSkill={(skillId) => setManagedSkillId(skillId)} />
          </section>
        )}

        {view === 'learning' && <LearningPage />}
        {view === 'pulse' && <PulsePage onAsk={() => setView('chat')} onLearn={() => setView('learning')} />}
      </main>
      {managedSkillId !== undefined && <SkillsDrawer initialSkillId={managedSkillId} onClose={() => setManagedSkillId(undefined)} />}
    </div>
  )
}

export default App
