export type Skill = {
  id: string
  name: string
  objective: string
  status: 'ACTIVE' | 'PAUSED'
  currentVersion: number
  dayOfWeek: string
  time: string
  timezone: string
  createdAt: string
  updatedAt: string
}

export type AgentStep = {
  order: number
  role: string
  status: 'COMPLETED' | 'FAILED'
  eventType: string
  inputSummary: string | null
  outputSummary: string | null
  toolName: string | null
  sourceUrl: string | null
  durationMs: number | null
  errorSummary: string | null
  completedAt: string | null
}

export type AgentRun = {
  id: string
  skillId: string | null
  skillVersion: number
  conversationId: string | null
  capability: string | null
  resultContentId: string | null
  auditId: string
  triggerType: 'MANUAL' | 'SCHEDULED' | 'CONVERSATION_RESEARCH'
  status: string
  maxSteps: number
  stepCount: number
  startedAt: string
  timeoutAt: string
  completedAt: string | null
  durationMs: number | null
  failureSummary: string | null
  steps: AgentStep[]
}

type Problem = { detail?: string }

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, init)
  if (!response.ok) {
    const problem = await response.json().catch(() => ({})) as Problem
    throw new Error(problem.detail ?? '请求暂时失败，请稍后重试。')
  }
  return response.json() as Promise<T>
}

const json = (method: string, body: object): RequestInit => ({
  method,
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body),
})

export const listSkills = () => request<Skill[]>('/api/skills')
export const updateSkill = (skillId: string, input: Partial<Pick<Skill, 'status' | 'dayOfWeek' | 'time' | 'timezone'>>) =>
  request<Skill>(`/api/skills/${skillId}`, json('PATCH', input))
export const startAgentRun = (skillId: string) => request<AgentRun>(`/api/skills/${skillId}/runs`, { method: 'POST' })
export const getAgentRun = (runId: string) => request<AgentRun>(`/api/skill-runs/${runId}`)

export async function getLatestAgentRun(skillId: string): Promise<AgentRun | null> {
  const response = await fetch(`/api/skills/${skillId}/runs/latest`)
  if (response.status === 404) return null
  if (!response.ok) throw new Error('最近运行结果加载失败。')
  return response.json() as Promise<AgentRun>
}

export function watchAgentRun(runId: string, onChange: (run: AgentRun) => void, onError: () => void) {
  const source = new EventSource(`/api/skill-runs/${runId}/events`)
  let isClosed = false
  const refresh = async () => {
    try {
      const run = await getAgentRun(runId)
      onChange(run)
      if (['COMPLETED', 'BLOCKED', 'FAILED', 'TIMED_OUT'].includes(run.status)) {
        isClosed = true
        source.close()
      }
    } catch {
      onError()
    }
  }
  source.addEventListener('snapshot', () => void refresh())
  source.addEventListener('changed', () => void refresh())
  source.addEventListener('finished', () => void refresh())
  source.onerror = () => { if (!isClosed) onError() }
  void refresh()
  return () => { isClosed = true; source.close() }
}
