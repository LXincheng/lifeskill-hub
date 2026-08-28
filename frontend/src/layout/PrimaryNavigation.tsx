import { Icon } from '../components/Icon'
import type { IconName } from '../components/Icon'
import { workspaceCopy } from '../copy'
import type { ThemeMode } from '../theme/useTheme'

export type PrimaryView = 'chat' | 'learning' | 'pulse'

type PrimaryNavigationProps = {
  activeView: PrimaryView
  isBusy: boolean
  onNavigate: (view: PrimaryView) => void
  onNewConversation: () => void
  theme: ThemeMode
  onToggleTheme: () => void
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
  theme,
  onToggleTheme,
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
        <div className="sidebar-footer">
          <button aria-label={theme === 'dark' ? '切换到明亮模式' : '切换到暗黑模式'} className="theme-toggle" onClick={onToggleTheme} title={theme === 'dark' ? '明亮模式' : '暗黑模式'}><Icon name={theme === 'dark' ? 'sun' : 'moon'} size={17} /><span>{theme === 'dark' ? '明亮模式' : '暗黑模式'}</span></button>
          <div aria-label="本地工作区"><span className="workspace-status" /><span><strong>服务已连接</strong><small>本地工作区</small></span></div>
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
        <button aria-label={theme === 'dark' ? '切换到明亮模式' : '切换到暗黑模式'} onClick={onToggleTheme}>
          <Icon name={theme === 'dark' ? 'sun' : 'moon'} size={21} />
          <span>{theme === 'dark' ? '明亮' : '暗黑'}</span>
        </button>
      </nav>
    </>
  )
}
