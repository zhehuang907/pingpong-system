<template>
  <div class="persona-channel-page">
    <Header />

    <main class="channel-main">
      <section class="channel-hero">
        <div class="hero-copy">
          <div class="hero-kicker">画像推荐中台</div>
          <h1>{{ workbench?.summaryTitle || profileInsight?.personaTitle || '为你的训练与选购建立一条更清晰的装备路线' }}</h1>
          <p>
            {{
              workbench?.summaryDescription ||
              profileInsight?.personaDescription ||
              '系统会把问答结果、浏览行为、收藏评论和器材组合沉淀，持续转化成更贴近你的推荐策略。'
            }}
          </p>
          <div v-if="keywordTags.length" class="hero-tags">
            <span v-for="tag in keywordTags" :key="tag">{{ tag }}</span>
          </div>
        </div>

        <div class="hero-side">
          <div class="hero-stat">
            <span>画像完成度</span>
            <strong>{{ profileInsight?.completionScore || 0 }}%</strong>
          </div>
          <div class="hero-stat">
            <span>推荐模式</span>
            <strong>{{ profileInsight?.recommendationMode || '待建立' }}</strong>
          </div>
          <div class="hero-actions">
            <el-button type="primary" @click="router.push('/quiz')">继续完善画像</el-button>
            <el-button @click="router.push('/profile')">查看个人中心</el-button>
          </div>
        </div>
      </section>

      <section v-if="coachSuggestions.length" class="channel-section">
        <div class="section-head">
          <div>
            <div class="section-kicker">教练建议</div>
            <h2>把你的训练背景翻译成器材决策语言</h2>
          </div>
        </div>

        <div class="insight-grid">
          <article v-for="item in coachSuggestions" :key="item" class="insight-card">
            <h3>建议</h3>
            <p>{{ item }}</p>
          </article>
        </div>
      </section>

      <section v-if="workbench?.combinationPlans?.length" class="channel-section">
        <div class="section-head">
          <div>
            <div class="section-kicker">组合推荐</div>
            <h2>球拍、胶皮与配件的成套搭配方案</h2>
          </div>
        </div>

        <div class="workbench-grid">
          <article
            v-for="plan in workbench.combinationPlans"
            :key="plan.title"
            class="plan-card"
          >
            <div class="card-pill">{{ plan.budgetFit ? '预算匹配' : '场景方案' }}</div>
            <h3>{{ plan.title }}</h3>
            <p>{{ plan.description }}</p>
            <div v-if="plan.estimatedBudget" class="card-budget">预计预算：{{ plan.estimatedBudget }} 元</div>
            <div class="equipment-mini-list">
              <button
                v-for="item in plan.items || []"
                :key="item.id"
                type="button"
                class="equipment-mini"
                @click="goToEquipment(item.id)"
              >
                <span class="mini-role">{{ roleText(item.role) }}</span>
                <strong>{{ item.name }}</strong>
              </button>
            </div>
            <div v-if="plan.tags?.length" class="topic-tags">
              <span v-for="tag in plan.tags" :key="tag">{{ tag }}</span>
            </div>
          </article>
        </div>
      </section>

      <section v-if="workbench?.upgradePlans?.length" class="channel-section">
        <div class="section-head">
          <div>
            <div class="section-kicker">升级路径</div>
            <h2>基于训练提升节奏给出装备迭代路线</h2>
          </div>
        </div>

        <div class="insight-grid">
          <article v-for="item in workbench.upgradePlans" :key="item.title" class="insight-card">
            <div class="card-pill">{{ item.stage }}</div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.description }}</p>
            <div v-if="item.tags?.length" class="topic-tags">
              <span v-for="tag in item.tags" :key="tag">{{ tag }}</span>
            </div>
          </article>
        </div>
      </section>

      <section class="channel-section">
        <div class="section-head">
          <div>
            <div class="section-kicker">榜单频道</div>
            <h2>先逛最贴近你画像的榜单入口</h2>
          </div>
          <el-button class="section-button" @click="router.push('/leaderboards')">查看完整榜单</el-button>
        </div>

        <div class="board-grid">
          <article
            v-for="board in prioritizedBoards"
            :key="board.boardType"
            :class="['board-card', { matched: board.isMatched }]"
          >
            <div class="board-card-head">
              <span class="board-chip">{{ board.isMatched ? '优先推荐' : '通用入口' }}</span>
              <span class="board-rank">{{ board.boardName }}</span>
            </div>
            <h3>{{ board.subtitle }}</h3>
            <p>{{ board.matchReason }}</p>
            <div v-if="board.topItem" class="board-highlight">
              <strong>{{ board.topItem.name }}</strong>
              <span>{{ board.topItem.boardReason }}</span>
            </div>
            <el-button type="primary" plain @click="goToLeaderboards(board.boardType)">进入频道</el-button>
          </article>
        </div>
      </section>

      <section v-if="workbench?.trendSignals?.length || workbench?.newArrivals?.length" class="channel-section">
        <div class="section-head">
          <div>
            <div class="section-kicker">趋势与新品</div>
            <h2>把热度变化和新入库装备一起交给你</h2>
          </div>
        </div>

        <div class="dual-grid">
          <div class="stack-panel">
            <article v-for="item in workbench?.trendSignals || []" :key="item.title" class="insight-card">
              <div class="card-pill">趋势信号</div>
              <h3>{{ item.title }}</h3>
              <p>{{ item.description }}</p>
              <div v-if="item.tags?.length" class="topic-tags">
                <span v-for="tag in item.tags" :key="tag">{{ tag }}</span>
              </div>
            </article>
          </div>

          <div class="new-arrival-panel">
            <div class="panel-head">
              <strong>新品推荐</strong>
              <span>最近上新的器材会优先纳入候选池</span>
            </div>
            <div class="new-arrival-list">
              <button
                v-for="item in workbench?.newArrivals || []"
                :key="item.id"
                type="button"
                class="arrival-item"
                @click="goToEquipment(item.id)"
              >
                <img :src="getEquipmentImageUrl(item)" :alt="item.name">
                <div>
                  <strong>{{ item.name }}</strong>
                  <p>{{ item.brand || '装备新品' }}</p>
                </div>
              </button>
            </div>
          </div>
        </div>
      </section>

      <section v-if="workbench?.trialCards?.length || workbench?.compareOptions?.length" class="channel-section">
        <div class="section-head">
          <div>
            <div class="section-kicker">辅助决策</div>
            <h2>试打预约、对比分析和选购缩圈工具</h2>
          </div>
        </div>

        <div class="dual-grid">
          <div class="stack-panel">
            <article v-for="card in workbench?.trialCards || []" :key="card.equipmentId" class="insight-card">
              <div class="card-pill">试打建议</div>
              <h3>{{ card.title }}</h3>
              <p>{{ card.description }}</p>
              <div class="card-budget">{{ card.bookingHint }}</div>
              <div v-if="card.tags?.length" class="topic-tags">
                <span v-for="tag in card.tags" :key="tag">{{ tag }}</span>
              </div>
              <el-button type="primary" plain @click="goToEquipment(card.equipmentId)">查看装备</el-button>
            </article>
          </div>

          <div class="stack-panel">
            <article v-for="option in workbench?.compareOptions || []" :key="option.title" class="insight-card">
              <div class="card-pill">对比工具</div>
              <h3>{{ option.title }}</h3>
              <p>{{ option.description }}</p>
              <div v-if="option.tags?.length" class="topic-tags">
                <span v-for="tag in option.tags" :key="tag">{{ tag }}</span>
              </div>
              <el-button type="primary" plain @click="goToCompare(option.ids)">去对比分析</el-button>
            </article>
          </div>
        </div>
      </section>

      <section v-if="playerReferences.length" class="channel-section">
        <div class="section-head">
          <div>
            <div class="section-kicker">球星参考</div>
            <h2>从专业选手的配置里找到可借鉴的方向</h2>
          </div>
        </div>

        <div class="player-grid">
          <article v-for="player in playerReferences" :key="player.playerId" class="player-card">
            <div class="player-head">
              <img :src="getEquipmentImageUrl(player)" :alt="player.name">
              <div>
                <h3>{{ player.name }}</h3>
                <span>{{ player.country || '国际选手' }}</span>
              </div>
            </div>
            <p>{{ player.description || '这位选手的配置可作为你理解打法与装备关系的参考。' }}</p>
            <div class="player-equipment-list">
              <button
                v-for="item in player.equipments || []"
                :key="item.id"
                type="button"
                class="player-equipment"
                @click="goToEquipment(item.id)"
              >
                {{ item.name }}
              </button>
            </div>
          </article>
        </div>
      </section>

      <section v-if="kickoffOverview" class="channel-section">
        <div class="section-head">
          <div>
            <div class="section-kicker">赛事联动</div>
            <h2>把画像直接映射到开球网积分赛事专题</h2>
          </div>
          <el-button class="section-button" @click="router.push('/kaiqiuwang-ranking')">进入赛事频道</el-button>
        </div>

        <div class="event-route-card">
          <div class="event-route-copy">
            <span class="event-route-badge">{{ kickoffOverview.season?.currentStopName || '赛事进行中' }}</span>
            <h3>{{ kickoffOverview.personalizedTrack?.title || kickoffOverview.season?.seasonName }}</h3>
            <p>{{ kickoffOverview.personalizedTrack?.description || kickoffOverview.season?.heroSubtitle }}</p>
            <div v-if="kickoffOverview.personalizedTrack?.tags?.length" class="event-route-tags">
              <span v-for="tag in kickoffOverview.personalizedTrack.tags" :key="tag">{{ tag }}</span>
            </div>
          </div>

          <div class="event-route-side">
            <strong>当前焦点选手</strong>
            <div v-for="item in kickoffTopStandings" :key="item.rankPosition" class="event-standing-item">
              <span>#{{ item.rankPosition }}</span>
              <div>
                <strong>{{ item.playerName }}</strong>
                <p>{{ item.hotForm }}</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section v-if="feedbackLoop" class="channel-section">
        <div class="section-head">
          <div>
            <div class="section-kicker">反馈闭环</div>
            <h2>{{ feedbackLoop.headline }}</h2>
          </div>
        </div>

        <div class="feedback-panel">
          <p class="feedback-copy">{{ feedbackLoop.description }}</p>
          <div class="metrics-grid">
            <div v-for="item in feedbackLoop.metrics || []" :key="item.label" class="metric-card">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
          <div class="feedback-actions">
            <div v-for="item in feedbackLoop.nextActions || []" :key="item" class="feedback-action">
              {{ item }}
            </div>
          </div>
        </div>
      </section>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import { getKickoffKingOverview } from '@/api/kickoffKing'
import { getHomeLeaderboards } from '@/api/leaderboard'
import { getProfileInsight, getRecommendationWorkbench } from '@/api/recommend'
import { isRealtimeEvent, subscribeRealtimeUpdates } from '@/utils/realtime'

const router = useRouter()
const boards = ref([])
const profileInsight = ref(null)
const kickoffOverview = ref(null)
const workbench = ref(null)

let stopRealtime = null
let refreshTimer = null

function getEquipmentImageUrl(equipment) {
  // 优先使用 mainImage，其次使用 imageUrl，最后使用占位图
  return equipment?.mainImage || equipment?.imageUrl || '/placeholder.png'
}

const keywordTags = computed(() => workbench.value?.profileKeywords || profileInsight.value?.coreTags || [])
const coachSuggestions = computed(() => workbench.value?.coachSuggestions || [])
const playerReferences = computed(() => workbench.value?.playerReferences || [])
const feedbackLoop = computed(() => workbench.value?.feedbackLoop || null)

const matchedBoardTypes = computed(() => {
  const source = `${profileInsight.value?.personaTitle || ''} ${(keywordTags.value || []).join(' ')}`
  const matches = ['weekly_popular']

  if (/(初级|初学者|入门)/.test(source) || (profileInsight.value?.completionScore || 0) < 60) {
    matches.unshift('beginner_pick')
  }
  if (/(高级|专业)/.test(source)) {
    matches.unshift('pro_favorite')
  }
  if (/(性价比|300 元以内|300 - 800 元)/.test(source)) {
    matches.unshift('value_king')
  }

  return Array.from(new Set(matches))
})

const prioritizedBoards = computed(() => {
  return boards.value
    .map((board) => ({
      ...board,
      topItem: board.items?.[0] || null,
      isMatched: matchedBoardTypes.value.includes(board.boardType),
      matchReason: buildBoardMatchReason(board.boardType)
    }))
    .sort((left, right) => Number(right.isMatched) - Number(left.isMatched))
})

const kickoffTopStandings = computed(() => (kickoffOverview.value?.standings || []).slice(0, 3))

onMounted(async () => {
  await Promise.all([loadBoards(), loadProfileInsight(), loadKickoffOverview(), loadWorkbench()])
  stopRealtime = subscribeRealtimeUpdates((message) => {
    if (!isRealtimeEvent(message, ['leaderboard_refreshed', 'kickoff_king_updated', 'equipment_changed', 'comment_changed'])) {
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

async function loadBoards() {
  try {
    const res = await getHomeLeaderboards({ limit: 10 })
    boards.value = res.data || []
  } catch (error) {
    boards.value = []
  }
}

async function loadProfileInsight() {
  try {
    const res = await getProfileInsight()
    profileInsight.value = res.data || null
  } catch (error) {
    profileInsight.value = null
  }
}

async function loadKickoffOverview() {
  try {
    const res = await getKickoffKingOverview()
    kickoffOverview.value = res.data || null
  } catch (error) {
    kickoffOverview.value = null
  }
}

async function loadWorkbench() {
  try {
    const res = await getRecommendationWorkbench()
    workbench.value = res.data || null
  } catch (error) {
    workbench.value = null
  }
}

function scheduleRefresh() {
  if (refreshTimer) {
    window.clearTimeout(refreshTimer)
  }
  refreshTimer = window.setTimeout(() => {
    Promise.all([loadBoards(), loadKickoffOverview(), loadProfileInsight(), loadWorkbench()])
  }, 250)
}

function buildBoardMatchReason(boardType) {
  const source = `${profileInsight.value?.personaTitle || ''} ${keywordTags.value.join(' ')}`

  if (boardType === 'beginner_pick') {
    return /(初级|初学者|入门)/.test(source) || (profileInsight.value?.completionScore || 0) < 60
      ? '你当前更适合先从稳定、好上手和容错更高的方案开始。'
      : '当你想回到更稳的基础配置时，这里会是最快的入口。'
  }

  if (boardType === 'value_king') {
    return /(性价比|300 元以内|300 - 800 元)/.test(source)
      ? '你的预算锚点已经比较清晰，这个频道会更贴近真实购买区间。'
      : '当你想缩小预算范围时，这里可以先帮你完成第一轮筛选。'
  }

  if (boardType === 'pro_favorite') {
    return /(高级|专业|比赛适配)/.test(source)
      ? '你当前更偏向进阶对抗场景，这里适合继续深挖高性能方案。'
      : '如果你想看更偏比赛向或上限更高的配置，可以从这里开始。'
  }

  return '这里适合作为通用趋势入口，快速了解当前热度更高的装备方向。'
}

function goToLeaderboards(boardType) {
  router.push({
    path: '/leaderboards',
    query: boardType ? { boardType } : {}
  })
}

function goToEquipment(id) {
  if (!id) {
    return
  }
  router.push(`/equipment/${id}`)
}

function goToCompare(ids = []) {
  const validIds = ids.filter(Boolean)
  if (!validIds.length) {
    return
  }
  router.push({
    path: '/compare',
    query: {
      ids: validIds.join(',')
    }
  })
}

function roleText(role) {
  const roleMap = {
    blade: '底板',
    rubber: '胶皮',
    shoe: '球鞋',
    accessory: '配件',
    equipment: '装备'
  }
  return roleMap[role] || '装备'
}
</script>

<style scoped lang="scss">
.persona-channel-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(255, 122, 39, 0.12), transparent 24%),
    radial-gradient(circle at bottom right, rgba(16, 59, 43, 0.12), transparent 28%),
    linear-gradient(180deg, #f7f9ff 0%, #ffffff 100%);
}

.channel-main {
  max-width: 1320px;
  margin: 0 auto;
  padding: 26px 20px 40px;
}

.channel-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.8fr);
  gap: 18px;
  padding: 28px;
  border-radius: 34px;
  background:
    radial-gradient(circle at top right, rgba(31, 78, 166, 0.14), transparent 24%),
    radial-gradient(circle at bottom left, rgba(255, 122, 39, 0.12), transparent 26%),
    linear-gradient(145deg, #ffffff 0%, #f6fbff 100%);
  box-shadow: 0 26px 52px rgba(28, 54, 90, 0.08);
}

.hero-kicker,
.section-kicker {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(16, 60, 110, 0.08);
  color: #2957b5;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.hero-copy h1 {
  margin: 14px 0 12px;
  color: #12245a;
  font-size: clamp(34px, 4vw, 50px);
  line-height: 1.08;
}

.hero-copy p,
.feedback-copy {
  margin: 0;
  color: #5f6f8f;
  line-height: 1.85;
}

.hero-tags,
.topic-tags,
.event-route-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.hero-tags span,
.topic-tags span,
.event-route-tags span {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: #24416f;
  font-size: 13px;
  font-weight: 700;
  box-shadow: inset 0 0 0 1px rgba(36, 65, 111, 0.08);
}

.hero-side {
  display: grid;
  gap: 14px;
}

.hero-stat,
.feedback-panel,
.new-arrival-panel,
.stack-panel,
.board-card,
.plan-card,
.insight-card,
.player-card {
  border-radius: 28px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid rgba(18, 36, 90, 0.08);
  box-shadow: 0 18px 38px rgba(28, 54, 90, 0.06);
}

.hero-stat {
  padding: 18px;
}

.hero-stat span,
.metric-card span,
.panel-head span,
.player-head span {
  display: block;
  color: #6f7b95;
  font-size: 13px;
}

.hero-stat strong {
  display: block;
  margin-top: 10px;
  color: #12245a;
  font-size: 28px;
  line-height: 1.3;
}

.hero-actions,
.results-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.channel-section {
  margin-top: 28px;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.section-head h2 {
  margin: 14px 0 0;
  color: #12245a;
  font-size: clamp(28px, 3vw, 40px);
}

.section-button {
  border-radius: 999px;
}

.insight-grid,
.board-grid,
.workbench-grid,
.player-grid,
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.dual-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
}

.stack-panel {
  padding: 18px;
  display: grid;
  gap: 16px;
}

.insight-card,
.plan-card,
.board-card,
.player-card {
  padding: 22px;
}

.card-pill,
.board-chip,
.mini-role {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 122, 39, 0.12);
  color: #d35b10;
  font-size: 12px;
  font-weight: 800;
}

.plan-card h3,
.insight-card h3,
.board-card h3,
.player-card h3 {
  margin: 16px 0 10px;
  color: #12245a;
  font-size: 24px;
  line-height: 1.3;
}

.plan-card p,
.insight-card p,
.board-card p,
.player-card p {
  margin: 0;
  color: #64708a;
  line-height: 1.8;
}

.card-budget {
  margin-top: 14px;
  color: #24416f;
  font-weight: 700;
}

.equipment-mini-list,
.player-equipment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.equipment-mini,
.player-equipment,
.arrival-item {
  border: none;
  cursor: pointer;
  text-align: left;
}

.equipment-mini {
  display: grid;
  gap: 8px;
  min-width: 160px;
  padding: 14px 16px;
  border-radius: 18px;
  background: #f5f8ff;
}

.equipment-mini strong,
.arrival-item strong,
.board-highlight strong,
.event-route-side > strong,
.panel-head strong,
.metric-card strong {
  color: #12245a;
}

.board-card.matched {
  border-color: rgba(31, 78, 166, 0.24);
  box-shadow: 0 24px 44px rgba(31, 78, 166, 0.1);
}

.board-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.board-rank {
  color: #3f5377;
  font-size: 13px;
  font-weight: 700;
}

.board-highlight {
  margin: 16px 0 18px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(31, 78, 166, 0.06);
}

.board-highlight span {
  display: block;
  margin-top: 8px;
  color: #5f6f8f;
  font-size: 13px;
  line-height: 1.7;
}

.new-arrival-panel {
  padding: 20px;
}

.panel-head {
  margin-bottom: 14px;
}

.new-arrival-list {
  display: grid;
  gap: 12px;
}

.arrival-item {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  padding: 12px;
  border-radius: 18px;
  background: #f5f8ff;
}

.arrival-item img,
.player-head img {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  object-fit: cover;
  background: #eef3ff;
}

.player-head {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.player-equipment {
  padding: 10px 14px;
  border-radius: 999px;
  background: #f2f6ff;
  color: #36527f;
  font-weight: 700;
}

.event-route-card {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(260px, 0.8fr);
  gap: 16px;
  padding: 24px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top right, rgba(255, 122, 39, 0.14), transparent 26%),
    linear-gradient(145deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid rgba(18, 36, 90, 0.08);
  box-shadow: 0 20px 40px rgba(28, 54, 90, 0.06);
}

.event-route-badge {
  display: inline-flex;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255, 122, 39, 0.12);
  color: #d35b10;
  font-size: 12px;
  font-weight: 800;
}

.event-route-copy h3 {
  margin: 16px 0 10px;
  color: #12245a;
  font-size: 28px;
}

.event-route-copy p,
.event-standing-item p {
  margin: 0;
  color: #64708a;
  line-height: 1.8;
}

.event-route-side {
  display: grid;
  gap: 12px;
  align-self: start;
}

.event-standing-item {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  border-radius: 18px;
  background: #f5f8ff;
}

.event-standing-item span {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: linear-gradient(135deg, #ff7a27, #ffb061);
  color: #fff;
  font-weight: 800;
}

.feedback-panel {
  padding: 24px;
}

.metrics-grid {
  margin-top: 20px;
}

.metric-card {
  padding: 18px;
  border-radius: 20px;
  background: #f5f8ff;
}

.feedback-actions {
  display: grid;
  gap: 12px;
  margin-top: 20px;
}

.feedback-action {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(31, 78, 166, 0.06);
  color: #36527f;
  line-height: 1.8;
}

@media (max-width: 1100px) {
  .channel-hero,
  .event-route-card,
  .dual-grid,
  .insight-grid,
  .board-grid,
  .workbench-grid,
  .player-grid,
  .metrics-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .channel-main {
    padding: 18px 14px 30px;
  }

  .section-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-actions {
    flex-direction: column;
  }
}
</style>
