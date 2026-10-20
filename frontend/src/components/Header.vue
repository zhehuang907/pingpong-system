<template>
  <header class="header">
    <div class="header-content">
      <div class="logo" @click="router.push('/')">
        <div class="logo-icon">
          <el-icon><Basketball /></el-icon>
        </div>
        <div class="logo-text">
          <span class="brand-name">PingPong</span>
          <span class="brand-sub">装备推荐平台</span>
        </div>
      </div>

      <nav class="nav-links">
        <button type="button" class="nav-link" :class="{ active: isActive('/') }" @click="router.push('/')">首页</button>
        <button type="button" class="nav-link" :class="{ active: isActive('/equipment-list') }" @click="router.push('/equipment-list')">装备库</button>
        <button type="button" class="nav-link" :class="{ active: isActive('/persona-channel') }" @click="router.push('/persona-channel')">画像频道</button>
        <button type="button" class="nav-link" :class="{ active: isActive('/ai-assistant') }" @click="router.push('/ai-assistant')">智能顾问</button>
        <button type="button" class="nav-link" :class="{ active: isActive('/leaderboards') }" @click="router.push('/leaderboards')">智能榜单</button>
        <button type="button" class="nav-link" :class="{ active: isActive('/kaiqiuwang-ranking') }" @click="router.push('/kaiqiuwang-ranking')">开球网赛事</button>
      </nav>

      <div class="search-box">
        <el-input
          v-model="keyword"
          placeholder="搜索装备、品牌、型号"
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" class="search-btn" @click="handleSearch">
          <el-icon><Search /></el-icon>
          <span>搜索</span>
        </el-button>
      </div>

      <div class="user-menu">
        <template v-if="isLoggedIn">
          <el-dropdown trigger="click">
            <div class="user-info">
              <el-avatar :size="36" :src="userInfo.avatar || ''" class="user-avatar">
                {{ userInfo.nickname?.charAt(0) || userInfo.username?.charAt(0) || 'U' }}
              </el-avatar>
              <div class="user-details">
                <span class="user-name">{{ userInfo.nickname || userInfo.username }}</span>
                <el-tag v-if="userInfo.role === 'ADMIN'" size="small" type="danger" effect="dark">管理员</el-tag>
              </div>
              <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
            </div>

            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">
                  <el-icon><User /></el-icon>
                  <span>个人中心</span>
                </el-dropdown-item>
                <el-dropdown-item @click="router.push('/favorite')">
                  <el-icon><Star /></el-icon>
                  <span>我的收藏</span>
                </el-dropdown-item>
                <el-dropdown-item @click="router.push('/cart')">
                  <el-icon><ShoppingCart /></el-icon>
                  <span>购物车</span>
                </el-dropdown-item>
                <el-dropdown-item @click="router.push('/history')">
                  <el-icon><Clock /></el-icon>
                  <span>浏览历史</span>
                </el-dropdown-item>
                <el-dropdown-item @click="router.push('/persona-channel')">
                  <el-icon><CollectionTag /></el-icon>
                  <span>画像频道</span>
                </el-dropdown-item>
                <el-dropdown-item @click="router.push('/ai-assistant')">
                  <el-icon><ChatDotRound /></el-icon>
                  <span>智能顾问</span>
                </el-dropdown-item>
                <el-dropdown-item @click="router.push('/leaderboards')">
                  <el-icon><TrendCharts /></el-icon>
                  <span>智能榜单</span>
                </el-dropdown-item>
                <el-dropdown-item @click="router.push('/kaiqiuwang-ranking')">
                  <el-icon><Basketball /></el-icon>
                  <span>开球网赛事</span>
                </el-dropdown-item>
                <el-dropdown-item v-if="userInfo.role === 'ADMIN'" @click="router.push('/admin')">
                  <el-icon><Setting /></el-icon>
                  <span>后台管理</span>
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  <span>退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>

        <template v-else>
          <el-button class="btn-login" @click="router.push('/login')">
            <el-icon><User /></el-icon>
            <span>登录</span>
          </el-button>
          <el-button type="primary" class="btn-register" @click="router.push('/register')">
            <el-icon><Plus /></el-icon>
            <span>注册</span>
          </el-button>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowDown,
  Basketball,
  ChatDotRound,
  Clock,
  CollectionTag,
  Plus,
  Search,
  Setting,
  ShoppingCart,
  Star,
  SwitchButton,
  TrendCharts,
  User
} from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { userInfo, token } = storeToRefs(userStore)

const keyword = ref('')
const isLoggedIn = computed(() => !!token.value)

function isActive(targetPath) {
  if (targetPath === '/') {
    return route.path === '/'
  }
  return route.path.startsWith(targetPath)
}

function handleSearch() {
  const normalizedKeyword = keyword.value.trim()
  if (!normalizedKeyword) {
    ElMessage.warning('请输入搜索关键词')
    return
  }

  router.push({
    name: 'EquipmentList',
    query: { keyword: normalizedKeyword }
  })
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/')
}
</script>

<style scoped lang="scss">
.header {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: rgba(7, 10, 16, 0.95);
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
}

.header-content {
  max-width: 1440px;
  margin: 0 auto;
  padding: 12px 24px;
  display: grid;
  grid-template-columns: auto auto minmax(320px, 1fr) auto;
  align-items: center;
  gap: 20px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.logo-icon {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(242, 140, 40, 0.2), rgba(255, 255, 255, 0.12));
  color: #fff;
  font-size: 24px;
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.brand-name {
  color: #fff;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 0.03em;
}

.brand-sub {
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
}

.nav-links {
  display: flex;
  gap: 10px;
}

.nav-link {
  padding: 10px 14px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: rgba(255, 255, 255, 0.76);
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.nav-link:hover,
.nav-link.active {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.search-box {
  display: flex;
  gap: 8px;
}

.search-input {
  flex: 1;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: none;
  padding: 7px 16px;
}

.search-btn,
.btn-register {
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #f28c28, #f6b55d);
  color: #fff;
}

.btn-login {
  border-radius: 999px;
  border-color: rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.04);
  color: #fff;
}

.user-menu {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  cursor: pointer;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-name {
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.dropdown-icon {
  color: rgba(255, 255, 255, 0.7);
}

@media (max-width: 1280px) {
  .header-content {
    grid-template-columns: auto minmax(260px, 1fr) auto;
  }

  .nav-links {
    display: none;
  }
}

@media (max-width: 880px) {
  .header-content {
    grid-template-columns: auto 1fr auto;
    gap: 12px;
    padding: 12px 16px;
  }

  .logo-text {
    display: none;
  }

  .search-btn span,
  .btn-login span,
  .btn-register span {
    display: none;
  }

  .user-details {
    display: none;
  }
}
</style>
