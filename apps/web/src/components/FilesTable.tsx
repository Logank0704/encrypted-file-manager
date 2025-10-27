import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { listFiles, deleteFile, renameFile, downloadUrl } from '../api'
import type { FileItem } from '../types'
import { Download, Pencil, Trash2 } from 'lucide-react'
import { fmtSize } from '../utils'
import { formatDistanceToNow } from 'date-fns'

export default function FilesTable() {
  const qc = useQueryClient()

  const filesQ = useQuery({
    queryKey: ['files'],
    queryFn: listFiles
  })

  const del = useMutation({
    mutationFn: (id: string) => deleteFile(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['files'] })
  })

  const ren = useMutation({
    mutationFn: ({ id, name }: { id: string; name: string }) => renameFile(id, name),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['files'] })
  })

  if (filesQ.isLoading) {
    return <div className="card p-6 text-slate-600">Loading files…</div>
  }

  const items = (filesQ.data ?? []) as FileItem[]

  if (items.length === 0) {
    return (
      <div className="card p-10 text-center text-slate-600">
        No files yet — drop some above ✨
      </div>
    )
  }

  return (
    <div className="card overflow-hidden">
      <table className="w-full text-sm">
        <thead className="bg-slate-50 border-b">
          <tr className="text-left text-slate-600">
            <th className="p-3">Name</th>
            <th className="p-3">Type</th>
            <th className="p-3">Size</th>
            <th className="p-3">Uploaded</th>
            <th className="p-3 text-right">Actions</th>
          </tr>
        </thead>
        <tbody>
          {items.map((f) => (
            <tr key={f.id} className="border-b last:border-0">
              <td className="p-3 font-medium">{f.name}</td>
              <td className="p-3"><span className="badge">{f.contentType}</span></td>
              <td className="p-3">{fmtSize(f.size)}</td>
              <td className="p-3">{formatDistanceToNow(new Date(f.createdAt), { addSuffix: true })}</td>
              <td className="p-3">
                <div className="flex justify-end gap-2">
                  <a className="btn-secondary px-2 py-1" href={downloadUrl(f.id)}>
                    <Download size={16} />
                  </a>
                  <button
                    className="btn-secondary px-2 py-1"
                    onClick={async () => {
                      const name = prompt('Rename file to:', f.name)
                      if (name && name.trim() && name !== f.name) {
                        await ren.mutateAsync({ id: f.id, name })
                      }
                    }}
                  >
                    <Pencil size={16} />
                  </button>
                  <button
                    className="btn-secondary px-2 py-1"
                    onClick={async () => {
                      if (confirm(`Delete "${f.name}"?`)) {
                        await del.mutateAsync(f.id)
                      }
                    }}
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
