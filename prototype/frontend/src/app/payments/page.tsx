import PaymentsPageClient from './payments-page-client'

interface PaymentsPageProps {
  searchParams?: Promise<{
    cpf?: string | string[]
  }>
}

export default async function PaymentsPage({ searchParams }: PaymentsPageProps) {
  const resolvedSearchParams = await searchParams
  const cpf = resolvedSearchParams?.cpf
  const initialCpfFilter = Array.isArray(cpf) ? cpf[0] ?? '' : cpf ?? ''

  return <PaymentsPageClient initialCpfFilter={initialCpfFilter} />
}
