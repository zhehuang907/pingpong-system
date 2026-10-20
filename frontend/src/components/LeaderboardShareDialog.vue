<template>
  <el-dialog
    :model-value="modelValue"
    width="760px"
    destroy-on-close
    class="leaderboard-share-dialog"
    @close="emit('update:modelValue', false)"
  >
    <template #header>
      <div class="dialog-header">
        <div>
          <div class="dialog-title">分享榜单</div>
          <div class="dialog-subtitle">把真实口碑榜单分享给更多球友</div>
        </div>
      </div>
    </template>

    <div class="dialog-body">
      <div class="channel-list">
        <button
          v-for="channel in channels"
          :key="channel.key"
          type="button"
          class="channel-card"
          @click="handleShare(channel.key)"
        >
          <span class="channel-name">{{ channel.label }}</span>
          <span class="channel-hint">{{ channel.hint }}</span>
        </button>
      </div>

      <div class="share-preview" v-loading="loading">
        <div class="preview-main">
          <div class="preview-meta">
            <div class="preview-badge">{{ boardName }}</div>
            <h3>{{ shareData.title || `${boardName}分享` }}</h3>
            <p>{{ shareData.shareText || defaultText }}</p>
            <div class="preview-link">{{ shareData.shareUrl || defaultUrl }}</div>
          </div>

          <div class="preview-actions">
            <el-button type="primary" @click="copyShareText">复制分享文案</el-button>
            <el-button @click="copyShareLink">复制链接</el-button>
          </div>
        </div>

        <div class="poster-wrap" v-if="shareData.posterSvg">
          <div class="poster-frame" v-html="shareData.posterSvg"></div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { recordLeaderboardShare } from '@/api/leaderboard'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  boardType: {
    type: String,
    default: ''
  },
  boardName: {
    type: String,
    default: '智能榜单'
  },
  equipmentId: {
    type: Number,
    default: null
  },
  equipmentName: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'shared'])

const channels = [
  { key: 'wechat', label: '微信', hint: '复制文案和海报，发给好友或群聊' },
  { key: 'weibo', label: '微博', hint: '打开微博分享窗口并带入推荐文案' },
  { key: 'qq', label: 'QQ', hint: '生成适合 QQ 分享的推荐链接' },
  { key: 'douyin', label: '抖音', hint: '复制短文案，适合视频或图文发布' }
]

const shareData = reactive({
  title: '',
  shareText: '',
  shareUrl: '',
  posterSvg: '',
  channel: ''
})

const loading = computed(() => shareData.channel === '__loading__')
const defaultUrl = computed(() =>
  props.equipmentId
    ? `${window.location.origin}/equipment/${props.equipmentId}`
    : `${window.location.origin}/leaderboards?boardType=${props.boardType}`
)
const defaultText = computed(() =>
  props.equipmentName
    ? `我在「${props.boardName}」里发现了 ${props.equipmentName}，一起来看看。`
    : `我在平台上发现了最新的「${props.boardName}」，一起来看看。`
)

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) {
      shareData.title = ''
      shareData.shareText = ''
      shareData.shareUrl = ''
      shareData.posterSvg = ''
      shareData.channel = ''
    }
  }
)

const handleShare = async (channel) => {
  shareData.channel = '__loading__'
  try {
    const res = await recordLeaderboardShare({
      boardType: props.boardType,
      equipmentId: props.equipmentId,
      channel
    })

    Object.assign(shareData, res.data, { channel })
    await dispatchShare(channel)
    emit('shared', { channel, boardType: props.boardType, equipmentId: props.equipmentId })
  } catch (error) {
    shareData.channel = ''
  }
}

const dispatchShare = async (channel) => {
  const url = shareData.shareUrl || defaultUrl.value
  const text = shareData.shareText || defaultText.value
  const title = shareData.title || props.boardName

  if (channel === 'weibo') {
    const shareUrl = `https://service.weibo.com/share/share.php?title=${encodeURIComponent(text)}&url=${encodeURIComponent(url)}`
    window.open(shareUrl, '_blank', 'noopener,noreferrer')
    ElMessage.success('已打开微博分享窗口')
    return
  }

  if (channel === 'qq') {
    const shareUrl = `https://connect.qq.com/widget/shareqq/index.html?title=${encodeURIComponent(title)}&summary=${encodeURIComponent(text)}&url=${encodeURIComponent(url)}`
    window.open(shareUrl, '_blank', 'noopener,noreferrer')
    ElMessage.success('已打开 QQ 分享窗口')
    return
  }

  await copyToClipboard(`${text}\n${url}`)
  ElMessage.success(channel === 'wechat' ? '分享文案已复制，可直接粘贴到微信' : '分享文案已复制，可直接粘贴到抖音')
}

const copyShareText = async () => {
  await copyToClipboard(`${shareData.shareText || defaultText.value}\n${shareData.shareUrl || defaultUrl.value}`)
  ElMessage.success('分享文案已复制')
}

const copyShareLink = async () => {
  await copyToClipboard(shareData.shareUrl || defaultUrl.value)
  ElMessage.success('链接已复制')
}

const copyToClipboard = async (text) => {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }

  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  document.body.removeChild(textarea)
}
</script>

<style scoped lang="scss">
.leaderboard-share-dialog {
  :deep(.el-dialog) {
    border-radius: 28px;
    overflow: hidden;
    background: linear-gradient(180deg, #fffef8 0%, #f5f8f1 100%);
  }

  :deep(.el-dialog__header) {
    margin: 0;
    padding: 24px 28px 0;
  }

  :deep(.el-dialog__body) {
    padding: 20px 28px 28px;
  }
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dialog-title {
  font-size: 28px;
  font-weight: 700;
  color: #103b2b;
}

.dialog-subtitle {
  margin-top: 6px;
  color: #5e6b63;
}

.dialog-body {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 20px;
}

.channel-list {
  display: grid;
  gap: 12px;
}

.channel-card {
  padding: 16px;
  border-radius: 20px;
  border: 1px solid rgba(16, 59, 43, 0.12);
  background: #ffffff;
  text-align: left;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  cursor: pointer;
}

.channel-card:hover {
  transform: translateY(-2px);
  border-color: rgba(242, 140, 40, 0.5);
  box-shadow: 0 18px 36px rgba(16, 59, 43, 0.08);
}

.channel-name {
  display: block;
  font-size: 17px;
  font-weight: 700;
  color: #103b2b;
}

.channel-hint {
  display: block;
  margin-top: 6px;
  color: #66756c;
  line-height: 1.6;
}

.share-preview {
  min-height: 420px;
  padding: 20px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(242, 140, 40, 0.18), transparent 34%),
    linear-gradient(160deg, rgba(16, 59, 43, 0.96), rgba(20, 92, 54, 0.92));
  color: #fff;
}

.preview-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.preview-badge {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: #ffe7bf;
  font-size: 13px;
  letter-spacing: 0.08em;
}

.preview-meta h3 {
  margin: 16px 0 10px;
  font-size: 28px;
}

.preview-meta p {
  margin: 0;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.86);
}

.preview-link {
  margin-top: 14px;
  color: #ffe7bf;
  word-break: break-all;
}

.preview-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.poster-wrap {
  margin-top: 18px;
}

.poster-frame {
  max-width: 280px;
  margin-left: auto;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 24px 44px rgba(0, 0, 0, 0.24);
}

.poster-frame :deep(svg) {
  display: block;
  width: 100%;
  height: auto;
}

@media (max-width: 900px) {
  .dialog-body {
    grid-template-columns: 1fr;
  }

  .poster-frame {
    margin: 18px auto 0;
  }
}
</style>
