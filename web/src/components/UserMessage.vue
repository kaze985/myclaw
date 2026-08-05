<script setup lang="ts">
import type { ChatMessage } from '../types'

defineProps<{ message: ChatMessage }>()
</script>

<template>
  <div class="msg msg-user">
    <div class="msg-bubble">{{ message.content }}</div>
  </div>
</template>
