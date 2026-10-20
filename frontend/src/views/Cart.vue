<template>
  <div class="cart-page">
    <Header />

    <main class="main-content">
      <section class="page-hero">
        <div>
          <div class="page-kicker">我的购物车</div>
          <h1>待比较、待下单的器材都在这里</h1>
          <p>支持商品添加、数量修改、删除、选中状态切换和价格汇总，方便集中比较后再做决定。</p>
        </div>
        <div class="hero-actions" v-if="cartList.length">
          <el-button @click="$router.push('/equipment-list')">继续挑选</el-button>
          <el-button type="danger" plain @click="handleClearCart">清空购物车</el-button>
        </div>
      </section>

      <section class="cart-shell" v-loading="loading">
        <el-empty v-if="!loading && !cartList.length" description="购物车还是空的">
          <el-button type="primary" @click="$router.push('/equipment-list')">去挑选器材</el-button>
        </el-empty>

        <template v-else>
          <div class="cart-list">
            <div class="cart-head">
              <el-checkbox
                :model-value="summary?.allSelected"
                @change="handleToggleAll"
              >
                全选
              </el-checkbox>
              <span>已选 {{ summary?.selectedItemCount || 0 }} 件 / 共 {{ summary?.itemCount || 0 }} 件</span>
            </div>

            <article v-for="item in cartList" :key="item.id" class="cart-card">
              <div class="cart-select">
                <el-checkbox
                  :model-value="item.selected === 1"
                  @change="(value) => handleToggleSelected(item, value)"
                />
              </div>

              <el-image
                :src="getEquipmentImageUrl(item.equipment)"
                fit="cover"
                class="cart-image"
                @click="$router.push(`/equipment/${item.equipmentId}`)"
              />

              <div class="cart-info">
                <div class="info-top">
                  <div>
                    <h3 class="equipment-name" @click="$router.push(`/equipment/${item.equipmentId}`)">
                      {{ item.equipment?.name || `器材 #${item.equipmentId}` }}
                    </h3>
                    <p class="equipment-meta">
                      {{ item.equipment?.brand || '未知品牌' }}
                      <span v-if="item.equipment?.model"> · {{ item.equipment.model }}</span>
                    </p>
                  </div>
                  <el-tag :type="item.equipment?.stock > 0 ? 'success' : 'danger'" effect="plain">
                    {{ item.equipment?.stock > 0 ? `库存 ${item.equipment.stock}` : '暂时缺货' }}
                  </el-tag>
                </div>

                <div class="info-bottom">
                  <div class="price-wrap">
                    <span class="currency">¥</span>
                    <span class="price">{{ formatPrice(item.equipment?.price) }}</span>
                  </div>

                  <div class="quantity-actions">
                    <span class="label">数量</span>
                    <el-input-number
                      :min="1"
                      :max="Math.max(item.equipment?.stock || 1, 1)"
                      :model-value="item.quantity"
                      @change="(value) => handleQuantityChange(item, value)"
                    />
                  </div>

                  <div class="subtotal">
                    小计：<strong>¥{{ formatPrice(calcSubtotal(item)) }}</strong>
                  </div>

                  <el-button text type="danger" @click="handleRemoveItem(item.id)">移出</el-button>
                </div>
              </div>
            </article>
          </div>

          <aside class="summary-card">
            <div class="summary-title">结算概览</div>
            <div class="summary-row">
              <span>商品件数</span>
              <strong>{{ summary?.itemCount || 0 }}</strong>
            </div>
            <div class="summary-row">
              <span>商品数量</span>
              <strong>{{ summary?.totalQuantity || 0 }}</strong>
            </div>
            <div class="summary-row">
              <span>已选商品</span>
              <strong>{{ summary?.selectedItemCount || 0 }}</strong>
            </div>
            <div class="summary-row">
              <span>已选数量</span>
              <strong>{{ summary?.selectedQuantity || 0 }}</strong>
            </div>
            <div class="summary-row">
              <span>全部金额</span>
              <strong>¥{{ formatPrice(summary?.totalAmount) }}</strong>
            </div>
            <div class="summary-row summary-row--highlight">
              <span>已选金额</span>
              <strong>¥{{ formatPrice(summary?.selectedAmount) }}</strong>
            </div>
            <div class="summary-tip">当前为购物车体验版，可继续收藏、对比或返回详情页进一步确认参数。</div>
            <el-button type="primary" class="summary-btn" :disabled="!(summary?.selectedItemCount > 0)" @click="$router.push('/equipment-list')">
              继续挑选
            </el-button>
          </aside>
        </template>
      </section>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  clearCart,
  getCartList,
  getCartSummary,
  removeCartItem,
  updateCartQuantity,
  updateCartSelected,
  updateCartSelectedAll
} from '@/api/cart'
import Footer from '@/components/Footer.vue'
import Header from '@/components/Header.vue'

const loading = ref(false)
const cartList = ref([])
const summary = ref(null)

function getEquipmentImageUrl(equipment) {
  // 优先使用 mainImage，其次使用 imageUrl，最后使用占位图
  return equipment?.mainImage || equipment?.imageUrl || 'data:image/svg+xml;charset=UTF-8,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 300 220"%3E%3Crect width="300" height="220" fill="%23eef2f7"/%3E%3Ccircle cx="150" cy="86" r="34" fill="%23cbd5e1"/%3E%3Cpath d="M84 178c18-28 42-42 66-42s48 14 66 42" fill="%23dbe3ec"/%3E%3C/svg%3E'
}

onMounted(loadCartData)

async function loadCartData() {
  loading.value = true
  try {
    const [listRes, summaryRes] = await Promise.all([getCartList(), getCartSummary()])
    cartList.value = listRes.data || []
    summary.value = summaryRes.data?.summary || null
  } catch (error) {
    ElMessage.error(error?.message || '购物车加载失败')
  } finally {
    loading.value = false
  }
}

async function handleQuantityChange(item, value) {
  const nextValue = Math.max(Number(value || 1), 1)
  try {
    const res = await updateCartQuantity(item.id, nextValue)
    item.quantity = res.data?.cartItem?.quantity ?? nextValue
    applySummary(res.data?.summary)
    ElMessage.success('数量已更新')
  } catch (error) {
    ElMessage.error(error?.message || '数量更新失败')
    await loadCartData()
  }
}

async function handleToggleSelected(item, value) {
  try {
    const res = await updateCartSelected(item.id, value)
    item.selected = value ? 1 : 0
    applySummary(res.data?.summary)
  } catch (error) {
    ElMessage.error(error?.message || '选中状态更新失败')
    await loadCartData()
  }
}

async function handleToggleAll(value) {
  try {
    const res = await updateCartSelectedAll(value)
    cartList.value = (res.data?.list || cartList.value).map(item => ({
      ...item,
      selected: value ? 1 : 0
    }))
    applySummary(res.data?.summary)
  } catch (error) {
    ElMessage.error(error?.message || '全选状态更新失败')
    await loadCartData()
  }
}

async function handleRemoveItem(cartItemId) {
  try {
    await removeCartItem(cartItemId)
    cartList.value = cartList.value.filter(item => item.id !== cartItemId)
    await refreshSummary()
    ElMessage.success('已移出购物车')
  } catch (error) {
    ElMessage.error(error?.message || '移除失败')
  }
}

async function handleClearCart() {
  try {
    await ElMessageBox.confirm('清空后需要重新添加，确定继续吗？', '清空确认', {
      type: 'warning'
    })
    await clearCart()
    cartList.value = []
    summary.value = {
      itemCount: 0,
      totalQuantity: 0,
      selectedItemCount: 0,
      selectedQuantity: 0,
      totalAmount: 0,
      selectedAmount: 0,
      allSelected: false
    }
    ElMessage.success('购物车已清空')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '清空失败')
    }
  }
}

async function refreshSummary() {
  const res = await getCartSummary()
  summary.value = res.data?.summary || null
}

function applySummary(nextSummary) {
  if (!nextSummary) {
    return
  }
  summary.value = nextSummary
}

function calcSubtotal(item) {
  return Number(item.equipment?.price || 0) * Number(item.quantity || 0)
}

function formatPrice(value) {
  return Number(value || 0).toFixed(2)
}
</script>

<style scoped lang="scss">
.cart-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(240, 138, 36, 0.12), transparent 24%),
    radial-gradient(circle at bottom right, rgba(29, 88, 70, 0.12), transparent 28%),
    linear-gradient(180deg, #f4efe6 0%, #f4f8f1 100%);
}

.main-content {
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 20px 40px;
}

.page-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
  padding: 28px 32px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 22px 48px rgba(23, 53, 46, 0.08);
}

.page-hero h1 {
  margin: 8px 0 6px;
  font-size: 30px;
  color: #17352e;
}

.page-hero p {
  margin: 0;
  line-height: 1.7;
  color: #66766d;
}

.page-kicker {
  color: #d96b1f;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.cart-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: start;
}

.cart-list {
  display: grid;
  gap: 16px;
}

.cart-head,
.cart-card,
.summary-card {
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 38px rgba(23, 53, 46, 0.08);
}

.cart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px;
  color: #55645b;
}

.cart-card {
  display: grid;
  grid-template-columns: 32px 180px minmax(0, 1fr);
  gap: 18px;
  padding: 18px;
}

.cart-select {
  display: flex;
  align-items: flex-start;
  padding-top: 10px;
}

.cart-image {
  width: 180px;
  height: 160px;
  border-radius: 20px;
  cursor: pointer;
  background: #eef3ee;
}

.cart-info {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 20px;
}

.info-top {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.equipment-name {
  margin: 0;
  font-size: 22px;
  color: #17352e;
  cursor: pointer;
}

.equipment-meta {
  margin: 10px 0 0;
  color: #69786f;
}

.info-bottom {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 18px;
}

.price-wrap {
  display: flex;
  align-items: baseline;
  gap: 4px;
  color: #f26f22;
}

.currency {
  font-size: 18px;
  font-weight: 700;
}

.price {
  font-size: 28px;
  font-weight: 800;
}

.quantity-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.label {
  color: #66766d;
  font-size: 14px;
}

.subtotal {
  margin-left: auto;
  color: #445147;
}

.summary-card {
  position: sticky;
  top: 96px;
  padding: 24px;
}

.summary-title {
  font-size: 22px;
  font-weight: 700;
  color: #17352e;
}

.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  color: #55645b;
}

.summary-row--highlight strong {
  color: #f26f22;
}

.summary-tip {
  margin-top: 18px;
  color: #76837c;
  line-height: 1.7;
}

.summary-btn {
  width: 100%;
  margin-top: 20px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #1d5846, #f08a24);
}

@media (max-width: 960px) {
  .cart-shell {
    grid-template-columns: 1fr;
  }

  .summary-card {
    position: static;
  }
}

@media (max-width: 768px) {
  .main-content {
    padding: 18px 14px 32px;
  }

  .page-hero,
  .cart-card {
    grid-template-columns: 1fr;
  }

  .page-hero {
    padding: 22px 20px;
  }

  .hero-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .cart-image {
    width: 100%;
    height: 220px;
  }

  .info-top,
  .info-bottom {
    flex-direction: column;
    align-items: flex-start;
  }

  .subtotal {
    margin-left: 0;
  }
}
</style>
