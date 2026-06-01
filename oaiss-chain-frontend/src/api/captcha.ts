import request from './request'
import type { CaptchaResponse, CaptchaSendRequest } from '../types'

export function generateCaptcha(): Promise<CaptchaResponse> {
  return request.get('/captcha/generate')
}

export function verifyCaptcha(data: { captchaKey: string; captchaCode: string }): Promise<boolean> {
  if (!data?.captchaKey) return Promise.reject(new Error('Captcha key is required'))
  if (!data?.captchaCode) return Promise.reject(new Error('Captcha code is required'))
  return request.post('/captcha/verify', data)
}

export function sendSmsCode(data: { phone: string; type?: number }): Promise<void> {
  if (!data?.phone) return Promise.reject(new Error('Phone number is required'))
  const requestData: CaptchaSendRequest = {
    target: data.phone,
    type: data.type ?? 1,
  }
  return request.post('/captcha/sms/send', requestData)
}

export function sendEmailCode(data: { email: string; type?: number }): Promise<void> {
  if (!data?.email) return Promise.reject(new Error('Email is required'))
  const requestData: CaptchaSendRequest = {
    target: data.email,
    type: data.type ?? 1,
  }
  return request.post('/captcha/email/send', requestData)
}
