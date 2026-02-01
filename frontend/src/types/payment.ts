/**
 * 決済関連の型定義
 */

export type TransactionStatus = 
  | 'PENDING' 
  | 'PROCESSING' 
  | 'COMPLETED' 
  | 'FAILED' 
  | 'REFUNDED' 
  | 'CANCELLED'

export type PaymentMethod = 
  | 'CREDIT_CARD' 
  | 'DEBIT_CARD' 
  | 'BANK_TRANSFER' 
  | 'CONVENIENCE_STORE'
  | 'QR_CODE' 
  | 'E_MONEY' 
  | 'WALLET'

export interface Transaction {
  transactionId: string
  amount: number
  currency: string
  status: TransactionStatus
  paymentMethod: PaymentMethod
  merchantId: string
  customerId: string
  description?: string
  errorCode?: string
  errorMessage?: string
  createdAt: string
  processedAt?: string
}

export interface PaymentRequest {
  amount: number
  currency: string
  paymentMethod: PaymentMethod
  merchantId: string
  customerId: string
  description?: string
  idempotencyKey?: string
  metadata?: string
}

export interface ApiResponse<T> {
  success: boolean
  data?: T
  errorCode?: string
  message?: string
  requestId?: string
  timestamp: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface SalesSummary {
  merchantId: string
  totalSales: number
  transactionsByStatus: Record<TransactionStatus, number>
  generatedAt: string
}

// ステータス表示用のマッピング
export const STATUS_LABELS: Record<TransactionStatus, string> = {
  PENDING: '処理待ち',
  PROCESSING: '処理中',
  COMPLETED: '完了',
  FAILED: '失敗',
  REFUNDED: '返金済み',
  CANCELLED: 'キャンセル',
}

export const STATUS_COLORS: Record<TransactionStatus, string> = {
  PENDING: 'bg-yellow-100 text-yellow-800',
  PROCESSING: 'bg-blue-100 text-blue-800',
  COMPLETED: 'bg-green-100 text-green-800',
  FAILED: 'bg-red-100 text-red-800',
  REFUNDED: 'bg-purple-100 text-purple-800',
  CANCELLED: 'bg-gray-100 text-gray-800',
}

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CREDIT_CARD: 'クレジットカード',
  DEBIT_CARD: 'デビットカード',
  BANK_TRANSFER: '銀行振込',
  CONVENIENCE_STORE: 'コンビニ決済',
  QR_CODE: 'QRコード決済',
  E_MONEY: '電子マネー',
  WALLET: 'ウォレット',
}
