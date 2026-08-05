<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { ChatMessage } from '../types'
import ReActPipeline from './ReActPipeline.vue'
import MarkdownBody from './MarkdownBody.vue'

const props = defineProps<{ message: ChatMessage }>()

const isStreaming = computed(() => props.message.status === 'streaming')

// ---------- 打字机效果 ----------
// 数据源：message.content 由流式 token 增量追加（真·流式）或 done 一次性定稿。
// 打字机 interval 从 content 头部逐字推进；流式期间追赶 content，done 后打完全文停止。
const displayText = ref('')
const typing = ref(false)
let timer: number | undefined
let mounted = false
let cursor = 0

const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

/** 长文本自动加速：每 tick 追加的字符数 */
function stepFor(len: number): number {
  if (len <= 120) return 1
  if (len <= 400) return 2
  if (len <= 900) return 4
  return 6
}

function stopTyping(): void {
  if (timer !== undefined) {
    clearInterval(timer)
    timer = undefined
  }
  typing.value = false
  displayText.value = props.message.content
}

function tick(): void {
  const full = props.message.content
  cursor = Math.min(cursor + stepFor(full.length), full.length)
  displayText.value = full.slice(0, cursor)
  if (cursor >= full.length && !isStreaming.value) {
    stopTyping()
  }
}

/** 内容非空且尚未打字时启动打字机；reduced-motion 直接全文 */
function ensureTyping(): void {
  if (reducedMotion) {
    displayText.value = props.message.content
    return
  }
  if (typing.value) return
  typing.value = true
  cursor = 0
  displayText.value = ''
  timer = window.setInterval(tick, 18)
}

/** 点击消息跳过打字，立即显示全文 */
function skipTyping(): void {
  if (typing.value) {
    stopTyping()
  }
}

// 挂载时已是 done 的历史消息：直接全文，不触发打字
onMounted(() => {
  mounted = true
  displayText.value = props.message.content
})

// 流式 token 到达（content 增量增长）→ 启动/继续打字
watch(
  () => props.message.content,
  (newContent) => {
    if (!mounted || !newContent) return
    ensureTyping()
  },
)

// done 收尾：未在打字（reduced-motion/内容过短）则直接全文
watch(
  () => props.message.status,
  (status) => {
    if (!mounted) return
    if (status === 'done' && !typing.value) {
      stopTyping()
    }
  },
)

onBeforeUnmount(() => {
  if (timer !== undefined) {
    clearInterval(timer)
  }
})
</script>

<template>
  <div
    class="msg msg-assistant"
    :class="{ 'msg-typing': typing }"
    :title="typing ? '点击跳过打字' : undefined"
    @click="skipTyping"
  >
    <ReActPipeline :thoughts="message.thoughts" :active="isStreaming" />
    <p v-if="message.error" class="msg-error" role="alert">{{ message.error }}</p>
    <template v-if="displayText">
      <MarkdownBody :content="displayText" />
      <span v-if="typing" class="type-caret" aria-hidden="true"></span>
    </template>
    <!-- 发送后到首段内容之间：立即显示闪烁光标，营造「正在回复」的即时感 -->
    <template v-else-if="isStreaming">
      <span class="type-caret type-caret-waiting" aria-hidden="true"></span>
      <span class="sr-only">正在生成回复</span>
    </template>
  </div>
</template>
