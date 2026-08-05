/** Agent 单步思考（onThought 推送的 think 阶段文本） */
export interface Thought {
  id: string
  text: string
  /** 从思考文本中轻量识别出的工具名，未命中为 null */
  tool: string | null
}

export type MessageStatus = 'streaming' | 'done' | 'error'

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  /** 最终回复（Markdown 文本） */
  content: string
  /** 思考/工具调用管线 */
  thoughts: Thought[]
  status: MessageStatus
  error?: string
  createdAt: number
}
