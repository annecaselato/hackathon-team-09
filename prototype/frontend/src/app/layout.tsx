import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'SIFAP 2.0',
  description: 'Sistema de Pagamento de Benefícios Sociais',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR">
      <body className="min-h-screen bg-gray-50 dark:bg-gray-900 text-gray-900 dark:text-gray-100">
        <nav className="bg-blue-800 text-white shadow-md">
          <div className="max-w-7xl mx-auto px-4 py-3 flex items-center gap-8">
            <span className="font-bold text-lg tracking-wide">SIFAP 2.0</span>
            <a href="/beneficiaries" className="hover:underline text-sm">Beneficiários</a>
            <a href="/payments" className="hover:underline text-sm">Pagamentos</a>
            <a href="/audit" className="hover:underline text-sm">Auditoria</a>
          </div>
        </nav>
        <main className="max-w-7xl mx-auto px-4 py-6">
          {children}
        </main>
      </body>
    </html>
  )
}
