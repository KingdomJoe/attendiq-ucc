import { useState, type FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Alert, Button, Card, Input, Layout } from '../components/Layout';

export function StudentRegister() {
  const { registerStudent, role } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    name: '',
    email: '',
    indexNumber: '',
    departmentCode: 'CSC',
    password: '',
  });
  const [error, setError] = useState('');

  if (role === 'STUDENT') return <Navigate to="/student/scan" replace />;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    try {
      await registerStudent(form);
      navigate('/student/scan');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed');
    }
  }

  return (
    <Layout title="Student registration">
      <Card className="max-w-md mx-auto">
        <h2 className="mb-4 text-xl font-semibold">Register</h2>
        {error && <Alert message={error} />}
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input label="Full name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          <Input label="Email (@ucc.edu.gh)" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
          <Input label="Index number" value={form.indexNumber} onChange={(e) => setForm({ ...form, indexNumber: e.target.value })} required />
          <Input label="Department code" value={form.departmentCode} onChange={(e) => setForm({ ...form, departmentCode: e.target.value })} required />
          <Input label="Password (min 8)" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required minLength={8} />
          <Button type="submit" className="w-full">Register</Button>
        </form>
        <p className="mt-4 text-center text-sm text-slate-600">
          Have an account? <Link to="/student/login" className="text-blue-800 underline">Login</Link>
        </p>
      </Card>
    </Layout>
  );
}
