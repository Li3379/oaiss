import request from './request'
import type { CarbonReportRequest, CarbonReportResponse, PageRequest, PageResponse } from '../types'

const REVIEW_APPROVED = 3
const REVIEW_REJECTED = 4
const CERTIFY_APPROVED = 5
const CERTIFY_REJECTED = 4

export function createReport(data: CarbonReportRequest): Promise<CarbonReportResponse> {
  if (!data?.title) return Promise.reject(new Error('鎶ュ憡鏍囬涓嶈兘涓虹┖'))
  return request.post('/carbon/reports', data)
}

export function submitReport(reportId: number): Promise<void> {
  if (!reportId) return Promise.reject(new Error('鎶ュ憡ID涓嶈兘涓虹┖'))
  return request.post(`/carbon/reports/${reportId}/submit`)
}

export function getReportList(params?: PageRequest): Promise<PageResponse<CarbonReportResponse>> {
  return request.get('/carbon/reports', { params })
}

export function getMyReports(params?: PageRequest): Promise<PageResponse<CarbonReportResponse>> {
  return request.get('/carbon/my-reports', { params })
}

export function deleteReport(reportId: number): Promise<void> {
  if (!reportId) return Promise.reject(new Error('鎶ュ憡ID涓嶈兘涓虹┖'))
  return request.delete(`/carbon/reports/${reportId}`)
}

export function reviewReport(data: { reportId: number; approved: boolean; comment: string }): Promise<void> {
  if (!data?.reportId) return Promise.reject(new Error('鎶ュ憡ID涓嶈兘涓虹┖'))
  return request.post('/carbon/review', {
    reportId: data.reportId,
    reviewResult: data.approved ? REVIEW_APPROVED : REVIEW_REJECTED,
    reviewComment: data.comment,
  })
}

export function certifyReport(data: { reportId: number; approved: boolean; comment: string }): Promise<void> {
  if (!data?.reportId) return Promise.reject(new Error('鎶ュ憡ID涓嶈兘涓虹┖'))
  return request.post('/carbon/certify', {
    reportId: data.reportId,
    reviewResult: data.approved ? CERTIFY_APPROVED : CERTIFY_REJECTED,
    reviewComment: data.comment,
  })
}

export function getReport(reportId: number): Promise<CarbonReportResponse> {
  if (!reportId) return Promise.reject(new Error('鎶ュ憡ID涓嶈兘涓虹┖'))
  return request.get(`/carbon/reports/${reportId}`)
}
