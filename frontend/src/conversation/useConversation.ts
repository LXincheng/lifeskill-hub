import { useCallback, useEffect, useRef, useState } from 'react'

import {
  ConversationApiError,
  createConversation,
  getConversation,
  sendConversationMessage,
} from './conversationApi'
import type { Conversation } from './conversationApi'

const ACTIVE_CONVERSATION_KEY = 'lifeskill.activeConversationId'

export type ConversationState = {
  conversation: Conversation | null
  error: string | null
  isLoading: boolean
  isSending: boolean
  reloadConversation: () => Promise<void>
  startNewConversation: () => Promise<void>
  sendMessage: (content: string) => Promise<boolean>
}

export function useConversation(): ConversationState {
  const [conversation, setConversation] = useState<Conversation | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSending, setIsSending] = useState(false)
  const hasInitialized = useRef(false)

  const rememberConversation = useCallback((nextConversation: Conversation) => {
    localStorage.setItem(ACTIVE_CONVERSATION_KEY, nextConversation.id)
    setConversation(nextConversation)
  }, [])

  const startNewConversation = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      rememberConversation(await createConversation())
    } catch {
      setError('无法创建对话，请确认后端已启动后重试。')
    } finally {
      setIsLoading(false)
    }
  }, [rememberConversation])

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
  }, [rememberConversation, startNewConversation])

  const sendMessage = useCallback(async (content: string) => {
    if (!conversation || isSending) return false

    setIsSending(true)
    setError(null)
    try {
      rememberConversation(await sendConversationMessage(conversation.id, content))
      return true
    } catch {
      setError('消息未发送，请保留内容并重试。')
      return false
    } finally {
      setIsSending(false)
    }
  }, [conversation, isSending, rememberConversation])

  useEffect(() => {
    if (hasInitialized.current) return
    hasInitialized.current = true
    void reloadConversation()
  }, [reloadConversation])

  return {
    conversation,
    error,
    isLoading,
    isSending,
    reloadConversation,
    startNewConversation,
    sendMessage,
  }
}
