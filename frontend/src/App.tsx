import { useState, useEffect } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import LoginPage from '@/components/auth/LoginPage'
import RegisterPage from '@/components/auth/RegisterPage'
import DashboardPage from '@/components/project/DashboardPage'
import EditorPage from '@/components/canvas/EditorPage'
import OAuthCallbackPage from '@/components/auth/OAuthCallbackPage'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuthStore()
  const [hydrated, setHydrated] = useState(useAuthStore.persist.hasHydrated)

  useEffect(() => {
    if (!hydrated) {
      const unsubscribe = useAuthStore.persist.onFinishHydration(() => setHydrated(true))
      return unsubscribe
    }
  }, [hydrated])

  if (!hydrated) return null
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
        <Route path="/" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
        <Route path="/workspaces/:workspaceId/projects/:projectId/documents/:documentId" element={<ProtectedRoute><EditorPage /></ProtectedRoute>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
