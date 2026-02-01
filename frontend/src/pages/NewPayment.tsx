import { useState } from 'react'
import { 
  CreditCard, 
  Smartphone, 
  Building, 
  Store,
  Wallet,
  Zap,
  CheckCircle2,
  AlertCircle
} from 'lucide-react'
import clsx from 'clsx'
import type { PaymentMethod } from '../types/payment'

/**
 * 新規決済ページ
 */

const paymentMethods = [
  { id: 'CREDIT_CARD' as PaymentMethod, label: 'クレジットカード', icon: CreditCard },
  { id: 'QR_CODE' as PaymentMethod, label: 'QRコード決済', icon: Smartphone },
  { id: 'BANK_TRANSFER' as PaymentMethod, label: '銀行振込', icon: Building },
  { id: 'CONVENIENCE_STORE' as PaymentMethod, label: 'コンビニ決済', icon: Store },
  { id: 'E_MONEY' as PaymentMethod, label: '電子マネー', icon: Zap },
  { id: 'WALLET' as PaymentMethod, label: 'ウォレット', icon: Wallet },
]

export default function NewPayment() {
  const [selectedMethod, setSelectedMethod] = useState<PaymentMethod>('CREDIT_CARD')
  const [amount, setAmount] = useState('')
  const [customerId, setCustomerId] = useState('')
  const [description, setDescription] = useState('')
  const [isProcessing, setIsProcessing] = useState(false)
  const [result, setResult] = useState<{ success: boolean; message: string } | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsProcessing(true)
    setResult(null)

    // シミュレート処理
    await new Promise((resolve) => setTimeout(resolve, 2000))

    // 成功をシミュレート（実際はAPIを呼び出す）
    setResult({
      success: true,
      message: `決済が完了しました。取引ID: txn_${Date.now().toString(36)}`,
    })
    setIsProcessing(false)
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6 animate-fade-in">
      {/* ページヘッダー */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">新規決済</h1>
        <p className="text-gray-500">決済トランザクションを作成します</p>
      </div>

      {/* 結果メッセージ */}
      {result && (
        <div
          className={clsx(
            'p-4 rounded-xl flex items-center gap-3',
            result.success ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'
          )}
        >
          {result.success ? (
            <CheckCircle2 className="w-5 h-5" />
          ) : (
            <AlertCircle className="w-5 h-5" />
          )}
          <span className="font-medium">{result.message}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* 決済方法選択 */}
        <div className="bg-white rounded-2xl border border-gray-100 p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">決済方法</h2>
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            {paymentMethods.map((method) => (
              <button
                key={method.id}
                type="button"
                onClick={() => setSelectedMethod(method.id)}
                className={clsx(
                  'flex flex-col items-center gap-3 p-4 rounded-xl border-2 transition-all',
                  selectedMethod === method.id
                    ? 'border-primary-500 bg-primary-50 text-primary-700'
                    : 'border-gray-100 hover:border-gray-200 text-gray-600'
                )}
              >
                <method.icon className="w-8 h-8" />
                <span className="text-sm font-medium">{method.label}</span>
              </button>
            ))}
          </div>
        </div>

        {/* 決済情報 */}
        <div className="bg-white rounded-2xl border border-gray-100 p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">決済情報</h2>
          <div className="space-y-4">
            {/* 金額 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                金額 <span className="text-red-500">*</span>
              </label>
              <div className="relative">
                <span className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500">¥</span>
                <input
                  type="number"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  placeholder="10000"
                  required
                  min="1"
                  className="w-full pl-10 pr-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                />
              </div>
            </div>

            {/* 顧客ID */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                顧客ID <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={customerId}
                onChange={(e) => setCustomerId(e.target.value)}
                placeholder="CUST_0001"
                required
                className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              />
            </div>

            {/* 説明 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                説明
              </label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="商品購入、サービス利用など"
                rows={3}
                className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
              />
            </div>
          </div>
        </div>

        {/* 確認・送信 */}
        <div className="bg-white rounded-2xl border border-gray-100 p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-lg font-semibold text-gray-900">決済金額</h2>
              <p className="text-3xl font-bold text-primary-600 mt-1">
                ¥{amount ? Number(amount).toLocaleString() : '0'}
              </p>
            </div>
            <div className="text-right text-sm text-gray-500">
              <p>加盟店ID: MERCHANT_001</p>
              <p>決済方法: {paymentMethods.find((m) => m.id === selectedMethod)?.label}</p>
            </div>
          </div>

          <button
            type="submit"
            disabled={isProcessing || !amount || !customerId}
            className={clsx(
              'w-full py-4 rounded-xl font-semibold text-lg transition-all',
              isProcessing || !amount || !customerId
                ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                : 'bg-primary-600 text-white hover:bg-primary-700 shadow-lg shadow-primary-500/30'
            )}
          >
            {isProcessing ? (
              <span className="flex items-center justify-center gap-2">
                <svg className="animate-spin w-5 h-5" viewBox="0 0 24 24">
                  <circle
                    className="opacity-25"
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="currentColor"
                    strokeWidth="4"
                    fill="none"
                  />
                  <path
                    className="opacity-75"
                    fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                  />
                </svg>
                処理中...
              </span>
            ) : (
              '決済を実行'
            )}
          </button>
        </div>
      </form>
    </div>
  )
}
