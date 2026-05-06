/** 碳币账户响应 */
export interface CarbonCoinAccountResponse {
  id: number
  userId: number
  balance: number
  totalRecharged: number
  totalSpent: number
  status: number
}

/** 碳币充值请求 */
export interface CarbonCoinRechargeRequest {
  amount: number
  paymentMethod?: number
  remark?: string
}

/** 碳币转账请求 */
export interface CarbonCoinTransferRequest {
  counterpartId: number
  amount: number
  remark?: string
}

/** 碳币交易记录 */
export interface CarbonCoinTransaction {
  id: number
  txNo: string
  userId: number
  txType: number
  amount: number
  balanceBefore: number
  balanceAfter: number
  relatedQuota: number
  relatedTradeId: number
  counterpartId: number
  remark: string
  createdAt: string
  updatedAt: string
}
