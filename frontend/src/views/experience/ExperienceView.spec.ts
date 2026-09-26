import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { Experience } from '@/api/experience'
import ExperienceView from './ExperienceView.vue'

vi.mock('@/api/experience', () => ({
  fetchExperiences: vi.fn(),
}))

// 时间轴含 ScrollTrigger/ECharts，单测 stub 掉，断言数据传递
vi.mock('@/components/experience/TimelineMain.vue', () => ({
  default: defineComponent({
    name: 'TimelineMain',
    props: { experiences: { type: Array, required: true } },
    template: '<div data-testid="timeline-stub"></div>',
  }),
}))

import { fetchExperiences } from '@/api/experience'

const mockedFetch = vi.mocked(fetchExperiences)

const FRONT_LAYOUT_STUB = { template: '<div><slot /></div>' }

function makeExp(id: number): Experience {
  return {
    id,
    company: `公司${id}`,
    title: `职位${id}`,
    startDate: '2021-01-01',
    endDate: null,
    achievements: [],
    radar: [
      { dimension: 'a', score: 80 },
      { dimension: 'b', score: 60 },
      { dimension: 'c', score: 90 },
    ],
    techTags: [],
    highlights: [],
    sortOrder: id,
  }
}

describe('工作经历页 ExperienceView', () => {
  beforeEach(() => {
    mockedFetch.mockReset()
  })

  it('加载成功：渲染时间轴并传入经历数据', async () => {
    mockedFetch.mockResolvedValue([makeExp(1), makeExp(2), makeExp(3)])
    const wrapper = mount(ExperienceView, {
      global: { stubs: { FrontLayout: FRONT_LAYOUT_STUB } },
    })

    expect(wrapper.find('[data-testid="timeline-stub"]').exists()).toBe(false) // 加载中
    await flushPromises()

    const stub = wrapper.findComponent({ name: 'TimelineMain' })
    expect(stub.exists()).toBe(true)
    expect(stub.props('experiences')).toHaveLength(3)
    expect(wrapper.find('.sub').text()).toContain('3 records')
  })

  it('加载失败：显示错误占位而非时间轴', async () => {
    mockedFetch.mockRejectedValue(new Error('boom'))
    const wrapper = mount(ExperienceView, {
      global: { stubs: { FrontLayout: FRONT_LAYOUT_STUB } },
    })
    await flushPromises()

    expect(wrapper.find('[data-testid="timeline-stub"]').exists()).toBe(false)
    expect(wrapper.find('.sub').text()).toContain('[error]')
  })
})
