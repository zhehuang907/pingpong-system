<template>
  <div class="leaderboard-center-page">
    <Header />

    <main class="leaderboard-center">
      <section class="hero-panel">
        <div class="hero-copy">
          <div class="hero-badge">口碑榜单中心</div>
          <h1>用真实用户互动，帮你更快找到靠谱装备</h1>
          <p>
            榜单综合收藏、评论、评分和时间衰减因子自动计算，并提供分享、订阅与筛选能力，让每次决策都更有依据。
          </p>
        </div>

        <div class="hero-metrics">
          <div class="metric-card">
            <span class="metric-value">{{ detailBoard.items?.length || 0 }}</span>
            <span class="metric-label">当前展示 Top</span>
          </div>
          <div class="metric-card">
            <span class="metric-value">{{ shareSummary.totalShares || 0 }}</span>
            <span class="metric-label">累计分享</span>
          </div>
          <div class="metric-card">
            <span class="metric-value">{{ activeSubscription ? '已订阅' : '可订阅' }}</span>
            <span class="metric-label">更新提醒</span>
          </div>
        </div>
      </section>

      <section class="toolbar">
        <div class="board-switcher">
          <button
            v-for="board in boardOptions"
            :key="board.value"
            type="button"
            :class="['board-switch', { active: activeBoardType === board.value }]"
            @click="switchBoard(board.value)"
          >
            <span>{{ board.label }}</span>
            <small>{{ board.tip }}</small>
          </button>
        </div>

        <div class="tool-actions">
          <el-segmented v-model="sortBy" :options="sortOptions" @change="loadBoard" />
          <el-button type="primary" @click="openBoardShare">分享榜单</el-button>
          <el-button
            v-if="isLoggedIn"
            :type="activeSubscription ? 'success' : 'default'"
            @click="toggleSubscription"
          >
            {{ activeSubscription ? '已订阅更新' : '订阅榜单' }}
          </el-button>
        </div>
      </section>

      <section class="summary-panel">
        <div class="summary-card">
          <span class="summary-label">排序方式</span>
          <strong>{{ sortLabel }}</strong>
        </div>
        <div class="summary-card">
          <span class="summary-label">更新时间</span>
          <strong>{{ formatTime(detailBoard.refreshedAt) }}</strong>
        </div>
        <div class="summary-card">
          <span class="summary-label">主要渠道</span>
          <strong>{{ dominantChannel }}</strong>
        </div>
      </section>

      <section class="board-content" v-loading="loading">
        <div
          v-for="item in detailBoard.items || []"
          :key="item.equipmentId"
          class="leaderboard-card"
        >
          <div class="rank-side">
            <div class="rank-number">#{{ item.rankPosition }}</div>
            <div class="rank-score">{{ formatScore(item.totalScore) }}</div>
            <div class="rank-score-label">综合得分</div>
          </div>

          <div class="card-main">
            <div class="card-header">
              <div>
                <h3>{{ item.name }}</h3>
                <p>{{ item.brand || '品牌待补充' }}<span v-if="item.model"> · {{ item.model }}</span></p>
              </div>
              <div class="reason-chip">{{ item.boardReason }}</div>
            </div>

            <div class="meta-grid">
              <div class="meta-item">
                <span class="meta-label">评分</span>
                <span class="meta-value">{{ formatScore(item.avgRating) }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">收藏</span>
                <span class="meta-value">{{ item.favoriteCount || 0 }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">评论</span>
                <span class="meta-value">{{ item.commentCount || 0 }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">满意度</span>
                <span class="meta-value">{{ formatPercent(item.satisfactionRate) }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">适用人群</span>
                <span class="meta-value">{{ item.suitableFor || '通用' }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">参考价格</span>
                <span class="meta-value">¥{{ item.price || '--' }}</span>
              </div>
            </div>

            <div class="card-actions">
              <el-button type="primary" plain @click="goToEquipment(item.equipmentId)">查看装备</el-button>
              <el-button @click="openItemShare(item)">分享该装备</el-button>
            </div>
          </div>
        </div>

        <el-empty v-if="!loading && !(detailBoard.items || []).length" description="暂无榜单数据" />
      </section>
    </main>

    <LeaderboardShareDialog
      v-model="shareVisible"
      :board-type="shareState.boardType"
      :board-name="shareState.boardName"
      :equipment-id="shareState.equipmentId"
      :equipment-name="shareState.equipmentName"
      @shared="loadShareStats"
    />

    <Footer />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import LeaderboardShareDialog from '@/components/LeaderboardShareDialog.vue'
import {
  getLeaderboardDetail,
  getLeaderboardShareStats,
  getLeaderboardSubscriptions,
  subscribeLeaderboard,
  unsubscribeLeaderboard
} from '@/api/leaderboard'
import { getToken } from '@/utils/auth'
import { isRealtimeEvent, subscribeRealtimeUpdates } from '@/utils/realtime'

const boardOptions = [
  { value: 'weekly_popular', label: '本周人气王', tip: '近 7 天热度综合排序' },
  { value: 'value_king', label: '性价比之王', tip: '价格优势与口碑并重' },
  { value: 'beginner_pick', label: '新手入门首选', tip: '更适合入门阶段上手' },
  { value: 'pro_favorite', label: '专业选手最爱', tip: '偏向进阶与对抗场景' }
]

const sortOptions = [
  { label: '按得分', value: 'score' },
  { label: '按热度', value: 'heat' },
  { label: '按时间', value: 'time' }
]

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const sortBy = ref('score')
const activeBoardType = ref(route.query.boardType || 'weekly_popular')
const detailBoard = ref({ items: [] })
const subscriptions = ref([])
const shareSummary = ref({ totalShares: 0, channels: {} })
const shareVisible = ref(false)
const shareState = reactive({
  boardType: '',
  boardName: '',
  equipmentId: null,
  equipmentName: ''
})

let stopRealtime = null
let refreshTimer = null

const isLoggedIn = computed(() => !!getToken())
const activeSubscription = computed(() =>
  subscriptions.value.some((item) => item.boardType === activeBoardType.value && item.active === 1)
)
const sortLabel = computed(() =>
  sortOptions.find((option) => option.value === sortBy.value)?.label || '按得分'
)
const activeBoardLabel = computed(() =>
  boardOptions.find((option) => option.value === activeBoardType.value)?.label || '智能榜单'
)
const dominantChannel = computed(() => {
  const channels = shareSummary.value.channels || {}
  const sorted = Object.entries(channels).sort((a, b) => b[1] - a[1])
  return sorted[0]?.[0] || '暂无分享数据'
})

const loadBoard = async () => {
  loading.value = true
  try {
    const res = await getLeaderboardDetail(activeBoardType.value, {
      sortBy: sortBy.value,
      limit: 10
    })
    detailBoard.value = res.data || { items: [] }
  } catch (error) {
    detailBoard.value = { items: [] }
    ElMessage.error('榜单数据加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const loadSubscriptions = async () => {
  if (!isLoggedIn.value) {
    subscriptions.value = []
    return
  }

  try {
    const res = await getLeaderboardSubscriptions()
    subscriptions.value = res.data || []
  } catch (error) {
    subscriptions.value = []
  }
}

const loadShareStats = async () => {
  try {
    const res = await getLeaderboardShareStats({ boardType: activeBoardType.value })
    shareSummary.value = res.data || { totalShares: 0, channels: {} }
  } catch (error) {
    shareSummary.value = { totalShares: 0, channels: {} }
  }
}

const switchBoard = (boardType) => {
  activeBoardType.value = boardType
  router.replace({
    path: '/leaderboards',
    query: { boardType }
  })
}

const toggleSubscription = async () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录后再订阅榜单')
    return
  }

  if (activeSubscription.value) {
    await unsubscribeLeaderboard(activeBoardType.value)
    ElMessage.success('已取消订阅当前榜单')
  } else {
    await subscribeLeaderboard(activeBoardType.value)
    ElMessage.success('订阅成功，后续更新会优先提示')
  }
  await loadSubscriptions()
}

const goToEquipment = (equipmentId) => {
  router.push(`/equipment/${equipmentId}`)
}

const openBoardShare = () => {
  shareState.boardType = activeBoardType.value
  shareState.boardName = activeBoardLabel.value
  shareState.equipmentId = null
  shareState.equipmentName = ''
  shareVisible.value = true
}

const openItemShare = (item) => {
  shareState.boardType = activeBoardType.value
  shareState.boardName = activeBoardLabel.value
  shareState.equipmentId = item.equipmentId
  shareState.equipmentName = item.name
  shareVisible.value = true
}

const formatScore = (value) => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '--'
  }
  return Number(value).toFixed(1)
}

const formatPercent = (value) => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '--'
  }
  return `${Number(value).toFixed(0)}%`
}

const formatTime = (value) => {
  if (!value) {
    return '等待首次计算'
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

watch(
  () => route.query.boardType,
  (boardType) => {
    if (boardType && boardType !== activeBoardType.value) {
      activeBoardType.value = boardType
    }
  }
)

watch([activeBoardType, sortBy], async () => {
  await loadBoard()
  await loadShareStats()
})

onMounted(async () => {
  await loadBoard()
  await Promise.allSettled([loadSubscriptions(), loadShareStats()])
  stopRealtime = subscribeRealtimeUpdates((message) => {
    if (!isRealtimeEvent(message, ['leaderboard_refreshed'])) {
      return
    }
    scheduleRefresh()
  })
})

onUnmounted(() => {
  if (stopRealtime) {
    stopRealtime()
  }
  if (refreshTimer) {
    window.clearTimeout(refreshTimer)
  }
})

function scheduleRefresh() {
  if (refreshTimer) {
    window.clearTimeout(refreshTimer)
  }
  refreshTimer = window.setTimeout(async () => {
    await loadBoard()
    await loadShareStats()
  }, 250)
}
</script>

<style scoped lang="scss">
.leaderboard-center-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(242, 140, 40, 0.12), transparent 26%),
    linear-gradient(180deg, #f4f7f1 0%, #eef4ea 100%);
}

.leaderboard-center {
  max-width: 1320px;
  margin: 0 auto;
  padding: 28px 24px 48px;
}

.hero-panel {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 20px;
  padding: 28px;
  border-radius: 34px;
  background:
    radial-gradient(circle at top right, rgba(242, 140, 40, 0.22), transparent 34%),
    linear-gradient(135deg, rgba(16, 59, 43, 0.97), rgba(19, 96, 57, 0.93));
  color: #fff;
}

.hero-badge {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: #ffe7bf;
}

.hero-copy h1 {
  margin: 16px 0 12px;
  font-size: clamp(32px, 5vw, 46px);
}

.hero-copy p {
  margin: 0;
  line-height: 1.9;
  color: rgba(255, 255, 255, 0.84);
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  align-self: end;
}

.metric-card {
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
}

.metric-value {
  display: block;
  font-size: 26px;
  font-weight: 800;
}

.metric-label {
  display: block;
  margin-top: 8px;
  color: rgba(255, 255, 255, 0.72);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  margin-top: 22px;
}

.board-switcher {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.board-switch {
  padding: 16px;
  border: 1px solid rgba(16, 59, 43, 0.1);
  border-radius: 22px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.board-switch.active {
  transform: translateY(-2px);
  box-shadow: 0 18px 36px rgba(16, 59, 43, 0.08);
  border-color: rgba(242, 140, 40, 0.5);
}

.board-switch span {
  display: block;
  font-size: 17px;
  font-weight: 700;
  color: #103b2b;
}

.board-switch small {
  display: block;
  margin-top: 8px;
  color: #66746b;
  line-height: 1.6;
}

.tool-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.summary-panel {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.summary-card {
  padding: 18px 20px;
  border-radius: 22px;
  background: #fff;
  border: 1px solid rgba(16, 59, 43, 0.08);
}

.summary-label {
  display: block;
  color: #7a867f;
  font-size: 13px;
}

.summary-card strong {
  display: block;
  margin-top: 10px;
  font-size: 20px;
  color: #103b2b;
}

.board-content {
  margin-top: 22px;
  display: grid;
  gap: 14px;
}

.leaderboard-card {
  display: grid;
  grid-template-columns: 160px minmax(0, 1fr);
  gap: 18px;
  padding: 22px;
  border-radius: 28px;
  background: #fff;
  border: 1px solid rgba(16, 59, 43, 0.08);
  box-shadow: 0 18px 36px rgba(16, 59, 43, 0.05);
}

.rank-side {
  border-radius: 22px;
  background:
    radial-gradient(circle at top, rgba(242, 140, 40, 0.28), transparent 42%),
    #103b2b;
  color: #fff;
  display: grid;
  place-items: center;
  text-align: center;
  padding: 18px;
}

.rank-number {
  font-size: 32px;
  font-weight: 800;
}

.rank-score {
  margin-top: 10px;
  font-size: 36px;
  font-weight: 800;
  color: #ffe7bf;
}

.rank-score-label {
  margin-top: 6px;
  color: rgba(255, 255, 255, 0.72);
}

.card-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.card-header h3 {
  margin: 0;
  font-size: 28px;
  color: #103b2b;
}

.card-header p {
  margin: 8px 0 0;
  color: #627066;
}

.reason-chip {
  align-self: flex-start;
  padding: 9px 14px;
  border-radius: 999px;
  background: rgba(242, 140, 40, 0.12);
  color: #b76611;
  white-space: nowrap;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.meta-item {
  padding: 14px 16px;
  border-radius: 18px;
  background: #f7faf5;
}

.meta-label {
  display: block;
  color: #758078;
  font-size: 13px;
}

.meta-value {
  display: block;
  margin-top: 8px;
  color: #103b2b;
  font-size: 18px;
  font-weight: 700;
}

.card-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}

@media (max-width: 1100px) {
  .hero-panel,
  .toolbar {
    grid-template-columns: 1fr;
    display: grid;
  }

  .board-switcher {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .leaderboard-center {
    padding: 18px 16px 32px;
  }

  .hero-panel {
    padding: 22px;
  }

  .hero-metrics,
  .summary-panel,
  .meta-grid,
  .board-switcher {
    grid-template-columns: 1fr;
  }

  .leaderboard-card {
    grid-template-columns: 1fr;
  }

  .card-header {
    flex-direction: column;
  }
}
</style>
