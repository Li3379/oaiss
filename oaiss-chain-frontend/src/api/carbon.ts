import request from './request'
import type { CarbonReportRequest, CarbonReportResponse, ReviewRequest, PageRequest } from '../types'

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

export function reviewReport(data: ReviewRequest): Promise<void> {
  if (!data?.reportId) return Promise.reject(new Error('报告ID不能为空'))
  if (!data?.status) return Promise.reject(new Error('审核状态不能为空'))
  return request.post('/carbon/review', data)
}
