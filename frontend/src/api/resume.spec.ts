import { afterEach, describe, expect, it, vi } from 'vitest'

import {
  RESUME_DOWNLOAD_URL,
  fetchAdminResumes,
  getLatest,
  restoreResume,
  uploadResume,
} from '@/api/resume'

type Stub = { ok: boolean; status: number; json: () => Promise<{ code: number; data: unknown }> }

function stubFetch(handler: (path: string, init?: RequestInit) => Stub) {
  vi.stubGlobal('fetch', vi.fn((path: string, init?: RequestInit) => Promise.resolve(handler(path, init))))
}

afterEach(() => {
  vi.unstubAllGlobals()
  localStorage.clear()
})

describe('简历 API（Spec 07）', () => {
  it('latest GET /api/resume/latest（公开，按钮显隐与文案）', async () => {
    const calls: { path: string; init?: RequestInit }[] = []
    stubFetch((path, init) => {
      calls.push({ path, init })
      return {
        ok: true,
        status: 200,
        json: () =>
          Promise.resolve({
            code: 0,
            data: { exists: true, displayName: '张三_简历_2026-08.pdf', updatedAt: '2026-08-20T10:00:00' },
          }),
      }
    })

    const latest = await getLatest()
    expect(calls[0].path).toBe('/api/resume/latest')
    expect((calls[0].init?.method ?? 'GET').toUpperCase()).toBe('GET')
    expect(latest.exists).toBe(true)
    expect(latest.displayName).toBe('张三_简历_2026-08.pdf')
  })

  it('后台版本列表 GET /admin/api/resumes', async () => {
    const paths: string[] = []
    stubFetch((path) => {
      paths.push(path)
      return { ok: true, status: 200, json: () => Promise.resolve({ code: 0, data: [] }) }
    })
    await fetchAdminResumes()
    expect(paths[0]).toBe('/admin/api/resumes')
  })

  it('上传 POST /admin/api/resumes 为 multipart（FormData，不强制 JSON Content-Type）', async () => {
    const calls: { init?: RequestInit }[] = []
    stubFetch((_path, init) => {
      calls.push({ init })
      return {
        ok: true,
        status: 200,
        json: () => Promise.resolve({ code: 0, data: { id: 3, versionNo: 3, evictedVersionNos: [1] } }),
      }
    })

    const file = new File(['%PDF-1.7'], 'resume.pdf', { type: 'application/pdf' })
    const result = await uploadResume(file)

    expect(calls[0].init?.method).toBe('POST')
    expect(calls[0].init?.body).toBeInstanceOf(FormData)
    const sent = calls[0].init?.body as FormData
    expect(sent.get('file')).toBeInstanceOf(File)
    const headers = (calls[0].init?.headers ?? {}) as Record<string, string>
    expect(headers['Content-Type']).toBeUndefined()
    expect(result).toEqual({ id: 3, versionNo: 3, evictedVersionNos: [1] })
  })

  it('上传 400 时抛出带后端原因的错误', async () => {
    stubFetch(() => ({
      ok: false,
      status: 400,
      json: () => Promise.resolve({ code: 400, data: null, message: '仅支持 PDF 文件' }),
    }))
    const file = new File(['plain'], 'note.txt', { type: 'text/plain' })
    await expect(uploadResume(file)).rejects.toThrow('仅支持 PDF 文件')
  })

  it('回滚 PUT /admin/api/resumes/{id}/restore', async () => {
    const calls: { path: string; init?: RequestInit }[] = []
    stubFetch((path, init) => {
      calls.push({ path, init })
      return { ok: true, status: 200, json: () => Promise.resolve({ code: 0, data: null }) }
    })
    await restoreResume(7)
    expect(calls[0].path).toBe('/admin/api/resumes/7/restore')
    expect(calls[0].init?.method).toBe('PUT')
  })

  it('下载地址固定为 /api/resume/download（服务端埋点 + Content-Disposition 文件名）', () => {
    expect(RESUME_DOWNLOAD_URL).toBe('/api/resume/download')
  })
})
