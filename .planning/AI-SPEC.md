## 1b. Domain Context

**Industry Vertical:** China Carbon Emissions Trading System (全国碳排放权交易市场)
**User Population:** Carbon-emitting enterprises (power generation, grid), government reviewers, third-party regulatory inspectors, system administrators
**Stakes Level:** Critical
**Output Consequence:** Emission reports determine enterprise carbon allowance compliance; incorrect calculations expose enterprises to regulatory penalties (up to 5x allowance value per MEE enforcement rules) and directly affect carbon trading eligibility and credit scores. Verified reports are committed to blockchain for tamper-proof audit trails.

### What Domain Experts Evaluate Against

**Dimension: Formula fidelity to official accounting guidelines**
Good: Calculation uses the exact 25-parameter formula for power generation enterprises from the NDRC/MEE guideline (E = FC_i x NCV_i x CC_i x OF_i x 44/12 + AD_electric x EF_grid), with each parameter mapped to a named input field and unit-verified before multiplication
Bad: Calculation uses generic scope1/2/3 aggregation (activity_data x emission_factor) without distinguishing fossil fuel combustion sub-formula structure, carbon oxidation rate, or the 44/12 molecular weight conversion
Stakes: Critical
Source: NDRC Guideline for GHG Emission Accounting of Power Generation Enterprises (试行) -- formula mismatch invalidates the entire report for regulatory submission; MEE 2022 revision tightened default values and added desulfurization emission terms

**Dimension: Parameter unit consistency before arithmetic**
Good: Fuel consumption entered in tonnes, NCV in GJ/tonne, CC in tC/GJ, OF as a decimal (0-1), purchased electricity in MWh, grid EF in tCO2/MWh -- each parameter validated against its expected unit and range before the formula is applied
Bad: System accepts any numeric value without unit enforcement; a fuel consumption entered in kg (not tonnes) silently produces a 1000x underestimate, or purchased electricity in kWh (not MWh) produces a 1000x overestimate
Stakes: Critical
Source: Production deployment experience across China ETS reporting platforms -- unit mismatch is the single most common data quality failure, cited in MEE enforcement reports as the leading cause of report rejection and penalty

**Dimension: Emission factor provenance and traceability**
Good: Each emission factor carries a source tag (default value from MEE table vs. enterprise-measured vs. regional grid factor) and an effective date; calculations record which factor version was used so reviewers can audit
Bad: Emission factors are hardcoded constants (CachePreloadService.redisTemplate.opsForHash) with no provenance metadata, no version, no regional differentiation, and no update mechanism beyond backend restart
Stakes: High
Source: MEE emission factor tables update annually (e.g., grid EF changed from 0.5810 to 0.5839 tCO2/MWh for North China region); the 2023 reporting guideline revision replaced NDRC defaults with MEE defaults -- unversioned factors produce non-compliant reports

**Dimension: Report lifecycle regulatory compliance**
Good: Report status machine matches MEE requirements (draft -> submitted -> under review -> approved/rejected -> on-chain); approved reports trigger cascading compliance actions (credit score, emission rating, blockchain commitment); rejected reports allow correction and resubmission
Bad: Status transitions allow illegal jumps (e.g., DRAFT directly to APPROVED) or lack reviewer identity recording; approved reports skip blockchain commitment, breaking the tamper-proof audit requirement
Stakes: High
Source: MEE carbon emission reporting regulation (碳排放权交易管理办法) requires full MRV lifecycle with verification before allowance settlement; data not committed to chain cannot serve as legal evidence in penalty disputes

**Dimension: Industry-specific calculation model selection**
Good: System detects enterprise industry type (power generation vs. grid enterprise) from the enterprise.industry field and routes to the correct accounting service (PowerGenerationAccountingService with 25 inputs, or GridEnterpriseAccountingService with 9 inputs); frontend renders the corresponding parameter form
Bad: All enterprises use the same generic scope1/2/3 JSON input regardless of industry; power generation enterprises cannot enter carbon oxidation rate or desulfurization parameters; grid enterprises cannot enter SF6 capacity/recovery data
Stakes: Critical
Source: GB/T 32151.1 (power generation) and GB/T 32151.2 (grid enterprise) define separate parameter sets and formulas -- using the wrong model produces a structurally invalid report that reviewers will reject immediately

### Known Failure Modes in This Domain

1. **Unit-scale silent explosion**: Fuel consumption entered in kg instead of tonnes, or electricity in kWh instead of MWh, causes 1000x errors that pass arithmetic validation but produce wildly wrong totals. The current code (CarbonService.calculateEmissions line 259) parses numeric values from JSON without unit enforcement. In China ETS enforcement, this is the top reason for report rejection and the basis for fraud penalties.

2. **Formula structural mismatch for regulatory submission**: The current generic formula (activity x factor per scope) does not match the official 25-parameter or 9-parameter structure. A reviewer comparing the system output against the MEE guideline template will find missing terms (desulfurization emissions for power generation, SF6 fugitive emissions for grid enterprises, the 44/12 carbon-to-CO2 conversion). Reports generated with the generic formula cannot pass formal verification.

3. **Emission factor version drift**: Hardcoded factors in CachePreloadService (electricity_grid=0.5839, coal=2.6600) are single flat values with no regional, temporal, or fuel-grade variation. The MEE grid emission factor varies by region (6 regional grids with different EFs) and was revised in 2022-2023. A report using stale factors fails cross-verification against MEE's published factor tables for the reporting year.

4. **Carbon oxidation rate omission**: The current scope1 calculation multiplies activity by emission_factor directly, omitting the carbon oxidation rate (OF) parameter that is mandatory in the official formula. For coal combustion, the default OF value matters (93%-98% depending on coal type) and represents real carbon loss in ash. Omitting it systematically underestimates scope1 emissions.

### Regulatory / Compliance Context

- **NDRC/MEE Guidelines**: 《中国发电企业温室气体排放核算方法与报告指南(试行)》 and 《中国电网企业温室气体排放核算方法与报告指南(试行)》 -- define mandatory formulas, parameter definitions, and default values
- **GB/T 32150-2015**: General rules for industrial enterprise GHG emission quantification and reporting -- establishes scope boundary and terminology
- **GB/T 32151.1 / 32151.2**: Industry-specific standards for power generation and grid enterprises respectively -- define exact parameter sets (25 and 9)
- **MEE碳排放权交易管理办法 (2021, revised 2023)**: Carbon ETS management regulation -- requires MRV (monitoring, reporting, verification) lifecycle; mandates data quality with penalties up to 5x allowance value for misreporting
- **MEE Emission Factor Tables**: Published annually with regional grid emission factors (6 regions) and fuel-specific default CC/NCV/OF values; must be used as defaults unless enterprise has measured values with documented methodology

### Domain Expert Roles for Evaluation

| Role | Responsibility in Eval |
|------|----------------------|
| Carbon accounting specialist (碳核算专员) | Verify formula fidelity against official guidelines; validate parameter definitions and default values; label reference dataset for correct vs. incorrect calculation outputs |
| MEE regulatory reviewer (审核员) | Calibrate rubric for report lifecycle compliance; define acceptable vs. unacceptable status transitions; verify emission factor provenance requirements |
| Emission factor data steward | Maintain factor version catalog; validate that system uses correct regional factors for each reporting year; verify factor update propagation |
| Enterprise carbon manager (企业碳管理人员) | Provide real-world input scenarios with known expected outputs; identify edge cases from actual reporting experience (unit conversion errors, multi-fuel mixing, partial-year reporting) |
| Financial settlement auditor | Verify that emission totals correctly flow into allowance calculations and trading eligibility; validate credit score and emission rating cascading after report approval |

### Research Sources
- NDRC/MEE Guidelines for GHG Emission Accounting of Power Generation Enterprises (试行) -- 25-parameter formula definition
- NDRC/MEE Guidelines for GHG Emission Accounting of Grid Enterprises (试行) -- 9-parameter formula definition
- GB/T 32150-2015 (General rules for industrial enterprise GHG quantification and reporting)
- GB/T 32151.1 (Power generation organizations) and GB/T 32151.2 (Grid organizations)
- MEE Carbon ETS Management Regulation (碳排放权交易管理办法, 2021, revised 2023)
- Project gap analysis (GAP-05: industry-specific formulas not implemented)
- Product specification document Section 4.2 (carbon accounting module with formula definitions)
- Current CarbonService.calculateEmissions() implementation (generic scope1/2/3 aggregation)
- CachePreloadService emission factor hardcoded values (no provenance, no versioning)