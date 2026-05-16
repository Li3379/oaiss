import request from './request'
import type { CaptchaResponse } from '../types'

export function generateCaptcha(): Promise<CaptchaResponse> {
  return request.get('/captcha/generate')
}

export function verifyCaptcha(data: { captchaKey: string; captchaCode: string }): Promise<{ valid: boolean }> {
  if (!data?.captchaKey) return Promise.reject(new Error('验证码key不能为空'))
  if (!data?.captchaCode) return Promise.reject(new Error('验证码不能为空'))
  return request.post('/captcha/verify', data)
}

export function sendSmsCode(data: { phone: string }): Promise<void> {
  if (!data?.phone) return Promise.reject(new Error('手机号不能为空'))
  return request.post('/captcha/sms/send', data)
}

export function sendEmailCode(data: { email: string }): Promise<void> {
  if (!data?.email) return Promise.reject(new Error('邮箱不能为空'))
  return request.post('/captcha/email/send', data)
}
