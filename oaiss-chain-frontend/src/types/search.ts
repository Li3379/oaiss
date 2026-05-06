/** 搜索-碳报告摘要 */
export interface CarbonReportSummary {
  id: number
  reportNo: string
  enterpriseId: number
  title: string
  accountingPeriod: string
  totalEmission: number
  status: number
  createdAt: string
}

/** 搜索-交易摘要 */
export interface TradeSummary {
  id: number
  tradeNo: string
  tradeType: number
  sellerId: number
  buyerId: number
  quantity: number
  unitPrice: number
  totalAmount: number
  status: number
  createdAt: string
}

/** 搜索-市场概览 */
export interface MarketOverview {
  totalEnterprises: number
  totalCarbonReports: number
  totalTransactions: number
  queryTime: string
}
