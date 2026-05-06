import request from './request'
import type { CarbonNeutralProjectRequest, CarbonNeutralProjectResponse, ProjectVerificationRequest, PageRequest } from '../types'

export function getProjects(params?: PageRequest): Promise<unknown> {
  return request.get('/carbon-neutral/search', { params })
}

export function getProject(projectId: number): Promise<CarbonNeutralProjectResponse> {
  return request.get(`/carbon-neutral/${projectId}`)
}

export function createProject(data: CarbonNeutralProjectRequest): Promise<CarbonNeutralProjectResponse> {
  return request.post('/carbon-neutral', data)
}

export function updateProject(projectId: number, data: Partial<CarbonNeutralProjectRequest>): Promise<CarbonNeutralProjectResponse> {
  return request.put(`/carbon-neutral/${projectId}`, data)
}

export function submitProject(projectId: number): Promise<void> {
  return request.post(`/carbon-neutral/${projectId}/submit`)
}

export function startProject(projectId: number): Promise<void> {
  return request.post(`/carbon-neutral/${projectId}/start`)
}

export function submitVerification(projectId: number, data: ProjectVerificationRequest): Promise<void> {
  return request.post(`/carbon-neutral/${projectId}/submit-verification`, data)
}

export function updateMonitoring(projectId: number, data: { monitoringData: Record<string, unknown> }): Promise<void> {
  return request.put(`/carbon-neutral/${projectId}/monitoring`, data)
}

export function applyCertification(projectId: number): Promise<void> {
  return request.post(`/carbon-neutral/${projectId}/apply-certification`)
}

export function terminateProject(projectId: number, data: { reason: string }): Promise<void> {
  return request.post(`/carbon-neutral/${projectId}/terminate`, data)
}
