import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (key: string) => key }),
}))

vi.mock('../../api/carbon', () => ({
  getMyReports: vi.fn(() => Promise.resolve({ items: [], total: 0 })),
}))

vi.mock('../../api/trade', () => ({
  getMyTrades: vi.fn(() => Promise.resolve({ items: [], total: 0 })),
}))

vi.mock('../../api/credit', () => ({
  getMyScore: vi.fn(() => Promise.resolve({ data: { score: 0 } })),
}))

vi.mock('../../api/enterprise', () => ({
  getMyEnterpriseAdmission: vi.fn(() => Promise.resolve([])),
}))

vi.mock('../../utils/echarts', () => ({
  default: {
    init: vi.fn(() => ({
      setOption: vi.fn(),
      resize: vi.fn(),
      dispose: vi.fn(),
    })),
    graphic: {
      LinearGradient: vi.fn(function LinearGradient() {
        return {}
      }),
    },
  },
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
    ElMessageBox: { confirm: vi.fn(() => Promise.resolve()) },
  }
})

import CompanyDashboard from '../enterprise/CompanyDashboard.vue'
import { getMyReports } from '../../api/carbon'
import { getMyTrades } from '../../api/trade'
import { getMyScore } from '../../api/credit'
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
  'el-space': { template: '<div class="el-space"><slot /></div>' },
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
  'el-option': {
    template: '<option :value="value"><slot /></option>',
    props: ['label', 'value'],
  },
  'el-radio-group': {
    template: '<div class="el-radio-group"><slot /></div>',
    props: ['modelValue'],
    emits: ['update:modelValue'],
  },
  'el-radio-button': {
    template: '<button class="el-radio-button"><slot /></button>',
    props: ['label'],
  },
  'el-skeleton': { template: '<div class="el-skeleton"></div>', props: ['rows', 'animated'] },
}

function mountComponent() {
  return mount(CompanyDashboard, {
    global: {
      plugins: [createPinia()],
      stubs,
    },
  })
}

describe('CompanyDashboard.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('renders the dashboard shell', () => {
    const wrapper = mountComponent()
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('calls dashboard APIs on mount', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(getMyReports).toHaveBeenCalled()
    expect(getMyTrades).toHaveBeenCalled()
    expect(getMyScore).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('shows an error message when report loading fails', async () => {
    getMyReports.mockRejectedValueOnce(new Error('network error'))
    const wrapper = mountComponent()
    await flushPromises()
    expect(ElMessage.error).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('shows chart containers after loading finishes', async () => {
    getMyReports.mockResolvedValueOnce({
      items: [{
        assetNo: 'AST-1001',
        category: 'Power',
        region: 'CN',
        scope1Emission: 10,
        scope2Emission: 20,
        scope3Emission: 5,
        totalEmission: 35,
      }],
      total: 1,
    })
    getMyTrades.mockResolvedValueOnce({
      items: [{
        quantity: 8,
        totalAmount: 100,
        createdAt: '2026-05-22T00:00:00Z',
      }],
      total: 1,
    })
    getMyScore.mockResolvedValueOnce({
      carbonCoins: 12,
      carbonQuota: 50,
      score: 88,
    })

    const wrapper = mountComponent()
    await flushPromises()

    expect(wrapper.findAll('.chart-box')).toHaveLength(6)
    wrapper.unmount()
  })
})
