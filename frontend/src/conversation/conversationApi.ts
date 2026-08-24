export type MessageRole = 'USER' | 'ASSISTANT' | 'SYSTEM'

export type ConversationMessage = {
  id: string
  role: MessageRole
  content: string
  createdAt: string
}

export type Conversation = {
  id: string
  title: string
  createdAt: string
  updatedAt: string
  messages: ConversationMessage[]
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
  return (await response.json()) as Conversation
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
