<script setup>
import LoadState from '@/components/common/LoadState.vue'
import { useLoadState } from '@/composables/useLoadState'
/* eslint-disable no-unused-vars */
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import ManagementTable from '@/components/common/ManagementTable.vue'
import { auditAdminHotel, getAdminHotelAudits } from '@/services/dataSource'

const rows = ref([])
const columns = [
  { key: 'name', label: '房源名称' },
  { key: 'applicant', label: '申请主体' },
  { key: 'city', label: '城市' },
  { key: 'type', label: '类型' },
  { key: 'submittedAt', label: '提交时间' },
  { key: 'status', label: '审核状态' },
]
const dialog = ref(false)
const current = ref(null)
const rejectForm = reactive({ reason: '' })
const { loading, error, load } = useLoadState(
  async () => {
    rows.value = await getAdminHotelAudits()
  },
  () => {
    rows.value = []
  },
)
onMounted(load)

async function approveApi(row) {
  await auditAdminHotel(row.id, true)
  row.status = '已上架'
  ElMessage.success('房源审核已通过')
}

async function rejectApi() {
  if (!rejectForm.reason.trim()) return ElMessage.warning('请填写驳回原因')
  await auditAdminHotel(current.value.id, false, rejectForm.reason)
  current.value.status = '已驳回'
  dialog.value = false
  ElMessage.success('审核结果已提交')
}
function approve(row) {
  row.status = '已上架'
  ElMessage.success(`已通过「${row.name}」的审核`)
}
function openReject(row) {
  current.value = row
  rejectForm.reason = ''
  dialog.value = true
}
function reject() {
  if (!rejectForm.reason.trim()) return ElMessage.warning('请填写驳回原因')
  current.value.status = '已驳回'
  dialog.value = false
  ElMessage.success('审核结果已提交')
}
</script>

<template>
  <div class="bo-page">
    <header class="bo-page-head">
      <div>
        <p class="bo-kicker admin-kicker">PROPERTY COMPLIANCE</p>
        <h1>房源审核</h1>
        <p>核验经营资质、房源信息与展示内容。</p>
      </div>
      <span class="queue"
        >待审核 {{ rows.filter((item) => item.status === '待审核').length }} 项</span
      >
    </header>
    <LoadState :loading="loading" :error="error" @retry="load" />
    <ManagementTable
      :rows="rows"
      :columns="columns"
      :search-keys="['name', 'applicant', 'city', 'status']"
      placeholder="搜索房源或申请主体"
      ><template #actions="{ row }"
        ><button type="button" @click="$message.info(`${row.address} · 起价 ¥${row.price}`)">
          资料</button
        ><button v-if="row.status === '待审核'" type="button" @click="approveApi(row)">通过</button
        ><button v-if="row.status === '待审核'" type="button" @click="openReject(row)">
          驳回
        </button></template
      ></ManagementTable
    ><el-dialog v-model="dialog" title="驳回房源审核" width="min(520px,92vw)"
      ><p class="reject-title">{{ current?.name }}</p>
      <label class="bo-field"
        >驳回原因<textarea
          v-model="rejectForm.reason"
          placeholder="明确说明需要补充或修改的内容"
        ></textarea></label
      ><template #footer
        ><button class="bo-button" type="button" @click="dialog = false">取消</button
        ><button class="bo-button bo-button--danger" type="button" @click="rejectApi">
          确认驳回
        </button></template
      ></el-dialog
    >
  </div>
</template>

<style scoped>
.admin-kicker {
  color: #c53030;
}
.queue {
  padding: 9px 12px;
  color: #9b5c16;
  background: #fff4d6;
  font-size: 12px;
}
.reject-title {
  padding: 12px;
  background: #f8fafc;
  color: #334e68;
}
</style>
