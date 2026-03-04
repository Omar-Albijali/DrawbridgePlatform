import { Navigate, Outlet, useLocation } from 'react-router-dom';
import AppNavbar from '../components/AppNavbar';
import { useAuth } from '../contexts/AuthContext';

export default function MainLayout(): JSX.Element {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="layout-loading">
        <div className="layout-loading__spinner" />
        <p>Loading...</p>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return (
    <div className="main-layout">
      <div className="main-layout__backdrop" />
      <AppNavbar />
      <main className="main-layout__content">
        <Outlet />
      </main>
    </div>
  );
}
