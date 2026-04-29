'use client'

import { useEffect, useState } from 'react'
import { useRouter, useParams } from 'next/navigation'
import { beneficiaryApi, type Beneficiary } from '@/lib/api'

export default function BeneficiaryDetailPage() {
  const params = useParams<{ id: string }>()
  const router = useRouter()
  const [beneficiary, setBeneficiary] = useState<Beneficiary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    beneficiaryApi.getById(Number(params.id))
      .then(setBeneficiary)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }, [params.id])

  if (loading) return <div className="text-center py-16 text-gray-400">Carregando...</div>
  if (error) return <div className="text-red-600 p-4">{error}</div>
  if (!beneficiary) return null

  return (
    <div className="max-w-lg mx-auto">
      <div className="flex items-center gap-3 mb-6">
        <button onClick={() => router.back()} className="text-gray-500 hover:text-gray-800">←</button>
        <h1 className="text-2xl font-bold">Detalhes do Beneficiário</h1>
      </div>

      <div className="bg-white rounded-xl shadow p-6 space-y-4">
        <DetailRow label="ID" value={String(beneficiary.id)} />
        <DetailRow label="CPF" value={formatCpf(beneficiary.cpf)} mono />
        <DetailRow label="Nome" value={beneficiary.nome} />
        <DetailRow label="Data de Nascimento" value={beneficiary.dtNascimento} />
        <DetailRow label="UF" value={beneficiary.uf} />
        <DetailRow label="Programa" value={beneficiary.codPrograma ?? '—'} />
        <DetailRow label="Status" value={beneficiary.status} />
        <DetailRow label="Cadastrado em" value={new Date(beneficiary.criadoEm).toLocaleString('pt-BR')} />
      </div>

      <div className="mt-4 flex gap-3">
        <button
          onClick={() => router.push(`/payments?cpf=${beneficiary.cpf}`)}
          className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg text-sm hover:bg-gray-200 transition">
          Ver Pagamentos
        </button>
        <button
          onClick={() => router.push(`/audit/entity/BENF/${beneficiary.id}`)}
          className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg text-sm hover:bg-gray-200 transition">
          Ver Auditoria
        </button>
      </div>
    </div>
  )
}

function DetailRow({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="flex justify-between border-b border-gray-50 pb-2 last:border-0">
      <span className="text-sm text-gray-500">{label}</span>
      <span className={`text-sm font-medium ${mono ? 'font-mono' : ''}`}>{value}</span>
    </div>
  )
}

function formatCpf(cpf: string) {
  if (cpf.length === 11)
    return `${cpf.slice(0,3)}.${cpf.slice(3,6)}.${cpf.slice(6,9)}-${cpf.slice(9)}`
  return cpf
}
