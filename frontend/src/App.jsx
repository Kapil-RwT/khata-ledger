import { Routes, Route, Navigate, Link, useNavigate } from 'react-router-dom'
import Login from './pages/Login.jsx'
import Dashboard from './pages/Dashboard.jsx'
import CustomerLedger from './pages/CustomerLedger.jsx'
import { getAuth, clearAuth } from './store/auth.js'

function ProtectedRoute({ children }) {
  const auth = getAuth()
  return auth ? children : <Navigate to="/login" replace />
}

function NavBar() {
  const auth = getAuth()
  const nav = useNavigate()
  if (!auth) return null
  return (
    <nav className="bg-white border-b border-slate-200 px-6 py-3 flex items-center justify-between">
      <Link to="/" className="font-semibold text-slate-800">Khata Ledger</Link>
      <div className="flex items-center gap-4 text-sm">
        <span className="text-slate-500">{auth.businessName}</span>
        <button
          onClick={() => { clearAuth(); nav('/login') }}
          className="text-rose-600 hover:underline">Logout</button>
      </div>
    </nav>
  )
}

export default function App() {
  return (
    <div className="min-h-screen">
      <NavBar />
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
        <Route path="/customers/:id" element={<ProtectedRoute><CustomerLedger /></ProtectedRoute>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  )
}
