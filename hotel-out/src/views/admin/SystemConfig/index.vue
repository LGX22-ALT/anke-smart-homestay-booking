<script setup>
import LoadState from '@/components/common/LoadState.vue'
import { useLoadState } from '@/composables/useLoadState'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import ManagementTable from '@/components/common/ManagementTable.vue'
import AiSettingsPanel from '@/components/business/AiConnectionLibrary.vue'
import { getAdminBanners, saveAdminBanner } from '@/services/dataSource'

const activeTab = ref('basic')
const banners = ref([])
const basic = reactive({
  platformName: '住见旅宿',
  servicePhone: '400-886-2026',
  serviceTime: '08:00 - 22:00',
  orderCancelHours: 24,
  reviewDays: 30,
})
const bannerDialog = ref(false)
const banner = reactive({ title: '', position: '首页横幅', status: '启用', sort: 1 })
const columns = [
  { key: 'title', label: '名称' },
  { key: 'position', label: '展示位置' },
  { key: 'sort', label: '排序' },
  { key: 'status', label: '状态' },
]
const { loading, error, load } = useLoadState(
  async () => {
    banners.value = await getAdminBanners()
  },
  () => {
    banners.value = []
  },
)
onMounted(load)
async function addBanner() {
  if (!banner.title.trim()) return ElMessage.warning('请输入内容名称')
  banners.value.push(await saveAdminBanner({ ...banner, sortNo: banner.sort }))
  bannerDialog.value = false
  ElMessage.success('运营内容已添加')
}

async function toggleBanner(row) {
  const status = Number(row.status) === 1 || row.status === '启用' ? 0 : 1
  const saved = await saveAdminBanner({ ...row, status }, row.id)
  Object.assign(row, saved)
  ElMessage.success('状态已更新')
}
</script>

<template>
  <div class="bo-page">
    <header class="bo-page-head">
      <div>
        <p class="bo-kicker admin-kicker">PLATFORM SETTINGS</p>
        <h1>系统配置</h1>
        <p>管理平台基础规则与首页运营内容。</p>
      </div>
    </header>
    <LoadState :loading="loading" :error="error" @retry="load" />
    <div class="config-tabs">
      <button :class="{ active: activeTab === 'ai' }" type="button" @click="activeTab = 'ai'">
        AI 助手
      </button>
      <button :class="{ active: activeTab === 'basic' }" type="button" @click="activeTab = 'basic'">
        基础设置</button
      ><button
        :class="{ active: activeTab === 'content' }"
        type="button"
        @click="activeTab = 'content'"
      >
        运营内容</button
      ><button
        :class="{ active: activeTab === 'dictionary' }"
        type="button"
        @click="activeTab = 'dictionary'"
      >
        业务字典
      </button>
    </div>
    <AiSettingsPanel v-if="activeTab === 'ai'" />
    <section v-else-if="activeTab === 'basic'" class="bo-panel config-panel">
      <h2>平台基础信息</h2>
      <div class="bo-form-grid">
        <label class="bo-field">平台名称<input v-model="basic.platformName" /></label
        ><label class="bo-field">客服电话<input v-model="basic.servicePhone" /></label
        ><label class="bo-field">客服时间<input v-model="basic.serviceTime" /></label
        ><label class="bo-field"
          >免费取消时限（小时）<input
            v-model.number="basic.orderCancelHours"
            type="number"
            min="0" /></label
        ><label class="bo-field"
          >可评价时限（天）<input v-model.number="basic.reviewDays" type="number" min="1"
        /></label>
      </div>
      <button class="bo-button bo-button--primary save" type="button" disabled>
        基础配置保存暂未开放
      </button>
    </section>
    <section v-else-if="activeTab === 'content'">
      <ManagementTable
        :rows="banners"
        :columns="columns"
        :search-keys="['title', 'position', 'status']"
        placeholder="搜索运营内容"
        ><template #tools
          ><button class="bo-button bo-button--primary" type="button" @click="bannerDialog = true">
            + 新增内容
          </button></template
        ><template #actions="{ row }"
          ><button type="button" @click="toggleBanner(row)">
            {{ Number(row.status) === 1 || row.status === '启用' ? '停用' : '启用' }}
          </button></template
        ></ManagementTable
      >
    </section>
    <section v-else class="bo-panel">
      <h2>业务字典</h2>
      <div class="dictionary">
        <div>
          <strong>房源类型</strong>
          <p><span>精品民宿</span><span>精品酒店</span><span>度假屋</span><span>营地</span></p>
        </div>
        <div>
          <strong>订单状态</strong>
          <p>
            <span>待支付</span><span>待确认</span><span>待入住</span><span>已完成</span
            ><span>已取消</span>
          </p>
        </div>
        <div>
          <strong>设施标签</strong>
          <p>
            <span>免费 WiFi</span><span>停车场</span><span>早餐</span><span>宠物友好</span
            ><span>亲子设施</span>
          </p>
        </div>
      </div>
    </section>
    <el-dialog v-model="bannerDialog" title="新增运营内容" width="min(520px,92vw)"
      ><div class="bo-form-grid">
        <label class="bo-field full">内容名称<input v-model="banner.title" /></label
        ><label class="bo-field"
          >展示位置<select v-model="banner.position">
            <option>首页横幅</option>
            <option>首页推荐位</option>
            <option>搜索页</option>
          </select></label
        ><label class="bo-field"
          >状态<select v-model="banner.status">
            <option>启用</option>
            <option>停用</option>
          </select></label
        ><label class="bo-field"
          >排序<input v-model.number="banner.sort" type="number" min="1"
        /></label>
      </div>
      <template #footer
        ><button class="bo-button" type="button" @click="bannerDialog = false">取消</button
        ><button class="bo-button bo-button--primary" type="button" @click="addBanner">
          添加
        </button></template
      ></el-dialog
    >
  </div>
</template>

<style scoped>
.admin-kicker {
  color: #c53030;
}
.config-tabs {
  display: flex;
  gap: 2px;
  border-bottom: 1px solid #cbd5e1;
}
.config-tabs button {
  padding: 11px 18px;
  border: 0;
  border-bottom: 2px solid transparent;
  color: var(--muted);
  background: transparent;
  cursor: pointer;
}
.config-tabs button.active {
  border-color: #c53030;
  color: #9b2c2c;
}
.config-panel {
  max-width: 850px;
}
.save {
  margin-top: 22px;
}
.dictionary {
  display: grid;
  gap: 18px;
}
.dictionary > div {
  display: grid;
  grid-template-columns: 110px 1fr;
  align-items: start;
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7eb;
}
.dictionary p {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin: 0;
}
.dictionary span {
  padding: 6px 9px;
  color: #52606d;
  background: #f0f4f8;
  font-size: 11px;
}
.full {
  grid-column: 1/-1;
}
</style>
