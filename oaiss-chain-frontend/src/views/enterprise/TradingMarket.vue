<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { submitBuyOrder, submitSellOrder, getAuctionOrders, getMyOrders, getMatchResults } from '../../api/auction'

const { t } = useI18n()

const searchKeyword = ref('')
const selectedRows = ref([])

const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const formRef = ref()

const activeTab = ref('all')
const tableLoading = ref(false)

const formModel = reactive({
  direction: '',
  quantity: '',
  price: '',
})

const tableData = ref([])

const matchData = ref([])
const matchLoading = ref(false)
const matchPage = ref(1)
const matchPageSize = ref(10)
const matchTotal = ref(0)

const formRules = {
  direction: [{ required: true, message: t('tradingMarket.selectDirection'), trigger: 'change' }],
  quantity: [{ required: true, message: t('tradingMarket.enterQuantity'), trigger: 'blur' }],
  price: [{ required: true, message: t('tradingMarket.enterPrice'), trigger: 'blur' }],
}

const directionOptions = [
  { label: 'tradingMarket.buy', value: 1 },
  { label: 'tradingMarket.sell', value: 2 },
]

const fetchData = async () => {
  if (activeTab.value === 'match') {
    await fetchMatchResults()
    return
  }
  tableLoading.value = true
  try {
    const response = activeTab.value === 'my'
      ? await getMyOrders({ pageNum: page.value, pageSize: pageSize.value })
      : await getAuctionOrders({ pageNum: page.value, pageSize: pageSize.value })

    tableData.value = response.items || []
    total.value = response.total || 0
  } catch (error) {
    ElMessage.error(t('tradingMarket.fetchDataFailed'))
    tableData.value = []
    total.value = 0
  } finally {
    tableLoading.value = false
  }
}

const fetchMatchResults = async () => {
  matchLoading.value = true
  try {
    const result = await getMatchResults({
      pageNum: matchPage.value,
      pageSize: matchPageSize.value,
    })
    matchData.value = result?.items || []
    matchTotal.value = result?.total || 0
  } catch (error) {
    ElMessage.error(t('tradingMarket.fetchMatchFailed'))
    matchData.value = []
    matchTotal.value = 0
  } finally {
    matchLoading.value = false
  }
}

const onMatchSizeChange = (size) => {
  matchPageSize.value = size
  matchPage.value = 1
  fetchMatchResults()
}

const onMatchPageChange = (current) => {
  matchPage.value = current
  fetchMatchResults()
}

const onTabChange = () => {
  page.value = 1
  matchPage.value = 1
  fetchData()
}

const onQuery = () => {
  page.value = 1
  fetchData()
}

const onSelectionChange = (rows) => {
  selectedRows.value = rows
}

const onSizeChange = (size) => {
  pageSize.value = size
  page.value = 1
  fetchData()
}

const onPageChange = (current) => {
  page.value = current
  fetchData()
}

const resetForm = () => {
  formModel.direction = ''
  formModel.quantity = ''
  formModel.price = ''
}

const openAddDialog = () => {
  resetForm()
  dialogVisible.value = true
}

const getDirectionLabel = (direction) => {
  return direction === 1 ? t('tradingMarket.buy') : t('tradingMarket.sell')
}

const getDirectionType = (direction) => {
  return direction === 1 ? 'success' : 'danger'
}

const getStatusLabel = (status) => {
  const statusMap = {
    1: t('tradingMarket.statusPending'),
    2: t('tradingMarket.statusPartial'),
    3: t('tradingMarket.statusMatched'),
    4: t('tradingMarket.statusCancelled'),
  }
  return statusMap[status] || t('tradingMarket.statusUnknown')
}

const getStatusType = (status) => {
  const typeMap = {
    1: 'info',
    2: 'warning',
    3: 'success',
    4: 'info',
  }
  return typeMap[status] || 'info'
}

const onSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning(t('tradingMarket.incompleteForm'))
    return
  }

  try {
    const orderData = {
      direction: formModel.direction,
      quantity: parseFloat(formModel.quantity),
      price: parseFloat(formModel.price),
    }

    if (formModel.direction === 1) {
      await submitBuyOrder(orderData)
      ElMessage.success(t('tradingMarket.buySuccess'))
    } else {
      await submitSellOrder(orderData)
      ElMessage.success(t('tradingMarket.sellSuccess'))
    }

    dialogVisible.value = false
    fetchData()
  } catch (error) {
    ElMessage.error(t('tradingMarket.submitFailed'))
  }
}

const onCancelDialog = () => {
  dialogVisible.value = false
  ElMessage.info(t('common.cancelled'))
}

onMounted(() => {
  fetchData()
})
</script>
<template>
  <section class="market-page">
    <el-card class="section-card" shadow="never">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>{{ t('tradingMarket.breadcrumbTrading') }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ t('tradingMarket.breadcrumbAuction') }}</el-breadcrumb-item>
      </el-breadcrumb>
    </el-card>

    <el-card class="section-card" shadow="never">
      <div class="search-row">
        <el-input
          v-model="searchKeyword"
          :placeholder="t('tradingMarket.searchPlaceholder')"
          clearable
          class="search-input"
        />
        <el-button type="primary" @click="onQuery">{{ t('common.search') }}</el-button>
        <el-button type="success" plain @click="openAddDialog">{{ t('tradingMarket.dialogCreate') }}</el-button>
      </div>
    </el-card>

    <el-card class="section-card" shadow="never">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane :label="t('tradingMarket.tabAll')" name="all" />
        <el-tab-pane :label="t('tradingMarket.tabMine')" name="my" />
        <el-tab-pane :label="t('tradingMarket.tabMatch')" name="match" />
      </el-tabs>

      <div v-if="activeTab !== 'match'" class="table-tip">{{ t('common.total', { count: selectedRows.length }) }}</div>

      <el-table
        v-if="activeTab !== 'match'"
        :data="tableData"
        :loading="tableLoading"
        border
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="56" />
        <el-table-column :label="t('tradingMarket.colIndex')" width="80">
          <template #default="scope">
            {{ (page - 1) * pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column :label="t('tradingMarket.colDirection')" min-width="100">
          <template #default="{ row }">
            <el-tag :type="getDirectionType(row.direction)">
              {{ getDirectionLabel(row.direction) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" :label="t('tradingMarket.colQuantity')" min-width="120" />
        <el-table-column prop="price" :label="t('tradingMarket.colPrice')" min-width="130" />
        <el-table-column prop="status" :label="t('tradingMarket.colStatus')" min-width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" :label="t('tradingMarket.colCreateTime')" min-width="180" />
      </el-table>

      <div v-if="activeTab !== 'match'" class="pager-row">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          background
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="onSizeChange"
          @current-change="onPageChange"
        />
      </div>

      <el-table
        v-if="activeTab === 'match'"
        :data="matchData"
        :loading="matchLoading"
        border
      >
        <el-table-column :label="t('tradingMarket.colIndex')" width="80">
          <template #default="scope">
            {{ (matchPage - 1) * matchPageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="buyOrderId" :label="t('tradingMarket.colBuyId')" min-width="100" />
        <el-table-column prop="sellOrderId" :label="t('tradingMarket.colSellId')" min-width="100" />
        <el-table-column prop="matchedQuantity" :label="t('tradingMarket.colMatchQuantity')" min-width="130" />
        <el-table-column prop="matchedPrice" :label="t('tradingMarket.colMatchPrice')" min-width="140" />
        <el-table-column prop="matchedAt" :label="t('tradingMarket.colMatchTime')" min-width="180" />
      </el-table>

      <div v-if="activeTab === 'match'" class="pager-row">
        <el-pagination
          v-model:current-page="matchPage"
          v-model:page-size="matchPageSize"
          background
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="matchTotal"
          @size-change="onMatchSizeChange"
          @current-change="onMatchPageChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="t('tradingMarket.dialogCreate')"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formModel" :rules="formRules" label-width="100px">
        <el-form-item :label="t('tradingMarket.colDirection')" prop="direction">
          <el-select v-model="formModel.direction" :placeholder="t('tradingMarket.selectDirection')" style="width: 100%">
            <el-option
              v-for="item in directionOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('tradingMarket.colQuantity')" prop="quantity">
          <el-input
            v-model="formModel.quantity"
            type="number"
            :placeholder="t('tradingMarket.enterQuantity')"
            :min="0"
            :step="0.01"
          />
        </el-form-item>

        <el-form-item :label="t('tradingMarket.colPrice')" prop="price">
          <el-input
            v-model="formModel.price"
            type="number"
            :placeholder="t('tradingMarket.enterPrice')"
            :min="0"
            :step="0.01"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="onCancelDialog">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="onSave">{{ t('tradingMarket.btnSubmit') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>
<style scoped>
.market-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
}

.search-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.search-input {
  width: 420px;
  max-width: 100%;
}

.table-tip {
  margin-bottom: 10px;
  color: var(--text-secondary);
  font-size: 13px;
}

.pager-row {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

</style>
