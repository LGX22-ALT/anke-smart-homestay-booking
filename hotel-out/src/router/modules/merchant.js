export default [
  {
    path: '/merchant',
    component: () => import('@/components/layout/MerchantLayout.vue'),
    meta: { requiresAuth: true, roles: ['merchant'] },
    children: [
      { path: '', redirect: '/merchant/dashboard' },
      {
        path: 'profile',
        name: 'merchant-profile',
        component: () => import('@/views/merchant/Profile/index.vue'),
        meta: { title: '个人资料' },
      },
      {
        path: 'dashboard',
        name: 'merchant-dashboard',
        component: () => import('@/views/merchant/Dashboard/index.vue'),
        meta: { title: '经营总览' },
      },
      {
        path: 'hotels',
        name: 'merchant-hotels',
        component: () => import('@/views/merchant/HotelManage/index.vue'),
        meta: { title: '房源管理' },
      },
      {
        path: 'calendar',
        name: 'merchant-calendar',
        component: () => import('@/views/merchant/RoomCalendar/index.vue'),
        meta: { title: '房态日历' },
      },
      {
        path: 'orders',
        name: 'merchant-orders',
        component: () => import('@/views/merchant/OrderManage/index.vue'),
        meta: { title: '订单管理' },
      },
      {
        path: 'comments',
        name: 'merchant-comments',
        component: () => import('@/views/merchant/CommentManage/index.vue'),
        meta: { title: '评价管理' },
      },
      {
        path: 'messages',
        name: 'merchant-messages',
        component: () => import('@/views/merchant/MessageManage/index.vue'),
        meta: { title: '住客私信' },
      },
      {
        path: 'statistics',
        name: 'merchant-statistics',
        component: () => import('@/views/merchant/DataStatistics/index.vue'),
        meta: { title: '数据统计' },
      },
    ],
  },
]
