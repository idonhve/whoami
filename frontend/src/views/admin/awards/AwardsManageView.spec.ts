import { flushPromises, mount } from '@vue/test-utils'
import type { VueWrapper } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { Certificate } from '@/api/certificate'

const mocks = vi.hoisted(() => ({
  fetchCertificates: vi.fn(),
  createCertificate: vi.fn(),
  updateCertificate: vi.fn(),
  deleteCertificate: vi.fn(),
}))

vi.mock('@/api/certificate', async (importOriginal) => {
  // 保留 validateCertificateFile / CERT_MAX_SIZE_BYTES 真实现，只 mock 网络函数
  const original = await importOriginal<typeof import('@/api/certificate')>()
  return {
    ...original,
    fetchCertificates: mocks.fetchCertificates,
    createCertificate: mocks.createCertificate,
    updateCertificate: mocks.updateCertificate,
    deleteCertificate: mocks.deleteCertificate,
  }
})

import AwardsManageView from './AwardsManageView.vue'

const SAMPLE: Certificate[] = [
  {
    id: 1,
    name: 'AWS Solutions Architect',
    obtainedAt: '2024-06-01',
    thumbUrl: '/uploads/certificate/1/thumb.webp',
    imageUrl: '/uploads/certificate/1/image.webp',
    sortOrder: 1,
  },
  {
    id: 2,
    name: 'PMP 项目管理',
    obtainedAt: '2023-03-15',
    thumbUrl: '/uploads/certificate/2/thumb.webp',
    imageUrl: '/uploads/certificate/2/image.webp',
    sortOrder: 2,
  },
]

function makeFile(type: string, size = 2048): File {
  return new File([new ArrayBuffer(size)], 'cert.png', { type })
}

/** 原生 file input 不能直接 setValue，用 defineProperty 注入 files 后触发 change */
async function pickFile(wrapper: VueWrapper, file: File) {
  const input = wrapper.find('input[type="file"]')
  Object.defineProperty(input.element, 'files', { value: [file], configurable: true })
  await input.trigger('change')
}

describe('后台证书管理页', () => {
  beforeEach(() => {
    mocks.fetchCertificates.mockReset()
    mocks.createCertificate.mockReset()
    mocks.updateCertificate.mockReset()
    mocks.deleteCertificate.mockReset()
    mocks.fetchCertificates.mockResolvedValue(SAMPLE)
  })

  it('加载后渲染列表（缩略图/名称/获取时间/排序）', async () => {
    const wrapper = mount(AwardsManageView)
    await flushPromises()

    const rows = wrapper.findAll('tbody tr')
    expect(rows).toHaveLength(2)
    expect(wrapper.find('.thumb-img').attributes('src')).toBe(SAMPLE[0]!.thumbUrl)
    expect(wrapper.text()).toContain('AWS Solutions Architect')
    expect(wrapper.text()).toContain('PMP 项目管理')
    expect(wrapper.text()).toContain('2024-06-01')
    expect(wrapper.text()).toContain('2023-03-15')
  })

  it('上传：选文件填表提交走 create，成功后清空表单并刷新列表', async () => {
    mocks.createCertificate.mockResolvedValue({ id: 9 })
    const wrapper = mount(AwardsManageView)
    await flushPromises()

    const file = makeFile('image/webp')
    await pickFile(wrapper, file)
    await wrapper.find('.name-input').setValue('CKA 认证')
    await wrapper.find('.date-input').setValue('2025-01-10')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mocks.createCertificate).toHaveBeenCalledTimes(1)
    expect(mocks.createCertificate).toHaveBeenCalledWith(file, 'CKA 认证', '2025-01-10')
    // 刷新列表
    expect(mocks.fetchCertificates).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('[ok]')
  })

  it('上传预校验：未选文件提交被拦下', async () => {
    const wrapper = mount(AwardsManageView)
    await flushPromises()

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mocks.createCertificate).not.toHaveBeenCalled()
    expect(wrapper.find('.error').text()).toContain('请选择图片文件')
  })

  it('上传预校验：类型不合法 / 超过 5MB 被拦下', async () => {
    const wrapper = mount(AwardsManageView)
    await flushPromises()

    await pickFile(wrapper, makeFile('image/gif'))
    await wrapper.find('.name-input').setValue('GIF 图')
    await wrapper.find('.date-input').setValue('2025-01-10')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mocks.createCertificate).not.toHaveBeenCalled()
    expect(wrapper.find('.error').text()).toContain('jpg')

    // 换成超大 jpg（6MB）同样拦下
    await pickFile(wrapper, makeFile('image/jpeg', 6 * 1024 * 1024))
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mocks.createCertificate).not.toHaveBeenCalled()
    expect(wrapper.find('.error').text()).toContain('5MB')
  })

  it('编辑：预填 name/obtainedAt/sortOrder，保存走 update 且带 id', async () => {
    mocks.updateCertificate.mockResolvedValue(null)
    const wrapper = mount(AwardsManageView)
    await flushPromises()

    await wrapper.findAll('.op-btn.edit')[0]!.trigger('click')

    const nameEdit = wrapper.find('.name-edit')
    expect((nameEdit.element as HTMLInputElement).value).toBe('AWS Solutions Architect')
    expect((wrapper.find('.date-edit').element as HTMLInputElement).value).toBe('2024-06-01')
    expect((wrapper.find('.sort-edit').element as HTMLInputElement).value).toBe('1')

    await nameEdit.setValue('AWS SAA（更新）')
    await wrapper.find('.sort-edit').setValue('5')
    await wrapper.findAll('.op-btn.save')[0]!.trigger('click')
    await flushPromises()

    expect(mocks.updateCertificate).toHaveBeenCalledTimes(1)
    const [id, patch] = mocks.updateCertificate.mock.calls[0]! as [
      number,
      Record<string, unknown>,
    ]
    expect(id).toBe(1)
    expect(patch).toMatchObject({
      name: 'AWS SAA（更新）',
      obtainedAt: '2024-06-01',
      sortOrder: 5,
    })
    expect(mocks.fetchCertificates).toHaveBeenCalledTimes(2)
  })

  it('编辑：名称清空保存被前端拦下', async () => {
    const wrapper = mount(AwardsManageView)
    await flushPromises()

    await wrapper.findAll('.op-btn.edit')[0]!.trigger('click')
    await wrapper.find('.name-edit').setValue('   ')
    await wrapper.findAll('.op-btn.save')[0]!.trigger('click')
    await flushPromises()

    expect(mocks.updateCertificate).not.toHaveBeenCalled()
    expect(wrapper.find('.error').text()).toContain('名称不能为空')
  })

  it('删除：确认后调用 delete 并刷新', async () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    mocks.deleteCertificate.mockResolvedValue(null)
    const wrapper = mount(AwardsManageView)
    await flushPromises()

    await wrapper.findAll('.op-btn.danger')[1]!.trigger('click') // 第二行 PMP
    await flushPromises()

    expect(mocks.deleteCertificate).toHaveBeenCalledWith(2)
    expect(mocks.fetchCertificates).toHaveBeenCalledTimes(2)
    confirmSpy.mockRestore()
  })

  it('删除：取消确认不动数据', async () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false)
    const wrapper = mount(AwardsManageView)
    await flushPromises()

    await wrapper.findAll('.op-btn.danger')[0]!.trigger('click')
    await flushPromises()

    expect(mocks.deleteCertificate).not.toHaveBeenCalled()
    expect(mocks.fetchCertificates).toHaveBeenCalledTimes(1)
    confirmSpy.mockRestore()
  })

  it('空列表显示占位提示', async () => {
    mocks.fetchCertificates.mockResolvedValue([])
    const wrapper = mount(AwardsManageView)
    await flushPromises()

    expect(wrapper.text()).toContain('暂无证书')
  })
})
