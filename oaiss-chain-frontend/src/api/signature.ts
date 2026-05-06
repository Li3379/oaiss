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

export function encryptData(data: { data: string; publicKey: string }): Promise<{ encrypted: string }> {
  return request.post('/signature/encrypt', data)
}

export function decryptData(data: { data: string; privateKey: string }): Promise<{ decrypted: string }> {
  return request.post('/signature/decrypt', data)
}
