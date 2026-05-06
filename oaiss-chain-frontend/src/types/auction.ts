/** 拍卖订单请求 */
export interface AuctionOrderRequest {
  direction: number
  quantity: number
  price: number
}

/** 拍卖订单响应 */
export interface AuctionOrderResponse {
  id: number
  orderNo: string
  userId: number
  direction: number
  directionText: string
  quantity: number
  price: number
  matchedQuantity: number
  remainingQuantity: number
  status: number
  statusText: string
  settlementPrice: number
  matchedAt: string
  createdAt: string
}

/** 撮合结果响应 */
export interface MatchingResultResponse {
  id: number
  matchNo: string
  buyOrderId: number
  sellOrderId: number
  buyerId: number
  sellerId: number
  buyerName: string
  sellerName: string
  matchedQuantity: number
  settlementPrice: number
  totalAmount: number
  status: number
  statusText: string
  transactionId: number
  settledAt: string
  createdAt: string
}
