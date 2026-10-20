<template>
  <div class="profile-page">
    <Header />

    <main class="main-content">
      <el-card class="profile-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div>
              <div class="card-kicker">个人中心</div>
              <h1>管理你的资料、偏好和浏览线索</h1>
              <p>头像、基础信息、浏览行为和问答偏好都会影响推荐结果，修改后会立即同步到当前账号。</p>
            </div>
          </div>
        </template>

        <el-tabs v-model="activeTab" class="profile-tabs">
          <el-tab-pane label="基础资料" name="basic">
            <section class="avatar-panel">
              <div class="avatar-meta">
                <el-avatar :size="96" :src="userForm.avatar || ''" class="profile-avatar">
                  {{ (userForm.nickname || userForm.username || 'U').charAt(0) }}
                </el-avatar>
                <div>
                  <h3>{{ userForm.nickname || userForm.username || '未命名用户' }}</h3>
                  <p>支持 JPG、PNG、GIF、WEBP。上传后可预览、裁剪并保存为头像。</p>
                </div>
              </div>
              <div class="avatar-actions">
                <input
                  ref="avatarInputRef"
                  type="file"
                  accept="image/png,image/jpeg,image/jpg,image/gif,image/webp"
                  class="hidden-input"
                  @change="handleAvatarChange"
                >
                <el-button type="primary" @click="avatarInputRef?.click()">上传头像</el-button>
              </div>
            </section>

            <el-form
              ref="formRef"
              :model="userForm"
              label-width="100px"
              class="profile-form"
            >
              <el-form-item label="用户名">
                <el-input v-model="userForm.username" disabled />
              </el-form-item>

              <el-form-item label="昵称">
                <el-input v-model="userForm.nickname" maxlength="20" />
              </el-form-item>

              <el-form-item label="邮箱">
                <el-input v-model="userForm.email" />
              </el-form-item>

              <el-form-item label="手机号">
                <el-input v-model="userForm.phone" />
              </el-form-item>

              <el-form-item label="性别">
                <el-radio-group v-model="userForm.gender">
                  <el-radio :label="0">未知</el-radio>
                  <el-radio :label="1">男</el-radio>
                  <el-radio :label="2">女</el-radio>
                </el-radio-group>
              </el-form-item>

              <el-form-item label="当前水平">
                <el-select v-model="userForm.level" placeholder="请选择当前水平">
                  <el-option label="初级" value="初级" />
                  <el-option label="中级" value="中级" />
                  <el-option label="高级" value="高级" />
                </el-select>
              </el-form-item>

              <el-form-item label="打法风格">
                <el-select v-model="userForm.playStyle" placeholder="请选择打法风格">
                  <el-option label="快攻" value="快攻" />
                  <el-option label="弧圈" value="弧圈" />
                  <el-option label="削球" value="削球" />
                  <el-option label="全能" value="全能" />
                </el-select>
              </el-form-item>

              <el-form-item>
                <el-button type="primary" @click="handleUpdate">保存资料</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="修改密码" name="password">
            <el-alert
              title="密码修改成功后，当前登录会话会失效，需要使用新密码重新登录。"
              type="warning"
              show-icon
              :closable="false"
              class="password-alert"
            />

            <el-form
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-width="110px"
              class="profile-form"
            >
              <el-form-item label="原密码" prop="oldPassword">
                <el-input
                  v-model="passwordForm.oldPassword"
                  type="password"
                  show-password
                  autocomplete="current-password"
                />
              </el-form-item>

              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  show-password
                  autocomplete="new-password"
                />
                <div class="field-tip">密码长度 10-32 位，需同时包含大小写字母、数字和特殊字符。</div>
              </el-form-item>

              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  show-password
                  autocomplete="new-password"
                />
              </el-form-item>

              <el-form-item>
                <el-button type="primary" @click="handleChangePassword">更新密码</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="浏览历史" name="history">
            <div v-if="historyList.length" class="history-toolbar">
              <el-button @click="router.push('/history')">查看完整历史</el-button>
              <el-button type="danger" plain @click="handleClearHistory">清空历史</el-button>
            </div>

            <div v-if="historyList.length" class="history-list">
              <article v-for="item in historyList" :key="item.id" class="history-item">
                <el-image
                  :src="getEquipmentImageUrl(item.equipment)"
                  fit="cover"
                  class="history-cover"
                  @click="goToEquipmentDetail(item.equipmentId)"
                />
                <div class="history-main">
                  <strong class="history-name" @click="goToEquipmentDetail(item.equipmentId)">
                    {{ item.equipmentName || item.equipment?.name || `器材 #${item.equipmentId}` }}
                  </strong>
                  <div class="history-meta">
                    <span>{{ formatHistoryType(item.contentType) }}</span>
                    <span>{{ formatDuration(item.durationSeconds) }}</span>
                    <span>最近访问：{{ formatTime(item.leaveTime || item.updateTime || item.createTime) }}</span>
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
                  <el-button size="small" @click="goToEquipmentDetail(item.equipmentId)">查看详情</el-button>
                  <el-button size="small" text type="danger" @click="handleDeleteHistory(item.id)">删除记录</el-button>
                </div>
              </article>
            </div>

            <el-empty v-else description="暂时还没有浏览历史">
              <el-button type="primary" @click="router.push('/equipment-list')">去逛器材库</el-button>
            </el-empty>
          </el-tab-pane>

          <el-tab-pane label="偏好问答" name="preferences">
            <section class="preference-hero">
              <div>
                <h3>问答偏好画像</h3>
                <p>系统会综合你的浏览时长、交互动作、技术水平、预算和目标，生成更贴近你的推荐结果。</p>
              </div>
              <div class="preference-actions">
                <el-button @click="router.push('/quiz')">打开问答向导</el-button>
                <el-button type="primary" @click="router.push('/quiz')">重新填写</el-button>
              </div>
            </section>

            <div class="analysis-grid">
              <div class="analysis-card">
                <span>浏览记录</span>
                <strong>{{ behaviorAnalysis.browseCount || 0 }}</strong>
              </div>
              <div class="analysis-card">
                <span>收藏记录</span>
                <strong>{{ behaviorAnalysis.favoriteCount || 0 }}</strong>
              </div>
              <div class="analysis-card">
                <span>评论记录</span>
                <strong>{{ behaviorAnalysis.commentCount || 0 }}</strong>
              </div>
            </div>

            <section v-if="profileInsight" class="insight-section">
              <div class="insight-summary-card">
                <div class="insight-summary-copy">
                  <div class="insight-kicker">画像洞察</div>
                  <h3>{{ profileInsight.personaTitle }}</h3>
                  <p>{{ profileInsight.personaDescription }}</p>
                  <div v-if="profileInsight.coreTags?.length" class="tag-list insight-tag-list">
                    <el-tag
                      v-for="tag in profileInsight.coreTags"
                      :key="tag"
                      type="success"
                      effect="plain"
                      round
                    >
                      {{ tag }}
                    </el-tag>
                  </div>
                </div>

                <div class="insight-progress-card">
                  <span>画像完成度</span>
                  <strong>{{ profileInsight.completionScore || 0 }}%</strong>
                  <el-progress
                    :percentage="profileInsight.completionScore || 0"
                    :stroke-width="12"
                    :show-text="false"
                    color="#1d5846"
                  />
                  <em>{{ profileInsight.recommendationMode }}</em>
                </div>
              </div>

              <div v-if="profileInsight.signals?.length" class="insight-signal-grid">
                <article v-for="signal in profileInsight.signals" :key="signal.label" class="insight-signal-card">
                  <span>{{ signal.label }}</span>
                  <strong>{{ signal.value }}</strong>
                  <p>{{ signal.detail }}</p>
                </article>
              </div>

              <div v-if="profileInsight.improvementActions?.length" class="insight-advice-card">
                <div class="tag-title">下一步建议</div>
                <div class="insight-advice-list">
                  <div v-for="item in profileInsight.improvementActions" :key="item" class="insight-advice-item">
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
                  <span>{{ action.title }}</span>
                  <h4>{{ action.buttonText }}</h4>
                  <p>{{ action.description }}</p>
                  <div v-if="action.tags?.length" class="insight-action-tags">
                    <el-tag v-for="tag in action.tags" :key="tag" round effect="dark">{{ tag }}</el-tag>
                  </div>
                </button>
              </div>
            </section>

            <div v-if="preferenceProfile" class="preference-content">
              <div class="preference-cards">
                <div class="preference-card">
                  <span>技术水平</span>
                  <strong>{{ preferenceProfile.level || '未设置' }}</strong>
                </div>
                <div class="preference-card">
                  <span>打法风格</span>
                  <strong>{{ preferenceProfile.playStyle || '未设置' }}</strong>
                </div>
                <div class="preference-card">
                  <span>握拍方式</span>
                  <strong>{{ preferenceProfile.gripStyle || '未设置' }}</strong>
                </div>
                <div class="preference-card">
                  <span>预算范围</span>
                  <strong>{{ formatBudget(preferenceProfile) }}</strong>
                </div>
                <div class="preference-card">
                  <span>使用频率</span>
                  <strong>{{ preferenceProfile.weeklyFrequency || '未设置' }}</strong>
                </div>
                <div class="preference-card">
                  <span>使用目标</span>
                  <strong>{{ preferenceProfile.primaryGoal || '未设置' }}</strong>
                </div>
                <div class="preference-card">
                  <span>优先考虑</span>
                  <strong>{{ preferenceProfile.priorityFocus || '未设置' }}</strong>
                </div>
              </div>

              <div v-if="preferredCategoryNames.length" class="tag-section">
                <div class="tag-title">偏好分类</div>
                <div class="tag-list">
                  <el-tag v-for="name in preferredCategoryNames" :key="name" round>{{ name }}</el-tag>
                </div>
              </div>

              <div v-if="preferenceProfile.preferenceTags?.length" class="tag-section">
                <div class="tag-title">偏好标签</div>
                <div class="tag-list">
                  <el-tag
                    v-for="tag in preferenceProfile.preferenceTags"
                    :key="tag"
                    type="success"
                    effect="plain"
                    round
                  >
                    {{ tag }}
                  </el-tag>
                </div>
              </div>
            </div>

            <el-empty v-else description="还没有保存偏好问答结果">
              <el-button type="primary" @click="router.push('/quiz')">现在开始问答</el-button>
            </el-empty>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </main>

    <AvatarCropDialog
      v-model="avatarDialogVisible"
      :file="selectedAvatarFile"
      @confirm="handleAvatarConfirm"
    />
    <Footer />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategoryList } from '@/api/category'
import { analyzeUserBehavior, getProfileInsight } from '@/api/recommend'
import { clearUserHistory, deleteHistoryItem, getUserHistory } from '@/api/history'
import {
  changePassword,
  getUserInfo,
  getUserPreferences,
  updateUserInfo,
  uploadAvatar
} from '@/api/user'
import { clearAuthState } from '@/utils/auth'
import { useUserStore } from '@/store/user'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import AvatarCropDialog from '@/components/profile/AvatarCropDialog.vue'

const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{10,32}$/
const SUPPORTED_IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/jpg', 'image/gif', 'image/webp']

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

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('basic')
const formRef = ref(null)
const passwordFormRef = ref(null)
const avatarInputRef = ref(null)
const avatarDialogVisible = ref(false)
const selectedAvatarFile = ref(null)
const userForm = ref({})
const historyList = ref([])
const preferenceProfile = ref(null)
const behaviorAnalysis = ref({})
const profileInsight = ref(null)
const categoryOptions = ref([])

function getEquipmentImageUrl(equipment) {
  // 优先使用 mainImage，其次使用 imageUrl，最后使用占位图
  return equipment?.mainImage || equipment?.imageUrl || 'data:image/svg+xml;charset=UTF-8,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 300 220"%3E%3Crect width="300" height="220" fill="%23eef2f7"/%3E%3Ccircle cx="150" cy="86" r="34" fill="%23cbd5e1"/%3E%3Cpath d="M84 178c18-28 42-42 66-42s48 14 66 42" fill="%23dbe3ec"/%3E%3C/svg%3E'
}

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const preferredCategoryNames = computed(() => {
  const ids = preferenceProfile.value?.preferredCategories || []
  if (!ids.length) {
    return []
  }
  const categoryMap = new Map(categoryOptions.value.map(item => [String(item.id), item.name]))
  return ids.map(id => categoryMap.get(String(id)) || `分类 ${id}`)
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const validateStrongPassword = (rule, value, callback) => {
  if (!PASSWORD_PATTERN.test(value || '')) {
    callback(new Error('密码长度 10-32 位，且需同时包含大小写字母、数字和特殊字符'))
    return
  }
  callback()
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { validator: validateStrongPassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

onMounted(async () => {
  await Promise.all([
    loadUserInfo(),
    loadHistory(),
    loadPreference(),
    loadBehaviorAnalysis(),
    loadProfileInsight(),
    loadCategories()
  ])
})

watch(avatarDialogVisible, (visible) => {
  if (!visible) {
    selectedAvatarFile.value = null
  }
})

async function loadUserInfo() {
  try {
    const res = await getUserInfo()
    userForm.value = res.data || {}
    userStore.setUserInfo(userForm.value)
  } catch (error) {
    ElMessage.error(error?.message || '加载用户信息失败')
  }
}

async function loadHistory() {
  try {
    const res = await getUserHistory({ limit: 12, deduplicate: false })
    historyList.value = res.data || []
  } catch (error) {
    ElMessage.error(error?.message || '加载浏览历史失败')
  }
}

async function loadPreference() {
  try {
    const res = await getUserPreferences()
    preferenceProfile.value = normalizePreference(res.data)
  } catch (error) {
    preferenceProfile.value = null
  }
}

async function loadBehaviorAnalysis() {
  try {
    const res = await analyzeUserBehavior()
    behaviorAnalysis.value = res.data || {}
  } catch (error) {
    behaviorAnalysis.value = {}
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

async function loadCategories() {
  try {
    const res = await getCategoryList()
    categoryOptions.value = res.data || []
  } catch (error) {
    categoryOptions.value = []
  }
}

async function handleUpdate() {
  try {
    await updateUserInfo(userForm.value)
    userStore.setUserInfo(userForm.value)
    await loadProfileInsight()
    ElMessage.success('资料已保存')
  } catch (error) {
    ElMessage.error(error?.message || '保存资料失败')
  }
}

async function handleChangePassword() {
  if (!passwordFormRef.value) {
    return
  }

  try {
    await passwordFormRef.value.validate()
  } catch (error) {
    return
  }

  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })

    ElMessage.success('密码已更新，请使用新密码重新登录')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''

    setTimeout(() => {
      clearAuthState()
      userStore.logout()
      router.push('/login')
    }, 1200)
  } catch (error) {
    ElMessage.error(error?.message || '修改密码失败')
  }
}

async function handleDeleteHistory(historyId) {
  try {
    await deleteHistoryItem(historyId)
    historyList.value = historyList.value.filter(item => item.id !== historyId)
    await Promise.all([loadBehaviorAnalysis(), loadProfileInsight()])
    ElMessage.success('浏览记录已删除')
  } catch (error) {
    ElMessage.error(error?.message || '删除浏览记录失败')
  }
}

async function handleClearHistory() {
  try {
    await ElMessageBox.confirm('清空后无法恢复，确定继续吗？', '清空确认', {
      type: 'warning'
    })
    await clearUserHistory()
    historyList.value = []
    await Promise.all([loadBehaviorAnalysis(), loadProfileInsight()])
    ElMessage.success('浏览历史已清空')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '清空浏览历史失败')
    }
  }
}

function handleAvatarChange(event) {
  const file = event.target.files?.[0]
  if (!file) {
    return
  }

  if (!SUPPORTED_IMAGE_TYPES.includes(file.type)) {
    ElMessage.warning('请选择 JPG、PNG、GIF 或 WEBP 图片')
    event.target.value = ''
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像图片不能超过 5MB')
    event.target.value = ''
    return
  }

  selectedAvatarFile.value = file
  avatarDialogVisible.value = true
  event.target.value = ''
}

async function handleAvatarConfirm(blob) {
  const file = new File([blob], `avatar-${Date.now()}.png`, { type: 'image/png' })
  try {
    const res = await uploadAvatar(file)
    userForm.value = {
      ...userForm.value,
      ...(res.data || {})
    }
    userStore.setUserInfo(userForm.value)
    avatarDialogVisible.value = false
    selectedAvatarFile.value = null
    ElMessage.success('头像已更新')
  } catch (error) {
    ElMessage.error(error?.message || '头像上传失败')
  }
}

function goToEquipmentDetail(equipmentId) {
  if (!equipmentId) {
    return
  }
  router.push(`/equipment/${equipmentId}`)
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

function normalizePreference(profile) {
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

function formatBudget(profile) {
  if (!profile) {
    return '未设置'
  }
  const min = profile.budgetMin
  const max = profile.budgetMax
  if (min && max) {
    return `¥${Number(min).toFixed(0)} - ¥${Number(max).toFixed(0)}`
  }
  if (max) {
    return `¥${Number(max).toFixed(0)} 以下`
  }
  if (min) {
    return `¥${Number(min).toFixed(0)} 以上`
  }
  return '未设置'
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
</script>

<style scoped lang="scss">
.profile-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(240, 138, 36, 0.1), transparent 24%),
    radial-gradient(circle at bottom right, rgba(29, 88, 70, 0.1), transparent 28%),
    linear-gradient(180deg, #f4efe6 0%, #f4f8f1 100%);
}

.main-content {
  max-width: 1120px;
  margin: 0 auto;
  padding: 28px 20px 36px;
}

.profile-card {
  border-radius: 28px;
  border: 1px solid rgba(29, 88, 70, 0.08);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 22px 48px rgba(23, 53, 46, 0.08);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.card-header h1 {
  margin: 8px 0 6px;
  font-size: 30px;
  color: #17352e;
}

.card-header p {
  margin: 0;
  color: #66766d;
  line-height: 1.7;
}

.card-kicker {
  color: #d96b1f;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.profile-tabs :deep(.el-tabs__item.is-active) {
  color: #1d5846;
}

.profile-tabs :deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #1d5846, #f08a24);
}

.avatar-panel,
.preference-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 20px 22px;
  border-radius: 24px;
  background: linear-gradient(135deg, #fff8ef 0%, #f1f8f3 100%);
  border: 1px solid rgba(29, 88, 70, 0.08);
  margin-bottom: 22px;
}

.avatar-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-meta h3,
.preference-hero h3 {
  margin: 0 0 8px;
  color: #17352e;
}

.avatar-meta p,
.preference-hero p {
  margin: 0;
  color: #66766d;
  line-height: 1.7;
}

.profile-avatar {
  box-shadow: 0 18px 36px rgba(23, 53, 46, 0.12);
}

.avatar-actions,
.preference-actions {
  display: flex;
  gap: 12px;
}

.hidden-input {
  display: none;
}

.profile-form {
  max-width: 640px;
}

.profile-form :deep(.el-input__wrapper),
.profile-form :deep(.el-select__wrapper) {
  min-height: 46px;
  border-radius: 14px;
  box-shadow: inset 0 0 0 1px rgba(23, 53, 46, 0.08);
  background: #fffefb;
}

.password-alert {
  margin-bottom: 18px;
}

.field-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #66766d;
}

.history-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-bottom: 14px;
}

.history-list {
  display: grid;
  gap: 12px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, #fff8ef 0%, #f1f8f3 100%);
  border: 1px solid rgba(29, 88, 70, 0.08);
}

.history-cover {
  width: 88px;
  height: 88px;
  border-radius: 18px;
  cursor: pointer;
  background: #eef3ee;
}

.history-main {
  flex: 1;
  min-width: 0;
}

.history-name {
  display: inline-block;
  color: #17352e;
  font-size: 15px;
  cursor: pointer;
}

.history-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
  color: #66766d;
  font-size: 13px;
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.history-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.analysis-grid,
.preference-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.preference-cards {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.analysis-card,
.preference-card {
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(29, 88, 70, 0.08);
  box-shadow: 0 14px 28px rgba(23, 53, 46, 0.06);
}

.analysis-card span,
.preference-card span {
  display: block;
  color: #66766d;
  font-size: 13px;
}

.analysis-card strong,
.preference-card strong {
  display: block;
  margin-top: 8px;
  color: #17352e;
  font-size: 22px;
}

.insight-section {
  margin-top: 18px;
}

.insight-summary-card {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(240px, 0.8fr);
  gap: 16px;
  padding: 22px;
  border-radius: 24px;
  background: linear-gradient(135deg, #fff8ef 0%, #f1f8f3 100%);
  border: 1px solid rgba(29, 88, 70, 0.08);
}

.insight-kicker {
  color: #1d5846;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.insight-summary-copy h3 {
  margin: 8px 0 10px;
  color: #17352e;
  font-size: 28px;
}

.insight-summary-copy p {
  margin: 0;
  color: #66766d;
  line-height: 1.8;
}

.insight-tag-list {
  margin-top: 14px;
}

.insight-progress-card {
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(29, 88, 70, 0.08);
  box-shadow: 0 14px 28px rgba(23, 53, 46, 0.06);
}

.insight-progress-card span {
  display: block;
  color: #66766d;
  font-size: 13px;
}

.insight-progress-card strong {
  display: block;
  margin: 10px 0 12px;
  color: #17352e;
  font-size: 30px;
}

.insight-progress-card em {
  display: block;
  margin-top: 12px;
  color: #1d5846;
  font-style: normal;
  font-size: 13px;
  font-weight: 700;
}

.insight-signal-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}

.insight-signal-card {
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(29, 88, 70, 0.08);
  box-shadow: 0 14px 28px rgba(23, 53, 46, 0.06);
}

.insight-signal-card span {
  display: block;
  color: #66766d;
  font-size: 13px;
}

.insight-signal-card strong {
  display: block;
  margin-top: 8px;
  color: #17352e;
  font-size: 24px;
}

.insight-signal-card p {
  margin: 10px 0 0;
  color: #66766d;
  line-height: 1.7;
  font-size: 13px;
}

.insight-advice-card {
  margin-top: 16px;
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(29, 88, 70, 0.08);
}

.insight-advice-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.insight-advice-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, #fff8ef 0%, #f6fbf7 100%);
  color: #4f6059;
  line-height: 1.75;
}

.insight-action-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}

.insight-action-card {
  padding: 20px;
  border-radius: 20px;
  border: 1px solid rgba(29, 88, 70, 0.08);
  background: linear-gradient(180deg, #ffffff 0%, #f7fbf8 100%);
  text-align: left;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.insight-action-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 30px rgba(23, 53, 46, 0.08);
}

.insight-action-card span {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(240, 138, 36, 0.12);
  color: #d96b1f;
  font-size: 12px;
  font-weight: 700;
}

.insight-action-card h4 {
  margin: 14px 0 10px;
  color: #17352e;
  font-size: 22px;
}

.insight-action-card p {
  margin: 0;
  color: #66766d;
  line-height: 1.8;
}

.insight-action-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.preference-content {
  margin-top: 20px;
}

.tag-section {
  margin-top: 18px;
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(29, 88, 70, 0.08);
}

.tag-title {
  font-size: 14px;
  font-weight: 700;
  color: #17352e;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

@media (max-width: 960px) {
  .analysis-grid,
  .preference-cards,
  .insight-signal-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .insight-summary-card,
  .insight-action-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .main-content {
    padding: 18px 14px 28px;
  }

  .profile-card {
    border-radius: 22px;
  }

  .card-header,
  .avatar-panel,
  .preference-hero,
  .history-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .avatar-actions,
  .preference-actions,
  .history-toolbar,
  .history-actions {
    width: 100%;
    align-items: stretch;
  }

  .history-cover {
    width: 100%;
    height: 180px;
  }

  .analysis-grid,
  .preference-cards,
  .insight-signal-grid,
  .insight-action-grid {
    grid-template-columns: 1fr;
  }
}
</style>
