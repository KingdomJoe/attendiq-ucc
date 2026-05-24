import { IconAlertCircle, IconInfoCircle } from '@tabler/icons-react';

export function InfoBar({
  children,
  variant = 'info',
}: {
  children: React.ReactNode;
  variant?: 'info' | 'warning';
}) {
  const Icon = variant === 'warning' ? IconAlertCircle : IconInfoCircle;
  return (
    <div className="info-bar">
      <Icon size={16} className="mt-0.5 shrink-0" />
      <span>{children}</span>
    </div>
  );
}
