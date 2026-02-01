import axios from 'axios'
import type { 
  Transaction, 
  PaymentRequest, 
  ApiResponse, 
  PageResponse,
  SalesSummary 
} from '../types/payment'

/**
 * API クライアント
 * 
 * 【設計思想】
 * - Axiosによる統一的なHTTP通信
 * - エラーハンドリングの集約
 * - 型安全なAPI呼び出し
 */

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
})

// リクエストインターセプター
api.interceptors.request.use((config) => {
  // リクエストIDを自動付与
  config.headers['X-Request-Id'] = `req_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  return config
})

// レスポンスインターセプター
api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error.response?.data || error.message)
    return Promise.reject(error)
  }
)

/**
 * 決済処理
 */
export const processPayment = async (request: PaymentRequest): Promise<Transaction> => {
  const response = await api.post<ApiResponse<Transaction>>('/payments', request)
  if (!response.data.success) {
    throw new Error(response.data.message || '決済処理に失敗しました')
  }
  return response.data.data!
}

/**
 * トランザクション取得
 */
export const getTransaction = async (transactionId: string): Promise<Transaction> => {
  const response = await api.get<ApiResponse<Transaction>>(`/payments/${transactionId}`)
  if (!response.data.success) {
    throw new Error(response.data.message || 'トランザクションの取得に失敗しました')
  }
  return response.data.data!
}

/**
 * 加盟店のトランザクション一覧取得
 */
export const getTransactionsByMerchant = async (
  merchantId: string,
  page: number = 0,
  size: number = 20
): Promise<PageResponse<Transaction>> => {
  const response = await api.get<ApiResponse<PageResponse<Transaction>>>(
    `/payments/merchant/${merchantId}`,
    { params: { page, size } }
  )
  if (!response.data.success) {
    throw new Error(response.data.message || 'トランザクション一覧の取得に失敗しました')
  }
  return response.data.data!
}

/**
 * 返金処理
 */
export const refundTransaction = async (
  transactionId: string,
  amount: number
): Promise<Transaction> => {
  const response = await api.post<ApiResponse<Transaction>>(
    `/payments/${transactionId}/refund`,
    null,
    { params: { amount } }
  )
  if (!response.data.success) {
    throw new Error(response.data.message || '返金処理に失敗しました')
  }
  return response.data.data!
}

/**
 * 売上サマリー取得
 */
export const getSalesSummary = async (merchantId: string): Promise<SalesSummary> => {
  const response = await api.get<ApiResponse<SalesSummary>>(
    `/payments/merchant/${merchantId}/summary`
  )
  if (!response.data.success) {
    throw new Error(response.data.message || '売上サマリーの取得に失敗しました')
  }
  return response.data.data!
}

/**
 * ヘルスチェック
 */
export const healthCheck = async (): Promise<{ status: string; service: string; version: string }> => {
  const response = await api.get<ApiResponse<{ status: string; service: string; version: string }>>(
    '/payments/health'
  )
  return response.data.data!
}

export default api
