<template>
  <div class="quiz-page">
    <Header />

    <div class="main-content">
      <div v-if="!showResults" class="quiz-container">
        <div class="quiz-header">
          <div class="quiz-kicker">画像问答</div>
          <h1>把你的打法、身体条件和使用目标转成可计算的推荐画像</h1>
          <p>
            这组问题会覆盖运动水平、惯用打法、握拍与发力习惯、预算、特殊需求和训练目标，
            让系统给出更像专业器材顾问的装备建议。
          </p>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: `${((currentStep + 1) / questions.length) * 100}%` }" />
          </div>
          <span class="step-label">{{ currentStep + 1 }} / {{ questions.length }}</span>
        </div>

        <transition name="slide" mode="out-in">
          <section :key="currentQuestion.key" class="question-card">
            <div class="question-meta">第 {{ currentStep + 1 }} 步</div>
            <div class="question-title">{{ currentQuestion.title }}</div>
            <div v-if="currentQuestion.tip" class="question-tip">{{ currentQuestion.tip }}</div>

            <div v-if="currentQuestion.type === 'single'" class="options-grid">
              <button
                v-for="option in currentQuestion.options"
                :key="option.value"
                type="button"
                :class="['option-card', { selected: answers[currentQuestion.key] === option.value }]"
                @click="selectSingleAnswer(currentQuestion.key, option.value)"
              >
                <span class="option-label">{{ option.label }}</span>
                <span v-if="option.desc" class="option-desc">{{ option.desc }}</span>
              </button>
            </div>

            <div v-else class="multi-grid">
              <el-checkbox-group v-model="answers[currentQuestion.key]" class="checkbox-grid">
                <el-checkbox
                  v-for="option in currentQuestion.options"
                  :key="option.value"
                  :label="option.value"
                  class="checkbox-card"
                >
                  {{ option.label }}
                </el-checkbox>
              </el-checkbox-group>
              <div class="multi-tip">
                最多选择 {{ selectionLimits[currentQuestion.key] || 3 }} 项，越贴近你真实使用场景越有帮助。
              </div>
            </div>
          </section>
        </transition>

        <div class="quiz-nav">
          <el-button v-if="currentStep > 0" round @click="currentStep -= 1">上一题</el-button>
          <el-button
            v-if="currentStep < questions.length - 1"
            type="primary"
            round
            :disabled="!isCurrentStepComplete"
            @click="nextStep"
          >
            下一题
          </el-button>
          <el-button
            v-else
            type="success"
            round
            :loading="quizLoading"
            :disabled="!isCurrentStepComplete"
            @click="submitQuiz"
          >
            保存画像并生成推荐
          </el-button>
        </div>
      </div>

      <div v-else class="results-container">
        <div class="results-header">
          <div class="quiz-kicker">画像已更新</div>
          <h1>你的个性化推荐基础已经就绪</h1>
          <p>系统会把这次问答结果写入用户画像，并与后续浏览、收藏、评论和使用反馈一起驱动推荐模型迭代。</p>
          <div class="profile-tags">
            <el-tag effect="dark" round>{{ answerLabel('level') }}</el-tag>
            <el-tag type="success" effect="dark" round>{{ answerLabel('playStyle') }}</el-tag>
            <el-tag type="warning" effect="dark" round>{{ answerLabel('budgetKey') }}</el-tag>
            <el-tag type="info" effect="dark" round>{{ answerLabel('swingForceHabit') }}</el-tag>
          </div>
        </div>

        <div class="result-summary">
          <div class="summary-card">
            <span>身体特征</span>
            <strong>{{ answerLabel('heightRange') }}</strong>
          </div>
          <div class="summary-card">
            <span>握拍与惯用手</span>
            <strong>{{ answerLabel('gripStyle') }} / {{ answerLabel('dominantHand') }}</strong>
          </div>
          <div class="summary-card">
            <span>训练频率</span>
            <strong>{{ answerLabel('weeklyFrequency') }}</strong>
          </div>
          <div class="summary-card">
            <span>主要目标</span>
            <strong>{{ answerLabel('primaryGoal') }}</strong>
          </div>
          <div class="summary-card">
            <span>专项诉求</span>
            <strong>{{ answerLabel('coachNeed') }}</strong>
          </div>
          <div class="summary-card">
            <span>特殊需求</span>
            <strong>{{ answerLabel('specialNeeds') || '暂无' }}</strong>
          </div>
        </div>

        <div v-if="quizResults.length" class="results-grid">
          <EquipmentCard
            v-for="item in quizResults"
            :key="item.id"
            :equipment="item"
          />
        </div>

        <el-empty v-else description="暂时还没有匹配到推荐结果，请稍后再试。" />

        <div class="results-actions">
          <el-button type="primary" round @click="router.push('/persona-channel')">进入画像频道</el-button>
          <el-button round @click="router.push('/profile')">查看个人中心</el-button>
          <el-button round @click="resetQuiz">重新填写</el-button>
        </div>
      </div>
    </div>

    <Footer />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCategoryList } from '@/api/category'
import { getPersonalRecommend } from '@/api/recommend'
import { getUserInfo, getUserPreferences, updateUserPreferences } from '@/api/user'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/store/user'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import EquipmentCard from '@/components/EquipmentCard.vue'

const router = useRouter()
const userStore = useUserStore()
const currentStep = ref(0)
const showResults = ref(false)
const quizLoading = ref(false)
const quizResults = ref([])
const categoryOptions = ref([])

const selectionLimits = {
  specialNeeds: 3,
  preferredCategories: 4
}

const answers = reactive({
  level: '',
  playStyle: '',
  gripStyle: '',
  dominantHand: '',
  heightRange: '',
  swingForceHabit: '',
  budgetKey: '',
  weeklyFrequency: '',
  primaryGoal: '',
  priorityFocus: '',
  coachNeed: '',
  specialNeeds: [],
  preferredCategories: []
})

const singleQuestionConfig = computed(() => ([
  {
    key: 'level',
    title: '你目前的乒乓球水平更接近哪一类？',
    type: 'single',
    options: [
      { value: '初级', label: '初学者', desc: '刚入门或还在建立基本动作。' },
      { value: '中级', label: '进阶型', desc: '已经有固定训练，开始追求更稳定的质量。' },
      { value: '高级', label: '专业/高水平', desc: '训练强度高，更在意细节和比赛适配。' }
    ]
  },
  {
    key: 'playStyle',
    title: '你的惯用打法是什么？',
    type: 'single',
    options: [
      { value: '快攻', label: '进攻型', desc: '重视出球速度、前冲和主动上手。' },
      { value: '削球', label: '防守型', desc: '强调控制、削防和反击转换。' },
      { value: '全能', label: '全面型', desc: '攻守切换都要兼顾，追求均衡。' },
      { value: '弧圈', label: '弧圈型', desc: '重视摩擦、旋转和连续相持。' }
    ]
  },
  {
    key: 'gripStyle',
    title: '你常用的握拍方式是？',
    type: 'single',
    options: [
      { value: '横拍', label: '横拍', desc: '两面衔接更灵活，适配范围广。' },
      { value: '直拍', label: '直拍', desc: '更看重前三板、手感和控制。' },
      { value: '待定', label: '还在尝试', desc: '希望系统给出更宽泛的建议。' }
    ]
  },
  {
    key: 'dominantHand',
    title: '你的惯用手是？',
    type: 'single',
    options: [
      { value: '右手', label: '右手', desc: '按主流配置做推荐。' },
      { value: '左手', label: '左手', desc: '更关注落点、线路和适应性。' },
      { value: '双手切换', label: '双手/不固定', desc: '更强调容错和通用性。' }
    ]
  },
  {
    key: 'heightRange',
    title: '你的身高区间大致是？',
    type: 'single',
    options: [
      { value: '170cm以下', label: '170cm 以下', desc: '通常更偏灵活、小动作衔接。' },
      { value: '170-180cm', label: '170-180cm', desc: '大多数训练型用户所在区间。' },
      { value: '180-185cm', label: '180-185cm', desc: '可兼顾灵活性和中远台支撑。' },
      { value: '185cm以上', label: '185cm 以上', desc: '通常更关注力量传导和支撑感。' }
    ]
  },
  {
    key: 'swingForceHabit',
    title: '你的发力习惯更接近哪种？',
    type: 'single',
    options: [
      { value: '爆发型', label: '爆发型', desc: '主动发力多，追求瞬间速度和冲击。' },
      { value: '连贯借力', label: '连贯借力', desc: '更重视连续性和借力质量。' },
      { value: '中等均衡', label: '中等均衡', desc: '希望速度、稳定和容错更平衡。' }
    ]
  },
  {
    key: 'budgetKey',
    title: '你的预算范围大概是多少？',
    type: 'single',
    options: [
      { value: 'entry', label: '300 元以内', desc: '优先性价比和上手难度。' },
      { value: 'mid', label: '300 - 800 元', desc: '适合稳步进阶和日常训练。' },
      { value: 'advanced', label: '800 - 1500 元', desc: '开始追求材料和性能细节。' },
      { value: 'pro', label: '1500 元以上', desc: '可接受高端定制化配置。' }
    ]
  },
  {
    key: 'weeklyFrequency',
    title: '你每周使用装备的频率是？',
    type: 'single',
    options: [
      { value: '休闲', label: '1-2 次', desc: '以体验和轻训练为主。' },
      { value: '稳定训练', label: '3-4 次', desc: '持续训练，需要更稳的配置。' },
      { value: '高频', label: '5 次以上', desc: '更接近专项训练或比赛准备。' }
    ]
  },
  {
    key: 'primaryGoal',
    title: '你最希望装备帮你解决什么问题？',
    type: 'single',
    options: [
      { value: '提升控制', label: '提升控制', desc: '减少失误，出球更稳定。' },
      { value: '增强速度', label: '增强速度', desc: '更快的上手和回球质量。' },
      { value: '加强旋转', label: '加强旋转', desc: '提高摩擦感和弧线质量。' },
      { value: '比赛适配', label: '比赛适配', desc: '更贴近专项训练和比赛节奏。' }
    ]
  },
  {
    key: 'priorityFocus',
    title: '如果只能先满足一项，你最看重什么？',
    type: 'single',
    options: [
      { value: '控制', label: '控制', desc: '容错高、落点稳。' },
      { value: '速度', label: '速度', desc: '出球快、回合压迫感强。' },
      { value: '旋转', label: '旋转', desc: '更容易制造变化和质量。' },
      { value: '性价比', label: '性价比', desc: '预算内优先选最适合的。' }
    ]
  },
  {
    key: 'coachNeed',
    title: '你更需要哪类辅助决策服务？',
    type: 'single',
    options: [
      { value: '组合推荐', label: '装备组合推荐', desc: '希望直接得到底板+胶皮+配件方案。' },
      { value: '升级路径', label: '升级路径建议', desc: '希望知道下一阶段该怎么换。' },
      { value: '试打预约', label: '试打与体验预约', desc: '更在意线下体验和试用。' },
      { value: '对比分析', label: '对比分析工具', desc: '希望系统帮你缩小选择范围。' }
    ]
  }
]))

const questions = computed(() => ([
  ...singleQuestionConfig.value,
  {
    key: 'specialNeeds',
    title: '你有没有需要系统特别照顾的需求？',
    tip: '比如伤病、重量敏感、希望更易控等，都可以直接告诉系统。',
    type: 'multiple',
    options: [
      { value: '手腕受伤需轻量化', label: '手腕受伤，优先轻量化' },
      { value: '更关注容错和稳定', label: '更关注容错和稳定' },
      { value: '不想太硬太顶手', label: '不想太硬太顶手' },
      { value: '移动多，重视球鞋支撑', label: '移动多，重视球鞋支撑' },
      { value: '训练时间长，重视舒适度', label: '训练时间长，重视舒适度' }
    ]
  },
  {
    key: 'preferredCategories',
    title: '如果让系统优先记住你的关注方向，你会选哪些分类？',
    type: 'multiple',
    options: categoryOptions.value.map(item => ({
      value: String(item.id),
      label: item.name
    }))
  }
]))

const currentQuestion = computed(() => questions.value[currentStep.value] || questions.value[0])
const isCurrentStepComplete = computed(() => {
  const currentValue = answers[currentQuestion.value.key]
  if (Array.isArray(currentValue)) {
    return currentValue.length > 0
  }
  return Boolean(currentValue)
})

onMounted(async () => {
  await Promise.all([loadCategories(), loadSavedPreference()])
})

watch(
  () => answers.specialNeeds,
  (value) => enforceLimit('specialNeeds', value),
  { deep: true }
)

watch(
  () => answers.preferredCategories,
  (value) => enforceLimit('preferredCategories', value),
  { deep: true }
)

async function loadCategories() {
  try {
    const res = await getCategoryList()
    categoryOptions.value = res.data || []
  } catch (error) {
    categoryOptions.value = []
  }
}

async function loadSavedPreference() {
  if (!getToken()) {
    return
  }

  try {
    const res = await getUserPreferences()
    const profile = normalizePreference(res.data)
    if (!profile) {
      return
    }

    answers.level = profile.level || ''
    answers.playStyle = profile.playStyle || ''
    answers.gripStyle = profile.gripStyle || ''
    answers.weeklyFrequency = profile.weeklyFrequency || ''
    answers.primaryGoal = profile.primaryGoal || ''
    answers.priorityFocus = profile.priorityFocus || ''
    answers.preferredCategories = profile.preferredCategories || []
    answers.budgetKey = profile.answerSnapshot?.budgetKey || restoreBudgetKey(profile)
    answers.dominantHand = profile.answerSnapshot?.dominantHand || ''
    answers.heightRange = profile.answerSnapshot?.heightRange || ''
    answers.swingForceHabit = profile.answerSnapshot?.swingForceHabit || ''
    answers.specialNeeds = profile.answerSnapshot?.specialNeeds || []
    answers.coachNeed = profile.answerSnapshot?.coachNeed || ''
  } catch (error) {
    // ignore
  }
}

function enforceLimit(key, value) {
  const limit = selectionLimits[key]
  if (!limit || !Array.isArray(value) || value.length <= limit) {
    return
  }
  answers[key] = value.slice(0, limit)
  ElMessage.warning(`最多选择 ${limit} 项`)
}

function selectSingleAnswer(key, value) {
  answers[key] = value
}

function nextStep() {
  if (isCurrentStepComplete.value && currentStep.value < questions.value.length - 1) {
    currentStep.value += 1
  }
}

async function submitQuiz() {
  if (!getToken()) {
    ElMessage.warning('请先登录后再保存问答结果')
    router.push({ name: 'Login', query: { redirect: '/quiz' } })
    return
  }

  quizLoading.value = true
  try {
    await updateUserPreferences(buildPayload())
    const userInfo = await getUserInfo()
    userStore.setUserInfo(userInfo.data)

    const recommendRes = await getPersonalRecommend({ count: 8 })
    quizResults.value = recommendRes.data || []
    showResults.value = true
    ElMessage.success('画像已更新，推荐结果已刷新')
  } catch (error) {
    ElMessage.error(error?.message || '问答保存失败')
  } finally {
    quizLoading.value = false
  }
}

function buildPayload() {
  const budgetMap = {
    entry: { min: 0, max: 300 },
    mid: { min: 300, max: 800 },
    advanced: { min: 800, max: 1500 },
    pro: { min: 1500, max: 5000 }
  }
  const budget = budgetMap[answers.budgetKey] || { min: 0, max: 5000 }

  return {
    level: answers.level,
    playStyle: answers.playStyle,
    gripStyle: answers.gripStyle,
    budgetMin: budget.min,
    budgetMax: budget.max,
    weeklyFrequency: answers.weeklyFrequency,
    primaryGoal: answers.primaryGoal,
    priorityFocus: answers.priorityFocus,
    preferredCategories: answers.preferredCategories.slice(0, selectionLimits.preferredCategories),
    preferredBrands: [],
    preferenceTags: [
      answers.level,
      answers.playStyle,
      answers.gripStyle,
      answers.dominantHand,
      answers.heightRange,
      answers.swingForceHabit,
      answers.weeklyFrequency,
      answers.primaryGoal,
      answers.priorityFocus,
      answers.coachNeed,
      ...answers.specialNeeds
    ].filter(Boolean),
    answerSnapshot: {
      ...answers
    },
    updatedSource: 'quiz'
  }
}

function answerLabel(key) {
  const question = questions.value.find(item => item.key === key)
  if (!question) {
    const raw = answers[key]
    return Array.isArray(raw) ? raw.join('、') : raw
  }

  const value = answers[key]
  if (Array.isArray(value)) {
    return question.options
      .filter(item => value.includes(item.value))
      .map(item => item.label)
      .join('、')
  }
  return question.options.find(item => item.value === value)?.label || value
}

function normalizePreference(profile) {
  if (!profile) {
    return null
  }

  return {
    ...profile,
    preferredCategories: safeJsonParse(profile.preferredCategories, []),
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

function restoreBudgetKey(profile) {
  const min = Number(profile.budgetMin || 0)
  const max = Number(profile.budgetMax || 0)
  if (max <= 300) {
    return 'entry'
  }
  if (min >= 300 && max <= 800) {
    return 'mid'
  }
  if (min >= 800 && max <= 1500) {
    return 'advanced'
  }
  return 'pro'
}

function resetQuiz() {
  currentStep.value = 0
  showResults.value = false
  quizResults.value = []
}
</script>

<style scoped lang="scss">
.quiz-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(240, 138, 36, 0.14), transparent 26%),
    radial-gradient(circle at bottom right, rgba(29, 88, 70, 0.14), transparent 30%),
    linear-gradient(180deg, #f5efe5 0%, #edf7f0 100%);
}

.main-content {
  max-width: 1120px;
  margin: 0 auto;
  padding: 32px 20px 64px;
}

.quiz-container,
.results-container {
  border-radius: 32px;
  padding: 42px 36px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 28px 60px rgba(23, 53, 46, 0.12);
}

.quiz-kicker {
  color: #d96b1f;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.quiz-header,
.results-header {
  text-align: center;
  margin-bottom: 34px;
}

.quiz-header h1,
.results-header h1 {
  margin: 10px 0 10px;
  font-size: 34px;
  color: #17352e;
}

.quiz-header p,
.results-header p {
  margin: 0 auto;
  max-width: 760px;
  color: #66766d;
  line-height: 1.8;
}

.progress-bar {
  height: 8px;
  margin: 24px auto 10px;
  border-radius: 999px;
  background: #e9ece8;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #1d5846, #f08a24);
  transition: width 0.35s ease;
}

.step-label {
  color: #8b98a7;
  font-size: 13px;
}

.question-card {
  min-height: 430px;
}

.question-meta {
  margin-bottom: 10px;
  color: #d96b1f;
  text-align: center;
  font-size: 13px;
  font-weight: 700;
}

.question-title {
  margin-bottom: 12px;
  font-size: 24px;
  font-weight: 700;
  color: #17352e;
  text-align: center;
}

.question-tip {
  margin-bottom: 28px;
  color: #66766d;
  text-align: center;
}

.options-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.option-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 22px 18px;
  border: 2px solid transparent;
  border-radius: 22px;
  background: linear-gradient(180deg, #fffaf3 0%, #f5fbf7 100%);
  cursor: pointer;
  text-align: left;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.option-card:hover,
.option-card.selected {
  transform: translateY(-2px);
  border-color: rgba(29, 88, 70, 0.45);
  box-shadow: 0 16px 28px rgba(23, 53, 46, 0.09);
}

.option-label {
  font-size: 17px;
  font-weight: 700;
  color: #17352e;
}

.option-desc {
  color: #66766d;
  line-height: 1.7;
}

.multi-grid {
  padding: 0 20px;
}

.checkbox-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.checkbox-card {
  display: flex;
  align-items: center;
  min-height: 54px;
  margin-right: 0;
  padding: 0 16px;
  border-radius: 18px;
  background: linear-gradient(180deg, #fffaf3 0%, #f5fbf7 100%);
  box-shadow: inset 0 0 0 1px rgba(23, 53, 46, 0.08);
}

.multi-tip {
  margin-top: 14px;
  color: #66766d;
  text-align: center;
}

.quiz-nav {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 28px;
}

.profile-tags {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.result-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 24px;
}

.summary-card {
  padding: 18px;
  border-radius: 22px;
  background: linear-gradient(180deg, #fffaf3 0%, #f5fbf7 100%);
  border: 1px solid rgba(29, 88, 70, 0.08);
}

.summary-card span {
  display: block;
  color: #66766d;
  font-size: 13px;
}

.summary-card strong {
  display: block;
  margin-top: 8px;
  color: #17352e;
  font-size: 20px;
  line-height: 1.5;
}

.results-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.results-actions {
  display: flex;
  justify-content: center;
  gap: 14px;
  margin-top: 28px;
  flex-wrap: wrap;
}

.slide-enter-active,
.slide-leave-active {
  transition: all 0.28s ease;
}

.slide-enter-from {
  opacity: 0;
  transform: translateX(24px);
}

.slide-leave-to {
  opacity: 0;
  transform: translateX(-24px);
}

@media (max-width: 1024px) {
  .results-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .main-content {
    padding: 20px 14px 40px;
  }

  .quiz-container,
  .results-container {
    padding: 28px 18px;
    border-radius: 24px;
  }

  .quiz-header h1,
  .results-header h1 {
    font-size: 28px;
  }

  .options-grid,
  .checkbox-grid,
  .result-summary,
  .results-grid {
    grid-template-columns: 1fr;
  }

  .question-card {
    min-height: auto;
  }
}
</style>
