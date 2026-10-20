<template>
  <div class="list-page">
    <Header />

    <div class="main-content">
      <div class="page-header">
        <div class="header-content">
          <h1 class="page-title">
            <el-icon>
              <ShoppingBag />
            </el-icon>
            装备库
          </h1>
          <p class="page-subtitle">发现更适合你的乒乓球装备组合</p>
        </div>

        <div class="header-stats" v-if="pagination.total > 0">
          <span class="stat-label">共找到</span>
          <span class="stat-number">{{ pagination.total }}</span>
          <span class="stat-label">件装备</span>
        </div>
      </div>

      <el-row :gutter="24">
        <el-col :xs="24" :sm="24" :md="8" :lg="5" class="filter-column">
          <div class="filter-panel">
            <div class="filter-header">
              <span class="filter-title">
                <el-icon>
                  <Filter />
                </el-icon>
                筛选条件
              </span>

              <el-button text size="small" @click="resetFilters" v-if="hasActiveFilters">
                <el-icon>
                  <Refresh />
                </el-icon>
                重置
              </el-button>
            </div>

            <el-divider />

            <div class="filter-section">
              <div class="section-title">
                <el-icon>
                  <List />
                </el-icon>
                <span>分类</span>
              </div>

              <div class="category-list">
                <button
                  type="button"
                  class="category-option"
                  :class="{ 'is-active': !filters.category }"
                  @click="handleCategorySelect('')"
                >
                  <span class="option-icon">
                    <el-icon>
                      <Grid />
                    </el-icon>
                  </span>
                  <span class="option-label">全部分类</span>
                </button>

                <button
                  v-for="item in categories"
                  :key="item.id"
                  type="button"
                  class="category-option"
                  :class="{ 'is-active': filters.category === item.id.toString() }"
                  @click="handleCategorySelect(item.id.toString())"
                >
                  <span class="option-icon">
                    <el-icon>
                      <component :is="getCategoryIcon(item.name)" />
                    </el-icon>
                  </span>
                  <span class="option-label">{{ item.name }}</span>
                </button>
              </div>
            </div>

            <el-divider />

            <div class="filter-section">
              <div class="section-title">
                <el-icon>
                  <Money />
                </el-icon>
                <span>价格区间</span>
              </div>

              <div class="price-inputs">
                <el-input v-model="filters.minPrice" placeholder="最低价" type="number" class="price-input" clearable>
                  <template #prefix>
                    <span class="currency-symbol">￥</span>
                  </template>
                </el-input>
                <span class="price-separator">-</span>
                <el-input v-model="filters.maxPrice" placeholder="最高价" type="number" class="price-input" clearable>
                  <template #prefix>
                    <span class="currency-symbol">￥</span>
                  </template>
                </el-input>
              </div>

              <el-button
                type="primary"
                size="default"
                class="price-filter-btn"
                :disabled="!filters.minPrice && !filters.maxPrice"
                @click="handlePriceFilter"
              >
                <el-icon>
                  <Check />
                </el-icon>
                确定
              </el-button>
            </div>
          </div>
        </el-col>

        <el-col :xs="24" :sm="24" :md="16" :lg="19" class="content-column">
          <div class="toolbar">
            <div class="toolbar-left">
              <div class="search-info" v-if="filters.keyword">
                <el-tag type="info" effect="plain">
                  <span class="filter-tag-content">
                    <el-icon>
                      <Search />
                    </el-icon>
                    <span>搜索“{{ filters.keyword }}”</span>
                  </span>
                </el-tag>
              </div>

              <div class="active-filters" v-if="filters.category || filters.minPrice || filters.maxPrice">
                <el-tag v-if="filters.category" type="primary" effect="plain" closable @close="clearCategory">
                  <span class="filter-tag-content">
                    <el-icon>
                      <component :is="selectedCategoryIcon" />
                    </el-icon>
                    <span>{{ selectedCategoryLabel }}</span>
                  </span>
                </el-tag>

                <el-tag
                  v-if="filters.minPrice || filters.maxPrice"
                  type="danger"
                  effect="plain"
                  closable
                  @close="clearPrice"
                >
                  <span class="filter-tag-content">
                    <el-icon>
                      <Money />
                    </el-icon>
                    <span>￥{{ filters.minPrice || 0 }} - ￥{{ filters.maxPrice || '不限' }}</span>
                  </span>
                </el-tag>
              </div>
            </div>

            <div class="toolbar-right">
              <span class="sort-label">
                <el-icon>
                  <Sort />
                </el-icon>
                排序：
              </span>

              <el-radio-group v-model="filters.sortBy" size="default" @change="loadEquipment">
                <el-radio-button label="">
                  <el-icon>
                    <Menu />
                  </el-icon>
                  默认
                </el-radio-button>
                <el-radio-button label="price_asc">
                  <el-icon>
                    <ArrowUp />
                  </el-icon>
                  价格升
                </el-radio-button>
                <el-radio-button label="price_desc">
                  <el-icon>
                    <ArrowDown />
                  </el-icon>
                  价格降
                </el-radio-button>
                <el-radio-button label="sales">
                  <el-icon>
                    <ShoppingCart />
                  </el-icon>
                  销量
                </el-radio-button>
                <el-radio-button label="rating">
                  <el-icon>
                    <Star />
                  </el-icon>
                  评分
                </el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <div class="equipment-list">
            <div v-if="loading" class="loading-state">
              <el-skeleton :rows="6" animated />
            </div>

            <el-row v-else :gutter="20">
              <el-col
                v-for="item in equipmentList"
                :key="item.id"
                :xs="24"
                :sm="12"
                :md="8"
                :lg="8"
              >
                <EquipmentCard :equipment="item" />
              </el-col>
            </el-row>

            <div v-if="!loading && equipmentList.length === 0" class="no-data">
              <el-empty description="暂无装备" :image-size="200">
                <template #image>
                  <el-icon :size="100" color="#909399">
                    <ShoppingBag />
                  </el-icon>
                </template>
                <el-button type="primary" @click="resetFilters">查看全部</el-button>
              </el-empty>
            </div>
          </div>

          <div class="pagination" v-if="!loading && equipmentList.length > 0">
            <el-pagination
              v-model:current-page="pagination.pageNum"
              v-model:page-size="pagination.pageSize"
              :total="pagination.total"
              :page-sizes="[9, 18, 27, 36]"
              layout="total, sizes, prev, pager, next, jumper"
              background
              class="pagination-custom"
              @size-change="loadEquipment"
              @current-change="loadEquipment"
            />
          </div>
        </el-col>
      </el-row>
    </div>

    <Footer />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Grid, CreditCard, CollectionTag, Goods, Handbag } from '@element-plus/icons-vue'
import { getCategoryList } from '@/api/category'
import { getEquipmentList } from '@/api/equipment'
import EquipmentCard from '@/components/EquipmentCard.vue'
import Footer from '@/components/Footer.vue'
import Header from '@/components/Header.vue'
import { isRealtimeEvent, subscribeRealtimeUpdates } from '@/utils/realtime'

const route = useRoute()
const loading = ref(true)
const categories = ref([])
const equipmentList = ref([])
let stopRealtime = null
let refreshTimer = null

const filters = reactive({
  category: '',
  keyword: '',
  minPrice: '',
  maxPrice: '',
  sortBy: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 9,
  total: 0
})

const hasActiveFilters = computed(() => Boolean(filters.category || filters.minPrice || filters.maxPrice))

const selectedCategoryName = computed(() => {
  const currentCategory = categories.value.find(item => item.id.toString() === filters.category)
  return currentCategory?.name || ''
})

const selectedCategoryLabel = computed(() => {
  return selectedCategoryName.value ? `已选分类：${selectedCategoryName.value}` : ''
})

const selectedCategoryIcon = computed(() => {
  return getCategoryIcon(selectedCategoryName.value)
})

onMounted(async () => {
  syncFiltersFromRoute()
  await loadCategories()
  await loadEquipment()
  stopRealtime = subscribeRealtimeUpdates((message) => {
    if (!isRealtimeEvent(message, ['equipment_changed'])) {
      return
    }
    scheduleRefresh()
  })
})

onUnmounted(() => {
  if (stopRealtime) {
    stopRealtime()
  }
  if (refreshTimer) {
    window.clearTimeout(refreshTimer)
  }
})

watch(
  () => route.query,
  () => {
    syncFiltersFromRoute()
    pagination.pageNum = 1
    loadEquipment()
  }
)

const syncFiltersFromRoute = () => {
  filters.category = route.query.categoryId ? String(route.query.categoryId) : ''
  filters.keyword = route.query.keyword || ''
  filters.minPrice = route.query.minPrice || ''
  filters.maxPrice = route.query.maxPrice || ''
  filters.sortBy = route.query.sortBy || ''
}

const loadCategories = async () => {
  const res = await getCategoryList({ level: 1 })
  categories.value = res.data || []
}

const loadEquipment = async () => {
  try {
    loading.value = true
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      categoryId: filters.category || undefined,
      keyword: filters.keyword || undefined,
      minPrice: filters.minPrice || undefined,
      maxPrice: filters.maxPrice || undefined,
      sortBy: filters.sortBy || undefined
    }

    const res = await getEquipmentList(params)
    equipmentList.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    console.error('加载装备列表失败:', error)
  } finally {
    loading.value = false
  }
}

const scheduleRefresh = () => {
  if (refreshTimer) {
    window.clearTimeout(refreshTimer)
  }
  refreshTimer = window.setTimeout(() => {
    loadEquipment()
  }, 250)
}

const handleCategorySelect = (categoryId) => {
  filters.category = categoryId
  pagination.pageNum = 1
  loadEquipment()
}

const handlePriceFilter = () => {
  pagination.pageNum = 1
  loadEquipment()
}

const resetFilters = () => {
  filters.category = ''
  filters.minPrice = ''
  filters.maxPrice = ''
  filters.sortBy = ''
  pagination.pageNum = 1
  loadEquipment()
}

const clearCategory = () => {
  filters.category = ''
  pagination.pageNum = 1
  loadEquipment()
}

const clearPrice = () => {
  filters.minPrice = ''
  filters.maxPrice = ''
  pagination.pageNum = 1
  loadEquipment()
}

const getCategoryIcon = (categoryName = '') => {
  const normalizedName = String(categoryName)

  if (/(底板|球板|板拍)/.test(normalizedName)) {
    return CreditCard
  }

  if (/(胶皮|套胶|反胶|长胶|生胶|正胶)/.test(normalizedName)) {
    return CollectionTag
  }

  if (/(鞋|球鞋|运动鞋)/.test(normalizedName)) {
    return Goods
  }

  if (/(服装|球衣|上衣|短裤|长裤|外套|T恤|服饰)/.test(normalizedName)) {
    return Handbag
  }

  return Grid
}
</script>

<style scoped lang="scss">
.list-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
}

.main-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 30px 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding: 40px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);

  .header-content {
    .page-title {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;
      font-size: 32px;
      font-weight: 700;
      color: #2c3e50;

      .el-icon {
        font-size: 36px;
        color: var(--primary-color);
      }
    }

    .page-subtitle {
      margin: 0;
      font-size: 16px;
      color: #5a6c7d;
    }
  }

  .header-stats {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 16px 24px;
    background: linear-gradient(135deg, #fff5f2 0%, #ffe8e0 100%);
    border-radius: 12px;

    .stat-label {
      font-size: 14px;
      color: #8b98a7;
    }

    .stat-number {
      font-size: 28px;
      font-weight: 700;
      color: var(--primary-color);
    }
  }
}

.filter-column {
  .filter-panel {
    position: sticky;
    top: 20px;
    padding: 24px;
    background: #fff;
    border-radius: 16px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);

    .filter-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 16px;

      .filter-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 18px;
        font-weight: 600;
        color: #2c3e50;

        .el-icon {
          font-size: 20px;
          color: var(--primary-color);
        }
      }
    }

    .filter-section {
      margin-bottom: 28px;

      .section-title {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 16px;
        font-size: 15px;
        font-weight: 600;
        color: #5a6c7d;

        .el-icon {
          color: var(--primary-color);
        }
      }
    }
  }
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.category-option {
  position: relative;
  isolation: isolate;
  overflow: visible;
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 14px 16px;
  border: none;
  border-radius: 16px;
  background: transparent;
  color: #3f4f5f;
  cursor: pointer;
  text-align: left;
  transition: all 0.25s ease;

  &:hover {
    background: #fff3ee;
    color: var(--primary-color);
    transform: translateX(4px);
  }

  .option-icon,
  .option-label {
    position: relative;
    z-index: 1;
  }

  .option-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    flex-shrink: 0;
    border-radius: 10px;
    background: #f5f7fa;
    color: inherit;

    .el-icon {
      font-size: 18px;
    }
  }

  .option-label {
    flex: 1;
    min-width: 0;
    position: relative;
    z-index: 2;
    font-size: 16px;
    font-weight: 600;
    line-height: 1.2;
    color: inherit;
    white-space: nowrap;
  }

  &.is-active {
    background: linear-gradient(135deg, #ff8f66 0%, #ff6b35 100%);
    color: #fff;
    box-shadow: 0 12px 24px rgba(255, 107, 53, 0.25);
  }

  &.is-active:hover {
    background: linear-gradient(135deg, #ff8c5a 0%, #ff6b35 100%);
    color: #fff;
    transform: none;
  }

  &.is-active .option-icon {
    background: rgba(255, 255, 255, 0.18);
    color: #fff;
  }

  &.is-active .option-label {
    color: #fff;
    text-shadow: 0 1px 1px rgba(0, 0, 0, 0.08);
  }
}

.price-inputs {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;

  .price-input {
    flex: 1;

    :deep(.el-input__wrapper) {
      border-radius: 8px;
    }

    .currency-symbol {
      font-weight: 600;
      color: var(--primary-color);
    }
  }

  .price-separator {
    font-size: 14px;
    color: #bcc4ce;
  }
}

.price-filter-btn {
  width: 100%;
  border-radius: 8px;
  font-weight: 500;
}

.content-column {
  .toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    margin-bottom: 24px;
    padding: 20px 24px;
    background: #fff;
    border-radius: 16px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

    .toolbar-left {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;

      .search-info {
        .el-tag {
          font-size: 14px;
        }
      }

      .active-filters {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 10px;

        .el-tag {
          font-size: 14px;
        }
      }

      .filter-tag-content {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        line-height: 1;
        white-space: nowrap;

        .el-icon {
          flex-shrink: 0;
        }
      }

      :deep(.el-tag__content) {
        display: inline-flex;
        align-items: center;
      }
    }

    .toolbar-right {
      display: flex;
      align-items: center;
      gap: 12px;

      .sort-label {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 14px;
        font-weight: 500;
        color: #5a6c7d;

        .el-icon {
          color: var(--primary-color);
        }
      }

      .el-radio-group {
        .el-radio-button {
          margin-left: 8px;

          .el-radio-button__inner {
            border-radius: 8px;
            font-weight: 500;
            transition: all 0.3s ease;

            &:hover {
              transform: translateY(-2px);
            }
          }

          &.is-active {
            .el-radio-button__inner {
              background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
              border-color: var(--primary-color);
              box-shadow: 0 4px 12px rgba(255, 107, 53, 0.3);
            }
          }
        }
      }
    }
  }

  .equipment-list {
    min-height: 600px;
    padding: 24px;
    background: #fff;
    border-radius: 16px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);

    .loading-state {
      padding: 20px;
    }

    .no-data {
      padding: 80px 20px;
      text-align: center;
    }
  }

  .pagination {
    display: flex;
    justify-content: center;
    margin-top: 30px;

    .pagination-custom {
      :deep(.el-pagination__total) {
        font-size: 14px;
        color: #8b98a7;
      }

      :deep(.el-pagination__sizes) {
        .el-select {
          .el-input__wrapper {
            border-radius: 8px;
          }
        }
      }

      :deep(.el-pager) {
        li {
          min-width: 36px;
          height: 36px;
          line-height: 36px;
          margin: 0 4px;
          border-radius: 8px;
          font-weight: 500;

          &.is-active {
            background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
          }

          &:hover:not(.is-active) {
            background: #f5f7fa;
          }
        }
      }

      :deep(.btn-prev),
      :deep(.btn-next) {
        min-width: 36px;
        height: 36px;
        border-radius: 8px;

        &:hover {
          color: var(--primary-color);
        }
      }
    }
  }
}

@media (max-width: 1200px) {
  .filter-column {
    .filter-panel {
      position: static;
    }
  }
}

@media (max-width: 768px) {
  .main-content {
    padding: 20px 16px;
  }

  .filter-column {
    margin-bottom: 16px;

    .filter-panel {
      position: static;
    }
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;

    .header-stats {
      align-self: flex-start;
    }
  }

  .category-option {
    padding: 12px 14px;

    .option-label {
      font-size: 15px;
    }
  }

  .price-inputs {
    flex-direction: column;
    align-items: stretch;

    .price-separator {
      align-self: center;
    }
  }

  .toolbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;

    .toolbar-right {
      flex-wrap: wrap;
    }
  }

  .equipment-list {
    padding: 16px;
  }
}
</style>
