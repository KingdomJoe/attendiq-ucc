import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export function Layout({ children, title }: { children: React.ReactNode; title?: string }) {
  const { user, logout, role } = useAuth();

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white shadow-sm">
        <div className="mx-auto flex max-w-4xl items-center justify-between px-4 py-4">
          <div>
            <Link to="/" className="text-lg font-semibold text-blue-800">
              Smart Attendance System
            </Link>
            {title && <p className="text-sm text-slate-600">{title}</p>}
          </div>
          {user && (
            <div className="flex items-center gap-4 text-sm">
              <span className="text-slate-700">
                {user.displayName} ({role})
              </span>
              <button
                type="button"
                onClick={logout}
                className="rounded-lg border border-slate-300 px-3 py-1 hover:bg-slate-100"
              >
                Logout
              </button>
            </div>
          )}
        </div>
      </header>
      <main className="mx-auto max-w-4xl px-4 py-8">{children}</main>
    </div>
  );
}

export function Card({ children, className = '' }: { children: React.ReactNode; className?: string }) {
  return (
    <div className={`rounded-xl border border-slate-200 bg-white p-6 shadow-sm ${className}`}>
      {children}
    </div>
  );
}

export function Button({
  children,
  variant = 'primary',
  className = '',
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' | 'danger' }) {
  const base = 'rounded-lg px-4 py-2 font-medium transition disabled:opacity-50';
  const styles =
    variant === 'primary'
      ? 'bg-blue-800 text-white hover:bg-blue-900'
      : variant === 'danger'
        ? 'bg-red-600 text-white hover:bg-red-700'
        : 'border border-slate-300 bg-white hover:bg-slate-50';
  return (
    <button type="button" className={`${base} ${styles} ${className}`} {...props}>
      {children}
    </button>
  );
}

export function Input({
  label,
  ...props
}: React.InputHTMLAttributes<HTMLInputElement> & { label: string }) {
  return (
    <label className="block text-sm">
      <span className="mb-1 block font-medium text-slate-700">{label}</span>
      <input
        className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-blue-600 focus:outline-none focus:ring-1 focus:ring-blue-600"
        {...props}
      />
    </label>
  );
}

export function Alert({ message, type = 'error' }: { message: string; type?: 'error' | 'success' }) {
  return (
    <div
      className={`mb-4 rounded-lg px-4 py-3 text-sm ${
        type === 'error' ? 'bg-red-50 text-red-800' : 'bg-green-50 text-green-800'
      }`}
    >
      {message}
    </div>
  );
}
