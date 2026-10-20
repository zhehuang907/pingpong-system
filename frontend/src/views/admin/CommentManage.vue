<template>
  <div class="comment-manage">
    <div class="summary-grid">
      <div v-for="item in summaryCards" :key="item.key" class="summary-card">
        <div class="summary-label">{{ item.label }}</div>
        <div class="summary-value">{{ item.value }}</div>
      </div>
    </div>

    <el-card class="filter-card">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="评论内容" clearable />
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="queryForm.reviewStatus" placeholder="全部状态" clearable>
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已驳回" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="评论状态">
          <el-select v-model="queryForm.status" placeholder="全部状态" clearable>
            <el-option label="正常" :value="1" />
            <el-option label="已删除" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button type="danger" plain :disabled="selectedIds.length === 0" @click="handleBatchDelete">
            批量删除
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table
        :data="tableData"
        stripe
        border
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="52" />
        <el-table-column prop="id" label="评论ID" width="88" />
        <el-table-column label="用户" min-width="150">
          <template #default="{ row }">
            <div class="user-cell">
              <span>{{ row.user?.nickname || row.user?.username || `用户#${row.userId}` }}</span>
              <small>ID：{{ row.userId }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="equipmentName" label="关联装备" min-width="180" show-overflow-tooltip />
        <el-table-column label="评论内容" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.content }}
          </template>
        </el-table-column>
        <el-table-column prop="rating" label="评分" width="80" />
        <el-table-column prop="likeCount" label="点赞" width="80" />
        <el-table-column prop="replyCount" label="回复" width="80" />
        <el-table-column label="审核状态" width="110">
          <template #default="{ row }">
            <el-tag :type="reviewStatusMap[row.reviewStatus]?.type || 'info'">
              {{ reviewStatusMap[row.reviewStatus]?.label || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评论状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '正常' : '已删除' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="评论时间" width="168" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
            <el-button
              link
              type="success"
              :disabled="row.reviewStatus === 1 || row.status !== 1"
              @click="openReviewDialog(row, 1)"
            >
              通过
            </el-button>
            <el-button
              link
              type="warning"
              :disabled="row.reviewStatus === 2 || row.status !== 1"
              @click="openReviewDialog(row, 2)"
            >
              驳回
            </el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" title="评论详情" size="720px">
      <div v-if="detailData" class="detail-panel">
        <div class="detail-section">
          <h4>基础信息</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="评论ID">{{ detailData.id }}</el-descriptions-item>
            <el-descriptions-item label="用户">
              {{ detailData.user?.nickname || detailData.user?.username || `用户#${detailData.userId}` }}
            </el-descriptions-item>
            <el-descriptions-item label="关联装备">{{ detailData.equipmentName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="评分">{{ detailData.rating || '-' }}</el-descriptions-item>
            <el-descriptions-item label="评论时间">{{ detailData.createTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="审核时间">{{ detailData.reviewedAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="审核人">{{ detailData.reviewerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="审核状态">
              {{ reviewStatusMap[detailData.reviewStatus]?.label || '未知' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="detail-section">
          <h4>评论内容</h4>
          <div class="detail-content">{{ detailData.content || '-' }}</div>
        </div>

        <div class="detail-section" v-if="detailImages.length">
          <h4>晒单图片</h4>
          <div class="image-grid">
            <el-image
              v-for="image in detailImages"
              :key="image"
              :src="image"
              fit="cover"
              :preview-src-list="detailImages"
              class="detail-image"
            />
          </div>
        </div>

        <div class="detail-section">
          <h4>互动数据</h4>
          <div class="metric-row">
            <span>点赞 {{ detailData.likeCount || 0 }}</span>
            <span>回复 {{ detailData.replyCount || 0 }}</span>
            <span>状态 {{ detailData.status === 1 ? '正常' : '已删除' }}</span>
          </div>
          <div v-if="detailData.reviewRemark" class="review-remark">
            审核备注：{{ detailData.reviewRemark }}
          </div>
        </div>

        <div class="detail-section">
          <h4>相关回复</h4>
          <el-empty v-if="!detailReplies.length" description="暂无回复" />
          <el-timeline v-else>
            <el-timeline-item
              v-for="reply in detailReplies"
              :key="reply.id"
              :timestamp="reply.createTime"
              placement="top"
            >
              <div class="reply-item">
                <div class="reply-user">{{ reply.user?.nickname || reply.user?.username || `用户#${reply.userId}` }}</div>
                <div class="reply-content">{{ reply.content }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="reviewDialogVisible" :title="reviewForm.reviewStatus === 1 ? '通过评论' : '驳回评论'" width="480px">
      <el-form label-width="92px">
        <el-form-item label="审核结果">
          <el-tag :type="reviewForm.reviewStatus === 1 ? 'success' : 'warning'">
            {{ reviewForm.reviewStatus === 1 ? '通过' : '驳回' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input
            v-model="reviewForm.reviewRemark"
            type="textarea"
            :rows="4"
            :placeholder="reviewForm.reviewStatus === 1 ? '可选填写通过理由' : '建议填写驳回原因'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  batchDeleteAdminComments,
  deleteAdminComment,
  getAdminCommentDetail,
  getAdminCommentList,
  getAdminCommentStatistics,
  reviewAdminComment
} from '@/api/admin'

const reviewStatusMap = {
  0: { label: '待审核', type: 'warning' },
  1: { label: '已通过', type: 'success' },
  2: { label: '已驳回', type: 'danger' }
}

const queryForm = reactive({
  keyword: '',
  reviewStatus: null,
  status: 1
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const statistics = reactive({
  total: 0,
  pending: 0,
  approved: 0,
  rejected: 0
})

const tableData = ref([])
const selectedIds = ref([])
const detailVisible = ref(false)
const detailData = ref(null)
const reviewDialogVisible = ref(false)
const reviewForm = reactive({
  commentId: null,
  reviewStatus: 1,
  reviewRemark: ''
})

const summaryCards = computed(() => [
  { key: 'total', label: '评论总量', value: statistics.total },
  { key: 'pending', label: '待审核', value: statistics.pending },
  { key: 'approved', label: '已通过', value: statistics.approved },
  { key: 'rejected', label: '已驳回', value: statistics.rejected }
])

const detailImages = computed(() => normalizeImages(detailData.value?.images))
const detailReplies = computed(() => detailData.value?.replies || [])

const loadStatistics = async () => {
  const res = await getAdminCommentStatistics()
  Object.assign(statistics, res.data || {})
}

const loadData = async () => {
  const res = await getAdminCommentList({
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize,
    keyword: queryForm.keyword || undefined,
    reviewStatus: queryForm.reviewStatus,
    status: queryForm.status
  })
  tableData.value = res.data?.records || []
  pagination.total = res.data?.total || 0
}

const handleSearch = async () => {
  try {
    pagination.pageNum = 1
    await loadData()
  } catch (error) {
    ElMessage.error(error?.message || '评论列表加载失败')
  }
}

const resetQuery = async () => {
  try {
    queryForm.keyword = ''
    queryForm.reviewStatus = null
    queryForm.status = 1
    pagination.pageNum = 1
    await loadData()
  } catch (error) {
    ElMessage.error(error?.message || '评论列表加载失败')
  }
}

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map((row) => row.id)
}

const handlePageChange = async () => {
  try {
    await loadData()
  } catch (error) {
    ElMessage.error(error?.message || '评论列表加载失败')
  }
}

const handlePageSizeChange = async () => {
  try {
    pagination.pageNum = 1
    await loadData()
  } catch (error) {
    ElMessage.error(error?.message || '评论列表加载失败')
  }
}

const openDetail = async (commentId) => {
  try {
    const res = await getAdminCommentDetail(commentId)
    detailData.value = res.data
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error?.message || '评论详情加载失败')
  }
}

const openReviewDialog = (row, reviewStatus) => {
  reviewForm.commentId = row.id
  reviewForm.reviewStatus = reviewStatus
  reviewForm.reviewRemark = reviewStatus === 1 ? '内容真实，允许展示' : ''
  reviewDialogVisible.value = true
}

const submitReview = async () => {
  try {
    await reviewAdminComment({
      commentId: reviewForm.commentId,
      reviewStatus: reviewForm.reviewStatus,
      reviewRemark: reviewForm.reviewRemark
    })
    ElMessage.success('审核结果已更新')
    reviewDialogVisible.value = false
    await Promise.all([loadStatistics(), loadData()])
  } catch (error) {
    ElMessage.error(error?.message || '审核操作失败')
  }
}

const handleDelete = async (commentId) => {
  try {
    await ElMessageBox.confirm('删除后该评论将不再对外展示，是否继续？', '删除确认', {
      type: 'warning'
    })
    await deleteAdminComment(commentId)
    ElMessage.success('评论已删除')
    await Promise.all([loadStatistics(), loadData()])
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '删除评论失败')
    }
  }
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定删除已选中的 ${selectedIds.value.length} 条评论吗？`, '批量删除确认', {
      type: 'warning'
    })
    await batchDeleteAdminComments(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    await Promise.all([loadStatistics(), loadData()])
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '批量删除失败')
    }
  }
}

const normalizeImages = (rawImages) => {
  if (!rawImages) {
    return []
  }

  if (Array.isArray(rawImages)) {
    return rawImages.filter(Boolean)
  }

  if (typeof rawImages === 'string') {
    try {
      const parsed = JSON.parse(rawImages)
      return Array.isArray(parsed) ? parsed.filter(Boolean) : []
    } catch (error) {
      return rawImages.split(',').map((item) => item.trim()).filter(Boolean)
    }
  }

  return []
}

onMounted(async () => {
  try {
    await Promise.all([loadStatistics(), loadData()])
  } catch (error) {
    ElMessage.error(error?.message || '评论管理数据加载失败')
  }
})
</script>

<style scoped lang="scss">
.comment-manage {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  padding: 20px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 14px 28px rgba(15, 81, 50, 0.08);
}

.summary-label {
  color: #6e7f75;
  font-size: 13px;
}

.summary-value {
  margin-top: 10px;
  color: #173326;
  font-size: 30px;
  font-weight: 700;
}

.filter-card,
.table-card {
  border: none;
  border-radius: 22px;
  box-shadow: 0 16px 32px rgba(15, 81, 50, 0.08);
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.user-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;

  span {
    color: #173326;
    font-weight: 600;
  }

  small {
    color: #84948a;
  }
}

.detail-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-section {
  h4 {
    margin: 0 0 12px;
    color: #173326;
    font-size: 16px;
  }
}

.detail-content {
  padding: 16px;
  border-radius: 16px;
  background: #f6f9f6;
  color: #294437;
  line-height: 1.8;
  white-space: pre-wrap;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
}

.detail-image {
  width: 100%;
  height: 120px;
  border-radius: 14px;
}

.metric-row {
  display: flex;
  gap: 18px;
  color: #5f7267;
  font-size: 14px;
}

.review-remark {
  margin-top: 10px;
  color: #6c7d73;
}

.reply-item {
  .reply-user {
    color: #173326;
    font-weight: 600;
    margin-bottom: 6px;
  }

  .reply-content {
    color: #5a6f62;
    line-height: 1.7;
  }
}

@media (max-width: 960px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .metric-row {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
