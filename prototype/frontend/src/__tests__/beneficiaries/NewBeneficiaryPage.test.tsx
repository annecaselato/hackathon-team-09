import { render, screen, fireEvent } from '@testing-library/react'
import '@testing-library/jest-dom'
import NewBeneficiaryPage from '@/app/beneficiaries/new/page'

// Mock next/navigation
jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: jest.fn(), back: jest.fn() }),
}))

// Mock API
jest.mock('@/lib/api', () => ({
  beneficiaryApi: {
    create: jest.fn().mockResolvedValue({ id: 1, cpf: '11144477735' }),
  },
}))

describe('NewBeneficiaryPage', () => {
  it('renders the registration form', () => {
    render(<NewBeneficiaryPage />)
    expect(screen.getByText('Novo Beneficiário')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('000.000.000-00')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('Nome Sobrenome')).toBeInTheDocument()
  })

  it('shows submit button', () => {
    render(<NewBeneficiaryPage />)
    expect(screen.getByRole('button', { name: /cadastrar/i })).toBeInTheDocument()
  })

  it('has UF select with options', () => {
    render(<NewBeneficiaryPage />)
    const select = screen.getByRole('combobox') as HTMLSelectElement
    expect(select).toBeInTheDocument()
    // SP should be an option
    expect(screen.getByText('SP')).toBeInTheDocument()
  })
})
