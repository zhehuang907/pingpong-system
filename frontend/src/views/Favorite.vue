<template>
  <div class="favorite-page">
    <Header />
    
    <div class="main-content">
      <h2>我的收藏</h2>
      
      <div class="favorite-list">
        <el-row :gutter="20" v-if="favorites.length > 0">
          <el-col :span="6" v-for="item in favorites" :key="item.id">
            <EquipmentCard :equipment="item.equipment" />
          </el-col>
        </el-row>
        
        <el-empty v-else description="暂无收藏" />
      </div>
    </div>
    
    <Footer />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFavoriteList } from '@/api/favorite'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import EquipmentCard from '@/components/EquipmentCard.vue'

const favorites = ref([])

onMounted(async () => {
  try {
    const res = await getFavoriteList()
    favorites.value = (res.data || []).filter(item => item.equipment)
  } catch (error) {
    console.error('加载收藏失败:', error)
  }
})
</script>

<style scoped lang="scss">
.favorite-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.main-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;

  h2 {
    margin-bottom: 20px;
    color: #333;
  }

  .favorite-list {
    background: white;
    padding: 20px;
    border-radius: 8px;
    min-height: 500px;
  }
}
</style>
