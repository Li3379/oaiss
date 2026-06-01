import request from './request'
import type { CarbonReportRequest, CarbonReportResponse, PageRequest, PageResponse } from '../types'
import { ReportStatusEnum } from '../types'

export function createReport(data: CarbonReportRequest): Promise<CarbonReportResponse> {
  if (!data?.title) return Promise.reject(new Error('Report title is required'))
  return request.post('/carbon/reports', data)
}

export function submitReport(reportId: number): Promise<void> {
  if (!reportId) return Promise.reject(new Error('Report ID is required'))
  return request.post(`/carbon/reports/${reportId}/submit`)
}

export function getReportList(params?: PageRequest): Promise<PageResponse<CarbonReportResponse>> {
  return request.get('/carbon/reports', { params })
}

export function getMyReports(params?: PageRequest): Promise<PageResponse<CarbonReportResponse>> {
  return request.get('/carbon/my-reports', { params })
}

export function deleteReport(reportId: number): Promise<void> {
  if (!reportId) return Promise.reject(new Error('Report ID is required'))
  return request.delete(`/carbon/reports/${reportId}`)
}

export function reviewReport(data: { reportId: number; approved: boolean; comment: string }): Promise<void> {
  if (!data?.reportId) return Promise.reject(new Error('Report ID is required'))
  return request.post('/carbon/review', {
    reportId: data.reportId,
    reviewResult: data.approved ? ReportStatusEnum.APPROVED : ReportStatusEnum.REJECTED,
    reviewComment: data.comment,
  })
}

export function certifyReport(data: { reportId: number; approved: boolean; comment: string }): Promise<void> {
  if (!data?.reportId) return Promise.reject(new Error('Report ID is required'))
  return request.post('/carbon/certify', {
    reportId: data.reportId,
    reviewResult: data.approved ? ReportStatusEnum.ON_CHAIN : ReportStatusEnum.REJECTED,
    reviewComment: data.comment,
  })
}

export function getReport(reportId: number): Promise<CarbonReportResponse> {
  if (!reportId) return Promise.reject(new Error('Report ID is required'))
  return request.get(`/carbon/reports/${reportId}`)
}
