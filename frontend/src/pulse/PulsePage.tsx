import { useCallback, useEffect, useState } from 'react'

import { Icon } from '../components/Icon'
import { listPulseItems } from './pulseApi'
import type { PulseItem } from './pulseApi'

type PulsePageProps = {
  onAsk: () => void
  onLearn: () => void
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
    .format(new Date(value))
}

export function PulsePage({ onAsk, onLearn }: PulsePageProps) {
  const [items, setItems] = useState<PulseItem[]>([])
  const [activeTab, setActiveTab] = useState('全部')
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setIsLoading(true)
    try {
      setItems(await listPulseItems())
      setError(null)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : '动态加载失败。')
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => { void load() }, [load])
  const tabs = ['全部', ...Array.from(new Set(items.map((item) => item.category)))]
  const visibleItems = activeTab === '全部' ? items : items.filter((item) => item.category === activeTab)

  return (
    <section className="pulse-view">
      <header className="pulse-header">
        <div className="pulse-heading-row">
          <div><h1>动态</h1><p>这里只展示由真实 Skill 运行产生并通过核验策略的内容。</p></div>
          <span className="pulse-count">{visibleItems.length} 条内容</span>
        </div>
        <div className="pulse-tabs">
          {tabs.map((tab) => <button className={activeTab === tab ? 'active' : ''} key={tab} onClick={() => setActiveTab(tab)}>{tab}</button>)}
        </div>
      </header>

      <div className="pulse-content">
        {isLoading ? <div className="page-state">正在读取动态…</div> : error ? (
          <div className="pulse-empty error"><Icon name="alert-circle" size={22} /><strong>暂时无法读取动态</strong><p>{error}</p><button className="secondary-button" onClick={() => void load()}><Icon name="refresh" size={15} />重试</button></div>
        ) : visibleItems.length === 0 ? (
          <div className="pulse-empty"><span><Icon name="rss" size={23} /></span><strong>还没有可靠动态</strong><p>确认一个 Skill 后，来源采集、证据核验和发布链路将在下一切片接入。没有 Evidence 的内容不会出现在这里。</p><button className="primary-button" onClick={onAsk}><Icon name="message" size={15} />从对话创建 Skill</button></div>
        ) : (
          <div className="pulse-grid">
            {visibleItems.map((item) => (
              <article className="pulse-card" key={item.id}>
                <i className="pulse-accent" />
                <div className="pulse-card-body">
                  <div className="pulse-meta"><span>{item.category}</span><i>·</i><span className="credibility"><Icon name="shield" size={12} />{item.verificationStatus}</span><i>·</i><span>{formatTime(item.publishedAt)}</span></div>
                  <h2>{item.title}</h2><p>{item.summary}</p>
                </div>
                <div className="pulse-actions"><button onClick={onAsk}><Icon name="message" size={14} />询问</button><button onClick={onLearn}><Icon name="book" size={14} />进入学习</button></div>
              </article>
            ))}
          </div>
        )}
      </div>
    </section>
  )
}
