<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getReportList } from '../../api/carbon'

const { t } = useI18n()

const searchForm = reactive({
  keyword: '',
})

const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    }
    const response = await getReportList(params)
    tableData.value = response.items || []
    total.value = response.total || 0
  } catch (error) {
    ElMessage.error(t('systemCarbon.loadFailed'))
  } finally {
    loading.value = false
  }
}

const onQuery = () => {
  currentPage.value = 1
  fetchData()
}

const onSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchData()
}

const onCurrentChange = (page) => {
  currentPage.value = page
  fetchData()
}

const getStatusType = (status) => {
  const statusMap = {
    0: 'info',
    1: 'warning',
    2: 'success',
    3: 'danger',
    4: 'info',
  }
  return statusMap[status] || 'info'
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <section class="carbon-page">
    <el-card class="section-card" shadow="never">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>{{ t('systemCarbon.breadcrumbSystem') }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ t('systemCarbon.breadcrumbCarbon') }}</el-breadcrumb-item>
      </el-breadcrumb>
    </el-card>

    <el-card class="section-card" shadow="never">
      <el-form :inline="true" class="search-form">
        <el-form-item :label="t('common.keyword')">
          <el-input v-model="searchForm.keyword" :placeholder="t('common.enterKeyword')" clearable />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="onQuery">{{ t('common.search') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="section-card" shadow="never">
      <el-table :data="tableData" border v-loading="loading">
        <el-table-column prop="reportNo" :label="t('systemCarbon.colReportNo')" min-width="140" />
        <el-table-column prop="enterpriseName" :label="t('systemCarbon.colEnterpriseName')" min-width="180" />
        <el-table-column prop="title" :label="t('systemCarbon.colReportTitle')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="totalEmission" :label="t('systemCarbon.colTotalEmission')" min-width="130">
          <template #default="{ row }">{{ row.totalEmission }} tCO2e</template>
        </el-table-column>
        <el-table-column prop="statusText" :label="t('common.status')" min-width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewerName" :label="t('systemCarbon.colReviewer')" min-width="120" />
        <el-table-column prop="createdAt" :label="t('common.createTime')" min-width="170" />
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
</template>

<style scoped>
.carbon-page {
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
