<template>
  <el-dialog
    :model-value="modelValue"
    title="裁剪头像"
    width="760px"
    destroy-on-close
    @close="handleClose"
  >
    <div class="cropper-shell">
      <div class="cropper-stage">
        <div
          class="crop-box"
          @mousedown="startDrag"
          @wheel.prevent="handleWheel"
        >
          <img
            v-if="imageSrc"
            :src="imageSrc"
            :style="imageStyle"
            class="crop-image"
            draggable="false"
          >
          <div class="crop-overlay" />
        </div>

        <div class="crop-tips">
          <span>拖动图片调整位置</span>
          <span>鼠标滚轮或滑块可以缩放</span>
        </div>
      </div>

      <aside class="preview-panel">
        <div class="preview-title">预览</div>
        <div class="preview-circle">
          <canvas ref="previewCanvasRef" width="180" height="180" />
        </div>

        <div class="control-group">
          <span>缩放</span>
          <el-slider v-model="zoom" :min="1" :max="3" :step="0.01" />
        </div>

        <div class="control-group">
          <span>横向位置</span>
          <el-slider v-model="offsetX" :min="offsetRangeX[0]" :max="offsetRangeX[1]" :step="1" />
        </div>

        <div class="control-group">
          <span>纵向位置</span>
          <el-slider v-model="offsetY" :min="offsetRangeY[0]" :max="offsetRangeY[1]" :step="1" />
        </div>
      </aside>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="rendering" @click="handleConfirm">保存头像</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'

const CROP_SIZE = 320
const OUTPUT_SIZE = 400

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  file: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const imageSrc = ref('')
const imageElement = ref(null)
const previewCanvasRef = ref(null)
const rendering = ref(false)
const zoom = ref(1)
const offsetX = ref(0)
const offsetY = ref(0)
const baseScale = ref(1)
const imageMeta = reactive({
  width: 0,
  height: 0
})

const dragging = reactive({
  active: false,
  startX: 0,
  startY: 0,
  originX: 0,
  originY: 0
})

const displayWidth = computed(() => imageMeta.width * baseScale.value * zoom.value)
const displayHeight = computed(() => imageMeta.height * baseScale.value * zoom.value)
const offsetRangeX = computed(() => {
  const halfRange = Math.max((displayWidth.value - CROP_SIZE) / 2, 0)
  return [-halfRange, halfRange]
})
const offsetRangeY = computed(() => {
  const halfRange = Math.max((displayHeight.value - CROP_SIZE) / 2, 0)
  return [-halfRange, halfRange]
})

const imageStyle = computed(() => ({
  width: `${displayWidth.value}px`,
  height: `${displayHeight.value}px`,
  left: `${(CROP_SIZE - displayWidth.value) / 2 + offsetX.value}px`,
  top: `${(CROP_SIZE - displayHeight.value) / 2 + offsetY.value}px`
}))

watch(
  () => [props.modelValue, props.file],
  async ([visible, file]) => {
    if (!visible || !file) {
      resetState()
      return
    }
    await initializeImage(file)
  },
  { immediate: true }
)

watch([zoom, offsetX, offsetY], async () => {
  clampOffsets()
  await nextTick()
  renderPreview()
})

function resetState() {
  imageSrc.value = ''
  imageElement.value = null
  imageMeta.width = 0
  imageMeta.height = 0
  baseScale.value = 1
  zoom.value = 1
  offsetX.value = 0
  offsetY.value = 0
}

function initializeImage(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const image = new Image()
      image.onload = async () => {
        imageSrc.value = reader.result
        imageElement.value = image
        imageMeta.width = image.naturalWidth
        imageMeta.height = image.naturalHeight
        baseScale.value = Math.max(CROP_SIZE / image.naturalWidth, CROP_SIZE / image.naturalHeight)
        zoom.value = 1
        offsetX.value = 0
        offsetY.value = 0
        await nextTick()
        renderPreview()
        resolve()
      }
      image.onerror = reject
      image.src = reader.result
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

function clampOffsets() {
  offsetX.value = clamp(offsetX.value, offsetRangeX.value[0], offsetRangeX.value[1])
  offsetY.value = clamp(offsetY.value, offsetRangeY.value[0], offsetRangeY.value[1])
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max)
}

function startDrag(event) {
  if (!imageSrc.value) {
    return
  }
  dragging.active = true
  dragging.startX = event.clientX
  dragging.startY = event.clientY
  dragging.originX = offsetX.value
  dragging.originY = offsetY.value
  window.addEventListener('mousemove', handleDrag)
  window.addEventListener('mouseup', stopDrag)
}

function handleDrag(event) {
  if (!dragging.active) {
    return
  }
  offsetX.value = dragging.originX + (event.clientX - dragging.startX)
  offsetY.value = dragging.originY + (event.clientY - dragging.startY)
  clampOffsets()
}

function stopDrag() {
  dragging.active = false
  window.removeEventListener('mousemove', handleDrag)
  window.removeEventListener('mouseup', stopDrag)
}

function handleWheel(event) {
  const delta = event.deltaY > 0 ? -0.05 : 0.05
  zoom.value = clamp(Number((zoom.value + delta).toFixed(2)), 1, 3)
}

function renderPreview() {
  if (!imageElement.value || !previewCanvasRef.value) {
    return
  }

  const canvas = previewCanvasRef.value
  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, canvas.width, canvas.height)

  const crop = calculateSourceRect()
  ctx.save()
  ctx.beginPath()
  ctx.arc(canvas.width / 2, canvas.height / 2, canvas.width / 2, 0, Math.PI * 2)
  ctx.clip()
  ctx.drawImage(
    imageElement.value,
    crop.sourceX,
    crop.sourceY,
    crop.sourceWidth,
    crop.sourceHeight,
    0,
    0,
    canvas.width,
    canvas.height
  )
  ctx.restore()
}

function calculateSourceRect() {
  const scale = baseScale.value * zoom.value
  const left = (CROP_SIZE - displayWidth.value) / 2 + offsetX.value
  const top = (CROP_SIZE - displayHeight.value) / 2 + offsetY.value
  const sourceX = clamp((0 - left) / scale, 0, imageMeta.width)
  const sourceY = clamp((0 - top) / scale, 0, imageMeta.height)
  const sourceWidth = clamp(CROP_SIZE / scale, 1, imageMeta.width)
  const sourceHeight = clamp(CROP_SIZE / scale, 1, imageMeta.height)

  return {
    sourceX,
    sourceY,
    sourceWidth,
    sourceHeight
  }
}

function handleClose() {
  stopDrag()
  emit('update:modelValue', false)
}

function handleConfirm() {
  if (!imageElement.value) {
    return
  }

  rendering.value = true
  const canvas = document.createElement('canvas')
  canvas.width = OUTPUT_SIZE
  canvas.height = OUTPUT_SIZE
  const ctx = canvas.getContext('2d')
  const crop = calculateSourceRect()

  ctx.drawImage(
    imageElement.value,
    crop.sourceX,
    crop.sourceY,
    crop.sourceWidth,
    crop.sourceHeight,
    0,
    0,
    OUTPUT_SIZE,
    OUTPUT_SIZE
  )

  canvas.toBlob((blob) => {
    rendering.value = false
    if (!blob) {
      return
    }
    emit('confirm', blob)
  }, 'image/png', 0.92)
}

onBeforeUnmount(() => {
  stopDrag()
})
</script>

<style scoped lang="scss">
.cropper-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 240px;
  gap: 24px;
  align-items: start;
}

.crop-box {
  position: relative;
  width: 320px;
  height: 320px;
  margin: 0 auto;
  overflow: hidden;
  border-radius: 28px;
  background:
    linear-gradient(135deg, rgba(29, 88, 70, 0.08), rgba(240, 138, 36, 0.08)),
    repeating-conic-gradient(from 45deg, #f3f4f6 0% 25%, #e7ebef 0% 50%) 50% / 24px 24px;
  cursor: grab;
}

.crop-box:active {
  cursor: grabbing;
}

.crop-image {
  position: absolute;
  user-select: none;
  pointer-events: none;
}

.crop-overlay {
  position: absolute;
  inset: 0;
  border-radius: 28px;
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.92);
  pointer-events: none;
}

.crop-overlay::before {
  content: '';
  position: absolute;
  inset: 14px;
  border: 2px solid rgba(29, 88, 70, 0.7);
  border-radius: 24px;
}

.crop-tips {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 14px;
  color: #6b7280;
  font-size: 13px;
}

.preview-panel {
  padding: 18px;
  border-radius: 24px;
  background: linear-gradient(180deg, #fffaf3 0%, #f5fbf7 100%);
  border: 1px solid rgba(29, 88, 70, 0.08);
}

.preview-title {
  font-size: 16px;
  font-weight: 700;
  color: #17352e;
}

.preview-circle {
  display: grid;
  place-items: center;
  margin: 18px 0 20px;
}

.preview-circle canvas {
  width: 180px;
  height: 180px;
  border-radius: 50%;
  box-shadow: 0 18px 36px rgba(23, 53, 46, 0.12);
  background: #fff;
}

.control-group + .control-group {
  margin-top: 18px;
}

.control-group span {
  display: block;
  margin-bottom: 10px;
  color: #55645b;
  font-size: 13px;
}

@media (max-width: 768px) {
  .cropper-shell {
    grid-template-columns: 1fr;
  }

  .crop-box {
    width: 320px;
    height: 320px;
  }
}
</style>
