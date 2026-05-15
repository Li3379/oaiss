import request from './request'
import type { PowerGenerationCalculationRequest, PowerGenerationCalculationResponse, PowerGridCalculationRequest, PowerGridCalculationResponse } from '@/types/carbonFormula'
import type { ApiResponse } from '@/types/api'

export function calculatePowerGeneration(data: PowerGenerationCalculationRequest): Promise<ApiResponse<PowerGenerationCalculationResponse>> {
  return request.post('/carbon/calculate/power-generation', data)
}

export function calculatePowerGrid(data: PowerGridCalculationRequest): Promise<ApiResponse<PowerGridCalculationResponse>> {
  return request.post('/carbon/calculate/power-grid', data)
}
