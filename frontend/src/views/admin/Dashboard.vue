<template>
  <div class="admin-layout">
    <el-container>
      <el-aside width="236px" class="admin-aside">
        <div class="brand-panel">
          <div class="brand-badge">PINGPONG CMS</div>
          <h2>综合管理台</h2>
          <p>围绕内容审核、运营洞察与装备维护的后台工作区</p>
        </div>

        <el-menu
          :default-active="activeMenu"
          class="admin-menu"
          background-color="transparent"
          text-color="#d7e7de"
          active-text-color="#ffffff"
          router
        >
          <el-menu-item index="/admin/overview">
            <el-icon><DataBoard /></el-icon>
            <span>数据看板</span>
          </el-menu-item>
          <el-menu-item index="/admin/comment">
            <el-icon><ChatLineSquare /></el-icon>
            <span>评论管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/equipment">
            <el-icon><Box /></el-icon>
            <span>装备管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/category">
            <el-icon><Collection /></el-icon>
            <span>分类管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/user">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header class="admin-header">
          <div>
            <div class="header-title">{{ currentTitle }}</div>
            <div class="header-subtitle">实时掌握平台内容状态与运营数据</div>
          </div>

          <div class="header-actions">
            <el-button plain @click="router.push('/')">返回前台</el-button>
            <el-dropdown>
              <span class="user-entry">
                {{ userDisplayName }}
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <el-main class="admin-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowDown,
  Box,
  ChatLineSquare,
  Collection,
  DataBoard,
  User
} from '@element-plus/icons-vue'
import { clearAuthState, getStoredUserInfo } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const userInfo = computed(() => getStoredUserInfo())
const userDisplayName = computed(() => userInfo.value.nickname || userInfo.value.username || '管理员')
const activeMenu = computed(() => {
  if (route.path === '/admin') {
    return '/admin/overview'
  }
  return route.path
})
const currentTitle = computed(() => route.meta.title || '后台管理')

const handleLogout = () => {
  clearAuthState()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped lang="scss">
.admin-layout {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(255, 142, 61, 0.18), transparent 28%),
    linear-gradient(180deg, #f5f8f3 0%, #eef3ed 100%);

  :deep(.el-container) {
    min-height: 100vh;
  }
}

.admin-aside {
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  background:
    radial-gradient(circle at top, rgba(255, 138, 61, 0.18), transparent 20%),
    linear-gradient(180deg, #0f5132 0%, #133f2d 100%);
  color: #fff;
}

.brand-panel {
  padding: 28px 24px 18px;

  h2 {
    margin: 10px 0 8px;
    color: #fff7eb;
    font-size: 26px;
    line-height: 1.15;
  }

  p {
    margin: 0;
    color: rgba(240, 246, 241, 0.8);
    font-size: 13px;
    line-height: 1.7;
  }
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  color: #fff7eb;
  font-size: 12px;
  letter-spacing: 0.08em;
}

.admin-menu {
  border-right: none;
  padding: 12px 12px 18px;

  :deep(.el-menu-item) {
    height: 48px;
    margin-bottom: 8px;
    border-radius: 14px;
  }

  :deep(.el-menu-item.is-active) {
    background: linear-gradient(135deg, #ff8e3d 0%, #ffb067 100%);
    box-shadow: 0 12px 28px rgba(255, 142, 61, 0.28);
  }

  :deep(.el-icon) {
    margin-right: 10px;
  }
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 28px;
  background: rgba(255, 255, 255, 0.82);
  border-bottom: 1px solid rgba(12, 76, 53, 0.08);
  backdrop-filter: blur(14px);
}

.header-title {
  color: #163427;
  font-size: 24px;
  font-weight: 700;
}

.header-subtitle {
  margin-top: 4px;
  color: #6b7d72;
  font-size: 13px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-entry {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #274939;
  font-weight: 600;
}

.admin-main {
  padding: 24px 28px 32px;
}

@media (max-width: 960px) {
  .admin-layout {
    :deep(.el-container) {
      flex-direction: column;
    }
  }

  .admin-aside {
    width: 100% !important;
  }

  .admin-menu {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    padding-top: 0;

    :deep(.el-menu-item) {
      width: calc(50% - 4px);
      margin-bottom: 0;
    }
  }

  .admin-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
  }
}

@media (max-width: 640px) {
  .admin-main {
    padding: 16px;
  }

  .admin-menu :deep(.el-menu-item) {
    width: 100%;
  }
}
</style>
