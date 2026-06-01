# Carbon Accounting Industry-Specific Formulas - Research

**Researched:** 2026-05-14
**Domain:** Carbon emission accounting standards and industry-specific calculation formulas
**Confidence:** MEDIUM (formulas and parameters verified from project docs; IPCC/China grid factors from training knowledge)

## Summary

This research documents the industry-specific carbon emission calculation formulas required for the OAISS CHAIN carbon trading platform. The platform currently implements a generic scope-based calculation (`emission = activity_data x emission_factor`) in `CarbonService.calculateEmissions()`, but needs to be upgraded with two China national standard-compliant industry models:

1. **Power Generation (发电企业)**: A 25-parameter model based on "中国发电企业温室气体排放核算方法与报告指南（试行）" with three emission components: fossil fuel combustion, desulfurization process, and purchased electricity.
2. **Power Grid (电网企业)**: A 9-parameter model based on "中国电网企业温室气体排放核算方法与报告指南（试行）" with two emission components: SF6 leakage from equipment and electricity transmission.

Both formulas decompose total emissions into scope-aligned subtotals and require fuel-specific default parameters (NCV, CC, OF) from national lookup tables. The existing `emissionData` JSON field in `CarbonReport` entity already provides the storage mechanism, but the calculation engine and structured input schema are not yet implemented.

**Primary recommendation:** Implement the two industry models as separate `CalculationService` implementations behind a `CarbonCalculationService` interface, with a fuel default parameters table seeded via Flyway migration and regional grid emission factors stored as configurable reference data.

## Industry Formulas

### Formula 1: Power Generation Enterprise (发电企业) - 25 Parameters

**Standard:** 《中国发电企业温室气体排放核算方法与报告指南（试行）》 [CITED: docs/specs/CARBON-CALCULATION-SPEC.md]
**Related national standard:** GB/T 32150 series (industrial enterprise GHG emission accounting) [ASSUMED]

#### Master Formula

```
E = E_combustion + E_desulfurization + E_electricity
```

Where:

**Component 1 - Fossil Fuel Combustion Emissions (Scope 1):**
```
E_combustion = SUM_i [ (FC_i x NCV_i x 10^-6) x (CC_i x OF_i x 44/12) ]
```

**Component 2 - Desulfurization Process Emissions (Scope 1):**
```
E_desulfurization = SUM_k [ (SUM_m B_k,m x 90%) x (EF_k,t x 100%) ]
```

**Component 3 - Purchased Electricity Indirect Emissions (Scope 2):**
```
E_electricity = AD_electricity x EF_grid
```

**Total:**
```
E = SUM_i[(FC_i x NCV_i x 10^-6) x (CC_i x OF_i x 44/12)]
  + SUM_k[(SUM_m B_k,m x 90%) x (EF_k,t x 100%)]
  + (AD_electricity x EF_grid)
```

#### Variable Definitions

| Symbol | Description | Unit | Scope |
|--------|-------------|------|-------|
| E | Total GHG emissions | tCO2e | - |
| FC_i | Consumption of fuel type i | tonnes or 10^3 Nm^3 | Scope 1 |
| NCV_i | Average net calorific value of fuel type i | kJ/kg or kJ/Nm^3 | Scope 1 |
| CC_i | Carbon content per unit calorific value of fuel type i | tC/TJ | Scope 1 |
| OF_i | Carbon oxidation rate of fuel type i | % | Scope 1 |
| 44/12 | CO2/C molecular weight ratio | dimensionless | Scope 1 |
| B_k,m | Consumption of desulfurizer type m in process k | tonnes | Scope 1 |
| EF_k,t | Emission factor for desulfurization process k | tCO2/tonne | Scope 1 |
| AD_electricity | Net purchased electricity | MWh | Scope 2 |
| EF_grid | Regional grid emission factor | tCO2/MWh | Scope 2 |

#### Sub-Calculations for NCV (Weighted Averages)

Fuel NCV is computed as a weighted average of sample measurements:

**Coal NCV:**
```
NCV_coal = SUM_i (NCV_coal_daily_i x w_coal_i)
```
Where NCV_coal_daily_i = daily average NCV from lab analysis (per GB/T 213-2008), w_coal_i = daily coal consumption weight.

**Fuel Oil NCV:**
```
NCV_oil = SUM_i (NCV_oil_batch_i x w_oil_i)
```
Where NCV_oil_batch_i = batch NCV from lab analysis (per DL/T 567.8-95), w_oil_i = batch weight.

**Natural Gas NCV:**
```
NCV_gas = SUM_i (NCV_gas_monthly_i x w_gas_i)
```
Where NCV_gas_monthly_i = monthly average NCV (per GB/T 11062-1998), w_gas_i = monthly consumption weight.

#### Sub-Calculation for Carbon Content (CC)

For coal, CC is calculated from elemental carbon analysis:
```
CC_coal = (C_coal x 10^6) / NCV_coal
```
Where C_coal = average elemental carbon content (%) per GB/T 476-2008.

For fuel oil and natural gas, CC uses default values from Table 1.

#### Sub-Calculation for Carbon Oxidation Rate (OF)

For coal, OF can be calculated from slag and fly ash measurements:
```
OF_coal = 1 - [(G_slag x C_slag + G_ash x (C_ash / (1 - eta_dust))) x 10^6] / (FC_coal x NCV_coal x CC_coal)
```

Where:
- G_slag = total slag output (tonnes)
- C_slag = average carbon content in slag (%)
- G_ash = total fly ash output (tonnes)
- C_ash = average carbon content in fly ash (%)
- eta_dust = dust removal system efficiency (%), use 100% if not provided
- FC_coal = coal consumption (tonnes)

For fuel oil and gas, OF uses default values (98% and 99% respectively).

### Formula 2: Power Grid Enterprise (电网企业) - 9 Parameters

**Standard:** 《中国电网企业温室气体排放核算方法与报告指南（试行）》 [CITED: docs/specs/CARBON-CALCULATION-SPEC.md]

#### Master Formula

```
E = E_SF6 + E_transmission
```

Where:

**Component 1 - SF6 Leakage from Equipment (Scope 1):**
```
E_SF6 = [ SUM_i (REC_capacity_i - REC_recovery_i) + SUM_j (REP_capacity_j - REP_recovery_j) ] x GWP_SF6 x 10^-3
```

**Component 2 - Electricity Transmission Emissions (Scope 2):**
```
E_transmission = (EL_generation + EL_import - EL_export - EL_sold) x EF_grid
```

**Total:**
```
E = [ SUM_i(REC_capacity_i - REC_recovery_i) + SUM_j(REP_capacity_j - REP_recovery_j) ] x GWP_SF6 x 10^-3
  + (EL_generation + EL_import - EL_export - EL_sold) x EF_grid
```

#### Variable Definitions

| Symbol | Description | Unit | Scope |
|--------|-------------|------|-------|
| REC_capacity_i | SF6 capacity of retired equipment i | kg | Scope 1 |
| REC_recovery_i | Actual SF6 recovered from retired equipment i | kg | Scope 1 |
| REP_capacity_j | SF6 capacity of repair equipment j | kg | Scope 1 |
| REP_recovery_j | Actual SF6 recovered from repair equipment j | kg | Scope 1 |
| GWP_SF6 | Global warming potential of SF6 | 23900 (constant) | Scope 1 |
| EL_generation | Grid-connected generation output | MWh | Scope 2 |
| EL_import | Electricity imported from other provinces | MWh | Scope 2 |
| EL_export | Electricity exported to other provinces | MWh | Scope 2 |
| EL_sold | Electricity sold to end users | MWh | Scope 2 |
| EF_grid | Regional grid average supply emission factor | tCO2/MWh | Scope 2 |

**Measurement standards:** DL/T 448-2000, GB 17167-2006, GB/T 25095-2010, GB 17215, GB 16934-1997 [ASSUMED - from project source doc]

## Parameter Definitions

### Power Generation - 25 Input Parameters

Based on Table 2 (发电企业原始数据输入项) from the national guidelines [CITED: docs/raw/04-碳核算模型介绍文档.md and docs/specs/CARBON-CALCULATION-SPEC.md]:

| # | Parameter | Symbol | Description | Data Source |
|---|-----------|--------|-------------|-------------|
| 1 | FC_i | FC_i | Fossil fuel consumption (tonnes or 10^3 Nm^3) | Enterprise metering |
| 2 | NCV_coal_daily | NCV_coal/daily_i | Daily average NCV of coal (kJ/kg) | Lab analysis per GB/T 213-2008 |
| 3 | w_coal | w_coal_i | Daily coal consumption weight | Enterprise metering |
| 4 | NCV_oil_batch | NCV_oil/batch_i | Batch NCV of fuel oil (kJ/kg) | Lab analysis per DL/T 567.8-95 |
| 5 | w_oil | w_oil_i | Batch fuel oil weight | Enterprise metering |
| 6 | NCV_gas_monthly | NCV_gas/monthly_i | Monthly average NCV of natural gas | Lab analysis per GB/T 11062-1998 |
| 7 | w_gas | w_gas_i | Monthly gas consumption weight | Enterprise metering |
| 8 | NCV_other | NCV_other_i | NCV of other fuels (biomass, etc.) | Lab analysis |
| 9 | w_other | w_other_i | Other fuel consumption weight | Enterprise metering |
| 10 | C_coal | C_coal | Average elemental carbon content of coal (%) | Lab analysis per GB/T 476-2008 |
| 11 | CC_oil | CC_oil | Unit calorific carbon content of oil (tC/TJ) | Default value from Table 1 |
| 12 | CC_gas | CC_gas | Unit calorific carbon content of gas (tC/TJ) | Default value from Table 1 |
| 13 | C_other | C_other | Unit calorific carbon content of other fuels | Default value / lab analysis |
| 14 | G_slag | G_slag | Total slag output (tonnes) | Enterprise measurement |
| 15 | G_ash | G_ash | Total fly ash output (tonnes) | Enterprise measurement |
| 16 | C_slag | C_slag | Average carbon content in slag (%) | Lab analysis |
| 17 | C_ash | C_ash | Average carbon content in fly ash (%) | Lab analysis |
| 18 | eta_dust | eta_dust | Dust removal system efficiency (%) | Enterprise data (default 100%) |
| 19 | OF_oil | OF_oil | Carbon oxidation rate of oil (%) | Default 98% |
| 20 | OF_gas | OF_gas | Carbon oxidation rate of gas (%) | Default 98% |
| 21 | OF_other | OF_other | Carbon oxidation rate of other fuels (%) | Default / lab analysis |
| 22 | B_k,m | B_k,m | Desulfurizer consumption (tonnes) | Enterprise metering |
| 23 | EF_k,t | EF_k,t | Desulfurization emission factor (tCO2/tonne) | Lab measurement |
| 24 | AD_electricity | AD_electricity | Net purchased electricity (MWh) | Enterprise metering |
| 25 | EF_grid | EF_grid | Regional grid emission factor (tCO2/MWh) | National published data |

### Power Grid - 9 Input Parameters

Based on Table 3 (电网企业原始数据输入项) from the national guidelines [CITED: docs/specs/CARBON-CALCULATION-SPEC.md]:

| # | Parameter | Symbol | Description | Data Source |
|---|-----------|--------|-------------|-------------|
| 1 | REC_capacity_i | REC_capacity_i | SF6 capacity of retired equipment i (kg) | Equipment nameplate data |
| 2 | REC_recovery_i | REC_recovery_i | Actual SF6 recovered from retired equipment i (kg) | Enterprise measurement |
| 3 | REP_capacity_j | REP_capacity_j | SF6 capacity of repair equipment j (kg) | Equipment nameplate data |
| 4 | REP_recovery_j | REP_recovery_j | Actual SF6 recovered from repair equipment j (kg) | Enterprise measurement |
| 5 | EL_generation | EL_generation | Grid-connected generation output (MWh) | Enterprise metering |
| 6 | EL_import | EL_import | Electricity imported from other provinces (MWh) | Enterprise metering |
| 7 | EL_export | EL_export | Electricity exported to other provinces (MWh) | Enterprise metering |
| 8 | EL_sold | EL_sold | Electricity sold to end users (MWh) | Enterprise metering |
| 9 | EF_grid | EF_grid | Regional grid average supply emission factor (tCO2/MWh) | National published data |

## Fuel Default Parameters (Table 1)

Common fossil fuel default parameters from the national guidelines [CITED: docs/specs/CARBON-CALCULATION-SPEC.md, docs/raw/04-碳核算模型介绍文档.md]:

| Fuel Type (能源名称) | Avg NCV (kJ/kg or kJ/Nm^3) | Unit Carbon Content (tC/TJ) | Carbon Oxidation Rate (%) |
|----------------------|---------------------------|---------------------------|--------------------------|
| Crude Oil (原油) | 41816 | 20.08 | 98 |
| Fuel Oil (燃料油) | 41816 | 21.1 | 98 |
| Gasoline (汽油) | 43070 | 18.9 | 98 |
| Diesel (柴油) | 42652 | 20.2 | 98 |
| Natural Gas (天然气) | 38931 | 15.32 | 99 |
| Coke Oven Gas (焦炉煤气) | 12726-17981 | 13.58 | - |
| Blast Furnace Gas (高炉煤气) | 12726-17981 | 13.58 | - |
| Other Coal Gas (其他煤气) | 52270 | 12.2 | - |

### Coal-Specific Default Parameters [ASSUMED - from IPCC 2006 Guidelines Vol 2 Ch 1 & 2]

| Coal Type | Avg NCV (kJ/kg) | Unit Carbon Content (tC/TJ) | Carbon Oxidation Rate (%) |
|-----------|-----------------|---------------------------|--------------------------|
| Anthracite (无烟煤) | 26350 | 26.8 | 98 |
| Bituminous (烟煤) | 20910 | 25.8 | 98 |
| Lignite (褐煤) | 12600 | 27.6 | 98 |
| Cleaned Coal (洗精煤) | 26350 | 25.8 | 98 |
| Other Washed Coal (其他洗煤) | 8360-20910 | 25.8 | 98 |

## Regional Grid Emission Factors (China)

China has six regional power grids, each with published emission factors updated periodically by the Ministry of Ecology and Environment (MEE). These factors are used for Scope 2 calculations (EF_grid) and for CDM baseline calculations. [ASSUMED - WebSearch could not retrieve current values; these are from training knowledge based on MEE publications through 2023]

### Regional Grid OM Emission Factors (tCO2/MWh)

The Operation Margin (OM) factor reflects the marginal emission rate of the grid:

| Region | Chinese Name | OM Factor (tCO2/MWh) | Year |
|--------|-------------|---------------------|------|
| North China | 华北区域 | ~0.8479 | 2022 |
| Northeast | 东北区域 | ~0.8159 | 2022 |
| East China | 华东区域 | ~0.7268 | 2022 |
| Central China | 华中区域 | ~0.6035 | 2022 |
| Northwest | 西北区域 | ~0.6944 | 2022 |
| South China | 南方区域 | ~0.5189 | 2022 |

### Regional Grid BM Emission Factors (tCO2/MWh)

The Build Margin (BM) factor reflects the emission rate of newly added capacity:

| Region | Chinese Name | BM Factor (tCO2/MWh) | Year |
|--------|-------------|---------------------|------|
| North China | 华北区域 | ~0.3166 | 2022 |
| Northeast | 东北区域 | ~0.2567 | 2022 |
| East China | 华东区域 | ~0.2045 | 2022 |
| Central China | 华中区域 | ~0.2014 | 2022 |
| Northwest | 西北区域 | ~0.1174 | 2022 |
| South China | 南方区域 | ~0.1174 | 2022 |

### Combined Emission Factor for Carbon Trading

For national carbon emission trading compliance, the combined emission factor (weighted average of OM and BM) is typically used. The MEE publishes updated factors annually. The platform should store these as versioned reference data. [ASSUMED]

**Important:** The above OM/BM values are approximate and need verification against the latest MEE publication before production use. The platform must support configurable, versioned emission factor tables.

## General Carbon Accounting Standards

### Scope 1: Direct Emissions

**Definition:** GHG emissions from sources owned or controlled by the reporting enterprise. [CITED: GHG Protocol Corporate Standard via ghgprotocol.org]

**Calculation methods:**
1. **Fuel combustion:** `E = SUM(FC_i x NCV_i x 10^-6 x CC_i x OF_i x 44/12)` -- the primary method for power generation
2. **Process emissions:** Calculated from process-specific emission factors (e.g., desulfurization: `B_k,m x EF_k,t`)
3. **Fugitive emissions:** Equipment leakage (e.g., SF6 leakage in grid enterprises: `capacity - recovery`)

**For the power generation industry, Scope 1 covers:**
- Fossil fuel combustion emissions (Component 1 of the 25-parameter formula)
- Desulfurization process emissions (Component 2 of the 25-parameter formula)

**For the power grid industry, Scope 1 covers:**
- SF6 leakage from retired/repaired equipment (Component 1 of the 9-parameter formula)

### Scope 2: Indirect Energy Emissions

**Definition:** GHG emissions from purchased electricity, steam, heat, and cooling. [CITED: GHG Protocol Corporate Standard]

**Calculation:** `E = purchased_energy x grid_emission_factor`

**For power generation:** Net purchased electricity x regional grid emission factor
**For power grid:** (EL_generation + EL_import - EL_export - EL_sold) x EF_grid

**Dual reporting requirement:** GHG Protocol Scope 2 Guidance (2015) requires reporting using both:
- **Location-based method:** Uses average grid emission factors for the grid region
- **Market-based method:** Uses contractual/instrument-specific factors (e.g., RECs, PPAs)

The China national guidelines use the location-based method. [ASSUMED]

### Scope 3: Other Indirect Emissions

**Definition:** All other indirect emissions in the value chain. [CITED: GHG Protocol Corporate Standard]

**For power generation, relevant Scope 3 categories include:**
- Upstream fuel extraction and transport
- Employee commuting
- Business travel
- Purchased goods and services

**Current implementation:** The existing `CarbonReport` entity has a `scope3Emission` field, but no structured calculation model. The national guidelines for power generation and grid enterprises do not mandate Scope 3 reporting. [CITED: project codebase - CarbonReport.java]

### Emission Factor Databases

| Database | Publisher | Coverage | Usage in This Platform |
|----------|-----------|----------|----------------------|
| IPCC 2006 Guidelines (Vol 2) | IPCC | Global default factors for all fuel types | Cross-reference for validation |
| IPCC 2019 Refinement | IPCC | Updated 2006 defaults | Preferred over 2006 where available |
| China National Default Values (Table 1) | MEE/NDRC | China-specific fuel defaults | PRIMARY source for this platform |
| Regional Grid Emission Factors | MEE | OM/BM factors per grid region | Scope 2 calculations |
| EPA Emission Factors Hub | US EPA | US-specific factors | NOT applicable |

**Key distinction:** China national default values (Table 1) differ from IPCC defaults because they reflect China-specific fuel quality and measurement standards. The platform MUST use China national defaults as primary, with IPCC as validation cross-reference. [CITED: docs/specs/CARBON-CALCULATION-SPEC.md]

## Implementation Guidance

### Architecture for Calculation Engine

The current `CarbonService.calculateEmissions()` uses a generic `activity_data x emission_factor` approach. The upgrade path requires:

```
CarbonCalculationService (interface)
  |-- PowerGenerationCalculationService (25-parameter model)
  |-- PowerGridCalculationService (9-parameter model)
  |-- GenericCalculationService (current behavior, backward-compatible)
```

**Routing logic:** Select implementation based on `industryType` field:
- `POWER_GENERATION` -> PowerGenerationCalculationService
- `POWER_GRID` -> PowerGridCalculationService
- `null/generic` -> GenericCalculationService (current behavior)

### Data Storage Strategy

The existing `emission_data` JSON field in `carbon_report` table provides flexible storage. The structured input data for each industry model should be stored as typed JSON within this field:

```json
{
  "industryType": "POWER_GENERATION",
  "powerGenerationData": {
    "coalConsumption": 50000,
    "coalNcvDaily": 20910,
    "oilConsumption": 1000,
    "gasConsumption": 500,
    "coalCarbonContent": 60.5,
    "coalOxidationRate": 98,
    "oilOxidationRate": 98,
    "gasOxidationRate": 99,
    "desulfurizerConsumption": 200,
    "desulfurizerEmissionFactor": 0.44,
    "purchasedElectricity": 10000,
    "gridEmissionFactor": 0.7268
  },
  "scope1": 12500.00,
  "scope2": 7268.00,
  "scope3": 0
}
```

### Fuel Default Parameters Table

Create a `fuel_default_parameter` reference table seeded via Flyway migration:

```sql
CREATE TABLE fuel_default_parameter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fuel_type VARCHAR(50) NOT NULL,       -- COAL_ANTHRACITE, COAL_BITUMINOUS, etc.
    fuel_name_zh VARCHAR(100) NOT NULL,   -- 无烟煤, 烟煤, etc.
    avg_ncv DECIMAL(15,4),                -- Average NCV (kJ/kg)
    unit_carbon_content DECIMAL(15,4),    -- CC (tC/TJ)
    carbon_oxidation_rate DECIMAL(5,2),   -- OF (%)
    effective_year INT,                   -- Year these values are valid for
    source VARCHAR(200),                  -- GB/T standard reference
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

### Regional Grid Emission Factor Table

```sql
CREATE TABLE grid_emission_factor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    region_code VARCHAR(20) NOT NULL,     -- NORTH, NORTHEAST, EAST, CENTRAL, NORTHWEST, SOUTH
    region_name_zh VARCHAR(50) NOT NULL,  -- 华北, 东北, etc.
    om_factor DECIMAL(15,6),              -- Operation Margin factor (tCO2/MWh)
    bm_factor DECIMAL(15,6),              -- Build Margin factor (tCO2/MWh)
    combined_factor DECIMAL(15,6),        -- Combined/average factor
    effective_year INT NOT NULL,          -- Publication year
    published_date DATE,                  -- When MEE published these values
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

### Validation Rules

1. **Required field validation:** All 25 parameters (power gen) or 9 parameters (grid) must have values or explicit default fallback
2. **Range validation:** NCV, CC, OF values must fall within physically plausible ranges
3. **Default value warnings:** When a parameter uses a default value (not measured), flag in response
4. **Precision:** All calculation results to 4 decimal places (matching `DECIMAL(15,4)` in DB)
5. **Negative value check:** Emission results must be >= 0; negative values indicate data errors
6. **Cross-validation:** Total emission should approximately equal sum of scope1 + scope2 + scope3

## Verification and Audit Requirements

### MRV Requirements (Monitoring, Reporting, Verification)

China's national carbon emission trading system requires MRV compliance [ASSUMED]:

| Phase | Requirement | Platform Support |
|-------|-------------|-----------------|
| **Monitoring** | Enterprises must maintain fuel consumption records, lab analysis reports, metering data | `emission_data` JSON storage with structured input |
| **Reporting** | Annual emission reports following national templates, signed by responsible person | `CarbonReport` entity with status workflow (DRAFT -> SUBMITTED -> APPROVED -> ON_CHAIN) |
| **Verification** | Third-party verification of emission data and calculation methodology | REVIEWER role with RSA signature verification |

### Data Quality Assurance

| Check | Implementation | Status |
|-------|---------------|--------|
| Material balance verification | Cross-check fuel consumption against inventory records | To implement |
| Default value flagging | Alert when calculated parameters use defaults instead of measured values | Specified in CARBON-CALCULATION-SPEC.md |
| Calculation traceability | Store intermediate calculation results (E_combustion, E_desulfurization, E_electricity) | Specified in CarbonReportEmissionData |
| Precision verification | Compare results with national standard calculator, tolerance < 0.01% | Specified in CARBON-CALCULATION-SPEC.md |
| RSA digital signature | Sign emission data before submission, verify on review | Implemented in DigitalSignatureService |

### Audit Trail

The existing system already supports:
1. `@AuditLog` AOP annotation for operation logging
2. Blockchain on-chain storage for immutable audit trail
3. RSA signature verification for data integrity
4. Reviewer approval workflow with comment tracking

## Common Pitfalls

### Pitfall 1: Unit Conversion Errors
**What goes wrong:** The formula mixes kJ, TJ, tonnes, and MWh across multiple conversion steps. The `10^-6` factor converts kJ to TJ, and `44/12` converts carbon to CO2.
**Why it happens:** Different parameters use different unit scales (kJ/kg vs TJ vs MWh).
**How to avoid:** Implement unit conversion as explicit, testable helper methods. Never embed implicit conversions in the main formula.
**Warning signs:** Results off by orders of magnitude (10^3 or 10^6).

### Pitfall 2: Default Value vs Measured Value Confusion
**What goes wrong:** Using default CC/OF values when measured values are available (or vice versa) leads to systematic bias.
**Why it happens:** The guidelines allow defaults but require measured values when available.
**How to avoid:** Always flag which values are defaults vs measured. Track `isDefaultValue` boolean for each parameter.
**Warning signs:** All enterprises in same region reporting identical emission factors.

### Pitfall 3: NCV Weighted Average Computation
**What goes wrong:** Computing simple average instead of consumption-weighted average for NCV.
**Why it happens:** The formula requires `NCV = SUM(NCV_i x w_i)` where `w_i` is the consumption fraction, not `NCV = AVG(NCV_i)`.
**How to avoid:** Store individual sample measurements with their weights; compute weighted average in calculation service.
**Warning signs:** NCV values that match single sample measurements rather than period averages.

### Pitfall 4: Grid Emission Factor Version Mismatch
**What goes wrong:** Using outdated grid emission factors (e.g., 2019 values for 2024 reporting).
**Why it happens:** MEE publishes updated factors annually; enterprises may not update their reference data.
**How to avoid:** Store emission factors with effective year. Validate that reporting period matches factor year. Alert when factors are more than 2 years old.
**Warning signs:** All reports using same EF_grid value regardless of reporting year.

### Pitfall 5: Carbon Oxidation Rate Over-Simplification
**What goes wrong:** Always using default OF (98% for coal) instead of calculating from slag/ash measurements.
**Why it happens:** The calculation from slag/ash (OF_coal formula) is complex; defaults are easier.
**How to avoid:** Support both paths: (1) calculated OF from slag/ash data when available, (2) default OF when slag/ash data is missing. Always record which method was used.
**Warning signs:** OF always exactly 0.98 for coal across all reports.

### Pitfall 6: Scope 2 Double Counting for Grid Enterprises
**What goes wrong:** For grid enterprises, the electricity term `(EL_generation + EL_import - EL_export - EL_sold)` can produce negative values if output + sold exceeds generation + import, which would indicate net electricity import (charging) rather than transmission loss emissions.
**Why it happens:** The formula models transmission losses, not generation emissions.
**How to avoid:** Validate that the electricity balance is physically plausible. Negative results should trigger a review flag.
**Warning signs:** Negative scope2 emission values.

## Data Sources

### Primary Sources (HIGH confidence)
- `docs/specs/CARBON-CALCULATION-SPEC.md` -- Project carbon calculation specification with formulas, parameters, and implementation plan [CITED]
- `docs/raw/04-碳核算模型介绍文档.md` -- Original carbon accounting model document with 25-parameter and 9-parameter formulas [CITED]
- `docs/product-specification.md` -- Product specification with formula definitions in section 4.2 [CITED]
- `ghgprotocol.org/corporate-standard` -- GHG Protocol Corporate Standard for Scope 1/2/3 definitions [CITED]

### Secondary Sources (MEDIUM confidence)
- `docs/specs/GAP-ANALYSIS.md` -- Gap analysis confirming current stub implementation [CITED]
- Project codebase: `CarbonService.java`, `CarbonReport.java`, `CarbonReportRequest.java` -- Current implementation state [CITED]

### Tertiary Sources (LOW confidence - from training knowledge)
- IPCC 2006 Guidelines Vol 2, Ch 2 (Stationary Combustion) -- Default emission factors for coal types [ASSUMED]
- MEE Regional Grid Emission Factor publications -- OM/BM factor values for 6 Chinese grid regions [ASSUMED]
- GB/T 32150 series -- National standard for industrial enterprise GHG emission accounting [ASSUMED]
- China MEE GHG accounting guidelines for power generation and grid enterprises -- Parameter requirements and formulas [ASSUMED for details beyond what's in project docs]

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Coal-specific NCV and CC default values (anthracite 26350/26.8, bituminous 20910/25.8, lignite 12600/27.6) | Fuel Default Parameters | Calculations for coal sub-types will be wrong |
| A2 | Regional grid OM/BM emission factor values for 2022 | Regional Grid Emission Factors | Scope 2 calculations will use incorrect factors |
| A3 | GB/T 32150 is the parent standard for the power generation/grid guidelines | Industry Formulas | Standard reference may be incorrect |
| A4 | MEE publishes updated grid emission factors annually | Regional Grid Emission Factors | Factor update mechanism may differ |
| A5 | China national guidelines use location-based method for Scope 2 | Scope 2 | May need to also support market-based method |
| A6 | Measurement standard references (DL/T 448-2000, GB 17167-2006, etc.) for grid enterprise metering | Formula 2 | Wrong measurement standards cited |
| A7 | MRV requirements as described | Verification and Audit | Compliance gaps |

**These assumptions need user confirmation before being treated as verified facts.**

## Open Questions

1. **Coal sub-type defaults:** The project docs provide defaults for "crude oil, fuel oil, gasoline, diesel, natural gas, coke oven gas" but not for coal sub-types (anthracite, bituminous, lignite). Are coal sub-type defaults available from another source, or should we require enterprises to always provide measured values for coal?

2. **Grid emission factor update mechanism:** How should the platform handle annual updates to regional grid emission factors? Should there be an admin UI for manual updates, or an automated feed from MEE publications?

3. **Scope 3 methodology:** The current system has a `scope3Emission` field but no calculation model. Should Scope 3 be implemented for the initial release, or deferred?

4. **Biomass co-firing treatment:** The `CarbonReportEmissionData` entity includes `biomassConsumption` and `biomassNcvMonthly`, but the national formula does not explicitly include biomass. How should biomass co-firing emissions be treated -- as zero (carbon-neutral) or with a separate calculation?

5. **Market-based Scope 2:** Should the platform support market-based Scope 2 reporting (RECs, PPAs) in addition to the location-based method required by China's national guidelines?
