/** 交易请求 */
export interface TradeRequest {
  tradeType: number
  buyerId?: number
  sellerId?: number
  quantity: number
  unitPrice: number
  reportId?: number
  remark?: string
}

/** 交易响应 */
export interface TradeResponse {
  id: number
  tradeNo: string
  tradeType: number
  tradeTypeText: string
  sellerId: number
  sellerName: string
  buyerId: number
  buyerName: string
  quantity: number
  unitPrice: number
  totalAmount: number
  reportId: number
  status: number
  statusText: string
  remark: string
  blockchainTxHash: string
  completedAt: string
  createdAt: string
}
