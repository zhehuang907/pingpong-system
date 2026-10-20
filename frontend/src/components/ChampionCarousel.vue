<template>
  <div
    class="champion-carousel-widget"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
  >
    <div class="carousel-stage">
      <div class="carousel-track" :style="trackStyle">
        <article
          v-for="(slide, index) in normalizedSlides"
          :key="`${slide.title}-${index}`"
          class="carousel-slide"
          :style="slideStyle"
        >
          <img
            :src="slide.image"
            :alt="slide.title"
            class="carousel-image"
            :style="imageStyle(slide)"
            :loading="index <= 1 ? 'eager' : 'lazy'"
            :fetchpriority="index === 0 ? 'high' : 'low'"
            decoding="async"
            draggable="false"
          />
          <div class="carousel-image-mask"></div>

          <div class="carousel-counter">{{ formatIndex(currentIndex + 1) }} / {{ formatIndex(total) }}</div>

          <div class="carousel-copy">
            <span class="carousel-tag">{{ slide.tag }}</span>
            <div class="carousel-summary">{{ slide.summary }}</div>
            <h4>{{ slide.title }}</h4>
            <p>{{ slide.description }}</p>

            <div class="carousel-meta">
              <span>{{ slide.metaLeft }}</span>
              <span>{{ slide.metaRight }}</span>
            </div>

            <div class="carousel-credit">{{ slide.credit }}</div>
          </div>
        </article>
      </div>

      <button
        v-if="total > 1"
        type="button"
        class="carousel-nav prev"
        aria-label="上一张"
        @click="goPrev"
      >
        <el-icon><ArrowLeftBold /></el-icon>
      </button>

      <button
        v-if="total > 1"
        type="button"
        class="carousel-nav next"
        aria-label="下一张"
        @click="goNext"
      >
        <el-icon><ArrowRightBold /></el-icon>
      </button>
    </div>

    <div v-if="total > 1" class="carousel-footer">
      <div class="carousel-indicators" role="tablist" aria-label="轮播指示器">
        <button
          v-for="(slide, index) in normalizedSlides"
          :key="`${slide.title}-dot-${index}`"
          type="button"
          class="indicator-dot"
          :class="{ active: index === currentIndex }"
          :aria-label="`切换到第 ${index + 1} 张`"
          @click="goTo(index)"
        ></button>
      </div>

      <div class="carousel-status">{{ pausedByHover ? '悬停中，自动播放已暂停' : autoplayText }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { ArrowLeftBold, ArrowRightBold } from '@element-plus/icons-vue'

const props = defineProps({
  slides: {
    type: Array,
    default: () => []
  },
  autoplayInterval: {
    type: Number,
    default: 4600
  }
})

const currentIndex = ref(0)
const pausedByHover = ref(false)

let autoplayTimer = null

const normalizedSlides = computed(() =>
  (props.slides || []).map((slide) => ({
    tag: '',
    title: '',
    description: '',
    summary: '',
    metaLeft: '',
    metaRight: '',
    credit: '',
    position: 'center center',
    fit: 'cover',
    scale: 1.02,
    image: '',
    ...slide
  }))
)

const total = computed(() => normalizedSlides.value.length)

const trackStyle = computed(() => ({
  width: `${Math.max(total.value, 1) * 100}%`,
  transform: `translate3d(-${currentIndex.value * (100 / Math.max(total.value, 1))}%, 0, 0)`
}))

const slideStyle = computed(() => ({
  flex: `0 0 ${100 / Math.max(total.value, 1)}%`,
  width: `${100 / Math.max(total.value, 1)}%`
}))

const autoplayText = computed(() => `自动播放间隔 ${Math.round(props.autoplayInterval / 1000)} 秒`)

function formatIndex(value) {
  return String(value).padStart(2, '0')
}

function imageStyle(slide) {
  return {
    objectPosition: slide.position || 'center center',
    objectFit: slide.fit || 'cover',
    transform: `scale(${slide.scale || 1.02})`
  }
}

function stopAutoplay() {
  if (autoplayTimer) {
    window.clearInterval(autoplayTimer)
    autoplayTimer = null
  }
}

function startAutoplay() {
  stopAutoplay()

  if (pausedByHover.value || total.value <= 1) {
    return
  }

  autoplayTimer = window.setInterval(() => {
    goNext()
  }, props.autoplayInterval)
}

function goTo(index) {
  if (!total.value) {
    return
  }

  currentIndex.value = (index + total.value) % total.value
  startAutoplay()
}

function goPrev() {
  goTo(currentIndex.value - 1)
}

function goNext() {
  goTo(currentIndex.value + 1)
}

function handleMouseEnter() {
  pausedByHover.value = true
  stopAutoplay()
}

function handleMouseLeave() {
  pausedByHover.value = false
  startAutoplay()
}

watch(total, (value) => {
  if (!value) {
    currentIndex.value = 0
    stopAutoplay()
    return
  }

  if (currentIndex.value >= value) {
    currentIndex.value = 0
  }

  startAutoplay()
})

watch(
  () => props.autoplayInterval,
  () => {
    startAutoplay()
  }
)

onMounted(() => {
  startAutoplay()
})

onUnmounted(() => {
  stopAutoplay()
})
</script>

<style scoped lang="scss">
.champion-carousel-widget {
  display: flex;
  height: 100%;
  min-height: 100%;
  flex-direction: column;
  gap: 12px;
}

.carousel-stage {
  position: relative;
  flex: 1;
  min-height: 390px;
  overflow: hidden;
  border-radius: 28px;
  background: #091228;
  box-shadow: 0 28px 52px rgba(5, 17, 43, 0.34);
}

.carousel-track {
  display: flex;
  height: 100%;
  transition: transform 720ms cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform;
  backface-visibility: hidden;
}

.carousel-slide {
  position: relative;
  height: 100%;
  min-height: 390px;
  overflow: hidden;
  contain: layout paint;
}

.carousel-slide::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 40px 40px;
  opacity: 0.18;
  pointer-events: none;
}

.carousel-image {
  width: 100%;
  height: 100%;
  display: block;
  filter: saturate(1.06) contrast(1.03);
  transition: transform 0.7s ease;
}

.carousel-image-mask {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at top right, rgba(0, 232, 255, 0.12), transparent 30%),
    linear-gradient(180deg, rgba(7, 10, 16, 0.1) 0%, rgba(7, 10, 16, 0.4) 40%, rgba(7, 10, 16, 0.85) 100%);
}

.carousel-counter {
  position: absolute;
  top: 22px;
  right: 22px;
  z-index: 2;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(8, 17, 39, 0.56);
  border: 1px solid rgba(255, 255, 255, 0.14);
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  backdrop-filter: blur(12px);
}

.carousel-copy {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 2;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: flex-start;
  padding: 40px 50px;
  text-shadow: 0 4px 30px rgba(0, 0, 0, 0.6);
  pointer-events: none;
}

.carousel-tag {
  width: fit-content;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.16);
  color: #ffffff;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.carousel-summary {
  width: fit-content;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(0, 232, 255, 0.16);
  color: #a7fbff;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.carousel-copy h4 {
  margin: 0;
  max-width: 560px;
  color: #ffffff;
  font-size: clamp(28px, 2.8vw, 42px);
  line-height: 1.1;
  font-weight: 900;
  letter-spacing: -0.01em;
}

.carousel-copy p {
  max-width: 620px;
  margin: 0;
  color: rgba(232, 239, 255, 0.9);
  font-size: 14px;
  line-height: 1.62;
}

.carousel-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.carousel-meta span {
  display: inline-flex;
  align-items: center;
  padding: 9px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: #e8f4ff;
  font-size: 12px;
  font-weight: 700;
}

.carousel-credit {
  color: rgba(224, 235, 255, 0.72);
  font-size: 11px;
  line-height: 1.5;
}

.carousel-nav {
  position: absolute;
  top: 50%;
  z-index: 3;
  width: 48px;
  height: 48px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 50%;
  background: rgba(8, 17, 39, 0.6);
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transform: translateY(-50%);
  backdrop-filter: blur(12px);
  transition: transform 0.24s ease, background 0.24s ease, border-color 0.24s ease;
}

.carousel-nav:hover {
  background: rgba(0, 232, 255, 0.18);
  border-color: rgba(0, 232, 255, 0.34);
  transform: translateY(-50%) scale(1.04);
}

.carousel-nav.prev {
  left: 18px;
}

.carousel-nav.next {
  right: 18px;
}

.carousel-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 0 6px;
}

.carousel-indicators {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.indicator-dot {
  width: 34px;
  height: 6px;
  border: none;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.24);
  cursor: pointer;
  transition: transform 0.24s ease, background 0.24s ease;
}

.indicator-dot.active {
  background: linear-gradient(90deg, #00e8ff, #ff9b52);
  transform: scaleX(1.06);
}

.carousel-status {
  color: rgba(228, 237, 255, 0.78);
  font-size: 13px;
  text-align: right;
}

@media (max-width: 1180px) {
  .champion-carousel-widget,
  .carousel-stage,
  .carousel-slide {
    min-height: 350px;
  }
}

@media (max-width: 768px) {
  .champion-carousel-widget,
  .carousel-stage,
  .carousel-slide {
    min-height: 320px;
  }

  .carousel-copy {
    right: 18px;
    bottom: 18px;
    left: 18px;
  }

  .carousel-copy h4 {
    font-size: 24px;
  }

  .carousel-copy p {
    font-size: 14px;
    line-height: 1.58;
  }

  .carousel-nav {
    width: 42px;
    height: 42px;
  }

  .carousel-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .carousel-status {
    text-align: left;
  }
}
</style>
