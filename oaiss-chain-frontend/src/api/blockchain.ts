import request from './request'
import type {
  BlockchainBlockResponse,
  BlockchainStatusResponse,
  BlockchainTransactionResponse,
  PageRequest,
  PageResponse,
} from '../types'

export function getStatus(): Promise<BlockchainStatusResponse> {
  return request.get('/blockchain/status')
}

export function getTransactions(params?: PageRequest): Promise<PageResponse<BlockchainTransactionResponse>> {
  return request.get('/blockchain/transactions', { params })
}

export function getLatestBlocks(params?: PageRequest): Promise<PageResponse<BlockchainBlockResponse>> {
  return request.get('/blockchain/blocks/latest', { params })
}

export function queryBlock(blockNumber: number): Promise<BlockchainBlockResponse | Record<string, unknown>> {
  return request.get(`/blockchain/block/${blockNumber}`)
}

export function queryTransaction(txHash: string): Promise<BlockchainTransactionResponse | Record<string, unknown>> {
  if (!txHash) return Promise.reject(new Error('Transaction hash is required'))
  return request.get(`/blockchain/transaction/${txHash}`)
}
