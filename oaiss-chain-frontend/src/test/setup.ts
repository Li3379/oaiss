import { config } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import i18n from '../i18n'

class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}

if (typeof globalThis.ResizeObserver === 'undefined') {
  globalThis.ResizeObserver = ResizeObserverStub as typeof ResizeObserver
}

if (typeof window !== 'undefined' && typeof window.matchMedia === 'undefined') {
  window.matchMedia = ((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener() {},
    removeListener() {},
    addEventListener() {},
    removeEventListener() {},
    dispatchEvent() {
      return false
    },
  })) as typeof window.matchMedia
}

config.global.plugins = [...(config.global.plugins || []), i18n, ElementPlus]
config.global.stubs = {
  ...(config.global.stubs || {}),
  transition: false,
  teleport: true,
  'router-link': {
    props: ['to'],
    template: '<a :href="typeof to === \'string\' ? to : to?.path || \'#\'"><slot /></a>',
  },
  'router-view': true,
}
