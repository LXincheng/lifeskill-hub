import { useCallback, useEffect, useState } from 'react'

import { Icon } from '../components/Icon'
import { m2Copy } from '../copy'
import { getLatestAgentRun, listSkills, startAgentRun, updateSkill, watchAgentRun } from './skillsApi'
import type { AgentRun, Skill } from './skillsApi'

type SkillsDrawerProps = {
  initialSkillId?: string | null
  onClose: () => void
}

const dayOptions = [
  ['MONDAY', '周一'], ['TUESDAY', '周二'], ['WEDNESDAY', '周三'], ['THURSDAY', '周四'],
  ['FRIDAY', '周五'], ['SATURDAY', '周六'], ['SUNDAY', '周日'],
] as const

const terminalStatuses = new Set(['COMPLETED', 'BLOCKED', 'FAILED', 'TIMED_OUT'])

function statusLabel(status: string) {
  return ({ ACTIVE: '运行中', PAUSED: '已暂停', COMPLETED: '已发布', BLOCKED: '已拦截', FAILED: '失败', TIMED_OUT: '超时' } as Record<string, string>)[status] ?? '执行中'
}

export function SkillsDrawer({ initialSkillId, onClose }: SkillsDrawerProps) {
  const [skills, setSkills] = useState<Skill[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(initialSkillId ?? null)
  const [run, setRun] = useState<AgentRun | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isMutating, setIsMutating] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const selected = skills.find((skill) => skill.id === selectedId) ?? null

  const load = useCallback(async () => {
    try {
      const next = await listSkills()
      setSkills(next)
      const nextId = initialSkillId && next.some((item) => item.id === initialSkillId) ? initialSkillId : next[0]?.id ?? null
      setSelectedId((current) => current && next.some((item) => item.id === current) ? current : nextId)
      setError(null)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Skills 加载失败。')
    } finally {
      setIsLoading(false)
    }
  }, [initialSkillId])

  useEffect(() => { void load() }, [load])
  useEffect(() => {
    if (!selectedId) { setRun(null); return }
    void getLatestAgentRun(selectedId).then(setRun).catch(() => setRun(null))
  }, [selectedId])

  useEffect(() => {
    if (!run || terminalStatuses.has(run.status)) return
    return watchAgentRun(run.id, setRun, () => setError('运行事件连接中断，可重新打开查看最终结果。'))
  }, [run?.id, run?.status])

  async function handleStatus() {
    if (!selected) return
    setIsMutating(true)
    try {
      const updated = await updateSkill(selected.id, { status: selected.status === 'ACTIVE' ? 'PAUSED' : 'ACTIVE' })
      setSkills((items) => items.map((item) => item.id === updated.id ? updated : item))
    } catch (cause) { setError(cause instanceof Error ? cause.message : '状态更新失败。') }
    finally { setIsMutating(false) }
  }

  async function handleSchedule(dayOfWeek: string, time: string) {
    if (!selected) return
    setIsMutating(true)
    try {
      const updated = await updateSkill(selected.id, { dayOfWeek, time })
      setSkills((items) => items.map((item) => item.id === updated.id ? updated : item))
    } catch (cause) { setError(cause instanceof Error ? cause.message : '频率保存失败。') }
    finally { setIsMutating(false) }
  }

  async function handleRun() {
    if (!selected) return
    setIsMutating(true)
    setError(null)
    try { setRun(await startAgentRun(selected.id)) }
    catch (cause) { setError(cause instanceof Error ? cause.message : '运行启动失败。') }
    finally { setIsMutating(false) }
  }

  return (
    <div className="drawer-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
      <aside aria-label="Skills 管理" className="skills-drawer">
        <header className="drawer-header"><div><strong>{m2Copy.skillsTitle}</strong><small>{m2Copy.skillsSubtitle}</small></div><button aria-label="关闭" className="icon-button" onClick={onClose}><Icon name="x" size={17} /></button></header>
        {error && <div className="drawer-error"><Icon name="alert-circle" size={15} />{error}</div>}
        {isLoading ? <div className="page-state">正在读取 Skills…</div> : skills.length === 0 ? <div className="drawer-empty">{m2Copy.noSkills}</div> : (
          <div className="skills-layout">
            <div className="skill-selector">{skills.map((skill) => <button className={selectedId === skill.id ? 'active' : ''} key={skill.id} onClick={() => setSelectedId(skill.id)}><span><strong>{skill.name}</strong><small>v{skill.currentVersion} · {statusLabel(skill.status)}</small></span><Icon name="chevron-right" size={15} /></button>)}</div>
            {selected && <div className="skill-detail">
              <div className="skill-detail-heading"><div><span className={`status-pill ${selected.status.toLowerCase()}`}>{statusLabel(selected.status)}</span><h2>{selected.name}</h2><p>{selected.objective}</p></div><button className="secondary-button" disabled={isMutating} onClick={() => void handleStatus()}><Icon name={selected.status === 'ACTIVE' ? 'clock' : 'check-circle'} size={14} />{selected.status === 'ACTIVE' ? '暂停' : '恢复'}</button></div>
              <form className="schedule-form" onSubmit={(event) => { event.preventDefault(); const data = new FormData(event.currentTarget); void handleSchedule(String(data.get('dayOfWeek')), String(data.get('time'))) }}>
                <label>每周<select defaultValue={selected.dayOfWeek} key={`${selected.id}-day`} name="dayOfWeek">{dayOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
                <label>时间<input defaultValue={selected.time} key={`${selected.id}-time`} name="time" type="time" /></label>
                <button className="secondary-button" disabled={isMutating}>{m2Copy.saveSchedule}</button>
              </form>
              <div className="run-heading"><div><strong>最近运行</strong><small>只展示可验证事件、工具、来源和耗时</small></div><button className="primary-button" disabled={isMutating || selected.status === 'PAUSED' || (!!run && !terminalStatuses.has(run.status))} onClick={() => void handleRun()}><Icon name="activity" size={15} />{run && !terminalStatuses.has(run.status) ? m2Copy.running : m2Copy.runNow}</button></div>
              {!run ? <div className="run-empty">尚未运行。手动运行稳定后，系统也会按保存的每周频率触发。</div> : <div className="run-panel">
                <div className="run-summary"><span className={`status-pill ${run.status.toLowerCase()}`}>{statusLabel(run.status)}</span><span>步骤 {run.stepCount}/{run.maxSteps}</span><span>{run.durationMs === null ? '进行中' : `${run.durationMs} ms`}</span></div>
                {run.failureSummary && <p className="run-failure">{run.failureSummary}</p>}
                <ol>{run.steps.map((step) => <li key={step.order}><i className={step.status === 'COMPLETED' ? 'done' : 'failed'}>{step.status === 'COMPLETED' ? <Icon name="check" size={11} /> : <Icon name="alert-circle" size={11} />}</i><span><strong>{step.role}</strong><small>{step.outputSummary ?? step.errorSummary ?? step.eventType}{step.toolName ? ` · ${step.toolName}` : ''}{step.durationMs ? ` · ${step.durationMs} ms` : ''}</small>{step.sourceUrl && <a href={step.sourceUrl} rel="noreferrer" target="_blank"><Icon name="external-link" size={12} />查看官方来源</a>}</span></li>)}</ol>
              </div>}
            </div>}
          </div>
        )}
      </aside>
    </div>
  )
}
