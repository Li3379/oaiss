import request from './request'
import type { RsaKeyPairResponse, SignatureResult, SignatureVerifyRequest } from '../types'

export function generateKeyPair(): Promise<RsaKeyPairResponse> {
  return request.post('/signature/keypair/generate')
}

export function getKeyPair(): Promise<RsaKeyPairResponse> {
  return request.get('/signature/keypair')
}

export function deleteKeyPair(): Promise<void> {
  return request.delete('/signature/keypair')
}

export function signData(data: { data: string }): Promise<SignatureResult> {
  return request.post('/signature/sign', data)
}

export function verifySignature(data: SignatureVerifyRequest): Promise<{ valid: boolean }> {
  return request.post('/signature/verify', data)
}

export function encryptData(data: string, reviewerId: number): Promise<string> {
  if (!data) return Promise.reject(new Error('待加密数据不能为空'))
  if (!reviewerId) return Promise.reject(new Error('审核员ID不能为空'))
  return request.post('/signature/encrypt', data, {
    headers: { 'Content-Type': 'text/plain' },
    params: { reviewerId }
  })
}

export function decryptData(encryptedData: string): Promise<string> {
  if (!encryptedData) return Promise.reject(new Error('加密数据不能为空'))
  return request.post('/signature/decrypt', encryptedData, {
    headers: { 'Content-Type': 'text/plain' }
  })
}
