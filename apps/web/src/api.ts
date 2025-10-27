import axios from 'axios'
import type { FileItem } from './types'

const API_BASE = import.meta.env.VITE_API_BASE || '/api'

export const api = axios.create({
  baseURL: API_BASE,
  timeout: 20_000
})

// --- Endpoints ---
export async function getHealth(): Promise<'ok'> {
  const { data } = await api.get<{ status: string }>('/health')
  return data.status as 'ok'
}

export async function listFiles(): Promise<FileItem[]> {
  const { data } = await api.get<FileItem[]>('/files')
  return data
}

export async function uploadFile(file: File): Promise<FileItem> {
  const form = new FormData()
  form.append('file', file)
  const { data } = await api.post<FileItem>('/files', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return data
}

export async function renameFile(id: string, name: string): Promise<FileItem> {
  const { data } = await api.patch<FileItem>(`/files/${id}`, { name })
  return data
}

export async function deleteFile(id: string): Promise<void> {
  await api.delete(`/files/${id}`)
}

export function downloadUrl(id: string): string {
  // use the same base as axios to avoid CORS
  return `${API_BASE}/files/${id}`
}
