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

vi.mock('../../api/admin', () => ({
  getUserList: vi.fn(() => Promise.resolve({ items: [], total: 0 })),
  updateUserStatus: vi.fn(() => Promise.resolve()),
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
    ElMessageBox: { confirm: vi.fn(() => Promise.resolve()) },
  }
})

import SystemUsers from '../admin/SystemUsers.vue'
import { getUserList } from '../../api/admin'
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
  'el-select': {
    template: '<select :value="modelValue" :data-placeholder="placeholder" @change="$emit(\'update:modelValue\', $event.target.value)"><slot /></select>',
    props: ['modelValue', 'placeholder', 'style', 'clearable'],
    emits: ['update:modelValue'],
  },
  'el-option': {
    template: '<option :value="value"><slot /></option>',
    props: ['label', 'value'],
  },
}

function mountComponent() {
  return mount(SystemUsers, {
    global: {
      plugins: [createPinia()],
      stubs,
      directives: {
        loading: {},
      },
    },
  })
}

describe('SystemUsers.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('renders successfully', () => {
    const wrapper = mountComponent()
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('loads user list on mount', async () => {
    const wrapper = mountComponent()
    await flushPromises()
    expect(getUserList).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('shows translated error when user list loading fails', async () => {
    vi.mocked(getUserList).mockRejectedValueOnce(new Error('network error'))
    const wrapper = mountComponent()
    await flushPromises()
    expect(ElMessage.error).toHaveBeenCalledWith('systemUsers.loadFailed')
    wrapper.unmount()
  })

  it('uses neutral all-status placeholder instead of enabled', async () => {
    const wrapper = mountComponent()
    await flushPromises()

    const selects = wrapper.findAll('select')
    expect(selects[1]?.attributes('data-placeholder')).toBe('systemUsers.typeAll')

    wrapper.unmount()
  })
})
