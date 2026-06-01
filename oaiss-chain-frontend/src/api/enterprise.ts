import request from './request'
import type { EnterpriseAdmissionResponse } from '../types/admin'
import type { EnterpriseQuotaResponse, EnterpriseResponse } from '../types/user'

export function getEnterpriseInfo(): Promise<EnterpriseResponse> {
  return request.get('/enterprise/info')
}

export function getQuotaInfo(): Promise<EnterpriseQuotaResponse> {
  return request.get('/enterprise/quota')
}

export function updateContact(data: { contactPerson: string; contactPhone: string }): Promise<void> {
  if (!data?.contactPerson) return Promise.reject(new Error('Contact person is required'))
  if (!data?.contactPhone) return Promise.reject(new Error('Contact phone is required'))
  return request.put('/enterprise/contact', null, { params: data })
}

export function getMyEnterpriseAdmission(): Promise<EnterpriseAdmissionResponse[]> {
  return request.get('/enterprise/admission/my')
}

export function getEnterpriseById(enterpriseId: number): Promise<EnterpriseResponse> {
  if (!enterpriseId) return Promise.reject(new Error('Enterprise ID is required'))
  return request.get(`/enterprise/${enterpriseId}`)
}
