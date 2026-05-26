<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { getLatestBlocks, getStatus, getTransactions, queryTransaction } from '../../api/blockchain'
import PageContainer from '../../components/PageContainer.vue'

const { t } = useI18n()

const activeTab = ref('blocks')

const chainStatus = ref<Record<string, unknown> | null>(null)
const statusLoading = ref(false)
const txHashQuery = ref('')
const txQueryLoading = ref(false)
const txQueryResult = ref<Record<string, unknown> | null>(null)
const txQueryError = ref('')

const blocks = ref([])
const blocksLoading = ref(false)
const blocksCurrentPage = ref(1)
const blocksPageSize = ref(10)
const blocksTotal = ref(0)

const transactions = ref([])
const transactionsLoading = ref(false)
const transactionsCurrentPage = ref(1)
const transactionsPageSize = ref(10)
const transactionsTotal = ref(0)

const parseRecord = (value: unknown): Record<string, unknown> | null => {
  if (!value) return null
  if (typeof value === 'string') {
    try {
      return JSON.parse(value) as Record<string, unknown>
    } catch {
      return { raw: value }
    }
  }
  if (typeof value === 'object') {
    return value as Record<string, unknown>
  }
  return { value }
}

const formatStatusValue = (value: unknown) => {
  if (typeof value === 'boolean') {
    return value ? t('blockchain.statusOnline') : t('blockchain.statusOffline')
  }
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  return String(value)
}

const statusTagType = (connected: unknown) => {
  return connected ? 'success' : 'danger'
}

const loadStatus = async () => {
  try {
    statusLoading.value = true
    chainStatus.value = parseRecord(await getStatus())
  } catch {
    chainStatus.value = null
    ElMessage.error(t('blockchain.loadStatusFailed'))
  } finally {
    statusLoading.value = false
  }
}

const loadBlocks = async () => {
  try {
    blocksLoading.value = true
    const result = await getLatestBlocks({
      pageNum: blocksCurrentPage.value,
      pageSize: blocksPageSize.value,
    })
    blocks.value = result?.items || []
    blocksTotal.value = result?.total || 0
  } catch {
    ElMessage.error(t('blockchain.loadBlocksFailed'))
  } finally {
    blocksLoading.value = false
  }
}

const loadTransactions = async () => {
  try {
    transactionsLoading.value = true
    const result = await getTransactions({
      pageNum: transactionsCurrentPage.value,
      pageSize: transactionsPageSize.value,
    })
    transactions.value = result?.items || []
    transactionsTotal.value = result?.total || 0
  } catch {
    ElMessage.error(t('blockchain.loadTxFailed'))
  } finally {
    transactionsLoading.value = false
  }
}

const onBlocksSizeChange = (size) => {
  blocksPageSize.value = size
  blocksCurrentPage.value = 1
  loadBlocks()
}

const onBlocksCurrentChange = (page) => {
  blocksCurrentPage.value = page
  loadBlocks()
}

const onTransactionsSizeChange = (size) => {
  transactionsPageSize.value = size
  transactionsCurrentPage.value = 1
  loadTransactions()
}

const onTransactionsCurrentChange = (page) => {
  transactionsCurrentPage.value = page
  loadTransactions()
}

const getBlockTypeTag = (type) => {
  const map = {
    'GENESIS': 'danger',
    'REGULAR': 'primary',
    'REWARD': 'success',
  }
  return map[type] || 'info'
}

const getBlockTypeText = (type) => {
  const map = {
    'GENESIS': t('blockchain.blockTypeGenesis'),
    'REGULAR': t('blockchain.blockTypeRegular'),
    'REWARD': t('blockchain.blockTypeReward'),
  }
  return map[type] || type
}

const getTransactionStatusTag = (status) => {
  const map = {
    'PENDING': 'warning',
    'CONFIRMED': 'success',
    'VALID': 'success',
    'FAILED': 'danger',
  }
  return map[status] || 'info'
}

const getTransactionStatusText = (status) => {
  const map = {
    'PENDING': t('blockchain.txStatusPending'),
    'CONFIRMED': t('blockchain.txStatusConfirmed'),
    'VALID': t('blockchain.txStatusValidated'),
    'FAILED': t('blockchain.txStatusFailed'),
  }
  return map[status] || status
}

const submitTxQuery = async () => {
  if (!txHashQuery.value.trim()) {
    txQueryError.value = t('blockchain.queryTxRequired')
    txQueryResult.value = null
    return
  }

  try {
    txQueryLoading.value = true
    txQueryError.value = ''
    const result = await queryTransaction(txHashQuery.value.trim())
    txQueryResult.value = parseRecord(result)
  } catch (error) {
    txQueryResult.value = null
    txQueryError.value = error instanceof Error ? error.message : t('blockchain.queryTxFailed')
  } finally {
    txQueryLoading.value = false
  }
}

onMounted(() => {
  loadStatus()
  loadBlocks()
  loadTransactions()
})
</script>

<template>
  <PageContainer :title="t('blockchain.title')" :description="t('blockchain.description')">
    <section class="blockchain-page">
      <el-card class="section-card" shadow="never">
        <div class="status-query-grid">
          <div class="status-panel">
            <div class="panel-header">
              <div>
                <h3 class="panel-title">{{ t('blockchain.statusTitle') }}</h3>
                <p class="panel-subtitle">{{ t('blockchain.statusDescription') }}</p>
              </div>
              <el-tag
                class="status-tag"
                :type="statusTagType(chainStatus?.connected)"
                data-testid="blockchain-status-tag"
              >
                {{ formatStatusValue(chainStatus?.connected) }}
              </el-tag>
            </div>

            <div v-loading="statusLoading" class="status-metrics">
              <div class="status-metric">
                <span class="status-label">{{ t('blockchain.statusMode') }}</span>
                <strong>{{ formatStatusValue(chainStatus?.mode) }}</strong>
              </div>
              <div class="status-metric">
                <span class="status-label">{{ t('blockchain.statusChannel') }}</span>
                <strong>{{ formatStatusValue(chainStatus?.channel) }}</strong>
              </div>
              <div class="status-metric">
                <span class="status-label">{{ t('blockchain.statusPeers') }}</span>
                <strong>{{ formatStatusValue(chainStatus?.peers) }}</strong>
              </div>
              <div class="status-metric">
                <span class="status-label">{{ t('blockchain.statusOrderers') }}</span>
                <strong>{{ formatStatusValue(chainStatus?.orderers) }}</strong>
              </div>
            </div>
          </div>

          <div class="query-panel">
            <div class="panel-header">
              <div>
                <h3 class="panel-title">{{ t('blockchain.queryTitle') }}</h3>
                <p class="panel-subtitle">{{ t('blockchain.queryDescription') }}</p>
              </div>
            </div>

            <div class="query-form">
              <el-input
                v-model="txHashQuery"
                data-testid="blockchain-tx-query-input"
                :placeholder="t('blockchain.queryPlaceholder')"
                clearable
                @keyup.enter="submitTxQuery"
              />
              <el-button
                type="primary"
                :loading="txQueryLoading"
                data-testid="blockchain-tx-query-submit"
                @click="submitTxQuery"
              >
                {{ t('blockchain.queryAction') }}
              </el-button>
            </div>

            <el-alert
              v-if="txQueryError"
              class="query-alert"
              type="error"
              :closable="false"
              :title="txQueryError"
            />

            <div
              v-if="txQueryResult"
              class="query-result"
              data-testid="blockchain-tx-query-result"
            >
              <div class="query-result-row">
                <span class="status-label">{{ t('blockchain.colTxHash') }}</span>
                <strong>{{ formatStatusValue(txQueryResult.txHash) }}</strong>
              </div>
              <div class="query-result-row">
                <span class="status-label">{{ t('blockchain.colTxStatus') }}</span>
                <el-tag :type="getTransactionStatusTag(String(txQueryResult.status || ''))">
                  {{ getTransactionStatusText(String(txQueryResult.status || '-')) }}
                </el-tag>
              </div>
              <div class="query-result-row">
                <span class="status-label">{{ t('blockchain.colTimestamp') }}</span>
                <strong>{{ formatStatusValue(txQueryResult.timestamp) }}</strong>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="section-card" shadow="never">
        <el-tabs v-model="activeTab" class="blockchain-tabs">
          <el-tab-pane :label="t('blockchain.tabBlocks')" name="blocks">
            <el-table :data="blocks" border v-loading="blocksLoading">
              <el-table-column :label="t('common.operation')" width="80">
                <template #default="scope">
                  {{ (blocksCurrentPage - 1) * blocksPageSize + scope.$index + 1 }}
                </template>
              </el-table-column>
              <el-table-column prop="blockNumber" :label="t('blockchain.colBlockHeight')" min-width="120" />
              <el-table-column prop="blockHash" :label="t('blockchain.colBlockHash')" min-width="200" show-overflow-tooltip />
              <el-table-column prop="blockType" :label="t('blockchain.colBlockType')" min-width="100">
                <template #default="{ row }">
                  <el-tag :type="getBlockTypeTag(row.blockType)">
                    {{ getBlockTypeText(row.blockType) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="txCount" :label="t('blockchain.colTxCount')" min-width="100" />
              <el-table-column prop="miner" :label="t('blockchain.colMinerAddress')" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">{{ row.miner || '-' }}</template>
              </el-table-column>
              <el-table-column prop="timestamp" :label="t('blockchain.colTimestamp')" min-width="170" />
            </el-table>

            <div class="pagination-row">
              <el-pagination
                v-model:current-page="blocksCurrentPage"
                v-model:page-size="blocksPageSize"
                background
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next, jumper"
                :total="blocksTotal"
                @size-change="onBlocksSizeChange"
                @current-change="onBlocksCurrentChange"
              />
            </div>
          </el-tab-pane>

          <el-tab-pane :label="t('blockchain.tabTransactions')" name="transactions">
            <el-table :data="transactions" border v-loading="transactionsLoading">
              <el-table-column :label="t('common.operation')" width="80">
                <template #default="scope">
                  {{ (transactionsCurrentPage - 1) * transactionsPageSize + scope.$index + 1 }}
                </template>
              </el-table-column>
              <el-table-column prop="txHash" :label="t('blockchain.colTxHash')" min-width="200" show-overflow-tooltip />
              <el-table-column prop="blockNumber" :label="t('blockchain.colBlockHeight')" min-width="120" />
              <el-table-column prop="fromAddress" :label="t('blockchain.colSender')" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">{{ row.fromAddress || '-' }}</template>
              </el-table-column>
              <el-table-column prop="toAddress" :label="t('blockchain.colReceiver')" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">{{ row.toAddress || '-' }}</template>
              </el-table-column>
              <el-table-column prop="amount" :label="t('blockchain.colAmount')" min-width="120">
                <template #default="{ row }">{{ row.amount ?? '-' }}</template>
              </el-table-column>
              <el-table-column prop="status" :label="t('blockchain.colTxStatus')" min-width="100">
                <template #default="{ row }">
                  <el-tag :type="getTransactionStatusTag(row.status)">
                    {{ getTransactionStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="timestamp" :label="t('blockchain.colTimestamp')" min-width="170" />
            </el-table>

            <div class="pagination-row">
              <el-pagination
                v-model:current-page="transactionsCurrentPage"
                v-model:page-size="transactionsPageSize"
                background
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next, jumper"
                :total="transactionsTotal"
                @size-change="onTransactionsSizeChange"
                @current-change="onTransactionsCurrentChange"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </section>
  </PageContainer>
</template>

<style scoped>
.blockchain-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.status-query-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(280px, 1fr));
  gap: 18px;
}

.section-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
}

.status-panel,
.query-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.panel-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.panel-subtitle {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.status-tag {
  flex-shrink: 0;
}

.status-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(120px, 1fr));
  gap: 12px;
}

.status-metric,
.query-result-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  border-radius: 10px;
  background: var(--el-fill-color-light);
}

.status-label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.query-form {
  display: flex;
  gap: 10px;
}

.query-alert {
  margin-top: 4px;
}

.query-result {
  display: grid;
  gap: 10px;
}

.blockchain-tabs {
  padding: 0 10px;
}

.pagination-row {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .status-query-grid {
    grid-template-columns: 1fr;
  }

  .status-metrics {
    grid-template-columns: 1fr 1fr;
  }

  .query-form {
    flex-direction: column;
  }
}
</style>
