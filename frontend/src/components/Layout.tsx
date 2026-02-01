import { Outlet, NavLink } from 'react-router-dom'
import { 
  LayoutDashboard, 
  CreditCard, 
  PlusCircle, 
  Settings,
  Bell,
  Search,
  Building2
} from 'lucide-react'
import clsx from 'clsx'

/**
 * メインレイアウト
 * 
 * 【設計思想】
 * - サイドバーナビゲーション
 * - レスポンシブデザイン
 * - モダンなUI/UX
 */
export default function Layout() {
  const navItems = [
    { to: '/', icon: LayoutDashboard, label: 'ダッシュボード' },
    { to: '/transactions', icon: CreditCard, label: '取引履歴' },
    { to: '/payments/new', icon: PlusCircle, label: '新規決済' },
  ]

  return (
    <div className="flex h-screen bg-gray-50">
      {/* サイドバー */}
      <aside className="w-64 bg-white border-r border-gray-200 flex flex-col">
        {/* ロゴ */}
        <div className="h-16 flex items-center px-6 border-b border-gray-200">
          <Building2 className="w-8 h-8 text-primary-600" />
          <div className="ml-3">
            <h1 className="text-lg font-bold text-gray-900">Payment Gateway</h1>
            <p className="text-xs text-gray-500">決済管理システム</p>
          </div>
        </div>

        {/* ナビゲーション */}
        <nav className="flex-1 px-4 py-6 space-y-2">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                clsx(
                  'flex items-center gap-3 px-4 py-3 rounded-xl transition-all',
                  isActive
                    ? 'bg-primary-50 text-primary-700 font-medium'
                    : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                )
              }
            >
              <item.icon className="w-5 h-5" />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        {/* フッター */}
        <div className="p-4 border-t border-gray-200">
          <div className="flex items-center gap-3 px-4 py-3 rounded-xl bg-gray-50">
            <div className="w-10 h-10 rounded-full bg-primary-100 flex items-center justify-center">
              <span className="text-primary-700 font-medium">M</span>
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-gray-900 truncate">MERCHANT_001</p>
              <p className="text-xs text-gray-500">加盟店ID</p>
            </div>
            <Settings className="w-5 h-5 text-gray-400 cursor-pointer hover:text-gray-600" />
          </div>
        </div>
      </aside>

      {/* メインコンテンツ */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* ヘッダー */}
        <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6">
          <div className="flex items-center gap-4 flex-1">
            <div className="relative max-w-md flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="text"
                placeholder="取引ID、顧客IDで検索..."
                className="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              />
            </div>
          </div>

          <div className="flex items-center gap-4">
            <button className="relative p-2 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100">
              <Bell className="w-5 h-5" />
              <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full"></span>
            </button>
            <div className="h-8 w-px bg-gray-200"></div>
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-full bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-white text-sm font-medium">
                A
              </div>
              <span className="text-sm font-medium text-gray-700">Admin</span>
            </div>
          </div>
        </header>

        {/* ページコンテンツ */}
        <main className="flex-1 overflow-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
