export default [
  {
    path: '/admin',
    component: () => import('@/components/layout/AdminLayout.vue'),
    meta: { requiresAuth: true, roles: ['admin'] },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      {
        path: 'dashboard',
        name: 'admin-dashboard',
        component: () => import('@/views/admin/Dashboard/index.vue'),
        meta: { title: '平台总览' },
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/views/admin/UserManage/index.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'audits',
        name: 'admin-audits',
        component: () => import('@/views/admin/HotelAudit/index.vue'),
        meta: { title: '房源审核' },
      },
      {
        path: 'orders',
        name: 'admin-orders',
        component: () => import('@/views/admin/OrderMonitor/index.vue'),
        meta: { title: '订单监控' },
      },
      {
        path: 'comments',
        name: 'admin-comments',
        component: () => import('@/views/admin/CommentManage/index.vue'),
        meta: { title: '评价治理' },
      },
      {
        path: 'ai',
        name: 'admin-ai',
        component: () => import('@/views/admin/AiManage/index.vue'),
        meta: { title: '大模型接入' },
      },
      {
        path: 'config',
        name: 'admin-config',
        component: () => import('@/views/admin/SystemConfig/index.vue'),
        meta: { title: '系统配置' },
      },
    ],
  },
]
