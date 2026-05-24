import {
  IconArrowRight,
  IconBolt,
  IconChalkboard,
  IconUser,
} from '@tabler/icons-react';
import { useState, type FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import type { UserRole } from '../api/client';
import { AppNav } from '../components/attendiq/AppNav';
import { Tabs } from '../components/attendiq/Tabs';

export function AuthPage() {
  const { role, loading, login, registerStudent, registerLecturer } = useAuth();
  const navigate = useNavigate();
  const [userType, setUserType] = useState<UserRole>('STUDENT');
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [name, setName] = useState('');
  const [identifier, setIdentifier] = useState('');
  const [indexOrCode, setIndexOrCode] = useState('');
  const [password, setPassword] = useState('');
  const [departmentCode, setDepartmentCode] = useState('CSC');
  const [error, setError] = useState('');

  if (!loading && role === 'STUDENT') return <Navigate to="/student" replace />;
  if (!loading && role === 'LECTURER') return <Navigate to="/lecturer" replace />;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    try {
      if (mode === 'login') {
        const id = userType === 'LECTURER' ? indexOrCode || identifier : identifier;
        await login(id, password, userType);
      } else if (userType === 'STUDENT') {
        await registerStudent({
          name,
          email: identifier,
          indexNumber: indexOrCode,
          departmentCode,
          password,
        });
      } else {
        await registerLecturer({
          name,
          lecturerCode: indexOrCode,
          departmentCode,
          password,
        });
      }
      navigate(userType === 'LECTURER' ? '/lecturer' : '/student');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Authentication failed');
    }
  }

  async function demo(type: UserRole) {
    setError('');
    try {
      if (type === 'STUDENT') {
        await login('student@ucc.edu.gh', 'student123', 'STUDENT');
        navigate('/student');
      } else {
        await login('LEC001', 'lecturer123', 'LECTURER');
        navigate('/lecturer');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Demo login failed — is the API running?');
    }
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <AppNav showAuthChip />
      <div className="mx-auto max-w-md px-5 py-9">
        <div className="mb-6">
          <h1 className="text-xl font-medium text-slate-900">Sign in to AttendIQ</h1>
          <p className="mt-1 text-sm text-slate-500">Fraud-proof attendance powered by dynamic QR</p>
        </div>

        {error && (
          <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800">{error}</div>
        )}

        <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="mb-4">
            <label className="mb-2 block text-[11px] font-medium uppercase tracking-wide text-slate-500">
              I am a
            </label>
            <Tabs
              value={userType}
              onChange={setUserType}
              options={[
                { id: 'STUDENT', label: (<><IconUser size={16} /> Student</>) },
                { id: 'LECTURER', label: (<><IconChalkboard size={16} /> Lecturer</>) },
              ]}
            />
          </div>

          <div className="mb-4">
            <Tabs
              value={mode}
              onChange={setMode}
              options={[
                { id: 'login', label: 'Login' },
                { id: 'register', label: 'Register' },
              ]}
            />
          </div>

          <form onSubmit={handleSubmit} className="space-y-3">
            {mode === 'register' && (
              <>
                <div>
                  <label className="mb-1 block text-[11px] font-medium uppercase tracking-wide text-slate-500">
                    Full name
                  </label>
                  <input
                    className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    required
                  />
                </div>
                <div>
                  <label className="mb-1 block text-[11px] font-medium uppercase tracking-wide text-slate-500">
                    {userType === 'STUDENT' ? 'Index number' : 'Lecturer code'}
                  </label>
                  <input
                    className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                    value={indexOrCode}
                    onChange={(e) => setIndexOrCode(e.target.value)}
                    required
                  />
                </div>
                <div>
                  <label className="mb-1 block text-[11px] font-medium uppercase tracking-wide text-slate-500">
                    Department code
                  </label>
                  <input
                    className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                    value={departmentCode}
                    onChange={(e) => setDepartmentCode(e.target.value)}
                    required
                  />
                </div>
              </>
            )}

            {mode === 'login' && userType === 'LECTURER' ? (
              <div>
                <label className="mb-1 block text-[11px] font-medium uppercase tracking-wide text-slate-500">
                  Lecturer code
                </label>
                <input
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                  value={indexOrCode}
                  onChange={(e) => setIndexOrCode(e.target.value)}
                  required
                />
              </div>
            ) : (
              <div>
                <label className="mb-1 block text-[11px] font-medium uppercase tracking-wide text-slate-500">
                  Institutional email
                </label>
                <input
                  type="email"
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                  placeholder="student@ucc.edu.gh"
                  value={identifier}
                  onChange={(e) => setIdentifier(e.target.value)}
                  required
                />
              </div>
            )}

            <div>
              <label className="mb-1 block text-[11px] font-medium uppercase tracking-wide text-slate-500">
                Password
              </label>
              <input
                type="password"
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                minLength={mode === 'register' ? 8 : undefined}
              />
            </div>

            <button type="submit" className="btn-attendiq w-full py-2.5">
              Continue <IconArrowRight size={16} />
            </button>
          </form>
        </div>

        <div className="mt-3 flex gap-2">
          <button type="button" className="btn-attendiq-outline flex-1 text-xs" onClick={() => demo('STUDENT')}>
            <IconBolt size={14} /> Demo Student
          </button>
          <button type="button" className="btn-attendiq-outline flex-1 text-xs" onClick={() => demo('LECTURER')}>
            <IconBolt size={14} /> Demo Lecturer
          </button>
        </div>
        <p className="mt-3 text-center text-[11px] text-slate-500">
          Use demo buttons to explore without signing in
        </p>
      </div>
    </div>
  );
}
