// Tiny localStorage-backed auth store. Frontend-only state -> not the same constraint
// as Anthropic artifacts; vanilla browser localStorage is fine for a real Vite app.

const KEY = 'khata.auth'

export function setAuth({ token, merchantId, businessName }) {
  localStorage.setItem(KEY, JSON.stringify({ token, merchantId, businessName }))
}

export function getAuth() {
  try { return JSON.parse(localStorage.getItem(KEY)) } catch { return null }
}

export function getToken() {
  const a = getAuth()
  return a ? a.token : null
}

export function clearAuth() {
  localStorage.removeItem(KEY)
}
