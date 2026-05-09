import request from './request'
import type { CarbonReportRequest, CarbonReportResponse, PageRequest } from '../types'

export function createReport(data: CarbonReportRequest): Promise<CarbonReportResponse> {
  if (!data?.title) return Promise.reject(new Error('报告标题不能为空'))
  return request.post('/carbon/reports', data)
}

export function submitReport(reportId: number): Promise<void> {
  if (!reportId) return Promise.reject(new Error('报告ID不能为空'))
  return request.post(`/carbon/reports/${reportId}/submit`)
}

export function getReportList(params?: PageRequest): Promise<unknown> {
  return request.get('/carbon/reports', { params })
}

export function getMyReports(params?: PageRequest): Promise<unknown> {
  return request.get('/carbon/my-reports', { params })
}

export function deleteReport(reportId: number): Promise<void> {
  if (!reportId) return Promise.reject(new Error('报告ID不能为空'))
  return request.delete(`/carbon/reports/${reportId}`)
}

export function reviewReport(data: { reportId: number; approved: boolean; comment: string }): Promise<void> {
  if (!data?.reportId) return Promise.reject(new Error('报告ID不能为空'))
  return request.post('/carbon/review', {
    reportId: data.reportId,
    reviewResult: data.approved ? 3 : 4,
    reviewComment: data.comment
  })
}
