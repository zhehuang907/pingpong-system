import request from '@/utils/request'

export function addToCart(equipmentId, data = { quantity: 1 }) {
  return request({
    url: `/cart/add/${equipmentId}`,
    method: 'post',
    data
  })
}

export function getCartList() {
  return request({
    url: '/cart/list',
    method: 'get'
  })
}

export function getCartSummary() {
  return request({
    url: '/cart/summary',
    method: 'get'
  })
}

export function updateCartQuantity(cartItemId, quantity) {
  return request({
    url: `/cart/${cartItemId}/quantity`,
    method: 'put',
    data: { quantity }
  })
}

export function updateCartSelected(cartItemId, selected) {
  return request({
    url: `/cart/${cartItemId}/selected`,
    method: 'put',
    data: { selected }
  })
}

export function updateCartSelectedAll(selected) {
  return request({
    url: '/cart/selected-all',
    method: 'put',
    data: { selected }
  })
}

export function removeCartItem(cartItemId) {
  return request({
    url: `/cart/${cartItemId}`,
    method: 'delete'
  })
}

export function clearCart() {
  return request({
    url: '/cart/clear',
    method: 'delete'
  })
}

export function getCartCount() {
  return request({
    url: '/cart/count',
    method: 'get'
  })
}
