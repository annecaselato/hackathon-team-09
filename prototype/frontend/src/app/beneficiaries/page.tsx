'use client'

import { useEffect, useState } from 'react'
import { beneficiaryApi, type Beneficiary, type BeneficiaryStatus } from '@/lib/api'
import Link from 'next/link'

const STATUS_LABELS: Record<BeneficiaryStatus, string> = {
  A: 'Ativo', S: 'Suspenso', C: 'Cancelado', I: 'Inativo', D: 'Excluído',
}

const STATUS_COLORS: Record<BeneficiaryStatus, string> = {
  A: 'bg-green-100 text-green-800',
  S: 'bg-yellow-100 text-yellow-800',
  C: 'bg-red-100 text-red-800',
  I: 'bg-gray-100 text-gray-600',
  D: 'bg-red-200 text-red-900',
}

export default function BeneficiariesPage() {
  const [beneficiaries, setBeneficiaries] = useState<Beneficiary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  useEffect(() => {
    setLoading(true)
    beneficiaryApi
      .list({ nome: search || undefined, page, size: 20 })
      .then((data) => {
        setBeneficiaries(data.content)
        setTotalPages(data.totalPages)
      })
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }, [search, page])

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">Beneficiários</h1>
        <Link href="/beneficiaries/new"
          className="bg-blue-700 text-white px-4 py-2 rounded-lg text-sm hover:bg-blue-800 transition">
          + Novo Cadastro
        </Link>
      </div>

      <div className="mb-4">
        <input
          type="text"
          placeholder="Buscar por nome..."
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0) }}
          className="w-full sm:w-80 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      {error && <div className="text-red-600 mb-4 p-3 bg-red-50 rounded">{error}</div>}

      <div className="bg-white rounded-xl shadow overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-600 uppercase text-xs">
            <tr>
              <th className="px-4 py-3 text-left">CPF</th>
              <th className="px-4 py-3 text-left">Nome</th>
              <th className="px-4 py-3 text-left">UF</th>
              <th className="px-4 py-3 text-left">Programa</th>
              <th className="px-4 py-3 text-left">Status</th>
              <th className="px-4 py-3 text-left">Ações</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {loading ? (
              <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">Carregando...</td></tr>
            ) : beneficiaries.length === 0 ? (
              <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">Nenhum resultado encontrado</td></tr>
            ) : (
              beneficiaries.map((b) => (
                <tr key={b.id} className="hover:bg-gray-50 transition">
                  <td className="px-4 py-3 font-mono">{formatCpf(b.cpf)}</td>
                  <td className="px-4 py-3 font-medium">{b.nome}</td>
                  <td className="px-4 py-3">{b.uf}</td>
                  <td className="px-4 py-3">{b.codPrograma ?? '—'}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_COLORS[b.status]}`}>
                      {STATUS_LABELS[b.status]}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <Link href={`/beneficiaries/${b.id}`} className="text-blue-600 hover:underline">
                      Ver
                    </Link>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="flex items-center gap-3 mt-4 justify-center text-sm">
          <button disabled={page === 0} onClick={() => setPage(p => p - 1)}
            className="px-3 py-1 rounded border disabled:opacity-40 hover:bg-gray-100">
            ← Anterior
          </button>
          <span className="text-gray-500">Página {page + 1} de {totalPages}</span>
          <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}
            className="px-3 py-1 rounded border disabled:opacity-40 hover:bg-gray-100">
            Próxima →
          </button>
        </div>
      )}
    </div>
  )
}

function formatCpf(cpf: string) {
  if (cpf.length === 11) {
    return `${cpf.slice(0,3)}.${cpf.slice(3,6)}.${cpf.slice(6,9)}-${cpf.slice(9)}`
  }
  return cpf
}
