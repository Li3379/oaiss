import request from './request'
import type {
  CreditScoreResponse,
  CreditEventResponse,
  CreditDeductionRequest,
  PageRequest,
  PageResponse,
} from '../types'

export function getMyScore(): Promise<CreditScoreResponse> {
  return request.get('/credit/my-score')
}

export function getScoreHistory(params?: PageRequest & { eventType?: number }): Promise<PageResponse<CreditEventResponse>> {
  return request.get('/credit/history', { params })
}

export function getScoreRanking(params?: PageRequest): Promise<PageResponse<CreditScoreResponse>> {
  return request.get('/credit/ranking', { params })
}

export function getEnterpriseScore(enterpriseId: number): Promise<CreditScoreResponse> {
  if (!enterpriseId) return Promise.reject(new Error('Enterprise ID is required'))
  return request.get(`/credit/${enterpriseId}`)
}

export function getEnterpriseCreditHistory(
  enterpriseId: number,
  params?: PageRequest & { eventType?: number },
): Promise<PageResponse<CreditEventResponse>> {
  if (!enterpriseId) return Promise.reject(new Error('Enterprise ID is required'))
  return request.get(`/credit/${enterpriseId}/history`, { params })
}

export function deductPoints(data: CreditDeductionRequest): Promise<CreditScoreResponse> {
  if (!data?.enterpriseId) return Promise.reject(new Error('Enterprise ID is required'))
  if (!data?.eventType) return Promise.reject(new Error('Credit event type is required'))
  return request.post('/credit/deduct', data)
}

export function addBonus(
  data: { enterpriseId: number; points: number; reason?: string; description?: string },
): Promise<CreditScoreResponse> {
  if (!data?.enterpriseId) return Promise.reject(new Error('Enterprise ID is required'))
  if (!data?.points || data.points <= 0) return Promise.reject(new Error('Bonus points must be greater than 0'))

  const description = data.description ?? data.reason
  if (!description) return Promise.reject(new Error('Bonus description is required'))

  return request.post('/credit/bonus', null, {
    params: {
      enterpriseId: data.enterpriseId,
      points: data.points,
      description,
    },
  })
}

export function evaluateLevel(enterpriseId: number): Promise<CreditScoreResponse> {
  if (!enterpriseId) return Promise.reject(new Error('Enterprise ID is required'))
  return request.post(`/credit/evaluate/${enterpriseId}`)
}

export function getRestrictedEnterprises(): Promise<CreditScoreResponse[]> {
  return request.get('/credit/restricted')
}

export function getFrozenEnterprises(): Promise<CreditScoreResponse[]> {
  return request.get('/credit/frozen')
}

export function checkTradePermission(enterpriseId: number): Promise<boolean> {
  if (!enterpriseId) return Promise.reject(new Error('Enterprise ID is required'))
  return request.get(`/credit/check-permission/${enterpriseId}`)
}
