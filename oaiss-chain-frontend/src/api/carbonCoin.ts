import request from './request'
import type {
  CarbonCoinAccountResponse,
  CarbonCoinRechargeRequest,
  CarbonCoinTransactionPage,
  CarbonCoinTransferRequest,
  PageRequest,
} from '../types'

export function getMyAccount(): Promise<CarbonCoinAccountResponse> {
  return request.get('/carbon-coin/account')
}

export function getTransactions(params?: PageRequest): Promise<CarbonCoinTransactionPage> {
  return request.get('/carbon-coin/transactions', { params })
}

export function transferCoins(data: CarbonCoinTransferRequest): Promise<CarbonCoinAccountResponse> {
  return request.post('/carbon-coin/transfer', data)
}

export function recharge(userId: number, amount: number): Promise<CarbonCoinAccountResponse> {
  if (!userId) return Promise.reject(new Error('用户ID不能为空'))
  if (!amount || amount <= 0) return Promise.reject(new Error('充值金额必须大于0'))

  const payload: CarbonCoinRechargeRequest = { amount }
  return request.post(`/carbon-coin/recharge?userId=${userId}`, payload)
}
