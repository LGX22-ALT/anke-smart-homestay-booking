<script setup>
import { ArrowRight } from '@element-plus/icons-vue'
import AiSettingsPanel from '@/components/business/AiConnectionLibrary.vue'
</script>

<template>
  <div class="bo-page ai-manage-page">
    <header class="bo-page-head">
      <div>
        <h1>大模型接入</h1>
        <p>配置首页住宿助手使用的模型接口，帮助住客用一句话搜索酒店和民宿。</p>
      </div>
      <RouterLink to="/" class="assistant-preview">查看首页助手 <ArrowRight /></RouterLink>
    </header>
    <AiSettingsPanel />
  </div>
</template>

<style scoped>
.bo-kicker {
  color: #6750b5;
}
.assistant-preview {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  flex-shrink: 0;
  padding: 10px 14px;
  border: 1px solid #d8d0e8;
  border-radius: 6px;
  color: #6750b5;
  background: #fff;
  text-decoration: none;
  font-size: 12px;
}
.assistant-preview svg {
  width: 14px;
  height: 14px;
}
.assistant-preview:focus-visible {
  outline: 2px solid #6750b5;
  outline-offset: 3px;
}
@media (max-width: 650px) {
  .bo-page-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 15px;
  }
}
</style>
