/** Agent 全部工具的英文名（与 ToolRegistration 注册名一致），用于思考文本的轻量识别 */
export const KNOWN_TOOLS = [
  'webSearch',
  'wallpaperSearch',
  'webScrape',
  'resourceDownload',
  'terminalExecute',
  'sandboxedFileOps',
  'pdfGenerate',
  'wordGenerate',
  'pptGenerate',
  'excelGenerate',
  'loadSkill',
  'doTerminate',
] as const

/** 在思考文本中识别工具名；未命中返回 null */
export function detectTool(text: string): string | null {
  const hit = KNOWN_TOOLS.find((t) => text.includes(t))
  return hit ?? null
}
