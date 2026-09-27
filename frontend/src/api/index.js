import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 20000,
  headers: { 'Content-Type': 'application/json' }
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && Object.prototype.hasOwnProperty.call(body, 'ok')) {
      if (!body.ok) {
        ElMessage.error(body.message || '操作失败')
        return Promise.reject(new Error(body.message || '操作失败'))
      }
      return body.data
    }
    return body
  },
  (error) => {
    const detail = error?.response?.data
    ElMessage.error((detail && detail.message) || error.message || '请求失败')
    return Promise.reject(error)
  }
)

/** 各模块的接口集合 */
export const metaApi = {
  dict: () => request.get('/meta')
}

export const styleApi = {
  list: (params) => request.get('/styles', { params }),
  detail: (id) => request.get(`/styles/${id}`),
  save: (data) => request.post('/styles', data),
  toggleSale: (id) => request.post(`/styles/${id}/sale`),
  remove: (id) => request.delete(`/styles/${id}`),
  meta: () => request.get('/styles/meta')
}

export const measurementApi = {
  list: (params) => request.get('/measurements', { params }),
  byCustomer: (customerId) => request.get(`/measurements/by-customer/${customerId}`),
  tailors: () => request.get('/measurements/tailors'),
  save: (data) => request.post('/measurements', data),
  remove: (id) => request.delete(`/measurements/${id}`)
}

export const fabricApi = {
  list: (params) => request.get('/fabrics', { params }),
  meta: () => request.get('/fabrics/meta'),
  save: (data) => request.post('/fabrics', data),
  restock: (id, delta) => request.post(`/fabrics/${id}/restock`, { delta }),
  remove: (id) => request.delete(`/fabrics/${id}`)
}

export const orderApi = {
  list: (params) => request.get('/orders', { params }),
  board: () => request.get('/orders/board'),
  customers: () => request.get('/orders/customers'),
  preview: (data) => request.post('/orders/price-preview', data),
  create: (data) => request.post('/orders', data),
  advance: (id, payload) => request.post(`/orders/${id}/advance`, payload || {}),
  payBalance: (id) => request.post(`/orders/${id}/balance-pay`),
  remove: (id) => request.delete(`/orders/${id}`)
}

export const workOrderApi = {
  list: (params) => request.get('/work-orders', { params }),
  save: (data) => request.post('/work-orders', data),
  advance: (id, payload) => request.post(`/work-orders/${id}/advance`, payload),
  remove: (id) => request.delete(`/work-orders/${id}`),
  stats: (params) => request.get('/work-orders/stats', { params })
}

export const fittingApi = {
  list: (params) => request.get('/fitting-issues', { params }),
  detail: (id) => request.get(`/fitting-issues/${id}`),
  create: (data) => request.post('/fitting-issues', data),
  startRework: (id, data) => request.post(`/fitting-issues/${id}/rework`, data),
  finishRound: (roundId, data) => request.post(`/fitting-issues/rounds/${roundId}/finish`, data),
  submitReview: (id, data) => request.post(`/fitting-issues/${id}/submit-review`, data),
  reviewPass: (id, data) => request.post(`/fitting-issues/${id}/review-pass`, data),
  reviewReject: (id, data) => request.post(`/fitting-issues/${id}/review-reject`, data),
  reopen: (id, data) => request.post(`/fitting-issues/${id}/reopen`, data),
  release: (id, data) => request.post(`/fitting-issues/${id}/release`, data),
  orderSummary: (orderId) => request.get(`/fitting-issues/order/${orderId}/summary`),
  rounds: (params) => request.get('/fitting-issues/rounds', { params }),
  orderLogs: (orderId) => request.get(`/fitting-issues/order/${orderId}/logs`),
  meta: () => request.get('/fitting-issues/meta')
}

export default request
