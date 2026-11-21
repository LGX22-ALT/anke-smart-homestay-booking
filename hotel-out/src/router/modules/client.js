import Home from '@/views/client/Home/index.vue'

export default [
  ...[
    ['/attractions', 'client-attractions', '景点探索', 'attractions'],
    ['/attractions/:id', 'attraction-detail', '景点详情', 'attractions'],
    ['/guides', 'client-guides', '旅行攻略', 'guides'],
    ['/guides/:id', 'guide-detail', '攻略详情', 'guides'],
    ['/history', 'client-history', '最近浏览', 'history'],
    ['/compare', 'client-compare', '房源对比', 'compare'],
    ['/help', 'client-help', '预订帮助', 'help'],
  ].map(([path, name, title, exploreType]) => ({
    path,
    name,
    component: () => import('@/views/client/Explore/index.vue'),
    meta: { title, exploreType },
  })),
  {
    path: '/',
    name: 'client-home',
    component: Home,
    meta: { title: '首页' },
  },
  {
    path: '/login',
    name: 'client-login',
    component: () => import('@/views/client/Login/index.vue'),
    meta: { guestOnly: true, title: '登录' },
  },
  {
    path: '/search',
    name: 'client-search',
    component: () => import('@/views/client/Search/index.vue'),
    meta: { title: '搜索房源' },
  },
  {
    path: '/hotel/:id',
    name: 'hotel-detail',
    component: () => import('@/views/client/HotelDetail/index.vue'),
    meta: { title: '房源详情' },
  },
  {
    path: '/ai-chat',
    name: 'ai-chat',
    component: () => import('@/views/client/AiChat/index.vue'),
    meta: { title: 'AI 旅行顾问' },
  },
  {
    path: '/messages/:hotelId?',
    name: 'client-messages',
    component: () => import('@/views/client/Messages/index.vue'),
    meta: { title: '消息中心', requiresAuth: true, roles: ['user'] },
  },
  {
    path: '/order/confirm',
    name: 'order-confirm',
    component: () => import('@/views/client/OrderConfirm/index.vue'),
    meta: { title: '确认订单', requiresAuth: true, roles: ['user'] },
  },
  {
    path: '/pay/result',
    name: 'pay-result',
    component: () => import('@/views/client/PayResult/index.vue'),
    meta: { title: '支付结果', requiresAuth: true, roles: ['user'] },
  },
  ...[
    ['orders', '我的订单', 'orders'],
    ['recharge', '账户充值', 'recharge'],
    ['favorites', '我的收藏', 'favorites'],
    ['reviews', '我的评价', 'reviews'],
    ['profile', '个人信息', 'profile'],
    ['security', '账户安全', 'security'],
  ].map(([path, title, section]) => ({
    path: `/user/${path}`,
    name: `user-${path}`,
    component: () => import('@/views/client/UserCenter/index.vue'),
    meta: { title, section, requiresAuth: true, roles: ['user'] },
  })),
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/views/client/Forbidden/index.vue'),
    meta: { title: '无权访问' },
  },
]
