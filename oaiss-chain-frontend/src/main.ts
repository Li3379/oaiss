import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import router from './router'
import { pinia } from './store'
import i18n from './i18n'
import './style.css'
import App from './App.vue'

createApp(App).use(pinia).use(router).use(i18n).use(ElementPlus, { locale: zhCn }).mount('#app')
