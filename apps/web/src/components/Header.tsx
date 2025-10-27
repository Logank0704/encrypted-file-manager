import { useQuery } from '@tanstack/react-query'
import { getHealth } from '../api'
import { Cloud, Shield } from 'lucide-react'

export default function Header() {
  const { data, isLoading } = useQuery({
    queryKey: ['health'],
    queryFn: getHealth,
    refetchInterval: 15_000
  })

  return (
    <header className="border-b bg-white">
      <div className="container py-4 row">
        <div className="flex items-center gap-3">
          <Shield className="text-slate-900" size={22} />
          <h1 className="text-lg font-semibold tracking-tight">
            Encrypted File Manager
          </h1>
        </div>
        <div className="flex items-center gap-3">
          <span className="badge">{isLoading ? 'checking…' : data === 'ok' ? 'API: online' : 'API: unknown'}</span>
          <span className="badge"><Cloud className="mr-1" size={14}/> Storage: S3</span>
        </div>
      </div>
    </header>
  )
}
