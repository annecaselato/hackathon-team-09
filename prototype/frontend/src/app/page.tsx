export default function HomePage() {
  return (
    <div className="text-center py-16">
      <h1 className="text-4xl font-bold text-blue-800 mb-4">SIFAP 2.0</h1>
      <p className="text-gray-600 mb-8">Sistema de Pagamento de Benefícios Sociais — Modernizado</p>
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 max-w-2xl mx-auto">
        <a href="/beneficiaries"
          className="bg-white rounded-xl shadow p-6 hover:shadow-md transition text-left border border-gray-100">
          <div className="text-2xl mb-2">👤</div>
          <div className="font-semibold">Beneficiários</div>
          <div className="text-sm text-gray-500 mt-1">Cadastro e consulta</div>
        </a>
        <a href="/payments"
          className="bg-white rounded-xl shadow p-6 hover:shadow-md transition text-left border border-gray-100">
          <div className="text-2xl mb-2">💳</div>
          <div className="font-semibold">Pagamentos</div>
          <div className="text-sm text-gray-500 mt-1">Histórico e status</div>
        </a>
        <a href="/audit"
          className="bg-white rounded-xl shadow p-6 hover:shadow-md transition text-left border border-gray-100">
          <div className="text-2xl mb-2">🔒</div>
          <div className="font-semibold">Auditoria</div>
          <div className="text-sm text-gray-500 mt-1">Trilha imutável — Lei 8159</div>
        </a>
      </div>
    </div>
  )
}
