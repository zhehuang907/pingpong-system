<template>
  <div class="kickoff-page">
    <Header />

    <main class="kickoff-main" v-loading="loading">
      <section class="hero-panel">
        <div class="hero-copy">
          <div class="hero-badge">{{ overview?.season?.seasonName || '开球网积分赛事' }}</div>
          <h1>{{ overview?.season?.heroTitle || '把开球网积分走势、分站焦点和装备路线串成一条决策链' }}</h1>
          <p>
            {{
              overview?.season?.heroSubtitle
                || '这里把城市分站热度、选手风格和装备专题放进同一页，方便你边看赛事边完成筛选。'
            }}
          </p>
          <div v-if="overview?.season?.highlightTags?.length" class="hero-tags">
            <span v-for="tag in overview.season.highlightTags" :key="tag">{{ tag }}</span>
          </div>
          <div class="hero-actions">
            <el-button
              v-if="overview?.personalizedTrack?.recommendedBoardType"
              type="primary"
              @click="goToBoard(overview.personalizedTrack.recommendedBoardType)"
            >
              去看 {{ overview?.personalizedTrack?.recommendedBoardName || '榜单' }}
            </el-button>
            <el-button
              v-if="overview?.personalizedTrack?.equipmentKeyword"
              @click="goToEquipments(overview.personalizedTrack.equipmentKeyword)"
            >
              查看专题装备
            </el-button>
          </div>
        </div>

        <div class="hero-side">
          <div class="metric-card">
            <span>当前分站</span>
            <strong>{{ overview?.season?.currentStopName || '--' }}</strong>
          </div>
          <div class="metric-card">
            <span>赛季进度</span>
            <strong>{{ overview?.season?.progressPercent || 0 }}%</strong>
          </div>
          <div class="metric-card track-card">
            <span>画像路线</span>
            <strong>{{ overview?.personalizedTrack?.title || '先完成画像问答' }}</strong>
            <p>
              {{
                overview?.personalizedTrack?.description
                  || '登录后会把更适合你的赛事路线、榜单频道和装备路线串联起来。'
              }}
            </p>
          </div>
        </div>
      </section>

      <section class="section-panel">
        <div class="section-head">
          <div>
            <div class="section-kicker">积分榜</div>
            <h2>争冠组与追分组的最新位置</h2>
          </div>
        </div>
        <div class="standing-grid">
          <article v-for="item in overview?.standings || []" :key="item.rankPosition" class="standing-card">
            <div class="standing-top">
              <span class="rank-chip">#{{ item.rankPosition }}</span>
              <span class="point-chip">{{ item.points || 0 }} 积分</span>
            </div>
            <h3>{{ item.playerName }}</h3>
            <p>{{ item.associationName || '开球网赛事联盟' }}</p>
            <div class="standing-meta">
              <span>分站冠军 {{ item.stageWins || 0 }}</span>
              <span>领奖台 {{ item.podiumCount || 0 }}</span>
            </div>
            <div class="standing-note">{{ item.serveStyle }}</div>
            <div class="standing-form">{{ item.hotForm }}</div>
          </article>
        </div>
      </section>

      <section class="section-panel">
        <div class="section-head">
          <div>
            <div class="section-kicker">分站主题</div>
            <h2>每一站都对应不同的技战术焦点</h2>
          </div>
        </div>
        <div class="stage-grid">
          <article v-for="stage in overview?.stages || []" :key="stage.id" class="stage-card">
            <div class="stage-top">
              <span class="stage-status">{{ stage.stageStatus }}</span>
              <span>{{ stage.stageDate }}</span>
            </div>
            <h3>{{ stage.stageName }} · {{ stage.cityName }}</h3>
            <strong>{{ stage.theme }}</strong>
            <p>{{ stage.highlightDesc }}</p>
            <div class="stage-focus">焦点能力：{{ stage.focusSkill }}</div>
          </article>
        </div>
      </section>

      <section class="section-panel">
        <div class="section-head">
          <div>
            <div class="section-kicker">赛事联动榜单</div>
            <h2>用榜单把观赛热度转成更可执行的选择</h2>
          </div>
        </div>
        <div class="board-grid">
          <article v-for="board in overview?.featuredBoards || []" :key="board.boardType" class="board-card">
            <div class="board-type">{{ board.boardName }}</div>
            <h3>{{ board.subtitle }}</h3>
            <p>{{ board.items?.[0]?.boardReason || '根据当前分站热点和真实互动持续刷新。' }}</p>
            <div v-if="board.items?.[0]" class="board-highlight">
              <strong>{{ board.items[0].name }}</strong>
              <span>TOP1 当前原因：{{ board.items[0].boardReason }}</span>
            </div>
            <el-button type="primary" plain @click="goToBoard(board.boardType)">进入频道</el-button>
          </article>
        </div>
      </section>

      <section class="section-panel">
        <div class="section-head">
          <div>
            <div class="section-kicker">赛事装备专题</div>
            <h2>围绕当前分站主题继续补足装备认知</h2>
          </div>
        </div>
        <div class="equipment-grid">
          <div v-for="item in overview?.featuredEquipments || []" :key="item.id" class="equipment-item">
            <EquipmentCard :equipment="item" />
          </div>
        </div>
      </section>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import EquipmentCard from '@/components/EquipmentCard.vue'
import { getKickoffKingOverview } from '@/api/kickoffKing'
import { isRealtimeEvent, subscribeRealtimeUpdates } from '@/utils/realtime'

const router = useRouter()
const loading = ref(false)
const overview = ref(null)

let stopRealtime = null
let refreshTimer = null

onMounted(async () => {
  await loadOverview()
  stopRealtime = subscribeRealtimeUpdates((message) => {
    if (!isRealtimeEvent(message, ['leaderboard_refreshed', 'kickoff_king_updated', 'equipment_changed'])) {
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

async function loadOverview() {
  loading.value = true
  try {
    const res = await getKickoffKingOverview()
    overview.value = res.data || null
  } catch (error) {
    overview.value = null
  } finally {
    loading.value = false
  }
}

function scheduleRefresh() {
  if (refreshTimer) {
    window.clearTimeout(refreshTimer)
  }
  refreshTimer = window.setTimeout(() => {
    loadOverview()
  }, 250)
}

function goToBoard(boardType) {
  router.push({
    path: '/leaderboards',
    query: boardType ? { boardType } : {}
  })
}

function goToEquipments(keyword) {
  router.push({
    path: '/equipment-list',
    query: keyword ? { keyword } : {}
  })
}
</script>

<style scoped lang="scss">
.kickoff-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(255, 122, 39, 0.12), transparent 28%),
    radial-gradient(circle at bottom right, rgba(16, 59, 43, 0.1), transparent 24%),
    linear-gradient(180deg, #f7faff 0%, #ffffff 100%);
}

.kickoff-main {
  max-width: 1320px;
  margin: 0 auto;
  padding: 28px 20px 40px;
}

.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.75fr);
  gap: 18px;
  padding: 28px;
  border-radius: 34px;
  background:
    radial-gradient(circle at top right, rgba(255, 122, 39, 0.22), transparent 32%),
    linear-gradient(135deg, rgba(16, 59, 43, 0.98), rgba(26, 92, 58, 0.92));
  color: #fff;
}

.hero-badge,
.section-kicker {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: #ffe6be;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.hero-copy h1,
.section-head h2 {
  margin: 16px 0 12px;
}

.hero-copy p {
  margin: 0;
  max-width: 680px;
  color: rgba(255, 255, 255, 0.82);
  line-height: 1.85;
}

.hero-tags,
.hero-actions,
.standing-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.hero-tags {
  margin-top: 18px;
}

.hero-tags span,
.stage-status,
.point-chip,
.rank-chip,
.board-type {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.hero-actions {
  margin-top: 20px;
}

.hero-side {
  display: grid;
  gap: 14px;
}

.metric-card {
  padding: 18px;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.12);
}

.metric-card span {
  display: block;
  color: rgba(255, 255, 255, 0.72);
  font-size: 13px;
}

.metric-card strong {
  display: block;
  margin-top: 10px;
  font-size: 28px;
}

.metric-card p {
  margin: 10px 0 0;
  color: rgba(255, 255, 255, 0.78);
  line-height: 1.7;
}

.section-panel {
  margin-top: 24px;
  padding: 24px;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(18, 38, 93, 0.08);
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}

.standing-grid,
.stage-grid,
.board-grid,
.equipment-grid {
  display: grid;
  gap: 16px;
  margin-top: 18px;
}

.standing-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.stage-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.board-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.equipment-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.standing-card,
.stage-card,
.board-card {
  padding: 20px;
  border-radius: 22px;
  background: rgba(18, 38, 93, 0.04);
}

.standing-top,
.stage-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.standing-card h3,
.stage-card h3,
.board-card h3 {
  margin: 14px 0 8px;
  color: #13265d;
}

.standing-card p,
.stage-card p,
.board-card p,
.standing-note,
.standing-form,
.stage-focus,
.board-highlight span {
  color: #60708f;
  line-height: 1.75;
}

.board-highlight {
  margin: 14px 0;
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(255, 122, 39, 0.08);
}

@media (max-width: 1200px) {
  .standing-grid,
  .board-grid,
  .equipment-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .hero-panel,
  .standing-grid,
  .stage-grid,
  .board-grid,
  .equipment-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .kickoff-main {
    padding: 20px 16px 32px;
  }

  .hero-panel,
  .section-panel {
    padding: 20px;
  }
}
</style>
