import { useCallback, useEffect, useState } from 'react'

import { Icon } from '../components/Icon'
import { m2Copy } from '../copy'
import { generateLearning, getPulseEvidence, listPulseItems } from './pulseApi'
import type { Evidence, PulseItem } from './pulseApi'

type PulsePageProps = {
  onAsk: () => void
  onLearn: () => void
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
    .format(new Date(value))
}

const accentTones = ['blue', 'purple', 'orange', 'green'] as const

function categoryTone(category: string) {
  const value = Array.from(category).reduce((total, character) => total + (character.codePointAt(0) ?? 0), 0)
  return accentTones[value % accentTones.length]
}

export function PulsePage({ onAsk, onLearn }: PulsePageProps) {
  const [items, setItems] = useState<PulseItem[]>([])
  const [activeTab, setActiveTab] = useState('全部')
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [expandedId, setExpandedId] = useState<string | null>(null)
  const [evidence, setEvidence] = useState<Record<string, Evidence[]>>({})
  const [busyId, setBusyId] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

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

  async function handleEvidence(item: PulseItem) {
    if (expandedId === item.id) { setExpandedId(null); return }
    setExpandedId(item.id)
    if (evidence[item.id]) return
    try {
      const sources = await getPulseEvidence(item.id)
      setEvidence((current) => ({ ...current, [item.id]: sources }))
    }
    catch (cause) { setError(cause instanceof Error ? cause.message : '证据详情加载失败。') }
  }

  async function handleLearning(item: PulseItem) {
    setBusyId(item.id)
    setNotice(null)
    try {
      await generateLearning(item.id)
      setNotice(m2Copy.learningCreated)
      onLearn()
    } catch (cause) { setError(cause instanceof Error ? cause.message : '学习内容生成失败。') }
    finally { setBusyId(null) }
  }

  return (
    <section className="pulse-view">
      <header className="pulse-header">
        <div className="pulse-heading-row">
          <div><h1>动态</h1><p>这里只展示由真实 Skill 运行产生并通过核验策略的内容。</p></div>
          <span className="pulse-count">{visibleItems.length} 条内容</span>
        </div>
        <div className="pulse-segmented" role="tablist" aria-label="动态分类">
          {tabs.map((tab) => <button className={activeTab === tab ? 'active' : ''} key={tab} onClick={() => setActiveTab(tab)}>{tab}</button>)}
        </div>
      </header>

      <div className="pulse-content">
        {notice && <div className="pulse-notice"><Icon name="check-circle" size={15} />{notice}</div>}
        {isLoading ? <div className="page-state">正在读取动态…</div> : error ? (
          <div className="pulse-empty error"><Icon name="alert-circle" size={22} /><strong>暂时无法读取动态</strong><p>{error}</p><button className="secondary-button" onClick={() => void load()}><Icon name="refresh" size={15} />重试</button></div>
        ) : visibleItems.length === 0 ? (
          <div className="pulse-empty"><span><Icon name="rss" size={23} /></span><strong>还没有可靠动态</strong><p>确认 Skill 后可手动运行。只有引用官方 Evidence、通过独立核验和 Policy Gate 的内容才会出现在这里。</p><button className="primary-button" onClick={onAsk}><Icon name="message" size={15} />从对话创建 Skill</button></div>
        ) : (
          <div className="pulse-grid">
            {visibleItems.map((item) => (
              <article className="pulse-card" key={item.id}>
                <i className={`pulse-accent ${categoryTone(item.category)}`} />
                <div className="pulse-card-body">
                  <div className="pulse-meta"><span>{item.category}</span><i>·</i><span className="credibility"><Icon name="shield" size={12} />已核验</span><i>·</i><span>{item.sourceCount} 个官方来源</span><i>·</i><span>{formatTime(item.publishedAt)}</span></div>
                  <h2>{item.title}</h2><p>{item.summary}</p><div className="recommendation-reason"><strong>推荐原因</strong><span>{item.recommendationReason}</span></div>
                </div>
                <div className="pulse-actions"><button onClick={() => void handleEvidence(item)}><Icon name="shield" size={14} />{m2Copy.evidenceTitle}</button><button disabled={busyId !== null} onClick={() => void handleLearning(item)}><Icon name="book" size={14} />{busyId === item.id ? '正在生成…' : m2Copy.generateLearning}</button></div>
                {expandedId === item.id && <div className="evidence-panel">{evidence[item.id] ? evidence[item.id].map((source) => <article key={source.id}><div><span className="status-pill active">官方来源</span><time>{source.publishedAt ? formatTime(source.publishedAt) : '未提供发布时间'}</time></div><h3>{source.title}</h3><p>{source.excerpt}</p><footer><a href={source.sourceUrl} rel="noreferrer" target="_blank"><Icon name="external-link" size={13} />打开原始来源</a><code title={source.contentHash}>SHA-256 {source.contentHash.slice(0, 12)}…</code></footer></article>) : <div className="evidence-loading">正在读取不可变 Evidence…</div>}</div>}
              </article>
            ))}
          </div>
        )}
      </div>
    </section>
  )
}
