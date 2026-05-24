import { useQuery } from '@tanstack/react-query';
import { Link, Navigate } from 'react-router-dom';
import { api, type HistoryItem } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Card, Layout } from '../components/Layout';

export function StudentHistory() {
  const { role } = useAuth();

  const { data, isLoading } = useQuery({
    queryKey: ['history'],
    queryFn: async () => (await api.get<HistoryItem[]>('/attendance/history')).data,
    enabled: role === 'STUDENT',
  });

  if (role !== 'STUDENT') return <Navigate to="/student/login" replace />;

  return (
    <Layout title="Attendance history">
      <Card>
        {isLoading && <p>Loading...</p>}
        {!isLoading && data?.length === 0 && (
          <p className="text-slate-600">No attendance records yet.</p>
        )}
        <ul className="divide-y divide-slate-200">
          {data?.map((item) => (
            <li key={`${item.sessionId}-${item.attendanceTime}`} className="py-3">
              <p className="font-medium">{item.courseCode} — {item.courseName}</p>
              <p className="text-sm text-slate-600">
                {new Date(item.attendanceTime).toLocaleString()}
              </p>
            </li>
          ))}
        </ul>
      </Card>
      <p className="mt-4">
        <Link to="/student/scan" className="text-blue-800 underline">
          Back to scanner
        </Link>
      </p>
    </Layout>
  );
}
