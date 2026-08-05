<script setup lang="ts">
const emit = defineEmits<{ (e: 'new-session'): void; (e: 'logout'): void }>()
</script>

<template>
  <header class="header">
    <div class="header-inner">
      <span class="brand" aria-label="MyClaw">MyClaw</span>
      <span class="brand-sub" aria-hidden="true">ReAct Agent</span>
      <div class="header-actions">
        <button class="btn-ghost" type="button" @click="emit('new-session')">新建会话</button>
        <button class="btn-ghost" type="button" @click="emit('logout')">登出</button>
      </div>
    </div>
  </header>
</template>
