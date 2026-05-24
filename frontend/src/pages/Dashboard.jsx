import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client.js'

function StatCard({ label, value, tone = 'slate' }) {
  const toneClass = {
    emerald: 'text-emerald-700 bg-emerald-50',
    rose: 'text-rose-700 bg-rose-50',
    slate: 'text-slate-700 bg-slate-50',
    amber: 'text-amber-700 bg-amber-50'
  }[tone]
  return (
    <div className={`rounded-lg p-4 ${toneClass}`}>
      <div className="text-xs uppercase tracking-wide">{label}</div>
      <div className="text-2xl font-semibold mt-1">{value}</div>
    </div>
  )
}

export default function Dashboard() {
  const [summary, setSummary] = useState(null)
  const [customers, setCustomers] = useState([])
  const [newName, setNewName] = useState('')
  const [newPhone, setNewPhone] = useState('')
  const [busy, setBusy] = useState(false)

  const refresh = async () => {
    const [{ data: s }, { data: c }] = await Promise.all([
      api.get('/ledger/dashboard'),
      api.get('/customers')
    ])
    setSummary(s); setCustomers(c)
  }

  useEffect(() => { refresh() }, [])

  const addCustomer = async (e) => {
    e.preventDefault()
    setBusy(true)
    try {
      await api.post('/customers', { name: newName, phone: newPhone })
      setNewName(''); setNewPhone('')
      await refresh()
    } finally { setBusy(false) }
  }

  return (
    <main className="max-w-5xl mx-auto p-6 space-y-6">
      <h1 className="text-xl font-semibold text-slate-800">Dashboard</h1>

      {summary && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          <StatCard label="Receivables" value={`Rs.${summary.receivables}`} tone="emerald" />
          <StatCard label="Payables" value={`Rs.${summary.payables}`} tone="rose" />
          <StatCard label="Customers" value={summary.customerCount} tone="slate" />
          <StatCard label="Overdue" value={summary.overdueCount} tone="amber" />
        </div>
      )}

      <section className="bg-white rounded-xl border border-slate-200 p-4">
        <h2 className="font-medium text-slate-800 mb-3">Add customer</h2>
        <form onSubmit={addCustomer} className="flex gap-2">
          <input
            value={newName} onChange={(e) => setNewName(e.target.value)}
            placeholder="Name" required
            className="flex-1 border border-slate-300 rounded px-3 py-2" />
          <input
            value={newPhone} onChange={(e) => setNewPhone(e.target.value)}
            placeholder="Phone (optional)"
            className="flex-1 border border-slate-300 rounded px-3 py-2" />
          <button disabled={busy}
            className="bg-emerald-600 text-white px-4 py-2 rounded hover:bg-emerald-700">
            Add
          </button>
        </form>
      </section>

      <section className="bg-white rounded-xl border border-slate-200">
        <h2 className="font-medium text-slate-800 px-4 py-3 border-b border-slate-100">Customers</h2>
        <table className="w-full text-sm">
          <thead className="text-slate-500 text-left">
            <tr>
              <th className="px-4 py-2">Name</th>
              <th className="px-4 py-2">Phone</th>
              <th className="px-4 py-2 text-right">Outstanding</th>
            </tr>
          </thead>
          <tbody>
            {customers.length === 0 && (
              <tr><td colSpan="3" className="px-4 py-6 text-center text-slate-400">
                No customers yet. Add one above to get started.
              </td></tr>
            )}
            {customers.map((c) => (
              <tr key={c.id} className="border-t border-slate-100 hover:bg-slate-50">
                <td className="px-4 py-2">
                  <Link to={`/customers/${c.id}`} className="text-emerald-700 hover:underline">{c.name}</Link>
                </td>
                <td className="px-4 py-2 text-slate-500">{c.phone || '-'}</td>
                <td className={`px-4 py-2 text-right font-medium ${Number(c.outstanding) > 0 ? 'text-rose-600' : 'text-emerald-600'}`}>
                  Rs.{c.outstanding}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </main>
  )
}
