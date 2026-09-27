/**
 * 简历上传前端预校验（Spec 07：仅 pdf、≤ 20MB）。
 * 后端仍会二次校验（400），此处只为提前拦截、给出明确原因。
 */

export const RESUME_MAX_BYTES = 20 * 1024 * 1024

/** 返回 null 表示通过；否则为可直接展示的错误原因 */
export function validateResumeFile(file: File): string | null {
  const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf')
  if (!isPdf) return '仅支持 PDF 文件'
  if (file.size === 0) return '文件为空'
  if (file.size > RESUME_MAX_BYTES) return '文件超过 20MB 上限'
  return null
}

/** 字节数人性化展示（B/KB/MB） */
export function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
}
