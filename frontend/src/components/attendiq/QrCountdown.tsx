import { useEffect, useState } from 'react';

const CIRC = 2 * Math.PI * 104;

export function QrCountdown({
  qrImageBase64,
  token,
  expiresAt,
}: {
  qrImageBase64: string;
  token: string;
  expiresAt: string;
}) {
  const [secondsLeft, setSecondsLeft] = useState(5);

  useEffect(() => {
    const tick = () => {
      const ms = new Date(expiresAt).getTime() - Date.now();
      setSecondsLeft(Math.max(0, Math.ceil(ms / 1000)));
    };
    tick();
    const id = setInterval(tick, 250);
    return () => clearInterval(id);
  }, [expiresAt]);

  const offset = CIRC * (1 - secondsLeft / 5);

  const displayToken = token.length > 20 ? `${token.slice(0, 12)}…` : token;

  return (
    <div className="text-center">
      <div className="relative mx-auto h-[220px] w-[220px]">
        <svg className="absolute inset-0" width={220} height={220} viewBox="0 0 220 220" aria-hidden>
          <circle cx={110} cy={110} r={104} fill="none" stroke="#e2e8f0" strokeWidth={2.5} />
          <circle
            cx={110}
            cy={110}
            r={104}
            fill="none"
            stroke="#1d9e75"
            strokeWidth={2.5}
            strokeLinecap="round"
            strokeDasharray={CIRC}
            strokeDashoffset={offset}
            style={{ transformOrigin: '110px 110px', transform: 'rotate(-90deg)' }}
          />
        </svg>
        <img
          src={`data:image/png;base64,${qrImageBase64}`}
          alt="Session QR code"
          className="absolute left-[10px] top-[10px] h-[200px] w-[200px] rounded-md"
        />
      </div>
      <p className="mt-2 text-xs text-slate-500">
        Refreshing in <strong>{secondsLeft}</strong>s
      </p>
      <p className="mt-1 font-mono text-[11px] tracking-wide text-slate-400">{displayToken}</p>
    </div>
  );
}
