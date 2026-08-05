<script setup lang="ts">
import HeaderBar from './HeaderBar.vue'
import MessageList from './MessageList.vue'
import Composer from './Composer.vue'
import { useLocalHistory } from '../composables/useLocalHistory'
import { useChat } from '../composables/useChat'

const emit = defineEmits<{ (e: 'logout'): void }>()

const { messages, clear } = useLocalHistory()
const { sending, send, newSession } = useChat(messages, {
  onUnauthorized: () => emit('logout'),
})

async function handleNewSession(): Promise<void> {
  await newSession()
  clear()
}
</script>

<template>
  <div class="chat">
    <HeaderBar @new-session="handleNewSession" @logout="emit('logout')" />
    <p class="chat-notice">
      与飞书通道共享同一会话上下文，两端对话互相可见；「新建会话」会同时清空飞书上下文。
    </p>
    <MessageList :messages="messages" />
    <Composer :sending="sending" @send="send" />
  </div>
</template>
