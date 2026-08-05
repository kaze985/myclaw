<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import type { ChatMessage } from '../types'
import UserMessage from './UserMessage.vue'
import AssistantMessage from './AssistantMessage.vue'

const props = defineProps<{ messages: ChatMessage[] }>()

const listEl = ref<HTMLElement | null>(null)

// 新消息到达时滚动到底部
watch(
  () => props.messages.length,
  async () => {
    await nextTick()
    const el = listEl.value
    if (el) el.scrollTop = el.scrollHeight
  },
)
</script>

<template>
  <div ref="listEl" class="message-list" role="log" aria-live="polite">
    <template v-for="m in messages" :key="m.id">
      <UserMessage v-if="m.role === 'user'" :message="m" />
      <AssistantMessage v-else :message="m" />
    </template>
  </div>
</template>
