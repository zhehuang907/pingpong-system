<template>
  <div class="overview-page" v-loading="loading">
    <div class="hero-card">
      <div>
        <div class="hero-tag">运营总览</div>
        <h3>内容与装备运营驾驶舱</h3>
        <p>聚合用户增长、访问趋势、评论审核和热门装备表现，帮助管理员快速把握平台运行状态。</p>
      </div>
      <div class="hero-actions">
        <el-button plain @click="handleExport">导出报表</el-button>
        <el-button type="primary" @click="loadOverview">刷新数据</el-button>
      </div>
    </div>

    <div class="stats-grid">
      <div v-for="item in summaryCards" :key="item.key" class="stat-card">
        <div class="stat-label">{{ item.label }}</div>
        <div class="stat-value">{{ item.value }}</div>
        <div class="stat-tip">{{ item.tip }}</div>
      </div>
    </div>

    <div class="chart-grid">
      <el-card class="chart-card">
        <template #header>
          <div class="section-title">用户增长趋势</div>
        </template>
        <div ref="userGrowthChartRef" class="chart-box"></div>
      </el-card>

      <el-card class="chart-card">
        <template #header>
          <div class="section-title">访问趋势</div>
        </template>
        <div ref="accessTrendChartRef" class="chart-box"></div>
      </el-card>
    </div>

    <div class="chart-grid">
      <el-card class="chart-card">
        <template #header>
          <div class="section-title">用户打法分布</div>
        </template>
        <div ref="playStyleChartRef" class="chart-box"></div>
      </el-card>

      <el-card class="chart-card">
        <template #header>
          <div class="section-title">热门装备 Top10</div>
        </template>
        <el-table :data="overview.topEquipments" stripe>
          <el-table-column prop="rank" label="排名" width="72" />
          <el-table-column label="装备" min-width="220">
            <template #default="{ row }">
              <div class="equipment-name">
                <span>{{ row.name }}</span>
                <small>{{ row.brand || '未标注品牌' }}</small>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="heatScore" label="热度分" width="100" />
          <el-table-column prop="favoriteCount" label="收藏量" width="100" />
          <el-table-column prop="viewCount" label="访问量" width="100" />
          <el-table-column prop="rating" label="评分" width="90" />
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { exportAdminDashboardOverview, getAdminDashboardOverview } from '@/api/admin'

const loading = ref(false)
const userGrowthChartRef = ref(null)
const accessTrendChartRef = ref(null)
const playStyleChartRef = ref(null)

const overview = reactive({
  summary: {},
  userGrowthTrend: [],
  accessTrend: [],
  playStyleDistribution: [],
  topEquipments: []
})

const chartInstances = {
  userGrowth: null,
  accessTrend: null,
  playStyle: null
}

const summaryCards = computed(() => [
  {
    key: 'totalUsers',
    label: '用户总量',
    value: overview.summary.totalUsers || 0,
    tip: `近 7 天活跃 ${overview.summary.activeUsers7d || 0}`
  },
  {
    key: 'totalComments',
    label: '评论总量',
    value: overview.summary.totalComments || 0,
    tip: `待审核 ${overview.summary.pendingComments || 0}`
  },
  {
    key: 'approvedComments',
    label: '已通过评论',
    value: overview.summary.approvedComments || 0,
    tip: `驳回 ${overview.summary.rejectedComments || 0}`
  },
  {
    key: 'totalEquipments',
    label: '装备数量',
    value: overview.summary.totalEquipments || 0,
    tip: `上架中 ${overview.summary.onlineEquipments || 0}`
  },
  {
    key: 'totalFavorites',
    label: '累计收藏',
    value: overview.summary.totalFavorites || 0,
    tip: '反映装备关注热度'
  },
  {
    key: 'totalViews',
    label: '累计访问',
    value: overview.summary.totalViews || 0,
    tip: '用于衡量内容曝光'
  }
])

const loadOverview = async () => {
  loading.value = true
  try {
    const res = await getAdminDashboardOverview()
    Object.assign(overview, {
      summary: res.data?.summary || {},
      userGrowthTrend: res.data?.userGrowthTrend || [],
      accessTrend: res.data?.accessTrend || [],
      playStyleDistribution: res.data?.playStyleDistribution || [],
      topEquipments: res.data?.topEquipments || []
    })
    await nextTick()
    renderCharts()
  } catch (error) {
    ElMessage.error(error?.message || '加载看板数据失败')
  } finally {
    loading.value = false
  }
}


const handleExport = async () => {
  try {
    const response = await exportAdminDashboardOverview()
    const blob = new Blob([response], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `admin-dashboard-overview-${Date.now()}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('报表导出成功')
  } catch (error) {
    ElMessage.error(error?.message || '报表导出失败')
  }
}
const renderCharts = () => {
  renderUserGrowthChart()
  renderAccessTrendChart()
  renderPlayStyleChart()
}

const renderUserGrowthChart = () => {
  if (!userGrowthChartRef.value) return
  chartInstances.userGrowth ||= echarts.init(userGrowthChartRef.value)

  chartInstances.userGrowth.setOption({
    color: ['#ff8e3d'],
    grid: { left: 36, right: 16, top: 24, bottom: 32 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: overview.userGrowthTrend.map((item) => item.date),
      axisLine: { lineStyle: { color: '#c8d6ce' } }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#edf1ee' } }
    },
    series: [
      {
        type: 'line',
        smooth: true,
        areaStyle: { color: 'rgba(255, 142, 61, 0.16)' },
        lineStyle: { width: 3 },
        symbolSize: 8,
        data: overview.userGrowthTrend.map((item) => item.value)
      }
    ]
  })
}

const renderAccessTrendChart = () => {
  if (!accessTrendChartRef.value) return
  chartInstances.accessTrend ||= echarts.init(accessTrendChartRef.value)

  chartInstances.accessTrend.setOption({
    color: ['#0f8b5f', '#ff8e3d'],
    grid: { left: 40, right: 16, top: 24, bottom: 32 },
    tooltip: { trigger: 'axis' },
    legend: { top: 0, textStyle: { color: '#567063' } },
    xAxis: {
      type: 'category',
      data: overview.accessTrend.map((item) => item.date),
      axisLine: { lineStyle: { color: '#c8d6ce' } }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#edf1ee' } }
    },
    series: [
      {
        name: 'PV',
        type: 'line',
        smooth: true,
        data: overview.accessTrend.map((item) => item.pv)
      },
      {
        name: 'UV',
        type: 'line',
        smooth: true,
        data: overview.accessTrend.map((item) => item.uv)
      }
    ]
  })
}

const renderPlayStyleChart = () => {
  if (!playStyleChartRef.value) return
  chartInstances.playStyle ||= echarts.init(playStyleChartRef.value)

  chartInstances.playStyle.setOption({
    color: ['#0f8b5f', '#ff8e3d', '#f3c969', '#51825c', '#7fb685'],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#567063' } },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '45%'],
        label: { formatter: '{b}\n{d}%' },
        data: overview.playStyleDistribution
      }
    ]
  })
}

const handleResize = () => {
  Object.values(chartInstances).forEach((chart) => chart?.resize())
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  await loadOverview()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  Object.values(chartInstances).forEach((chart) => chart?.dispose())
})
</script>

<style scoped lang="scss">
.overview-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 24px 28px;
  border-radius: 24px;
  background:
    linear-gradient(135deg, rgba(15, 81, 50, 0.96) 0%, rgba(22, 98, 61, 0.92) 60%, rgba(255, 142, 61, 0.82) 100%);
  box-shadow: 0 20px 48px rgba(26, 74, 53, 0.16);
  color: #fff;

  h3 {
    margin: 10px 0 8px;
    font-size: 30px;
  }

  p {
    max-width: 680px;
    margin: 0;
    line-height: 1.8;
    color: rgba(255, 247, 235, 0.88);
  }
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.hero-tag {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 12px;
  letter-spacing: 0.08em;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  padding: 20px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 16px 32px rgba(15, 81, 50, 0.08);
}

.stat-label {
  color: #6c7d73;
  font-size: 13px;
}

.stat-value {
  margin: 14px 0 8px;
  color: #173326;
  font-size: 32px;
  font-weight: 700;
}

.stat-tip {
  color: #8b998f;
  font-size: 13px;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.chart-card {
  border: none;
  border-radius: 24px;
  box-shadow: 0 16px 32px rgba(15, 81, 50, 0.08);
}

.section-title {
  color: #173326;
  font-size: 18px;
  font-weight: 700;
}

.chart-box {
  height: 320px;
}

.equipment-name {
  display: flex;
  flex-direction: column;
  gap: 4px;

  span {
    color: #173326;
    font-weight: 600;
  }

  small {
    color: #7f8f86;
  }
}

@media (max-width: 1280px) {
  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .hero-card,
  .chart-grid {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 720px) {
  .stats-grid,
  .chart-grid {
    grid-template-columns: 1fr;
  }

  .hero-card {
    padding: 20px;

    h3 {
      font-size: 24px;
    }
  }

  .chart-box {
    height: 280px;
  }
}
</style>



