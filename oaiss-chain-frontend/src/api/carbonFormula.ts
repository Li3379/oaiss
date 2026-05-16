import request from './request'
import type { PowerGenerationCalculationRequest, PowerGenerationCalculationResponse, PowerGridCalculationRequest, PowerGridCalculationResponse } from '@/types/carbonFormula'

export function calculatePowerGeneration(data: PowerGenerationCalculationRequest): Promise<PowerGenerationCalculationResponse> {
  return request.post('/carbon/calculate/power-generation', data)
}

export function calculatePowerGrid(data: PowerGridCalculationRequest): Promise<PowerGridCalculationResponse> {
  return request.post('/carbon/calculate/power-grid', data)
}
