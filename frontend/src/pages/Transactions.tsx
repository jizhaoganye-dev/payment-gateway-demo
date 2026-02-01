import { useState } from 'react'
import { 
  Search, 
  Filter, 
  Download, 
  RefreshCw,
  ChevronLeft,
  ChevronRight,
  Eye,
  RotateCcw
} from 'lucide-react'
import clsx from 'clsx'
import { STATUS_LABELS, STATUS_COLORS, PAYMENT_METHOD_LABELS } from '../types/payment'
import type { TransactionStatus, PaymentMethod } from '../types/payment'

/**
 * 取引履歴ページ
 */

// モックデータ
const mockTransactions = Array.from({ length: 20 }, (_, i) => ({
  transactionId: `txn_${String(i + 1).padStart(6, '0')}`,
  amount: Math.floor(Math.random() * 100000) + 1000,
  currency: 'JPY',
  status: (['COMPLETED', 'COMPLETED', 'COMPLETED', 'PROCESSING', 'FAILED'] as TransactionStatus[])[
    Math.floor(Math.random() * 5)
  ],
  paymentMethod: (['CREDIT_CARD', 'QR_CODE', 'BANK_TRANSFER', 'E_MONEY'] as PaymentMethod[])[
    Math.floor(Math.random() * 4)
  ],
  merchantId: 'MERCHANT_001',
  customerId: `CUST_${String(Math.floor(Math.random() * 1000)).padStart(4, '0')}`,
  description: '商品購入',
  createdAt: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
  processedAt: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
}))

export default function Transactions() {
  const [searchQuery, setSearchQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('all')
  const [currentPage, setCurrentPage] = useState(1)
  const itemsPerPage = 10

  const filteredTransactions = mockTransactions.filter((tx) => {
    const matchesSearch = 
      tx.transactionId.toLowerCase().includes(searchQuery.toLowerCase()) ||
      tx.customerId.toLowerCase().includes(searchQuery.toLowerCase())
    const matchesStatus = statusFilter === 'all' || tx.status === statusFilter
    return matchesSearch && matchesStatus
  })

  const totalPages = Math.ceil(filteredTransactions.length / itemsPerPage)
  const paginatedTransactions = filteredTransactions.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  )

  const formatDate = (dateString: string) => {
    const date = new Date(dateString)
    return date.toLocaleString('ja-JP', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  return (
    <div className="space-y-6 animate-fade-in">
      {/* ページヘッダー */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">取引履歴</h1>
          <p className="text-gray-500">すべての決済トランザクションを管理</p>
        </div>
        <div className="flex items-center gap-3">
          <button className="flex items-center gap-2 px-4 py-2 text-gray-600 bg-white border border-gray-200 rounded-lg hover:bg-gray-50">
            <Download className="w-4 h-4" />
            エクスポート
          </button>
          <button className="flex items-center gap-2 px-4 py-2 text-white bg-primary-600 rounded-lg hover:bg-primary-700">
            <RefreshCw className="w-4 h-4" />
            更新
          </button>
        </div>
      </div>

      {/* フィルター */}
      <div className="bg-white rounded-xl border border-gray-100 p-4">
        <div className="flex flex-col md:flex-row gap-4">
          {/* 検索 */}
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              type="text"
              placeholder="取引ID、顧客IDで検索..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>

          {/* ステータスフィルター */}
          <div className="flex items-center gap-2">
            <Filter className="w-5 h-5 text-gray-400" />
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="px-4 py-2.5 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            >
              <option value="all">すべてのステータス</option>
              <option value="COMPLETED">完了</option>
              <option value="PROCESSING">処理中</option>
              <option value="FAILED">失敗</option>
              <option value="REFUNDED">返金済み</option>
            </select>
          </div>
        </div>
      </div>

      {/* テーブル */}
      <div className="bg-white rounded-xl border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                  取引ID
                </th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                  金額
                </th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                  ステータス
                </th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                  決済方法
                </th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                  顧客ID
                </th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                  日時
                </th>
                <th className="px-6 py-4 text-right text-xs font-semibold text-gray-500 uppercase tracking-wider">
                  操作
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {paginatedTransactions.map((tx) => (
                <tr key={tx.transactionId} className="hover:bg-gray-50">
                  <td className="px-6 py-4">
                    <span className="font-mono text-sm text-gray-900">{tx.transactionId}</span>
                  </td>
                  <td className="px-6 py-4">
                    <span className="font-semibold text-gray-900">
                      ¥{tx.amount.toLocaleString()}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <span className={clsx(
                      'inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium',
                      STATUS_COLORS[tx.status]
                    )}>
                      {STATUS_LABELS[tx.status]}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-600">
                    {PAYMENT_METHOD_LABELS[tx.paymentMethod]}
                  </td>
                  <td className="px-6 py-4">
                    <span className="font-mono text-sm text-gray-600">{tx.customerId}</span>
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-500">
                    {formatDate(tx.createdAt)}
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center justify-end gap-2">
                      <button className="p-2 text-gray-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg">
                        <Eye className="w-4 h-4" />
                      </button>
                      {tx.status === 'COMPLETED' && (
                        <button className="p-2 text-gray-400 hover:text-orange-600 hover:bg-orange-50 rounded-lg">
                          <RotateCcw className="w-4 h-4" />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* ページネーション */}
        <div className="px-6 py-4 border-t border-gray-100 flex items-center justify-between">
          <p className="text-sm text-gray-500">
            {filteredTransactions.length}件中 {(currentPage - 1) * itemsPerPage + 1}-
            {Math.min(currentPage * itemsPerPage, filteredTransactions.length)}件を表示
          </p>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              disabled={currentPage === 1}
              className="p-2 rounded-lg border border-gray-200 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="px-4 py-2 text-sm text-gray-600">
              {currentPage} / {totalPages}
            </span>
            <button
              onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
              disabled={currentPage === totalPages}
              className="p-2 rounded-lg border border-gray-200 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
