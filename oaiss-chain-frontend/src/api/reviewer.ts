import request from './request'
import type {
  CarbonReportResponse,
  PageRequest,
  PagedItems,
  ReviewerInfoResponse,
  ReviewerQualificationResponse,
  ReviewerStatisticsResponse,
} from '../types'

export function getReviewerInfo(): Promise<ReviewerInfoResponse> {
  return request.get('/reviewer/info')
}

export function getPendingReports(params?: PageRequest): Promise<PagedItems<CarbonReportResponse>> {
  return request.get('/reviewer/reports/pending', { params })
}

export function getReviewHistory(params?: PageRequest): Promise<PagedItems<CarbonReportResponse>> {
  return request.get('/reviewer/history', { params })
}

export function getMyReviewerQualification(): Promise<ReviewerQualificationResponse[]> {
  return request.get('/reviewer/qualification/my')
}

export function getStatistics(): Promise<ReviewerStatisticsResponse> {
  return request.get('/reviewer/statistics')
}
