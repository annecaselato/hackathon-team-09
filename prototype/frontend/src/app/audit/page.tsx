'use client'

import { useEffect, useState } from 'react'
import { auditApi, type AuditRecord } from '@/lib/api'

const ACTION_LABELS: Record<string, string> = {
  IN: 'Inserção', AL: 'Alteração', EX: 'Exclusão (tentativa)',
  CO: 'Confirmação', DV: 'Divergência', LG: 'Login', LO: 'Logout',
  BT: 'Batch', ER: 'Erro', AU: 'Auditoria', RE: 'Reversão',
}

const ACTION_COLORS: Record<string, string> = {
  IN: 'bg-green-100 text-green-800',
  AL: 'bg-blue-100 text-blue-800',
  EX: 'bg-red-100 text-red-800',
  CO: 'bg-emerald-100 text-emerald-800',
  BT: 'bg-indigo-100 text-indigo-800',
  ER: 'bg-orange-100 text-orange-800',
}

export default function AuditPage() {
  const [records, setRecords] = useState<AuditRecord[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  useEffect(() => {
    setLoading(true)
    auditApi
      .standard({ from: from || undefined, to: to || undefined, page })
      .then((data) => {
        setRecords(data.content)
        setTotalPages(data.totalPages)
      })
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }, [from, to, page])

  return (
    <div>
      <h1 className="text-2xl font-bold mb-2">Trilha de Auditoria</h1>
      <p className="text-sm text-gray-500 mb-6">
        Relatório padrão — exclui ações EX (tentativas de exclusão). Lei 8159 — 10 anos de retenção.
      </p>

      <div className="flex flex-wrap gap-3 mb-4 items-center">
        <div>
          <label className="text-xs text-gray-500 block mb-1">De</label>
          <input type="date" value={from} onChange={(e) => { setFrom(e.target.value); setPage(0) }}
            className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
        </div>
        <div>
          <label className="text-xs text-gray-500 block mb-1">Até</label>
          <input type="date" value={to} onChange={(e) => { setTo(e.target.value); setPage(0) }}
            className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
        </div>
      </div>

      {error && <div className="text-red-600 mb-4 p-3 bg-red-50 rounded">{error}</div>}

      <div className="bg-white rounded-xl shadow overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-600 uppercase text-xs">
            <tr>
              <th className="px-4 py-3 text-left">Data/Hora</th>
              <th className="px-4 py-3 text-left">Ação</th>
              <th className="px-4 py-3 text-left">Entidade</th>
              <th className="px-4 py-3 text-left">ID</th>
              <th className="px-4 py-3 text-left">Módulo</th>
              <th className="px-4 py-3 text-left">Usuário</th>
              <th className="px-4 py-3 text-left">Origem</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {loading ? (
              <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-400">Carregando...</td></tr>
            ) : records.length === 0 ? (
              <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-400">Nenhum registro</td></tr>
            ) : (
              records.map((r) => (
                <tr key={r.id} className="hover:bg-gray-50 transition">
                  <td className="px-4 py-3 font-mono text-xs">
                    {new Date(r.tsEvento).toLocaleString('pt-BR')}
                  </td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${ACTION_COLORS[r.codAcao] ?? 'bg-gray-100 text-gray-600'}`}>
                      {ACTION_LABELS[r.codAcao] ?? r.codAcao}
                    </span>
                  </td>
                  <td className="px-4 py-3">{r.tipoEntidade}</td>
                  <td className="px-4 py-3 font-mono">{r.idEntidade}</td>
                  <td className="px-4 py-3 text-gray-500 text-xs">{r.codModulo}</td>
                  <td className="px-4 py-3 font-mono text-xs">{r.usrEvento}</td>
                  <td className="px-4 py-3">
                    <span className={`text-xs px-1.5 py-0.5 rounded ${r.sistemaOrigem === 'L' ? 'bg-amber-100 text-amber-700' : 'bg-blue-50 text-blue-600'}`}>
                      {r.sistemaOrigem === 'L' ? 'Legacy' : 'SIFAP 2.0'}
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
          <button disabled={page === 0} onClick={() => setPage(p => p - 1)}
            className="px-3 py-1 rounded border disabled:opacity-40 hover:bg-gray-100">← Anterior</button>
          <span className="text-gray-500">Página {page + 1} de {totalPages}</span>
          <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}
            className="px-3 py-1 rounded border disabled:opacity-40 hover:bg-gray-100">Próxima →</button>
        </div>
      )}
    </div>
  )
}
