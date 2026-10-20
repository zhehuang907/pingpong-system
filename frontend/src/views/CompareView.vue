<template>
  <div class="compare-page">
    <Header />
    <div class="main-content">
      <div class="page-header">
        <h1>
          <el-icon><Rank /></el-icon>
          装备对比
        </h1>
        <p class="subtitle">横向对比最多 3 款装备，找到最适合你的选择</p>
      </div>

      <!-- 空状态 -->
      <div v-if="compareItems.length === 0" class="empty-state">
        <el-empty description="还没有添加对比装备">
          <el-button type="primary" @click="$router.push('/equipment-list')">
            去选装备
          </el-button>
        </el-empty>
      </div>

      <!-- 对比表格 -->
      <div v-else class="compare-table-wrapper">
        <table class="compare-table">
          <thead>
            <tr>
              <th class="label-col">对比项</th>
              <th v-for="item in compareItems" :key="item.id" class="item-col">
                <div class="item-header">
                  <el-image :src="getEquipmentImageUrl(item)" fit="cover" class="item-image">
                    <template #error>
                      <div class="img-fallback">暂无图片</div>
                    </template>
                  </el-image>
                  <h3 class="item-name">{{ item.name }}</h3>
                  <el-button type="danger" size="small" circle @click="removeItem(item.id)">
                    <el-icon><Close /></el-icon>
                  </el-button>
                </div>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="label-cell">品牌</td>
              <td v-for="item in compareItems" :key="item.id">{{ item.brand || '-' }}</td>
            </tr>
            <tr>
              <td class="label-cell">型号</td>
              <td v-for="item in compareItems" :key="item.id">{{ item.model || '-' }}</td>
            </tr>
            <tr>
              <td class="label-cell">价格</td>
              <td v-for="item in compareItems" :key="item.id" class="price-cell">
                <span class="price">¥{{ item.price }}</span>
                <span v-if="item.originalPrice" class="original">¥{{ item.originalPrice }}</span>
              </td>
            </tr>
            <tr>
              <td class="label-cell">评分</td>
              <td v-for="item in compareItems" :key="item.id">
                <el-rate :model-value="Number(item.rating) || 0" disabled show-score />
              </td>
            </tr>
            <tr>
              <td class="label-cell">适合人群</td>
              <td v-for="item in compareItems" :key="item.id">{{ item.suitableFor || '-' }}</td>
            </tr>
            <tr>
              <td class="label-cell">产品特点</td>
              <td v-for="item in compareItems" :key="item.id">
                <div class="features-cell" v-if="item">
                  <span v-for="(feat, fIdx) in (item.features||'').split(',')" :key="fIdx">
                    <el-tag v-if="feat && feat.trim()" size="small" effect="plain" round class="feat-tag">
                      {{ feat.trim() }}
                    </el-tag>
                  </span>
                  <span v-if="!item.features">-</span>
                </div>
              </td>
            </tr>
            <tr>
              <td class="label-cell">库存</td>
              <td v-for="item in compareItems" :key="item.id">
                <el-tag :type="(item.stock || 0) > 0 ? 'success' : 'danger'" size="small">
                  {{ (item.stock || 0) > 0 ? '有货 (' + item.stock + ')' : '缺货' }}
                </el-tag>
              </td>
            </tr>
            <tr>
              <td class="label-cell">销量</td>
              <td v-for="item in compareItems" :key="item.id">{{ item.salesCount || 0 }}</td>
            </tr>
            <tr>
              <td class="label-cell">浏览量</td>
              <td v-for="item in compareItems" :key="item.id">{{ item.viewCount || 0 }}</td>
            </tr>
            <tr>
              <td class="label-cell">操作</td>
              <td v-for="item in compareItems" :key="item.id">
                <el-button type="primary" size="small" @click="$router.push('/equipment/' + item.id)">
                  查看详情
                </el-button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 添加更多 -->
      <div v-if="compareItems.length > 0 && compareItems.length < 3" class="add-more">
        <el-button type="primary" plain @click="$router.push('/equipment-list')">
          <el-icon><Plus /></el-icon>
          添加更多装备对比
        </el-button>
      </div>
    </div>
    <Footer />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getEquipmentDetail } from '@/api/equipment'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'

const route = useRoute()
const compareItems = ref([])

function getEquipmentImageUrl(equipment) {
  // 优先使用 mainImage，其次使用 imageUrl，最后使用占位图
  return equipment?.mainImage || equipment?.imageUrl || '/placeholder.png'
}

onMounted(() => { loadFromQuery() })
watch(() => route.query, () => { loadFromQuery() })

const loadFromQuery = async () => {
  const idsParam = route.query.ids
  if (!idsParam) { compareItems.value = []; return }
  const ids = idsParam.split(',').map(Number).filter(n => n > 0).slice(0, 3)
  const results = []
  for (const id of ids) {
    try {
      const res = await getEquipmentDetail(id)
      if (res && res.data) results.push(res.data)
    } catch (e) { /* skip invalid */ }
  }
  compareItems.value = results
}

const removeItem = (id) => {
  compareItems.value = compareItems.value.filter(i => i.id !== id)
}
</script>

<style scoped lang="scss">
.compare-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px 20px 60px;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;

  h1 {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    font-size: 28px;
    color: #2c3e50;
    .el-icon { color: #ff6b35; }
  }

  .subtitle {
    color: #8b98a7;
    margin-top: 8px;
  }
}

.compare-table-wrapper {
  overflow-x: auto;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  padding: 24px;
}

.compare-table {
  width: 100%;
  border-collapse: collapse;

  th, td {
    padding: 16px 20px;
    text-align: center;
    border-bottom: 1px solid #f0f2f5;
    vertical-align: middle;
  }

  .label-col { width: 120px; }
  .label-cell {
    font-weight: 600;
    color: #5a6c7d;
    text-align: left;
    background: #f8f9fa;
    border-radius: 8px 0 0 8px;
  }

  .item-header {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    position: relative;

    .item-image {
      width: 160px;
      height: 160px;
      border-radius: 12px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.1);
    }

    .item-name {
      font-size: 15px;
      font-weight: 600;
      color: #2c3e50;
      max-width: 200px;
      text-align: center;
    }

    .el-button { position: absolute; top: -8px; right: -8px; }
  }

  .price-cell {
    .price { color: #ff6b35; font-weight: 700; font-size: 18px; }
    .original { color: #999; text-decoration: line-through; font-size: 13px; margin-left: 6px; }
  }

  .features-cell {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    justify-content: center;
    .feat-tag { font-size: 12px; }
  }

  .img-fallback {
    width: 160px;
    height: 160px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f5f7fa;
    color: #bbb;
    border-radius: 12px;
  }
}

.add-more {
  text-align: center;
  margin-top: 30px;
}

.empty-state {
  background: white;
  border-radius: 16px;
  padding: 80px 20px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
}
</style>
