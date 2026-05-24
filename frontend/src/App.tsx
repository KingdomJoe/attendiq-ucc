import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider, useAuth } from './auth/AuthContext';
import { AuthPage } from './pages/AuthPage';
import { LecturerWorkspace } from './pages/LecturerWorkspace';
import { StudentWorkspace } from './pages/StudentWorkspace';

const queryClient = new QueryClient();

function ProtectedRoute({
  children,
  allowed,
}: {
  children: React.ReactNode;
  allowed: 'STUDENT' | 'LECTURER';
}) {
  const { role, loading } = useAuth();
  if (loading) return <div className="p-8 text-center text-slate-600">Loading…</div>;
  if (role !== allowed) return <Navigate to="/" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<AuthPage />} />
            <Route
              path="/lecturer"
              element={
                <ProtectedRoute allowed="LECTURER">
                  <LecturerWorkspace />
                </ProtectedRoute>
              }
            />
            <Route
              path="/student"
              element={
                <ProtectedRoute allowed="STUDENT">
                  <StudentWorkspace />
                </ProtectedRoute>
              }
            />
            {/* Legacy redirects */}
            <Route path="/student/login" element={<Navigate to="/" replace />} />
            <Route path="/student/register" element={<Navigate to="/" replace />} />
            <Route path="/student/scan" element={<Navigate to="/student" replace />} />
            <Route path="/student/history" element={<Navigate to="/student" replace />} />
            <Route path="/lecturer/login" element={<Navigate to="/" replace />} />
            <Route path="/lecturer/register" element={<Navigate to="/" replace />} />
            <Route path="/lecturer/dashboard" element={<Navigate to="/lecturer" replace />} />
            <Route path="/lecturer/session/:id" element={<Navigate to="/lecturer" replace />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}
