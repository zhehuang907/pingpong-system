<template>
  <section class="kickoff-showcase" v-loading="loading">
    <div class="showcase-shell">
      <div class="showcase-head">
        <div>
          <div class="eyebrow">赛事拓展业务</div>
          <h2>{{ overview?.season?.seasonName || '开球网积分赛事' }}</h2>
          <p>
            {{
              overview?.season?.heroSubtitle
                || '把积分走势、分站主题和装备路线放进同一个业务场景里，让观赛、选品和推荐真正联动起来。'
            }}
          </p>
        </div>
        <div class="head-actions">
          <el-button type="primary" @click="router.push('/kaiqiuwang-ranking')">进入赛事频道</el-button>
          <el-button
            v-if="overview?.personalizedTrack?.recommendedBoardType"
            @click="goToBoard(overview.personalizedTrack.recommendedBoardType)"
          >
            去看推荐榜单
          </el-button>
        </div>
      </div>

      <div class="hero-grid">
        <article class="season-card">
          <div class="season-badge">{{ overview?.season?.currentStopName || '赛事进行中' }}</div>
          <h3>{{ overview?.season?.heroTitle || '开球网积分走势正在重塑分站关注焦点' }}</h3>
          <p>
            {{
              overview?.season?.announcement
                || '从城市分站热度到器材路线，这里把原本分散的信息收束成一个更容易行动的入口。'
            }}
          </p>
          <div v-if="overview?.season?.highlightTags?.length" class="season-tags">
            <span v-for="tag in overview.season.highlightTags" :key="tag">{{ tag }}</span>
          </div>
          <div class="season-progress">
            <span>赛季进度 {{ overview?.season?.progressPercent || 0 }}%</span>
            <strong>{{ overview?.season?.completedStops || 0 }} / {{ overview?.season?.totalStops || 0 }} 站</strong>
          </div>
        </article>

        <article class="track-card">
          <div class="track-kicker">画像赛事路线</div>
          <h3>{{ overview?.personalizedTrack?.title || '先建立画像，再解锁赛事路线' }}</h3>
          <p>
            {{
              overview?.personalizedTrack?.description
                || '登录并完成问答后，这里会把更适合你的榜单频道和赛事装备路线串起来。'
            }}
          </p>
          <div v-if="overview?.personalizedTrack?.tags?.length" class="track-tags">
            <span v-for="tag in overview.personalizedTrack.tags" :key="tag">{{ tag }}</span>
          </div>
          <div class="track-actions">
            <el-button
              v-if="overview?.personalizedTrack?.recommendedBoardType"
              type="primary"
              plain
              @click="goToBoard(overview.personalizedTrack.recommendedBoardType)"
            >
              {{ overview?.personalizedTrack?.recommendedBoardName || '查看榜单' }}
            </el-button>
            <el-button
              v-if="overview?.personalizedTrack?.equipmentKeyword"
              @click="goToEquipments(overview.personalizedTrack.equipmentKeyword)"
            >
              查看专题装备
            </el-button>
          </div>
        </article>
      </div>

      <div class="content-grid">
        <div class="standings-card">
          <div class="card-head">
            <div>
              <div class="card-kicker">积分榜</div>
              <h3>当前最值得盯紧的积分选手</h3>
            </div>
          </div>
          <div class="standing-list">
            <article v-for="item in topStandings" :key="item.rankPosition" class="standing-item">
              <div class="standing-rank">#{{ item.rankPosition }}</div>
              <div class="standing-main">
                <strong>{{ item.playerName }}</strong>
                <span>{{ item.associationName || '开球网赛事联盟' }}</span>
                <p>{{ item.serveStyle }}</p>
              </div>
              <div class="standing-points">
                <strong>{{ item.points || 0 }}</strong>
                <span>积分</span>
              </div>
            </article>
          </div>
        </div>

        <div class="gear-card">
          <div class="card-head">
            <div>
              <div class="card-kicker">赛事装备专题</div>
              <h3>沿着分站主题继续补齐装备认知</h3>
            </div>
          </div>
          <div class="gear-grid">
            <div v-for="item in featuredEquipments" :key="item.id" class="gear-item">
              <EquipmentCard :equipment="item" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getKickoffKingOverview } from '@/api/kickoffKing'
import EquipmentCard from '@/components/EquipmentCard.vue'
import { isRealtimeEvent, subscribeRealtimeUpdates } from '@/utils/realtime'

const router = useRouter()
const loading = ref(false)
const overview = ref(null)

let stopRealtime = null
let refreshTimer = null

const topStandings = computed(() => (overview.value?.standings || []).slice(0, 4))
const featuredEquipments = computed(() => (overview.value?.featuredEquipments || []).slice(0, 3))

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
.kickoff-showcase {
  padding: 0 24px 44px;
}

.showcase-shell {
  max-width: 1320px;
  margin: 0 auto;
  padding: 30px;
  border-radius: 34px;
  background:
    radial-gradient(circle at top right, rgba(255, 122, 39, 0.16), transparent 28%),
    radial-gradient(circle at bottom left, rgba(17, 86, 162, 0.14), transparent 26%),
    linear-gradient(145deg, #fffdf8 0%, #f8fbff 100%);
  box-shadow: 0 26px 54px rgba(20, 42, 90, 0.08);
}

.showcase-head,
.card-head,
.head-actions,
.track-actions {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.eyebrow,
.card-kicker,
.track-kicker {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(17, 86, 162, 0.08);
  color: #2453a6;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.showcase-head h2,
.season-card h3,
.track-card h3,
.card-head h3 {
  margin: 14px 0 10px;
  color: #13265d;
}

.showcase-head p,
.season-card p,
.track-card p {
  margin: 0;
  color: #60708f;
  line-height: 1.8;
}

.hero-grid,
.content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.season-card,
.track-card,
.standings-card,
.gear-card {
  padding: 24px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(18, 38, 93, 0.08);
}

.season-badge {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 122, 39, 0.12);
  color: #cf6115;
  font-weight: 700;
}

.season-tags,
.track-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.season-tags span,
.track-tags span {
  padding: 7px 11px;
  border-radius: 999px;
  background: rgba(18, 38, 93, 0.06);
  color: #19366b;
  font-size: 12px;
  font-weight: 700;
}

.season-progress {
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #53627d;
}

.standing-list {
  display: grid;
  gap: 14px;
  margin-top: 14px;
}

.standing-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 14px;
  align-items: center;
  padding: 16px;
  border-radius: 20px;
  background: rgba(18, 38, 93, 0.04);
}

.standing-rank {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: #163d7d;
  color: #fff;
  font-weight: 800;
}

.standing-main strong,
.standing-points strong {
  color: #13265d;
}

.standing-main span,
.standing-main p,
.standing-points span {
  display: block;
  color: #60708f;
}

.standing-main p {
  margin: 8px 0 0;
  line-height: 1.6;
}

.gear-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 14px;
}

@media (max-width: 1024px) {
  .hero-grid,
  .content-grid,
  .gear-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .kickoff-showcase {
    padding: 0 16px 36px;
  }

  .showcase-shell {
    padding: 22px;
  }

  .standing-item {
    grid-template-columns: 1fr;
  }
}
</style>
