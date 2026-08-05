<script setup lang="ts">
import { ref } from 'vue'
import { useAuth } from '../composables/useAuth'

const emit = defineEmits<{ (e: 'logged-in'): void }>()

const { login } = useAuth()
const password = ref('')
const error = ref('')
const busy = ref(false)

async function submit(): Promise<void> {
  if (busy.value) return
  busy.value = true
  error.value = ''
  const ok = await login(password.value)
  busy.value = false
  if (ok) {
    emit('logged-in')
  } else {
    error.value = '密码错误，请重试'
    password.value = ''
  }
}
</script>

<template>
  <main class="login">
    <div class="login-card">
      <p class="login-brand">MyClaw</p>
      <h1 class="login-title">登录</h1>
      <p class="login-hint">输入访问密码以连接 Agent</p>
      <form class="login-form" @submit.prevent="submit">
        <input
          v-model="password"
          type="password"
          autocomplete="current-password"
          placeholder="访问密码"
          aria-label="访问密码"
          autofocus
        />
        <button type="submit" :disabled="busy || !password">
          {{ busy ? '验证中…' : '进入' }}
        </button>
      </form>
      <p v-if="error" class="login-error" role="alert">{{ error }}</p>
    </div>
  </main>
</template>
