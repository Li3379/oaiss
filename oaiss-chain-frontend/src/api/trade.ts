import request from './request'
import type { TradeRequest, TradeResponse, PageRequest } from '../types'

export function createP2PTrade(data: TradeRequest): Promise<TradeResponse> {
  if (!data?.carbonAmount || data.carbonAmount <= 0) return Promise.reject(new Error('碳配额数量必须大于0'))
  if (!data?.price || data.price <= 0) return Promise.reject(new Error('价格必须大于0'))
  return request.post('/trade/p2p', data)
}

export function cancelTrade(tradeId: number): Promise<void> {
  if (!tradeId) return Promise.reject(new Error('交易ID不能为空'))
  return request.post(`/trade/${tradeId}/cancel`)
}

export function getMyTrades(params?: PageRequest): Promise<unknown> {
  return request.get('/trade/my-trades', { params })
}
