import { IconQrcode } from '@tabler/icons-react';

export function ScannerBox({
  children,
  showLine = false,
}: {
  children?: React.ReactNode;
  showLine?: boolean;
}) {
  return (
    <div className="scanner-box">
      <span className="scanner-corner left-2 top-2 border-l-2 border-t-2" />
      <span className="scanner-corner right-2 top-2 border-r-2 border-t-2" />
      <span className="scanner-corner bottom-2 left-2 border-b-2 border-l-2" />
      <span className="scanner-corner bottom-2 right-2 border-b-2 border-r-2" />
      {showLine && <div className="scan-line" />}
      {children ?? <IconQrcode size={52} className="text-slate-300" stroke={1.25} />}
    </div>
  );
}
