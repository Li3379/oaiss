import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ErrorBoundary from '../ErrorBoundary.vue'

describe('ErrorBoundary', () => {
  it('renders slot content when no error', () => {
    const wrapper = mount(ErrorBoundary, {
      slots: { default: '<div class="child">Hello</div>' },
    })
    expect(wrapper.find('.child').exists()).toBe(true)
    expect(wrapper.find('.error-boundary').exists()).toBe(false)
  })

  it('shows error UI when error is captured', async () => {
    const wrapper = mount(ErrorBoundary, {
      global: { stubs: { 'el-button': true } },
    })
    wrapper.vm.error = new Error('test error')
    wrapper.vm.info = 'at component'
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.error-boundary').exists()).toBe(true)
    expect(wrapper.find('.error-message').text()).toContain('test error')
  })

  it('clears error on retry button click', async () => {
    const wrapper = mount(ErrorBoundary, {
      global: { stubs: { 'el-button': { template: '<button @click="$emit(\'click\')">重试</button>' } } },
    })
    wrapper.vm.error = new Error('test')
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.error-boundary').exists()).toBe(true)

    await wrapper.find('button').trigger('click')
    expect(wrapper.vm.error).toBeNull()
  })
})
