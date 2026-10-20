<template>
  <div class="equipment-image-uploader">
    <input
      ref="fileInputRef"
      type="file"
      accept=".jpg,.jpeg,.png,image/jpeg,image/png"
      multiple
      class="hidden-input"
      @change="handleFileSelect"
    />

    <div class="uploader-header">
      <div>
        <div class="uploader-title">{{ copy.title }}</div>
        <div class="uploader-hint">{{ copy.hint }}</div>
      </div>
      <el-button type="primary" @click="openFileDialog" :disabled="uploading || reachLimit">
        {{ copy.selectButton }}
      </el-button>
    </div>

    <div class="uploader-guides">
      <span>{{ copy.guide }}</span>
      <span>{{ copy.limitPrefix }} {{ maxCount }} {{ copy.limitSuffix }} {{ displayItems.length }} {{ copy.countSuffix }}</span>
    </div>

    <div v-if="!displayItems.length" class="empty-state">
      <div class="empty-title">{{ copy.emptyTitle }}</div>
      <div class="empty-text">{{ copy.emptyText }}</div>
    </div>

    <div v-else class="preview-grid">
      <div
        v-for="item in displayItems"
        :key="item.uid"
        :class="['preview-card', { main: allowMainImage && item.url === localMainImage }]"
      >
        <div class="preview-image-wrap">
          <img :src="item.previewUrl || item.url" :alt="item.name" class="preview-image">
          <div v-if="allowMainImage && item.url === localMainImage" class="main-badge">主图</div>
        </div>

        <div class="preview-info">
          <div class="preview-name">{{ item.name }}</div>
          <div class="preview-status">
            <el-tag v-if="item.status === 'done'" type="success">上传成功</el-tag>
            <el-tag v-else-if="item.status === 'uploading'" type="warning">上传中</el-tag>
            <el-tag v-else-if="item.status === 'error'" type="danger">上传失败</el-tag>
            <el-tag v-else>处理中</el-tag>
          </div>
          <div v-if="item.status === 'uploading'" class="progress-line">
            <el-progress :percentage="item.progress" :stroke-width="8" />
          </div>
          <div v-if="item.error" class="error-text">{{ item.error }}</div>
        </div>

        <div class="preview-actions">
          <el-button
            v-if="allowMainImage"
            link
            type="primary"
            :disabled="item.status !== 'done' || item.url === localMainImage"
            @click="setMainImage(item.url)"
          >
            设为主图
          </el-button>
          <el-button link type="danger" @click="removeItem(item.uid)">删除</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadEquipmentImage } from '@/api/upload'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  mainImage: {
    type: String,
    default: ''
  },
  maxCount: {
    type: Number,
    default: 6
  },
  scene: {
    type: String,
    default: 'equipment'
  },
  allowMainImage: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['update:modelValue', 'update:mainImage'])

const fileInputRef = ref(null)
const items = ref([])
const localMainImage = ref('')
const uploadingCount = ref(0)

const uploading = computed(() => uploadingCount.value > 0)
const reachLimit = computed(() => items.value.filter((item) => item.status !== 'error').length >= props.maxCount)
const displayItems = computed(() => items.value)
const copy = computed(() => {
  if (props.scene === 'comment') {
    return {
      title: '评论配图上传',
      hint: '支持 JPG、JPEG、PNG，单张不超过 5MB，上传前会自动压缩优化。',
      selectButton: '选择评论图片',
      guide: '图片为可选项，建议上传实拍图、细节图或上手图，帮助其他用户更直观地了解装备表现。',
      limitPrefix: '最多可上传',
      limitSuffix: '张，当前',
      countSuffix: '张。',
      emptyTitle: '还没有上传评论图片',
      emptyText: '选择图片后即可预览、压缩并上传评论配图。'
    }
  }

  return {
    title: '装备照片上传',
    hint: '支持 JPG、JPEG、PNG，单张不超过 5MB，上传前会自动压缩优化。',
    selectButton: '选择图片',
    guide: '建议上传多角度实拍图，主图会优先用于列表与详情展示。',
    limitPrefix: '最多可上传',
    limitSuffix: '张，当前',
    countSuffix: '张。',
    emptyTitle: '还没有上传图片',
    emptyText: '选择图片后即可预览、压缩并上传装备照片。'
  }
})

watch(
  () => props.modelValue,
  (images) => {
    const normalized = Array.isArray(images) ? images.filter(Boolean) : []
    const pendingItems = items.value.filter((item) => item.status === 'uploading')
    const stableItems = normalized.map((url, index) => ({
      uid: `remote-${index}-${url}`,
      name: `已上传图片 ${index + 1}`,
      url,
      previewUrl: url,
      progress: 100,
      status: 'done',
      error: ''
    }))
    items.value = [...stableItems, ...pendingItems]
  },
  { immediate: true, deep: true }
)

watch(
  () => props.mainImage,
  (value) => {
    if (!props.allowMainImage) {
      localMainImage.value = ''
      return
    }
    localMainImage.value = value || props.modelValue[0] || ''
  },
  { immediate: true }
)

const openFileDialog = () => {
  fileInputRef.value?.click()
}

const handleFileSelect = async (event) => {
  const files = Array.from(event.target.files || [])
  if (!files.length) {
    return
  }

  const activeCount = items.value.filter((item) => item.status !== 'error').length
  if (activeCount + files.length > props.maxCount) {
    ElMessage.warning(`最多上传 ${props.maxCount} 张图片`)
  }

  const availableSlots = Math.max(props.maxCount - activeCount, 0)
  const acceptedFiles = files.slice(0, availableSlots)
  for (const file of acceptedFiles) {
    await uploadSingleFile(file)
  }
  event.target.value = ''
}

const uploadSingleFile = async (file) => {
  const validationError = validateFile(file)
  if (validationError) {
    ElMessage.warning(validationError)
    return
  }

  const uid = `${Date.now()}-${Math.random().toString(36).slice(2)}`
  const item = {
    uid,
    name: file.name,
    url: '',
    previewUrl: URL.createObjectURL(file),
    progress: 0,
    status: 'preparing',
    error: ''
  }
  items.value.push(item)
  uploadingCount.value += 1

  try {
    const compressedFile = await compressImage(file)
    if (compressedFile.size > 5 * 1024 * 1024) {
      throw new Error('压缩后的图片仍超过 5MB，请更换更小的图片')
    }

    item.status = 'uploading'
    const res = await uploadEquipmentImage(compressedFile, (progressEvent) => {
      if (!progressEvent.total) {
        item.progress = 0
        return
      }
      item.progress = Math.min(100, Math.round((progressEvent.loaded / progressEvent.total) * 100))
    })

    cleanupPreviewUrl(item.previewUrl)
    item.url = res.data?.url || ''
    item.previewUrl = item.url || item.previewUrl
    item.progress = 100
    item.status = 'done'
    item.error = ''

    syncToParent()
    ElMessage.success(`${file.name} 上传成功`)
  } catch (error) {
    item.status = 'error'
    item.error = error.message || '上传失败'
    ElMessage.error(item.error)
  } finally {
    uploadingCount.value = Math.max(0, uploadingCount.value - 1)
  }
}

const removeItem = (uid) => {
  const removedItem = items.value.find((item) => item.uid === uid)
  if (removedItem) {
    cleanupPreviewUrl(removedItem.previewUrl)
  }
  items.value = items.value.filter((item) => item.uid !== uid)
  syncToParent()
}

const setMainImage = (url) => {
  if (!props.allowMainImage) {
    return
  }
  localMainImage.value = url
  emit('update:mainImage', url)
}

const syncToParent = () => {
  const uploadedUrls = items.value
    .filter((item) => item.status === 'done' && item.url)
    .map((item) => item.url)

  emit('update:modelValue', uploadedUrls)

  if (!props.allowMainImage) {
    localMainImage.value = ''
    emit('update:mainImage', '')
    return
  }

  const nextMainImage = localMainImage.value && uploadedUrls.includes(localMainImage.value)
    ? localMainImage.value
    : (uploadedUrls[0] || '')

  localMainImage.value = nextMainImage
  emit('update:mainImage', nextMainImage)
}

const validateFile = (file) => {
  const allowedTypes = ['image/jpeg', 'image/png']
  const allowedExt = ['jpg', 'jpeg', 'png']
  const extension = file.name.split('.').pop()?.toLowerCase() || ''

  if (!allowedTypes.includes(file.type) || !allowedExt.includes(extension)) {
    return '仅支持 JPG、JPEG、PNG 格式图片'
  }

  if (file.size > 5 * 1024 * 1024) {
    return '图片大小不能超过 5MB'
  }

  return ''
}

const compressImage = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const image = new Image()
      image.onload = () => {
        const maxEdge = props.scene === 'comment' ? 1600 : 1800
        const ratio = Math.min(1, maxEdge / Math.max(image.width, image.height))
        const canvas = document.createElement('canvas')
        canvas.width = Math.max(1, Math.round(image.width * ratio))
        canvas.height = Math.max(1, Math.round(image.height * ratio))
        const context = canvas.getContext('2d')
        if (!context) {
          reject(new Error('图片处理失败，请重试'))
          return
        }

        context.drawImage(image, 0, 0, canvas.width, canvas.height)
        const outputType = file.type === 'image/png' ? 'image/png' : 'image/jpeg'
        const quality = outputType === 'image/jpeg' ? 0.82 : undefined

        canvas.toBlob((blob) => {
          if (!blob) {
            reject(new Error('图片压缩失败，请更换图片后重试'))
            return
          }

          const compressedFile = new File([blob], normalizeFileName(file.name, outputType), {
            type: outputType,
            lastModified: Date.now()
          })
          resolve(compressedFile.size < file.size ? compressedFile : file)
        }, outputType, quality)
      }
      image.onerror = () => reject(new Error('无法读取图片内容'))
      image.src = reader.result
    }
    reader.onerror = () => reject(new Error('图片读取失败'))
    reader.readAsDataURL(file)
  })
}

const normalizeFileName = (name, outputType) => {
  const baseName = name.replace(/\.[^.]+$/, '')
  const extension = outputType === 'image/png' ? 'png' : 'jpg'
  return `${baseName}.${extension}`
}

const cleanupPreviewUrl = (previewUrl) => {
  if (typeof previewUrl === 'string' && previewUrl.startsWith('blob:')) {
    URL.revokeObjectURL(previewUrl)
  }
}
</script>

<style scoped lang="scss">
.equipment-image-uploader {
  width: 100%;
  padding: 18px;
  border: 1px dashed rgba(16, 59, 43, 0.18);
  border-radius: 22px;
  background:
    radial-gradient(circle at top right, rgba(242, 140, 40, 0.12), transparent 32%),
    linear-gradient(180deg, #fffef8 0%, #f5f8f1 100%);
}

.hidden-input {
  display: none;
}

.uploader-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.uploader-title {
  font-size: 18px;
  font-weight: 700;
  color: #103b2b;
}

.uploader-hint,
.uploader-guides {
  color: #65746b;
  line-height: 1.8;
}

.uploader-guides {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 10px;
  font-size: 13px;
}

.empty-state {
  margin-top: 18px;
  padding: 32px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  text-align: center;
}

.empty-title {
  font-size: 16px;
  font-weight: 700;
  color: #103b2b;
}

.empty-text {
  margin-top: 8px;
  color: #76837c;
}

.preview-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.preview-card {
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(16, 59, 43, 0.08);
  box-shadow: 0 12px 26px rgba(16, 59, 43, 0.08);
}

.preview-card.main {
  border-color: rgba(242, 140, 40, 0.45);
  box-shadow: 0 16px 30px rgba(242, 140, 40, 0.12);
}

.preview-image-wrap {
  position: relative;
  overflow: hidden;
  border-radius: 14px;
  aspect-ratio: 4 / 3;
  background: #eef3ee;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.main-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(16, 59, 43, 0.84);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.preview-info {
  margin-top: 12px;
}

.preview-name {
  font-size: 14px;
  font-weight: 600;
  color: #103b2b;
  word-break: break-all;
}

.preview-status {
  margin-top: 8px;
}

.progress-line {
  margin-top: 10px;
}

.error-text {
  margin-top: 10px;
  color: #c45656;
  font-size: 13px;
}

.preview-actions {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
}

@media (max-width: 768px) {
  .uploader-header {
    flex-direction: column;
  }

  .preview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
