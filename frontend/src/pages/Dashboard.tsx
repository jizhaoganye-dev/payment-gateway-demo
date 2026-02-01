import { useState } from 'react'
import { 
  TrendingUp, 
  CreditCard, 
  CheckCircle2, 
  XCircle, 
  Clock,
  ArrowUpRight,
  ArrowDownRight
} from 'lucide-react'
import { 
  AreaChart, 
  Area, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell
} from 'recharts'
import clsx from 'clsx'

/**
 * ダッシュボードページ
 * 
 * 【機能】
 * - 売上サマリー表示
 * - リアルタイムチャート
 * - 最近の取引一覧
 */

// モックデータ（実際はAPIから取得）
const mockStats = [
  { 
    label: '本日の売上', 
    value: '¥1,234,567', 
    change: '+12.5%', 
    trend: 'up',
    icon: TrendingUp,
    color: 'text-green-600 bg-green-50'
  },
  { 
    label: '処理件数', 
    value: '256', 
    change: '+8.3%', 
    trend: 'up',
    icon: CreditCard,
    color: 'text-blue-600 bg-blue-50'
  },
  { 
    label: '成功率', 
    value: '98.5%', 
    change: '+0.5%', 
    trend: 'up',
    icon: CheckCircle2,
    color: 'text-emerald-600 bg-emerald-50'
  },
  { 
    label: '平均処理時間', 
    value: '1.2s', 
    change: '-0.3s', 
    trend: 'down',
    icon: Clock,
    color: 'text-purple-600 bg-purple-50'
  },
]

const chartData = [
  { time: '00:00', amount: 4000 },
  { time: '04:00', amount: 3000 },
  { time: '08:00', amount: 5000 },
  { time: '12:00', amount: 8000 },
  { time: '16:00', amount: 12000 },
  { time: '20:00', amount: 9000 },
  { time: '24:00', amount: 6000 },
]

const statusData = [
  { name: '完了', value: 245, color: '#22c55e' },
  { name: '処理中', value: 5, color: '#3b82f6' },
  { name: '失敗', value: 6, color: '#ef4444' },
]

const recentTransactions = [
  { id: 'txn_001', amount: 15000, status: 'COMPLETED', method: 'CREDIT_CARD', time: '2分前' },
  { id: 'txn_002', amount: 8500, status: 'COMPLETED', method: 'QR_CODE', time: '5分前' },
  { id: 'txn_003', amount: 32000, status: 'PROCESSING', method: 'BANK_TRANSFER', time: '8分前' },
  { id: 'txn_004', amount: 5000, status: 'FAILED', method: 'CREDIT_CARD', time: '12分前' },
  { id: 'txn_005', amount: 12800, status: 'COMPLETED', method: 'E_MONEY', time: '15分前' },
]

export default function Dashboard() {
  const [timeRange, setTimeRange] = useState('today')

  return (
    <div className="space-y-6 animate-fade-in">
      {/* ページヘッダー */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">ダッシュボード</h1>
          <p className="text-gray-500">決済状況をリアルタイムで監視</p>
        </div>
        <div className="flex items-center gap-2 bg-white rounded-lg border border-gray-200 p-1">
          {['today', 'week', 'month'].map((range) => (
            <button
              key={range}
              onClick={() => setTimeRange(range)}
              className={clsx(
                'px-4 py-2 rounded-md text-sm font-medium transition-colors',
                timeRange === range
                  ? 'bg-primary-600 text-white'
                  : 'text-gray-600 hover:bg-gray-100'
              )}
            >
              {range === 'today' ? '今日' : range === 'week' ? '週間' : '月間'}
            </button>
          ))}
        </div>
      </div>

      {/* 統計カード */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {mockStats.map((stat) => (
          <div
            key={stat.label}
            className="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm hover:shadow-md transition-shadow"
          >
            <div className="flex items-center justify-between mb-4">
              <div className={clsx('p-3 rounded-xl', stat.color)}>
                <stat.icon className="w-6 h-6" />
              </div>
              <div className={clsx(
                'flex items-center gap-1 text-sm font-medium',
                stat.trend === 'up' ? 'text-green-600' : 'text-red-600'
              )}>
                {stat.trend === 'up' ? (
                  <ArrowUpRight className="w-4 h-4" />
                ) : (
                  <ArrowDownRight className="w-4 h-4" />
                )}
                {stat.change}
              </div>
            </div>
            <p className="text-2xl font-bold text-gray-900">{stat.value}</p>
            <p className="text-sm text-gray-500 mt-1">{stat.label}</p>
          </div>
        ))}
      </div>

      {/* チャートセクション */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* 売上推移チャート */}
        <div className="lg:col-span-2 bg-white rounded-2xl p-6 border border-gray-100 shadow-sm">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">売上推移</h2>
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="colorAmount" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="time" stroke="#94a3b8" fontSize={12} />
                <YAxis stroke="#94a3b8" fontSize={12} />
                <Tooltip 
                  contentStyle={{ 
                    backgroundColor: 'white', 
                    border: '1px solid #e2e8f0',
                    borderRadius: '8px'
                  }}
                />
                <Area
                  type="monotone"
                  dataKey="amount"
                  stroke="#3b82f6"
                  strokeWidth={2}
                  fillOpacity={1}
                  fill="url(#colorAmount)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* ステータス別円グラフ */}
        <div className="bg-white rounded-2xl p-6 border border-gray-100 shadow-sm">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">取引ステータス</h2>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={statusData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={80}
                  paddingAngle={5}
                  dataKey="value"
                >
                  {statusData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="flex justify-center gap-6 mt-4">
            {statusData.map((status) => (
              <div key={status.name} className="flex items-center gap-2">
                <div 
                  className="w-3 h-3 rounded-full" 
                  style={{ backgroundColor: status.color }}
                />
                <span className="text-sm text-gray-600">{status.name}</span>
                <span className="text-sm font-medium text-gray-900">{status.value}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* 最近の取引 */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-gray-900">最近の取引</h2>
          <a href="/transactions" className="text-primary-600 text-sm font-medium hover:underline">
            すべて表示 →
          </a>
        </div>
        <div className="divide-y divide-gray-100">
          {recentTransactions.map((tx) => (
            <div key={tx.id} className="px-6 py-4 flex items-center justify-between hover:bg-gray-50">
              <div className="flex items-center gap-4">
                <div className={clsx(
                  'w-10 h-10 rounded-full flex items-center justify-center',
                  tx.status === 'COMPLETED' ? 'bg-green-100' :
                  tx.status === 'PROCESSING' ? 'bg-blue-100' : 'bg-red-100'
                )}>
                  {tx.status === 'COMPLETED' ? (
                    <CheckCircle2 className="w-5 h-5 text-green-600" />
                  ) : tx.status === 'PROCESSING' ? (
                    <Clock className="w-5 h-5 text-blue-600" />
                  ) : (
                    <XCircle className="w-5 h-5 text-red-600" />
                  )}
                </div>
                <div>
                  <p className="font-medium text-gray-900">{tx.id}</p>
                  <p className="text-sm text-gray-500">{tx.method} • {tx.time}</p>
                </div>
              </div>
              <div className="text-right">
                <p className="font-semibold text-gray-900">¥{tx.amount.toLocaleString()}</p>
                <p className={clsx(
                  'text-sm font-medium',
                  tx.status === 'COMPLETED' ? 'text-green-600' :
                  tx.status === 'PROCESSING' ? 'text-blue-600' : 'text-red-600'
                )}>
                  {tx.status === 'COMPLETED' ? '完了' :
                   tx.status === 'PROCESSING' ? '処理中' : '失敗'}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
