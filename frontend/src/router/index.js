import { createRouter, createWebHistory } from 'vue-router'
import { getStoredUserInfo, getToken } from '@/utils/auth'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/equipment/:id',
    name: 'EquipmentDetail',
    component: () => import('@/views/EquipmentDetail.vue'),
    meta: { title: '装备详情' }
  },
  {
    path: '/equipment-list',
    name: 'EquipmentList',
    component: () => import('@/views/EquipmentList.vue'),
    meta: { title: '装备列表' }
  },
  {
    path: '/leaderboards',
    name: 'Leaderboards',
    component: () => import('@/views/LeaderboardCenter.vue'),
    meta: { title: '智能榜单' }
  },
  {
    path: '/kaiqiuwang-ranking',
    name: 'KickoffKing',
    component: () => import('@/views/KickoffKingEvent.vue'),
    meta: { title: '开球网积分赛事' }
  },
  {
    path: '/kickoff-king',
    redirect: '/kaiqiuwang-ranking'
  },
  {
    path: '/compare',
    name: 'Compare',
    component: () => import('@/views/CompareView.vue'),
    meta: { title: '装备对比' }
  },
  {
    path: '/quiz',
    name: 'SuitabilityQuiz',
    component: () => import('@/views/SuitabilityQuiz.vue'),
    meta: { title: '打法适配问答' }
  },
  {
    path: '/favorite',
    name: 'Favorite',
    component: () => import('@/views/Favorite.vue'),
    meta: { title: '我的收藏', requiresAuth: true }
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('@/views/Cart.vue'),
    meta: { title: '我的购物车', requiresAuth: true }
  },
  {
    path: '/history',
    name: 'History',
    component: () => import('@/views/History.vue'),
    meta: { title: '浏览历史', requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '个人中心', requiresAuth: true }
  },
  {
    path: '/persona-channel',
    name: 'PersonaChannel',
    component: () => import('@/views/PersonaChannel.vue'),
    meta: { title: '画像频道', requiresAuth: true }
  },
  {
    path: '/ai-assistant',
    name: 'AiAssistant',
    component: () => import('@/views/AiAssistant.vue'),
    meta: { title: '智能装备顾问', requiresAuth: true }
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/Dashboard.vue'),
    meta: { title: '后台管理', requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: '',
        redirect: '/admin/overview'
      },
      {
        path: 'overview',
        name: 'AdminOverview',
        component: () => import('@/views/admin/AdminOverview.vue'),
        meta: { title: '数据看板' }
      },
      {
        path: 'comment',
        name: 'AdminComment',
        component: () => import('@/views/admin/CommentManage.vue'),
        meta: { title: '评论管理' }
      },
      {
        path: 'equipment',
        name: 'AdminEquipment',
        component: () => import('@/views/admin/EquipmentManage.vue'),
        meta: { title: '装备管理' }
      },
      {
        path: 'category',
        name: 'AdminCategory',
        component: () => import('@/views/admin/CategoryManage.vue'),
        meta: { title: '分类管理' }
      },
      {
        path: 'user',
        name: 'AdminUser',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: { title: '用户管理' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title
    ? `${to.meta.title} - 乒乓球装备推荐系统`
    : '乒乓球装备推荐系统'

  const token = getToken()
  const userInfo = getStoredUserInfo()

  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.requiresAdmin && userInfo.role !== 'ADMIN') {
    next({ name: 'Home' })
    return
  }

  next()
})

export default router
