import { useState } from 'react'

import { ChatPage } from './conversation/ChatPage'
import { ConversationDrawer } from './conversation/ConversationDrawer'
import { useConversation } from './conversation/useConversation'
import { LearningPage } from './learning/LearningPage'
import { ReportOverlay } from './learning/ReportOverlay'
import { PrimaryNavigation } from './layout/PrimaryNavigation'
import type { PrimaryView } from './layout/PrimaryNavigation'
import { PulsePage } from './pulse/PulsePage'
import { SkillsDrawer } from './skills/SkillsDrawer'
import { useTheme } from './theme/useTheme'

function App() {
  const [view, setView] = useState<PrimaryView>('chat')
  const [managedSkillId, setManagedSkillId] = useState<string | null | undefined>(undefined)
  const [reportContentId, setReportContentId] = useState<string | null>(null)
  const [isHistoryOpen, setIsHistoryOpen] = useState(false)
  const conversationState = useConversation()
  const { theme, toggleTheme } = useTheme()
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
        onToggleTheme={toggleTheme}
        theme={theme}
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
                <button className="header-skill-button" onClick={() => setIsHistoryOpen(true)}><span>对话记录</span></button>
                <button className="header-skill-button" onClick={() => setManagedSkillId(null)}>持续任务</button>
                <span className={conversationState.error ? 'connection-status error' : 'connection-status'}>
                  <i />
                  {conversationState.isLoading ? '正在连接' : conversationState.error ? '连接中断' : '历史已同步'}
                </span>
              </div>
            </header>
            <ChatPage state={conversationState} onManageSkill={(skillId) => setManagedSkillId(skillId)} onOpenReport={setReportContentId} />
          </section>
        )}

        {view === 'learning' && <LearningPage />}
        {view === 'pulse' && <PulsePage onAsk={() => setView('chat')} onLearn={() => setView('learning')} />}
      </main>
      {isHistoryOpen && <ConversationDrawer state={conversationState} onClose={() => setIsHistoryOpen(false)} />}
      {managedSkillId !== undefined && <SkillsDrawer initialSkillId={managedSkillId} onClose={() => setManagedSkillId(undefined)} />}
      {reportContentId && <ReportOverlay contentId={reportContentId} onClose={() => setReportContentId(null)} />}
    </div>
  )
}

export default App
