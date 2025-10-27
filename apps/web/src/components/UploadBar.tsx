import { useState, useRef } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { uploadFile } from '../api'
import { Upload } from 'lucide-react'
import clsx from 'clsx'

export default function UploadBar() {
  const qc = useQueryClient()
  const [isOver, setIsOver] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  const m = useMutation({
    mutationFn: uploadFile,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['files'] })
  })

  function onFiles(files: FileList | null) {
    if (!files || files.length === 0) return
    // Queue sequentially for clarity
    ;(async () => {
      for (const f of Array.from(files)) {
        await m.mutateAsync(f)
      }
    })()
  }

  return (
    <div
      className={clsx(
        'card p-4 border-dashed transition',
        isOver ? 'border-slate-900 bg-slate-50' : 'border-slate-200'
      )}
      onDragOver={(e) => { e.preventDefault(); setIsOver(true) }}
      onDragLeave={() => setIsOver(false)}
      onDrop={(e) => {
        e.preventDefault()
        setIsOver(false)
        onFiles(e.dataTransfer.files)
      }}
    >
      <div className="row">
        <div>
          <div className="font-medium">Upload files</div>
          <div className="text-sm text-slate-600">
            Drag & drop here, or choose files to upload (encrypted at rest).
          </div>
        </div>
        <button
          className="btn"
          onClick={() => inputRef.current?.click()}
          disabled={m.isPending}
        >
          <Upload size={16} /> Choose files
        </button>
        <input
          ref={inputRef}
          type="file"
          multiple
          className="hidden"
          onChange={(e) => onFiles(e.target.files)}
        />
      </div>

      {m.isPending && (
        <div className="mt-3 text-sm text-slate-700">Uploading…</div>
      )}
    </div>
  )
}
