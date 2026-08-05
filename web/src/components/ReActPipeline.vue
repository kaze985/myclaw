<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Thought } from '../types'
import { detectTool } from '../utils/toolNames'

const props = defineProps<{
  thoughts: Thought[]
  /** 是否仍在流式生成（最后一步节点高亮脉冲） */
  active: boolean
}>()

const collapsed = ref(false)

const steps = computed(() =>
  props.thoughts.map((t) => ({ ...t, tool: t.tool ?? detectTool(t.text) })),
)

function toggle(): void {
  collapsed.value = !collapsed.value
}
</script>

<template>
  <div v-if="thoughts.length" class="pipeline" :class="{ 'pipeline-collapsed': collapsed }">
    <button
      type="button"
      class="pipeline-toggle"
      :aria-expanded="!collapsed"
      @click="toggle"
    >
      <span class="pipeline-label">思考过程</span>
      <span class="pipeline-count">{{ steps.length }} 步</span>
      <span class="pipeline-arrow" aria-hidden="true">{{ collapsed ? '›' : '⌄' }}</span>
    </button>
    <ol v-show="!collapsed" class="pipeline-steps">
      <li
        v-for="(step, i) in steps"
        :key="step.id"
        class="pipeline-step"
        :class="{ 'pipeline-step-active': active && i === steps.length - 1 }"
      >
        <span v-if="step.tool" class="tool-badge" :title="`工具：${step.tool}`">{{ step.tool }}</span>
        <span class="step-text">{{ step.text }}</span>
      </li>
    </ol>
  </div>
</template>
