import request from './request'
import type { PageRequest } from '../types'

export function getCarbonReports(params?: PageRequest): Promise<unknown> {
  return request.get('/third-party/carbon-reports', { params })
}

export function getStatistics(): Promise<unknown> {
  return request.get('/third-party/statistics')
}
