import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'

vi.mock('../../api/auth', () => ({
  login: vi.fn(),
}))

vi.mock('../../api/captcha', () => ({
  generateCaptcha: vi.fn(),
}))

const mockRouterReplace = vi.fn()
const mockRouteQuery = {}

vi.mock('vue-router', () => ({
  useRoute: vi.fn(() => ({ query: mockRouteQuery })),
  useRouter: vi.fn(() => ({ replace: mockRouterReplace, query: mockRouteQuery })),
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
  }
})

import Login from '../Login.vue'
import { login } from '../../api/auth'
import { generateCaptcha } from '../../api/captcha'
import { ElMessage } from 'element-plus'

const stubGlobal = {
  stubs: {
    'el-card': { template: '<div class="el-card"><slot /></div>' },
    'el-form': {
      template: '<form @submit.prevent><slot /></form>',
      methods: {
        validate() { return Promise.resolve(true) },
      },
    },
    'el-form-item': { template: '<div class="el-form-item"><slot /></div>', props: ['label', 'prop'] },
    'el-input': {
      template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
      props: ['modelValue', 'type', 'placeholder', 'showPassword', 'clearable'],
      emits: ['update:modelValue'],
    },
    'el-checkbox': {
      template: '<label><input type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)" /><slot /></label>',
      props: ['modelValue'],
      emits: ['update:modelValue'],
    },
    'el-button': {
      template: '<button :disabled="loading" @click="$emit(\'click\')"><slot /></button>',
      props: ['type', 'size', 'loading'],
      emits: ['click'],
    },
  },
}

function mountLogin() {
  return mount(Login, { global: stubGlobal })
}

async function flush() {
  await nextTick()
  await nextTick()
  await nextTick()
}

describe('Login.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
    localStorage.clear()

    // Reset route query
    Object.keys(mockRouteQuery).forEach(k => delete mockRouteQuery[k])

    generateCaptcha.mockResolvedValue({
      captchaKey: 'test-key-123',
      captchaImage: 'data:image/png;base64,abc123',
    })
  })

  describe('captcha loading', () => {
    it('calls generateCaptcha on mount', async () => {
      mountLogin()
      await flush()
      expect(generateCaptcha).toHaveBeenCalledTimes(1)
    })

    it('renders captcha image when loaded', async () => {
      const wrapper = mountLogin()
      await flush()
      const img = wrapper.find('.captcha-image')
      expect(img.exists()).toBe(true)
    })

    it('handles captcha load failure gracefully', async () => {
      generateCaptcha.mockRejectedValue(new Error('fail'))
      const wrapper = mountLogin()
      await flush()
      // Component should not crash; no captcha image rendered
      const img = wrapper.find('img.captcha-image')
      expect(img.exists()).toBe(false)
      // The div placeholder with "点击加载" text should be present
      expect(wrapper.html()).toContain('点击加载')
    })
  })

  describe('form rendering', () => {
    it('renders account, password, and captcha inputs', () => {
      const wrapper = mountLogin()
      const inputs = wrapper.findAll('input')
      // account, password, captcha, rememberPassword checkbox
      expect(inputs.length).toBeGreaterThanOrEqual(3)
    })

    it('renders remember password checkbox', () => {
      const wrapper = mountLogin()
      expect(wrapper.find('label').exists()).toBe(true)
    })

    it('renders submit button', () => {
      const wrapper = mountLogin()
      const btn = wrapper.find('button')
      expect(btn.exists()).toBe(true)
    })
  })

  describe('login success', () => {
    it('calls login API with form data', async () => {
      login.mockResolvedValue({
        accessToken: 'access-token-123',
        refreshToken: 'refresh-token-456',
      })

      const wrapper = mountLogin()
      await flush()

      const inputs = wrapper.findAll('input')
      await inputs[0].setValue('testuser')
      await inputs[1].setValue('password123')
      await inputs[2].setValue('ABC1')

      const btn = wrapper.find('button')
      await btn.trigger('click')
      await flush()

      expect(login).toHaveBeenCalledWith({
        username: 'testuser',
        password: 'password123',
        captchaKey: 'test-key-123',
        captcha: 'ABC1',
      })
    })

    it('shows success message on login', async () => {
      login.mockResolvedValue({
        accessToken: 'token',
        refreshToken: 'refresh',
      })

      const wrapper = mountLogin()
      await flush()

      const inputs = wrapper.findAll('input')
      await inputs[0].setValue('user')
      await inputs[1].setValue('pass')
      await inputs[2].setValue('code')

      const btn = wrapper.find('button')
      await btn.trigger('click')
      await flush()

      expect(ElMessage.success).toHaveBeenCalledWith('登录成功')
    })

    it('navigates to redirect path on success', async () => {
      mockRouteQuery.redirect = '/dashboard'

      login.mockResolvedValue({
        accessToken: 'token',
        refreshToken: 'refresh',
      })

      const wrapper = mountLogin()
      await flush()

      const inputs = wrapper.findAll('input')
      await inputs[0].setValue('user')
      await inputs[1].setValue('pass')
      await inputs[2].setValue('code')

      const btn = wrapper.find('button')
      await btn.trigger('click')
      await flush()

      expect(mockRouterReplace).toHaveBeenCalledWith('/dashboard')
    })

    it('navigates to homePath when no redirect', async () => {
      login.mockResolvedValue({
        accessToken: 'token',
        refreshToken: 'refresh',
      })

      const wrapper = mountLogin()
      await flush()

      const inputs = wrapper.findAll('input')
      await inputs[0].setValue('user')
      await inputs[1].setValue('pass')
      await inputs[2].setValue('code')

      const btn = wrapper.find('button')
      await btn.trigger('click')
      await flush()

      // Default homePath is '/enterprise/carbon/upload'
      expect(mockRouterReplace).toHaveBeenCalledWith('/enterprise/carbon/upload')
    })
  })

  describe('login failure', () => {
    it('refreshes captcha on login failure', async () => {
      login.mockRejectedValue(new Error('Invalid credentials'))
      generateCaptcha.mockResolvedValue({
        captchaKey: 'new-key',
        captchaImage: 'data:image/png;base64,xyz',
      })

      const wrapper = mountLogin()
      await flush()
      vi.clearAllMocks()

      const inputs = wrapper.findAll('input')
      await inputs[0].setValue('user')
      await inputs[1].setValue('wrong')
      await inputs[2].setValue('code')

      const btn = wrapper.find('button')
      await btn.trigger('click')
      await flush()

      expect(generateCaptcha).toHaveBeenCalled()
    })

    it('clears captcha input on failure', async () => {
      login.mockRejectedValue(new Error('fail'))

      const wrapper = mountLogin()
      await flush()

      const inputs = wrapper.findAll('input')
      await inputs[0].setValue('user')
      await inputs[1].setValue('pass')
      await inputs[2].setValue('code')

      const btn = wrapper.find('button')
      await btn.trigger('click')
      await flush()

      // captchaInput should be cleared
      expect(inputs[2].element.value).toBe('')
    })
  })

  describe('remember password', () => {
    it('saves form to localStorage when rememberPassword is true', async () => {
      login.mockResolvedValue({
        accessToken: 'token',
        refreshToken: 'refresh',
      })

      const wrapper = mountLogin()
      await flush()

      const inputs = wrapper.findAll('input')
      await inputs[0].setValue('myuser')
      await inputs[1].setValue('mypass')
      await inputs[2].setValue('code')

      const btn = wrapper.find('button')
      await btn.trigger('click')
      await flush()

      const saved = JSON.parse(localStorage.getItem('carbon-admin-login-form'))
      expect(saved).toEqual({ account: 'myuser', rememberPassword: true })
    })

    it('restores account from localStorage on mount', async () => {
      localStorage.setItem('carbon-admin-login-form', JSON.stringify({
        account: 'saved-user',
        rememberPassword: true,
      }))

      const wrapper = mountLogin()
      await flush()

      const inputs = wrapper.findAll('input')
      expect(inputs[0].element.value).toBe('saved-user')
    })
  })
})
