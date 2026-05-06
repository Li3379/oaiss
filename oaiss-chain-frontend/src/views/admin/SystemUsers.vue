<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, updateUserStatus } from '../../api/admin'

const { t } = useI18n()

const searchForm = reactive({
  userType: '',
  status: '',
})

const userTypeOptions = [
  { label: t('systemUsers.typeAll'), value: '' },
  { label: t('systemUsers.typeEnterprise'), value: 1 },
  { label: t('systemUsers.typeAuditor'), value: 2 },
  { label: t('systemUsers.typeThirdParty'), value: 3 },
  { label: t('systemUsers.typeAdmin'), value: 4 },
]

const statusOptions = [
  { label: t('systemUsers.typeAll'), value: '' },
  { label: t('systemUsers.statusEnabled'), value: 1 },
  { label: t('systemUsers.statusDisabled'), value: 0 },
]

const userList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const userTypeMap = {
  1: t('systemUsers.typeEnterprise'),
  2: t('systemUsers.typeAuditor'),
  3: t('systemUsers.typeThirdParty'),
  4: t('systemUsers.typeAdmin'),
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      ...(searchForm.userType !== '' && { userType: searchForm.userType }),
      ...(searchForm.status !== '' && { status: searchForm.status }),
    }
    const response = await getUserList(params)
    userList.value = response.items || []
    total.value = response.total || 0
  } catch (error) {
    ElMessage.error(t('systemUsers.loadFailed'))
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

const handleStatusToggle = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const statusText = newStatus === 1 ? t('systemUsers.statusEnabled') : t('systemUsers.statusDisabled')

  try {
    await ElMessageBox.confirm(
      newStatus === 1 ? t('systemUsers.confirmEnable') : t('systemUsers.confirmDisable'),
      newStatus === 1 ? t('systemUsers.enableUser') : t('systemUsers.disableUser'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning',
      }
    )

    await updateUserStatus(row.id, newStatus)
    ElMessage.success(newStatus === 1 ? t('systemUsers.enableSuccess') : t('systemUsers.disableSuccess'))
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(newStatus === 1 ? t('systemUsers.enableFailed') : t('systemUsers.disableFailed'))
    }
  }
}

const getStatusType = (status) => {
  return status === 1 ? 'success' : 'danger'
}

const getStatusText = (status) => {
  return status === 1 ? t('systemUsers.statusEnabled') : t('systemUsers.statusDisabled')
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <section class="users-page">
    <el-card class="section-card" shadow="never">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>{{ t('systemUsers.breadcrumbSystem') }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ t('systemUsers.breadcrumbUsers') }}</el-breadcrumb-item>
      </el-breadcrumb>
    </el-card>

    <el-card class="section-card" shadow="never">
      <el-form :inline="true" class="search-form">
        <el-form-item :label="t('systemUsers.colUserType')">
          <el-select v-model="searchForm.userType" :placeholder="t('systemUsers.typeAll')" style="width: 140px" clearable>
            <el-option v-for="item in userTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('common.status')">
          <el-select v-model="searchForm.status" :placeholder="t('systemUsers.statusEnabled')" style="width: 140px" clearable>
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="onQuery">{{ t('common.search') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="section-card" shadow="never">
      <el-table :data="userList" border v-loading="loading">
        <el-table-column prop="username" :label="t('systemUsers.colUsername')" min-width="120" />
        <el-table-column prop="email" :label="t('systemUsers.colEmail')" min-width="180" />
        <el-table-column prop="userType" :label="t('systemUsers.colUserType')" min-width="120">
          <template #default="{ row }">
            {{ userTypeMap[row.userType] || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="t('common.status')" min-width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" :label="t('common.createTime')" min-width="170" />
        <el-table-column :label="t('common.operation')" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleStatusToggle(row)">
              {{ row.status === 1 ? t('systemUsers.statusDisabled') : t('systemUsers.statusEnabled') }}
            </el-button>
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
  </section>
</template>

<style scoped>
.users-page {
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
