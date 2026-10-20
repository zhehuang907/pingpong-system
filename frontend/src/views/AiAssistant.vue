<template>
  <div class="assistant-page">
    <Header />

    <main class="assistant-main">
      <section class="assistant-shell">
        <aside class="session-sidebar">
          <div class="sidebar-top">
            <div>
              <div class="sidebar-kicker">DeepSeek x 装备推荐</div>
              <h2>智能装备顾问</h2>
            </div>
            <el-button type="primary" round @click="handleCreateSession">
              <el-icon><Plus /></el-icon>
              新建会话
            </el-button>
          </div>

          <div class="sidebar-hero">
            <div class="hero-badge">
              <el-icon><MagicStick /></el-icon>
              画像增强问答
            </div>
            <p>{{ heroDescription }}</p>
            <div class="hero-metrics">
              <div>
                <strong>{{ sessions.length }}</strong>
                <span>会话</span>
              </div>
              <div>
                <strong>{{ keywordTags.length || 0 }}</strong>
                <span>画像标签</span>
              </div>
              <div>
                <strong>{{ focusEquipment ? 1 : 0 }}</strong>
                <span>聚焦器材</span>
              </div>
            </div>
            <div v-if="keywordTags.length" class="keyword-tags">
              <span v-for="tag in keywordTags" :key="tag">{{ tag }}</span>
            </div>
          </div>

          <el-scrollbar class="session-scroll">
            <button
              v-for="session in sessions"
              :key="session.id"
              type="button"
              :class="['session-item', { active: session.id === activeSessionId }]"
              @click="openSession(session.id)"
            >
              <div class="session-item-head">
                <strong>{{ session.title || '新会话' }}</strong>
                <span>{{ formatTime(session.lastMessageAt || session.updateTime) }}</span>
              </div>
              <p>{{ getSessionPreview(session) }}</p>
              <div class="session-item-foot">
                <span>{{ session.messageCount || 0 }} 条消息</span>
                <span>{{ session.scene === 'EQUIPMENT_ADVISOR' ? '器材顾问' : 'AI 会话' }}</span>
              </div>
            </button>

            <div v-if="!loadingSessions && !sessions.length" class="session-empty">
              <el-empty description="还没有聊天会话" :image-size="110">
                <el-button type="primary" round @click="handleCreateSession">开始第一轮咨询</el-button>
              </el-empty>
            </div>
          </el-scrollbar>
        </aside>

        <section class="chat-panel">
          <div class="chat-topbar">
            <div class="topbar-copy">
              <div class="topbar-kicker">智能装备顾问</div>
              <h1>{{ currentSession?.title || '新会话' }}</h1>
              <p>
                {{
                  focusEquipment
                    ? `当前会围绕 ${focusEquipment.name} 给出选购、打法匹配和升级建议。`
                    : '把你的预算、打法、训练目标和纠结点告诉顾问，它会结合画像和推荐结果来回答。'
                }}
              </p>
              <div class="topbar-highlights">
                <span>预算搭配</span>
                <span>打法适配</span>
                <span>升级顺序</span>
                <span v-if="focusEquipment">围绕单品深聊</span>
              </div>
            </div>

            <div class="topbar-actions">
              <el-tag v-if="focusEquipment" class="focus-tag" effect="dark" round>
                聚焦器材: {{ focusEquipment.name }}
              </el-tag>
              <el-button circle @click="reloadActiveSession" :disabled="!activeSessionId || loadingMessages">
                <el-icon><RefreshRight /></el-icon>
              </el-button>
              <el-dropdown v-if="currentSession" trigger="click">
                <el-button circle>
                  <el-icon><Setting /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="handleRenameSession(currentSession)">
                      <el-icon><EditPen /></el-icon>
                      重命名
                    </el-dropdown-item>
                    <el-dropdown-item divided @click="handleDeleteSession(currentSession)">
                      <el-icon><Delete /></el-icon>
                      删除会话
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>

          <div v-if="focusEquipment" class="focus-equipment-card">
            <img :src="getEquipmentImageUrl(focusEquipment)" :alt="focusEquipment.name">
            <div class="focus-equipment-copy">
              <strong>{{ focusEquipment.name }}</strong>
              <p>{{ focusEquipment.brand || '站内装备' }} / {{ focusEquipment.model || '规格待补充' }}</p>
              <span>
                {{ focusEquipment.price ? `参考价 ${focusEquipment.price} 元` : '价格待补充' }}
                {{ focusEquipment.suitableFor ? ` · ${focusEquipment.suitableFor}` : '' }}
              </span>
            </div>
            <el-button round @click="router.push(`/equipment/${focusEquipment.id}`)">查看详情页</el-button>
          </div>

          <div v-if="loadingMessages" class="message-loading">
            <el-skeleton :rows="8" animated />
          </div>

          <template v-else>
            <div v-if="!currentMessages.length" class="empty-chat">
              <div class="empty-chat-icon">
                <el-icon><Service /></el-icon>
              </div>
              <h3>{{ workbench?.summaryTitle || profileInsight?.personaTitle || '聊聊你的器材升级计划' }}</h3>
              <p>{{ workbench?.summaryDescription || '你可以直接问预算搭配、打法适配、器材取舍，或者让顾问基于你的画像给出升级路线。' }}</p>

              <div class="starter-grid">
                <button
                  v-for="prompt in starterPrompts"
                  :key="prompt"
                  type="button"
                  class="starter-card"
                  @click="usePrompt(prompt)"
                >
                  {{ prompt }}
                </button>
              </div>

              <div class="empty-chat-tip">
                会话会自动保存在左侧，你可以围绕同一套器材持续追问。
              </div>
            </div>

            <el-scrollbar v-else class="message-scroll">
              <div class="message-list">
                <article
                  v-for="message in currentMessages"
                  :key="message.id"
                  :class="['message-row', message.role, { failed: message.status === 'FAILED' }]"
                >
                  <div class="message-avatar">
                    <el-icon v-if="message.role === 'assistant'"><Connection /></el-icon>
                    <span v-else>我</span>
                  </div>

                  <div class="message-bubble">
                    <div class="message-meta">
                      <strong>{{ message.role === 'assistant' ? 'DeepSeek 顾问' : '我的提问' }}</strong>
                      <span>{{ formatTime(message.createTime) }}</span>
                      <el-tag v-if="message.status === 'FAILED'" type="warning" size="small" round>待补发</el-tag>
                    </div>
                    <div class="message-content">{{ getVisibleMessageContent(message) }}</div>
                    <div class="message-foot" v-if="message.role === 'assistant' && getAssistantHint(message)">
                      <span class="message-hint">{{ getAssistantHint(message) }}</span>
                    </div>
                  </div>
                </article>
                <div ref="messageEndRef"></div>
              </div>
            </el-scrollbar>
          </template>

          <div class="composer-panel">
            <div class="composer-head">
              <div class="composer-kicker">输入你的问题</div>
            </div>

            <el-input
              v-model="draft"
              type="textarea"
              resize="none"
              :rows="4"
              maxlength="1500"
              show-word-limit
              placeholder="例如：给我配一套 2000 元左右、适合中近台弧圈快攻的底板和正手胶皮方案"
              @keyup.ctrl.enter="handleSend"
            />

            <div class="composer-actions">
              <div class="composer-note">
                <el-icon><Warning /></el-icon>
                <span>按 Ctrl + Enter 可直接发送。想换方向时，随时可以另开一个新会话。</span>
              </div>
              <div class="composer-buttons">
                <el-button plain round @click="handleCreateSession">另开新话题</el-button>
                <el-button type="primary" size="large" round :loading="sending" @click="handleSend">
                  {{ sending ? '顾问思考中...' : '发送给顾问' }}
                </el-button>
              </div>
            </div>
          </div>
        </section>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Connection,
  Delete,
  EditPen,
  MagicStick,
  Plus,
  RefreshRight,
  Service,
  Setting,
  Warning
} from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import Header from '@/components/Header.vue'
import {
  createAiChatSession,
  deleteAiChatSession,
  getAiChatSessionDetail,
  listAiChatSessions,
  sendAiChatMessage,
  updateAiChatSession
} from '@/api/aiChat'
import { getEquipmentDetail } from '@/api/equipment'
import { getProfileInsight, getRecommendationWorkbench } from '@/api/recommend'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { userInfo } = storeToRefs(userStore)

const loadingSessions = ref(false)
const loadingMessages = ref(false)
const sending = ref(false)
const sessions = ref([])
const activeSessionId = ref(null)
const currentSession = ref(null)
const currentMessages = ref([])
const draft = ref('')
const profileInsight = ref(null)
const workbench = ref(null)
const focusEquipment = ref(null)
const messageEndRef = ref(null)

const heroDescription = computed(() => {
  if (workbench.value?.summaryDescription) {
    return workbench.value.summaryDescription
  }
  if (profileInsight.value?.personaDescription) {
    return profileInsight.value.personaDescription
  }
  const nickname = userInfo.value?.nickname || userInfo.value?.username || '你'
  return `${nickname}，把画像、推荐结果和器材目录一起交给 DeepSeek，让它更像站内的资深装备教练。`
})

const keywordTags = computed(() => {
  const fromWorkbench = workbench.value?.profileKeywords || []
  const fromInsight = profileInsight.value?.coreTags || []
  return [...new Set([...fromWorkbench, ...fromInsight].filter(Boolean))].slice(0, 6)
})

const starterPrompts = computed(() => {
  const prompts = [
    '结合我的画像，先换底板还是先换胶皮更划算？',
    '给我配一套 1500 元左右、适合弧圈快攻的器材方案。',
    '我最近收藏的装备里，哪几款更适合稳定上手？',
    '如果我准备参加业余比赛，器材升级优先级应该怎么排？'
  ]

  if (focusEquipment.value?.name) {
    prompts.unshift(`这款 ${focusEquipment.value.name} 更适合什么打法和水平？`)
  }
  return prompts
})

watch(
  () => route.query.equipmentId,
  async () => {
    await loadFocusEquipment()
  }
)

onMounted(async () => {
  await Promise.allSettled([
    loadSessions(),
    loadInsightContext(),
    loadFocusEquipment()
  ])
})

async function loadSessions() {
  loadingSessions.value = true
  try {
    const res = await listAiChatSessions()
    sessions.value = res.data || []
    if (!activeSessionId.value && sessions.value.length) {
      await openSession(sessions.value[0].id)
    }
  } catch (error) {
    ElMessage.error(error?.message || '会话列表加载失败')
  } finally {
    loadingSessions.value = false
  }
}

async function loadInsightContext() {
  try {
    const [insightRes, workbenchRes] = await Promise.allSettled([
      getProfileInsight(),
      getRecommendationWorkbench()
    ])

    if (insightRes.status === 'fulfilled') {
      profileInsight.value = insightRes.value.data
    }
    if (workbenchRes.status === 'fulfilled') {
      workbench.value = workbenchRes.value.data
    }
  } catch (error) {
    console.warn('ai assistant context load failed', error)
  }
}

async function loadFocusEquipment() {
  const equipmentId = Number(route.query.equipmentId || 0)
  if (!equipmentId) {
    focusEquipment.value = null
    return
  }

  try {
    const res = await getEquipmentDetail(equipmentId, { trackView: false })
    focusEquipment.value = res.data
  } catch (error) {
    focusEquipment.value = null
  }
}

async function openSession(sessionId) {
  if (!sessionId) {
    return
  }
  loadingMessages.value = true
  try {
    const res = await getAiChatSessionDetail(sessionId)
    activeSessionId.value = sessionId
    currentSession.value = res.data?.session || null
    currentMessages.value = res.data?.messages || []
    await scrollToBottom()
  } catch (error) {
    ElMessage.error(error?.message || '会话详情加载失败')
  } finally {
    loadingMessages.value = false
  }
}

async function reloadActiveSession() {
  if (!activeSessionId.value) {
    return
  }
  await openSession(activeSessionId.value)
}

async function handleCreateSession() {
  try {
    const title = focusEquipment.value?.name ? `聊聊 ${focusEquipment.value.name}` : '新会话'
    const res = await createAiChatSession({ title })
    const session = res.data
    sessions.value = [session, ...sessions.value.filter((item) => item.id !== session.id)]
    activeSessionId.value = session.id
    currentSession.value = session
    currentMessages.value = []
    draft.value = ''
    await scrollToBottom()
  } catch (error) {
    ElMessage.error(error?.message || '创建会话失败')
  }
}

function usePrompt(prompt) {
  draft.value = prompt
}

async function handleSend() {
  const content = draft.value.trim()
  if (!content || sending.value) {
    return
  }

  sending.value = true
  try {
    const res = await sendAiChatMessage({
      sessionId: activeSessionId.value || undefined,
      content,
      equipmentId: focusEquipment.value?.id || undefined
    })
    const payload = res.data
    currentSession.value = payload.session
    activeSessionId.value = payload.session?.id || null
    currentMessages.value = [
      ...currentMessages.value,
      payload.userMessage,
      payload.assistantMessage
    ].filter(Boolean)
    draft.value = ''
    await loadSessions()

    const refreshed = sessions.value.find((item) => item.id === activeSessionId.value)
    if (refreshed) {
      currentSession.value = refreshed
    }

    if (payload.assistantMessage?.status === 'FAILED') {
      ElMessage.warning('这一轮回复没有完整返回，不过会话已经保存，你可以继续追问。')
    }

    await scrollToBottom()
  } catch (error) {
    ElMessage.error(error?.message || '发送失败')
  } finally {
    sending.value = false
  }
}

async function handleRenameSession(session) {
  if (!session?.id) {
    return
  }

  try {
    const { value } = await ElMessageBox.prompt('请输入新的会话标题', '重命名会话', {
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputValue: session.title || '新会话',
      inputPattern: /\S+/,
      inputErrorMessage: '标题不能为空'
    })
    const res = await updateAiChatSession(session.id, { title: value })
    const updated = res.data
    sessions.value = sessions.value.map((item) => (item.id === updated.id ? updated : item))
    if (currentSession.value?.id === updated.id) {
      currentSession.value = updated
    }
    ElMessage.success('会话标题已更新')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '重命名失败')
    }
  }
}

async function handleDeleteSession(session) {
  if (!session?.id) {
    return
  }

  try {
    await ElMessageBox.confirm('删除后将同时移除这段聊天记录，确定继续吗？', '删除会话', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })

    await deleteAiChatSession(session.id)
    sessions.value = sessions.value.filter((item) => item.id !== session.id)

    if (activeSessionId.value === session.id) {
      activeSessionId.value = null
      currentSession.value = null
      currentMessages.value = []
      if (sessions.value.length) {
        await openSession(sessions.value[0].id)
      }
    }
    ElMessage.success('会话已删除')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

function formatTime(time) {
  if (!time) {
    return '--'
  }
  return dayjs(time).format('MM-DD HH:mm')
}

function getSessionPreview(session) {
  const preview = session?.lastMessagePreview?.trim()
  if (!preview) {
    return '还没有聊天记录，点击开始第一轮咨询。'
  }
  if (/DEEPSEEK_API_KEY|DeepSeek API Key|HTTP\s+\d+|顾问缺少|暂时不可用|没有顺利完成/.test(preview)) {
    return '最近一轮没有完整返回，点击继续追问。'
  }
  return preview
}

function getVisibleMessageContent(message) {
  if (message?.role === 'assistant' && message?.status === 'FAILED') {
    return '这一轮回复没有顺利完成，不过你的提问已经保存下来了。你可以稍后重试，或者换个说法继续问我。'
  }
  return message?.content || ''
}

function getAssistantHint(message) {
  if (!message || message.role !== 'assistant') {
    return ''
  }
  if (message.status === 'FAILED') {
    return '提问已保存，可继续追问或稍后重试'
  }
  if (message.relatedEquipmentIds) {
    return '已结合当前器材、画像和站内推荐'
  }
  return '已结合画像、近期偏好和站内推荐'
}

async function scrollToBottom() {
  await nextTick()
  messageEndRef.value?.scrollIntoView({ behavior: 'smooth', block: 'end' })
}

function getEquipmentImageUrl(equipment) {
  // 优先使用 mainImage，其次使用 imageUrl，最后使用占位图
  return equipment.mainImage || equipment.imageUrl || '/placeholder.png'
}
</script>

<style scoped lang="scss">
.assistant-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(255, 168, 0, 0.18), transparent 32%),
    radial-gradient(circle at top right, rgba(26, 137, 221, 0.16), transparent 28%),
    linear-gradient(180deg, #0d1119 0%, #141b29 18%, #eef2f8 18%, #eef2f8 100%);
}

.assistant-main {
  max-width: 1480px;
  margin: 0 auto;
  padding: 28px 24px 48px;
}

.assistant-shell {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.session-sidebar {
  position: sticky;
  top: 96px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: calc(100vh - 140px);
  padding: 22px;
  border-radius: 28px;
  background: rgba(9, 14, 24, 0.94);
  color: #fff;
  box-shadow: 0 28px 70px rgba(5, 10, 18, 0.35);
}

.sidebar-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.sidebar-kicker {
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.sidebar-top h2 {
  margin-top: 6px;
  font-size: 24px;
}

.sidebar-hero {
  padding: 18px;
  border-radius: 22px;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.12), rgba(242, 140, 40, 0.18));
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 12px;
  font-weight: 700;
}

.sidebar-hero p {
  margin: 14px 0 0;
  color: rgba(255, 255, 255, 0.84);
  line-height: 1.75;
  font-size: 13px;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 18px;
}

.hero-metrics div {
  padding: 12px 10px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.1);
  text-align: center;
}

.hero-metrics strong {
  display: block;
  font-size: 18px;
  font-weight: 800;
}

.hero-metrics span {
  display: block;
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
}

.keyword-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.keyword-tags span {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  font-size: 12px;
}

.session-scroll {
  flex: 1;
  min-height: 0;
}

.session-item {
  width: 100%;
  margin-bottom: 12px;
  padding: 16px;
  border: 1px solid transparent;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.session-item:hover,
.session-item.active {
  transform: translateY(-2px);
  border-color: rgba(242, 140, 40, 0.58);
  background: rgba(255, 255, 255, 0.11);
}

.session-item-head,
.session-item-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.session-item-head strong {
  font-size: 14px;
}

.session-item-head span,
.session-item-foot span {
  color: rgba(255, 255, 255, 0.62);
  font-size: 12px;
}

.session-item p {
  margin: 10px 0 12px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.65;
  font-size: 13px;
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.session-empty {
  padding: 24px 0;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: calc(100vh - 140px);
}

.chat-topbar,
.focus-equipment-card,
.empty-chat,
.composer-panel,
.message-loading,
.message-scroll {
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 20px 55px rgba(13, 20, 33, 0.08);
}

.chat-topbar {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 20px;
  padding: 28px 30px;
}

.topbar-kicker,
.composer-kicker {
  color: #f28c28;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 800;
}

.topbar-copy h1 {
  margin-top: 10px;
  font-size: 30px;
  color: #0f172a;
}

.topbar-copy p {
  margin-top: 10px;
  max-width: 760px;
  color: #546174;
  line-height: 1.75;
}

.topbar-highlights {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 16px;
}

.topbar-highlights span {
  padding: 7px 12px;
  border-radius: 999px;
  background: #f3f7fd;
  color: #28415f;
  font-size: 12px;
  font-weight: 700;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.focus-tag {
  border: none;
  background: linear-gradient(135deg, #0f1e3d, #1f5b98);
}

.focus-equipment-card {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
  padding: 20px 24px;
}

.focus-equipment-card img {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 22px;
  background: #f3f5f9;
}

.focus-equipment-copy strong {
  font-size: 20px;
  color: #0f172a;
}

.focus-equipment-copy p,
.focus-equipment-copy span {
  display: block;
  margin-top: 6px;
  color: #5a6779;
}

.message-loading {
  padding: 28px;
}

.empty-chat {
  padding: 48px 34px;
  text-align: center;
}

.empty-chat-icon {
  width: 74px;
  height: 74px;
  margin: 0 auto;
  display: grid;
  place-items: center;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(242, 140, 40, 0.12), rgba(21, 94, 239, 0.12));
  color: #1d4ed8;
  font-size: 34px;
}

.empty-chat h3 {
  margin-top: 22px;
  font-size: 28px;
  color: #0f172a;
}

.empty-chat p {
  max-width: 760px;
  margin: 14px auto 0;
  color: #5f6b7c;
  line-height: 1.8;
}

.empty-chat-tip {
  display: inline-flex;
  margin-top: 20px;
  padding: 10px 16px;
  border-radius: 999px;
  background: #f5f8fc;
  color: #5d6a7b;
  font-size: 13px;
}

.starter-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 28px;
}

.starter-card {
  padding: 18px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 20px;
  background: linear-gradient(145deg, #fff, #f8fbff);
  color: #142033;
  text-align: left;
  line-height: 1.7;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.starter-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.message-scroll {
  flex: 1;
  min-height: 0;
  padding: 26px 0;
}

.message-list {
  padding: 0 28px;
}

.message-row {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
  margin-bottom: 18px;
}

.message-row.user {
  grid-template-columns: minmax(0, 1fr) 48px;
}

.message-row.user .message-avatar {
  order: 2;
  margin-left: auto;
}

.message-row.user .message-bubble {
  order: 1;
  margin-left: auto;
  background: linear-gradient(135deg, #113362, #1f5da8);
  color: #fff;
}

.message-row.user .message-meta span,
.message-row.user .message-foot,
.message-row.user .message-meta strong {
  color: rgba(255, 255, 255, 0.84);
}

.message-avatar {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  background: linear-gradient(135deg, #f28c28, #f6b55d);
  color: #fff;
  font-weight: 800;
}

.message-row.assistant .message-avatar {
  background: linear-gradient(135deg, #0f1f3d, #1b5ca0);
}

.message-bubble {
  max-width: min(820px, 100%);
  padding: 18px 20px;
  border-radius: 22px;
  background: #f4f7fb;
}

.message-row.failed .message-bubble {
  border: 1px solid rgba(245, 158, 11, 0.35);
  background: #fff9ed;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.message-meta strong {
  color: #0f172a;
}

.message-meta span {
  color: #7b8898;
  font-size: 12px;
}

.message-content {
  margin-top: 10px;
  white-space: pre-wrap;
  line-height: 1.85;
  color: inherit;
}

.message-foot {
  margin-top: 12px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  color: #7b8898;
  font-size: 12px;
}

.message-hint {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
  color: inherit;
}

.composer-panel {
  padding: 24px 24px 22px;
}

.composer-head {
  margin-bottom: 16px;
}

.composer-panel :deep(.el-textarea__inner) {
  min-height: 132px !important;
  padding: 16px 18px;
  border-radius: 22px;
  line-height: 1.8;
  box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.08);
}

.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
}

.composer-note {
  color: #6b7280;
  font-size: 13px;
  line-height: 1.7;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.composer-buttons {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 1180px) {
  .assistant-shell {
    grid-template-columns: 1fr;
  }

  .session-sidebar {
    position: static;
    min-height: auto;
  }
}

@media (max-width: 900px) {
  .assistant-main {
    padding: 20px 14px 32px;
  }

  .chat-topbar,
  .focus-equipment-card,
  .composer-panel {
    padding: 22px 18px;
  }

  .chat-topbar,
  .composer-actions,
  .composer-head {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-metrics {
    grid-template-columns: 1fr;
  }

  .focus-equipment-card {
    grid-template-columns: 1fr;
  }

  .starter-grid {
    grid-template-columns: 1fr;
  }

  .message-row,
  .message-row.user {
    grid-template-columns: 40px minmax(0, 1fr);
  }

  .message-row.user .message-avatar {
    order: 1;
  }

  .message-row.user .message-bubble {
    order: 2;
    margin-left: 0;
  }

  .message-avatar {
    width: 40px;
    height: 40px;
  }

  .message-list {
    padding: 0 16px;
  }
}
</style>
