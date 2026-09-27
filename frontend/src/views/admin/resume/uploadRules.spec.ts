import { describe, expect, it } from 'vitest'

import { RESUME_MAX_BYTES, formatSize, validateResumeFile } from './uploadRules'

describe('简历上传前端预校验（仅 pdf ≤ 20MB）', () => {
  it('合法 PDF（MIME + 扩展名）通过', () => {
    const file = new File(['%PDF-1.7'], 'resume.pdf', { type: 'application/pdf' })
    expect(validateResumeFile(file)).toBeNull()
  })

  it('MIME 缺失但扩展名为 .pdf 也通过（部分浏览器不给 MIME）', () => {
    const file = new File(['%PDF-1.7'], 'RESUME.PDF', { type: '' })
    expect(validateResumeFile(file)).toBeNull()
  })

  it('非 PDF 类型被拒绝', () => {
    const file = new File(['plain'], 'note.txt', { type: 'text/plain' })
    expect(validateResumeFile(file)).toBe('仅支持 PDF 文件')
  })

  it('超过 20MB 被拒绝', () => {
    const big = new File([new Uint8Array(RESUME_MAX_BYTES + 1)], 'big.pdf', {
      type: 'application/pdf',
    })
    expect(validateResumeFile(big)).toBe('文件超过 20MB 上限')
  })

  it('空文件被拒绝', () => {
    const empty = new File([], 'empty.pdf', { type: 'application/pdf' })
    expect(validateResumeFile(empty)).toBe('文件为空')
  })
})

describe('formatSize', () => {
  it('B / KB / MB 分档展示', () => {
    expect(formatSize(512)).toBe('512 B')
    expect(formatSize(2048)).toBe('2.0 KB')
    expect(formatSize(3 * 1024 * 1024)).toBe('3.00 MB')
  })
})
