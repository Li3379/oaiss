import request from './request'
import type { CreditScoreResponse, CreditEventResponse, CreditDeductionRequest, PageRequest } from '../types'

export function getMyScore(): Promise<CreditScoreResponse> {
  return request.get('/credit/my-score')
}

export function getScoreHistory(params?: PageRequest): Promise<CreditEventResponse[]> {
  return request.get('/credit/history', { params })
}

export function getScoreRanking(params?: PageRequest): Promise<unknown> {
  return request.get('/credit/ranking', { params })
}

export function getEnterpriseScore(enterpriseId: number): Promise<CreditScoreResponse> {
  if (!enterpriseId) return Promise.reject(new Error('企业ID不能为空'))
  return request.get(`/credit/${enterpriseId}`)
}

export function getEnterpriseCreditHistory(enterpriseId: number, params?: PageRequest): Promise<CreditEventResponse[]> {
  if (!enterpriseId) return Promise.reject(new Error('企业ID不能为空'))
  return request.get(`/credit/${enterpriseId}/history`, { params })
}

export function deductPoints(data: CreditDeductionRequest): Promise<void> {
  if (!data?.enterpriseId) return Promise.reject(new Error('企业ID不能为空'))
  if (!data?.eventType) return Promise.reject(new Error('事件类型不能为空'))
  return request.post('/credit/deduct', data)
}

export function addBonus(data: { enterpriseId: number; points: number; reason: string }): Promise<void> {
  if (!data?.enterpriseId) return Promise.reject(new Error('企业ID不能为空'))
  if (!data?.points || data.points <= 0) return Promise.reject(new Error('奖励分数必须大于0'))
  return request.post('/credit/bonus', data)
}

export function evaluateLevel(enterpriseId: number): Promise<CreditScoreResponse> {
  if (!enterpriseId) return Promise.reject(new Error('企业ID不能为空'))
  return request.post(`/credit/evaluate/${enterpriseId}`)
}

export function getRestrictedEnterprises(params?: PageRequest): Promise<unknown> {
  return request.get('/credit/restricted', { params })
}

export function getFrozenEnterprises(params?: PageRequest): Promise<unknown> {
  return request.get('/credit/frozen', { params })
}

export function checkTradePermission(enterpriseId: number): Promise<{ allowed: boolean }> {
  if (!enterpriseId) return Promise.reject(new Error('企业ID不能为空'))
  return request.get(`/credit/check-permission/${enterpriseId}`)
}
