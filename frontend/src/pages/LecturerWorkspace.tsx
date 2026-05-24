import {
  IconChartBar,
  IconChalkboard,
  IconDownload,
  IconPlayerPlay,
  IconPlayerStop,
} from '@tabler/icons-react';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import { Navigate } from 'react-router-dom';
import {
  api,
  downloadAttendanceCsv,
  type Course,
  type CourseAnalytics,
  type LecturerStats,
  type QrResponse,
  type SessionAttendance,
  type SessionType,
} from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { AppNav } from '../components/attendiq/AppNav';
import { AttendanceTable } from '../components/attendiq/AttendanceTable';
import { InfoBar } from '../components/attendiq/InfoBar';
import { MetricGrid } from '../components/attendiq/MetricGrid';
import { QrCountdown } from '../components/attendiq/QrCountdown';

export function LecturerWorkspace() {
  const { role, user } = useAuth();
  const [activeSessionId, setActiveSessionId] = useState<number | null>(null);
  const [courseId, setCourseId] = useState<number | ''>('');
  const [sessionType, setSessionType] = useState<SessionType>('LECTURE');
  const [error, setError] = useState('');
  const [showAnalytics, setShowAnalytics] = useState(false);
  const [exportToast, setExportToast] = useState(false);
  const prevPresentRef = useRef<Set<number>>(new Set());

  const { data: courses } = useQuery({
    queryKey: ['courses'],
    queryFn: async () => (await api.get<Course[]>('/courses')).data,
    enabled: role === 'LECTURER',
  });

  const { data: stats, refetch: refetchStats } = useQuery({
    queryKey: ['lecturer-stats', activeSessionId],
    queryFn: async () =>
      (
        await api.get<LecturerStats>('/lecturer/stats', {
          params: activeSessionId ? { sessionId: activeSessionId } : {},
        })
      ).data,
    enabled: role === 'LECTURER',
    refetchInterval: activeSessionId ? 3000 : false,
  });

  const { data: qr } = useQuery({
    queryKey: ['qr', activeSessionId],
    queryFn: async () =>
      (await api.get<QrResponse>(`/sessions/${activeSessionId}/qr`)).data,
    enabled: !!activeSessionId,
    refetchInterval: 5000,
  });

  const { data: attendance } = useQuery({
    queryKey: ['attendance', activeSessionId],
    queryFn: async () =>
      (await api.get<SessionAttendance>(`/sessions/${activeSessionId}/attendance`)).data,
    enabled: !!activeSessionId,
    refetchInterval: 3000,
  });

  const { data: analytics } = useQuery({
    queryKey: ['analytics'],
    queryFn: async () => (await api.get<{ courses: CourseAnalytics[] }>('/lecturer/analytics')).data,
    enabled: showAnalytics && role === 'LECTURER',
  });

  const highlightIds = new Set<number>();
  if (attendance) {
    for (const row of attendance.rows) {
      if (row.present && !prevPresentRef.current.has(row.studentId)) {
        highlightIds.add(row.studentId);
      }
    }
    prevPresentRef.current = new Set(
      attendance.rows.filter((r) => r.present).map((r) => r.studentId)
    );
  }

  useEffect(() => {
    if (courses?.length && courseId === '') {
      setCourseId(courses[0].id);
    }
  }, [courses, courseId]);

  if (role !== 'LECTURER') return <Navigate to="/" replace />;

  async function startSession() {
    if (!courseId) return;
    setError('');
    try {
      const { data } = await api.post<{ id: number }>('/sessions', { courseId, sessionType });
      setActiveSessionId(data.id);
      prevPresentRef.current = new Set();
      refetchStats();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to start session');
    }
  }

  async function endSession() {
    if (!activeSessionId) return;
    try {
      await api.post(`/sessions/${activeSessionId}/close`);
      setActiveSessionId(null);
      refetchStats();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to end session');
    }
  }

  async function handleExport() {
    if (!activeSessionId) return;
    try {
      await downloadAttendanceCsv(activeSessionId);
      setExportToast(true);
      setTimeout(() => setExportToast(false), 2500);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Export failed');
    }
  }

  const chip = user ? (
    <span className="inline-flex items-center gap-1 rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1 text-xs text-slate-600">
      <IconChalkboard size={14} />
      {user.displayName} · {user.emailOrCode}
    </span>
  ) : null;

  return (
    <div className="min-h-screen bg-slate-50">
      <AppNav chip={chip} />
      <main className="mx-auto max-w-6xl px-5 py-5">
        {error && (
          <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800">{error}</div>
        )}
        {exportToast && (
          <div className="fixed bottom-5 left-1/2 z-50 -translate-x-1/2 rounded-full bg-[#1d9e75] px-5 py-2 text-sm text-white shadow-lg">
            Attendance exported as CSV
          </div>
        )}

        <MetricGrid
          metrics={[
            { label: 'Enrolled', value: stats?.enrolled ?? 0 },
            { label: 'Present', value: stats?.present ?? 0, valueClassName: 'text-[#3b6d11]' },
            { label: 'Absent', value: stats?.absent ?? 0, valueClassName: 'text-[#a32d2d]' },
            { label: 'Rate', value: `${stats?.ratePercent ?? 0}%` },
          ]}
        />

        <div className="grid gap-4 lg:grid-cols-2">
          <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-sm font-medium">Session control</h2>
              {activeSessionId && (
                <span className="badge-live">
                  <span className="live-dot" /> Live
                </span>
              )}
            </div>

            {!activeSessionId ? (
              <div>
                <p className="mb-4 text-sm text-slate-500">
                  Generate a dynamic QR code for your students to mark attendance in real time.
                </p>
                <div className="mb-3">
                  <label className="mb-1 block text-[11px] font-medium uppercase tracking-wide text-slate-500">
                    Course
                  </label>
                  <select
                    className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                    value={courseId}
                    onChange={(e) => setCourseId(Number(e.target.value))}
                  >
                    {courses?.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.courseCode} — {c.courseName}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="mb-4">
                  <label className="mb-1 block text-[11px] font-medium uppercase tracking-wide text-slate-500">
                    Session type
                  </label>
                  <select
                    className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                    value={sessionType}
                    onChange={(e) => setSessionType(e.target.value as SessionType)}
                  >
                    <option value="LECTURE">Lecture</option>
                    <option value="PRACTICAL">Practical</option>
                    <option value="TUTORIAL">Tutorial</option>
                  </select>
                </div>
                <button type="button" className="btn-attendiq w-full py-2.5" onClick={startSession}>
                  <IconPlayerPlay size={16} /> Start session
                </button>
              </div>
            ) : (
              <div>
                {qr && (
                  <QrCountdown
                    qrImageBase64={qr.qrImageBase64}
                    token={qr.token}
                    expiresAt={qr.expiresAt}
                  />
                )}
                <hr className="my-4 border-slate-200" />
                <button type="button" className="btn-attendiq w-full bg-red-600 border-red-600 hover:bg-red-700" onClick={endSession}>
                  <IconPlayerStop size={16} /> End session
                </button>
              </div>
            )}
          </div>

          <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-sm font-medium">Attendance</h2>
              <span className="text-xs text-slate-500">
                {attendance?.presentCount ?? 0} / {attendance?.enrolledCount ?? 0} present
              </span>
            </div>
            <div className="max-h-[310px] overflow-y-auto">
              <AttendanceTable rows={attendance?.rows ?? []} highlightIds={highlightIds} />
            </div>
            <hr className="my-4 border-slate-200" />
            <div className="flex gap-2">
              <button
                type="button"
                className="btn-attendiq-outline flex-1 text-xs"
                disabled={!activeSessionId}
                onClick={handleExport}
              >
                <IconDownload size={14} /> Export CSV
              </button>
              <button
                type="button"
                className="btn-attendiq-outline flex-1 text-xs"
                onClick={() => setShowAnalytics((v) => !v)}
              >
                <IconChartBar size={14} /> Analytics
              </button>
            </div>
            {showAnalytics && analytics && (
              <div className="mt-3 space-y-2 text-xs">
                {analytics.courses.map((c) => (
                  <div key={c.courseCode} className="rounded-lg bg-slate-50 p-2">
                    <strong>{c.courseCode}</strong> — {c.averageRatePercent}% avg · {c.sessionsHeld} sessions
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <InfoBar>
          QR codes rotate every 5 seconds and are cryptographically tied to this session, your lecturer
          code, and a server timestamp — preventing screenshot sharing or proxy attendance.
        </InfoBar>
      </main>
    </div>
  );
}
