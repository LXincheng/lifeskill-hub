import { useCallback, useEffect, useRef, useState } from 'react'

import {
  ConversationApiError,
  confirmSkillDraft,
  createConversation,
  deleteConversation,
  getConversation,
  listConversations,
  sendConversationMessage,
} from './conversationApi'
import type { Conversation, ConversationMessage, ConversationSummary } from './conversationApi'

const ACTIVE_CONVERSATION_KEY = 'lifeskill.activeConversationId'

export type ConversationState = {
  conversation: Conversation | null
  conversations: ConversationSummary[]
  error: string | null
  isLoading: boolean
  isSending: boolean
  pendingMessage: ConversationMessage | null
  confirmingDraftId: string | null
  reloadConversation: () => Promise<void>
  startNewConversation: () => Promise<void>
  selectConversation: (conversationId: string) => Promise<void>
  removeConversation: (conversationId: string) => Promise<void>
  sendMessage: (content: string) => Promise<boolean>
  confirmDraft: (draftId: string) => Promise<boolean>
}

export function useConversation(): ConversationState {
  const [conversation, setConversation] = useState<Conversation | null>(null)
  const [conversations, setConversations] = useState<ConversationSummary[]>([])
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSending, setIsSending] = useState(false)
  const [pendingMessage, setPendingMessage] = useState<ConversationMessage | null>(null)
  const [confirmingDraftId, setConfirmingDraftId] = useState<string | null>(null)
  const hasInitialized = useRef(false)
  const confirmationKeys = useRef(new Map<string, string>())

  const rememberConversation = useCallback((nextConversation: Conversation) => {
    localStorage.setItem(ACTIVE_CONVERSATION_KEY, nextConversation.id)
    setConversation(nextConversation)
  }, [])

  const refreshSummaries = useCallback(async () => {
    setConversations(await listConversations())
  }, [])

  const startNewConversation = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    setPendingMessage(null)
    try {
      rememberConversation(await createConversation())
      await refreshSummaries()
    } catch {
      setError('无法创建对话，请确认后端已启动后重试。')
    } finally {
      setIsLoading(false)
    }
  }, [refreshSummaries, rememberConversation])

  const selectConversation = useCallback(async (conversationId: string) => {
    setIsLoading(true)
    setError(null)
    try {
      rememberConversation(await getConversation(conversationId))
    } catch {
      setError('无法打开这段对话，请稍后重试。')
    } finally {
      setIsLoading(false)
    }
  }, [rememberConversation])

  const removeConversation = useCallback(async (conversationId: string) => {
    await deleteConversation(conversationId)
    const remaining = (await listConversations()).filter((item) => item.id !== conversationId)
    setConversations(remaining)
    if (conversation?.id !== conversationId) return
    if (remaining[0]) await selectConversation(remaining[0].id)
    else await startNewConversation()
  }, [conversation?.id, selectConversation, startNewConversation])

  const reloadConversation = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    const conversationId = localStorage.getItem(ACTIVE_CONVERSATION_KEY)

    if (!conversationId) {
      await startNewConversation()
      return
    }

    try {
      rememberConversation(await getConversation(conversationId))
      await refreshSummaries()
    } catch (caughtError) {
      if (caughtError instanceof ConversationApiError && caughtError.status === 404) {
        localStorage.removeItem(ACTIVE_CONVERSATION_KEY)
        await startNewConversation()
        return
      }
      setError('无法恢复对话历史，请稍后重试。')
    } finally {
      setIsLoading(false)
    }
  }, [refreshSummaries, rememberConversation, startNewConversation])

  const sendMessage = useCallback(async (content: string) => {
    if (!conversation || isSending) return false

    setIsSending(true)
    setError(null)
    setPendingMessage({
      id: `pending-${crypto.randomUUID()}`,
      role: 'USER',
      content,
      createdAt: new Date().toISOString(),
        processingSteps: [],
        durationMs: null,
        agentRunId: null,
    })
    try {
      rememberConversation(await sendConversationMessage(conversation.id, content))
      await refreshSummaries()
      return true
    } catch {
      setError('消息未发送，请保留内容并重试。')
      return false
    } finally {
      setPendingMessage(null)
      setIsSending(false)
    }
  }, [conversation, isSending, refreshSummaries, rememberConversation])

  const confirmDraft = useCallback(async (draftId: string) => {
    if (!conversation || confirmingDraftId) return false

    const idempotencyKey = confirmationKeys.current.get(draftId) ?? crypto.randomUUID()
    confirmationKeys.current.set(draftId, idempotencyKey)
    setConfirmingDraftId(draftId)
    setError(null)
    try {
      await confirmSkillDraft(draftId, idempotencyKey)
      rememberConversation(await getConversation(conversation.id))
      confirmationKeys.current.delete(draftId)
      return true
    } catch {
      setError('Skill 未能确认，请重试；重复操作不会创建多个 Skill。')
      return false
    } finally {
      setConfirmingDraftId(null)
    }
  }, [confirmingDraftId, conversation, rememberConversation])

  useEffect(() => {
    if (hasInitialized.current) return
    hasInitialized.current = true
    void reloadConversation()
  }, [reloadConversation])

  return {
    conversation,
    conversations,
    error,
    isLoading,
    isSending,
    pendingMessage,
    confirmingDraftId,
    reloadConversation,
    startNewConversation,
    selectConversation,
    removeConversation,
    sendMessage,
    confirmDraft,
  }
}
