<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js/lib/core'
import java from 'highlight.js/lib/languages/java'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
import json from 'highlight.js/lib/languages/json'
import bash from 'highlight.js/lib/languages/bash'
import sql from 'highlight.js/lib/languages/sql'
import python from 'highlight.js/lib/languages/python'
import css from 'highlight.js/lib/languages/css'
import yaml from 'highlight.js/lib/languages/yaml'
import 'highlight.js/styles/github.css'

// 按需注册常用语言，避免全量引入
hljs.registerLanguage('java', java)
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('json', json)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('python', python)
hljs.registerLanguage('css', css)
hljs.registerLanguage('yaml', yaml)

const props = defineProps<{ content: string }>()

const bodyEl = ref<HTMLElement | null>(null)

/** 将工具产物路径（tmp/file/xxx.ext，兼容 / 与 \ 分隔）渲染为下载链接 */
function linkArtifacts(content: string): string {
  return content.replace(
    /(?:tmp[\\/]file[\\/])([\w\-.()\u4e00-\u9fa5]+)/g,
    (_match, name: string) => {
      const encoded = encodeURIComponent(name)
      return `[下载产物：${name}](/api/files/${encoded})`
    },
  )
}

// marked 渲染（breaks: true 保留 Agent 回复中的单个换行，gfm 开启表格/删除线等扩展），
// DOMPurify 消毒防 XSS
const html = computed(() => {
  const raw = marked.parse(linkArtifacts(props.content ?? ''), {
    async: false,
    breaks: true,
    gfm: true,
  }) as string
  return DOMPurify.sanitize(raw)
})

// 内容变化后对代码块做语法高亮
watch(
  html,
  () => {
    bodyEl.value?.querySelectorAll('pre code').forEach((el) => {
      hljs.highlightElement(el as HTMLElement)
    })
  },
  { immediate: true },
)
</script>

<template>
  <div ref="bodyEl" class="markdown" v-html="html"></div>
</template>
