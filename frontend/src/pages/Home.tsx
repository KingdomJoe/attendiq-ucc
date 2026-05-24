import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Card, Layout } from '../components/Layout';

export function Home() {
  const { role, loading } = useAuth();

  if (loading) return <Layout>Loading...</Layout>;
  if (role === 'STUDENT') return <Navigate to="/student/scan" replace />;
  if (role === 'LECTURER') return <Navigate to="/lecturer/dashboard" replace />;

  return (
    <Layout title="University of Cape Coast">
      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <h2 className="mb-2 text-xl font-semibold text-blue-800">Student</h2>
          <p className="mb-4 text-slate-600">Scan QR codes and view attendance history.</p>
          <div className="flex gap-3">
            <Link
              to="/student/login"
              className="rounded-lg bg-blue-800 px-4 py-2 text-white hover:bg-blue-900"
            >
              Login
            </Link>
            <Link
              to="/student/register"
              className="rounded-lg border border-slate-300 px-4 py-2 hover:bg-slate-50"
            >
              Register
            </Link>
          </div>
        </Card>
        <Card>
          <h2 className="mb-2 text-xl font-semibold text-blue-800">Lecturer</h2>
          <p className="mb-4 text-slate-600">Create sessions and display dynamic QR codes.</p>
          <div className="flex gap-3">
            <Link
              to="/lecturer/login"
              className="rounded-lg bg-blue-800 px-4 py-2 text-white hover:bg-blue-900"
            >
              Login
            </Link>
            <Link
              to="/lecturer/register"
              className="rounded-lg border border-slate-300 px-4 py-2 hover:bg-slate-50"
            >
              Register
            </Link>
          </div>
        </Card>
      </div>
    </Layout>
  );
}
