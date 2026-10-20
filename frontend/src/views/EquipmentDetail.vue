<template>
  <div class="detail-page">
    <Header />

    <div class="main-content">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="10" animated />
      </div>

      <div v-else class="detail-container">
        <el-row :gutter="40">
          <!-- 左侧图片区 -->
          <el-col :span="10">
            <div class="image-section">
              <div class="image-gallery">
                <el-image :src="getImageUrl(equipment)" fit="cover" class="main-image"
                  :preview-src-list="[getImageUrl(equipment)]" preview-teleported>
                  <template #error>
                    <img src="/placeholder.png" class="main-image placeholder-image" style="object-fit: cover; width: 100%; height: 100%;" />
                  </template>
                </el-image>

                <!-- 缩略图列表 -->
                <div class="thumbnail-list" v-if="equipment.images && equipment.images.length > 1">
                  <div v-for="(img, idx) in equipment.images" :key="idx"
                    :class="['thumbnail-item', { active: currentImageIndex === idx }]" @click="currentImageIndex = idx">
                    <el-image :src="img" fit="cover" class="thumbnail-image" />
                  </div>
                </div>
              </div>
            </div>
          </el-col>

          <!-- 右侧信息区 -->
          <el-col :span="14">
            <div class="info-section">
              <!-- 标题和品牌 -->
              <div class="title-section">
                <h1 class="equipment-name">
                  <span class="name-text">{{ equipment.name }}</span>
                  <el-tag v-if="equipment.isHot" type="danger" effect="dark" class="hot-tag">
                    <el-icon>
                      <StarFilled />
                    </el-icon>
                    热销
                  </el-tag>
                </h1>
                <p class="brand-info" v-if="equipment.brand">
                  <el-icon>
                    <Collection />
                  </el-icon>
                  {{ equipment.brand }} · {{ equipment.model }}
                </p>
              </div>

              <!-- 价格区 -->
              <div class="price-section">
                <div class="price-main">
                  <span class="currency">¥</span>
                  <span class="current-price">{{ equipment.price }}</span>
                </div>
                <div class="price-extra" v-if="equipment.originalPrice">
                  <span class="original-price">¥{{ equipment.originalPrice }}</span>
                  <el-tag type="danger" size="small" v-if="equipment.discount">
                    {{ equipment.discount }}折
                  </el-tag>
                </div>
              </div>

              <!-- 统计信息 -->
              <div class="stats-grid">
                <div class="stat-card">
                  <div class="stat-icon view">
                    <el-icon>
                      <View />
                    </el-icon>
                  </div>
                  <div class="stat-info">
                    <span class="stat-value">{{ formatNumber(equipment.viewCount || 0) }}</span>
                    <span class="stat-label">浏览</span>
                  </div>
                </div>
                <div class="stat-card">
                  <div class="stat-icon favorite">
                    <el-icon>
                      <Star />
                    </el-icon>
                  </div>
                  <div class="stat-info">
                    <span class="stat-value">{{ formatNumber(equipment.favoriteCount || 0) }}</span>
                    <span class="stat-label">收藏</span>
                  </div>
                </div>
                <div class="stat-card">
                  <div class="stat-icon sales">
                    <el-icon>
                      <ShoppingCart />
                    </el-icon>
                  </div>
                  <div class="stat-info">
                    <span class="stat-value">{{ formatNumber(equipment.salesCount || 0) }}</span>
                    <span class="stat-label">销量</span>
                  </div>
                </div>
                <div class="stat-card">
                  <div class="stat-icon rating">
                    <el-icon>
                      <Orange />
                    </el-icon>
                  </div>
                  <div class="stat-info">
                    <span class="stat-value">{{ equipment.rating || 0 }}</span>
                    <span class="stat-label">评分</span>
                  </div>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div class="action-buttons">
                <el-button type="primary" size="large" @click="toggleFavorite"
                  :class="['favorite-btn', { active: isFavorite }]" round>
                  <el-icon>
                    <Star />
                  </el-icon>
                  {{ isFavorite ? '已收藏' : '收藏' }}
                </el-button>
                <el-button type="success" size="large" @click="addToCart" round>
                  <el-icon>
                    <ShoppingCart />
                  </el-icon>
                  加入购物车
                </el-button>
                <el-button
                  type="warning"
                  size="large"
                  round
                  @click="$router.push({ path: '/ai-assistant', query: { equipmentId: route.params.id } })"
                >
                  问智能顾问
                </el-button>
                <el-button size="large" @click="goBack" round>
                  返回列表
                </el-button>
              </div>

              <!-- 库存状态 -->
              <div class="stock-status" v-if="equipment.stock !== undefined">
                <el-tag :type="equipment.stock > 0 ? 'success' : 'danger'" effect="dark">
                  <el-icon v-if="equipment.stock > 0">
                    <CircleCheck />
                  </el-icon>
                  <el-icon v-else>
                    <CircleClose />
                  </el-icon>
                  {{ equipment.stock > 0 ? '有货' : '缺货' }}
                </el-tag>
                <span class="stock-count" v-if="equipment.stock > 0">
                  库存：{{ equipment.stock }}
                </span>
              </div>

              <!-- 商品描述 -->
              <div class="description-section">
                <div class="section-title">
                  <el-icon>
                    <Document />
                  </el-icon>
                  <span>商品描述</span>
                </div>
                <p class="description-text">{{ equipment.description || '暂无描述' }}</p>
              </div>

              <!-- 产品特点 -->
              <div class="features-section" v-if="featureList.length">
                <div class="section-title">
                  <el-icon>
                    <List />
                  </el-icon>
                  <span>产品特点</span>
                </div>
                <div class="features-tags">
                  <el-tag v-for="(feature, idx) in featureList" :key="`${feature}-${idx}`" effect="plain" round
                    class="feature-tag">
                    {{ feature.trim() }}
                  </el-tag>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 相似推荐 -->
      <div class="recommend-section" v-if="!loading && similarRecommend.length > 0">
        <div class="section-header">
          <h2>
            <el-icon>
              <Guide />
            </el-icon>
            相似推荐
          </h2>
          <el-button text @click="$router.push('/equipment')">
            查看更多
            <el-icon>
              <ArrowRight />
            </el-icon>
          </el-button>
        </div>
        <el-row :gutter="20">
          <el-col :span="6" v-for="item in similarRecommend" :key="item.id">
            <EquipmentCard :equipment="item" />
          </el-col>
        </el-row>
      </div>

      <!-- 用户评论 -->
      <div class="comments-section" v-if="!loading">
        <div class="section-header">
          <h2>
            <el-icon>
              <ChatDotRound />
            </el-icon>
            用户评论
            <span class="comment-count" v-if="comments.length > 0">({{ comments.length }})</span>
          </h2>
          <el-button type="primary" round @click="writeComment">
            <el-icon><Edit /></el-icon>
            写评论
          </el-button>
        </div>
        <el-card shadow="hover" class="comments-card">
          <div class="comment-list">
            <div v-if="comments.length === 0" class="no-comments">
              <el-empty description="暂无评论" :image-size="150">
                <el-button type="primary" @click="writeComment">
                  写评论
                </el-button>
              </el-empty>
            </div>
            <div v-else>
              <div v-for="comment in comments" :key="comment.id" class="comment-item">
                <div class="comment-header">
                  <div class="user-info">
                    <el-avatar :size="44" :src="comment.user?.avatar" class="user-avatar">
                      {{ comment.user?.nickname?.charAt(0) || 'U' }}
                    </el-avatar>
                    <div class="user-details">
                      <span class="username">{{ comment.user?.nickname || comment.user?.username || '匿名用户' }}</span>
                      <div class="rating-display">
                        <el-rate v-model="comment.rating" disabled size="small" />
                      </div>
                    </div>
                  </div>
                  <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                </div>
                <div class="comment-content">
                  {{ comment.content }}
                </div>
                <div class="comment-actions">
                  <el-button
                    text
                    size="small"
                    :loading="Boolean(likeLoadingMap[comment.id])"
                    :class="{ active: comment.likedByCurrentUser }"
                    @click="handleCommentLike(comment)"
                  >
                    <el-icon>
                      <Top />
                    </el-icon>
                    {{ comment.likeCount || 0 }}
                  </el-button>
                  <el-button
                    text
                    size="small"
                    :class="{ active: isReplyExpanded(comment.id) }"
                    @click="toggleReplyPanel(comment)"
                  >
                    <el-icon>
                      <ChatLineSquare />
                    </el-icon>
                    {{ isReplyExpanded(comment.id) ? '收起回复' : '回复' }}{{ comment.replyCount ? ` ${comment.replyCount}` : '' }}
                  </el-button>
                </div>
                <div v-if="isReplyExpanded(comment.id)" class="reply-panel">
                  <div v-if="replyLoadingMap[comment.id]" class="reply-loading">
                    <el-skeleton :rows="2" animated />
                  </div>
                  <div v-else-if="getReplyList(comment.id).length" class="reply-list">
                    <div v-for="reply in getReplyList(comment.id)" :key="reply.id" class="reply-item">
                      <div class="reply-item-header">
                        <div class="reply-user">
                          <el-avatar :size="30" :src="reply.user?.avatar">
                            {{ reply.user?.nickname?.charAt(0) || reply.user?.username?.charAt(0) || 'U' }}
                          </el-avatar>
                          <span>{{ reply.user?.nickname || reply.user?.username || '匿名用户' }}</span>
                        </div>
                        <span class="reply-time">{{ formatTime(reply.createTime) }}</span>
                      </div>
                      <div class="reply-content">{{ reply.content }}</div>
                      <div class="reply-actions">
                        <el-button
                          text
                          size="small"
                          :loading="Boolean(likeLoadingMap[reply.id])"
                          :class="{ active: reply.likedByCurrentUser }"
                          @click="handleCommentLike(reply, comment.id)"
                        >
                          <el-icon>
                            <Top />
                          </el-icon>
                          {{ reply.likeCount || 0 }}
                        </el-button>
                      </div>
                    </div>
                  </div>
                  <div v-else class="reply-empty">还没有回复，来抢个沙发吧</div>
                  <div class="reply-compose">
                    <template v-if="hasToken()">
                      <el-input
                        v-model="replyDraftMap[comment.id]"
                        type="textarea"
                        :rows="2"
                        maxlength="300"
                        show-word-limit
                        placeholder="写下你的回复..."
                      />
                      <div class="reply-compose-actions">
                        <span class="reply-compose-tip">回复 {{ comment.user?.nickname || comment.user?.username || '匿名用户' }}</span>
                        <el-button
                          type="primary"
                          size="small"
                          :loading="Boolean(replySubmittingMap[comment.id])"
                          @click="submitReply(comment)"
                        >
                          提交回复
                        </el-button>
                      </div>
                    </template>
                    <div v-else class="reply-login-tip">
                      <span>登录后即可参与回复互动</span>
                      <el-button link type="primary" @click="redirectToLogin">立即登录</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 评论弹窗 -->
    <el-dialog v-model="commentDialogVisible" title="发表评论" width="520px" destroy-on-close>
      <div class="comment-form">
        <div class="form-item">
          <label>评分</label>
          <el-rate v-model="commentForm.rating" :texts="['很差', '较差', '一般', '满意', '非常满意']" show-text />
        </div>
        <div class="form-item">
          <label>评论内容</label>
          <el-input v-model="commentForm.content" type="textarea" :rows="4" placeholder="分享您的使用体验..." maxlength="500" show-word-limit />
        </div>
      </div>
      <template #footer>
        <el-button @click="commentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitComment" :loading="commentSubmitting">提交评论</el-button>
      </template>
    </el-dialog>

    <Footer />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { getEquipmentDetail } from '@/api/equipment'
import { recordHistory } from '@/api/history'
import { getSimilarRecommend, recordClick } from '@/api/recommend'
import { addComment, getEquipmentComments, getReplyComments, likeComment as apiLikeComment } from '@/api/comment'
import { checkFavorite, toggleFavorite as apiToggleFavorite } from '@/api/favorite'
import { addToCart as apiAddToCart } from '@/api/cart'
import { getToken } from '@/utils/auth'
import { isRealtimeEvent, subscribeRealtimeUpdates } from '@/utils/realtime'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import EquipmentCard from '@/components/EquipmentCard.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const equipment = ref({})
const similarRecommend = ref([])
const comments = ref([])
const isFavorite = ref(false)
const currentImageIndex = ref(0)
const commentDialogVisible = ref(false)
const commentSubmitting = ref(false)
const commentForm = ref({ rating: 5, content: '' })
const expandedReplyIds = ref([])
const replyDraftMap = reactive({})
const replyListMap = reactive({})
const replyLoadingMap = reactive({})
const replySubmittingMap = reactive({})
const likeLoadingMap = reactive({})
const historySession = ref(null)
const featureList = computed(() => normalizeFeatureList(equipment.value?.features))

let stopRealtime = null
let refreshTimer = null

onMounted(async () => {
  await initializePage(route.params.id)
  window.addEventListener('beforeunload', handleBeforeUnload)
  stopRealtime = subscribeRealtimeUpdates((message) => {
    if (!isRealtimeEvent(message, ['equipment_changed', 'comment_changed'])) {
      return
    }

    const equipmentId = Number(route.params.id)
    const payloadEquipmentId = Number(message?.payload?.equipmentId || 0)
    const payloadEquipmentIds = Array.isArray(message?.payload?.equipmentIds)
      ? message.payload.equipmentIds.map((item) => Number(item))
      : []

    if (message.type === 'equipment_changed' && payloadEquipmentId === equipmentId) {
      if (message.payload?.action === 'delete') {
        ElMessage.warning('当前器材已被管理员下线，已为你返回列表页')
        router.push('/equipment-list')
        return
      }
      scheduleRealtimeRefresh('equipment')
      return
    }

    if (message.type === 'comment_changed' && (payloadEquipmentId === equipmentId || payloadEquipmentIds.includes(equipmentId))) {
      scheduleRealtimeRefresh('comment')
    }
  })
})

onBeforeRouteLeave(async () => {
  await finalizeHistorySession('route_leave')
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  finalizeHistorySession('unload', true)
  if (stopRealtime) {
    stopRealtime()
  }
  if (refreshTimer) {
    window.clearTimeout(refreshTimer)
  }
})

watch(() => route.params.id, async (newId, oldId) => {
  if (!newId || newId === oldId) {
    return
  }
  await finalizeHistorySession('route_leave')
  await initializePage(newId)
})

watch(currentImageIndex, (newValue, oldValue) => {
  if (oldValue !== undefined && newValue !== oldValue) {
    markHistoryAction('switch_image')
  }
})

async function initializePage(equipmentId) {
  if (!equipmentId) {
    return
  }
  await loadEquipmentDetail(equipmentId)
  await Promise.all([
    loadSimilarRecommend(equipmentId),
    loadComments(equipmentId),
    checkFavoriteStatus(equipmentId)
  ])
  recordClick(equipmentId)
  await startHistorySession()
}

async function loadEquipmentDetail(equipmentId, trackView = true) {
  try {
    loading.value = true
    const res = await getEquipmentDetail(equipmentId, { trackView })
    equipment.value = res.data || {}
    currentImageIndex.value = 0
  } catch (error) {
    ElMessage.error('加载商品详情失败')
  } finally {
    loading.value = false
  }
}

async function loadSimilarRecommend(equipmentId) {
  try {
    const res = await getSimilarRecommend(equipmentId, { count: 4 })
    similarRecommend.value = res.data || []
  } catch (error) {
    console.error('加载相似推荐失败:', error)
  }
}

async function loadComments(equipmentId) {
  try {
    const res = await getEquipmentComments(equipmentId, { pageNum: 1, pageSize: 10 })
    comments.value = normalizeCommentList(res.data?.records || [])

    const expandedIds = [...expandedReplyIds.value]
    if (expandedIds.length) {
      await Promise.all(expandedIds.map((commentId) => loadReplyList(commentId, true)))
    }
  } catch (error) {
    console.error('加载评论失败:', error)
  }
}

function normalizeCommentList(list = []) {
  return list.map((item) => ({
    ...item,
    likedByCurrentUser: Boolean(item?.likedByCurrentUser)
  }))
}

function hasToken() {
  return Boolean(getToken())
}

function redirectToLogin() {
  router.push('/login')
}

function isReplyExpanded(commentId) {
  return expandedReplyIds.value.includes(commentId)
}

function getReplyList(commentId) {
  return Array.isArray(replyListMap[commentId]) ? replyListMap[commentId] : []
}

async function loadReplyList(commentId, force = false) {
  if (!commentId || replyLoadingMap[commentId]) {
    return
  }

  if (!force && Array.isArray(replyListMap[commentId])) {
    return
  }

  replyLoadingMap[commentId] = true
  try {
    const res = await getReplyComments(commentId)
    replyListMap[commentId] = normalizeCommentList(res.data || [])
  } catch (error) {
    console.error('加载回复失败:', error)
    ElMessage.error('加载回复失败，请稍后重试')
  } finally {
    replyLoadingMap[commentId] = false
  }
}

async function toggleReplyPanel(comment) {
  const commentId = Number(comment?.id)
  if (!commentId) {
    return
  }

  if (isReplyExpanded(commentId)) {
    expandedReplyIds.value = expandedReplyIds.value.filter((item) => item !== commentId)
    return
  }

  expandedReplyIds.value = [...expandedReplyIds.value, commentId]
  markHistoryAction('expand_reply')
  await loadReplyList(commentId)
}

async function handleCommentLike(comment, parentCommentId = null) {
  if (!hasToken()) {
    ElMessage.warning('请先登录')
    redirectToLogin()
    return
  }

  const commentId = Number(comment?.id)
  if (!commentId || likeLoadingMap[commentId]) {
    return
  }

  const likedBefore = Boolean(comment.likedByCurrentUser)
  likeLoadingMap[commentId] = true

  try {
    const res = await apiLikeComment(commentId)
    if (!res.data) {
      throw new Error('评论点赞失败')
    }

    comment.likedByCurrentUser = !likedBefore
    comment.likeCount = Math.max(0, Number(comment.likeCount || 0) + (likedBefore ? -1 : 1))
    markHistoryAction(likedBefore ? 'unlike_comment' : 'like_comment')

    if (parentCommentId && Array.isArray(replyListMap[parentCommentId])) {
      replyListMap[parentCommentId] = [...replyListMap[parentCommentId]]
    }

    ElMessage.success(likedBefore ? '已取消点赞' : '点赞成功')
  } catch (error) {
    ElMessage.error(error?.message || '评论点赞失败')
  } finally {
    likeLoadingMap[commentId] = false
  }
}

async function submitReply(comment) {
  if (!hasToken()) {
    ElMessage.warning('请先登录')
    redirectToLogin()
    return
  }

  const commentId = Number(comment?.id)
  const content = (replyDraftMap[commentId] || '').trim()
  if (!content || content.length < 2) {
    ElMessage.warning('回复内容至少输入 2 个字')
    return
  }

  replySubmittingMap[commentId] = true
  try {
    const res = await addComment({
      equipmentId: Number(route.params.id),
      parentId: commentId,
      content
    })
    replyDraftMap[commentId] = ''
    markHistoryAction('reply_comment')
    if (!isReplyExpanded(commentId)) {
      expandedReplyIds.value = [...expandedReplyIds.value, commentId]
    }
    await Promise.all([
      loadComments(route.params.id),
      loadReplyList(commentId, true)
    ])
    ElMessage.success(res.msg || '回复已提交')
  } catch (error) {
    ElMessage.error(error?.message || '回复提交失败')
  } finally {
    replySubmittingMap[commentId] = false
  }
}

async function checkFavoriteStatus(equipmentId) {
  if (!getToken()) {
    return
  }

  try {
    const res = await checkFavorite(equipmentId)
    isFavorite.value = res.data
  } catch (error) {
    console.error('查询收藏状态失败:', error)
  }
}

async function startHistorySession() {
  if (!getToken() || !equipment.value?.id) {
    return
  }

  const session = {
    sessionId: `equipment_${equipment.value.id}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    equipmentId: Number(equipment.value.id),
    enterTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    actions: new Set(['view_detail']),
    sourcePage: document.referrer || 'equipment',
    contentSnapshot: equipment.value.name || ''
  }
  historySession.value = session

  try {
    await recordHistory(buildHistoryPayload(session))
  } catch (error) {
    console.debug('记录浏览历史失败:', error)
  }
}

function buildHistoryPayload(session, exitAction) {
  if (exitAction) {
    session.actions.add(exitAction)
  }
  const leaveTime = dayjs().format('YYYY-MM-DD HH:mm:ss')
  const durationSeconds = Math.max(dayjs(leaveTime).diff(dayjs(session.enterTime), 'second'), 0)
  return {
    equipmentId: session.equipmentId,
    contentType: 'equipment_detail',
    sourcePage: session.sourcePage,
    sessionId: session.sessionId,
    enterTime: session.enterTime,
    leaveTime,
    durationSeconds,
    interactionCount: session.actions.size,
    interactionActions: Array.from(session.actions),
    contentSnapshot: session.contentSnapshot
  }
}

async function finalizeHistorySession(exitAction, keepalive = false) {
  const session = historySession.value
  if (!session || !getToken()) {
    return
  }

  historySession.value = null
  const payload = buildHistoryPayload(session, exitAction)

  try {
    if (keepalive) {
      await fetch('/api/history/record', {
        method: 'POST',
        keepalive: true,
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${getToken()}`
        },
        body: JSON.stringify(payload)
      })
      return
    }
    await recordHistory(payload)
  } catch (error) {
    console.debug('结束浏览会话失败:', error)
  }
}

function handleBeforeUnload() {
  finalizeHistorySession('unload', true)
}

function markHistoryAction(action) {
  if (!historySession.value) {
    return
  }
  historySession.value.actions.add(action)
}

function normalizeFeatureList(rawFeatures) {
  if (!rawFeatures) {
    return []
  }

  const source = Array.isArray(rawFeatures) ? rawFeatures.join(',') : String(rawFeatures)
  const seen = new Set()

  return source
    .split(/[,，/|]+/)
    .map((item) => item.replace(/\s+/g, ' ').trim())
    .filter((item) => {
      if (!item) {
        return false
      }
      const key = item.toLowerCase()
      if (seen.has(key)) {
        return false
      }
      seen.add(key)
      return true
    })
}

const toggleFavorite = async () => {
  if (!getToken()) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    await apiToggleFavorite(route.params.id)
    isFavorite.value = !isFavorite.value
    markHistoryAction('favorite')
    ElMessage.success(isFavorite.value ? '收藏成功' : '已取消收藏')
  } catch (error) {
    ElMessage.error('收藏操作失败')
  }
}

const addToCart = async () => {
  if (!getToken()) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  if (equipment.value.stock <= 0) {
    ElMessage.warning('当前商品暂时缺货')
    return
  }

  try {
    await apiAddToCart(route.params.id, { quantity: 1 })
    markHistoryAction('add_to_cart')
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error('加入购物车失败')
  }
}

const writeComment = () => {
  if (!getToken()) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  markHistoryAction('write_comment')
  commentForm.value = { rating: 5, content: '' }
  commentDialogVisible.value = true
}

const submitComment = async () => {
  if (!commentForm.value.content || commentForm.value.content.trim().length < 2) {
    ElMessage.warning('评论内容至少输入 2 个字')
    return
  }
  commentSubmitting.value = true
  try {
    const res = await addComment({
      equipmentId: Number(route.params.id),
      rating: commentForm.value.rating,
      content: commentForm.value.content.trim()
    })
    markHistoryAction('submit_comment')
    ElMessage.success(res.msg || '评论提交成功，感谢你的反馈')
    commentDialogVisible.value = false
    await loadComments(route.params.id)
  } catch (error) {
    ElMessage.error(error?.message || '评论提交失败')
  } finally {
    commentSubmitting.value = false
  }
}

const goBack = () => {
  router.back()
}

const formatNumber = (num) => {
  if (num >= 10000) {
    return `${(num / 10000).toFixed(1)}w`
  }
  if (num >= 1000) {
    return `${(num / 1000).toFixed(1)}k`
  }
  return num.toString()
}

const getImageUrl = (equipment) => {
  // 优先使用 mainImage，其次使用 imageUrl，最后使用占位图
  return equipment.mainImage || equipment.imageUrl || '/placeholder.png'
}

const formatTime = (time) => {
  if (!time) return ''
  const now = new Date()
  const commentTime = new Date(time)
  const diff = now - commentTime

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`

  return commentTime.toLocaleDateString('zh-CN')
}

function scheduleRealtimeRefresh(scope) {
  if (refreshTimer) {
    window.clearTimeout(refreshTimer)
  }
  refreshTimer = window.setTimeout(async () => {
    const equipmentId = route.params.id
    if (scope === 'comment') {
      await Promise.all([
        loadEquipmentDetail(equipmentId, false),
        loadComments(equipmentId)
      ])
      return
    }

    await Promise.all([
      loadEquipmentDetail(equipmentId, false),
      loadSimilarRecommend(equipmentId),
      loadComments(equipmentId)
    ])
  }, 250)
}
</script>

<style scoped lang="scss">
.detail-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
}

.main-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 30px 20px;
}

.loading-container {
  background: white;
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.detail-container {
  background: white;
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.image-section {
  .image-gallery {
    position: relative;

    .main-image {
      width: 100%;
      height: 500px;
      border-radius: 16px;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
      transition: transform 0.3s ease;

      &:hover {
        transform: scale(1.02);
      }
    }

    .image-error {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 400px;
      background: #f5f7fa;
      border-radius: 16px;
      color: #909399;

      .el-icon {
        font-size: 48px;
        margin-bottom: 10px;
      }
    }

    .thumbnail-list {
      display: flex;
      gap: 12px;
      margin-top: 16px;

      .thumbnail-item {
        width: 80px;
        height: 80px;
        border-radius: 8px;
        overflow: hidden;
        cursor: pointer;
        border: 2px solid transparent;
        transition: all 0.3s ease;

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        }

        &.active {
          border-color: var(--primary-color);
          box-shadow: 0 4px 12px rgba(255, 107, 53, 0.3);
        }

        .thumbnail-image {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
      }
    }
  }
}

.info-section {
  .title-section {
    margin-bottom: 24px;

    .equipment-name {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 32px;
      font-weight: 600;
      color: #2c3e50;
      margin-bottom: 12px;

      .name-text {
        background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
      }

      .hot-tag {
        animation: pulse 2s infinite;
      }
    }

    .brand-info {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #5a6c7d;
      font-size: 16px;

      .el-icon {
        color: var(--primary-color);
      }
    }
  }

  .price-section {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 28px;
    padding: 20px;
    background: linear-gradient(135deg, #fff5f2 0%, #ffe8e0 100%);
    border-radius: 12px;

    .price-main {
      display: flex;
      align-items: baseline;

      .currency {
        font-size: 20px;
        color: #ff6b35;
        font-weight: 600;
      }

      .current-price {
        font-size: 42px;
        color: #ff6b35;
        font-weight: 700;
        line-height: 1;
      }
    }

    .price-extra {
      display: flex;
      align-items: center;
      gap: 8px;

      .original-price {
        font-size: 18px;
        color: #909399;
        text-decoration: line-through;
      }
    }
  }

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    margin-bottom: 30px;

    .stat-card {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px;
      background: #f8f9fa;
      border-radius: 12px;
      transition: all 0.3s ease;

      &:hover {
        background: linear-gradient(135deg, #f8f9fa 0%, #e8ecf1 100%);
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
      }

      .stat-icon {
        width: 44px;
        height: 44px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 10px;
        font-size: 22px;

        &.view {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          color: white;
        }

        &.favorite {
          background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
          color: white;
        }

        &.sales {
          background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
          color: white;
        }

        &.rating {
          background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
          color: white;
        }
      }

      .stat-info {
        display: flex;
        flex-direction: column;

        .stat-value {
          font-size: 20px;
          font-weight: 600;
          color: #2c3e50;
        }

        .stat-label {
          font-size: 13px;
          color: #8b98a7;
          margin-top: 2px;
        }
      }
    }
  }

  .action-buttons {
    display: flex;
    gap: 12px;
    margin-bottom: 30px;

    .favorite-btn {
      &.active {
        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
        border: none;
        box-shadow: 0 4px 12px rgba(245, 87, 108, 0.3);
      }
    }

    .el-button {
      flex: 1;
      font-weight: 500;
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
      }
    }
  }

  .stock-status {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 28px;
    padding: 14px 18px;
    background: #f0f9ff;
    border-radius: 10px;

    .stock-count {
      color: #5a6c7d;
      font-size: 14px;
    }
  }

  .description-section,
  .features-section {
    margin-top: 28px;
    padding: 24px;
    background: #f8f9fa;
    border-radius: 12px;

    .section-title {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 16px;
      font-size: 18px;
      font-weight: 600;
      color: #2c3e50;

      .el-icon {
        color: var(--primary-color);
        font-size: 20px;
      }
    }

    .description-text {
      color: #5a6c7d;
      line-height: 1.8;
      font-size: 15px;
    }

    .features-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;

      .feature-tag {
        padding: 8px 16px;
        font-size: 14px;
        background: #ffffff;
        border-color: rgba(31, 58, 147, 0.26);
        color: #16356f;
        --el-tag-bg-color: #ffffff;
        --el-tag-border-color: rgba(31, 58, 147, 0.26);
        --el-tag-text-color: #16356f;
        transition: all 0.3s ease;

        &:hover {
          background: #16356f;
          color: #ffffff;
          --el-tag-bg-color: #16356f;
          --el-tag-border-color: #16356f;
          --el-tag-text-color: #ffffff;
        }
      }
    }
  }
}

.recommend-section {
  margin-top: 40px;

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    h2 {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 24px;
      font-weight: 600;
      color: #2c3e50;

      .el-icon {
        color: var(--primary-color);
        font-size: 26px;
      }
    }

    .el-button {
      color: #5a6c7d;
      font-weight: 500;

      &:hover {
        color: var(--primary-color);
      }
    }
  }
}

.comments-section {
  margin-top: 40px;

  .section-header {
    margin-bottom: 24px;

    h2 {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 24px;
      font-weight: 600;
      color: #2c3e50;

      .el-icon {
        color: var(--primary-color);
        font-size: 26px;
      }

      .comment-count {
        font-size: 16px;
        color: #8b98a7;
        font-weight: 400;
      }
    }
  }

  .comments-card {
    border-radius: 16px;

    &:hover {
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    }
  }

  .comment-list {
    .no-comments {
      padding: 60px 20px;
    }

    .comment-item {
      padding: 24px 0;
      border-bottom: 1px solid #e8ecef;

      &:last-child {
        border-bottom: none;
      }

      .comment-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 16px;

        .user-info {
          display: flex;
          gap: 14px;
          align-items: center;

          .user-avatar {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          }

          .user-details {
            display: flex;
            flex-direction: column;
            gap: 6px;

            .username {
              font-size: 15px;
              font-weight: 500;
              color: #2c3e50;
            }

            .rating-display {
              .el-rate {
                color: #ffd04b;
              }
            }
          }
        }

        .comment-time {
          color: #8b98a7;
          font-size: 13px;
        }
      }

      .comment-content {
        color: #5a6c7d;
        line-height: 1.7;
        font-size: 15px;
        margin-bottom: 14px;
      }

      .comment-actions {
        display: flex;
        gap: 16px;

        .el-button {
          color: #8b98a7;

          &:hover,
          &.active {
            color: var(--primary-color);
          }
        }
      }

      .reply-panel {
        margin-top: 16px;
        padding: 16px;
        border-radius: 14px;
        background: #f7f9fc;
        border: 1px solid #e8edf4;
      }

      .reply-list {
        display: grid;
        gap: 12px;
        margin-bottom: 14px;
      }

      .reply-item {
        padding: 12px 14px;
        background: #fff;
        border-radius: 12px;
        border: 1px solid #edf1f6;
      }

      .reply-item-header {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        margin-bottom: 8px;
      }

      .reply-user {
        display: flex;
        align-items: center;
        gap: 8px;
        color: #2c3e50;
        font-size: 14px;
        font-weight: 500;
      }

      .reply-time {
        color: #97a4b2;
        font-size: 12px;
      }

      .reply-content {
        color: #5a6c7d;
        line-height: 1.6;
        font-size: 14px;
      }

      .reply-actions {
        margin-top: 8px;

        .el-button {
          color: #8b98a7;

          &:hover,
          &.active {
            color: var(--primary-color);
          }
        }
      }

      .reply-empty {
        color: #8b98a7;
        font-size: 13px;
        margin-bottom: 12px;
      }

      .reply-compose {
        display: grid;
        gap: 10px;
      }

      .reply-compose-actions {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 12px;
      }

      .reply-compose-tip,
      .reply-login-tip {
        color: #8b98a7;
        font-size: 13px;
      }

      .reply-login-tip {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }
    }
  }
}

@keyframes pulse {

  0%,
  100% {
    transform: scale(1);
  }

  50% {
    transform: scale(1.05);
  }
}

@media (max-width: 768px) {
  .detail-container {
    padding: 20px;
  }

  .image-section .image-gallery .main-image {
    height: 300px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .action-buttons {
    flex-direction: column;
  }
}

h2 {
  margin-bottom: 20px;
  color: #333;
}

.comment-item {
  padding: 20px 0;
  border-bottom: 1px solid #eee;

  &:last-child {
    border-bottom: none;
  }

  .comment-header {
    display: flex;
    align-items: center;
    gap: 15px;
    margin-bottom: 10px;

    .comment-info {
      flex: 1;

      .username {
        display: block;
        margin-bottom: 5px;
        color: #333;
        font-weight: 500;
      }
    }

    .comment-time {
      color: #999;
      font-size: 13px;
    }
  }

  .comment-content {
    color: #606266;
    line-height: 1.6;
  }
}

.no-comments {
  text-align: center;
  color: #999;
  padding: 40px 0;
}
</style>
