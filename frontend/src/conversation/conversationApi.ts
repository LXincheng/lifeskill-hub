export type MessageRole = 'USER' | 'ASSISTANT' | 'SYSTEM'

export type ConversationMessage = {
  id: string
  role: MessageRole
  content: string
  createdAt: string
  processingSteps: ProcessingStep[]
  durationMs: number | null
}

export type ProcessingStep = {
  stage: string
  label: string
  status: 'COMPLETED' | 'BLOCKED' | 'FAILED'
  durationMs: number
  detail: string
}

export type SkillDraft = {
  id: string
  sourceMessageId: string
  title: string
  objective: string
  dayOfWeek: string
  time: string
  timezone: string
  status: 'PENDING_CONFIRMATION' | 'CONFIRMED'
  confirmedSkillId: string | null
  confirmedAt: string | null
  createdAt: string
}

export type SkillConfirmation = {
  draftId: string
  draftStatus: 'CONFIRMED'
  skillId: string
  skillName: string
  skillStatus: 'ACTIVE' | 'PAUSED'
  currentVersion: number
  confirmedAt: string
}

export type Conversation = {
  id: string
  title: string
  createdAt: string
  updatedAt: string
  messages: ConversationMessage[]
  skillDrafts: SkillDraft[]
}

type ProblemDetails = {
  detail?: string
  code?: string
}

export class ConversationApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code?: string,
  ) {
    super(message)
  }
}

async function requestConversation(path: string, init?: RequestInit): Promise<Conversation> {
  const response = await fetch(path, init)
  if (!response.ok) {
    let problem: ProblemDetails = {}
    try {
      problem = (await response.json()) as ProblemDetails
    } catch {
      // The status still gives the UI a safe fallback when a proxy returns non-JSON.
    }
    throw new ConversationApiError(problem.detail ?? '请求暂时失败，请稍后重试。', response.status, problem.code)
  }
  const conversation = (await response.json()) as Conversation
  return {
    ...conversation,
    messages: conversation.messages.map((message) => ({
      ...message,
      processingSteps: message.processingSteps ?? [],
      durationMs: message.durationMs ?? null,
    })),
    skillDrafts: conversation.skillDrafts ?? [],
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, init)
  if (!response.ok) {
    let problem: ProblemDetails = {}
    try {
      problem = (await response.json()) as ProblemDetails
    } catch {
      // The status still gives the UI a safe fallback when a proxy returns non-JSON.
    }
    throw new ConversationApiError(problem.detail ?? '请求暂时失败，请稍后重试。', response.status, problem.code)
  }
  return response.json() as Promise<T>
}

export function createConversation(): Promise<Conversation> {
  return requestConversation('/api/conversations', { method: 'POST' })
}

export function getConversation(conversationId: string): Promise<Conversation> {
  return requestConversation(`/api/conversations/${conversationId}`)
}

export function sendConversationMessage(conversationId: string, content: string): Promise<Conversation> {
  return requestConversation(`/api/conversations/${conversationId}/messages`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ content }),
  })
}

export function confirmSkillDraft(draftId: string, idempotencyKey: string): Promise<SkillConfirmation> {
  return request<SkillConfirmation>(`/api/skill-drafts/${draftId}/confirmations`, {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
  })
}
