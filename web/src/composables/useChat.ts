import { reactive, ref, type Ref } from 'vue'
import type { ChatMessage, Thought } from '../types'
import { detectTool } from '../utils/toolNames'

let counter = 0
function uid(): string {
  counter += 1
  return `${Date.now().toString(36)}-${counter.toString(36)}`
}

interface UseChatOptions {
  /** 会话失效（401）时回调，用于回到登录视图 */
  onUnauthorized?: () => void
}

/**
 * SSE 流式聊天：fetch POST /api/chat + ReadableStream 解析 text/event-stream 帧。
 * EventSource 仅支持 GET 无法携带消息体，故自行解析流。
 *
 * 注意：推入消息数组的对象必须用 reactive() 包装，否则后续
 * `status`/`thoughts`/`content` 的修改落在原始对象上，不会触发 Vue 重新渲染
 * （界面会一直停留在初始的「思考中」状态）。
 */
export function useChat(
  messages: Ref<ChatMessage[]>,
  options: UseChatOptions = {},
): { sending: Ref<boolean>; send: (text: string) => Promise<void>; newSession: () => Promise<void> } {
  const sending = ref(false)

  /** 发送消息并流式接收 Agent 思考与最终回复 */
  async function send(text: string): Promise<void> {
    const trimmed = text.trim()
    if (!trimmed || sending.value) return

    messages.value.push({
      id: uid(),
      role: 'user',
      content: trimmed,
      thoughts: [],
      status: 'done',
      createdAt: Date.now(),
    })
    // reactive() 包装：后续对 status/thoughts/content 的修改走 proxy set，
    // 才能触发消息列表重新渲染
    const assistant = reactive<ChatMessage>({
      id: uid(),
      role: 'assistant',
      content: '',
      thoughts: [],
      status: 'streaming',
      createdAt: Date.now(),
    })
    messages.value.push(assistant)
    sending.value = true

    try {
      const res = await fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: trimmed }),
      })
      if (res.status === 401) {
        options.onUnauthorized?.()
        assistant.status = 'error'
        assistant.error = '会话已失效，请重新登录'
        return
      }
      if (!res.ok || !res.body) {
        assistant.status = 'error'
        assistant.error = `请求失败（HTTP ${res.status}）`
        return
      }

      const reader = res.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      for (;;) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const { frames, rest } = extractFrames(buffer)
        buffer = rest
        for (const frame of frames) {
          handleFrame(frame, assistant)
        }
      }
      // 流结束：flush 解码器残留，并处理尾部未以空行结尾的帧
      buffer += decoder.decode()
      const { frames, rest } = extractFrames(buffer)
      for (const frame of frames) {
        handleFrame(frame, assistant)
      }
      if (rest.trim()) {
        handleFrame(rest, assistant)
      }
      if (assistant.status === 'streaming') {
        assistant.status = 'done'
      }
    } catch {
      assistant.status = 'error'
      assistant.error = '连接中断，请重试'
    } finally {
      sending.value = false
    }
  }

  /** 新建会话：清空后端 Agent 上下文 */
  async function newSession(): Promise<void> {
    try {
      await fetch('/api/chat/new', { method: 'POST' })
    } catch {
      /* 后端不可达时仅清空本地，下轮请求自会报错 */
    }
  }

  /** 按空行分割 SSE 帧，兼容 LF（\n\n）与 CRLF（\r\n\r\n）两种行尾 */
  function extractFrames(buffer: string): { frames: string[]; rest: string } {
    const frames: string[] = []
    let rest = buffer
    for (;;) {
      const lf = rest.indexOf('\n\n')
      const crlf = rest.indexOf('\r\n\r\n')
      let frameEnd = -1
      let sepLen = 0
      if (lf >= 0 && (crlf < 0 || lf < crlf)) {
        frameEnd = lf
        sepLen = 2
      } else if (crlf >= 0) {
        frameEnd = crlf
        sepLen = 4
      }
      if (frameEnd < 0) break
      frames.push(rest.slice(0, frameEnd))
      rest = rest.slice(frameEnd + sepLen)
    }
    return { frames, rest }
  }

  /** 解析单个 SSE 帧（event:/data: 行）并更新消息状态 */
  function handleFrame(frame: string, msg: ChatMessage): void {
    let event = 'message'
    let data = ''
    for (const line of frame.split('\n')) {
      if (line.startsWith('event:')) {
        event = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        data += line.slice(5).trim()
      }
    }
    if (!data) return

    let payload: Record<string, string>
    try {
      payload = JSON.parse(data)
    } catch {
      return
    }

    if (event === 'thought') {
      const text = payload.text ?? ''
      const thought: Thought = { id: uid(), text, tool: detectTool(text) }
      msg.thoughts.push(thought)
    } else if (event === 'done') {
      msg.content = payload.content ?? ''
      msg.status = 'done'
    } else if (event === 'error') {
      msg.status = 'error'
      msg.error = payload.message ?? '处理消息时发生错误'
    }
  }

  return { sending, send, newSession }
}
