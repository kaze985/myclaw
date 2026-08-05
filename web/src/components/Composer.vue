<script setup lang="ts">
import { ref } from 'vue'

defineProps<{ sending: boolean }>()
const emit = defineEmits<{ (e: 'send', text: string): void }>()

const text = ref('')

function submit(): void {
  const trimmed = text.value.trim()
  if (!trimmed) return
  emit('send', trimmed)
  text.value = ''
}

// Enter 发送、Shift+Enter 换行（由 keydown.enter.exact 处理）
</script>

<template>
  <div class="composer">
    <div class="composer-inner">
      <textarea
        v-model="text"
        rows="1"
        :disabled="sending"
        placeholder="输入消息，Enter 发送，Shift+Enter 换行"
        aria-label="消息输入"
        @keydown.enter.exact.prevent="submit"
      ></textarea>
      <button type="button" :disabled="sending || !text.trim()" @click="submit">
        {{ sending ? '思考中…' : '发送' }}
      </button>
    </div>
  </div>
</template>
