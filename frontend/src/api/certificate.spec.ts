import { afterEach, describe, expect, it, vi } from 'vitest'

import {
  CERT_MAX_SIZE_BYTES,
  createCertificate,
  deleteCertificate,
  fetchCertificates,
  updateCertificate,
  validateCertificateFile,
} from '@/api/certificate'
import { TOKEN_KEY } from '@/api/http'

function mockApiResponse(data: unknown) {
  const fetchMock = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: () => Promise.resolve({ code: 0, message: 'ok', data }),
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

function makeFile(type = 'image/jpeg', size = 1024): File {
  return new File([new ArrayBuffer(size)], 'cert.jpg', { type })
}

describe('证书模块 API', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  it('公开列表走 GET /api/certificates（免登录）', async () => {
    const fetchMock = mockApiResponse([])
    await fetchCertificates()
    expect(fetchMock).toHaveBeenCalledWith('/api/certificates', expect.anything())
  })

  it('上传走 multipart POST /admin/api/certificates（file + name + obtainedAt）', async () => {
    const fetchMock = mockApiResponse({ id: 9 })
    const file = makeFile('image/webp')
    const result = await createCertificate(file, 'AWS SAA', '2024-06-01')
    expect(result.id).toBe(9)

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [path, options] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(path).toBe('/admin/api/certificates')
    expect(options.method).toBe('POST')
    expect(options.body).toBeInstanceOf(FormData)
    const form = options.body as FormData
    expect(form.get('name')).toBe('AWS SAA')
    expect(form.get('obtainedAt')).toBe('2024-06-01')
    expect(form.get('file')).toBe(file)
    // multipart 边界由浏览器生成，前端不得手写 Content-Type
    const headers = options.headers as Record<string, string>
    expect(headers['Content-Type']).toBeUndefined()
  })

  it('上传自动附带 Bearer token（有登录态时）', async () => {
    const fetchMock = mockApiResponse({ id: 10 })
    localStorage.setItem(TOKEN_KEY, 'tok-1')
    await createCertificate(makeFile(), 'PMP', '2023-03-15')

    const options = fetchMock.mock.calls[0]![1] as RequestInit
    expect((options.headers as Record<string, string>).Authorization).toBe('Bearer tok-1')
  })

  it('更新走 PUT /admin/api/certificates/{id}', async () => {
    const fetchMock = mockApiResponse(null)
    await updateCertificate(3, { name: '新名称', sortOrder: 5 })
    expect(fetchMock).toHaveBeenCalledWith(
      '/admin/api/certificates/3',
      expect.objectContaining({ method: 'PUT' }),
    )
  })

  it('删除走 DELETE /admin/api/certificates/{id}', async () => {
    const fetchMock = mockApiResponse(null)
    await deleteCertificate(5)
    expect(fetchMock).toHaveBeenCalledWith(
      '/admin/api/certificates/5',
      expect.objectContaining({ method: 'DELETE' }),
    )
  })

  it('上传预校验：类型不合法 / 超过 5MB 返回错误文案', () => {
    expect(validateCertificateFile(makeFile('image/gif'))).toContain('jpg')
    expect(validateCertificateFile(new File([new ArrayBuffer(CERT_MAX_SIZE_BYTES + 1)], 'big.png', { type: 'image/png' }))).toContain('5MB')
  })

  it('上传预校验：jpg / png / webp 且不超限返回 null', () => {
    expect(validateCertificateFile(makeFile('image/jpeg'))).toBeNull()
    expect(validateCertificateFile(makeFile('image/png'))).toBeNull()
    expect(validateCertificateFile(makeFile('image/webp'))).toBeNull()
  })
})
