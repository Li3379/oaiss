import request from './request'
import type { CarbonCoinAccountResponse, CarbonCoinTransaction, CarbonCoinTransferRequest, PageRequest } from '../types'

export function getMyAccount(): Promise<CarbonCoinAccountResponse> {
  return request.get('/carbon-coin/account')
}

export function getTransactions(params?: PageRequest): Promise<CarbonCoinTransaction[]> {
  return request.get('/carbon-coin/transactions', { params })
}

export function transferCoins(data: CarbonCoinTransferRequest): Promise<CarbonCoinAccountResponse> {
  return request.post('/carbon-coin/transfer', data)
}
