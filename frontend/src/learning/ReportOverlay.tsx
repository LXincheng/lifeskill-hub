import { useEffect, useState } from 'react'

import { Icon } from '../components/Icon'
import { LearningContentViewer } from './LearningContentViews'
import { getContent } from './learningApi'
import type { ContentItem } from './learningApi'

export function ReportOverlay({ contentId, onClose }: { contentId: string; onClose: () => void }) {
  const [item, setItem] = useState<ContentItem | null>(null)
  const [error, setError] = useState<string | null>(null)
  useEffect(() => { void getContent(contentId).then(setItem).catch((cause) => setError(cause instanceof Error ? cause.message : '报告加载失败。')) }, [contentId])
  const isVerifiedReport = item?.type === 'REPORT' && item.verificationStatus === 'VERIFIED'
  return <div className="report-overlay" role="dialog" aria-modal="true" aria-label={item?.type === 'LEARNING_PATH' ? '学习路径' : '专业研究报告'}>
    <header><button onClick={onClose}><Icon name="arrow-left" size={16} />返回工作台</button><span><Icon name={isVerifiedReport ? 'shield' : 'route'} size={14} />{isVerifiedReport ? 'Evidence-backed report' : 'Personal learning system'}</span></header>
    <main>{error ? <div className="pulse-empty error"><Icon name="alert-circle" size={22} /><strong>报告暂时无法打开</strong><p>{error}</p></div> : item ? <LearningContentViewer item={item} /> : <div className="page-state">正在读取报告…</div>}</main>
  </div>
}
