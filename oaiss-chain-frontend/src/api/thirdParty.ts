import request from './request'
import type {
  ThirdPartyCarbonReportPage,
  ThirdPartyCarbonReportQuery,
  ThirdPartyOrg,
  ThirdPartyStatistics,
} from '../types'

export function getCarbonReports(params?: ThirdPartyCarbonReportQuery): Promise<ThirdPartyCarbonReportPage> {
  return request.get('/third-party/carbon-reports', { params })
}

export function getStatistics(): Promise<ThirdPartyStatistics> {
  return request.get('/third-party/statistics')
}

export function getOrgInfo(): Promise<ThirdPartyOrg> {
  return request.get('/third-party/org-info')
}

export function updateContact(data: { contactPerson: string; contactPhone: string }): Promise<void> {
  if (!data?.contactPerson) return Promise.reject(new Error('Contact person is required'))
  if (!data?.contactPhone) return Promise.reject(new Error('Contact phone is required'))
  return request.put('/third-party/contact', null, { params: data })
}
