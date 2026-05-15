/** Power generation fuel parameters */
export interface FuelParams {
  fc: number | null  // fuel consumption (t)
  ncv: number | null // net calorific value (GJ/t)
  cc: number | null  // carbon content (tC/GJ)
  of: number | null  // oxidation rate (0-1)
}

/** Power generation calculation request */
export interface PowerGenerationCalculationRequest {
  rawCoal: FuelParams
  cleanedCoal: FuelParams
  otherWashedCoal: FuelParams
  briquette: FuelParams
  otherCoal: FuelParams
  carbonateConsumed: number | null
  desulfEmissionFactor: number | null
  desulfConversionRate: number | null
  reportingYear: number
  enterpriseName: string
}

/** Fuel emission detail */
export interface FuelEmissionDetail {
  fuelType: string
  fuelConsumption: number
  netCalorificValue: number
  carbonContent: number
  oxidationRate: number
  emission: number
}

/** Power generation calculation response */
export interface PowerGenerationCalculationResponse {
  totalEmission: number
  combustionEmission: number
  desulfurizationEmission: number
  fuelDetails: FuelEmissionDetail[]
  reportingYear: string
  enterpriseName: string
  formulaReference: string
  calculatedAt: string
}

/** Power grid calculation request */
export interface PowerGridCalculationRequest {
  transmissionVolume: number | null
  lineLossRate: number | null
  gridEmissionFactor: number | null
  generationVolume: number | null
  importedElectricity: number | null
  exportedElectricity: number | null
  importEmissionFactor: number | null
  reportingYear: number
  enterpriseName: string
}

/** Power grid calculation response */
export interface PowerGridCalculationResponse {
  totalEmission: number
  transmissionLossEmission: number
  importedEmission: number
  transmissionLoss: number
  formulaReference: string
  reportingYear: string
  enterpriseName: string
  calculatedAt: string
}
