<script setup lang="ts">
import { computed } from 'vue'
import type { ChatMessage } from '../types'
import ReActPipeline from './ReActPipeline.vue'
import MarkdownBody from './MarkdownBody.vue'

const props = defineProps<{ message: ChatMessage }>()

const isStreaming = computed(() => props.message.status === 'streaming')
</script>

<template>
  <div class="msg msg-assistant">
    <ReActPipeline :thoughts="message.thoughts" :active="isStreaming" />
    <p v-if="message.error" class="msg-error" role="alert">{{ message.error }}</p>
    <MarkdownBody v-if="message.content" :content="message.content" />
    <p v-else-if="isStreaming && message.thoughts.length === 0" class="msg-thinking">
      <span class="dots" aria-hidden="true">思考中</span>
    </p>
  </div>
</template>
