import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'

import type { Experience } from '@/api/experience'
import ExpandableCard from './ExpandableCard.vue'

// 雷达图由 ECharts canvas 渲染，单测只 stub 掉，断言其收到正确 radar 数据
vi.mock('./RadarChart.vue', () => ({
  default: defineComponent({
    name: 'RadarChart',
    props: { radar: { type: Array, required: true }, height: { type: String, default: '220px' } },
    template: '<div data-testid="radar-stub"></div>',
  }),
}))

function makeExp(overrides: Partial<Experience> = {}): Experience {
  return {
    id: 1,
    company: '星云科技',
    title: '资深前端工程师',
    startDate: '2021-06-01',
    endDate: null,
    achievements: [
      { value: '300%', context: '核心接口性能提升' },
      { value: '50w+', context: '日活用户规模' },
    ],
    radar: [
      { dimension: '架构', score: 88 },
      { dimension: '沟通', score: 92 },
      { dimension: '工程', score: 76 },
      { dimension: '产品', score: 84 },
    ],
    techTags: ['vuejs', 'typescript', 'spring'],
    highlights: ['主导前端基建重构', '搭建数据看板上线'],
    sortOrder: 0,
    ...overrides,
  }
}

describe('经历卡 ExpandableCard', () => {
  it('默认态摘要（公司 · 职位 · 时间）字符数 ≤ 30', () => {
    const wrapper = mount(ExpandableCard, { props: { experience: makeExp() } })
    const summary = wrapper.find('[data-testid="exp-summary"]').text()
    expect(summary.length).toBeLessThanOrEqual(30)
    expect(summary).toContain('星云科技')
    expect(summary).toContain('资深前端工程师')
  })

  it('默认态渲染视觉卡片：战果翻牌 + 技术标签图标云 + 雷达维度', () => {
    const wrapper = mount(ExpandableCard, { props: { experience: makeExp() } })

    const achievements = wrapper.find('[data-testid="achievements"]')
    expect(achievements.exists()).toBe(true)
    expect(achievements.findAll('.ach')).toHaveLength(2)

    const tagCloud = wrapper.find('[data-testid="tech-tag-cloud"]')
    expect(tagCloud.exists()).toBe(true)
    expect(tagCloud.findAll('.tag')).toHaveLength(3)

    const radarStub = wrapper.findComponent({ name: 'RadarChart' })
    expect(radarStub.exists()).toBe(true)
    expect(radarStub.props('radar')).toHaveLength(4)
  })

  it('默认态不渲染展开要点；点击后展开显示要点列表', async () => {
    const wrapper = mount(ExpandableCard, { props: { experience: makeExp() } })

    // 展开区以 v-if 挂载：默认态不存在
    expect(wrapper.find('[data-testid="exp-highlights"]').exists()).toBe(false)
    expect(wrapper.find('.exp-toggle').attributes('aria-expanded')).toBe('false')

    await wrapper.find('.exp-toggle').trigger('click')
    const details = wrapper.find('[data-testid="exp-highlights"]')
    expect(details.exists()).toBe(true)
    expect(wrapper.find('.exp-toggle').attributes('aria-expanded')).toBe('true')
    expect(details.findAll('.hl-item')).toHaveLength(2)
    expect(details.text()).toContain('主导前端基建重构')

    // 再点收起
    await wrapper.find('.exp-toggle').trigger('click')
    expect(wrapper.find('[data-testid="exp-highlights"]').exists()).toBe(false)
  })
})
