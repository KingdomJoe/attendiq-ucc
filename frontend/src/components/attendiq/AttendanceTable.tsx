import { IconCheck } from '@tabler/icons-react';
import type { AttendanceRow } from '../../api/client';

export function AttendanceTable({
  rows,
  highlightIds,
}: {
  rows: AttendanceRow[];
  highlightIds?: Set<number>;
}) {
  if (rows.length === 0) {
    return <p className="py-6 text-center text-sm text-slate-500">No attendance yet</p>;
  }

  return (
    <table className="w-full border-collapse text-sm">
      <thead>
        <tr className="border-b border-slate-200 text-left text-[11px] font-medium uppercase tracking-wide text-slate-500">
          <th className="px-2 py-2">Student</th>
          <th className="px-2 py-2">Index</th>
          <th className="px-2 py-2">Time</th>
          <th className="px-2 py-2">Status</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr
            key={row.studentId}
            className={`border-b border-slate-100 ${highlightIds?.has(row.studentId) ? 'row-new' : ''}`}
          >
            <td className="px-2 py-2 font-medium">{row.name}</td>
            <td className="px-2 py-2 font-mono text-xs text-slate-500">{row.indexNumber}</td>
            <td className="px-2 py-2 text-slate-500">
              {row.attendanceTime
                ? new Date(row.attendanceTime).toLocaleTimeString()
                : '—'}
            </td>
            <td className="px-2 py-2">
              {row.present ? (
                <span className="badge-present">
                  <IconCheck size={14} /> Present
                </span>
              ) : (
                <span className="badge-absent">Absent</span>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
