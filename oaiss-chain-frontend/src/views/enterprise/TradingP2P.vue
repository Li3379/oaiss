<script setup lang="ts">
import { reactive, ref, onMounted } from "vue"
import { useI18n } from "vue-i18n"
import { ElMessage, ElMessageBox } from "element-plus"
import { getMyTrades, createP2PTrade, cancelTrade } from "../../api/trade"

const { t } = useI18n()

const loading = ref(false)
const searchForm = reactive({
  name: "",
  identity: "",
  orderNo: "",
})

const identityOptions = ["tradingP2P.identityBuyer", "tradingP2P.identitySeller"]

const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const tableData = ref([])

const dialogVisible = ref(false)
const dialogFormRef = ref()
const dialogForm = reactive({
  quantity: "",
  unitPrice: "",
  remark: "",
})

const dialogRules = {
  quantity: [
    { required: true, message: t("tradingP2P.enterQuantity"), trigger: "blur" },
    { type: "number", min: 0.01, message: t("tradingP2P.quantityPositive"), trigger: "blur" }
  ],
  unitPrice: [
    { required: true, message: t("tradingP2P.enterPrice"), trigger: "blur" },
    { type: "number", min: 0.01, message: t("tradingP2P.pricePositive"), trigger: "blur" }
  ],
}

const getStatusTagType = (status) => {
  const statusMap = {
    0: "warning",
    1: "primary",
    2: "success",
    3: "info",
    4: "danger"
  }
  return statusMap[status] || "info"
}

const loadTrades = async () => {
  loading.value = true
  try {
    const result = await getMyTrades({
      pageNum: page.value,
      pageSize: pageSize.value,
      keyword: searchForm.name || undefined,
      tradeNo: searchForm.orderNo || undefined,
    })
    tableData.value = result?.items || []
    total.value = result?.total || 0
  } catch (error) {
    ElMessage.error(t("tradingP2P.loadFailed"))
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const onQuery = () => {
  page.value = 1
  loadTrades()
}

const onSizeChange = (size) => {
  pageSize.value = size
  page.value = 1
  loadTrades()
}

const onPageChange = (val) => {
  page.value = val
  loadTrades()
}

const resetDialogForm = () => {
  dialogForm.quantity = ""
  dialogForm.unitPrice = ""
  dialogForm.remark = ""
}

const openAddDialog = () => {
  resetDialogForm()
  dialogVisible.value = true
}

const onCancel = () => {
  dialogVisible.value = false
  ElMessage.info(t("tradingP2P.cancelled"))
}

const onSave = async () => {
  const valid = await dialogFormRef.value.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning(t("tradingP2P.incompleteForm"))
    return
  }

  try {
    await createP2PTrade({
      tradeType: 2,
      quantity: Number(dialogForm.quantity),
      unitPrice: Number(dialogForm.unitPrice),
      remark: dialogForm.remark || "",
    })

    dialogVisible.value = false
    ElMessage.success(t("tradingP2P.createSuccess"))
    await loadTrades()
  } catch (error) {
    ElMessage.error(t("tradingP2P.createFailed") + ": " + (error.message || ""))
  }
}

const onCancelTrade = async (row) => {
  if (row.status !== 0) {
    ElMessage.warning(t("tradingP2P.canOnlyCancelPending"))
    return
  }

  try {
    await ElMessageBox.confirm(t("ordersManage.confirmCancel"), t("ordersManage.cancelTrade"), {
      confirmButtonText: t("common.confirm"),
      cancelButtonText: t("common.cancel"),
      type: "warning",
    })

    await cancelTrade(row.id)
    ElMessage.success(t("tradingP2P.cancelSuccess"))
    await loadTrades()
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error(t("tradingP2P.cancelFailed") + ": " + (error.message || ""))
    } else {
      ElMessage.info(t("common.cancelled"))
    }
  }
}

onMounted(() => {
  loadTrades()
})
</script>

<template>
  <section class="p2p-page">
    <el-card class="section-card" shadow="never">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>{{ t('tradingP2P.breadcrumbTrading') }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ t('tradingP2P.breadcrumbP2P') }}</el-breadcrumb-item>
      </el-breadcrumb>
    </el-card>

    <el-card class="section-card" shadow="never">
      <el-form :inline="true" class="search-form">
        <el-form-item :label="t('tradingP2P.colBuyerName')">
          <el-input v-model="searchForm.name" :placeholder="t('common.enterKeyword')" clearable />
        </el-form-item>

        <el-form-item :label="t('ordersManage.colTradeType')">
          <el-select v-model="searchForm.identity" :placeholder="t('tradingP2P.colTradeType')" clearable>
            <el-option v-for="item in identityOptions" :key="item" :label="t(item)" :value="item" />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('tradingP2P.colTradeNo')">
          <el-input v-model="searchForm.orderNo" :placeholder="t('common.enterKeyword')" clearable />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="onQuery">{{ t('common.search') }}</el-button>
          <el-button type="success" plain @click="openAddDialog">{{ t('tradingP2P.dialogCreate') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="section-card" shadow="never">
      <el-table :data="tableData" border v-loading="loading" :empty-text="t('tradingP2P.emptyText')">
        <el-table-column :label="t('tradingMarket.colIndex')" width="80">
          <template #default="scope">
            {{ (page - 1) * pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="tradeNo" :label="t('tradingP2P.colTradeNo')" min-width="160" />
        <el-table-column prop="buyerName" :label="t('tradingP2P.colBuyerName')" min-width="140" />
        <el-table-column prop="sellerName" :label="t('tradingP2P.colSellerName')" min-width="140" />
        <el-table-column prop="quantity" :label="t('tradingP2P.colQuantity')" min-width="110" />
        <el-table-column prop="unitPrice" :label="t('tradingP2P.colUnitPrice')" min-width="110" />
        <el-table-column prop="totalAmount" :label="t('tradingP2P.colTotalAmount')" min-width="120" />
        <el-table-column prop="statusText" :label="t('tradingP2P.colStatus')" min-width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" :label="t('tradingP2P.colCreateTime')" min-width="160" />
        <el-table-column :label="t('tradingP2P.colOperation')" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" link type="danger" @click="onCancelTrade(row)">{{ t('ordersManage.btnCancel') }}</el-button>
            <span v-else style="color: #909399; font-size: 12px;">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
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
    </el-card>

    <el-dialog v-model="dialogVisible" :title="t('tradingP2P.dialogCreate')" width="520px" destroy-on-close>
      <el-form ref="dialogFormRef" :model="dialogForm" :rules="dialogRules" label-width="100px">
        <el-form-item :label="t('tradingP2P.colQuantity')" prop="quantity">
          <el-input-number v-model="dialogForm.quantity" :min="0.01" :precision="2" :step="1" style="width: 100%" :placeholder="t('tradingP2P.enterQuantity')" />
        </el-form-item>

        <el-form-item :label="t('tradingP2P.colUnitPrice')" prop="unitPrice">
          <el-input-number v-model="dialogForm.unitPrice" :min="0.01" :precision="2" :step="0.01" style="width: 100%" :placeholder="t('tradingP2P.enterPrice')" />
        </el-form-item>

        <el-form-item :label="t('ordersManage.labelRemark')" prop="remark">
          <el-input v-model="dialogForm.remark" type="textarea" :rows="3" :placeholder="t('ordersManage.labelRemark') + ' (' + t('common.cancel') + ')'" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="onCancel">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="onSave">{{ t('common.create') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.p2p-page {
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

.pagination-row {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
