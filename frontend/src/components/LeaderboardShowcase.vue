<template>
  <section class="leaderboard-showcase">
    <div class="showcase-shell">
      <div class="showcase-head">
        <div>
          <div class="eyebrow">口碑智能榜单</div>
          <h2>真实互动驱动的装备排行榜</h2>
          <p>每天自动刷新收藏、评论和评分数据，让热门趋势与高口碑装备更快被看见。</p>
        </div>
        <el-button class="overview-button" @click="goToCenter()">
          查看完整榜单
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>

      <div class="board-tabs">
        <button
          v-for="board in boards"
          :key="board.boardType"
          type="button"
          :class="['board-tab', { active: activeBoardType === board.boardType }]"
          @click="activeBoardType = board.boardType"
        >
          <span class="board-tab-title">{{ board.boardName }}</span>
          <span class="board-tab-subtitle">{{ board.subtitle }}</span>
        </button>
      </div>

      <div class="showcase-body" v-loading="loading">
        <template v-if="activeBoard">
          <div class="hero-board">
            <div class="board-intro">
              <div class="board-badge">{{ activeBoard.boardName }}</div>
              <h3>{{ activeBoard.subtitle }}</h3>
              <p>前 10 名装备由综合口碑得分实时排序，兼顾热度、评分与新鲜度。</p>
              <div class="board-actions">
                <el-button type="primary" @click="goToCenter(activeBoard.boardType)">
                  进入榜单
                </el-button>
                <el-button @click="openShare(activeBoard.boardType, activeBoard.boardName)">
                  分享榜单
                </el-button>
              </div>
            </div>

            <div class="top-card" v-if="activeBoard.items?.length">
              <div class="top-rank">TOP 1</div>
              <h4>{{ activeBoard.items[0].name }}</h4>
              <p>{{ activeBoard.items[0].boardReason }}</p>
              <div class="top-meta">
                <span>综合得分 {{ formatScore(activeBoard.items[0].totalScore) }}</span>
                <span>评分 {{ formatScore(activeBoard.items[0].avgRating) }}</span>
              </div>
              <div class="top-actions">
                <el-button type="primary" plain @click="goToEquipment(activeBoard.items[0].equipmentId)">
                  查看装备
                </el-button>
                <el-button @click="openShare(activeBoard.boardType, activeBoard.boardName, activeBoard.items[0])">
                  分享装备
                </el-button>
              </div>
            </div>
          </div>

          <div class="board-list">
            <button
              v-for="item in activeBoard.items?.slice(0, 6)"
              :key="item.equipmentId"
              type="button"
              class="board-item"
              @click="goToEquipment(item.equipmentId)"
            >
              <div class="rank-pill">{{ item.rankPosition }}</div>
              <div class="item-main">
                <div class="item-title">{{ item.name }}</div>
                <div class="item-meta">
                  <span>{{ item.brand || '品牌待补充' }}</span>
                  <span v-if="item.model">{{ item.model }}</span>
                </div>
                <div class="item-reason">{{ item.boardReason }}</div>
              </div>
              <div class="item-score">
                <span class="score-value">{{ formatScore(item.totalScore) }}</span>
                <span class="score-label">综合得分</span>
              </div>
            </button>
          </div>
        </template>
      </div>
    </div>

    <LeaderboardShareDialog
      v-model="shareVisible"
      :board-type="shareState.boardType"
      :board-name="shareState.boardName"
      :equipment-id="shareState.equipmentId"
      :equipment-name="shareState.equipmentName"
    />
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import { getHomeLeaderboards } from '@/api/leaderboard'
import LeaderboardShareDialog from '@/components/LeaderboardShareDialog.vue'
import { isRealtimeEvent, subscribeRealtimeUpdates } from '@/utils/realtime'

const router = useRouter()
const loading = ref(false)
const boards = ref([])
const activeBoardType = ref('')
const shareVisible = ref(false)
const shareState = reactive({
  boardType: '',
  boardName: '',
  equipmentId: null,
  equipmentName: ''
})

let stopRealtime = null
let refreshTimer = null

const activeBoard = computed(() =>
  boards.value.find((board) => board.boardType === activeBoardType.value) || boards.value[0]
)

const loadBoards = async () => {
  loading.value = true
  try {
    const res = await getHomeLeaderboards({ limit: 10 })
    boards.value = res.data || []
    activeBoardType.value = boards.value[0]?.boardType || ''
  } finally {
    loading.value = false
  }
}

const goToCenter = (boardType) => {
  router.push({
    path: '/leaderboards',
    query: boardType ? { boardType } : {}
  })
}

const goToEquipment = (equipmentId) => {
  router.push(`/equipment/${equipmentId}`)
}

const openShare = (boardType, boardName, item = null) => {
  shareState.boardType = boardType
  shareState.boardName = boardName
  shareState.equipmentId = item?.equipmentId ?? null
  shareState.equipmentName = item?.name ?? ''
  shareVisible.value = true
}

const formatScore = (value) => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '--'
  }
  return Number(value).toFixed(1)
}

onMounted(async () => {
  await loadBoards()
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
  refreshTimer = window.setTimeout(() => {
    loadBoards()
  }, 250)
}
</script>

<style scoped lang="scss">
.leaderboard-showcase {
  padding: 0 24px 44px;
}

.showcase-shell {
  max-width: 1320px;
  margin: 0 auto;
  padding: 30px;
  border-radius: 36px;
  background:
    radial-gradient(circle at top left, rgba(242, 140, 40, 0.16), transparent 32%),
    radial-gradient(circle at bottom right, rgba(16, 59, 43, 0.18), transparent 36%),
    linear-gradient(165deg, #fffef8 0%, #f5f9f1 100%);
  box-shadow: 0 30px 60px rgba(16, 59, 43, 0.08);
}

.showcase-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 7px 14px;
  border-radius: 999px;
  background: rgba(16, 59, 43, 0.08);
  color: #145c36;
  font-size: 13px;
  letter-spacing: 0.08em;
}

.showcase-head h2 {
  margin: 14px 0 10px;
  font-size: clamp(30px, 4vw, 42px);
  color: #103b2b;
}

.showcase-head p {
  margin: 0;
  max-width: 720px;
  color: #627066;
  line-height: 1.8;
}

.overview-button {
  border-radius: 999px;
  border-color: rgba(16, 59, 43, 0.16);
  color: #145c36;
}

.board-tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.board-tab {
  padding: 18px;
  border: 1px solid rgba(16, 59, 43, 0.12);
  border-radius: 24px;
  background: #ffffff;
  text-align: left;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  cursor: pointer;
}

.board-tab.active {
  transform: translateY(-2px);
  border-color: rgba(242, 140, 40, 0.55);
  box-shadow: 0 18px 36px rgba(16, 59, 43, 0.08);
}

.board-tab-title {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: #103b2b;
}

.board-tab-subtitle {
  display: block;
  margin-top: 8px;
  color: #66756c;
  line-height: 1.6;
}

.showcase-body {
  min-height: 360px;
}

.hero-board {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 18px;
  margin-bottom: 18px;
}

.board-intro,
.top-card {
  padding: 24px;
  border-radius: 28px;
}

.board-intro {
  background:
    linear-gradient(135deg, rgba(16, 59, 43, 0.96), rgba(17, 91, 54, 0.92)),
    #103b2b;
  color: #ffffff;
}

.board-badge {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: #ffe6be;
}

.board-intro h3 {
  margin: 16px 0 10px;
  font-size: 28px;
}

.board-intro p {
  margin: 0;
  color: rgba(255, 255, 255, 0.84);
  line-height: 1.9;
}

.board-actions,
.top-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 20px;
}

.top-card {
  background:
    radial-gradient(circle at top right, rgba(242, 140, 40, 0.2), transparent 36%),
    #ffffff;
  border: 1px solid rgba(16, 59, 43, 0.08);
}

.top-rank {
  display: inline-flex;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(242, 140, 40, 0.16);
  color: #b66511;
  font-weight: 700;
}

.top-card h4 {
  margin: 18px 0 10px;
  font-size: 30px;
  color: #103b2b;
}

.top-card p {
  margin: 0;
  color: #5f6e65;
  line-height: 1.8;
}

.top-meta {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  margin-top: 18px;
  color: #145c36;
  font-weight: 600;
}

.board-list {
  display: grid;
  gap: 12px;
}

.board-item {
  display: grid;
  grid-template-columns: 62px minmax(0, 1fr) 116px;
  gap: 16px;
  align-items: center;
  padding: 18px 20px;
  border: 1px solid rgba(16, 59, 43, 0.08);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.82);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.board-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 36px rgba(16, 59, 43, 0.08);
}

.rank-pill {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  background: linear-gradient(135deg, #f28c28, #f6b15b);
  color: #fff;
  font-size: 18px;
  font-weight: 700;
}

.item-title {
  font-size: 18px;
  font-weight: 700;
  color: #103b2b;
}

.item-meta {
  display: flex;
  gap: 10px;
  margin-top: 8px;
  color: #6b786f;
}

.item-reason {
  margin-top: 8px;
  color: #4f5e56;
}

.item-score {
  text-align: right;
}

.score-value {
  display: block;
  font-size: 28px;
  font-weight: 800;
  color: #103b2b;
}

.score-label {
  color: #6b786f;
}

@media (max-width: 1100px) {
  .board-tabs {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero-board {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .leaderboard-showcase {
    padding: 0 16px 28px;
  }

  .showcase-shell {
    padding: 22px 18px;
    border-radius: 28px;
  }

  .showcase-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .board-tabs {
    grid-template-columns: 1fr;
  }

  .board-item {
    grid-template-columns: 48px minmax(0, 1fr);
  }

  .item-score {
    text-align: left;
    grid-column: 2;
  }
}
</style>
