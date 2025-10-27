import Header from './components/Header'
import UploadBar from './components/UploadBar'
import FilesTable from './components/FilesTable'

export default function App() {
  return (
    <div className="min-h-screen">
      <Header />
      <main className="container py-8 space-y-6">
        <UploadBar />
        <FilesTable />
      </main>
    </div>
  )
}
