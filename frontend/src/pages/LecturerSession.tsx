import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { Link, Navigate, useParams } from 'react-router-dom';
import { api, type QrResponse, type SessionAttendance } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Alert, Button, Card, Layout } from '../components/Layout';

export function LecturerSession() {
  const { id } = useParams<{ id: string }>();
  const sessionId = Number(id);
  const { role } = useAuth();
  const [error, setError] = useState('');

  const qrQuery = useQuery({
    queryKey: ['qr', sessionId],
    queryFn: async () => (await api.get<QrResponse>(`/sessions/${sessionId}/qr`)).data,
    enabled: role === 'LECTURER' && !!sessionId,
    refetchInterval: 5000,
  });

  const attendanceQuery = useQuery({
    queryKey: ['attendance', sessionId],
    queryFn: async () =>
      (await api.get<SessionAttendance>(`/sessions/${sessionId}/attendance`)).data,
    enabled: role === 'LECTURER' && !!sessionId,
    refetchInterval: 3000,
  });

  useEffect(() => {
    if (qrQuery.error) {
      setError(qrQuery.error instanceof Error ? qrQuery.error.message : 'QR error');
    }
  }, [qrQuery.error]);

  if (role !== 'LECTURER') return <Navigate to="/lecturer/login" replace />;

  async function closeSession() {
    try {
      await api.post(`/sessions/${sessionId}/close`);
      window.location.href = '/lecturer/dashboard';
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to close');
    }
  }

  const qr = qrQuery.data;
  const attendance = attendanceQuery.data;

  return (
    <Layout title={`Session #${sessionId}`}>
      {error && <Alert message={error} />}
      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="text-center">
          <h2 className="mb-2 text-lg font-semibold">Live QR Code</h2>
          <p className="mb-4 text-sm text-slate-600">Refreshes every 5 seconds</p>
          {qr ? (
            <>
              <img
                src={`data:image/png;base64,${qr.qrImageBase64}`}
                alt="Attendance QR"
                className="mx-auto h-64 w-64 rounded-lg border border-slate-200"
              />
              <p className="mt-2 text-xs text-slate-500">
                Expires: {new Date(qr.expiresAt).toLocaleTimeString()}
              </p>
            </>
          ) : (
            <p>Loading QR...</p>
          )}
          <Button variant="danger" className="mt-4" onClick={closeSession}>
            Close session
          </Button>
        </Card>
        <Card>
          <h2 className="mb-4 text-lg font-semibold">Attendance</h2>
          <p className="mb-4 text-sm text-slate-600">
            Present: {attendance?.presentCount ?? 0} / {attendance?.enrolledCount ?? 0}
          </p>
          <div className="max-h-96 overflow-y-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200">
                  <th className="py-2">Status</th>
                  <th className="py-2">Name</th>
                  <th className="py-2">Index</th>
                </tr>
              </thead>
              <tbody>
                {attendance?.rows.map((row) => (
                  <tr key={row.studentId} className="border-b border-slate-100">
                    <td className="py-2">
                      {row.present ? (
                        <span className="text-green-600" title="Present">
                          ✓
                        </span>
                      ) : (
                        <span className="text-slate-300" title="Absent">
                          ☐
                        </span>
                      )}
                    </td>
                    <td className="py-2">{row.name}</td>
                    <td className="py-2">{row.indexNumber}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      </div>
      <p className="mt-4">
        <Link to="/lecturer/dashboard" className="text-blue-800 underline">
          Back to dashboard
        </Link>
      </p>
    </Layout>
  );
}
