const API_BASE = '/api/backend'

const BASIC_AUTH_HEADER = 'Basic b3BlcmF0b3I6c2lmYXAxMjM='

async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      Authorization: BASIC_AUTH_HEADER,
      ...options?.headers,
    },
  })

  if (!res.ok) {
    const detail = await res.json().catch(() => ({ detail: res.statusText }))
    throw new Error(detail?.detail ?? `API error ${res.status}`)
  }

  return res.json() as Promise<T>
}

// ---- Beneficiary types ----

export type BeneficiaryStatus = 'A' | 'S' | 'C' | 'I' | 'D'

export interface Beneficiary {
  id: number
  cpf: string
  nome: string
  dtNascimento: string
  uf: string
  status: BeneficiaryStatus
  codPrograma: string | null
  criadoEm: string
  atualizadoEm: string | null
}

export interface BeneficiaryPage {
  content: Beneficiary[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface CreateBeneficiaryInput {
  cpf: string
  nome: string
  dtNascimento: string
  uf: string
  status?: BeneficiaryStatus
  codPrograma?: string
}

// ---- Payment types ----

export type PaymentStatus = 'P' | 'G' | 'E' | 'C' | 'D' | 'X' | 'R'

export interface Payment {
  id: number
  cpfBenef: string
  anoMesRef: string
  codPrograma: string | null
  vlrBruto: number
  vlrDescontoTotal: number
  vlrLiquido: number
  status: PaymentStatus
  criadoEm: string
}

export interface PaymentPage {
  content: Payment[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

// ---- Audit types ----

export interface AuditRecord {
  id: number
  dtEvento: string
  tsEvento: string
  codAcao: string
  codModulo: string
  tipoEntidade: string
  idEntidade: string
  usrEvento: string
  sistemaOrigem: string
}

export interface AuditPage {
  content: AuditRecord[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

// ---- API functions ----

export const beneficiaryApi = {
  list: (params?: { status?: string; nome?: string; page?: number; size?: number }) => {
    const q = new URLSearchParams()
    if (params?.status) q.set('status', params.status)
    if (params?.nome) q.set('nome', params.nome)
    if (params?.page !== undefined) q.set('page', String(params.page))
    if (params?.size !== undefined) q.set('size', String(params.size))
    return apiFetch<BeneficiaryPage>(`/api/v1/beneficiaries?${q}`)
  },

  getById: (id: number) =>
    apiFetch<Beneficiary>(`/api/v1/beneficiaries/${id}`),

  create: (input: CreateBeneficiaryInput) =>
    apiFetch<Beneficiary>('/api/v1/beneficiaries', {
      method: 'POST',
      body: JSON.stringify(input),
    }),

  updateStatus: (id: number, status: BeneficiaryStatus) =>
    apiFetch<Beneficiary>(`/api/v1/beneficiaries/${id}/status?status=${status}`, {
      method: 'PATCH',
    }),
}

export const paymentApi = {
  list: (params?: { cpf?: string; anoMesRef?: string; status?: string; page?: number }) => {
    const q = new URLSearchParams()
    if (params?.cpf) q.set('cpf', params.cpf)
    if (params?.anoMesRef) q.set('anoMesRef', params.anoMesRef)
    if (params?.status) q.set('status', params.status)
    if (params?.page !== undefined) q.set('page', String(params.page))
    return apiFetch<PaymentPage>(`/api/v1/payments?${q}`)
  },

  getById: (id: number) =>
    apiFetch<Payment>(`/api/v1/payments/${id}`),
}

export const auditApi = {
  standard: (params?: { from?: string; to?: string; page?: number }) => {
    const q = new URLSearchParams()
    if (params?.from) q.set('from', params.from)
    if (params?.to) q.set('to', params.to)
    if (params?.page !== undefined) q.set('page', String(params.page))
    return apiFetch<AuditPage>(`/api/v1/audit?${q}`)
  },
}
