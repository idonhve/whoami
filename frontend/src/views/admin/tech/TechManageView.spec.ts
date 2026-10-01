import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { TechItem } from '@/api/tech'

const mocks = vi.hoisted(() => ({
  fetchAdminTechStack: vi.fn(),
  createTechItem: vi.fn(),
  updateTechItem: vi.fn(),
  deleteTechItem: vi.fn(),
}))

vi.mock('@/api/tech', async (importOriginal) => {
  const original = await importOriginal<typeof import('@/api/tech')>()
  return {
    ...original,
    fetchAdminTechStack: mocks.fetchAdminTechStack,
    createTechItem: mocks.createTechItem,
    updateTechItem: mocks.updateTechItem,
    deleteTechItem: mocks.deleteTechItem,
  }
})

import TechManageView from './TechManageView.vue'

const SAMPLE: TechItem[] = [
  { id: 1, name: 'Vue 3', icon: 'vuejs', category: '前端', proficiency: 'master', weight: 8, sortOrder: 1 },
  { id: 2, name: 'MySQL', icon: null, category: '数据库', proficiency: 'proficient', weight: 6, sortOrder: 2 },
]

describe('后台技术栈管理页', () => {
  beforeEach(() => {
    mocks.fetchAdminTechStack.mockReset()
    mocks.createTechItem.mockReset()
    mocks.updateTechItem.mockReset()
    mocks.deleteTechItem.mockReset()
    mocks.fetchAdminTechStack.mockResolvedValue(SAMPLE)
  })

  it('加载后渲染技术项表格（名称/分类/熟练度/权重）', async () => {
    const wrapper = mount(TechManageView)
    await flushPromises()

    const text = wrapper.text()
    expect(text).toContain('Vue 3')
    expect(text).toContain('MySQL')
    expect(text).toContain('数据库')
    expect(text).toContain('精通')
  })

  it('新增：填写分类（自由文本）与字段后提交发送 create，请求体无 id', async () => {
    mocks.createTechItem.mockResolvedValue({ id: 99 })
    const wrapper = mount(TechManageView)
    await flushPromises()

    const inputs = wrapper.findAll('input')
    const nameInput = inputs[0]!
    const categoryInput = inputs[2]!
    const weightInput = inputs[3]!

    nameInput.setValue('Go')
    categoryInput.setValue('后端') // 分类下拉只提供建议值，可自由输入
    weightInput.setValue(7)

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mocks.createTechItem).toHaveBeenCalledTimes(1)
    const payload = mocks.createTechItem.mock.calls[0]![0] as Record<string, unknown>
    expect(payload).toMatchObject({ name: 'Go', category: '后端', weight: 7, proficiency: 'proficient' })
    expect(payload).not.toHaveProperty('id')
    // 保存后重新拉全量
    expect(mocks.fetchAdminTechStack).toHaveBeenCalledTimes(2)
  })

  it('编辑：点击编辑预填表单，提交走 update 且带 id', async () => {
    mocks.updateTechItem.mockResolvedValue(null)
    const wrapper = mount(TechManageView)
    await flushPromises()

    const editBtns = wrapper.findAll('.op-btn')
    await editBtns[0]!.trigger('click') // 编辑 Vue 3（下标 0 = 第一行操作列）

    const inputs = wrapper.findAll('input')
    expect((inputs[0]!.element as HTMLInputElement).value).toBe('Vue 3')
    expect((inputs[3]!.element as HTMLInputElement).value).toBe('8')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mocks.updateTechItem).toHaveBeenCalledTimes(1)
    const [id, payload] = mocks.updateTechItem.mock.calls[0]!
    expect(id).toBe(1)
    expect(payload).toMatchObject({ name: 'Vue 3', category: '前端' })
  })

  it('删除：确认后调用 delete 并 reload', async () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    mocks.deleteTechItem.mockResolvedValue(null)
    const wrapper = mount(TechManageView)
    await flushPromises()

    const deleteBtns = wrapper.findAll('.op-btn.danger')
    await deleteBtns[1]!.trigger('click') // 第二行 MySQL
    await flushPromises()

    expect(mocks.deleteTechItem).toHaveBeenCalledWith(2)
    expect(mocks.fetchAdminTechStack).toHaveBeenCalledTimes(2)
    confirmSpy.mockRestore()
  })

  it('分类输入带常用值 datalist 建议但不硬编码枚举', () => {
    const wrapper = mount(TechManageView)
    const datalist = wrapper.find('datalist#tech-categories')
    expect(datalist.exists()).toBe(true)
    const options = datalist.findAll('option').map((o) => o.attributes('value'))
    expect(options).toContain('前端')
    expect(options).toContain('后端')
    expect(options).toContain('数据库')
    expect(options).toContain('工具')
    expect(options).toContain('其他')
    // 分类输入框本身是自由文本 input（非受限 select）
    expect(wrapper.findAll('input[list="tech-categories"]')).toHaveLength(1)
  })
})