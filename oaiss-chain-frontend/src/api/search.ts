import request from './request'
import type { CarbonReportSummary, TradeSummary, MarketOverview, PageRequest } from '../types'

export function searchReports(params?: PageRequest): Promise<CarbonReportSummary[]> {
  return request.get('/search/reports', { params })
}

export function searchTrades(params?: PageRequest): Promise<TradeSummary[]> {
  return request.get('/search/trades', { params })
}

export function getMarketOverview(): Promise<MarketOverview> {
  return request.get('/search/market-overview')
}
