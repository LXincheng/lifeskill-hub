import { Icon } from '../components/Icon'
import type { IconName } from '../components/Icon'

export type PrimaryView = 'chat' | 'learning' | 'pulse'

type PrimaryNavigationProps = {
  activeView: PrimaryView
  isBusy: boolean
  onNavigate: (view: PrimaryView) => void
  onNewConversation: () => void
}

const navigationItems: Array<{ id: PrimaryView; label: string; icon: IconName }> = [
  { id: 'chat', label: '对话', icon: 'message' },
  { id: 'learning', label: '学习', icon: 'book' },
  { id: 'pulse', label: '动态', icon: 'rss' },
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
        <div className="brand-mark" title="LifeSkill Hub">LS</div>
        <button
          aria-label="新对话"
          className="sidebar-action sidebar-new"
          disabled={isBusy}
          onClick={onNewConversation}
          title="新对话"
        >
          <Icon name="plus" size={20} />
          <span>新建</span>
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
              <span>{item.label}</span>
            </button>
          ))}
        </nav>
        <div className="sidebar-footer">
          <div className="profile-avatar" title="本地空间">L</div>
        </div>
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
