import FingerprintJS from '@fingerprintjs/fingerprintjs';
import { Html5Qrcode } from 'html5-qrcode';
import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Alert, Button, Card, Input, Layout } from '../components/Layout';

export function StudentScan() {
  const { role, user } = useAuth();
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [pendingToken, setPendingToken] = useState<string | null>(null);
  const [indexNumber, setIndexNumber] = useState('');
  const [fingerprint, setFingerprint] = useState('');
  const scannerRef = useRef<Html5Qrcode | null>(null);
  const startedRef = useRef(false);

  useEffect(() => {
    if (user?.indexNumber) setIndexNumber(user.indexNumber);
  }, [user]);

  useEffect(() => {
    FingerprintJS.load().then((fp) => fp.get()).then((r) => setFingerprint(r.visitorId));
  }, []);

  useEffect(() => {
    if (role !== 'STUDENT' || startedRef.current) return;
    startedRef.current = true;
    const scanner = new Html5Qrcode('qr-reader');
    scannerRef.current = scanner;

    scanner
      .start(
        { facingMode: 'environment' },
        { fps: 10, qrbox: { width: 250, height: 250 } },
        (decoded) => {
          setPendingToken(decoded);
          scanner.stop().catch(() => undefined);
        },
        () => undefined
      )
      .catch(() => setError('Camera access denied or unavailable'));

    return () => {
      scanner.stop().catch(() => undefined);
    };
  }, [role]);

  if (role !== 'STUDENT') return <Navigate to="/student/login" replace />;

  async function confirmAttendance(e: FormEvent) {
    e.preventDefault();
    if (!pendingToken || !fingerprint) return;
    setError('');
    setSuccess('');
    try {
      const { data } = await api.post<{ message: string; courseCode: string }>('/attendance/scan', {
        token: pendingToken,
        deviceFingerprint: fingerprint,
        indexNumber,
      });
      setSuccess(`${data.message} — ${data.courseCode}`);
      setPendingToken(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Scan failed');
    }
  }

  return (
    <Layout title="Scan attendance">
      {error && <Alert message={error} />}
      {success && <Alert message={success} type="success" />}
      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <h2 className="mb-4 text-lg font-semibold">QR Scanner</h2>
          {!pendingToken && <div id="qr-reader" className="overflow-hidden rounded-lg" />}
          {pendingToken && (
            <p className="text-sm text-green-700">QR captured. Confirm your index number below.</p>
          )}
        </Card>
        <Card>
          {pendingToken ? (
            <form onSubmit={confirmAttendance} className="space-y-4">
              <h2 className="text-lg font-semibold">Verify index number</h2>
              <Input
                label="Index number"
                value={indexNumber}
                onChange={(e) => setIndexNumber(e.target.value)}
                required
              />
              <Button type="submit" className="w-full">
                Confirm attendance
              </Button>
              <Button type="button" variant="secondary" className="w-full" onClick={() => window.location.reload()}>
                Scan again
              </Button>
            </form>
          ) : (
            <p className="text-slate-600">Point your camera at the lecturer&apos;s QR code.</p>
          )}
        </Card>
      </div>
      <p className="mt-4 flex gap-4 text-sm">
        <Link to="/student/history" className="text-blue-800 underline">
          Attendance history
        </Link>
        <Link to="/" className="text-blue-800 underline">
          Home
        </Link>
      </p>
    </Layout>
  );
}
