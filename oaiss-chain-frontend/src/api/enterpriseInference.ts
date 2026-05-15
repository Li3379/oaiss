import request from './request'
import type { EnterpriseInferenceResponse } from '@/types/ai'

export function getEnterpriseInference(enterpriseId: number): Promise<EnterpriseInferenceResponse> {
  return request.get(`/predict/enterprise/${enterpriseId}/inference`)
}
