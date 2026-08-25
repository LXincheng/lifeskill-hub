import { useState } from 'react'

import { Icon } from '../components/Icon'

type PulsePageProps = {
  onAsk: () => void
  onLearn: () => void
}

const pulseItems = [
  {
    source: 'Java Agent Weekly',
    title: 'Spring AI 工具执行边界发生变化',
    summary: '与你当前计划相关，可继续询问、查看证据或进入学习。',
    meta: '3 个一手来源',
    verification: '核验通过',
    time: '2 小时前',
    accent: 'blue',
  },
  {
    source: '贵金属基础',
    title: '黄金波动扩大，驱动因素仍存在来源冲突',
    summary: '多个来源对短期驱动因素判断不一致，系统已降低主动推送强度。',
    meta: '4 个来源',
    verification: '部分确认',
    time: '昨天',
    accent: 'amber',
  },
  {
    source: '力量训练计划',
    title: '深蹲技术复习已准备好',
    summary: '包含动作要点、常见错误和下一次训练前的快速检查。',
    meta: '3 张动作卡',
    verification: '来源可信',
    time: '2 天前',
    accent: 'green',
  },
]

export function PulsePage({ onAsk, onLearn }: PulsePageProps) {
  const [activeTab, setActiveTab] = useState('全部')
  const tabs = ['全部', ...pulseItems.map((item) => item.source)]
  const visibleItems = activeTab === '全部' ? pulseItems : pulseItems.filter((item) => item.source === activeTab)

  return (
    <section className="pulse-view">
      <header className="pulse-header">
        <div className="pulse-heading-row">
          <div><h1>动态</h1><p>基于你的 Skill、学习计划和主动关注生成。</p></div>
          <span className="pulse-count">{visibleItems.length} 条内容</span>
        </div>
        <div className="pulse-tabs">
          {tabs.map((tab) => <button className={activeTab === tab ? 'active' : ''} key={tab} onClick={() => setActiveTab(tab)}>{tab}</button>)}
        </div>
      </header>

      <div className="pulse-content">
        <div className="pulse-grid">
          {visibleItems.map((item) => (
            <article className="pulse-card" key={item.title}>
              <i className={`pulse-accent ${item.accent}`} />
              <div className="pulse-card-body">
                <div className="pulse-meta"><span>{item.source}</span><i>·</i><span className={item.verification === '部分确认' ? 'credibility warning' : 'credibility'}><Icon name="shield" size={12} />{item.verification}</span><i>·</i><span>{item.time}</span></div>
                <h2>{item.title}</h2>
                <p>{item.summary}</p>
                <footer><span>{item.meta}</span><span>来自 {item.source}</span></footer>
              </div>
              <div className="pulse-actions"><button onClick={onAsk}><Icon name="message" size={14} />询问</button><button onClick={onLearn}><Icon name="book" size={14} />进入学习</button></div>
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}
