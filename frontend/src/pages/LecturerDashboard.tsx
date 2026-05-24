import { useQuery } from '@tanstack/react-query';
import { Link, Navigate } from 'react-router-dom';
import { api, type Course } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Alert, Button, Card, Layout } from '../components/Layout';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

export function LecturerDashboard() {
  const { role } = useAuth();
  const navigate = useNavigate();
  const [starting, setStarting] = useState<number | null>(null);
  const [error, setError] = useState('');

  const { data: courses, isLoading } = useQuery({
    queryKey: ['courses'],
    queryFn: async () => (await api.get<Course[]>('/courses')).data,
    enabled: role === 'LECTURER',
  });

  if (role !== 'LECTURER') return <Navigate to="/lecturer/login" replace />;

  async function startSession(courseId: number) {
    setError('');
    setStarting(courseId);
    try {
      const { data } = await api.post<{ id: number }>('/sessions', { courseId });
      navigate(`/lecturer/session/${data.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to start session');
    } finally {
      setStarting(null);
    }
  }

  return (
    <Layout title="Lecturer dashboard">
      {error && <Alert message={error} />}
      <Card>
        <h2 className="mb-4 text-xl font-semibold">Your courses</h2>
        {isLoading && <p>Loading courses...</p>}
        {!isLoading && courses?.length === 0 && (
          <p className="text-slate-600">No courses yet. Create one via API or use demo seed.</p>
        )}
        <ul className="space-y-3">
          {courses?.map((c) => (
            <li
              key={c.id}
              className="flex items-center justify-between rounded-lg border border-slate-200 p-4"
            >
              <div>
                <p className="font-medium">{c.courseCode}</p>
                <p className="text-sm text-slate-600">{c.courseName}</p>
              </div>
              <Button
                disabled={starting === c.id}
                onClick={() => startSession(c.id)}
              >
                {starting === c.id ? 'Starting...' : 'Start session'}
              </Button>
            </li>
          ))}
        </ul>
      </Card>
      <p className="mt-4 text-sm text-slate-600">
        <Link to="/" className="text-blue-800 underline">Back to home</Link>
      </p>
    </Layout>
  );
}
