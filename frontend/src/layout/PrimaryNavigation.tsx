import { Icon } from '../components/Icon'
import type { IconName } from '../components/Icon'
import { workspaceCopy } from '../copy'

export type PrimaryView = 'chat' | 'learning' | 'pulse'

type PrimaryNavigationProps = {
  activeView: PrimaryView
  isBusy: boolean
  onNavigate: (view: PrimaryView) => void
  onNewConversation: () => void
}

const navigationItems: Array<{ id: PrimaryView; label: string; detail: string; icon: IconName }> = [
  { id: 'chat', label: '控制台', detail: '对话与 Agent', icon: 'message' },
  { id: 'pulse', label: '世界动态', detail: '已核验情报', icon: 'globe' },
  { id: 'learning', label: '学习空间', detail: '计划与知识库', icon: 'book' },
]

export function PrimaryNavigation({
  activeView,
  isBusy,
  onNavigate,
  onNewConversation,
}: PrimaryNavigationProps) {
  return (
    <>
      <aside className="primary-sidebar">
        <div className="brand-lockup"><span className="brand-mark"><Icon name="sparkles" size={18} /></span><span><strong>{workspaceCopy.productName}</strong><small>{workspaceCopy.productTagline}</small></span></div>
        <button
          aria-label="新对话"
          className="sidebar-action sidebar-new"
          disabled={isBusy}
          onClick={onNewConversation}
          title="新对话"
        >
          <Icon name="plus" size={20} />
          <span><strong>新建对话</strong><small>开始研究或学习</small></span>
        </button>
        <nav className="primary-nav" aria-label="主导航">
          {navigationItems.map((item) => (
            <button
              aria-current={activeView === item.id ? 'page' : undefined}
              className={activeView === item.id ? 'primary-nav-item active' : 'primary-nav-item'}
              key={item.id}
              onClick={() => onNavigate(item.id)}
              title={item.label}
            >
              <Icon name={item.icon} size={20} />
              <span><strong>{item.label}</strong><small>{item.detail}</small></span>
            </button>
          ))}
        </nav>
        <div className="sidebar-footer" aria-label="本地工作区"><span className="workspace-status" /><span><strong>Local workspace</strong><small>PostgreSQL · DeepSeek</small></span></div>
      </aside>

      <nav className="mobile-navigation" aria-label="主导航">
        {navigationItems.map((item) => (
          <button
            aria-current={activeView === item.id ? 'page' : undefined}
            className={activeView === item.id ? 'active' : ''}
            key={item.id}
            onClick={() => onNavigate(item.id)}
          >
            <Icon name={item.icon} size={21} />
            <span>{item.label}</span>
          </button>
        ))}
      </nav>
    </>
  )
}
