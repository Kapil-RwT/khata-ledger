import axios from 'axios'
import { getToken, clearAuth } from '../store/auth.js'

// In dev (npm run dev): VITE_API_BASE_URL is unset, so we fall back to '/api'
// and Vite's proxy (see vite.config.js) forwards to http://localhost:8080.
//
// In production (Vercel build): set VITE_API_BASE_URL to your deployed backend URL,
// e.g. https://khata-ledger-api.onrender.com/api  -- baked in at build time.
const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'

export const api = axios.create({ baseURL })

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response && err.response.status === 401) {
      clearAuth()
      // hard reload to /login
      if (window.location.pathname !== '/login') window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)
