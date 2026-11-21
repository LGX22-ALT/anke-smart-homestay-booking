<script setup>
import LoadState from '@/components/common/LoadState.vue'
import { useLoadState } from '@/composables/useLoadState'
/* eslint-disable no-unused-vars */
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import ImageUpload from '@/components/business/ImageUpload.vue'
import ManagementTable from '@/components/common/ManagementTable.vue'
import { getMerchantHotels, saveMerchantHotel, setMerchantHotelStatus } from '@/services/dataSource'

const rows = ref([])
const dialogVisible = ref(false)
const statusFilter = ref('')
const editingId = ref(null)
const form = reactive({
  name: '',
  type: '精品民宿',
  city: '',
  address: '',
  price: 399,
  status: '草稿',
  images: [],
  roomName: '景观大床房',
  bed: '1.8 米大床',
  guests: 2,
  facilities: '免费 WiFi、停车场、早餐',
  nearbyText: '',
})
const visibleRows = computed(() =>
  statusFilter.value ? rows.value.filter((item) => item.status === statusFilter.value) : rows.value,
)
const columns = [
  { key: 'name', label: '房源名称' },
  { key: 'type', label: '类型' },
  { key: 'city', label: '城市' },
  { key: 'price', label: '起价' },
  { key: 'score', label: '评分' },
  { key: 'status', label: '状态' },
]

const { loading, error, load } = useLoadState(
  async () => {
    rows.value = await getMerchantHotels({ pageSize: 100 })
  },
  () => {
    rows.value = []
  },
)
onMounted(load)

async function saveApi() {
  if (!form.name.trim() || !form.city.trim()) return ElMessage.warning('请填写房源名称和城市')
  const saved = await saveMerchantHotel(form, editingId.value)
  const index = rows.value.findIndex((item) => item.id === saved.id)
  if (index >= 0) rows.value[index] = saved
  else rows.value.unshift(saved)
  dialogVisible.value = false
  ElMessage.success('房源信息已提交审核')
}

async function toggleStatusApi(row) {
  const status = row.status === '已上架' ? '已下架' : '已上架'
  Object.assign(row, await setMerchantHotelStatus(row.id, status))
  ElMessage.success('房源状态已更新')
}

function openEditor(row) {
  editingId.value = row?.id || null
  Object.assign(
    form,
    row
      ? {
          ...row,
          images: row.gallery || [row.image],
          nearbyText: (row.nearby || [])
            .map((item) =>
              [item.type, item.name, item.distance, item.description].filter(Boolean).join('｜'),
            )
            .join('\n'),
        }
      : {
          name: '',
          type: '精品民宿',
          city: '',
          address: '',
          price: 399,
          status: '草稿',
          images: [],
          roomName: '景观大床房',
          bed: '1.8 米大床',
          guests: 2,
          facilities: '免费 WiFi、停车场、早餐',
          nearbyText: '',
        },
  )
  dialogVisible.value = true
}

function save() {
  if (!form.name.trim() || !form.city.trim()) return ElMessage.warning('请填写房源名称和所在城市')
  if (editingId.value) {
    rows.value = rows.value.map((item) =>
      item.id === editingId.value ? { ...item, ...form } : item,
    )
  } else {
    rows.value.unshift({
      ...form,
      id: Date.now(),
      score: '-',
      image: form.images[0] || '',
      gallery: form.images,
    })
  }
  dialogVisible.value = false
  ElMessage.success('房源信息已保存')
}

function toggleStatus(row) {
  row.status = row.status === '已上架' ? '已下架' : '已上架'
  ElMessage.success(`已${row.status === '已上架' ? '上架' : '下架'}「${row.name}」`)
}
</script>

<template>
  <div class="bo-page">
    <header class="bo-page-head">
      <div>
        <h1>房源管理</h1>
        <p>维护房源资料、售价和上架状态。</p>
      </div>
      <button class="bo-button bo-button--primary" type="button" @click="openEditor()">
        + 新增房源
      </button>
    </header>
    <LoadState :loading="loading" :error="error" @retry="load" />
    <ManagementTable
      :rows="visibleRows"
      :columns="columns"
      :search-keys="['name', 'city', 'type', 'status']"
      placeholder="搜索房源名称或城市"
    >
      <template #tools
        ><select v-model="statusFilter" class="status-filter">
          <option value="">全部状态</option>
          <option>已上架</option>
          <option>待审核</option>
          <option>已下架</option>
          <option>草稿</option>
        </select></template
      >
      <template #cell-price="{ value }">¥{{ value }} 起</template>
      <template #cell-score="{ value }"
        ><span class="score">★ {{ value }}</span></template
      >
      <template #actions="{ row }"
        ><button type="button" @click="openEditor(row)">编辑</button
        ><RouterLink class="table-link" :to="{ name: 'hotel-detail', params: { id: row.id } }"
          >预览</RouterLink
        ><button type="button" @click="toggleStatusApi(row)">
          {{ row.status === '已上架' ? '下架' : '上架' }}
        </button></template
      >
    </ManagementTable>
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑房源' : '新增房源'"
      width="min(680px, 94vw)"
    >
      <div class="bo-form-grid">
        <label class="bo-field"
          >房源名称<input v-model="form.name" placeholder="例如：山岚白房子" /></label
        ><label class="bo-field"
          >房源类型<select v-model="form.type">
            <option>精品民宿</option>
            <option>精品酒店</option>
            <option>度假屋</option>
            <option>营地</option>
          </select></label
        ><label class="bo-field">所在城市<input v-model="form.city" placeholder="城市" /></label
        ><label class="bo-field"
          >每晚起价<input v-model.number="form.price" type="number" min="0" /></label
        ><label class="bo-field full">详细地址<input v-model="form.address" /></label
        ><label class="bo-field"
          >状态<select v-model="form.status">
            <option>草稿</option>
            <option>待审核</option>
            <option>已上架</option>
            <option>已下架</option>
          </select></label
        >
        <label class="bo-field">默认房型<input v-model="form.roomName" /></label>
        <label class="bo-field">床型<input v-model="form.bed" /></label>
        <label class="bo-field"
          >可住人数<input v-model.number="form.guests" type="number" min="1"
        /></label>
        <label class="bo-field full"
          >设施配置<input v-model="form.facilities" placeholder="使用顿号分隔多个设施"
        /></label>
        <label class="bo-field full"
          >周边信息
          <textarea
            v-model="form.nearbyText"
            rows="4"
            placeholder="每行一个地点：类型｜名称｜距离｜说明，例如：景点｜广州塔｜步行12分钟｜珠江夜景地标"
          />
          <small>用于房源详情页的“周边与出行”，每行填写一个地点。</small>
        </label>
        <div class="bo-field full"><span>房源图片</span><ImageUpload v-model="form.images" /></div>
      </div>
      <template #footer
        ><button class="bo-button" type="button" @click="dialogVisible = false">取消</button
        ><button class="bo-button bo-button--primary" type="button" @click="saveApi">
          保存房源
        </button></template
      >
    </el-dialog>
  </div>
</template>

<style scoped>
.score {
  color: #b7791f;
}
.full {
  grid-column: 1/-1;
}
.status-filter {
  min-height: 36px;
  padding: 0 10px;
  border: 1px solid #cbd5e1;
  background: #fff;
}
.table-link {
  padding: 5px 8px;
  border: 1px solid #cbd5e1;
  border-radius: 3px;
  color: #52606d;
  text-decoration: none;
}
.bo-field textarea {
  resize: vertical;
  padding: 10px 12px;
  border: 1px solid #cbd5e1;
  font: inherit;
}
.bo-field small {
  color: var(--muted);
  font-size: 11px;
}
</style>
