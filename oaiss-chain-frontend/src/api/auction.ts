import request from './request'
import type { AuctionOrderRequest, AuctionOrderResponse, MatchingResultResponse, PageRequest } from '../types'

export function submitBuyOrder(data: AuctionOrderRequest): Promise<AuctionOrderResponse> {
  return request.post('/auction/buy', data)
}

export function submitSellOrder(data: AuctionOrderRequest): Promise<AuctionOrderResponse> {
  return request.post('/auction/sell', data)
}

export function getAuctionOrders(params?: PageRequest): Promise<unknown> {
  return request.get('/auction/orders', { params })
}

export function getMyOrders(params?: PageRequest): Promise<unknown> {
  return request.get('/auction/my-orders', { params })
}

export function getMatchResults(params?: PageRequest): Promise<MatchingResultResponse[]> {
  return request.get('/auction/results', { params })
}
