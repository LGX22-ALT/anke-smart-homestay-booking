<script setup>
import {
  ChatDotRound,
  Connection,
  DataAnalysis,
  DocumentChecked,
  Setting,
  Tickets,
  User,
} from '@element-plus/icons-vue'

import BackOfficeLayout from './BackOfficeLayout.vue'

const navigation = [
  { to: '/admin/dashboard', label: '工作台', icon: DataAnalysis },
  { to: '/admin/users', label: '用户管理', icon: User },
  { to: '/admin/audits', label: '房源审核', icon: DocumentChecked },
  { to: '/admin/orders', label: '订单监控', icon: Tickets },
  { to: '/admin/comments', label: '评价管理', icon: ChatDotRound },
  { to: '/admin/ai', label: '大模型接入', icon: Connection },
  { to: '/admin/config', label: '系统配置', icon: Setting },
]
</script>

<template>
  <BackOfficeLayout
    title="HOTEL OUT"
    role-label="平台管理中心"
    accent="#e6a23c"
    :navigation="navigation"
  />
</template>
