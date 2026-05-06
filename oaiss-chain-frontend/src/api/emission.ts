import request from './request'
import type { EmissionRating, CarbonPredictionRequest, CarbonPredictionResponse, PageRequest } from '../types'

export function getEnterpriseRatings(enterpriseId: number): Promise<EmissionRating[]> {
  return request.get(`/emission/ratings/${enterpriseId}`)
}

export function getIndustryRankings(year: number, params?: PageRequest): Promise<unknown> {
  return request.get(`/emission/rankings/${year}`, { params })
}

export function predictEmission(data: CarbonPredictionRequest): Promise<CarbonPredictionResponse> {
  return request.post('/emission/predict', data)
}
