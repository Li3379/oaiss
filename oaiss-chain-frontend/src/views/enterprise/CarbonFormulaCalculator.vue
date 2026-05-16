<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { calculatePowerGeneration, calculatePowerGrid } from '../../api/carbonFormula'
import type { PowerGenerationCalculationResponse, PowerGridCalculationResponse } from '../../types/carbonFormula'
import PageContainer from '../../components/PageContainer.vue'

const { t } = useI18n()

function createEmptyFuelParams() {
  return { fc: null, ncv: null, cc: null, of: null } as { fc: number | null; ncv: number | null; cc: number | null; of: number | null }
}

const activeTab = ref('powerGeneration')

// Power Generation form
const pgForm = ref({
  rawCoal: createEmptyFuelParams(),
  cleanedCoal: createEmptyFuelParams(),
  otherWashedCoal: createEmptyFuelParams(),
  briquette: createEmptyFuelParams(),
  otherCoal: createEmptyFuelParams(),
  carbonateConsumed: null as number | null,
  desulfEmissionFactor: null as number | null,
  desulfConversionRate: null as number | null,
  reportingYear: new Date().getFullYear(),
  enterpriseName: '',
})
const pgLoading = ref(false)
const pgResult = ref<PowerGenerationCalculationResponse | null>(null)

const fuelSections = [
  { key: 'rawCoal' as const, labelKey: 'carbonFormula.rawCoal' },
  { key: 'cleanedCoal' as const, labelKey: 'carbonFormula.cleanedCoal' },
  { key: 'otherWashedCoal' as const, labelKey: 'carbonFormula.otherWashedCoal' },
  { key: 'briquette' as const, labelKey: 'carbonFormula.briquette' },
  { key: 'otherCoal' as const, labelKey: 'carbonFormula.otherCoal' },
]

const onCalculatePowerGeneration = async () => {
  try {
    pgLoading.value = true
    const f = pgForm.value
    const payload = {
      rawCoalFc: f.rawCoal.fc,
      rawCoalNcv: f.rawCoal.ncv,
      rawCoalCc: f.rawCoal.cc,
      rawCoalOf: f.rawCoal.of,
      cleanedCoalFc: f.cleanedCoal.fc,
      cleanedCoalNcv: f.cleanedCoal.ncv,
      cleanedCoalCc: f.cleanedCoal.cc,
      cleanedCoalOf: f.cleanedCoal.of,
      otherWashedCoalFc: f.otherWashedCoal.fc,
      otherWashedCoalNcv: f.otherWashedCoal.ncv,
      otherWashedCoalCc: f.otherWashedCoal.cc,
      otherWashedCoalOf: f.otherWashedCoal.of,
      briquetteFc: f.briquette.fc,
      briquetteNcv: f.briquette.ncv,
      briquetteCc: f.briquette.cc,
      briquetteOf: f.briquette.of,
      otherCoalFc: f.otherCoal.fc,
      otherCoalNcv: f.otherCoal.ncv,
      otherCoalCc: f.otherCoal.cc,
      otherCoalOf: f.otherCoal.of,
      carbonateConsumed: f.carbonateConsumed,
      desulfEmissionFactor: f.desulfEmissionFactor,
      desulfConversionRate: f.desulfConversionRate,
      reportingYear: typeof f.reportingYear === 'string'
        ? parseInt(f.reportingYear, 10)
        : f.reportingYear,
      enterpriseName: f.enterpriseName,
    }
    const res = await calculatePowerGeneration(payload)
    pgResult.value = res
    ElMessage.success(t('carbonFormula.calcSuccess'))
  } catch {
    ElMessage.error(t('carbonFormula.calcFailed'))
  } finally {
    pgLoading.value = false
  }
}

// Power Grid form
const gridForm = ref({
  transmissionVolume: null as number | null,
  lineLossRate: null as number | null,
  gridEmissionFactor: null as number | null,
  generationVolume: null as number | null,
  importedElectricity: null as number | null,
  exportedElectricity: null as number | null,
  importEmissionFactor: null as number | null,
  reportingYear: new Date().getFullYear(),
  enterpriseName: '',
})
const gridLoading = ref(false)
const gridResult = ref<PowerGridCalculationResponse | null>(null)

const onCalculatePowerGrid = async () => {
  try {
    gridLoading.value = true
    const payload = {
      ...gridForm.value,
      reportingYear: typeof gridForm.value.reportingYear === 'string'
        ? parseInt(gridForm.value.reportingYear, 10)
        : gridForm.value.reportingYear,
    }
    const res = await calculatePowerGrid(payload)
    gridResult.value = res
    ElMessage.success(t('carbonFormula.calcSuccess'))
  } catch {
    ElMessage.error(t('carbonFormula.calcFailed'))
  } finally {
    gridLoading.value = false
  }
}
</script>

<template>
  <PageContainer :title="t('carbonFormula.title')" :description="t('carbonFormula.description')">
    <section class="formula-page">
      <el-card class="section-card" shadow="never">
        <el-tabs v-model="activeTab">
          <!-- Power Generation Tab -->
          <el-tab-pane :label="t('carbonFormula.tabPowerGeneration')" name="powerGeneration">
            <el-form label-width="160px" style="max-width: 900px; padding: 16px 0">
              <!-- Metadata -->
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item :label="t('carbonFormula.reportingYear')">
                    <el-date-picker
                      v-model="pgForm.reportingYear"
                      type="year"
                      value-format="YYYY"
                      :placeholder="t('carbonFormula.selectYear')"
                      style="width: 100%"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item :label="t('carbonFormula.enterpriseName')">
                    <el-input v-model="pgForm.enterpriseName" :placeholder="t('carbonFormula.enterEnterpriseName')" />
                  </el-form-item>
                </el-col>
              </el-row>

              <!-- Fuel sections -->
              <el-collapse>
                <el-collapse-item v-for="section in fuelSections" :key="section.key" :title="t(section.labelKey)">
                  <el-row :gutter="20">
                    <el-col :span="12">
                      <el-form-item :label="t('carbonFormula.fc')">
                        <el-input-number v-model="pgForm[section.key].fc" :min="0" :precision="4" :controls="false" :placeholder="t('carbonFormula.fcUnit')" style="width: 100%" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item :label="t('carbonFormula.ncv')">
                        <el-input-number v-model="pgForm[section.key].ncv" :min="0" :precision="4" :controls="false" :placeholder="t('carbonFormula.ncvUnit')" style="width: 100%" />
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-row :gutter="20">
                    <el-col :span="12">
                      <el-form-item :label="t('carbonFormula.cc')">
                        <el-input-number v-model="pgForm[section.key].cc" :min="0" :precision="6" :controls="false" :placeholder="t('carbonFormula.ccUnit')" style="width: 100%" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item :label="t('carbonFormula.of')">
                        <el-input-number v-model="pgForm[section.key].of" :min="0" :max="1" :precision="4" :step="0.01" :controls="false" :placeholder="t('carbonFormula.ofUnit')" style="width: 100%" />
                      </el-form-item>
                    </el-col>
                  </el-row>
                </el-collapse-item>
              </el-collapse>

              <!-- Desulfurization -->
              <el-card class="section-card" shadow="never" style="margin-top: 16px">
                <template #header>
                  <span>{{ t('carbonFormula.desulfurization') }}</span>
                </template>
                <el-row :gutter="20">
                  <el-col :span="8">
                    <el-form-item :label="t('carbonFormula.carbonateConsumed')">
                      <el-input-number v-model="pgForm.carbonateConsumed" :min="0" :precision="4" :controls="false" :placeholder="t('carbonFormula.carbonateConsumedUnit')" style="width: 100%" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item :label="t('carbonFormula.desulfEmissionFactor')">
                      <el-input-number v-model="pgForm.desulfEmissionFactor" :min="0" :precision="6" :controls="false" :placeholder="t('carbonFormula.desulfEmissionFactorUnit')" style="width: 100%" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item :label="t('carbonFormula.desulfConversionRate')">
                      <el-input-number v-model="pgForm.desulfConversionRate" :min="0" :max="1" :precision="4" :step="0.01" :controls="false" :placeholder="t('carbonFormula.desulfConversionRateUnit')" style="width: 100%" />
                    </el-form-item>
                  </el-col>
                </el-row>
              </el-card>

              <el-form-item style="margin-top: 20px">
                <el-button type="primary" :loading="pgLoading" @click="onCalculatePowerGeneration">
                  {{ t('carbonFormula.calculate') }}
                </el-button>
              </el-form-item>
            </el-form>

            <!-- Results -->
            <template v-if="pgResult">
              <el-card class="section-card" shadow="never" style="margin-top: 20px">
                <template #header>
                  <span>{{ t('carbonFormula.resultTitle') }}</span>
                </template>
                <el-descriptions :column="2" border>
                  <el-descriptions-item :label="t('carbonFormula.totalEmission')">
                    {{ pgResult.totalEmission }} tCO2e
                  </el-descriptions-item>
                  <el-descriptions-item :label="t('carbonFormula.combustionEmission')">
                    {{ pgResult.combustionEmission }} tCO2e
                  </el-descriptions-item>
                  <el-descriptions-item :label="t('carbonFormula.desulfurizationEmission')">
                    {{ pgResult.desulfurizationEmission }} tCO2e
                  </el-descriptions-item>
                  <el-descriptions-item :label="t('carbonFormula.formulaReference')">
                    {{ pgResult.formulaReference }}
                  </el-descriptions-item>
                </el-descriptions>

                <el-table :data="pgResult.fuelDetails" border style="margin-top: 16px">
                  <el-table-column prop="fuelType" :label="t('carbonFormula.fuelType')" min-width="120" />
                  <el-table-column prop="fuelConsumption" :label="t('carbonFormula.fuelConsumption')" min-width="120" />
                  <el-table-column prop="netCalorificValue" :label="t('carbonFormula.netCalorificValue')" min-width="140" />
                  <el-table-column prop="carbonContent" :label="t('carbonFormula.carbonContent')" min-width="120" />
                  <el-table-column prop="oxidationRate" :label="t('carbonFormula.oxidationRate')" min-width="120" />
                  <el-table-column prop="emission" :label="t('carbonFormula.emission')" min-width="120" />
                </el-table>
              </el-card>
            </template>
          </el-tab-pane>

          <!-- Power Grid Tab -->
          <el-tab-pane :label="t('carbonFormula.tabPowerGrid')" name="powerGrid">
            <el-form label-width="180px" style="max-width: 900px; padding: 16px 0">
              <!-- Metadata -->
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item :label="t('carbonFormula.reportingYear')">
                    <el-date-picker
                      v-model="gridForm.reportingYear"
                      type="year"
                      value-format="YYYY"
                      :placeholder="t('carbonFormula.selectYear')"
                      style="width: 100%"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item :label="t('carbonFormula.enterpriseName')">
                    <el-input v-model="gridForm.enterpriseName" :placeholder="t('carbonFormula.enterEnterpriseName')" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item :label="t('carbonFormula.transmissionVolume')">
                    <el-input-number v-model="gridForm.transmissionVolume" :min="0" :precision="4" :controls="false" :placeholder="t('carbonFormula.transmissionVolumeUnit')" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item :label="t('carbonFormula.lineLossRate')">
                    <el-input-number v-model="gridForm.lineLossRate" :min="0" :max="1" :precision="4" :step="0.01" :controls="false" :placeholder="t('carbonFormula.lineLossRateUnit')" style="width: 100%" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item :label="t('carbonFormula.gridEmissionFactor')">
                    <el-input-number v-model="gridForm.gridEmissionFactor" :min="0" :precision="6" :controls="false" :placeholder="t('carbonFormula.gridEmissionFactorUnit')" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item :label="t('carbonFormula.generationVolume')">
                    <el-input-number v-model="gridForm.generationVolume" :min="0" :precision="4" :controls="false" :placeholder="t('carbonFormula.generationVolumeUnit')" style="width: 100%" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item :label="t('carbonFormula.importedElectricity')">
                    <el-input-number v-model="gridForm.importedElectricity" :min="0" :precision="4" :controls="false" :placeholder="t('carbonFormula.importedElectricityUnit')" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item :label="t('carbonFormula.exportedElectricity')">
                    <el-input-number v-model="gridForm.exportedElectricity" :min="0" :precision="4" :controls="false" :placeholder="t('carbonFormula.exportedElectricityUnit')" style="width: 100%" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item :label="t('carbonFormula.importEmissionFactor')">
                    <el-input-number v-model="gridForm.importEmissionFactor" :min="0" :precision="6" :controls="false" :placeholder="t('carbonFormula.importEmissionFactorUnit')" style="width: 100%" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item style="margin-top: 20px">
                <el-button type="primary" :loading="gridLoading" @click="onCalculatePowerGrid">
                  {{ t('carbonFormula.calculate') }}
                </el-button>
              </el-form-item>
            </el-form>

            <!-- Results -->
            <template v-if="gridResult">
              <el-card class="section-card" shadow="never" style="margin-top: 20px">
                <template #header>
                  <span>{{ t('carbonFormula.resultTitle') }}</span>
                </template>
                <el-descriptions :column="2" border>
                  <el-descriptions-item :label="t('carbonFormula.totalEmission')">
                    {{ gridResult.totalEmission }} tCO2e
                  </el-descriptions-item>
                  <el-descriptions-item :label="t('carbonFormula.transmissionLossEmission')">
                    {{ gridResult.transmissionLossEmission }} tCO2e
                  </el-descriptions-item>
                  <el-descriptions-item :label="t('carbonFormula.importedEmission')">
                    {{ gridResult.importedEmission }} tCO2e
                  </el-descriptions-item>
                  <el-descriptions-item :label="t('carbonFormula.transmissionLoss')">
                    {{ gridResult.transmissionLoss }} MWh
                  </el-descriptions-item>
                  <el-descriptions-item :label="t('carbonFormula.formulaReference')">
                    {{ gridResult.formulaReference }}
                  </el-descriptions-item>
                </el-descriptions>
              </el-card>
            </template>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </section>
  </PageContainer>
</template>

<style scoped>
.formula-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
}
</style>
