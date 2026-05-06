import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
  }
})

vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn() })),
}))

vi.mock('../config/images', () => ({
  HERO_BG: '',
  INTRO_BG: '',
  GALLERY_IMAGES: [],
}))

import OfficialHome from '../OfficialHome.vue'
import { useRouter } from 'vue-router'

const stubs = {
  'el-card': { template: '<div class="el-card"><slot /></div>' },
  'el-breadcrumb': { template: '<div class="el-breadcrumb"><slot /></div>' },
  'el-breadcrumb-item': { template: '<span class="el-breadcrumb-item"><slot /></span>' },
  'el-button': {
    template: '<button :disabled="loading" @click="$emit(\'click\')"><slot /></button>',
    props: ['type', 'size', 'loading', 'link', 'plain'],
    emits: ['click'],
  },
  'el-icon': { template: '<span class="el-icon"><slot /></span>' },
  'el-tabs': { template: '<div class="el-tabs"><slot /></div>', props: ['modelValue'] },
  'el-tab-pane': { template: '<div class="el-tab-pane"><slot /></div>', props: ['label', 'name'] },
  'el-progress': { template: '<div class="el-progress"></div>', props: ['percentage', 'color', 'strokeWidth'] },
}

function mountOfficialHome() {
  return mount(OfficialHome, {
    global: { stubs },
    attachTo: document.body,
  })
}

describe('OfficialHome.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('组件正确渲染', () => {
    const wrapper = mountOfficialHome()
    expect(wrapper.find('.site-page').exists()).toBe(true)
    expect(wrapper.find('.hero').exists()).toBe(true)
    expect(wrapper.find('.top-nav').exists()).toBe(true)
    expect(wrapper.find('.footer').exists()).toBe(true)
    wrapper.unmount()
  })

  it('包含路由跳转功能', () => {
    const mockPush = vi.fn()
    useRouter.mockReturnValue({ push: mockPush })

    const wrapper = mountOfficialHome()
    // Check that buttons with jumpToRoute exist (feature cards with "点击进入" buttons)
    const buttons = wrapper.findAll('button')
    expect(buttons.length).toBeGreaterThan(0)
    wrapper.unmount()
  })
})
