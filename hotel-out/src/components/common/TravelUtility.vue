<script setup>
import { compareHotels } from '@/utils/travelStorage'
</script>
<template>
  <nav class="travel-utility" aria-label="旅行工具">
    <RouterLink to="/user/favorites">我的收藏</RouterLink
    ><RouterLink to="/history">最近浏览</RouterLink
    ><RouterLink to="/compare"
      >房源对比 <b v-if="compareHotels.length">{{ compareHotels.length }}</b></RouterLink
    ><RouterLink to="/help">预订帮助</RouterLink>
  </nav>
</template>
<style scoped>
.travel-utility {
  display: flex;
  justify-content: flex-end;
  gap: 24px;
  padding: 9px 24px;
  background: #f3f7fb;
  border-bottom: 1px solid #e4edf5;
  font-size: 11px;
}
.travel-utility a {
  color: #6a8399;
  text-decoration: none;
}
.travel-utility b {
  color: #ff7546;
}
@media (max-width: 520px) {
  .travel-utility {
    justify-content: center;
    gap: 18px;
    padding: 9px 10px;
  }
}
</style>
