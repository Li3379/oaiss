<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getMyAccount, getTransactions } from '../../api/carbonCoin'
import PageContainer from '../../components/PageContainer.vue'

const { t } = useI18n()

const accountData = ref(null)
const accountLoading = ref(false)

const transactionData = ref([])
const transactionLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const loadAccount = async () => {
  try {
    accountLoading.value = true
    const result = await getMyAccount()
    accountData.value = result
  } catch (error) {
    ElMessage.error(t('carbonCoin.loadAccountFailed'))
  } finally {
    accountLoading.value = false
  }
}

const loadTransactions = async () => {
  try {
    transactionLoading.value = true
    const result = await getTransactions({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    })
    transactionData.value = result?.items || []
    total.value = result?.total || 0
  } catch (error) {
    ElMessage.error(t('carbonCoin.loadRecordFailed'))
  } finally {
    transactionLoading.value = false
  }
}

const onSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadTransactions()
}

const onCurrentChange = (page) => {
  currentPage.value = page
  loadTransactions()
}

const getTransactionTypeTag = (type) => {
  const map = {
    'RECHARGE': 'success',
    'TRANSFER_IN': 'success',
    'TRANSFER_OUT': 'danger',
    'PURCHASE': 'warning',
    'SALE': 'primary',
    'FROZEN': 'danger',
    'UNFROZEN': 'success',
  }
  return map[type] || 'info'
}

const getTransactionTypeText = (type) => {
  const map = {
    'RECHARGE': t('carbonCoin.txTypeRecharge'),
    'TRANSFER_IN': t('carbonCoin.txTypeTransferIn'),
    'TRANSFER_OUT': t('carbonCoin.txTypeTransferOut'),
    'PURCHASE': t('carbonCoin.txTypeBuy'),
    'SALE': t('carbonCoin.txTypeSell'),
    'FROZEN': t('carbonCoin.txTypeFreeze'),
    'UNFROZEN': t('carbonCoin.txTypeUnfreeze'),
  }
  return map[type] || type
}


const getAmountClass = (type) => {
  const positiveTypes = ['RECHARGE', 'TRANSFER_IN', 'SALE']
  return positiveTypes.includes(type) ? 'amount-positive' : 'amount-negative'
}

const formatAmount = (type, amount) => {
  const positiveTypes = ['RECHARGE', 'TRANSFER_IN', 'SALE']
  const prefix = positiveTypes.includes(type) ? '+' : ''
  return prefix + amount
}

onMounted(() => {
  loadAccount()
  loadTransactions()
})
</script>

<template>
  <PageContainer :title="t('carbonCoin.title')" :description="t('carbonCoin.description')">
    <section class="carbon-coin-page">
      <el-card class="section-card" shadow="never" v-loading="accountLoading">
        <template #header>
          <span class="card-header">{{ t('carbonCoin.accountInfo') }}</span>
        </template>
        <div v-if="accountData" class="account-info">
          <div class="info-card main-balance">
            <div class="info-label">{{ t('carbonCoin.balance') }}</div>
            <div class="info-value">{{ accountData.balance || 0 }}</div>
            <div class="info-unit">{{ t('carbonCoin.unit') }}</div>
          </div>
          <div class="info-card">
            <div class="info-label">{{ t('carbonCoin.frozenAmount') }}</div>
            <div class="info-value frozen">{{ accountData.frozenAmount || 0 }}</div>
            <div class="info-unit">{{ t('carbonCoin.unit') }}</div>
          </div>
          <div class="info-card">
            <div class="info-label">{{ t('carbonCoin.totalRecharge') }}</div>
            <div class="info-value">{{ accountData.totalRecharged || 0 }}</div>
            <div class="info-unit">{{ t('carbonCoin.unit') }}</div>
          </div>
          <div class="info-card">
            <div class="info-label">{{ t('carbonCoin.totalConsume') }}</div>
            <div class="info-value spent">{{ accountData.totalSpent || 0 }}</div>
            <div class="info-unit">{{ t('carbonCoin.unit') }}</div>
          </div>
        </div>
        <el-empty v-else :description="t('carbonCoin.emptyText')" />
      </el-card>

      <el-card class="section-card" shadow="never">
        <template #header>
          <span class="card-header">{{ t('carbonCoin.txRecord') }}</span>
        </template>
        <el-table :data="transactionData" border v-loading="transactionLoading">
          <el-table-column :label="t('common.total')" width="80">
            <template #default="scope">
              {{ (currentPage - 1) * pageSize + scope.$index + 1 }}
            </template>
          </el-table-column>
          <el-table-column prop="transactionNo" :label="t('carbonCoin.colTxNo')" min-width="180" />
          <el-table-column prop="transactionType" :label="t('carbonCoin.colTxType')" min-width="120">
            <template #default="{ row }">
              <el-tag :type="getTransactionTypeTag(row.transactionType)">
                {{ getTransactionTypeText(row.transactionType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="amount" :label="t('carbonCoin.colAmount')" min-width="120">
            <template #default="{ row }">
              <span :class="getAmountClass(row.transactionType)">
                {{ formatAmount(row.transactionType, row.amount) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="balance" :label="t('carbonCoin.colBalanceAfter')" min-width="120" />
          <el-table-column prop="remark" :label="t('carbonCoin.colRemark')" min-width="200" show-overflow-tooltip />
          <el-table-column prop="createdAt" :label="t('carbonCoin.colTime')" min-width="170" />
        </el-table>

        <div class="pagination-row">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            background
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="onSizeChange"
            @current-change="onCurrentChange"
          />
        </div>
      </el-card>
    </section>
  </PageContainer>
</template>

<style scoped>
.carbon-coin-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
}

.card-header {
  font-weight: 600;
  font-size: 16px;
}

.account-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  padding: 10px 0;
}

.info-card {
  background: var(--el-fill-color-light);
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  transition: transform 0.2s;
}

.info-card:hover {
  transform: translateY(-2px);
}

.info-card.main-balance {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.info-label {
  font-size: 14px;
  margin-bottom: 12px;
  opacity: 0.9;
}

.info-value {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 8px;
  line-height: 1;
}

.info-unit {
  font-size: 12px;
  opacity: 0.8;
}

.info-value.frozen {
  color: var(--el-color-danger);
}

.info-value.spent {
  color: var(--el-color-warning);
}

.amount-positive {
  color: var(--el-color-success);
  font-weight: 600;
}

.amount-negative {
  color: var(--el-color-danger);
  font-weight: 600;
}

.pagination-row {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
