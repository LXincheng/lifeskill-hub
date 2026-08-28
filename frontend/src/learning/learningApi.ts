export type LearningFolder = {
  id: string
  name: string
  description: string
  createdAt: string
  updatedAt: string
}

export type ContentItemType = 'REPORT' | 'LEARNING_PATH' | 'ARTICLE' | 'NOTE' | 'QUIZ' | 'CHECKLIST'

export type ContentItem = {
  id: string
  folderId: string
  sourceSkillRunId: string | null
  type: ContentItemType
  title: string
  body: string
  verificationStatus: 'USER_AUTHORED' | 'VERIFIED' | 'PARTIALLY_VERIFIED' | 'AI_GENERATED'
  createdAt: string
  updatedAt: string
}

type FolderInput = { name?: string; description?: string }
type ContentInput = { type?: ContentItemType; title?: string; body?: string }

export type LearningAttempt = {
  id: string
  contentItemId: string
  kind: 'PROGRESS' | 'QUIZ'
  status: 'IN_PROGRESS' | 'COMPLETED'
  completedUnits: number
  totalUnits: number
  score: number | null
  completedUnitIndexes: number[]
  completedAt: string | null
  createdAt: string
}

export type LearningProgress = {
  contentCount: number
  startedCount: number
  completedCount: number
  completionPercent: number
  averageQuizScore: number | null
  latestActivityAt: string | null
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, init)
  if (!response.ok) {
    const problem = await response.json().catch(() => ({})) as { detail?: string }
    throw new Error(problem.detail ?? '请求暂时失败，请稍后重试。')
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

const json = (method: string, body: object): RequestInit => ({
  method,
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body),
})

export const listFolders = () => request<LearningFolder[]>('/api/learning-folders')
export const createFolder = (input: FolderInput) => request<LearningFolder>('/api/learning-folders', json('POST', input))
export const updateFolder = (id: string, input: FolderInput) => request<LearningFolder>(`/api/learning-folders/${id}`, json('PATCH', input))
export const deleteFolder = (id: string) => request<void>(`/api/learning-folders/${id}`, { method: 'DELETE' })
export const listContent = (folderId: string) => request<ContentItem[]>(`/api/learning-folders/${folderId}/content-items`)
export const getContent = (id: string) => request<ContentItem>(`/api/content-items/${id}`)
export const createContent = (folderId: string, input: ContentInput) => request<ContentItem>(`/api/learning-folders/${folderId}/content-items`, json('POST', input))
export const updateContent = (id: string, input: ContentInput) => request<ContentItem>(`/api/content-items/${id}`, json('PATCH', input))
export const deleteContent = (id: string) => request<void>(`/api/content-items/${id}`, { method: 'DELETE' })
export const listAttempts = (id: string) => request<LearningAttempt[]>(`/api/content-items/${id}/attempts`)
export const recordAttempt = (id: string, input: Omit<LearningAttempt, 'id' | 'contentItemId' | 'score' | 'completedAt' | 'createdAt'>) => request<LearningAttempt>(`/api/content-items/${id}/attempts`, json('POST', input))
export const getLearningProgress = (folderId: string) => request<LearningProgress>(`/api/learning-folders/${folderId}/progress`)
