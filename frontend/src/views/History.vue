<template>
  <div class="history-page">
    <Header />

    <main class="main-content">
      <section class="page-hero">
        <div>
          <div class="page-kicker">浏览历史</div>
          <h1>完整记录你的浏览行为</h1>
          <p>这里会保留最近的访问时间、停留时长、浏览内容类型以及关键交互动作，方便回看和继续比较。</p>
        </div>
        <div class="hero-actions" v-if="historyList.length">
          <el-button @click="$router.push('/equipment-list')">继续浏览</el-button>
          <el-button type="danger" plain @click="handleClearHistory">清空历史</el-button>
        </div>
      </section>

      <section class="history-shell" v-loading="loading">
        <el-empty v-if="!loading && !historyList.length" description="还没有浏览记录">
          <el-button type="primary" @click="$router.push('/equipment-list')">去看看器材</el-button>
        </el-empty>

        <div v-else class="history-grid">
          <article v-for="item in historyList" :key="item.id" class="history-card">
            <el-image
              :src="getEquipmentImageUrl(item.equipment)"
              fit="cover"
              class="history-image"
              @click="$router.push(`/equipment/${item.equipmentId}`)"
            />

            <div class="history-content">
              <div class="history-main">
                <h3 class="equipment-name" @click="$router.push(`/equipment/${item.equipmentId}`)">
                  {{ item.equipment?.name || item.equipmentName || `器材 #${item.equipmentId}` }}
                </h3>
                <p class="equipment-meta">
                  {{ item.equipment?.brand || '未知品牌' }}
                  <span v-if="item.equipment?.model"> · {{ item.equipment.model }}</span>
                </p>

                <div class="history-metrics">
                  <span>{{ formatHistoryType(item.contentType) }}</span>
                  <span>{{ formatDuration(item.durationSeconds) }}</span>
                  <span>进入：{{ formatTime(item.enterTime || item.createTime) }}</span>
                  <span>离开：{{ formatTime(item.leaveTime || item.updateTime) }}</span>
                </div>

                <div v-if="item.interactionActionList?.length" class="history-tags">
                  <el-tag
                    v-for="action in item.interactionActionList"
                    :key="action"
                    size="small"
                    effect="plain"
                    round
                  >
                    {{ actionLabelMap[action] || action }}
                  </el-tag>
                </div>
              </div>

              <div class="history-actions">
                <div class="price">¥{{ formatPrice(item.equipment?.price) }}</div>
                <el-button @click="$router.push(`/equipment/${item.equipmentId}`)">查看详情</el-button>
                <el-button text type="danger" @click="handleDeleteHistory(item.id)">删除记录</el-button>
              </div>
            </div>
          </article>
        </div>
      </section>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { clearUserHistory, deleteHistoryItem, getUserHistory } from '@/api/history'
import Footer from '@/components/Footer.vue'
import Header from '@/components/Header.vue'

const actionLabelMap = {
  view_detail: '进入详情',
  switch_image: '切换图片',
  favorite: '收藏操作',
  add_to_cart: '加入购物车',
  write_comment: '打开评论',
  submit_comment: '提交评论',
  route_leave: '离开页面',
  unload: '关闭页面'
}

const loading = ref(false)
const historyList = ref([])

function getEquipmentImageUrl(equipment) {
  // 优先使用 mainImage，其次使用 imageUrl，最后使用占位图
  return equipment?.mainImage || equipment?.imageUrl || 'data:image/svg+xml;charset=UTF-8,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 300 220"%3E%3Crect width="300" height="220" fill="%23eef2f7"/%3E%3Ccircle cx="150" cy="86" r="34" fill="%23cbd5e1"/%3E%3Cpath d="M84 178c18-28 42-42 66-42s48 14 66 42" fill="%23dbe3ec"/%3E%3C/svg%3E'
}

onMounted(loadHistory)

async function loadHistory() {
  loading.value = true
  try {
    const res = await getUserHistory({ limit: 50, deduplicate: false })
    historyList.value = res.data || []
  } catch (error) {
    ElMessage.error(error?.message || '浏览历史加载失败')
  } finally {
    loading.value = false
  }
}

async function handleDeleteHistory(historyId) {
  try {
    await deleteHistoryItem(historyId)
    historyList.value = historyList.value.filter(item => item.id !== historyId)
    ElMessage.success('浏览记录已删除')
  } catch (error) {
    ElMessage.error(error?.message || '删除失败')
  }
}

async function handleClearHistory() {
  try {
    await ElMessageBox.confirm('清空后将无法恢复，确定继续吗？', '清空确认', {
      type: 'warning'
    })
    await clearUserHistory()
    historyList.value = []
    ElMessage.success('浏览历史已清空')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '清空失败')
    }
  }
}

function formatPrice(value) {
  return Number(value || 0).toFixed(2)
}

function formatDuration(value) {
  const seconds = Number(value || 0)
  if (!seconds) {
    return '停留不足 1 秒'
  }
  if (seconds < 60) {
    return `停留 ${seconds} 秒`
  }
  const minutes = Math.floor(seconds / 60)
  const restSeconds = seconds % 60
  return `停留 ${minutes} 分 ${restSeconds} 秒`
}

function formatHistoryType(type) {
  const labelMap = {
    equipment_detail: '器材详情',
    compare: '对比页',
    equipment_list: '列表页'
  }
  return labelMap[type] || '浏览记录'
}

function formatTime(value) {
  if (!value) {
    return '-'
  }
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss')
}
</script>

<style scoped lang="scss">
.history-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(240, 138, 36, 0.12), transparent 24%),
    radial-gradient(circle at bottom right, rgba(29, 88, 70, 0.12), transparent 28%),
    linear-gradient(180deg, #f4efe6 0%, #f4f8f1 100%);
}

.main-content {
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 20px 40px;
}

.page-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
  padding: 28px 32px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 22px 48px rgba(23, 53, 46, 0.08);
}

.page-hero h1 {
  margin: 8px 0 6px;
  font-size: 30px;
  color: #17352e;
}

.page-hero p {
  margin: 0;
  line-height: 1.7;
  color: #66766d;
}

.page-kicker {
  color: #d96b1f;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.history-grid {
  display: grid;
  gap: 16px;
}

.history-card {
  display: grid;
  grid-template-columns: 170px minmax(0, 1fr);
  gap: 18px;
  padding: 18px;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 38px rgba(23, 53, 46, 0.08);
}

.history-image {
  width: 170px;
  height: 150px;
  border-radius: 20px;
  cursor: pointer;
  background: #eef3ee;
}

.history-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.history-main {
  min-width: 0;
}

.equipment-name {
  margin: 0;
  font-size: 22px;
  color: #17352e;
  cursor: pointer;
}

.equipment-meta {
  margin: 10px 0 0;
  color: #69786f;
}

.history-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
  color: #69786f;
  font-size: 13px;
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.history-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.price {
  font-size: 28px;
  font-weight: 800;
  color: #f26f22;
}

@media (max-width: 768px) {
  .main-content {
    padding: 18px 14px 32px;
  }

  .page-hero,
  .history-card {
    grid-template-columns: 1fr;
  }

  .page-hero {
    padding: 22px 20px;
  }

  .hero-actions,
  .history-content {
    width: 100%;
    flex-wrap: wrap;
  }

  .history-content,
  .history-actions {
    align-items: flex-start;
  }

  .history-image {
    width: 100%;
    height: 220px;
  }
}
</style>
