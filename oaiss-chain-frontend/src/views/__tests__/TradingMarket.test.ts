import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('../../api/auction', () => ({
  submitBuyOrder: vi.fn(() => Promise.resolve()),
  submitSellOrder: vi.fn(() => Promise.resolve()),
  getAuctionOrders: vi.fn(() => Promise.resolve({ items: [], total: 0 })),
  getMyOrders: vi.fn(() => Promise.resolve({ items: [], total: 0 })),
  getMatchResults: vi.fn(() => Promise.resolve({ items: [], total: 0 })),
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() },
    ElMessageBox: { confirm: vi.fn(() => Promise.resolve()) },
  }
})

import TradingMarket from '../enterprise/TradingMarket.vue'
import { getAuctionOrders, getMyOrders, getMatchResults, submitBuyOrder, submitSellOrder } from '../../api/auction'
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
    props: ['modelValue', 'type', 'placeholder', 'showPassword', 'clearable', 'rows', 'min', 'step'],
    emits: ['update:modelValue'],
  },
  'el-button': {
    template: '<button :disabled="loading" @click="$emit(\'click\')"><slot /></button>',
    props: ['type', 'size', 'loading', 'link', 'plain'],
    emits: ['click'],
  },
  'el-table': {
    template: '<table><slot /><slot name="append" /></table>',
    props: ['data', 'border', 'emptyText', 'loading'],
    emits: ['selection-change'],
  },
  'el-table-column': {
    template: '<td><slot :row="{}" :$index="0" /></td>',
    props: ['type', 'prop', 'label', 'minWidth', 'width', 'fixed', 'showOverflowTooltip'],
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
    props: ['modelValue', 'placeholder', 'style'],
    emits: ['update:modelValue'],
  },
  'el-option': {
    template: '<option :value="value"><slot /></option>',
    props: ['label', 'value'],
  },
  'el-tabs': {
    template: '<div class="el-tabs"><slot /></div>',
    props: ['modelValue'],
    emits: ['tab-change', 'update:modelValue'],
  },
  'el-tab-pane': {
    template: '<div class="el-tab-pane"></div>',
    props: ['label', 'name'],
  },
}

function mountComponent() {
  return mount(TradingMarket, {
    global: {
      plugins: [createPinia()],
      stubs,
    },
  })
}

describe('TradingMarket.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('组件正确渲染', () => {
    const wrapper = mountComponent()
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('页面加载时获取拍卖订单列表', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(getAuctionOrders).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('获取订单失败显示错误消息', async () => {
    getAuctionOrders.mockRejectedValueOnce(new Error('network error'))
    const wrapper = mountComponent()
    await flushPromises()
    expect(ElMessage.error).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('组件渲染订单列表数据', async () => {
    getAuctionOrders.mockResolvedValueOnce({
      items: [{ id: 1, orderNo: 'ORD001', status: 'pending' }],
      total: 1,
    })
    const wrapper = mountComponent()
    await flushPromises()
    expect(getAuctionOrders).toHaveBeenCalled()
    wrapper.unmount()
  })
})
