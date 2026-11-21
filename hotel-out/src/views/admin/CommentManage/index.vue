<script setup>
import LoadState from '@/components/common/LoadState.vue'
import { useLoadState } from '@/composables/useLoadState'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, ref } from 'vue'

import ManagementTable from '@/components/common/ManagementTable.vue'
import { getAdminReviews, moderateAdminReview } from '@/services/dataSource'

const rows = ref([])
const columns = [
  { key: 'hotel', label: '房源' },
  { key: 'user', label: '发布者' },
  { key: 'score', label: '评分' },
  { key: 'content', label: '评价内容' },
  { key: 'date', label: '发布时间' },
  { key: 'status', label: '状态' },
]
const { loading, error, load } = useLoadState(
  async () => {
    rows.value = await getAdminReviews()
  },
  () => {
    rows.value = []
  },
)
onMounted(load)
async function hide(row) {
  await ElMessageBox.confirm('隐藏后前台将不再展示该评价，是否继续？', '内容治理', {
    type: 'warning',
  })
  await moderateAdminReview(row.id, false)
  row.status = '已隐藏'
  ElMessage.success('评价已隐藏')
}

async function restore(row) {
  await moderateAdminReview(row.id, true)
  row.status = '正常'
  ElMessage.success('评价已恢复')
}
</script>

<template>
  <div class="bo-page">
    <header class="bo-page-head">
      <div>
        <p class="bo-kicker admin-kicker">CONTENT SAFETY</p>
        <h1>评价治理</h1>
        <p>核验举报与敏感内容，维护真实可信的评价环境。</p>
      </div>
      <span class="bo-tag bo-tag--danger"
        >待处理 {{ rows.filter((item) => item.status === '待处理').length }}</span
      >
    </header>
    <LoadState :loading="loading" :error="error" @retry="load" />
    <ManagementTable
      :rows="rows"
      :columns="columns"
      :search-keys="['hotel', 'user', 'content', 'status']"
      placeholder="搜索评价内容或发布者"
      ><template #cell-score="{ value }"
        ><span class="stars">{{ '★'.repeat(value) }}</span></template
      ><template #cell-content="{ value }"
        ><span class="content" :title="value">{{ value }}</span></template
      ><template #actions="{ row }"
        ><button type="button" @click="$message.info(row.reply || '商家暂未回复')">上下文</button
        ><button v-if="row.status !== '已隐藏'" type="button" @click="hide(row)">隐藏</button
        ><button v-else type="button" @click="restore(row)">恢复</button></template
      ></ManagementTable
    >
  </div>
</template>

<style scoped>
.admin-kicker {
  color: #c53030;
}
.stars {
  color: #d69e2e;
}
.content {
  display: block;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
