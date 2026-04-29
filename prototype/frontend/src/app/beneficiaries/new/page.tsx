'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { beneficiaryApi, type CreateBeneficiaryInput } from '@/lib/api'

const UF_LIST = ['AC','AL','AP','AM','BA','CE','DF','ES','GO','MA','MT','MS','MG',
  'PA','PB','PR','PE','PI','RJ','RN','RS','RO','RR','SC','SP','SE','TO']

export default function NewBeneficiaryPage() {
  const router = useRouter()
  const [form, setForm] = useState<Partial<CreateBeneficiaryInput>>({ status: 'A' })
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  function handleChange(e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) {
    setForm(f => ({ ...f, [e.target.name]: e.target.value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await beneficiaryApi.create(form as CreateBeneficiaryInput)
      router.push('/beneficiaries')
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Erro ao cadastrar')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-lg mx-auto">
      <div className="flex items-center gap-3 mb-6">
        <button onClick={() => router.back()} className="text-gray-500 hover:text-gray-800">←</button>
        <h1 className="text-2xl font-bold">Novo Beneficiário</h1>
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">{error}</div>
      )}

      <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow p-6 space-y-4">
        <Field label="CPF" name="cpf" placeholder="000.000.000-00"
          required onChange={handleChange} />
        <Field label="Nome Completo" name="nome" placeholder="Nome Sobrenome"
          required onChange={handleChange} />
        <Field label="Data de Nascimento" name="dtNascimento" type="date"
          required onChange={handleChange} />

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">UF</label>
          <select name="uf" required onChange={handleChange}
            className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
            <option value="">Selecione...</option>
            {UF_LIST.map(uf => <option key={uf} value={uf}>{uf}</option>)}
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Programa</label>
          <select name="codPrograma" onChange={handleChange}
            className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
            <option value="">Nenhum</option>
            <option value="A">Programa A</option>
            <option value="B">Programa B</option>
            <option value="C">Programa C</option>
          </select>
        </div>

        <button type="submit" disabled={loading}
          className="w-full bg-blue-700 text-white py-2 rounded-lg text-sm hover:bg-blue-800 transition disabled:opacity-50">
          {loading ? 'Cadastrando...' : 'Cadastrar Beneficiário'}
        </button>
      </form>
    </div>
  )
}

function Field({ label, name, type = 'text', placeholder, required, onChange }: {
  label: string
  name: string
  type?: string
  placeholder?: string
  required?: boolean
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void
}) {
  return (
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
      <input
        type={type} name={name} placeholder={placeholder} required={required}
        onChange={onChange}
        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
      />
    </div>
  )
}
