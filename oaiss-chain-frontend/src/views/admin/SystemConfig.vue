<script setup lang="ts">
import { computed, reactive, ref, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'

const { t } = useI18n()

const STORAGE_KEY = 'oaiss_system_configs'

const DEFAULT_CONFIGS = [
  {
    id: 1,
    description: t('systemConfig.defaultConfig1Desc'),
    name: 'core-service',
    host: '10.0.8.21',
    env: 'NODE_ENV=production',
    serviceUrl: 'https://core.example.com/api',
    updatedBy: 'admin01',
  },
  {
    id: 2,
    description: t('systemConfig.defaultConfig2Desc'),
    name: 'chain-gateway',
    host: '10.0.8.31',
    env: 'CHAIN_MODE=mainnet',
    serviceUrl: 'https://chain.example.com/gateway',
    updatedBy: 'admin02',
  },
  {
    id: 3,
    description: t('systemConfig.defaultConfig3Desc'),
    name: 'audit-service',
    host: '10.0.8.41',
    env: 'AUDIT_REGION=east',
    serviceUrl: 'https://audit.example.com/v1',
    updatedBy: 'admin03',
  },
]

const searchForm = reactive({
  description: '',
  name: '',
})

const loadConfigs = () => {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    return saved ? JSON.parse(saved) : DEFAULT_CONFIGS
  } catch {
    return DEFAULT_CONFIGS
  }
}

const configList = ref(loadConfigs())

watch(configList, (newVal) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(newVal))
}, { deep: true })

const selectedRows = ref([])
const page = ref(1)
const pageSize = ref(10)

const dialogVisible = ref(false)
const dialogMode = ref('add')
const editingId = ref(null)
const formRef = ref()
const formModel = reactive({
  description: '',
  name: '',
  host: '',
  env: '',
  serviceUrl: '',
})

const formRules = {
  description: [{ required: true, message: t('systemConfig.enterDesc'), trigger: 'blur' }],
  name: [{ required: true, message: t('systemConfig.enterName'), trigger: 'blur' }],
  host: [{ required: true, message: t('systemConfig.enterServerHost'), trigger: 'blur' }],
  env: [{ required: true, message: t('systemConfig.enterEnvVar'), trigger: 'blur' }],
  serviceUrl: [{ required: true, message: t('systemConfig.enterNetworkAddress'), trigger: 'blur' }],
}

const filteredData = computed(() => {
  const description = searchForm.description.trim().toLowerCase()
  const name = searchForm.name.trim().toLowerCase()

  return configList.value.filter((item) => {
    const descriptionMatch = !description || item.description.toLowerCase().includes(description)
    const nameMatch = !name || item.name.toLowerCase().includes(name)
    return descriptionMatch && nameMatch
  })
})

const total = computed(() => filteredData.value.length)

const pagedData = computed(() => {
  const start = (page.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredData.value.slice(start, end)
})

const onQuery = () => {
  page.value = 1
}

const onSelectionChange = (rows) => {
  selectedRows.value = rows
}

const onSizeChange = (size) => {
  pageSize.value = size
  page.value = 1
}

const onCurrentChange = (current) => {
  page.value = current
}

const resetForm = () => {
  formModel.description = ''
  formModel.name = ''
  formModel.host = ''
  formModel.env = ''
  formModel.serviceUrl = ''
}

const openAddDialog = () => {
  dialogMode.value = 'add'
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  dialogMode.value = 'edit'
  editingId.value = row.id
  formModel.description = row.description
  formModel.name = row.name
  formModel.host = row.host
  formModel.env = row.env
  formModel.serviceUrl = row.serviceUrl
  dialogVisible.value = true
}

const onCancelDialog = () => {
  dialogVisible.value = false
  ElMessage.info(t('common.cancelled'))
}

const onSaveDialog = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning(t('common.failed'))
    return
  }

  const payload = {
    description: formModel.description.trim(),
    name: formModel.name.trim(),
    host: formModel.host.trim(),
    env: formModel.env.trim(),
    serviceUrl: formModel.serviceUrl.trim(),
    updatedBy: t('systemConfig.defaultUpdatedBy'),
  }

  if (dialogMode.value === 'add') {
    configList.value.unshift({
      id: Date.now(),
      ...payload,
    })
    ElMessage.success(t('systemConfig.createSuccess'))
  } else {
    const idx = configList.value.findIndex((item) => item.id === editingId.value)
    if (idx > -1) {
      configList.value[idx] = {
        ...configList.value[idx],
        ...payload,
      }
      ElMessage.success(t('systemConfig.editSuccess'))
    }
  }

  dialogVisible.value = false
  page.value = 1
}

const onDelete = async (row) => {
  try {
    await ElMessageBox.confirm(t('systemConfig.confirmDelete'), t('common.delete'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })

    configList.value = configList.value.filter((item) => item.id !== row.id)
    selectedRows.value = selectedRows.value.filter((item) => item.id !== row.id)
    ElMessage.success(t('systemConfig.deleteSuccess'))

    if ((page.value - 1) * pageSize.value >= filteredData.value.length && page.value > 1) {
      page.value -= 1
    }
  } catch {
    ElMessage.info(t('common.cancelled'))
  }
}
</script>

<template>
  <section class="config-page">
    <el-card class="section-card" shadow="never">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>{{ t('systemConfig.breadcrumbSystem') }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ t('systemConfig.breadcrumbConfig') }}</el-breadcrumb-item>
      </el-breadcrumb>
    </el-card>

    <el-card class="section-card" shadow="never">
      <el-form :inline="true" class="search-form">
        <el-form-item :label="t('systemConfig.searchDesc')">
          <el-input v-model="searchForm.description" :placeholder="t('systemConfig.enterDesc')" clearable />
        </el-form-item>

        <el-form-item :label="t('systemConfig.searchName')">
          <el-input v-model="searchForm.name" :placeholder="t('systemConfig.enterName')" clearable />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="onQuery">{{ t('common.search') }}</el-button>
          <el-button type="success" plain @click="openAddDialog">{{ t('common.create') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="section-card" shadow="never">
      <div class="table-tip">{{ t('common.total', { count: total }) }}</div>

      <el-table :data="pagedData" border @selection-change="onSelectionChange">
        <el-table-column type="selection" width="56" />
        <el-table-column prop="host" :label="t('systemConfig.colServerHost')" min-width="140" />
        <el-table-column prop="env" :label="t('systemConfig.colEnvVar')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="serviceUrl" :label="t('systemConfig.colNetworkAddress')" min-width="220" show-overflow-tooltip />
        <el-table-column prop="updatedBy" :label="t('systemConfig.colUpdatedBy')" min-width="120" />
        <el-table-column :label="t('common.operation')" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">{{ t('common.edit') }}</el-button>
            <el-button link type="danger" @click="onDelete(row)">{{ t('common.delete') }}</el-button>
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
          @current-change="onCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? t('systemConfig.dialogCreate') : t('systemConfig.dialogEdit')"
      width="640px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formModel" :rules="formRules" label-width="110px">
        <el-form-item :label="t('systemConfig.searchDesc')" prop="description">
          <el-input v-model="formModel.description" :placeholder="t('systemConfig.enterDesc')" />
        </el-form-item>

        <el-form-item :label="t('systemConfig.searchName')" prop="name">
          <el-input v-model="formModel.name" :placeholder="t('systemConfig.enterName')" />
        </el-form-item>

        <el-form-item :label="t('systemConfig.colServerHost')" prop="host">
          <el-input v-model="formModel.host" :placeholder="t('systemConfig.enterServerHost')" />
        </el-form-item>

        <el-form-item :label="t('systemConfig.colEnvVar')" prop="env">
          <el-input v-model="formModel.env" :placeholder="t('systemConfig.enterEnvVar')" />
        </el-form-item>

        <el-form-item :label="t('systemConfig.colNetworkAddress')" prop="serviceUrl">
          <el-input v-model="formModel.serviceUrl" :placeholder="t('systemConfig.enterNetworkAddress')" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="onCancelDialog">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="onSaveDialog">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.config-page {
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
