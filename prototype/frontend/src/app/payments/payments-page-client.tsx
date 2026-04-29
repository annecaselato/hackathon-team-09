'use client'

import { useEffect, useState } from 'react'
import { paymentApi, type Payment, type PaymentStatus } from '@/lib/api'

const STATUS_LABELS: Record<PaymentStatus, string> = {
  P: 'Pendente', G: 'Gerado', E: 'Erro',
  C: 'Confirmado', D: 'Devolvido', X: 'Cancelado', R: 'Revertido',
}

const STATUS_COLORS: Record<PaymentStatus, string> = {
  P: 'bg-yellow-100 text-yellow-800',
  G: 'bg-blue-100 text-blue-800',
  E: 'bg-red-100 text-red-800',
  C: 'bg-green-100 text-green-800',
  D: 'bg-orange-100 text-orange-800',
  X: 'bg-gray-100 text-gray-600',
  R: 'bg-purple-100 text-purple-800',
}

interface PaymentsPageClientProps {
  initialCpfFilter?: string
}

export default function PaymentsPageClient({ initialCpfFilter = '' }: PaymentsPageClientProps) {
  const [payments, setPayments] = useState<Payment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [cpfFilter, setCpfFilter] = useState(initialCpfFilter)
  const [competenceFilter, setCompetenceFilter] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  useEffect(() => {
    setLoading(true)
    paymentApi
      .list({
        cpf: cpfFilter || undefined,
        anoMesRef: competenceFilter || undefined,
        page,
      })
      .then((data) => {
        setPayments(data.content)
        setTotalPages(data.totalPages)
      })
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }, [cpfFilter, competenceFilter, page])

  const formatCurrency = (v: number) =>
    v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Pagamentos</h1>

      <div className="flex flex-wrap gap-3 mb-4">
        <input
          type="text"
          placeholder="CPF (apenas dígitos)"
          value={cpfFilter}
          onChange={(e) => { setCpfFilter(e.target.value); setPage(0) }}
          className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 w-48"
        />
        <input
          type="text"
          placeholder="Competência (ex: 202604)"
          value={competenceFilter}
          onChange={(e) => { setCompetenceFilter(e.target.value); setPage(0) }}
          className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 w-48"
        />
      </div>

      {error && <div className="text-red-600 mb-4 p-3 bg-red-50 rounded">{error}</div>}

      <div className="bg-white rounded-xl shadow overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-600 uppercase text-xs">
            <tr>
              <th className="px-4 py-3 text-left">ID</th>
              <th className="px-4 py-3 text-left">CPF</th>
              <th className="px-4 py-3 text-left">Competência</th>
              <th className="px-4 py-3 text-right">Bruto</th>
              <th className="px-4 py-3 text-right">Desconto</th>
              <th className="px-4 py-3 text-right">Líquido</th>
              <th className="px-4 py-3 text-left">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {loading ? (
              <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-400">Carregando...</td></tr>
            ) : payments.length === 0 ? (
              <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-400">Nenhum pagamento encontrado</td></tr>
            ) : (
              payments.map((p) => (
                <tr key={p.id} className="hover:bg-gray-50 transition">
                  <td className="px-4 py-3 font-mono text-gray-400">{p.id}</td>
                  <td className="px-4 py-3 font-mono">{formatCpf(p.cpfBenef)}</td>
                  <td className="px-4 py-3">{formatCompetence(p.anoMesRef)}</td>
                  <td className="px-4 py-3 text-right">{formatCurrency(p.vlrBruto)}</td>
                  <td className="px-4 py-3 text-right text-red-600">-{formatCurrency(p.vlrDescontoTotal)}</td>
                  <td className="px-4 py-3 text-right font-medium">{formatCurrency(p.vlrLiquido)}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_COLORS[p.status]}`}>
                      {STATUS_LABELS[p.status]}
                    </span>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="flex items-center gap-3 mt-4 justify-center text-sm">
          <button disabled={page === 0} onClick={() => setPage((currentPage) => currentPage - 1)}
            className="px-3 py-1 rounded border disabled:opacity-40 hover:bg-gray-100">← Anterior</button>
          <span className="text-gray-500">Página {page + 1} de {totalPages}</span>
          <button disabled={page >= totalPages - 1} onClick={() => setPage((currentPage) => currentPage + 1)}
            className="px-3 py-1 rounded border disabled:opacity-40 hover:bg-gray-100">Próxima →</button>
        </div>
      )}
    </div>
  )
}

function formatCpf(cpf: string) {
  if (cpf.length === 11) {
    return `${cpf.slice(0, 3)}.${cpf.slice(3, 6)}.${cpf.slice(6, 9)}-${cpf.slice(9)}`
  }

  return cpf
}

function formatCompetence(aaaamm: string) {
  if (aaaamm.length === 6) {
    return `${aaaamm.slice(4, 6)}/${aaaamm.slice(0, 4)}`
  }

  return aaaamm
}