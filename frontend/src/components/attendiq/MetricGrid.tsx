export interface Metric {
  label: string;
  value: string | number;
  valueClassName?: string;
}

export function MetricGrid({ metrics }: { metrics: Metric[] }) {
  return (
    <div className="mb-5 grid grid-cols-2 gap-2.5 sm:grid-cols-4">
      {metrics.map((m) => (
        <div key={m.label} className="metric-card">
          <div className={`metric-val ${m.valueClassName ?? ''}`}>{m.value}</div>
          <div className="metric-lbl">{m.label}</div>
        </div>
      ))}
    </div>
  );
}
