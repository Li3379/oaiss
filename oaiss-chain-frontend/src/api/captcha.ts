import request from './request'
import type { CaptchaResponse } from '../types'

export function generateCaptcha(): Promise<CaptchaResponse> {
  return request.get('/captcha/generate')
}
