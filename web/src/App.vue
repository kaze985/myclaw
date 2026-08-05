<script setup lang="ts">
import { onMounted } from 'vue'
import LoginView from './components/LoginView.vue'
import ChatView from './components/ChatView.vue'
import { useAuth } from './composables/useAuth'

const { authenticated, checking, check, logout } = useAuth()

function handleLoggedIn(): void {
  authenticated.value = true
}

async function handleLogout(): Promise<void> {
  await logout()
}

onMounted(check)
</script>

<template>
  <div class="app-shell">
    <div v-if="checking" class="boot">加载中…</div>
    <LoginView v-else-if="!authenticated" @logged-in="handleLoggedIn" />
    <ChatView v-else @logout="handleLogout" />
  </div>
</template>
