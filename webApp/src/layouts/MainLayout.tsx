import { Navigate, Outlet, useLocation } from 'react-router-dom';
import AppNavbar from '../components/AppNavbar';
import { useAuth } from '../contexts/AuthContext';

interface MainLayoutProps {
  requireAuth?: boolean;
}

export default function MainLayout({ requireAuth = true }: MainLayoutProps): JSX.Element {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (requireAuth && isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center px-4 text-slate-600 dark:text-slate-300">
        <div className="flex items-center gap-3 rounded-xl border border-slate-200 bg-white/80 px-5 py-3 shadow-sm dark:border-white/10 dark:bg-slate-900/70">
          <div className="h-5 w-5 animate-spin rounded-full border-2 border-primary-500 border-t-transparent" />
          <p className="text-sm font-medium">Loading...</p>
        </div>
      </div>
    );
  }

  if (requireAuth && !isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return (
    <div className="relative min-h-screen">
      <div className="pointer-events-none fixed inset-0 bg-gradient-to-br from-primary-500/8 via-transparent to-cyan-400/10" />
      <AppNavbar />
      <main className="px-4 pb-10 pt-6 sm:px-6 lg:px-8">
        <div className="mx-auto w-full max-w-7xl">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
