import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('../../api/emission', () => ({
  getMyRating: vi.fn(() => Promise.resolve([])),
  getIndustryRankings: vi.fn(() => Promise.resolve({ items: [] })),
  predictEmission: vi.fn(() => Promise.resolve({ data: {} })),
}))

vi.mock('../../api/enterprise', () => ({
  getEnterpriseInfo: vi.fn(() => Promise.resolve({ id: 1001 })),
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
    ElMessageBox: { confirm: vi.fn(() => Promise.resolve()) },
  }
})

import EmissionData from '../enterprise/EmissionData.vue'
import { getMyRating, getIndustryRankings } from '../../api/emission'
import { ElMessage } from 'element-plus'

const stubs = {
  'el-card': { template: '<div class="el-card"><slot /></div>' },
  'el-breadcrumb': { template: '<div class="el-breadcrumb"><slot /></div>' },
  'el-breadcrumb-item': { template: '<span class="el-breadcrumb-item"><slot /></span>' },
  'el-form': {
    template: '<form @submit.prevent><slot /></form>',
    methods: {
      validate() { return Promise.resolve(true) },
      resetFields() {},
    },
  },
  'el-form-item': { template: '<div class="el-form-item"><slot /></div>', props: ['label', 'prop'] },
  'el-input': {
    template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
    props: ['modelValue', 'type', 'placeholder', 'showPassword', 'clearable', 'rows'],
    emits: ['update:modelValue'],
  },
  'el-button': {
    template: '<button :disabled="loading" @click="$emit(\'click\')"><slot /></button>',
    props: ['type', 'size', 'loading', 'link', 'plain'],
    emits: ['click'],
  },
  'el-table': {
    template: '<table><slot /><slot name="append" /></table>',
    props: ['data', 'border', 'emptyText'],
  },
  'el-table-column': {
    template: '<td><slot :row="{}" :$index="0" /></td>',
    props: ['prop', 'label', 'minWidth', 'width', 'fixed', 'showOverflowTooltip'],
  },
  'el-tag': { template: '<span class="el-tag"><slot /></span>', props: ['type'] },
  'el-pagination': {
    template: '<div class="el-pagination"></div>',
    props: ['currentPage', 'pageSize', 'background', 'pageSizes', 'layout', 'total'],
    emits: ['size-change', 'current-change', 'update:current-page', 'update:page-size'],
  },
  'el-dialog': {
    template: '<div class="el-dialog" v-if="modelValue"><slot /><slot name="footer" /></div>',
    props: ['modelValue', 'title', 'width', 'destroyOnClose'],
    emits: ['update:modelValue'],
  },
  'el-select': {
    template: '<select :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><slot /></select>',
    props: ['modelValue', 'placeholder', 'style', 'clearable'],
    emits: ['update:modelValue'],
  },
  'el-tabs': {
    template: '<div class="el-tabs"><slot /></div>',
    props: ['modelValue'],
    emits: ['update:modelValue'],
  },
  'el-tab-pane': { template: '<div class="el-tab-pane"><slot /></div>', props: ['label', 'name', 'disabled'] },
  'el-option': {
    template: '<option :value="value"><slot /></option>',
    props: ['label', 'value'],
  },
  'el-date-picker': {
    template: '<input class="date-picker" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
    props: ['modelValue', 'type', 'valueFormat', 'placeholder'],
    emits: ['update:modelValue', 'change'],
  },
  'el-input-number': {
    template: '<input role="spinbutton" :value="modelValue ?? \'\'" @input="$emit(\'update:modelValue\', Number($event.target.value))" />',
    props: ['modelValue', 'min', 'max'],
    emits: ['update:modelValue'],
  },
  'el-alert': { template: '<div class="el-alert"><slot /></div>', props: ['title', 'type', 'closable', 'showIcon'] },
  'el-descriptions': { template: '<div class="el-descriptions"><slot /></div>', props: ['title', 'column', 'border'] },
  'el-descriptions-item': { template: '<div class="el-descriptions-item"><slot /></div>', props: ['label', 'span'] },
}

function mountComponent() {
  return mount(EmissionData, {
    global: {
      plugins: [createPinia()],
      stubs,
    },
  })
}

describe('EmissionData.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
    localStorage.setItem('enterpriseId', 'test-enterprise')
  })

  it('组件正确渲染', () => {
    const wrapper = mountComponent()
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('页面加载时调用API', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(getMyRating).toHaveBeenCalled()
    expect(getIndustryRankings).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('API调用失败显示错误消息', async () => {
    getMyRating.mockRejectedValueOnce(new Error('network error'))
    const wrapper = mountComponent()
    await flushPromises()
    expect(ElMessage.error).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('组件渲染数据', async () => {
    getMyRating.mockResolvedValueOnce([{ ratingLevel: 'A', totalEmission: 100, emissionIntensity: 1.2, ratingScore: 95, ratingYear: '2026' }])
    const wrapper = mountComponent()
    await flushPromises()
    expect(getMyRating).toHaveBeenCalled()
    wrapper.unmount()
  })
})
