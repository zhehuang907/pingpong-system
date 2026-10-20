<template>
  <div class="equipment-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>装备管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增装备
          </el-button>
        </div>
      </template>
      
      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="装备名称/品牌" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryForm.categoryId" placeholder="全部分类" clearable>
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      
      <el-table :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="brand" label="品牌" width="100" />
        <el-table-column prop="price" label="价格" width="100" />
        <el-table-column prop="viewCount" label="浏览量" width="100" />
        <el-table-column prop="favoriteCount" label="收藏量" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>
    
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="装备名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="品牌" prop="brand">
          <el-input v-model="form.brand" />
        </el-form-item>
        <el-form-item label="型号" prop="model">
          <el-input v-model="form.model" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :precision="2" :min="0" />
        </el-form-item>
        <el-form-item label="原价" prop="originalPrice">
          <el-input-number v-model="form.originalPrice" :precision="2" :min="0" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="装备照片">
          <EquipmentImageUploader
            :key="uploaderKey"
            v-model="form.imageList"
            v-model:main-image="form.mainImage"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEquipmentList, addEquipment, updateEquipment, deleteEquipment } from '@/api/equipment'
import { getCategoryList } from '@/api/category'
import EquipmentImageUploader from '@/components/EquipmentImageUploader.vue'

const queryForm = reactive({
  keyword: '',
  categoryId: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])
const categories = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const isEdit = ref(false)
const uploaderKey = ref(0)

const form = reactive({
  id: null,
  name: '',
  categoryId: null,
  brand: '',
  model: '',
  price: 0,
  originalPrice: null,
  stock: 0,
  description: '',
  mainImage: '',
  imageList: [],
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入装备名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
}

onMounted(async () => {
  await loadCategories()
  await loadData()
})

const loadCategories = async () => {
  const res = await getCategoryList()
  categories.value = res.data || []
}

const loadData = async () => {
  const params = {
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize,
    keyword: queryForm.keyword || undefined,
    categoryId: queryForm.categoryId || undefined
  }
  
  const res = await getEquipmentList(params)
  tableData.value = res.data?.records || []
  pagination.total = res.data?.total || 0
}

const resetQuery = () => {
  queryForm.keyword = ''
  queryForm.categoryId = ''
  pagination.pageNum = 1
  loadData()
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增装备'
  resetForm()
  uploaderKey.value += 1
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑装备'
  resetForm()
  Object.assign(form, row, {
    imageList: normalizeImageList(row.images),
    mainImage: row.mainImage || normalizeImageList(row.images)[0] || ''
  })
  uploaderKey.value += 1
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该装备吗？', '提示', {
      type: 'warning'
    })
    await deleteEquipment(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const payload = buildSubmitPayload()
        if (isEdit.value) {
          await updateEquipment(payload)
          ElMessage.success('更新成功')
        } else {
          await addEquipment(payload)
          ElMessage.success('添加成功')
        }
        dialogVisible.value = false
        loadData()
      } catch (error) {
        ElMessage.error('操作失败')
      }
    }
  })
}

const resetForm = () => {
  form.id = null
  form.name = ''
  form.categoryId = null
  form.brand = ''
  form.model = ''
  form.price = 0
  form.originalPrice = null
  form.stock = 0
  form.description = ''
  form.mainImage = ''
  form.imageList = []
  form.status = 1
}

const buildSubmitPayload = () => {
  const imageList = Array.isArray(form.imageList) ? form.imageList.filter(Boolean) : []
  const mainImage = form.mainImage || imageList[0] || ''

  return {
    id: form.id,
    name: form.name,
    categoryId: form.categoryId,
    brand: form.brand,
    model: form.model,
    price: form.price,
    originalPrice: form.originalPrice,
    stock: form.stock,
    description: form.description,
    mainImage,
    images: imageList.length ? JSON.stringify(imageList) : null,
    status: form.status
  }
}

const normalizeImageList = (value) => {
  if (Array.isArray(value)) {
    return value.filter(Boolean)
  }

  if (typeof value === 'string' && value.trim()) {
    try {
      const parsed = JSON.parse(value)
      if (Array.isArray(parsed)) {
        return parsed.filter(Boolean)
      }
    } catch (error) {
      return value.split(',').map((item) => item.trim()).filter(Boolean)
    }
  }

  return []
}
</script>

<style scoped lang="scss">
.equipment-manage {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .query-form {
    margin-bottom: 20px;
  }

  :deep(.el-dialog__body) {
    padding-top: 12px;
  }
}
</style>
