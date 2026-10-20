<template>
  <div class="home-page">
    <Header />

    <main class="home-main">
      <section class="hero-section">
        <div class="hero-shell">
          <div class="hero-copy">
            <div class="hero-champion-panel">
              <ChampionCarousel :slides="championSlides" :autoplay-interval="2500" />
            </div>

          </div>

          <div
            class="hero-waterfall"
            :class="{ paused: waterfallPaused }"
            @mouseenter="waterfallPaused = true"
            @mouseleave="waterfallPaused = false"
          >
            <div class="waterfall-surface"></div>
            <div class="waterfall-container">
              <div
                v-for="(column, columnIndex) in scrollableWaterfallColumns"
                :key="`col-${columnIndex}`"
                class="waterfall-column"
                :style="waterfallAnimation(columnIndex)"
              >
                <!-- Double the list for infinite scroll -->
                <div class="column-content">
                  <button
                    v-for="(card, cardIndex) in [...column, ...column]"
                    :key="`${card.id}-${cardIndex}`"
                    type="button"
                    class="waterfall-card"
                    :class="[`tone-${card.tone}`, `size-${card.size}`]"
                    @click="handleWaterfallCardClick(card)"
                  >
                    <div class="card-gridline"></div>
                    <div class="card-copy">
                      <div class="card-accent">{{ card.accent }}</div>
                      <h3>{{ card.title }}</h3>
                      <p>{{ card.subtitle }}</p>
                    </div>
                    <div class="card-visual-shell" :class="`visual-${card.visualTone}`">
                      <img :src="card.visual" :alt="card.title" class="card-visual" />
                    </div>
                    <div class="card-footer">
                      <span class="card-footer-main">{{ card.footer }}</span>
                    </div>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <LeaderboardShowcase />
      <KickoffKingShowcase />

      <section class="category-section">
        <div class="section-surface category-surface">
          <div class="section-head">
            <div>
              <div class="section-kicker">热门方向</div>
              <h2>按打法与场景切入，快速缩小选择范围</h2>
            </div>
            <button type="button" class="section-link" @click="goToEquipmentList()">
              浏览全部装备
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>

          <div class="category-layout">
            <div class="category-grid">
              <button
                v-for="item in categories"
                :key="item.title"
                type="button"
                class="category-card"
                @click="goToEquipmentList(item.query)"
              >
                <div class="category-label">{{ item.label }}</div>
                <h3>{{ item.title }}</h3>
                <p>{{ item.description }}</p>
                <div class="category-tags">
                  <span v-for="tag in item.tags" :key="tag">{{ tag }}</span>
                </div>
              </button>
            </div>

            <aside class="category-aside">
              <div class="category-aside-card primary">
                <span class="aside-label">选购顺序</span>
                <h3>先定打法，再定预算，最后看口碑榜单</h3>
                <p>把选择拆成几个更轻的步骤，首页会更容易帮你把范围收窄到真正值得点开的装备。</p>
              </div>

              <div class="category-aside-card">
                <div v-for="item in browseChecklist" :key="item.title" class="aside-check-item">
                  <strong>{{ item.title }}</strong>
                  <span>{{ item.description }}</span>
                </div>
              </div>
            </aside>
          </div>
        </div>
      </section>

      <section class="recommend-section" id="featured-equipments">
        <div class="section-surface recommend-surface">
          <div class="section-head">
            <div>
              <div class="section-kicker">{{ recommendationKicker }}</div>
              <h2>{{ recommendationTitle }}</h2>
              <p>{{ recommendationDescription }}</p>
            </div>
            <button type="button" class="section-link" @click="goToEquipmentList({ sortBy: 'rating' })">
              查看更多
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>

          <div v-if="isLoggedIn && preferenceReady" class="preference-spotlight-shell">
            <div v-if="hasPreferenceProfile" class="preference-spotlight">
              <div class="preference-copy">
                <div class="preference-eyebrow">问答画像已接入首页</div>
                <h3>{{ preferenceHeadline }}</h3>
                <p>{{ preferenceNarrative }}</p>
                <div class="preference-tag-list">
                  <span v-for="tag in visiblePreferenceTags" :key="tag" class="preference-tag">{{ tag }}</span>
                </div>
              </div>

              <div class="preference-metrics">
                <div v-for="item in preferenceHighlights" :key="item.label" class="preference-metric">
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                </div>
              </div>

              <div class="preference-actions">
                <button type="button" class="preference-btn primary" @click="router.push('/quiz')">调整问答画像</button>
                <button type="button" class="preference-btn" @click="router.push('/profile')">查看个人画像</button>
              </div>
            </div>

            <div v-else class="preference-spotlight empty">
              <div class="preference-copy">
                <div class="preference-eyebrow">问答入口</div>
                <h3>先完成 1 次偏好问答，让首页推荐更贴近你</h3>
                <p>完成后会把技术水平、打法、预算、训练频率和目标同步到首页推荐，并持续参与个性化排序。</p>
              </div>

              <div class="preference-actions">
                <button type="button" class="preference-btn primary" @click="router.push('/quiz')">立即开始问答</button>
                <button type="button" class="preference-btn" @click="router.push('/profile')">查看个人资料</button>
              </div>
            </div>
          </div>

          <div v-if="isLoggedIn && profileInsight" class="insight-board-shell">
            <div class="insight-board">
              <div class="insight-main">
                <div class="insight-kicker">画像洞察</div>
                <h3>{{ profileInsight.personaTitle }}</h3>
                <p>{{ profileInsight.personaDescription }}</p>
                <div class="insight-mode-row">
                  <span class="insight-mode">{{ profileInsight.recommendationMode }}</span>
                  <span class="insight-mode">画像完成度 {{ profileInsight.completionScore || 0 }}%</span>
                </div>
                <div v-if="profileInsight.coreTags?.length" class="insight-tag-list">
                  <span v-for="tag in profileInsight.coreTags" :key="tag" class="insight-tag">{{ tag }}</span>
                </div>
                <div class="insight-inline-actions">
                  <button type="button" class="insight-link-button" @click="router.push('/persona-channel')">进入画像频道</button>
                </div>
              </div>

              <div v-if="profileInsight.signals?.length" class="insight-signal-grid">
                <article v-for="signal in profileInsight.signals" :key="signal.label" class="insight-signal-card">
                  <span>{{ signal.label }}</span>
                  <strong>{{ signal.value }}</strong>
                  <p>{{ signal.detail }}</p>
                </article>
              </div>
            </div>

            <div v-if="profileInsight.improvementActions?.length" class="insight-todo">
              <div class="insight-todo-title">下一步建议</div>
              <div class="insight-todo-list">
                <div v-for="item in profileInsight.improvementActions" :key="item" class="insight-todo-item">
                  {{ item }}
                </div>
              </div>
            </div>

            <div v-if="profileInsight.actionCards?.length" class="insight-action-grid">
              <button
                v-for="action in profileInsight.actionCards"
                :key="`${action.title}-${action.targetPath}`"
                type="button"
                class="insight-action-card"
                @click="handleInsightAction(action)"
              >
                <div class="insight-action-copy">
                  <span>{{ action.title }}</span>
                  <h3>{{ action.buttonText }}</h3>
                  <p>{{ action.description }}</p>
                </div>
                <div v-if="action.tags?.length" class="insight-action-tags">
                  <span v-for="tag in action.tags" :key="tag">{{ tag }}</span>
                </div>
              </button>
            </div>
          </div>

          <div class="recommend-stage">
            <div class="recommend-main">
              <div v-if="loading" class="recommend-loading">
                <el-skeleton v-for="index in 4" :key="index" :rows="6" animated />
              </div>

              <div v-else-if="displayEquipments.length" class="recommend-grid">
                <div v-for="item in displayEquipments" :key="item.id" class="recommend-item">
                  <EquipmentCard :equipment="item" />
                </div>
              </div>

              <div v-else class="curated-fallback-grid">
                <button
                  v-for="item in fallbackCurations"
                  :key="item.title"
                  type="button"
                  class="curated-fallback-card"
                  @click="goToEquipmentList(item.query)"
                >
                  <div class="curated-image">
                    <img :src="item.image" :alt="item.title" />
                  </div>
                  <div class="curated-copy">
                    <span>{{ item.tag }}</span>
                    <h3>{{ item.title }}</h3>
                    <p>{{ item.description }}</p>
                  </div>
                </button>
              </div>
            </div>

            <aside class="recommend-aside">
              <div class="recommend-aside-panel primary">
                <span class="aside-label">推荐工作流</span>
                <h3>不是只看热度，而是把画像、口碑和行为一起纳入排序</h3>
                <p>首页推荐会优先考虑你的近期浏览、收藏、问答画像和装备口碑，让每一屏都更接近真实选购决策。</p>
              </div>

              <div class="recommend-aside-panel">
                <div v-for="item in recommendationChecklist" :key="item.title" class="aside-check-item">
                  <strong>{{ item.title }}</strong>
                  <span>{{ item.description }}</span>
                </div>
              </div>
            </aside>
          </div>
        </div>
      </section>

      <section class="feature-section">
        <div class="section-surface feature-surface">
          <div class="section-head">
            <div>
              <div class="section-kicker">平台体验</div>
              <h2>把“选装备”做成更轻的决策流程</h2>
            </div>
          </div>

          <div class="feature-layout">
            <div class="feature-lead-card">
              <span class="aside-label">平台体验</span>
              <h3>从内容筛选、画像理解到实时反馈，首页本身就是一个决策工作台</h3>
              <p>我们把推荐、榜单、画像和社区行为放到同一条路径里，让用户不是“被动看内容”，而是在首页就开始完成筛选。</p>
            </div>

            <div class="feature-grid">
              <article v-for="item in features" :key="item.title" class="feature-card">
                <div class="feature-icon">
                  <el-icon><component :is="item.icon" /></el-icon>
                </div>
                <div class="feature-index">{{ item.index }}</div>
                <h3>{{ item.title }}</h3>
                <p>{{ item.description }}</p>
              </article>
            </div>
          </div>
        </div>
      </section>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, VideoPause, Monitor, Cellphone, Cpu } from '@element-plus/icons-vue'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import EquipmentCard from '@/components/EquipmentCard.vue'
import ChampionCarousel from '@/components/ChampionCarousel.vue'
import KickoffKingShowcase from '@/components/KickoffKingShowcase.vue'
import LeaderboardShowcase from '@/components/LeaderboardShowcase.vue'
import { championSlides } from '@/data/championSlides'
import { equipmentApi } from '@/api/equipment'
import { getHotRecommend, getPersonalRecommend, getProfileInsight } from '@/api/recommend'
import { getUserPreferences } from '@/api/user'
import { useUserStore } from '@/store/user'
import { isRealtimeEvent, subscribeRealtimeUpdates } from '@/utils/realtime'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const equipmentList = ref([])
const waterfallCatalog = ref([])
const waterfallPaused = ref(false)
const activeDotIndex = ref(0)
const preferenceProfile = ref(null)
const preferenceReady = ref(false)
const profileInsight = ref(null)

let dotTimer = null
let stopRealtimeSubscription = null
let realtimeRefreshTimer = null

const waterfallColumnOffsets = ['-26px', '18px', '-48px', '12px']
const waterfallColumnFloats = ['-24px', '-34px', '-28px', '-22px']
const waterfallColumnDurations = ['8.6s', '10.4s', '9.2s', '11.1s']

const heroStats = [
  { value: '1000+', label: '装备选择' },
  { value: '10w+', label: '用户信赖' },
  { value: '98%', label: '好评率' }
]

const categories = [

  {
    label: '底板方向',
    title: '从手感到出球弧线，一眼锁定底板',
    description: '五木、七夹、外置纤维和内置纤维分层整理，适合从新手到进阶选手快速筛选。',
    tags: ['控球手感', '外置纤维', '弧圈进攻'],
    query: { keyword: '底板' }
  },
  {
    label: '胶皮方向',
    title: '速度、旋转、稳定性一起看',
    description: '把胶皮拆成正手爆冲、反手快撕、控制容错三条主线，少走弯路。',
    tags: ['爆冲', '旋转', '反手友好'],
    query: { keyword: '胶皮' }
  },
  {
    label: '步法方向',
    title: '球鞋与服装，让移动更顺',
    description: '轻量支撑、抓地止滑、训练穿搭统一整合，兼顾实战与日常训练舒适度。',
    tags: ['轻量支撑', '抓地', '训练穿搭'],
    query: { keyword: '球鞋' }
  },
  {
    label: '榜单方向',
    title: '先看口碑榜，再做最终决定',
    description: '真实收藏、评分和评论驱动榜单，让高热度和高满意度装备更容易被看见。',
    tags: ['本周人气', '性价比', '专业偏好'],
    query: {}
  }
]

const features = [
  {
    index: '01',
    icon: Cpu,
    title: 'AI 画像匹配',
    description: '把问答结果、浏览行为和互动反馈一起纳入推荐，让首页更懂你的打法与预算。'
  },
  {
    index: '02',
    icon: Monitor,
    title: '真实口碑榜单',
    description: '收藏、评论、评分与时间衰减共同参与，保证榜单既新鲜又有说服力。'
  },
  {
    index: '03',
    icon: Cellphone,
    title: '社区评测联动',
    description: '从晒单、评论到榜单位置一体联动，形成可追踪、可验证的决策闭环。'
  }
]

const browseChecklist = [
  {
    title: '先看底板框架',
    description: '先判断你是追求纯木手感、外置爆发，还是内置容错。'
  },
  {
    title: '再看胶皮方向',
    description: '把正手旋转、反手衔接和控制容错拆开看，选择会轻很多。'
  },
  {
    title: '最后用榜单验证',
    description: '结合口碑、热度和真实评论，把主观偏好交给数据再校准一次。'
  }
]

const recommendationChecklist = [
  {
    title: '画像优先',
    description: '优先使用技术水平、打法风格、预算和训练频率来确定推荐方向。'
  },
  {
    title: '行为校准',
    description: '近期浏览、收藏、评论和停留时长会继续调整推荐顺序。'
  },
  {
    title: '榜单回看',
    description: '热门与高评分装备会作为兜底参考，避免结果只偏向单一路径。'
  }
]

const fallbackCurations = computed(() => [
  {
    tag: '精选底板',
    title: '进攻与控制兼顾的底板组合',
    description: '从出球速度、持球感到容错率，帮你先缩小决策面。',
    image: createVisual('board', '底板精选', '#00e8ff', '#ff7a27'),
    query: { keyword: '底板' }
  },
  {
    tag: '胶皮方案',
    title: '正反手胶皮一次看明白',
    description: '把速度、旋转和稳定性拆开比较，减少盲买概率。',
    image: createVisual('rubber', '胶皮精选', '#00e8ff', '#29f0c6'),
    query: { keyword: '胶皮' }
  },
  {
    tag: '步法装备',
    title: '球鞋与训练服一站式整理',
    description: '从包裹到抓地，把移动体验一起纳入推荐。',
    image: createVisual('shoe', '步法装备', '#ffd54d', '#ff7a27'),
    query: { keyword: '球鞋' }
  }
])

const waterfallColumnStyle = (columnIndex) => ({
  '--column-delay': `${columnIndex * 0.9}s`,
  '--column-shift': waterfallColumnOffsets[columnIndex] || '0px',
  '--column-float': waterfallColumnFloats[columnIndex] || '-18px',
  '--column-duration': waterfallColumnDurations[columnIndex] || '10s'
})

const isLoggedIn = computed(() => Boolean(userStore.token))
const displayEquipments = computed(() => equipmentList.value.slice(0, 8))
const hasPreferenceProfile = computed(() => hasProfileData(preferenceProfile.value))
const preferenceBudgetLabel = computed(() => formatBudgetRange(preferenceProfile.value))
const visiblePreferenceTags = computed(() => {
  const profile = preferenceProfile.value
  if (!profile) {
    return []
  }

  return Array.from(new Set([
    profile.level,
    profile.playStyle,
    profile.gripStyle,
    profile.weeklyFrequency,
    profile.primaryGoal,
    profile.priorityFocus,
    preferenceBudgetLabel.value
  ].filter(Boolean))).slice(0, 6)
})
const preferenceHeadline = computed(() => {
  if (!hasPreferenceProfile.value) {
    return '还没有建立专属偏好画像'
  }

  const profile = preferenceProfile.value
  const parts = [profile?.level, profile?.playStyle, profile?.gripStyle].filter(Boolean)
  if (!parts.length) {
    return '你的问答画像已经同步到首页推荐'
  }
  return `${parts.join(' / ')} 画像已同步到首页推荐`
})
const preferenceNarrative = computed(() => {
  if (!hasPreferenceProfile.value) {
    return '完成一次偏好问答后，首页会按你的技术水平、打法、预算和训练习惯调整推荐顺序。'
  }

  const profile = preferenceProfile.value
  const focusParts = [profile?.priorityFocus, profile?.primaryGoal, profile?.weeklyFrequency].filter(Boolean)
  if (!focusParts.length) {
    return '系统会把你的问答结果与近期浏览、收藏行为一起纳入推荐排序。'
  }
  return `系统会把${focusParts.join('、')}与近期浏览、收藏行为一起纳入推荐排序。`
})
const preferenceHighlights = computed(() => {
  if (!hasPreferenceProfile.value) {
    return []
  }

  const profile = preferenceProfile.value
  return [
    { label: '技术水平', value: profile?.level || '未填写' },
    { label: '打法风格', value: profile?.playStyle || '未填写' },
    { label: '预算区间', value: preferenceBudgetLabel.value || '未填写' },
    { label: '训练频率', value: profile?.weeklyFrequency || '未填写' }
  ]
})
const recommendationKicker = computed(() => {
  if (hasPreferenceProfile.value) {
    return '画像推荐'
  }
  return isLoggedIn.value ? '个人推荐' : '热门精选'
})
const recommendationTitle = computed(() => {
  if (hasPreferenceProfile.value) {
    const profile = preferenceProfile.value
    const parts = [profile?.level, profile?.playStyle, profile?.gripStyle].filter(Boolean)
    return parts.length ? `根据你的 ${parts.join(' / ')} 画像挑出的今日装备` : '根据你的问答画像挑出的今日装备'
  }
  return isLoggedIn.value ? '结合你的行为生成的推荐清单' : '现在最值得先看的热门装备'
})
const recommendationDescription = computed(() =>
  hasPreferenceProfile.value
    ? `已结合${visiblePreferenceTags.value.slice(0, 4).join('、')}等问答结果，并参考最近浏览与收藏行为排序。`
    : isLoggedIn.value
      ? '当前先根据你的浏览、收藏和评分行为生成推荐；完成偏好问答后，首页会展示更明确的画像推荐。'
      : '从高评分、高热度和高讨论度中先筛一轮，首页就能快速把趋势装备看明白。'
)

const showcasePool = computed(() => {
  if (displayEquipments.value.length) {
    return displayEquipments.value
  }

  return [
    normalizeEquipment({ id: null, name: '银河 U2 五木底板', brand: '银河', price: 299, rating: 4.9 }, 0),
    normalizeEquipment({ id: null, name: '729 龙影反手胶皮', brand: '729', price: 129, rating: 4.8 }, 1),
    normalizeEquipment({ id: null, name: '双鱼 F55 专业球鞋', brand: '双鱼', price: 359, rating: 4.7 }, 2),
    normalizeEquipment({ id: null, name: '李宁竞训速干比赛服', brand: '李宁', price: 199, rating: 4.8 }, 3)
  ]
})

const waterfallCardSeeds = [
  [
    {
      id: 'malong-gear',
      tone: 'dark',
      size: 'tall',
      accent: '马龙同款',
      title: '红双喜 狂飙龙 5',
      subtitle: '双圈大满贯底板，极致控制与底劲，内置芳碳护航。',
      fallbackVisual: createVisual('board', '内置纤维', '#00e8ff', '#ff7a27'),
      fallbackQuery: { keyword: '底板' },
      targetNames: ['红双喜 狂飙龙 5']
    },
    {
      id: 'fan-gear',
      tone: 'dark',
      size: 'medium',
      accent: '樊振东同款',
      title: '蝴蝶 樊振东 ALC',
      subtitle: '暴力美学之作，蓝芳碳带来的速度极限与力量感。',
      fallbackVisual: createVisual('board', '外置蓝碳', '#29f0c6', '#00e8ff'),
      fallbackQuery: { keyword: '底板' },
      targetNames: ['蝴蝶 樊振东 ALC']
    }
  ],
  [
    {
      id: 'wang-gear',
      tone: 'dark',
      size: 'tall',
      accent: '王楚钦同款',
      title: '红双喜 狂飙王',
      subtitle: '新一代左手利刃，灵动衔接与近台爆发的黄金平衡。',
      fallbackVisual: createVisual('board', '纯木/纤维', '#29f0c6', '#ff7a27'),
      fallbackQuery: { keyword: '底板' },
      targetNames: ['红双喜 狂飙王 王楚钦同款', '红双喜 狂飙王']
    },
    {
      id: 'chen-gear',
      tone: 'accent',
      size: 'medium',
      accent: '陈梦同款',
      title: '蝴蝶 樊振东 SALC',
      subtitle: '超级纤维底板，顶级相持能力的保驾护航。',
      fallbackVisual: createVisual('board', '超级芳碳', '#00e8ff', '#8f7aff'),
      fallbackQuery: { keyword: '底板' },
      targetNames: ['蝴蝶 樊振东 SALC', '蝴蝶 樊振东 Super ALC']
    }
  ],
  [
    {
      id: 'sun-gear',
      tone: 'dark',
      size: 'tall',
      accent: '孙颖莎同款',
      title: '红双喜 狂飙莎',
      subtitle: '世界第一女单之选，稳定容错与暴力的完美兼容。',
      fallbackVisual: createVisual('board', '内置碳素', '#00e8ff', '#31d0ff'),
      fallbackQuery: { keyword: '底板' },
      targetNames: ['红双喜 狂飙莎', '红双喜 Hurricane Sun']
    },
    {
      id: 'zjk-gear',
      tone: 'accent',
      size: 'medium',
      accent: '张继科同款',
      title: '蝴蝶 Viscaria',
      subtitle: '大满贯常青之作，开启外置芳碳时代的经典手感。',
      fallbackVisual: createVisual('board', '经典 ALC', '#ff7a27', '#ffe780'),
      fallbackQuery: { keyword: '底板' },
      targetNames: ['蝴蝶 Viscaria']
    }
  ]
]

const waterfallTargetMap = {
  'malong-gear': ['\u7ea2\u53cc\u559c \u72c2\u98d9\u9f99 5'],
  'fan-gear': ['\u8774\u8776 \u6a0a\u632f\u4e1c ALC'],
  'wang-gear': [
    '\u7ea2\u53cc\u559c \u72c2\u98d9\u738b \u738b\u695a\u94a6\u540c\u6b3e',
    '\u7ea2\u53cc\u559c \u72c2\u98d9\u738b (\u738b\u695a\u94a6\u540c\u6b3e)',
    '\u7ea2\u53cc\u559c \u72c2\u98d9\u738b'
  ],
  'chen-gear': ['\u8774\u8776 \u6a0a\u632f\u4e1c SALC', '\u8774\u8776 \u6a0a\u632f\u4e1c Super ALC'],
  'sun-gear': ['\u7ea2\u53cc\u559c \u72c2\u98d9\u838e', '\u7ea2\u53cc\u559c Hurricane Sun'],
  'zjk-gear': ['\u8774\u8776 Viscaria']
}

const waterfallColumns = computed(() => {
  return waterfallCardSeeds.map((column) =>
    column.map((card) => {
      const target = findWaterfallEquipment(waterfallTargetMap[card.id] || card.targetNames)
      
      // 为特定装备设置自定义图片
      let customImage = null
      if (card.id === 'malong-gear') {
        customImage = '/api/upload/equipment/equipment_026_5.png'
      } else if (card.id === 'wang-gear') {
        customImage = '/api/upload/equipment/equipment_028_equipment.png'
      }
      
      return {
        ...card,
        visual: customImage || target?.mainImage || target?.imageUrl || card.fallbackVisual,
        visualTone: customImage || target?.mainImage || target?.imageUrl ? 'light' : 'dark',
        footer: target ? '点击查看装备详情' : '查看同类装备',
        targetType: target ? 'equipment' : 'list',
        targetId: target?.id || null,
        query: target ? null : card.fallbackQuery
      }
    })
  )
})

const scrollableWaterfallColumns = computed(() => {
  return waterfallColumns.value
})

const waterfallAnimation = (index) => ({
  '--column-duration': index === 0 ? '40s' : '52s',
  '--column-direction': index === 0 ? 'normal' : 'reverse'
})

const goToEquipmentList = (query = {}) => {
  router.push({
    name: 'EquipmentList',
    query
  })
}

const goToLeaderboards = () => {
  router.push('/leaderboards')
}

const handleWaterfallCardClick = (card) => {
  if (card.targetType === 'equipment' && card.targetId) {
    router.push(`/equipment/${card.targetId}`)
    return
  }

  if (card.targetType === 'leaderboard') {
    goToLeaderboards()
    return
  }

  goToEquipmentList(card.query || {})
}

async function loadHomeRecommend() {
  loading.value = true

  try {
    try {
      const primaryResponse = isLoggedIn.value
        ? await getPersonalRecommend({ count: 8 })
        : await getHotRecommend({ count: 8 })

      const primaryList = extractEquipmentRecords(primaryResponse)
      if (primaryList.length) {
        equipmentList.value = primaryList.map((item, index) => normalizeEquipment(item, index))
        return
      }
    } catch (error) {
      // 主推荐失败时回退到装备列表
    }

    try {
      const backupResponse = await equipmentApi.list({
        pageNum: 1,
        pageSize: 8,
        sortBy: 'rating'
      })

      const backupList = extractEquipmentRecords(backupResponse)
      equipmentList.value = backupList.map((item, index) => normalizeEquipment(item, index))
    } catch (error) {
      equipmentList.value = []
    }
  } finally {
    loading.value = false
  }
}

async function loadPreferenceProfile() {
  preferenceReady.value = false
  preferenceProfile.value = null

  if (!isLoggedIn.value) {
    preferenceReady.value = true
    return
  }

  try {
    const res = await getUserPreferences()
    preferenceProfile.value = normalizePreferenceProfile(res.data)
  } catch (error) {
    preferenceProfile.value = null
  } finally {
    preferenceReady.value = true
  }
}

async function loadProfileInsight() {
  profileInsight.value = null

  if (!isLoggedIn.value) {
    return
  }

  try {
    const res = await getProfileInsight()
    profileInsight.value = res.data || null
  } catch (error) {
    profileInsight.value = null
  }
}

async function loadWaterfallCatalog() {
  try {
    const res = await equipmentApi.list({
      pageNum: 1,
      pageSize: 200,
      sortBy: 'rating'
    })
    waterfallCatalog.value = extractEquipmentRecords(res).map((item, index) => normalizeEquipment(item, index))
  } catch (error) {
    waterfallCatalog.value = []
  }
}

async function initializeHomeContent() {
  await Promise.all([
    loadWaterfallCatalog(),
    loadHomeRecommend(),
    loadPreferenceProfile(),
    loadProfileInsight()
  ])
}

function extractEquipmentRecords(response) {
  if (Array.isArray(response?.data)) {
    return response.data
  }

  if (Array.isArray(response?.data?.records)) {
    return response.data.records
  }

  if (Array.isArray(response?.data?.list)) {
    return response.data.list
  }

  if (Array.isArray(response?.rows)) {
    return response.rows
  }

  return []
}

function normalizeEquipment(item, index) {
  const name = item?.name || `精选装备 ${index + 1}`
  const brand = item?.brand || 'PingPong'
  const tonePairs = [
    ['#00e8ff', '#ff7a27'],
    ['#29f0c6', '#00e8ff'],
    ['#ffe780', '#ff7a27'],
    ['#98a8ff', '#00e8ff']
  ]
  const [accent, secondary] = tonePairs[index % tonePairs.length]

  return {
    ...item,
    name,
    brand,
    model: item?.model || '',
    price: item?.price ?? item?.currentPrice ?? 0,
    originalPrice: item?.originalPrice ?? item?.original_price ?? item?.price ?? 0,
    favoriteCount: Number(item?.favoriteCount ?? item?.favorite_count ?? 0),
    viewCount: Number(item?.viewCount ?? item?.view_count ?? 0),
    salesCount: Number(item?.salesCount ?? item?.sales_count ?? 0),
    stock: item?.stock ?? 99,
    rating: Number(item?.rating ?? 4.8),
    mainImage: item?.mainImage || item?.imageUrl || createVisual('product', name, accent, secondary)
  }
}

function pickShowcaseItem(index) {
  return showcasePool.value[index % showcasePool.value.length]
}

function findWaterfallEquipment(targetNames = []) {
  if (!Array.isArray(targetNames) || !targetNames.length) {
    return null
  }

  for (const targetName of targetNames) {
    const normalizedTarget = normalizeEquipmentName(targetName)
    if (!normalizedTarget) {
      continue
    }

    const exact = waterfallCatalog.value.find((item) => normalizeEquipmentName(item.name) === normalizedTarget)
    if (exact) {
      return exact
    }

    const fuzzy = waterfallCatalog.value.find((item) => {
      const normalizedName = normalizeEquipmentName(item.name)
      return normalizedName.includes(normalizedTarget) || normalizedTarget.includes(normalizedName)
    })
    if (fuzzy) {
      return fuzzy
    }
  }

  return null
}

function normalizeEquipmentName(value) {
  return String(value || '')
    .toLowerCase()
    .replace(/[\s·•()（）\-_/]/g, '')
    .trim()
}

function createVisual(type, label, accent = '#00e8ff', secondary = '#ff7a27') {
  const safeLabel = escapeXml(label)
  const svgMap = {
    brand: `
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 520 700">
        <defs>
          <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="#070a10"/>
            <stop offset="100%" stop-color="#101826"/>
          </linearGradient>
          <linearGradient id="line" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="${accent}"/>
            <stop offset="100%" stop-color="${secondary}"/>
          </linearGradient>
        </defs>
        <rect width="520" height="700" rx="34" fill="url(#bg)"/>
        <g opacity="0.14">
          <path d="M40 80h440M40 160h440M40 240h440M40 320h440M40 400h440M40 480h440M40 560h440M40 640h440" stroke="${accent}" />
          <path d="M80 40v620M160 40v620M240 40v620M320 40v620M400 40v620" stroke="${accent}" />
        </g>
        <text x="58" y="102" fill="${accent}" font-size="44" font-weight="800" font-family="Arial, sans-serif">${safeLabel}</text>
        <text x="58" y="156" fill="#ffffff" font-size="56" font-weight="800" font-family="Arial, sans-serif">装备矩阵</text>
        <g transform="translate(54 230)">
          <rect x="0" y="0" width="180" height="250" rx="28" fill="#ffffff"/>
          <rect x="210" y="38" width="206" height="206" rx="34" fill="none" stroke="url(#line)" stroke-width="20"/>
          <circle cx="313" cy="140" r="72" fill="url(#line)" opacity="0.35"/>
          <path d="M62 58c32-26 84-22 110 10c30 38 22 104-22 146c-22 20-46 36-68 46c-30-18-60-44-74-76c-18-44-6-90 28-126c8-8 16-14 26-18z" fill="${accent}" opacity="0.22"/>
          <path d="M118 50c26 30 38 82 28 128c-8 38-28 78-64 120" stroke="${secondary}" stroke-width="14" stroke-linecap="round"/>
          <path d="M84 74c42 16 78 42 102 78" stroke="${accent}" stroke-width="14" stroke-linecap="round"/>
          <rect x="34" y="320" width="340" height="94" rx="22" fill="#ffffff" opacity="0.08"/>
        </g>
      </svg>`,
    ranking: `
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 520 700">
        <defs>
          <linearGradient id="rankBg" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="#ffffff"/>
            <stop offset="100%" stop-color="#eff6ff"/>
          </linearGradient>
        </defs>
        <rect width="520" height="700" rx="34" fill="url(#rankBg)"/>
        <rect x="46" y="52" width="116" height="44" rx="22" fill="#111111"/>
        <text x="76" y="82" fill="#ffffff" font-size="22" font-weight="700" font-family="Arial, sans-serif">TOP 3</text>
        <text x="50" y="144" fill="#111111" font-size="32" font-weight="800" font-family="Arial, sans-serif">${safeLabel}</text>
        <g transform="translate(58 198)">
          <rect x="0" y="0" width="404" height="360" rx="30" fill="#f9fbff" stroke="#dde6f2"/>
          <rect x="106" y="70" width="194" height="210" rx="20" fill="#ffffff" stroke="#d4dce8"/>
          <ellipse cx="202" cy="284" rx="120" ry="26" fill="#d7e0ef"/>
          <path d="M152 106c18-34 84-34 102 0l18 124h-138z" fill="${accent}" opacity="0.18"/>
          <path d="M170 88h64c34 0 64 30 64 66v92H106v-92c0-36 30-66 64-66z" fill="#1a2232"/>
          <rect x="84" y="300" width="236" height="22" rx="11" fill="#101826" opacity="0.08"/>
        </g>
      </svg>`,
    rubber: `
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 520 700">
        <defs>
          <linearGradient id="rbg" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="#070a10"/>
            <stop offset="100%" stop-color="#0d1620"/>
          </linearGradient>
          <linearGradient id="rubber" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="${accent}"/>
            <stop offset="100%" stop-color="${secondary}"/>
          </linearGradient>
        </defs>
        <rect width="520" height="700" rx="34" fill="url(#rbg)"/>
        <text x="56" y="96" fill="${accent}" font-size="42" font-weight="800" font-family="Arial, sans-serif">SPIN</text>
        <text x="56" y="144" fill="#ffffff" font-size="50" font-weight="800" font-family="Arial, sans-serif">RUBBER</text>
        <g transform="translate(66 208)">
          <rect x="0" y="0" width="320" height="320" rx="38" fill="url(#rubber)" opacity="0.18"/>
          <rect x="22" y="22" width="276" height="276" rx="28" fill="#141f2a" stroke="${accent}" stroke-width="2"/>
          <g opacity="0.2">
            <path d="M36 70h248M36 110h248M36 150h248M36 190h248M36 230h248M36 270h248" stroke="${accent}"/>
            <path d="M70 36v248M110 36v248M150 36v248M190 36v248M230 36v248M270 36v248" stroke="${accent}"/>
          </g>
          <circle cx="264" cy="264" r="86" fill="url(#rubber)" opacity="0.72"/>
          <circle cx="264" cy="264" r="46" fill="#ffffff" opacity="0.14"/>
        </g>
      </svg>`,
    shoe: `
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 520 700">
        <defs>
          <linearGradient id="sbg" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="#ffffff"/>
            <stop offset="100%" stop-color="#eef5ff"/>
          </linearGradient>
          <linearGradient id="shoe" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="${accent}"/>
            <stop offset="100%" stop-color="${secondary}"/>
          </linearGradient>
        </defs>
        <rect width="520" height="700" rx="34" fill="url(#sbg)"/>
        <text x="50" y="90" fill="#111111" font-size="34" font-weight="800" font-family="Arial, sans-serif">${safeLabel}</text>
        <g transform="translate(48 210)">
          <ellipse cx="208" cy="290" rx="190" ry="36" fill="#dde6f2"/>
          <path d="M66 208c20-52 84-112 142-132c66-22 132 20 162 72l48 66c8 10 0 24-12 24H86c-18 0-28-12-20-30z" fill="url(#shoe)"/>
          <path d="M132 146h118c54 0 108 40 136 92" stroke="#ffffff" stroke-width="10" stroke-linecap="round" opacity="0.72"/>
          <path d="M106 238h282" stroke="#ffffff" stroke-width="12" stroke-linecap="round"/>
          <path d="M122 278h252" stroke="#111111" stroke-width="10" stroke-linecap="round" opacity="0.14"/>
          <circle cx="146" cy="250" r="12" fill="#ffffff"/>
          <circle cx="190" cy="248" r="12" fill="#ffffff"/>
          <circle cx="234" cy="246" r="12" fill="#ffffff"/>
          <circle cx="278" cy="244" r="12" fill="#ffffff"/>
        </g>
      </svg>`,
    community: `
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 520 700">
        <defs>
          <linearGradient id="cbg" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="#090c13"/>
            <stop offset="100%" stop-color="#0e1826"/>
          </linearGradient>
          <linearGradient id="cfill" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="${accent}"/>
            <stop offset="100%" stop-color="${secondary}"/>
          </linearGradient>
        </defs>
        <rect width="520" height="700" rx="34" fill="url(#cbg)"/>
        <text x="52" y="94" fill="${accent}" font-size="40" font-weight="800" font-family="Arial, sans-serif">COMMUNITY</text>
        <g transform="translate(58 182)">
          <rect x="0" y="0" width="404" height="420" rx="28" fill="#ffffff"/>
          <circle cx="206" cy="118" r="58" fill="#d7dff0"/>
          <path d="M112 300c28-76 82-124 146-124c64 0 118 48 146 124" fill="#0f1725"/>
          <path d="M134 232c24 10 52 14 72 14c26 0 50-4 74-14" stroke="url(#cfill)" stroke-width="14" stroke-linecap="round"/>
          <rect x="116" y="324" width="180" height="34" rx="17" fill="#111111"/>
          <text x="136" y="347" fill="#ffffff" font-size="20" font-weight="700" font-family="Arial, sans-serif">打法穿搭合集</text>
        </g>
      </svg>`,
    gift: `
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 520 700">
        <defs>
          <linearGradient id="gbg" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="#09111a"/>
            <stop offset="100%" stop-color="#142433"/>
          </linearGradient>
          <linearGradient id="gift" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="${accent}"/>
            <stop offset="100%" stop-color="${secondary}"/>
          </linearGradient>
        </defs>
        <rect width="520" height="700" rx="34" fill="url(#gbg)"/>
        <g transform="translate(92 190)">
          <rect x="0" y="120" width="336" height="180" rx="28" fill="url(#gift)"/>
          <rect x="26" y="72" width="284" height="92" rx="24" fill="#1bdcf7"/>
          <rect x="150" y="72" width="36" height="228" rx="18" fill="#0d1620"/>
          <rect x="0" y="168" width="336" height="36" rx="18" fill="#0d1620"/>
          <path d="M170 58c-10-28-34-46-64-46c-26 0-40 16-40 34c0 30 28 40 72 40h32V58z" fill="#ffffff"/>
          <path d="M166 58c10-28 34-46 64-46c26 0 40 16 40 34c0 30-28 40-72 40h-32V58z" fill="#ffffff"/>
        </g>
      </svg>`,
    compare: `
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 520 700">
        <defs>
          <linearGradient id="cmpbg" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="#070a10"/>
            <stop offset="100%" stop-color="#101826"/>
          </linearGradient>
          <linearGradient id="cmp" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="${accent}"/>
            <stop offset="100%" stop-color="${secondary}"/>
          </linearGradient>
        </defs>
        <rect width="520" height="700" rx="34" fill="url(#cmpbg)"/>
        <g transform="translate(52 148)">
          <rect x="0" y="0" width="176" height="402" rx="28" fill="#0f1725" stroke="${accent}" stroke-opacity="0.3"/>
          <rect x="240" y="0" width="176" height="402" rx="28" fill="#0f1725" stroke="${secondary}" stroke-opacity="0.3"/>
          <circle cx="88" cy="120" r="54" fill="${accent}" opacity="0.16"/>
          <circle cx="328" cy="120" r="54" fill="${secondary}" opacity="0.18"/>
          <rect x="48" y="228" width="84" height="120" rx="22" fill="${accent}" opacity="0.22"/>
          <rect x="288" y="212" width="84" height="136" rx="22" fill="${secondary}" opacity="0.3"/>
          <path d="M170 230l68-38" stroke="#ffffff" stroke-width="12" stroke-linecap="round"/>
          <path d="M170 286h68" stroke="#ffffff" stroke-width="12" stroke-linecap="round"/>
          <path d="M170 342l68 38" stroke="#ffffff" stroke-width="12" stroke-linecap="round"/>
        </g>
      </svg>`,
    ai: `
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 520 700">
        <defs>
          <linearGradient id="aibg" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="#09101a"/>
            <stop offset="100%" stop-color="#131f34"/>
          </linearGradient>
          <linearGradient id="aifill" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="${accent}"/>
            <stop offset="100%" stop-color="${secondary}"/>
          </linearGradient>
        </defs>
        <rect width="520" height="700" rx="34" fill="url(#aibg)"/>
        <g transform="translate(74 170)">
          <rect x="48" y="0" width="276" height="276" rx="34" fill="#0d1620" stroke="${accent}" stroke-opacity="0.45"/>
          <circle cx="186" cy="138" r="72" fill="url(#aifill)" opacity="0.24"/>
          <circle cx="186" cy="138" r="28" fill="#ffffff"/>
          <path d="M54 374c54-48 124-74 198-74c76 0 146 26 200 74" stroke="${accent}" stroke-width="18" stroke-linecap="round"/>
          <circle cx="72" cy="394" r="16" fill="${accent}"/>
          <circle cx="300" cy="394" r="16" fill="${secondary}"/>
          <circle cx="372" cy="394" r="16" fill="#ffffff"/>
        </g>
      </svg>`,
    shipping: `
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 520 700">
        <defs>
          <linearGradient id="ship" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="${accent}"/>
            <stop offset="100%" stop-color="${secondary}"/>
          </linearGradient>
        </defs>
        <rect width="520" height="700" rx="34" fill="#0b1119"/>
        <g transform="translate(76 186)">
          <path d="M42 292h286c20 0 38-10 48-28l42-74c8-16-4-36-24-36h-60l-26-72H136l-38 74H54c-18 0-30 18-22 34l24 52h-14c-20 0-36 16-36 36v14c0 14 10 24 24 24z" fill="url(#ship)"/>
          <rect x="82" y="94" width="148" height="86" rx="18" fill="#ffffff" opacity="0.78"/>
          <circle cx="124" cy="322" r="30" fill="#ffffff"/>
          <circle cx="302" cy="322" r="30" fill="#ffffff"/>
        </g>
      </svg>`,
    kit: `
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 520 700">
        <defs>
          <linearGradient id="kitbg" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="#ffffff"/>
            <stop offset="100%" stop-color="#eef5ff"/>
          </linearGradient>
          <linearGradient id="kitfill" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="${accent}"/>
            <stop offset="100%" stop-color="${secondary}"/>
          </linearGradient>
        </defs>
        <rect width="520" height="700" rx="34" fill="url(#kitbg)"/>
        <g transform="translate(70 184)">
          <rect x="0" y="0" width="138" height="188" rx="28" fill="#0f1725"/>
          <rect x="174" y="0" width="208" height="132" rx="28" fill="${accent}" opacity="0.24"/>
          <rect x="174" y="164" width="208" height="188" rx="28" fill="#ffffff" stroke="#d8e1ef"/>
          <circle cx="76" cy="92" r="46" fill="url(#kitfill)" opacity="0.86"/>
          <rect x="226" y="204" width="104" height="104" rx="28" fill="url(#kitfill)" opacity="0.8"/>
        </g>
      </svg>`,
    product: `
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 520 700">
        <defs>
          <linearGradient id="pbg" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="#ffffff"/>
            <stop offset="100%" stop-color="#eef4fb"/>
          </linearGradient>
          <linearGradient id="pfill" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="${accent}"/>
            <stop offset="100%" stop-color="${secondary}"/>
          </linearGradient>
        </defs>
        <rect width="520" height="700" rx="34" fill="url(#pbg)"/>
        <text x="50" y="94" fill="#111111" font-size="30" font-weight="800" font-family="Arial, sans-serif">${safeLabel}</text>
        <g transform="translate(64 176)">
          <rect x="40" y="8" width="260" height="320" rx="36" fill="url(#pfill)" opacity="0.18"/>
          <path d="M178 58c68 24 126 90 138 160c8 50-6 94-38 120c-28 22-70 32-112 30c-66-4-118-42-134-94c-14-48-4-98 34-148c30-38 68-62 112-68z" fill="#ffffff" stroke="#cfd9e6"/>
          <path d="M186 92c48 16 92 58 110 106" stroke="url(#pfill)" stroke-width="16" stroke-linecap="round"/>
          <path d="M148 208c30 26 58 58 84 98" stroke="${secondary}" stroke-width="16" stroke-linecap="round"/>
          <circle cx="284" cy="82" r="22" fill="${accent}" opacity="0.82"/>
        </g>
      </svg>`
  }

  return toDataUri(svgMap[type] || svgMap.product)
}

function toDataUri(svg) {
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg.replace(/\n+/g, ' ').trim())}`
}

function escapeXml(value = '') {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function handleInsightAction(action) {
  if (!action?.targetPath) {
    return
  }

  if (action.query && Object.keys(action.query).length) {
    router.push({
      path: action.targetPath,
      query: normalizeRouteQuery(action.query)
    })
    return
  }

  router.push(action.targetPath)
}

function normalizeRouteQuery(query = {}) {
  return Object.entries(query).reduce((result, [key, value]) => {
    if (value === undefined || value === null || value === '') {
      return result
    }
    result[key] = String(value)
    return result
  }, {})
}

function normalizePreferenceProfile(profile) {
  if (!profile) {
    return null
  }

  return {
    ...profile,
    preferredCategories: safeJsonParse(profile.preferredCategories, []),
    preferredBrands: safeJsonParse(profile.preferredBrands, []),
    preferenceTags: safeJsonParse(profile.preferenceTags, []),
    answerSnapshot: safeJsonParse(profile.answerSnapshot, {})
  }
}

function safeJsonParse(value, fallback) {
  if (!value) {
    return fallback
  }

  if (typeof value !== 'string') {
    return value
  }

  try {
    return JSON.parse(value)
  } catch (error) {
    return fallback
  }
}

function hasProfileData(profile) {
  if (!profile) {
    return false
  }

  return Boolean([
    profile.level,
    profile.playStyle,
    profile.gripStyle,
    profile.weeklyFrequency,
    profile.primaryGoal,
    profile.priorityFocus,
    profile.budgetMin,
    profile.budgetMax
  ].some(Boolean) || profile.preferenceTags?.length)
}

function formatBudgetRange(profile) {
  if (!profile) {
    return ''
  }

  const min = Number(profile.budgetMin ?? 0)
  const max = Number(profile.budgetMax ?? 0)

  if (!min && !max) {
    return ''
  }

  if (max && max <= 300) {
    return '300 元以内'
  }

  if (min >= 1500 && max >= 1500) {
    return '1500 元以上'
  }

  if (min && max) {
    return `${min} - ${max} 元`
  }

  if (max) {
    return `${max} 元以内`
  }

  return `${min} 元以上`
}

onMounted(() => {
  initializeHomeContent()

  stopRealtimeSubscription = subscribeRealtimeUpdates((message) => {
    if (!isRealtimeEvent(message, ['equipment_changed', 'comment_changed', 'leaderboard_refreshed'])) {
      return
    }
    scheduleRealtimeRefresh()
  })

  dotTimer = window.setInterval(() => {
    if (!waterfallPaused.value) {
      activeDotIndex.value = (activeDotIndex.value + 1) % 4
    }
  }, 2200)
})

watch(() => userStore.token, async (nextToken, previousToken) => {
  if (nextToken === previousToken) {
    return
  }
  await initializeHomeContent()
})

onUnmounted(() => {
  if (dotTimer) {
    window.clearInterval(dotTimer)
  }
  if (stopRealtimeSubscription) {
    stopRealtimeSubscription()
  }
  if (realtimeRefreshTimer) {
    window.clearTimeout(realtimeRefreshTimer)
  }
})

function scheduleRealtimeRefresh() {
  if (realtimeRefreshTimer) {
    window.clearTimeout(realtimeRefreshTimer)
  }
  realtimeRefreshTimer = window.setTimeout(() => {
    initializeHomeContent()
  }, 250)
}
</script>

<style scoped lang="scss">
.home-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top right, rgba(0, 141, 255, 0.14), transparent 30%),
    radial-gradient(circle at top left, rgba(255, 133, 42, 0.12), transparent 26%),
    linear-gradient(180deg, #f4f8ff 0%, #ffffff 28%, #f7fbff 100%);
}

.home-main {
  padding-bottom: 30px;
}

.hero-section {
  padding: 16px 20px 24px;
}

.hero-shell {
  max-width: 100%;
  margin: 0;
  display: grid;
  grid-template-columns: 52fr 48fr;
  gap: 0;
  overflow: hidden;
  border-radius: 0;
  height: 460px;
}

.hero-copy {
  position: relative;
  height: 460px;
  padding: 0;
  background: #070a10;
  color: #ffffff;
}

.hero-copy-main {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 5;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  padding: 60px 50px;
  background: linear-gradient(90deg, rgba(7, 10, 16, 0.45) 0%, rgba(7, 10, 16, 0.2) 50%, transparent 100%);
  pointer-events: none;
}

.hero-copy::after {
  content: '';
  position: absolute;
  inset: 14px;
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  pointer-events: none;
}

.hero-copy-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.11);
  color: rgba(255, 255, 255, 0.94);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  backdrop-filter: blur(12px);
}

.hero-title {
  margin: 10px 0;
  display: flex;
  flex-direction: column;
  font-size: clamp(28px, 4vw, 48px);
  line-height: 1.1;
  font-weight: 900;
  letter-spacing: -0.02em;
}

.hero-title span {
  display: block;
}

.hero-subtitle {
  max-width: 420px;
  margin: 0;
  color: rgba(233, 240, 255, 0.7);
  font-size: clamp(14px, 1.2vw, 18px);
  line-height: 1.6;
  font-weight: 500;
}


.hero-champion-panel {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 0;
}

.champion-panel-kicker {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(0, 232, 255, 0.16);
  color: #a7fbff;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.champion-panel-chip {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 4;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(7, 15, 37, 0.46);
  border: 1px solid rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(14px);
}

.champion-panel-note {
  color: rgba(232, 239, 255, 0.82);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.03em;
}



.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.hero-stat-card {
  padding: 16px 14px 14px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(93, 112, 197, 0.28), rgba(75, 97, 180, 0.14));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.hero-stat-card strong {
  display: block;
  font-size: clamp(24px, 2.5vw, 32px);
  line-height: 1;
  font-weight: 900;
}

.hero-stat-card span {
  display: block;
  margin-top: 6px;
  color: rgba(230, 237, 255, 0.82);
  font-size: 13px;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 14px;
}

.explore-btn {
  min-width: 176px;
  height: 50px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #ff7a27, #ffb35d);
  color: #ffffff;
  font-size: 16px;
  font-weight: 800;
  box-shadow: 0 20px 42px rgba(255, 122, 39, 0.34);
}

.secondary-link {
  border: none;
  background: transparent;
  color: rgba(238, 244, 255, 0.8);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.hero-top-link,
.hero-inline-link {
  padding: 0;
}

.hero-waterfall {
  position: relative;
  height: 460px;
  padding: 10px 12px;
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgba(0, 232, 255, 0.16), transparent 18%),
    radial-gradient(circle at center, rgba(255, 122, 39, 0.1), transparent 30%),
    linear-gradient(180deg, #070a10 0%, #0b1118 100%);
}

.waterfall-surface {
  position: absolute;
  inset: 12px;
  border-radius: 26px;
  background:
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 24px 24px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.05);
  opacity: 0.82;
}

.hero-waterfall::before {
  content: '';
  position: absolute;
  inset: -20% auto auto -10%;
  width: 320px;
  height: 320px;
  background: radial-gradient(circle, rgba(0, 232, 255, 0.18), transparent 70%);
  filter: blur(30px);
}

.hero-waterfall::after {
  content: '';
  position: absolute;
  right: -90px;
  bottom: -100px;
  width: 360px;
  height: 360px;
  background: radial-gradient(circle, rgba(255, 122, 39, 0.16), transparent 72%);
  filter: blur(34px);
}

.pause-hint {
  position: absolute;
  top: 18px;
  right: 16px;
  z-index: 3;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: #eaf4ff;
  font-size: 12px;
  backdrop-filter: blur(12px);
}

.pause-hint .el-icon {
  font-size: 18px;
  color: #00e8ff;
}

.floating-badge {
  position: absolute;
  z-index: 3;
  display: inline-flex;
  align-items: center;
  padding: 10px 16px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.04em;
  backdrop-filter: blur(10px);
  box-shadow: 0 14px 24px rgba(0, 0, 0, 0.22);
}

.floating-badge.cyan {
  background: rgba(8, 237, 255, 0.16);
  color: #7bf7ff;
  border: 1px solid rgba(123, 247, 255, 0.26);
}

.floating-badge.orange {
  background: rgba(255, 122, 39, 0.18);
  color: #ffd1b4;
  border: 1px solid rgba(255, 160, 110, 0.26);
}

.floating-badge.white {
  background: rgba(255, 255, 255, 0.9);
  color: #121826;
}

.waterfall-container {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  height: 100%;
  padding: 10px 6px;
}

.hero-waterfall.paused .waterfall-column {
  animation-play-state: paused;
}

.waterfall-column {
  display: flex;
  flex-direction: column;
  height: fit-content;
  animation: verticalScroll var(--column-duration, 50s) linear infinite;
  animation-direction: var(--column-direction, normal);
}

.column-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

@keyframes verticalScroll {
  0% {
    transform: translateY(0);
  }
  100% {
    transform: translateY(-50%);
  }
}

.waterfall-card {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  gap: 0;
  width: 100%;
  padding: 0;
  overflow: hidden;
  border: none;
  border-radius: 10px;
  background: #0f1620;
  text-align: left;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.waterfall-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 26px 42px rgba(0, 0, 0, 0.26);
}

.waterfall-card::after {
  content: '';
  position: absolute;
  top: -40%;
  left: -40%;
  width: 42%;
  height: 180%;
  background: linear-gradient(180deg, transparent, rgba(255, 255, 255, 0.12), transparent);
  transform: rotate(24deg);
  opacity: 0;
  transition: transform 0.6s ease, opacity 0.4s ease;
}

.waterfall-card:hover::after {
  opacity: 1;
  transform: translateX(320%) rotate(24deg);
}

.tone-dark {
  background: rgba(7, 10, 16, 0.94);
  color: #ffffff;
}

.tone-light {
  background: linear-gradient(180deg, #ffffff 0%, #f7fbff 100%);
  color: #111111;
}

.tone-accent {
  background: linear-gradient(180deg, rgba(255, 119, 62, 0.96), rgba(255, 148, 83, 0.92));
  color: #ffffff;
  border-color: rgba(255, 206, 170, 0.26);
}

.size-tall {
  min-height: 232px;
}

.size-medium {
  min-height: 176px;
}

.size-short {
  min-height: 128px;
}

.size-mini {
  min-height: 100px;
}

.hero-champion-panel :deep(.champion-carousel-widget) {
  height: 100%;
  min-height: 100%;
}

.hero-champion-panel :deep(.carousel-stage) {
  height: 460px;
  min-height: 460px;
  border-radius: 0;
  background: #070a10;
}

.hero-champion-panel :deep(.carousel-slide) {
  height: 460px;
  min-height: 460px;
}

.hero-champion-panel :deep(.carousel-nav),
.hero-champion-panel :deep(.carousel-footer) {
  display: none !important;
}

.hero-champion-panel :deep(.carousel-counter) {
  top: 40px;
  right: 40px;
  background: rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.1);
  font-family: inherit;
}

.hero-champion-panel :deep(.carousel-footer) {
  position: absolute;
  right: 18px;
  bottom: 16px;
  z-index: 5;
  justify-content: flex-end;
  padding: 0;
  pointer-events: none;
}

.hero-champion-panel :deep(.carousel-indicators) {
  pointer-events: auto;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(7, 15, 37, 0.44);
  border: 1px solid rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(14px);
}

.hero-champion-panel :deep(.carousel-status) {
  display: none;
}

.card-gridline {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 22px 22px;
  opacity: 0.26;
  pointer-events: none;
}

.tone-light .card-gridline {
  background:
    linear-gradient(rgba(18, 24, 38, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(18, 24, 38, 0.04) 1px, transparent 1px);
}

.card-copy,
.card-visual-shell {
  position: relative;
  z-index: 1;
}

.card-copy {
  padding: 14px 12px 16px;
}

.card-accent {
  font-size: 14px;
  line-height: 1.2;
  font-weight: 700;
  color: #00e8ff;
  text-shadow: none;
}

.tone-light .card-accent {
  color: #0b1b39;
  text-shadow: none;
}

.tone-accent .card-accent {
  color: #ffffff;
  text-shadow: 0 0 20px rgba(255, 255, 255, 0.18);
}

.card-copy h3 {
  margin: 4px 0 0;
  font-size: 20px;
  line-height: 1.25;
  font-weight: 800;
}

.size-medium .card-copy h3 {
  font-size: clamp(24px, 2vw, 34px);
}

.size-short .card-copy h3 {
  font-size: clamp(20px, 1.7vw, 28px);
}

.size-mini .card-copy h3 {
  font-size: clamp(17px, 1.35vw, 21px);
  line-height: 1.28;
}

.card-copy p {
  margin: 12px 0 0;
  color: rgba(255, 255, 255, 0.76);
  line-height: 1.65;
  font-size: 15px;
}

.size-mini .card-copy p {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.55;
}

.tone-light .card-copy p {
  color: #5b6576;
}

.tone-accent .card-copy p {
  color: rgba(255, 255, 255, 0.86);
}

.card-visual-shell {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100px;
  border-radius: 0;
  overflow: hidden;
}

.visual-dark {
  background: rgba(255, 255, 255, 0.03);
}

.visual-light {
  background: rgba(255, 255, 255, 0.9);
  box-shadow: inset 0 0 0 1px rgba(18, 24, 38, 0.05);
}

.card-visual {
  width: 100%;
  height: auto;
  display: block;
  object-fit: cover;
  transform: scale(1.02);
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
  font-weight: 700;
}

.card-footer-main {
  color: rgba(255, 255, 255, 0.78);
}

.tone-light .card-footer-main {
  color: #556173;
}

.stage-dots {
  position: absolute;
  right: 24px;
  bottom: 24px;
  z-index: 3;
  display: flex;
  gap: 10px;
}

.stage-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  transition: transform 0.3s ease, background 0.3s ease;
}

.stage-dot.active {
  background: #00e8ff;
  transform: scale(1.55);
  box-shadow: 0 0 18px rgba(0, 232, 255, 0.55);
}

.section-head {
  max-width: 1320px;
  margin: 0 auto 24px;
  padding: 0 24px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.section-kicker {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(16, 60, 110, 0.08);
  color: #2957b5;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.section-head h2 {
  margin: 16px 0 0;
  color: #12245a;
  font-size: clamp(30px, 4vw, 44px);
  line-height: 1.1;
}

.section-head p {
  max-width: 720px;
  margin: 12px 0 0;
  color: #64708a;
  line-height: 1.8;
}

.section-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: none;
  background: transparent;
  color: #1f4ea6;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
}

.category-section,
.recommend-section,
.feature-section {
  padding: 0 20px 34px;
}

.section-surface {
  position: relative;
  max-width: 1320px;
  margin: 0 auto;
  padding: 28px 0 30px;
  border-radius: 36px;
  border: 1px solid rgba(18, 36, 90, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 251, 255, 0.92));
  box-shadow: 0 26px 60px rgba(27, 47, 89, 0.07);
  overflow: hidden;
}

.section-surface::before {
  content: '';
  position: absolute;
  inset: 0 auto auto 0;
  width: 220px;
  height: 220px;
  background: radial-gradient(circle, rgba(0, 232, 255, 0.12), transparent 70%);
  pointer-events: none;
}

.section-surface::after {
  content: '';
  position: absolute;
  inset: auto 0 0 auto;
  width: 260px;
  height: 260px;
  background: radial-gradient(circle, rgba(255, 122, 39, 0.12), transparent 72%);
  pointer-events: none;
}

.category-surface,
.recommend-surface,
.feature-surface {
  backdrop-filter: blur(14px);
}

.category-grid,
.feature-grid,
.curated-fallback-grid {
  padding: 0 24px;
  display: grid;
  gap: 18px;
}

.category-layout,
.feature-layout,
.recommend-stage {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 20px;
  padding: 0 24px;
}

.category-layout {
  grid-template-columns: minmax(0, 1fr) 320px;
  align-items: start;
}

.category-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  padding: 0;
}

.category-card {
  padding: 26px;
  border: 1px solid rgba(18, 36, 90, 0.08);
  border-radius: 30px;
  background:
    radial-gradient(circle at top right, rgba(0, 232, 255, 0.12), transparent 30%),
    linear-gradient(180deg, #ffffff, #f8fbff);
  text-align: left;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.category-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 22px 40px rgba(28, 54, 90, 0.1);
}

.category-label {
  display: inline-flex;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255, 122, 39, 0.12);
  color: #d35b10;
  font-size: 12px;
  font-weight: 800;
}

.category-card h3 {
  margin: 18px 0 10px;
  color: #12245a;
  font-size: 24px;
  line-height: 1.35;
}

.category-card p {
  margin: 0;
  color: #64708a;
  line-height: 1.7;
}

.category-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.category-tags span {
  padding: 8px 12px;
  border-radius: 999px;
  background: #f0f5ff;
  color: #385281;
  font-size: 13px;
  font-weight: 700;
}

.category-aside,
.recommend-aside {
  display: grid;
  gap: 16px;
}

.category-aside-card,
.recommend-aside-panel,
.feature-lead-card {
  position: relative;
  padding: 22px 22px 20px;
  border-radius: 28px;
  border: 1px solid rgba(18, 36, 90, 0.08);
  background: rgba(255, 255, 255, 0.84);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.category-aside-card.primary,
.recommend-aside-panel.primary,
.feature-lead-card {
  background:
    radial-gradient(circle at top right, rgba(0, 232, 255, 0.16), transparent 28%),
    linear-gradient(145deg, #12245a, #224ea3 58%, #2f6bdd 100%);
  color: #ffffff;
  border-color: rgba(19, 49, 116, 0.22);
}

.aside-label {
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

.category-aside-card.primary .aside-label,
.recommend-aside-panel.primary .aside-label,
.feature-lead-card .aside-label {
  background: rgba(255, 255, 255, 0.14);
  color: rgba(255, 255, 255, 0.92);
}

.category-aside-card h3,
.recommend-aside-panel h3,
.feature-lead-card h3 {
  margin: 16px 0 10px;
  font-size: 28px;
  line-height: 1.25;
  color: inherit;
}

.category-aside-card p,
.recommend-aside-panel p,
.feature-lead-card p {
  margin: 0;
  color: inherit;
  line-height: 1.8;
}

.category-aside-card.primary p,
.recommend-aside-panel.primary p,
.feature-lead-card p {
  color: rgba(237, 243, 255, 0.82);
}

.aside-check-item {
  display: grid;
  gap: 6px;
  padding: 14px 0;
  border-bottom: 1px solid rgba(18, 36, 90, 0.08);
}

.aside-check-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.aside-check-item:first-child {
  padding-top: 0;
}

.aside-check-item strong {
  color: #12245a;
  font-size: 16px;
}

.aside-check-item span {
  color: #64708a;
  line-height: 1.7;
  font-size: 14px;
}

.recommend-loading,
.recommend-grid {
  padding: 0;
}

.preference-spotlight-shell {
  max-width: 1320px;
  margin: 0 auto 24px;
  padding: 0 24px;
}

.insight-board-shell {
  max-width: 1320px;
  margin: 0 auto 24px;
  padding: 0 24px;
}

.preference-spotlight {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(280px, 1fr) auto;
  gap: 18px;
  padding: 24px 26px;
  border-radius: 32px;
  border: 1px solid rgba(18, 36, 90, 0.08);
  background:
    radial-gradient(circle at top right, rgba(0, 232, 255, 0.16), transparent 26%),
    radial-gradient(circle at bottom left, rgba(255, 122, 39, 0.12), transparent 24%),
    linear-gradient(135deg, #ffffff 0%, #f6faff 100%);
  box-shadow: 0 22px 44px rgba(28, 54, 90, 0.08);
}

.preference-spotlight.empty {
  grid-template-columns: minmax(0, 1fr) auto;
}

.preference-copy h3 {
  margin: 10px 0 12px;
  color: #12245a;
  font-size: clamp(24px, 3vw, 32px);
  line-height: 1.25;
}

.preference-copy p {
  margin: 0;
  color: #5f6f8f;
  line-height: 1.8;
}

.preference-eyebrow {
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

.preference-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.preference-tag {
  display: inline-flex;
  align-items: center;
  padding: 9px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  color: #27457b;
  font-size: 13px;
  font-weight: 700;
  box-shadow: inset 0 0 0 1px rgba(39, 69, 123, 0.08);
}

.preference-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.preference-metric {
  padding: 16px 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.8);
  box-shadow: inset 0 0 0 1px rgba(39, 69, 123, 0.08);
}

.preference-metric span {
  display: block;
  color: #6b7895;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.preference-metric strong {
  display: block;
  margin-top: 10px;
  color: #12245a;
  font-size: 18px;
  line-height: 1.35;
}

.preference-actions {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: center;
  gap: 12px;
}

.preference-btn {
  min-width: 160px;
  min-height: 46px;
  padding: 0 18px;
  border-radius: 999px;
  border: 1px solid rgba(31, 78, 166, 0.14);
  background: rgba(255, 255, 255, 0.9);
  color: #1f4ea6;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.preference-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(31, 78, 166, 0.12);
}

.preference-btn.primary {
  border-color: transparent;
  background: linear-gradient(135deg, #1f4ea6, #00a6ff);
  color: #ffffff;
  box-shadow: 0 16px 30px rgba(31, 78, 166, 0.22);
}

.insight-board {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 18px;
  padding: 24px 26px;
  border-radius: 32px;
  border: 1px solid rgba(18, 36, 90, 0.08);
  background:
    radial-gradient(circle at top right, rgba(31, 78, 166, 0.12), transparent 24%),
    radial-gradient(circle at bottom left, rgba(255, 122, 39, 0.1), transparent 24%),
    linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 18px 40px rgba(28, 54, 90, 0.08);
}

.insight-kicker,
.insight-todo-title {
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

.insight-main h3 {
  margin: 12px 0 10px;
  color: #12245a;
  font-size: clamp(24px, 2.8vw, 34px);
  line-height: 1.2;
}

.insight-main p {
  margin: 0;
  color: #5f6f8f;
  line-height: 1.8;
}

.insight-mode-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.insight-inline-actions {
  margin-top: 16px;
}

.insight-mode {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.84);
  color: #24416f;
  font-size: 13px;
  font-weight: 700;
  box-shadow: inset 0 0 0 1px rgba(36, 65, 111, 0.08);
}

.insight-link-button {
  min-height: 42px;
  padding: 0 18px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #12245a, #2f64d1);
  color: #ffffff;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 14px 28px rgba(18, 36, 90, 0.18);
}

.insight-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.insight-tag {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(31, 78, 166, 0.08);
  color: #21498e;
  font-size: 13px;
  font-weight: 700;
}

.insight-signal-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.insight-signal-card {
  padding: 16px 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: inset 0 0 0 1px rgba(31, 78, 166, 0.08);
}

.insight-signal-card span {
  display: block;
  color: #6f7b95;
  font-size: 12px;
  font-weight: 700;
}

.insight-signal-card strong {
  display: block;
  margin-top: 10px;
  color: #12245a;
  font-size: 24px;
}

.insight-signal-card p {
  margin: 10px 0 0;
  color: #5f6f8f;
  font-size: 13px;
  line-height: 1.7;
}

.insight-todo {
  margin-top: 16px;
  padding: 22px 24px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(18, 36, 90, 0.08);
}

.insight-todo-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.insight-todo-item {
  padding: 14px 16px;
  border-radius: 18px;
  background: linear-gradient(135deg, #f7faff, #ffffff);
  color: #42526f;
  line-height: 1.75;
  box-shadow: inset 0 0 0 1px rgba(31, 78, 166, 0.06);
}

.insight-action-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.insight-action-card {
  padding: 22px;
  border: 1px solid rgba(18, 36, 90, 0.08);
  border-radius: 28px;
  background: linear-gradient(180deg, #ffffff 0%, #f7fbff 100%);
  text-align: left;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.insight-action-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 18px 36px rgba(28, 54, 90, 0.1);
}

.insight-action-copy span {
  display: inline-flex;
  padding: 6px 11px;
  border-radius: 999px;
  background: rgba(255, 122, 39, 0.12);
  color: #d35b10;
  font-size: 12px;
  font-weight: 800;
}

.insight-action-copy h3 {
  margin: 16px 0 10px;
  color: #12245a;
  font-size: 24px;
  line-height: 1.3;
}

.insight-action-copy p {
  margin: 0;
  color: #64708a;
  line-height: 1.8;
}

.insight-action-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.insight-action-tags span {
  padding: 8px 12px;
  border-radius: 999px;
  background: #f0f5ff;
  color: #395589;
  font-size: 12px;
  font-weight: 700;
}

.recommend-loading {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.recommend-stage {
  grid-template-columns: minmax(0, 1fr) 300px;
  align-items: start;
}

.recommend-main {
  min-width: 0;
}

.recommend-item {
  min-width: 0;
}

.curated-fallback-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.waterfall-card .card-footer {
  display: none;
}
.curated-fallback-card {
  padding: 18px;
  border: 1px solid rgba(18, 36, 90, 0.08);
  border-radius: 28px;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.curated-fallback-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 24px 44px rgba(28, 54, 90, 0.1);
}

.curated-image {
  overflow: hidden;
  border-radius: 22px;
  background: #f2f7ff;
}

.curated-image img {
  display: block;
  width: 100%;
}

.curated-copy span {
  display: inline-flex;
  margin-top: 16px;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(0, 232, 255, 0.1);
  color: #007a9b;
  font-size: 12px;
  font-weight: 800;
}

.curated-copy h3 {
  margin: 16px 0 10px;
  color: #12245a;
  font-size: 24px;
}

.curated-copy p {
  margin: 0;
  color: #64708a;
  line-height: 1.8;
}

.feature-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  padding: 0;
}

.feature-card {
  position: relative;
  padding: 28px;
  border-radius: 30px;
  background:
    radial-gradient(circle at top right, rgba(255, 122, 39, 0.12), transparent 32%),
    linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid rgba(18, 36, 90, 0.08);
  box-shadow: 0 18px 34px rgba(30, 52, 95, 0.07);
}

.feature-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 18px;
  background: linear-gradient(135deg, #16356f, #2f6bdd);
  color: #ffffff;
  font-size: 22px;
}

.feature-index {
  display: inline-flex;
  margin-top: 18px;
  color: #ff7a27;
  font-size: 14px;
  font-weight: 900;
  letter-spacing: 0.18em;
}

.feature-card h3 {
  margin: 18px 0 10px;
  color: #12245a;
  font-size: 24px;
}

.feature-card p {
  margin: 0;
  color: #64708a;
  line-height: 1.8;
}

/* Removed old floatColumn animation in favor of verticalScroll */

@media (max-width: 1380px) {
  .hero-shell {
    grid-template-columns: 1fr;
  }

  .hero-waterfall {
    min-height: 860px;
  }
}

@media (max-width: 1180px) {
  .waterfall-grid,
  .recommend-grid,
  .recommend-loading,
  .category-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .category-layout,
  .recommend-stage,
  .feature-layout {
    grid-template-columns: 1fr;
  }

  .preference-spotlight,
  .preference-spotlight.empty,
  .insight-board {
    grid-template-columns: 1fr;
  }

  .preference-actions {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .insight-action-grid {
    grid-template-columns: 1fr;
  }

  .feature-grid,
  .curated-fallback-grid {
    grid-template-columns: 1fr;
  }

  .floating-badge {
    display: none;
  }
}

@media (max-width: 768px) {
  .hero-section {
    padding: 16px 16px 24px;
  }

  .category-section,
  .recommend-section,
  .feature-section {
    padding: 0 16px 28px;
  }

  .hero-shell {
    border-radius: 28px;
  }

  .hero-copy,
  .hero-waterfall {
    padding: 16px;
  }

  .hero-copy::after,
  .waterfall-surface {
    inset: 14px;
    border-radius: 24px;
  }

  .hero-title {
    font-size: 46px;
  }

  .hero-stats,
  .waterfall-grid,
  .recommend-grid,
  .recommend-loading,
  .category-grid {
    grid-template-columns: 1fr;
  }

  .hero-copy-topline,
  .hero-actions,
  .section-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-copy-main {
    top: 14px;
    left: 14px;
    width: calc(100% - 28px);
    padding: 14px;
  }

  .champion-panel-chip {
    top: 12px;
    left: 12px;
    padding: 8px 10px;
  }

  .hero-waterfall {
    min-height: auto;
  }

  .pause-hint {
    position: relative;
    top: auto;
    right: auto;
    margin-bottom: 16px;
  }

  .stage-header {
    position: relative;
    top: auto;
    left: auto;
    margin-bottom: 16px;
  }

  .waterfall-grid {
    padding-top: 0;
  }

  .stage-dots {
    position: static;
    margin-top: 16px;
  }

  .section-head,
  .preference-spotlight-shell,
  .insight-board-shell,
  .category-layout,
  .recommend-stage,
  .feature-layout,
  .category-grid,
  .recommend-grid,
  .recommend-loading,
  .curated-fallback-grid,
  .feature-grid {
    padding-left: 16px;
    padding-right: 16px;
  }

  .size-short .card-visual-shell {
    min-height: 100px;
  }

  .size-mini .card-visual-shell {
    min-height: 74px;
  }

  .preference-metrics {
    grid-template-columns: 1fr;
  }

  .insight-signal-grid {
    grid-template-columns: 1fr;
  }

  .section-surface {
    padding-top: 22px;
    border-radius: 28px;
  }

  .category-card,
  .feature-card,
  .curated-fallback-card,
  .insight-action-card,
  .category-aside-card,
  .recommend-aside-panel,
  .feature-lead-card {
    border-radius: 24px;
  }

  .preference-btn {
    width: 100%;
  }
}
</style>
