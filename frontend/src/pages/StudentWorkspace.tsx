import FingerprintJS from '@fingerprintjs/fingerprintjs';
import { Html5Qrcode } from 'html5-qrcode';
import {
  IconCheck,
  IconClock,
  IconDeviceMobile,
  IconRotate,
  IconScan,
  IconUser,
  IconX,
} from '@tabler/icons-react';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Navigate } from 'react-router-dom';
import { api, type HistoryItem, type StudentStats } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { AppNav } from '../components/attendiq/AppNav';
import { InfoBar } from '../components/attendiq/InfoBar';
import { MetricGrid } from '../components/attendiq/MetricGrid';
import { ScannerBox } from '../components/attendiq/ScannerBox';

type ScanPhase = 'idle' | 'scanning' | 'confirm' | 'success';

export function StudentWorkspace() {
  const { role, user } = useAuth();
  const [phase, setPhase] = useState<ScanPhase>('idle');
  const [pendingToken, setPendingToken] = useState<string | null>(null);
  const [indexNumber, setIndexNumber] = useState('');
  const [fingerprint, setFingerprint] = useState('');
  const [error, setError] = useState('');
  const [successCourse, setSuccessCourse] = useState('');
  const [markTime, setMarkTime] = useState('');
  const scannerRef = useRef<Html5Qrcode | null>(null);
  const readerId = 'qr-reader-student';

  const { data: stats } = useQuery({
    queryKey: ['student-stats'],
    queryFn: async () => (await api.get<StudentStats>('/student/stats')).data,
    enabled: role === 'STUDENT',
  });

  const { data: history, refetch: refetchHistory } = useQuery({
    queryKey: ['history'],
    queryFn: async () => (await api.get<HistoryItem[]>('/attendance/history')).data,
    enabled: role === 'STUDENT',
  });

  useEffect(() => {
    if (user?.indexNumber) setIndexNumber(user.indexNumber);
  }, [user]);

  useEffect(() => {
    FingerprintJS.load().then((fp) => fp.get()).then((r) => setFingerprint(r.visitorId));
  }, []);

  useEffect(() => {
    return () => {
      scannerRef.current?.stop().catch(() => undefined);
    };
  }, []);

  if (role !== 'STUDENT') return <Navigate to="/" replace />;

  async function startScanner() {
    setError('');
    setPhase('scanning');
    const scanner = new Html5Qrcode(readerId);
    scannerRef.current = scanner;
    try {
      await scanner.start(
        { facingMode: 'environment' },
        { fps: 10, qrbox: { width: 200, height: 200 } },
        (decoded) => {
          setPendingToken(decoded);
          scanner.stop().catch(() => undefined);
          setPhase('confirm');
        },
        () => undefined
      );
    } catch {
      setError('Camera access denied or unavailable');
      setPhase('idle');
    }
  }

  function cancelScan() {
    scannerRef.current?.stop().catch(() => undefined);
    setPhase('idle');
    setPendingToken(null);
  }

  async function confirmAttendance(e: FormEvent) {
    e.preventDefault();
    if (!pendingToken || !fingerprint) return;
    setError('');
    try {
      const { data } = await api.post<{ message: string; courseCode: string; attendanceTime: string }>(
        '/attendance/scan',
        { token: pendingToken, deviceFingerprint: fingerprint, indexNumber }
      );
      setSuccessCourse(data.courseCode);
      setMarkTime(new Date(data.attendanceTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }));
      setPhase('success');
      refetchHistory();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Scan failed');
      setPhase('confirm');
    }
  }

  function resetScan() {
    setPendingToken(null);
    setPhase('idle');
  }

  const chip = user ? (
    <span className="inline-flex items-center gap-1 rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1 text-xs text-slate-600">
      <IconUser size={14} />
      {user.displayName} · {user.indexNumber ?? user.emailOrCode}
    </span>
  ) : null;

  return (
    <div className="min-h-screen bg-slate-50">
      <AppNav chip={chip} />
      <main className="mx-auto max-w-6xl px-5 py-5">
        {error && (
          <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800">{error}</div>
        )}

        <MetricGrid
          metrics={[
            { label: 'Sessions', value: stats?.totalSessions ?? 0 },
            { label: 'Attended', value: stats?.attended ?? 0, valueClassName: 'text-[#3b6d11]' },
            { label: 'Missed', value: stats?.missed ?? 0, valueClassName: 'text-[#a32d2d]' },
            { label: 'Rate', value: `${stats?.ratePercent ?? 0}%` },
          ]}
        />

        <div className="grid gap-4 lg:grid-cols-2">
          <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="mb-4 text-sm font-medium">Mark attendance</h2>

            {phase === 'idle' && (
              <div className="text-center">
                <p className="mb-4 text-sm text-slate-500">
                  Point your camera at the QR code on your lecturer&apos;s screen.
                </p>
                <ScannerBox />
                <button type="button" className="btn-attendiq mt-4 w-full py-2.5" onClick={startScanner}>
                  <IconScan size={16} /> Open scanner
                </button>
              </div>
            )}

            {phase === 'scanning' && (
              <div className="text-center">
                <div id={readerId} className="mx-auto max-w-[280px] overflow-hidden rounded-lg" />
                <p className="mt-3 text-xs text-slate-500">Detecting QR code…</p>
                <p className="text-[11px] text-slate-400">Validating device &amp; session</p>
                <button type="button" className="btn-attendiq-outline mt-3 w-full text-xs" onClick={cancelScan}>
                  Cancel
                </button>
              </div>
            )}

            {phase === 'confirm' && (
              <form onSubmit={confirmAttendance} className="space-y-4">
                <p className="text-sm text-[#3b6d11]">QR captured. Confirm your index number.</p>
                <div>
                  <label className="mb-1 block text-[11px] font-medium uppercase tracking-wide text-slate-500">
                    Index number
                  </label>
                  <input
                    className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                    value={indexNumber}
                    onChange={(e) => setIndexNumber(e.target.value)}
                    required
                  />
                </div>
                <button type="submit" className="btn-attendiq w-full py-2.5">
                  Confirm attendance
                </button>
                <button type="button" className="btn-attendiq-outline w-full text-xs" onClick={cancelScan}>
                  Cancel
                </button>
              </form>
            )}

            {phase === 'success' && (
              <div className="text-center py-2">
                <div className="success-ring">
                  <IconCheck size={34} className="text-[#3b6d11]" />
                </div>
                <p className="text-base font-medium">Attendance marked!</p>
                <p className="mb-3 text-sm text-slate-500">{successCourse}</p>
                <div className="mb-4 flex flex-wrap justify-center gap-2">
                  <span className="inline-flex items-center gap-1 rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1 text-xs">
                    <IconClock size={14} /> {markTime}
                  </span>
                  <span className="inline-flex items-center gap-1 rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1 text-xs">
                    <IconDeviceMobile size={14} /> Verified
                  </span>
                </div>
                <button type="button" className="btn-attendiq-outline w-full text-sm" onClick={resetScan}>
                  <IconRotate size={16} /> Scan another
                </button>
              </div>
            )}
          </div>

          <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="mb-4 text-sm font-medium">Attendance history</h2>
            <div className="max-h-[320px] overflow-y-auto">
              <table className="w-full border-collapse text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-left text-[11px] font-medium uppercase tracking-wide text-slate-500">
                    <th className="px-2 py-2">Course</th>
                    <th className="px-2 py-2">Date</th>
                    <th className="px-2 py-2">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {history?.length === 0 && (
                    <tr>
                      <td colSpan={3} className="py-6 text-center text-slate-500">
                        No records yet
                      </td>
                    </tr>
                  )}
                  {history?.map((h) => (
                    <tr key={`${h.sessionId}-${h.attendanceTime}`} className="border-b border-slate-100">
                      <td className="px-2 py-2">{h.courseCode}</td>
                      <td className="px-2 py-2 text-slate-500">
                        {new Date(h.attendanceTime).toLocaleDateString(undefined, {
                          month: 'short',
                          day: 'numeric',
                        })}
                      </td>
                      <td className="px-2 py-2">
                        {h.status === 'PRESENT' ? (
                          <span className="badge-present">
                            <IconCheck size={14} /> Present
                          </span>
                        ) : (
                          <span className="badge-absent">
                            <IconX size={14} /> Absent
                          </span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <InfoBar variant="warning">
              Attendance below 75% may affect your exam eligibility. See your advisor.
            </InfoBar>
          </div>
        </div>
      </main>
    </div>
  );
}
