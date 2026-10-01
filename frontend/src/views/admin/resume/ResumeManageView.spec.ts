import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

import ResumeManageView from './ResumeManageView.vue'
import { fetchAdminResumes, restoreResume, uploadResume, type ResumeVersion } from '@/api/resume'

vi.mock('@/api/resume', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/resume')>()
  return {
    ...actual,
    fetchAdminResumes: vi.fn(),
    uploadResume: vi.fn(),
    restoreResume: vi.fn(),
  }
})

const mockedList = vi.mocked(fetchAdminResumes)
const mockedUpload = vi.mocked(uploadResume)
const mockedRestore = vi.mocked(restoreResume)

const VERSIONS: ResumeVersion[] = [
  { id: 3, versionNo: 3, displayName: '张三_简历_2026-08.pdf', sizeBytes: 210_000, isCurrent: true, uploadedAt: '2026-08-20T10:00:00' },
  { id: 2, versionNo: 2, displayName: '张三_简历_2026-06.pdf', sizeBytes: 198_000, isCurrent: false, uploadedAt: '2026-06-15T09:00:00' },
  { id: 1, versionNo: 1, displayName: '张三_简历_2026-04.pdf', sizeBytes: 180_000, isCurrent: false, uploadedAt: '2026-04-01T08:00:00' },
]

function setFile(input: HTMLInputElement, file: File) {
  const dt = { files: [file] }
  Object.defineProperty(input, 'files', { value: dt.files, configurable: true })
  input.dispatchEvent(new Event('change'))
}

describe('后台简历管理页（Spec 07）', () => {
  beforeEach(() => {
    mockedList.mockReset()
    mockedUpload.mockReset()
    mockedRestore.mockReset()
    localStorage.clear()
  })

  it('版本列表倒序渲染：versionNo / displayName / sizeBytes / isCurrent / uploadedAt', async () => {
    mockedList.mockResolvedValue(VERSIONS)
    const wrapper = mount(ResumeManageView)
    await flushPromises()

    const rows = wrapper.findAll('tbody tr')
    expect(rows).toHaveLength(3)
    expect(rows[0].find('.ver').text()).toBe('v3')
    expect(rows[0].find('.badge.current').exists()).toBe(true)
    expect(rows[0].find('.name').text()).toBe('张三_简历_2026-08.pdf')
    expect(rows[1].find('.badge.current').exists()).toBe(false)
    expect(rows[2].find('.ver').text()).toBe('v1')
  })

  it('空列表展示引导文案（前台按钮保持隐藏）', async () => {
    mockedList.mockResolvedValue([])
    const wrapper = mount(ResumeManageView)
    await flushPromises()

    expect(wrapper.find('.empty-cell').text()).toContain('尚未上传简历')
  })

  it('选择非 PDF 文件前端预校验拦截，不发起上传', async () => {
    mockedList.mockResolvedValue(VERSIONS)
    const wrapper = mount(ResumeManageView)
    await flushPromises()

    const input = wrapper.find('input[type="file"]').element as HTMLInputElement
    setFile(input, new File(['plain'], 'note.txt', { type: 'text/plain' }))
    await flushPromises()

    expect(wrapper.find('.error').text()).toContain('仅支持 PDF 文件')
    expect(wrapper.find('.sync-btn').attributes('disabled')).toBeDefined()
    expect(mockedUpload).not.toHaveBeenCalled()
  })

  it('选择超 20MB 的 PDF 被预校验拦截', async () => {
    mockedList.mockResolvedValue(VERSIONS)
    const wrapper = mount(ResumeManageView)
    await flushPromises()

    const input = wrapper.find('input[type="file"]').element as HTMLInputElement
    const big = new File([new Uint8Array(21 * 1024 * 1024)], 'big.pdf', { type: 'application/pdf' })
    setFile(input, big)
    await flushPromises()

    expect(wrapper.find('.error').text()).toContain('20MB')
    expect(mockedUpload).not.toHaveBeenCalled()
  })

  it('上传合法 PDF → 调上传接口 → 版本列表刷新并提示淘汰版本', async () => {
    mockedList
      .mockResolvedValueOnce(VERSIONS)
      .mockResolvedValueOnce([
        { id: 4, versionNo: 4, displayName: '张三_简历_2026-09.pdf', sizeBytes: 220_000, isCurrent: true, uploadedAt: '2026-09-10T10:00:00' },
        VERSIONS[0],
        VERSIONS[1],
      ])
    mockedUpload.mockResolvedValue({ id: 4, versionNo: 4, evictedVersionNos: [1] })

    const wrapper = mount(ResumeManageView)
    await flushPromises()

    const input = wrapper.find('input[type="file"]').element as HTMLInputElement
    setFile(input, new File(['%PDF-1.7'], 'resume.pdf', { type: 'application/pdf' }))
    await flushPromises()
    await wrapper.find('.sync-btn').trigger('click')
    await flushPromises()

    expect(mockedUpload).toHaveBeenCalledTimes(1)
    expect(mockedList).toHaveBeenCalledTimes(2)
    expect(wrapper.find('.notice').text()).toContain('v4')
    expect(wrapper.find('.notice').text()).toContain('v1')
  })

  it('回滚需确认弹窗：确认后调 restore 且 isCurrent 标记变化', async () => {
    const afterRestore: ResumeVersion[] = [
      { ...VERSIONS[0], isCurrent: false },
      { ...VERSIONS[1], isCurrent: true },
      VERSIONS[2],
    ]
    mockedList.mockResolvedValueOnce(VERSIONS).mockResolvedValueOnce(afterRestore)
    mockedRestore.mockResolvedValue(null)

    const wrapper = mount(ResumeManageView)
    await flushPromises()

    const rollbackBtns = wrapper.findAll('.ops .op-btn')
    expect(rollbackBtns).toHaveLength(2) // 当前版本无回滚按钮
    await rollbackBtns[0].trigger('click')
    await flushPromises()

    expect(wrapper.find('.confirm-overlay').exists()).toBe(true)
    expect(wrapper.find('.confirm-cmd').text()).toContain('v2')
    expect(mockedRestore).not.toHaveBeenCalled()

    await wrapper.find('.confirm-ops .op-btn.danger').trigger('click')
    await flushPromises()

    expect(mockedRestore).toHaveBeenCalledWith(2)
    expect(wrapper.find('.confirm-overlay').exists()).toBe(false)
    expect(wrapper.findAll('tbody tr')[1].find('.badge.current').exists()).toBe(true)
    expect(wrapper.find('.notice').text()).toContain('回滚')
  })

  it('取消回滚不触发接口', async () => {
    mockedList.mockResolvedValue(VERSIONS)
    const wrapper = mount(ResumeManageView)
    await flushPromises()

    await wrapper.findAll('.ops .op-btn')[1].trigger('click')
    await flushPromises()
    await wrapper.find('.confirm-ops .op-btn:not(.danger)').trigger('click')
    await flushPromises()

    expect(mockedRestore).not.toHaveBeenCalled()
    expect(wrapper.find('.confirm-overlay').exists()).toBe(false)
  })
})
