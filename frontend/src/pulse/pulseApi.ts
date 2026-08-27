export type PulseItem = {
  id: string
  skillRunId: string
  category: string
  title: string
  summary: string
  verificationStatus: string
  sourceCount: number
  recommendationReason: string
  publishedAt: string
  readAt: string | null
}

export type Evidence = {
  id: string
  sourceName: string
  sourceUrl: string
  title: string
  excerpt: string
  publishedAt: string | null
  fetchedAt: string
  contentHash: string
  officialSource: boolean
}

export type LearningBundle = {
  folder: { id: string; name: string; description: string }
  contentItems: Array<{ id: string; type: string; title: string }>
}

export async function listPulseItems(): Promise<PulseItem[]> {
  const response = await fetch('/api/pulse-items')
  if (!response.ok) throw new Error('动态加载失败，请稍后重试。')
  return response.json() as Promise<PulseItem[]>
}

export async function getPulseEvidence(pulseId: string): Promise<Evidence[]> {
  const response = await fetch(`/api/pulse-items/${pulseId}/evidence`)
  if (!response.ok) throw new Error('证据详情加载失败。')
  return response.json() as Promise<Evidence[]>
}

export async function generateLearning(pulseId: string): Promise<LearningBundle> {
  const response = await fetch(`/api/pulse-items/${pulseId}/learning-folder`, { method: 'POST' })
  if (!response.ok) {
    const problem = await response.json().catch(() => ({})) as { detail?: string }
    throw new Error(problem.detail ?? '学习内容生成失败。')
  }
  return response.json() as Promise<LearningBundle>
}
