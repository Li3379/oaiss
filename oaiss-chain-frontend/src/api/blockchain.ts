import request from './request'

export function getStatus(): Promise<unknown> {
  return request.get('/blockchain/status')
}

export function getTransactions(params?: Record<string, unknown>): Promise<unknown> {
  return request.get('/blockchain/transactions', { params })
}

export function getLatestBlocks(params?: Record<string, unknown>): Promise<unknown> {
  return request.get('/blockchain/blocks/latest', { params })
}
