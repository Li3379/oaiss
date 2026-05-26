import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('../../api/carbon', () => ({
  getReportList: vi.fn(() => Promise.resolve({ items: [], total: 0 })),
  reviewReport: vi.fn(() => Promise.resolve()),
  certifyReport: vi.fn(() => Promise.resolve()),
}))

vi.mock('../../api/blockchain', () => ({
  getStatus: vi.fn(() => Promise.resolve({ status: 'Normal' })),
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() },
    ElMessageBox: { confirm: vi.fn(() => Promise.resolve()) },
  }
})

vi.mock('../../components/PageContainer.vue', () => ({
  default: {
    template: '<div class="page-container"><slot /></div>',
    props: ['title', 'description'],
  },
}))

import VerifyList from '../admin/VerifyList.vue'
import { getReportList } from '../../api/carbon'
import { getStatus } from '../../api/blockchain'
import { ElMessage } from 'element-plus'

const stubs = {
  'el-card': { template: '<div class="el-card"><slot /></div>' },
  'el-statistic': { template: '<div class="el-statistic"></div>', props: ['title', 'value'] },
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
  'el-tag': { template: '<span class="el-tag"><slot /></span>', props: ['type', 'size'] },
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
  'el-descriptions': { template: '<div class="el-descriptions"><slot /></div>', props: ['column', 'border'] },
  'el-descriptions-item': { template: '<div class="el-descriptions-item"><slot /></div>', props: ['label', 'span'] },
}

function mountVerifyList() {
  return mount(VerifyList, {
    global: {
      stubs,
      directives: {
        loading: {},
      },
    },
    attachTo: document.body,
  })
}

async function flush() {
  await nextTick()
  await nextTick()
}

describe('VerifyList.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders page container and section cards', async () => {
    const wrapper = mountVerifyList()
    await flush()
    expect(wrapper.find('.page-container').exists()).toBe(true)
    expect(wrapper.find('.section-card').exists()).toBe(true)
    wrapper.unmount()
  })

  it('loads report list and blockchain status on mount', async () => {
    const wrapper = mountVerifyList()
    await flush()
    expect(getReportList).toHaveBeenCalled()
    expect(getStatus).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('shows translated load error when report query fails', async () => {
    vi.mocked(getReportList).mockRejectedValueOnce(new Error('network'))
    const wrapper = mountVerifyList()
    await flush()
    expect(ElMessage.error).toHaveBeenCalledWith('verifyList.loadFailed')
    wrapper.unmount()
  })

  it('renders stats row after receiving data', async () => {
    vi.mocked(getReportList).mockResolvedValueOnce({
      items: [{ id: 1, reportNo: 'RPT-001', enterpriseName: 'Demo', status: 3, totalEmission: 100 }],
      total: 1,
    } as any)

    const wrapper = mountVerifyList()
    await flush()
    expect(wrapper.find('.stats-row').exists()).toBe(true)
    wrapper.unmount()
  })
})
