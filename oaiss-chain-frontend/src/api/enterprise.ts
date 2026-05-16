import request from './request'

export function getEnterpriseInfo(): Promise<unknown> {
  return request.get('/enterprise/info')
}

export function getQuotaInfo(): Promise<unknown> {
  return request.get('/enterprise/quota')
}

export function updateContact(data: { contactPerson: string; contactPhone: string }): Promise<void> {
  if (!data?.contactPerson) return Promise.reject(new Error('联系人不能为空'))
  if (!data?.contactPhone) return Promise.reject(new Error('联系电话不能为空'))
  return request.put('/enterprise/contact', data)
}

export function getEnterpriseById(enterpriseId: number): Promise<unknown> {
  if (!enterpriseId) return Promise.reject(new Error('企业ID不能为空'))
  return request.get(`/enterprise/${enterpriseId}`)
}
