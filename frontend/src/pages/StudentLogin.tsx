import { useState, type FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Alert, Button, Card, Input, Layout } from '../components/Layout';

export function StudentLogin() {
  const { login, role } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  if (role === 'STUDENT') return <Navigate to="/student/scan" replace />;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    try {
      await login(email, password, 'STUDENT');
      navigate('/student/scan');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed');
    }
  }

  return (
    <Layout title="Student login">
      <Card className="max-w-md mx-auto">
        <h2 className="mb-4 text-xl font-semibold">Student Login</h2>
        {error && <Alert message={error} />}
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input label="Email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          <Input label="Password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          <Button type="submit" className="w-full">Login</Button>
        </form>
        <p className="mt-4 text-center text-sm text-slate-600">
          No account? <Link to="/student/register" className="text-blue-800 underline">Register</Link>
        </p>
      </Card>
    </Layout>
  );
}
