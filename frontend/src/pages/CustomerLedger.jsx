import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api/client.js'

export default function CustomerLedger() {
  const { id } = useParams()
  const [customer, setCustomer] = useState(null)
  const [txns, setTxns] = useState([])
  const [type, setType] = useState('CREDIT')
  const [amount, setAmount] = useState('')
  const [note, setNote] = useState('')

  // AI ask state
  const [question, setQuestion] = useState('')
  const [askResult, setAskResult] = useState(null)
  const [asking, setAsking] = useState(false)

  const refresh = async () => {
    const [{ data: c }, { data: t }] = await Promise.all([
      api.get(`/customers/${id}`),
      api.get(`/customers/${id}/transactions`)
    ])
    setCustomer(c); setTxns(t)
  }

  useEffect(() => { refresh() }, [id])

  const addTxn = async (e) => {
    e.preventDefault()
    await api.post(`/customers/${id}/transactions`, { type, amount, note })
    setAmount(''); setNote('')
    await refresh()
  }

  const ask = async (e) => {
    e.preventDefault()
    setAsking(true); setAskResult(null)
    try {
      const { data } = await api.post('/ledger/ask', { question })
      setAskResult(data)
    } catch (err) {
      setAskResult({ answer: err.response?.data?.detail || 'Ask failed', data: [], intent: 'ERROR' })
    } finally {
      setAsking(false)
    }
  }

  if (!customer) return <main className="p-6 text-slate-500">Loading...</main>

  return (
    <main className="max-w-5xl mx-auto p-6 grid md:grid-cols-3 gap-6">
      <div className="md:col-span-2 space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <Link to="/" className="text-sm text-slate-500 hover:underline">&larr; All customers</Link>
            <h1 className="text-xl font-semibold text-slate-800 mt-1">{customer.name}</h1>
            <div className="text-sm text-slate-500">{customer.phone || 'No phone'}</div>
          </div>
          <div className={`text-2xl font-semibold ${Number(customer.outstanding) > 0 ? 'text-rose-600' : 'text-emerald-600'}`}>
            Rs.{customer.outstanding}
          </div>
        </div>

        <section className="bg-white rounded-xl border border-slate-200 p-4">
          <h2 className="font-medium text-slate-800 mb-3">Record entry</h2>
          <form onSubmit={addTxn} className="grid grid-cols-2 md:grid-cols-4 gap-2">
            <select value={type} onChange={(e) => setType(e.target.value)}
              className="border border-slate-300 rounded px-2 py-2">
              <option value="CREDIT">Udhaar (CREDIT)</option>
              <option value="DEBIT">Jama (DEBIT)</option>
            </select>
            <input type="number" step="0.01" min="0.01" value={amount}
              onChange={(e) => setAmount(e.target.value)} placeholder="Amount" required
              className="border border-slate-300 rounded px-3 py-2" />
            <input value={note} onChange={(e) => setNote(e.target.value)} placeholder="Note (optional)"
              className="border border-slate-300 rounded px-3 py-2 md:col-span-1" />
            <button className="bg-emerald-600 text-white px-4 py-2 rounded hover:bg-emerald-700">Save</button>
          </form>
        </section>

        <section className="bg-white rounded-xl border border-slate-200">
          <h2 className="font-medium text-slate-800 px-4 py-3 border-b border-slate-100">Transactions</h2>
          <table className="w-full text-sm">
            <thead className="text-slate-500 text-left">
              <tr>
                <th className="px-4 py-2">When</th>
                <th className="px-4 py-2">Type</th>
                <th className="px-4 py-2 text-right">Amount</th>
                <th className="px-4 py-2">Note</th>
              </tr>
            </thead>
            <tbody>
              {txns.length === 0 && (
                <tr><td colSpan="4" className="px-4 py-6 text-center text-slate-400">
                  No transactions yet.
                </td></tr>
              )}
              {txns.map((t) => (
                <tr key={t.id} className="border-t border-slate-100">
                  <td className="px-4 py-2 text-slate-500">{new Date(t.occurredAt).toLocaleString()}</td>
                  <td className="px-4 py-2">
                    <span className={`text-xs px-2 py-0.5 rounded-full ${
                      t.type === 'CREDIT' ? 'bg-rose-50 text-rose-700' : 'bg-emerald-50 text-emerald-700'
                    }`}>{t.type}</span>
                  </td>
                  <td className="px-4 py-2 text-right">Rs.{t.amount}</td>
                  <td className="px-4 py-2 text-slate-600">{t.note || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      </div>

      <aside className="bg-white rounded-xl border border-slate-200 p-4 h-fit space-y-3">
        <h2 className="font-medium text-slate-800">Ask your ledger</h2>
        <p className="text-xs text-slate-500">
          Try: "Who owes me the most?", "Total outstanding?", "Show me the last 5 transactions",
          "Which customers are overdue?"
        </p>
        <form onSubmit={ask} className="space-y-2">
          <textarea
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="Type a question..."
            rows={3}
            className="w-full border border-slate-300 rounded px-3 py-2 text-sm" />
          <button disabled={asking || !question.trim()}
            className="w-full bg-slate-900 text-white py-2 rounded hover:bg-slate-700 disabled:opacity-50">
            {asking ? 'Thinking...' : 'Ask'}
          </button>
        </form>

        {askResult && (
          <div className="text-sm space-y-2">
            <div className="text-slate-800">{askResult.answer}</div>
            <div className="text-xs text-slate-400">
              intent: <code>{askResult.intent}</code>
              {' · '}
              {askResult.usedLlm ? 'routed via Gemini' : 'routed locally (no API key)'}
            </div>
            {askResult.data && askResult.data.length > 0 && (
              <pre className="bg-slate-50 text-slate-700 rounded p-2 text-xs overflow-auto max-h-64">
{JSON.stringify(askResult.data, null, 2)}
              </pre>
            )}
          </div>
        )}
      </aside>
    </main>
  )
}
