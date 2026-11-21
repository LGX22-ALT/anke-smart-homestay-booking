<script setup>
import {
  Calendar,
  ChatDotRound,
  ChatLineRound,
  DataAnalysis,
  House,
  Tickets,
  TrendCharts,
  User,
} from '@element-plus/icons-vue'

import BackOfficeLayout from './BackOfficeLayout.vue'

const navigation = [
  { to: '/merchant/dashboard', label: '工作台', icon: DataAnalysis },
  { to: '/merchant/hotels', label: '房源管理', icon: House },
  { to: '/merchant/calendar', label: '房态日历', icon: Calendar },
  { to: '/merchant/orders', label: '订单管理', icon: Tickets },
  { to: '/merchant/comments', label: '评价管理', icon: ChatDotRound },
  { to: '/merchant/messages', label: '住客私信', icon: ChatLineRound },
  { to: '/merchant/statistics', label: '经营统计', icon: TrendCharts },
  { to: '/merchant/profile', label: '个人资料', icon: User },
]
</script>

<template>
  <BackOfficeLayout title="HOTEL OUT" role-label="商家管理台" :navigation="navigation" />
</template>
