import { useEffect, useState } from 'react'

import { Icon } from '../components/Icon'
import { getAgentRun, watchAgentRun } from '../skills/skillsApi'
import type { AgentRun } from '../skills/skillsApi'

const terminal = new Set(['COMPLETED', 'BLOCKED', 'FAILED', 'TIMED_OUT'])

export function ResearchRunPanel({ runId, onOpenReport }: { runId: string; onOpenReport: (contentId: string) => void }) {
  const [run, setRun] = useState<AgentRun | null>(null)
  const [connectionError, setConnectionError] = useState(false)

  useEffect(() => {
    let stop = () => {}
    void getAgentRun(runId).then(setRun).catch(() => setConnectionError(true))
    stop = watchAgentRun(runId, (next) => { setRun(next); setConnectionError(false) }, () => setConnectionError(true))
    return () => stop()
  }, [runId])

  const isTerminal = run ? terminal.has(run.status) : false
  const isLearning = run?.capability === 'LEARNING_PLAN'
  return <section className="research-run-panel" aria-live="polite">
    <header><span><Icon name={isLearning ? 'route' : 'activity'} size={16} /><strong>{isLearning ? '学习系统构建' : '官方研究运行'}</strong></span><em className={`status-pill ${(run?.status ?? 'running').toLowerCase()}`}>{run?.status ?? 'CONNECTING'}</em></header>
    <div className="research-progress"><i><b style={{ width: `${run ? run.stepCount / run.maxSteps * 100 : 4}%` }} /></i><span>{run ? `${run.stepCount} / ${run.maxSteps} 个受控步骤` : '正在连接运行事件'}</span></div>
    {run && <ol>{run.steps.map((step) => <li className={step.status.toLowerCase()} key={step.order}><i>{step.status === 'COMPLETED' ? <Icon name="check" size={12} /> : <Icon name="alert-circle" size={12} />}</i><span><strong>{step.role}</strong><small>{step.outputSummary ?? step.errorSummary ?? step.eventType}{step.toolName ? ` · ${step.toolName}` : ''}{step.durationMs ? ` · ${step.durationMs} ms` : ''}</small></span></li>)}</ol>}
    {connectionError && !isTerminal && <p className="research-connection-warning">实时连接暂时中断，刷新后仍可从数据库恢复运行状态。</p>}
    {run?.failureSummary && <p className="research-failure">{run.failureSummary}</p>}
    {run?.status === 'COMPLETED' && run.resultContentId && <button className="open-report-button" onClick={() => onOpenReport(run.resultContentId!)}><span><Icon name={isLearning ? 'route' : 'file'} size={17} /><strong>{isLearning ? '打开学习路径' : '打开专业研究报告'}</strong><small>{isLearning ? '路径、导读与测验已保存到同一文件夹' : '已核验 · 可追溯到官方 Evidence'}</small></span><Icon name="arrow-right" size={17} /></button>}
  </section>
}
