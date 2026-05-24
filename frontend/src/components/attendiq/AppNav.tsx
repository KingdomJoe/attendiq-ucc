import { IconLogout, IconQrcode, IconShieldCheck } from '@tabler/icons-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';

export function AppNav({
  chip,
  showAuthChip = false,
}: {
  chip?: React.ReactNode;
  showAuthChip?: boolean;
}) {
  const { user, logout, role } = useAuth();

  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-5 py-3">
        <Link to={user ? (role === 'LECTURER' ? '/lecturer' : '/student') : '/'} className="flex items-center gap-2 text-sm font-medium text-slate-900">
          <IconQrcode className="text-[#1d9e75]" size={20} stroke={1.75} />
          AttendIQ
        </Link>
        <div className="flex items-center gap-2">
          {showAuthChip && (
            <span className="inline-flex items-center gap-1 rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1 text-xs text-slate-600">
              <IconShieldCheck size={14} />
              Secure · JWT
            </span>
          )}
          {chip}
          {user && (
            <button type="button" onClick={logout} className="btn-attendiq-outline p-2" aria-label="Logout">
              <IconLogout size={18} />
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
