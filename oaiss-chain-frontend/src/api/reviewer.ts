import request from './request'
import type { PageRequest } from '../types'

export function getReviewerInfo(): Promise<unknown> {
  return request.get('/reviewer/info')
}

export function getPendingReports(params?: PageRequest): Promise<unknown> {
  return request.get('/reviewer/reports/pending', { params })
}

export function getReviewHistory(params?: PageRequest): Promise<unknown> {
  return request.get('/reviewer/history', { params })
}

export function getMyReviewerQualification(): Promise<unknown> {
  return request.get('/reviewer/qualification/my')
}

export function getStatistics(): Promise<unknown> {
  return request.get('/reviewer/statistics')
}
