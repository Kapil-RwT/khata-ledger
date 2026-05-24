import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client.js'
import { setAuth } from '../store/auth.js'

export default function Login() {
  const [mode, setMode] = useState('login')   // 'login' | 'signup'
  const [businessName, setBusinessName] = useState('')
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const nav = useNavigate()

  const submit = async (e) => {
    e.preventDefault()
    setLoading(true); setError('')
    try {
      const path = mode === 'login' ? '/auth/login' : '/auth/signup'
      const body = mode === 'login' ? { phone, password } : { businessName, phone, password }
      const { data } = await api.post(path, body)
      setAuth(data)
      nav('/')
    } catch (err) {
      setError(err.response?.data?.detail || 'Something went wrong')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-100">
      <form onSubmit={submit} className="bg-white p-8 rounded-xl shadow-sm w-full max-w-sm space-y-4">
        <h1 className="text-2xl font-semibold text-slate-800">Khata Ledger</h1>
        <p className="text-sm text-slate-500">
          {mode === 'login' ? 'Welcome back' : 'Start your digital khata'}
        </p>

        {mode === 'signup' && (
          <input
            value={businessName}
            onChange={(e) => setBusinessName(e.target.value)}
            placeholder="Business name"
            className="w-full border border-slate-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-emerald-500"
            required
          />
        )}
        <input
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
          placeholder="Phone"
          className="w-full border border-slate-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-emerald-500"
          required
        />
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="Password"
          className="w-full border border-slate-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-emerald-500"
          required
        />

        {error && <div className="text-rose-600 text-sm">{error}</div>}

        <button
          disabled={loading}
          className="w-full bg-emerald-600 text-white py-2 rounded hover:bg-emerald-700 disabled:opacity-50">
          {loading ? '...' : mode === 'login' ? 'Login' : 'Sign up'}
        </button>

        <button
          type="button"
          onClick={() => setMode(mode === 'login' ? 'signup' : 'login')}
          className="text-sm text-slate-600 hover:underline w-full text-center">
          {mode === 'login' ? "New here? Create an account" : 'Already registered? Login'}
        </button>
      </form>
    </div>
  )
}
