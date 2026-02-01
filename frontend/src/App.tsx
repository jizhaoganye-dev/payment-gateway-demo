import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Layout from './components/Layout'
import Dashboard from './pages/Dashboard'
import Transactions from './pages/Transactions'
import NewPayment from './pages/NewPayment'

/**
 * メインアプリケーション
 * 
 * 【アーキテクチャ特徴】
 * - React Router によるSPAルーティング
 * - コンポーネントベース設計
 * - フロントエンドとバックエンドの完全分離
 */
function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="transactions" element={<Transactions />} />
          <Route path="payments/new" element={<NewPayment />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App
