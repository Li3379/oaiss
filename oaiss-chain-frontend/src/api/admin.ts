import request from './request'
import type { PageRequest } from '../types'

export function getUserList(params?: PageRequest): Promise<unknown> {
  return request.get('/admin/users', { params })
}

export function updateUserStatus(userId: number, status: number): Promise<void> {
  return request.put(`/admin/users/${userId}/status`, null, { params: { status } })
}

export function getStatistics(): Promise<unknown> {
  return request.get('/admin/statistics')
}

// ============ Enterprise Admission (准入证书) ============

export function getEnterpriseAdmissionList(params?: PageRequest): Promise<unknown> {
  return request.get('/admin/enterprise-admission', { params })
}

export function issueEnterpriseAdmission(enterpriseId: number): Promise<unknown> {
  return request.post(`/admin/enterprise-admission/${enterpriseId}/issue`)
}

export function revokeEnterpriseAdmission(enterpriseId: number): Promise<void> {
  return request.delete(`/admin/enterprise-admission/${enterpriseId}`)
}

export function getMyEnterpriseAdmission(): Promise<unknown> {
  return request.get('/admin/enterprise-admission/my')
}

// ============ Reviewer Qualification (审核员资格证) ============

export function getReviewerQualificationList(params?: PageRequest): Promise<unknown> {
  return request.get('/admin/reviewer-qualification', { params })
}

export function issueReviewerQualification(reviewerId: number): Promise<unknown> {
  return request.post(`/admin/reviewer-qualification/${reviewerId}/issue`)
}

export function revokeReviewerQualification(reviewerId: number): Promise<void> {
  return request.delete(`/admin/reviewer-qualification/${reviewerId}`)
}

export function getMyReviewerQualification(): Promise<unknown> {
  return request.get('/admin/reviewer-qualification/my')
}
