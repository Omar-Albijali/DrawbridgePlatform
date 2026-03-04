import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';

const defaultLinks = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/marketplace', label: 'Marketplace' },
  { to: '/orders', label: 'Orders' },
  { to: '/cart', label: 'Cart' },
  { to: '/settings', label: 'Settings' },
] as const;

export default function AppNavbar(): JSX.Element {
  const { dark, toggleTheme } = useTheme();
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async (): Promise<void> => {
    await logout();
    navigate('/login');
  };

  return (
    <nav className="app-navbar">
      <div className="app-navbar__content">
        <Link to="/dashboard" className="app-navbar__brand">
          <span className="app-navbar__logo">DB</span>
          <span>Drawbridge</span>
        </Link>

        <div className="app-navbar__links">
          {defaultLinks.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                isActive ? 'app-navbar__link app-navbar__link--active' : 'app-navbar__link'
              }
            >
              {link.label}
            </NavLink>
          ))}
        </div>

        <div className="app-navbar__actions">
          <button type="button" onClick={toggleTheme} className="app-navbar__icon-btn">
            {dark ? 'Light' : 'Dark'}
          </button>
          {!isAuthenticated ? (
            <Link to="/login" className="app-navbar__cta">
              Login
            </Link>
          ) : (
            <button type="button" onClick={handleLogout} className="app-navbar__icon-btn">
              {user?.name ?? 'Logout'}
            </button>
          )}
        </div>
      </div>
    </nav>
  );
}
