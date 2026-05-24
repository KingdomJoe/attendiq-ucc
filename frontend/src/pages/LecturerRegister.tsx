import { useState, type FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Alert, Button, Card, Input, Layout } from '../components/Layout';

export function LecturerRegister() {
  const { registerLecturer, role } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    name: '',
    lecturerCode: '',
    departmentCode: 'CSC',
    password: '',
  });
  const [error, setError] = useState('');

  if (role === 'LECTURER') return <Navigate to="/lecturer/dashboard" replace />;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    try {
      await registerLecturer(form);
      navigate('/lecturer/dashboard');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed');
    }
  }

  return (
    <Layout title="Lecturer registration">
      <Card className="max-w-md mx-auto">
        <h2 className="mb-4 text-xl font-semibold">Register</h2>
        {error && <Alert message={error} />}
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input label="Full name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          <Input label="Lecturer code" value={form.lecturerCode} onChange={(e) => setForm({ ...form, lecturerCode: e.target.value })} required />
          <Input label="Department code" value={form.departmentCode} onChange={(e) => setForm({ ...form, departmentCode: e.target.value })} required />
          <Input label="Password (min 8)" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required minLength={8} />
          <Button type="submit" className="w-full">Register</Button>
        </form>
        <p className="mt-4 text-center text-sm text-slate-600">
          <Link to="/lecturer/login" className="text-blue-800 underline">Login</Link>
        </p>
      </Card>
    </Layout>
  );
}
