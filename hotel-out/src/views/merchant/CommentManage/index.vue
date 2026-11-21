<script setup>
import LoadState from '@/components/common/LoadState.vue'
import { useLoadState } from '@/composables/useLoadState'
/* eslint-disable no-unused-vars */
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'

import ManagementTable from '@/components/common/ManagementTable.vue'
import { getMerchantReviews, replyMerchantReview } from '@/services/dataSource'

const rows = ref([])
const replyVisible = ref(false)
const current = ref(null)
const reply = ref('')
const columns = [
  { key: 'hotel', label: '房源' },
  { key: 'user', label: '住客' },
  { key: 'score', label: '评分' },
  { key: 'content', label: '评价内容' },
  { key: 'date', label: '日期' },
  { key: 'status', label: '状态' },
]
const { loading, error, load } = useLoadState(
  async () => {
    rows.value = await getMerchantReviews()
  },
  () => {
    rows.value = []
  },
)
onMounted(load)

async function saveReplyApi() {
  Object.assign(current.value, await replyMerchantReview(current.value.id, reply.value.trim()))
  replyVisible.value = false
  ElMessage.success('回复已发布')
}
function openReply(row) {
  current.value = row
  reply.value = row.reply
  replyVisible.value = true
}
function saveReply() {
  current.value.reply = reply.value.trim()
  replyVisible.value = false
  ElMessage.success('回复已发布')
}
</script>

<template>
  <div class="bo-page">
    <header class="bo-page-head">
      <div>
        <h1>评价管理</h1>
        <p>阅读住客反馈，建立稳定的服务沟通。</p>
      </div>
    </header>
    <LoadState :loading="loading" :error="error" @retry="load" />
    <ManagementTable
      :rows="rows"
      :columns="columns"
      :search-keys="['hotel', 'user', 'content', 'status']"
      placeholder="搜索房源、住客或评价"
      ><template #cell-score="{ value }"
        ><span class="stars">{{ '★'.repeat(value) }}</span></template
      ><template #cell-content="{ value }"
        ><span class="comment-cell" :title="value">{{ value }}</span></template
      ><template #actions="{ row }"
        ><button type="button" @click="openReply(row)">
          {{ row.reply ? '修改回复' : '回复' }}
        </button></template
      ></ManagementTable
    ><el-dialog v-model="replyVisible" title="回复住客评价" width="min(560px, 92vw)"
      ><div v-if="current" class="review-context">
        <strong>{{ current.user }} · {{ current.score }} 分</strong>
        <p>{{ current.content }}</p>
      </div>
      <label class="bo-field"
        >商家回复<textarea
          v-model="reply"
          maxlength="300"
          placeholder="感谢住客的反馈，清晰说明后续改进。"
        ></textarea></label
      ><template #footer
        ><button class="bo-button" type="button" @click="replyVisible = false">取消</button
        ><button
          class="bo-button bo-button--primary"
          type="button"
          :disabled="!reply.trim()"
          @click="saveReplyApi"
        >
          发布回复
        </button></template
      ></el-dialog
    >
  </div>
</template>

<style scoped>
.stars {
  color: #d69e2e;
  white-space: nowrap;
}
.comment-cell {
  display: block;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
}
.review-context {
  margin-bottom: 16px;
  padding: 14px;
  background: #f8fafc;
}
.review-context p {
  margin: 8px 0 0;
  color: #52606d;
  line-height: 1.6;
}
</style>
