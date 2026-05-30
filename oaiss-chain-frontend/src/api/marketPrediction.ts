import request from './request'
import type { MarketForecastResponse } from '@/types/ai'

export function getMarketTrend(horizonDays: number = 30): Promise<MarketForecastResponse> {
  return request.post('/ai/market/trend', null, { params: { horizonDays }, suppressErrorMessage: true })
}

export function getMarketPrice(horizonDays: number = 30): Promise<MarketForecastResponse> {
  return request.post('/ai/market/price', null, { params: { horizonDays }, suppressErrorMessage: true })
}

export function getSupplyDemand(horizonDays: number = 30): Promise<MarketForecastResponse> {
  return request.post('/ai/market/supply-demand', null, { params: { horizonDays }, suppressErrorMessage: true })
}
