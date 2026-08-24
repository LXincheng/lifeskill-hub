import { Icon } from '../components/Icon'
import type { ConversationState } from './useConversation'

type ConversationContextPanelProps = {
  state: ConversationState
}

const capabilityPaths = [
  { icon: 'compass' as const, label: '一次任务', detail: '把模糊目标拆成可执行步骤' },
  { icon: 'book' as const, label: '学习路线', detail: '沉淀内容并持续记录进度' },
  { icon: 'activity' as const, label: '持续关注', detail: '确认草案后再定期运行' },
]

export function ConversationContextPanel({ state }: ConversationContextPanelProps) {
  const messageCount = state.conversation?.messages.length ?? 0

  return (
    <aside className="context-panel" aria-label="对话上下文">
      <section className="context-section">
        <div className="context-heading"><span>本次对话</span><Icon name="message" size={14} /></div>
        <h2>{state.conversation?.title ?? '正在建立对话'}</h2>
        <div className="context-facts">
          <div><span>消息</span><strong>{messageCount} 条</strong></div>
          <div><span>保存状态</span><strong className={state.error ? 'context-danger' : state.isLoading ? '' : 'context-success'}>{state.isLoading ? '正在连接' : state.error ? '连接中断' : '已同步'}</strong></div>
        </div>
      </section>

      <section className="context-section">
        <div className="context-heading"><span>可以沉淀为</span><Icon name="sparkles" size={14} /></div>
        <div className="capability-paths">
          {capabilityPaths.map((path) => (
            <div key={path.label}>
              <span><Icon name={path.icon} size={15} /></span>
              <p><strong>{path.label}</strong><small>{path.detail}</small></p>
            </div>
          ))}
        </div>
      </section>

      <section className="context-section context-policy">
        <div className="context-heading"><span>工作原则</span><Icon name="check" size={14} /></div>
        <p><Icon name="check" size={13} />长期动作创建前由你确认</p>
        <p><Icon name="check" size={13} />重要结论需要关联来源</p>
        <p><Icon name="check" size={13} />只展示可验证的执行事件</p>
      </section>
    </aside>
  )
}
