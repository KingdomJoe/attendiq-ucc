import { useState, type FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Alert, Button, Card, Input, Layout } from '../components/Layout';

export function LecturerLogin() {
  const { login, role } = useAuth();
  const navigate = useNavigate();
  const [code, setCode] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  if (role === 'LECTURER') return <Navigate to="/lecturer/dashboard" replace />;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    try {
      await login(code, password, 'LECTURER');
      navigate('/lecturer/dashboard');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed');
    }
  }

  return (
    <Layout title="Lecturer login">
      <Card className="max-w-md mx-auto">
        <h2 className="mb-4 text-xl font-semibold">Lecturer Login</h2>
        {error && <Alert message={error} />}
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input label="Lecturer code" value={code} onChange={(e) => setCode(e.target.value)} required />
          <Input label="Password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          <Button type="submit" className="w-full">Login</Button>
        </form>
        <p className="mt-4 text-center text-sm text-slate-600">
          Demo: LEC001 / lecturer123
        </p>
      </Card>
    </Layout>
  );
}
