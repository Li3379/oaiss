import request from './request'
import type { CreditScoreResponse, CreditEventResponse, PageRequest } from '../types'

export function getMyScore(): Promise<CreditScoreResponse> {
  return request.get('/credit/my-score')
}

export function getScoreHistory(params?: PageRequest): Promise<CreditEventResponse[]> {
  return request.get('/credit/history', { params })
}
