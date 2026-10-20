<template>
  <el-card class="equipment-card" :body-style="{ padding: '0px' }" shadow="hover" @click="goToDetail">
    <div class="image-box">
      <el-image :src="getImageUrl(equipment)" fit="cover" class="equipment-image">
        <template #error>
          <img src="/placeholder.png" class="equipment-image placeholder-image" style="object-fit: cover; width: 100%; height: 100%;" />
        </template>
      </el-image>

      <!-- 价格标签 -->
      <div class="price-tag" v-if="equipment.price">
        <span class="currency">¥</span>
        <span class="price-value">{{ equipment.price }}</span>
        <span class="original-price" v-if="equipment.originalPrice && equipment.originalPrice > equipment.price">
          ¥{{ equipment.originalPrice }}
        </span>
      </div>

      <!-- 折扣标签 -->
      <div class="discount-tag" v-if="equipment.originalPrice && equipment.originalPrice > equipment.price">
        {{ Math.round((1 - equipment.price / equipment.originalPrice) * 100) }}% OFF
      </div>

      <!-- 收藏按钮 -->
      <button class="favorite-btn" @click.stop="toggleFavorite">
        <el-icon>
          <Star v-if="isFavorite" />
          <StarFilled v-else />
        </el-icon>
      </button>
    </div>

    <div class="content">
      <h3 class="title">{{ equipment.name }}</h3>
      <p class="brand" v-if="equipment.brand">
        <el-tag size="small" type="info" effect="plain">{{ equipment.brand }}</el-tag>
        <span class="model">{{ equipment.model }}</span>
      </p>

      <!-- 评分 -->
      <div class="rating" v-if="equipment.rating">
        <el-rate :model-value="equipment.rating" disabled size="small" />
        <span class="rating-text">{{ equipment.rating }}分</span>
      </div>

      <div v-if="equipment.recommendReason || equipment.recommendTags?.length" class="recommend-panel">
        <div class="recommend-topline">
          <span class="recommend-channel">{{ equipment.recommendChannel || '推荐说明' }}</span>
        </div>
        <p v-if="equipment.recommendReason" class="recommend-text">{{ equipment.recommendReason }}</p>
        <div v-if="equipment.recommendTags?.length" class="recommend-tags">
          <span v-for="tag in equipment.recommendTags" :key="tag">{{ tag }}</span>
        </div>
      </div>

      <!-- 统计数据 -->
      <div class="stats">
        <span class="stat-item" :title="`${equipment.viewCount || 0}次浏览`">
          <el-icon>
            <View />
          </el-icon>
          <span>{{ formatNumber(equipment.viewCount || 0) }}</span>
        </span>
        <span class="stat-item" :title="`${equipment.favoriteCount || 0}次收藏`">
          <el-icon>
            <Star />
          </el-icon>
          <span>{{ formatNumber(equipment.favoriteCount || 0) }}</span>
        </span>
        <span class="stat-item" v-if="equipment.sales" :title="`${equipment.sales}件销量`">
          <el-icon>
            <ShoppingCart />
          </el-icon>
          <span>{{ formatNumber(equipment.sales || 0) }}</span>
        </span>
      </div>

      <!-- 库存状态 -->
      <div class="stock-status" v-if="equipment.stock !== undefined">
        <el-tag :type="equipment.stock > 0 ? 'success' : 'danger'" size="small">
          {{ equipment.stock > 0 ? '有货' : '缺货' }}
        </el-tag>
        <span class="stock-count" v-if="equipment.stock > 0 && equipment.stock < 10">
          仅剩{{ equipment.stock }}件
        </span>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture, View, Star, StarFilled, ShoppingCart } from '@element-plus/icons-vue'
import { favoriteApi } from '@/api/favorite'
import { getToken } from '@/utils/auth'

const router = useRouter()
const props = defineProps({
  equipment: {
    type: Object,
    required: true
  }
})

const isFavorite = ref(false)

onMounted(() => {
  checkFavorite()
})

const checkFavorite = async () => {
  const token = getToken()
  if (!token) return

  try {
    const res = await favoriteApi.check(props.equipment.id)
    isFavorite.value = res.data
  } catch (error) {
    // 忽略错误
  }
}

const toggleFavorite = async () => {
  const token = getToken()
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    await favoriteApi.toggle(props.equipment.id)
    isFavorite.value = !isFavorite.value
    ElMessage.success(isFavorite.value ? '收藏成功' : '取消收藏')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const formatNumber = (num) => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  return num.toString()
}

const getImageUrl = (equipment) => {
  // 优先使用 mainImage，其次使用 imageUrl，最后使用占位图
  return equipment.mainImage || equipment.imageUrl || '/placeholder.png'
}

const goToDetail = () => {
  router.push(`/equipment/${props.equipment.id}`)
}
</script>

<style scoped lang="scss">
.equipment-card {
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  background: white;

  &:hover {
    transform: translateY(-8px);
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);

    .equipment-image {
      transform: scale(1.08);
    }

    .favorite-btn {
      opacity: 1;
      transform: translateX(0);
    }
  }
}

.image-box {
  position: relative;
  width: 100%;
  height: 240px;
  overflow: hidden;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.equipment-image {
  width: 100%;
  height: 100%;
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.image-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  color: #909399;
  font-size: 14px;

  .el-icon {
    font-size: 48px;
    margin-bottom: 8px;
  }
}

.price-tag {
  position: absolute;
  bottom: 12px;
  left: 12px;
  background: linear-gradient(135deg, #ff6b35, #ff8c5a);
  color: white;
  padding: 6px 14px;
  border-radius: 20px;
  font-weight: 700;
  display: flex;
  align-items: baseline;
  gap: 4px;
  box-shadow: 0 4px 12px rgba(255, 107, 53, 0.4);
  backdrop-filter: blur(10px);

  .currency {
    font-size: 14px;
  }

  .price-value {
    font-size: 20px;
  }

  .original-price {
    font-size: 12px;
    opacity: 0.8;
    text-decoration: line-through;
    margin-left: 4px;
  }
}

.discount-tag {
  position: absolute;
  top: 12px;
  left: 12px;
  background: linear-gradient(135deg, #f5222d, #ff4d4f);
  color: white;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(245, 34, 45, 0.4);
}

.favorite-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 36px;
  height: 36px;
  border: none;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transform: translateX(10px);
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);

  .el-icon {
    font-size: 18px;
    color: #ff6b35;
  }

  &:hover {
    background: white;
    transform: scale(1.1);
  }
}

.content {
  padding: 16px;
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 8px 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  min-height: 44px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 13px;
  color: #5a6c7d;

  .model {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;

  .rating-text {
    font-size: 12px;
    color: #faad14;
    font-weight: 600;
  }
}

.recommend-panel {
  margin-bottom: 12px;
  padding: 12px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(255, 122, 39, 0.08), rgba(0, 166, 255, 0.08));
}

.recommend-topline {
  display: flex;
  align-items: center;
  gap: 8px;
}

.recommend-channel {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.84);
  color: #21498e;
  font-size: 12px;
  font-weight: 700;
}

.recommend-text {
  margin: 10px 0 0;
  color: #42526f;
  font-size: 13px;
  line-height: 1.7;
}

.recommend-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.recommend-tags span {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #21498e;
  font-size: 12px;
  font-weight: 700;
}

.stats {
  display: flex;
  gap: 16px;
  padding-top: 10px;
  border-top: 1px solid #f0f2f5;

  .stat-item {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #8b98a7;

    .el-icon {
      font-size: 14px;
    }
  }
}

.stock-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;

  .stock-count {
    font-size: 12px;
    color: #f5222d;
    font-weight: 500;
  }
}
</style>
