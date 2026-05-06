<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { getLatestBlocks, getTransactions } from '../../api/blockchain'
import PageContainer from '../../components/PageContainer.vue'

const { t } = useI18n()

const activeTab = ref('blocks')

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

const loadBlocks = async () => {
  try {
    blocksLoading.value = true
    const result = await getLatestBlocks({
      pageNum: blocksCurrentPage.value,
      pageSize: blocksPageSize.value,
    })
    blocks.value = result?.items || []
    blocksTotal.value = result?.total || 0
  } catch (error) {
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
  } catch (error) {
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
    'FAILED': 'danger',
  }
  return map[status] || 'info'
}

const getTransactionStatusText = (status) => {
  const map = {
    'PENDING': t('blockchain.txStatusPending'),
    'CONFIRMED': t('blockchain.txStatusConfirmed'),
    'FAILED': t('blockchain.txStatusFailed'),
  }
  return map[status] || status
}

onMounted(() => {
  loadBlocks()
  loadTransactions()
})
</script>

<template>
  <PageContainer :title="t('blockchain.title')" :description="t('blockchain.description')">
    <section class="blockchain-page">
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
              <el-table-column prop="transactionCount" :label="t('blockchain.colTxCount')" min-width="100" />
              <el-table-column prop="miner" :label="t('blockchain.colMinerAddress')" min-width="180" show-overflow-tooltip />
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
              <el-table-column prop="fromAddress" :label="t('blockchain.colSender')" min-width="180" show-overflow-tooltip />
              <el-table-column prop="toAddress" :label="t('blockchain.colReceiver')" min-width="180" show-overflow-tooltip />
              <el-table-column prop="amount" :label="t('blockchain.colAmount')" min-width="120" />
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

.section-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
}

.blockchain-tabs {
  padding: 0 10px;
}

.pagination-row {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
