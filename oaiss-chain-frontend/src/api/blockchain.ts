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

export function queryBlock(blockNumber: number): Promise<unknown> {
  return request.get(`/blockchain/block/${blockNumber}`)
}

export function queryTransaction(txHash: string): Promise<unknown> {
  if (!txHash) return Promise.reject(new Error('交易哈希不能为空'))
  return request.get(`/blockchain/transaction/${txHash}`)
}
