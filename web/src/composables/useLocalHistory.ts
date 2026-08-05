import { ref, watch, type Ref } from 'vue'
import type { ChatMessage } from '../types'

const STORAGE_KEY = 'myclaw.history.v1'

/**
 * 聊天历史持久化：消息数组写入 localStorage，刷新不丢。
 */
export function useLocalHistory(): { messages: Ref<ChatMessage[]>; clear: () => void } {
  const messages = ref<ChatMessage[]>(load())

  function load(): ChatMessage[] {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      return raw ? (JSON.parse(raw) as ChatMessage[]) : []
    } catch {
      return []
    }
  }

  watch(
    messages,
    (value) => {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(value))
      } catch {
        /* 存储不可用（隐私模式/超限）时静默降级为仅内存 */
      }
    },
    { deep: true },
  )

  function clear(): void {
    messages.value = []
    try {
      localStorage.removeItem(STORAGE_KEY)
    } catch {
      /* ignore */
    }
  }

  return { messages, clear }
}
