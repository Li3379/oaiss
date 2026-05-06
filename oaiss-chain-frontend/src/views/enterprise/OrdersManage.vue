<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyTrades, cancelTrade } from '../../api/trade'

const { t } = useI18n()

const searchForm = reactive({
  tradeNo: '',
  tradeType: '',
  dateRange: [],
})

const tradeTypeOptions = [
  { label: 'ordersManage.tradeTypeAuction', value: 1 },
  { label: 'ordersManage.tradeTypeP2P', value: 2 },
]

const tableData = ref([])
const loading = ref(false)
const selectedRows = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const detailDialogVisible = ref(false)
const currentDetailTrade = ref(null)

const loadTrades = async () => {
  try {
    loading.value = true
    const data = await getMyTrades({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      tradeNo: searchForm.tradeNo || undefined,
      tradeType: searchForm.tradeType || undefined,
      startDate: searchForm.dateRange?.[0] || undefined,
      endDate: searchForm.dateRange?.[1] || undefined,
    })
    tableData.value = data?.items || []
    total.value = data?.total || 0
  } catch (error) {
    ElMessage.error(t('ordersManage.loadFailed'))
  } finally {
    loading.value = false
  }
}

const onQuery = () => {
  currentPage.value = 1
  loadTrades()
}

const onReset = () => {
  searchForm.tradeNo = ''
  searchForm.tradeType = ''
  searchForm.dateRange = []
  currentPage.value = 1
  loadTrades()
}

const onSelectionChange = (rows) => {
  selectedRows.value = rows
}

const onSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadTrades()
}

const onCurrentChange = (page) => {
  currentPage.value = page
  loadTrades()
}

const getStatusType = (status) => {
  const map = {
    0: 'warning',
    1: 'primary',
    2: 'success',
    3: 'info',
  }
  return map[status] || 'info'
}

const getCounterparty = (row) => {
  return row.buyerName || row.sellerName || '-'
}

const openDetail = (row) => {
  currentDetailTrade.value = row
  detailDialogVisible.value = true
}

const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm(t('ordersManage.confirmCancel'), t('ordersManage.cancelTrade'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })

    await cancelTrade(row.id)
    ElMessage.success(t('ordersManage.cancelSuccess'))
    loadTrades()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(t('ordersManage.cancelFailed'))
    }
  }
}

onMounted(() => {
  loadTrades()
})
</script>

<template>
  <section class="orders-page">
    <el-card class="section-card" shadow="never">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>{{ t('ordersManage.breadcrumbTrading') }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ t('ordersManage.breadcrumbOrders') }}</el-breadcrumb-item>
      </el-breadcrumb>
    </el-card>

    <el-card class="section-card" shadow="never">
      <el-form :inline="true" class="search-form">
        <el-form-item :label="t('ordersManage.colTradeNo')">
          <el-input v-model="searchForm.tradeNo" :placeholder="t('ordersManage.placeholderTradeNo')" clearable />
        </el-form-item>

        <el-form-item :label="t('ordersManage.colTradeType')">
          <el-select v-model="searchForm.tradeType" :placeholder="t('ordersManage.placeholderTradeType')" clearable style="width: 150px">
            <el-option v-for="item in tradeTypeOptions" :key="item.value" :label="t(item.label)" :value="item.value" />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('ordersManage.dateRange')">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            :range-separator="t('ordersManage.dateSeparator')"
            :start-placeholder="t('ordersManage.startDate')"
            :end-placeholder="t('ordersManage.endDate')"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="onQuery">{{ t('common.search') }}</el-button>
          <el-button @click="onReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="section-card" shadow="never">
      <div class="table-tip">{{ t('common.total', { count: selectedRows.length }) }}</div>
      <el-table :data="tableData" border v-loading="loading" :empty-text="t('ordersManage.emptyText')" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="56" />
        <el-table-column :label="t('tradingMarket.colIndex')" width="80">
          <template #default="scope">
            {{ (currentPage - 1) * pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="tradeNo" :label="t('ordersManage.colTradeNo')" min-width="180" />
        <el-table-column prop="tradeTypeText" :label="t('ordersManage.colTradeType')" min-width="100" />
        <el-table-column :label="t('ordersManage.colCounterparty')" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ getCounterparty(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="quantity" :label="t('ordersManage.colQuantity')" min-width="120" />
        <el-table-column prop="unitPrice" :label="t('ordersManage.colUnitPrice')" min-width="120" />
        <el-table-column prop="totalAmount" :label="t('ordersManage.colTotalAmount')" min-width="120" />
        <el-table-column prop="statusText" :label="t('ordersManage.colTradeStatus')" min-width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" :label="t('ordersManage.colCreateTime')" min-width="170" />
        <el-table-column :label="t('ordersManage.colOperation')" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ t('common.viewDetail') }}</el-button>
            <el-button v-if="row.status === 0" link type="danger" @click="handleCancel(row)">{{ t('ordersManage.btnCancel') }}</el-button>
          </template>
        </el-table-column>
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

    <el-dialog v-model="detailDialogVisible" :title="t('ordersManage.dialogDetail')" width="700px">
      <el-descriptions :column="2" border v-if="currentDetailTrade">
        <el-descriptions-item :label="t('ordersManage.colTradeNo')">{{ currentDetailTrade.tradeNo }}</el-descriptions-item>
        <el-descriptions-item :label="t('ordersManage.colTradeType')">{{ currentDetailTrade.tradeTypeText }}</el-descriptions-item>
        <el-descriptions-item :label="t('ordersManage.labelBuyer')">{{ currentDetailTrade.buyerName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('ordersManage.labelSeller')">{{ currentDetailTrade.sellerName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('ordersManage.colQuantity')">{{ currentDetailTrade.quantity }}</el-descriptions-item>
        <el-descriptions-item :label="t('ordersManage.colUnitPrice')">{{ currentDetailTrade.unitPrice }}</el-descriptions-item>
        <el-descriptions-item :label="t('ordersManage.colTotalAmount')">{{ currentDetailTrade.totalAmount }}</el-descriptions-item>
        <el-descriptions-item :label="t('ordersManage.colTradeStatus')">
          <el-tag :type="getStatusType(currentDetailTrade.status)">
            {{ currentDetailTrade.statusText }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('ordersManage.colCreateTime')" :span="2">{{ currentDetailTrade.createdAt }}</el-descriptions-item>
        <el-descriptions-item :label="t('ordersManage.labelRemark')" :span="2">{{ currentDetailTrade.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="currentDetailTrade.blockchainTxHash" :label="t('ordersManage.labelBlockchainHash')" :span="2">
          {{ currentDetailTrade.blockchainTxHash }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </section>
</template>

<style scoped>
.orders-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
}

.table-tip {
  margin-bottom: 10px;
  color: var(--text-secondary);
  font-size: 13px;
}

.pagination-row {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
