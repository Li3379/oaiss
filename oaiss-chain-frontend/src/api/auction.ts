import request from './request'
import type {
  AuctionOrderPage,
  AuctionOrderRequest,
  AuctionOrderResponse,
  MatchingResultPage,
  MatchingResultResponse,
  PageRequest,
} from '../types'

export function submitBuyOrder(data: AuctionOrderRequest): Promise<AuctionOrderResponse> {
  return request.post('/auction/buy', data)
}

export function submitSellOrder(data: AuctionOrderRequest): Promise<AuctionOrderResponse> {
  return request.post('/auction/sell', data)
}

export function getAuctionOrders(params?: PageRequest): Promise<AuctionOrderPage> {
  return request.get('/auction/orders', { params })
}

export function getMyOrders(params?: PageRequest): Promise<AuctionOrderPage> {
  return request.get('/auction/my-orders', { params })
}

export function getMatchResults(params?: PageRequest): Promise<MatchingResultPage> {
  return request.get('/auction/results', { params })
}

export function executeMatching(): Promise<MatchingResultResponse[]> {
  return request.post('/auction/match')
}
