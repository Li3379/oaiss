import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { ref } from 'vue'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
    locale: ref('zh-CN'),
  }),
}))

vi.mock('../../api/carbon', () => ({
  reviewReport: vi.fn(() => Promise.resolve()),
}))

vi.mock('../../api/reviewer', () => ({
  getPendingReports: vi.fn(() => Promise.resolve({ items: [], total: 0 })),
  getReviewHistory: vi.fn(() => Promise.resolve({ items: [], total: 0 })),
  getMyReviewerQualification: vi.fn(() => Promise.resolve([])),
  getReviewerInfo: vi.fn(() => Promise.resolve(null)),
  getStatistics: vi.fn(() => Promise.resolve(null)),
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
    ElMessageBox: { confirm: vi.fn(() => Promise.resolve()) },
  }
})

import AuditList from '../auditor/AuditList.vue'
import { getPendingReports, getReviewHistory, getMyReviewerQualification, getReviewerInfo, getStatistics } from '../../api/reviewer'
import { ElMessage } from 'element-plus'

const stubs = {
  'el-card': { template: '<div class="el-card"><slot /></div>' },
  'el-space': { template: '<div class="el-space"><slot /></div>', props: ['size'] },
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
  'el-radio-group': {
    template: '<div class="el-radio-group"><slot /></div>',
    props: ['modelValue'],
    emits: ['update:modelValue'],
  },
  'el-radio': {
    template: '<label class="el-radio"><slot /></label>',
    props: ['label', 'value'],
  },
  'el-tabs': {
    template: '<div class="el-tabs"><slot /></div>',
    props: ['modelValue'],
    emits: ['update:modelValue', 'tab-change'],
  },
  'el-tab-pane': {
    template: '<div class="el-tab-pane"><slot /></div>',
    props: ['label', 'name'],
  },
}

function mountComponent() {
  return mount(AuditList, {
    global: {
      plugins: [createPinia()],
      stubs,
      directives: {
        loading: {},
      },
    },
  })
}

describe('AuditList.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('renders successfully', () => {
    const wrapper = mountComponent()
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('loads pending reports and reviewer summary data on mount', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    expect(getPendingReports).toHaveBeenCalledWith({ pageNum: 1, pageSize: 10 })
    expect(getMyReviewerQualification).toHaveBeenCalled()
    expect(getReviewerInfo).toHaveBeenCalled()
    expect(getStatistics).toHaveBeenCalled()

    wrapper.unmount()
  })

  it('shows an error message when pending report loading fails', async () => {
    vi.mocked(getPendingReports).mockRejectedValueOnce(new Error('network error'))

    const wrapper = mountComponent()
    await flushPromises()

    expect(ElMessage.error).toHaveBeenCalledWith('auditList.loadFailed')
    wrapper.unmount()
  })

  it('loads review history when the tab switches to all reports', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    ;(wrapper.vm as any).activeTab = 'all'
    await (wrapper.vm as any).onTabChange('all')
    await flushPromises()

    expect(getReviewHistory).toHaveBeenCalledWith({ pageNum: 1, pageSize: 10 })
    wrapper.unmount()
  })
})
