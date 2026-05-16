import request from './request'
import type { EmissionRating, EmissionRatingRequest, CarbonPredictionRequest, CarbonPredictionResponse, PageRequest } from '../types'

export function getEnterpriseRatings(enterpriseId: number): Promise<EmissionRating[]> {
  return request.get(`/emission/ratings/${enterpriseId}`)
}

export function getIndustryRankings(year: number, params?: PageRequest): Promise<unknown> {
  return request.get(`/emission/rankings/${year}`, { params })
}

export function predictEmission(data: CarbonPredictionRequest): Promise<CarbonPredictionResponse> {
  return request.post('/emission/predict', data)
}

export function createRating(data: EmissionRatingRequest): Promise<EmissionRating> {
  if (!data?.enterpriseId) return Promise.reject(new Error('企业ID不能为空'))
  return request.post('/emission/ratings', data)
}
