import { useState } from 'react'

import { Icon } from '../components/Icon'
import type { IconName } from '../components/Icon'

type LearningType = 'cards' | 'article' | 'quiz'

const folders: Array<{ label: string; count: number; icon: IconName }> = [
  { label: 'Java Agent', count: 12, icon: 'code' },
  { label: 'Spring AI', count: 8, icon: 'sparkles' },
  { label: 'Agent 工程', count: 6, icon: 'target' },
  { label: '力量训练', count: 4, icon: 'folder' },
  { label: '贵金属基础', count: 5, icon: 'folder' },
]

const learningItems = [
  { type: '概念卡', title: 'premain 与 agentmain', icon: 'file' as const, meta: '8 分钟' },
  { type: '流程卡', title: '工具调用为什么由应用执行', icon: 'list' as const, meta: '12 分钟' },
  { type: '代码实践', title: '实现一个受控 ToolCallback', icon: 'code' as const, meta: '25 分钟' },
  { type: '复习', title: 'Instrumentation 本周复习', icon: 'check-circle' as const, meta: '5 分钟' },
]

export function LearningPage() {
  const [learningType, setLearningType] = useState<LearningType>('cards')
  const [selectedFolder, setSelectedFolder] = useState('Java Agent')

  return (
    <section className="learning-view">
      <aside className="learning-sidebar">
        <div className="section-label"><span>学习库</span></div>
        <div className="folder-list">
          {folders.map((folder) => (
            <button
              className={selectedFolder === folder.label ? 'active' : ''}
              key={folder.label}
              onClick={() => setSelectedFolder(folder.label)}
            >
              <Icon name="chevron-right" size={14} />
              <span className="folder-icon"><Icon name={folder.icon} size={16} /></span>
              <strong>{folder.label}</strong>
              <small>{folder.count}</small>
            </button>
          ))}
        </div>
      </aside>

      <div className="learning-main">
        <div className="learning-content">
          <div className="breadcrumb"><span>学习库</span><Icon name="chevron-right" size={12} /><strong>{selectedFolder}</strong></div>
          <header className="learning-title-block">
            <div><span className="type-badge"><Icon name="list" size={13} />学习路径</span><small>12 个模块 · 约 8 小时</small></div>
            <h1>{selectedFolder}</h1>
            <p>从核心概念出发，逐步理解模型、工具、权限和可靠性边界，把知识转化为可运行的 Agent 工程能力。</p>
          </header>

          <section className="progress-card">
            <div><strong>学习进度</strong><span>5 / 12 已完成</span></div>
            <div className="progress-track"><i /></div>
            <footer><span><Icon name="clock" size={13} />上次学习：2 天前</span><span className="success"><Icon name="check-circle" size={13} />连续学习 7 天</span></footer>
          </section>

          <div className="learning-tabs" role="tablist">
            {(['cards', 'article', 'quiz'] as LearningType[]).map((type) => (
              <button
                aria-selected={learningType === type}
                className={learningType === type ? 'active' : ''}
                key={type}
                onClick={() => setLearningType(type)}
                role="tab"
              >{type === 'cards' ? '学习卡片' : type === 'article' ? '文章阅读' : '测验'}</button>
            ))}
          </div>

          {learningType === 'cards' && (
            <div className="learning-module-list">
              {learningItems.map((item, index) => (
                <button key={item.title}>
                  <span className={index < 2 ? 'module-index done' : index === 2 ? 'module-index current' : 'module-index'}>
                    {index < 2 ? <Icon name="check" size={14} /> : index + 1}
                  </span>
                  <span className="module-copy"><small>{item.type}</small><strong>{item.title}</strong></span>
                  <span className="module-meta"><Icon name="clock" size={13} />{item.meta}<Icon name="arrow-right" size={15} /></span>
                </button>
              ))}
            </div>
          )}

          {learningType === 'article' && (
            <article className="learning-reader">
              <small>12 分钟阅读 · 2 个官方来源</small>
              <h2>从一次工具调用理解 Agent Harness</h2>
              <p>语言模型返回工具名称与参数，由应用校验并执行，再把结果送回模型。这个边界决定了权限、预算、超时和审计应该放在哪里。</p>
              <p>LifeSkill 会保存每次工具调用的证据与发布决策，而不是让模型自行声明内容可靠。</p>
              <footer>Spring AI Tool Calling · DeepSeek Tool Calls</footer>
            </article>
          )}

          {learningType === 'quiz' && (
            <div className="learning-quiz">
              <small>理解检查 · 第 1 / 3 题</small>
              <h2>为什么不能让模型直接执行任意工具？</h2>
              {['模型生成速度不够快', '应用需要控制权限、参数、预算与审计', 'Java 不支持工具调用', '工具结果不能返回模型'].map((choice) => <button key={choice}>{choice}</button>)}
            </div>
          )}
        </div>
      </div>
    </section>
  )
}
