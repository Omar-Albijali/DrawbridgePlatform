import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { LayoutDashboard, LogOut, Moon, ReceiptText, Settings, ShoppingCart, Store, Sun } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { useCart } from '../contexts/CartContext';
import { useTheme } from '../contexts/ThemeContext';
import { UserRole } from '../types';

const retailerLinks = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/marketplace', label: 'Marketplace', icon: Store },
  { to: '/inventory', label: 'Inventory', icon: Store },
  { to: '/orders', label: 'Orders', icon: ReceiptText },
] as const;

const wholesalerLinks = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/marketplace', label: 'Marketplace', icon: Store },
  { to: '/products', label: 'Products', icon: Store },
  { to: '/orders', label: 'Orders', icon: ReceiptText },
] as const;

export default function AppNavbar(): JSX.Element {
  const { dark, toggleTheme } = useTheme();
  const { isAuthenticated, user, logout } = useAuth();
  const { itemCount } = useCart();
  const navigate = useNavigate();
  const isWholesaler = user?.role === UserRole.WHOLESALER;
  const navLinks = isAuthenticated
    ? (isWholesaler ? wholesalerLinks : retailerLinks)
    : [{ to: '/marketplace', label: 'Marketplace', icon: Store }];
  const brandPath = isAuthenticated ? '/dashboard' : '/';
  const [isAccountOpen, setIsAccountOpen] = useState(false);
  const accountMenuRef = useRef<HTMLDivElement | null>(null);
  const displayName = (user?.name ?? '').trim() || 'Account';
  const avatarInitials = displayName
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('');

  useEffect(() => {
    const onPointerDown = (event: MouseEvent): void => {
      if (!accountMenuRef.current?.contains(event.target as Node)) {
        setIsAccountOpen(false);
      }
    };

    document.addEventListener('mousedown', onPointerDown);
    return () => document.removeEventListener('mousedown', onPointerDown);
  }, []);

  const handleLogout = async (): Promise<void> => {
    setIsAccountOpen(false);
    await logout();
    navigate('/login');
  };

  return (
    <nav className="glass-nav sticky top-0 z-40 px-4 py-3 sm:px-6 lg:px-8">
      <div className="mx-auto flex w-full max-w-7xl items-center justify-between gap-4">
        <div className="flex items-center gap-5 lg:gap-7">
          <Link to={brandPath} className="group inline-flex items-center gap-3 text-slate-900 dark:text-white">
            <span className="grid h-9 w-9 place-items-center rounded-xl bg-primary-500/20 text-xs font-black tracking-wider text-primary-700 dark:text-primary-300">
              DB
            </span>
            <span className="text-xl font-black tracking-tight group-hover:text-primary-600 dark:group-hover:text-primary-300">
              Drawbridge
            </span>
          </Link>

          <div className="hidden items-center gap-1 pl-2 md:flex lg:pl-4">
            {navLinks.map((link) => {
              const Icon = link.icon;

              return (
                <NavLink
                  key={link.to}
                  to={link.to}
                  className={({ isActive }) =>
                    `inline-flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-semibold transition-colors ${
                      isActive
                        ? 'bg-primary-500/15 text-slate-900 dark:text-white'
                        : 'text-slate-500 hover:text-primary-600 dark:text-slate-300 dark:hover:text-primary-300'
                    }`
                  }
                >
                  <Icon className="h-4 w-4" />
                  <span>{link.label}</span>
                </NavLink>
              );
            })}
          </div>
        </div>

        <div className="flex items-center gap-2 sm:gap-3">
          <button
            type="button"
            onClick={toggleTheme}
            className="grid h-10 w-10 place-items-center rounded-full border border-slate-300 bg-slate-100 text-slate-700 transition hover:bg-slate-200 dark:border-white/15 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800"
            aria-label={dark ? 'Switch to light theme' : 'Switch to dark theme'}
            title={dark ? 'Light mode' : 'Dark mode'}
          >
            {dark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
          </button>

          {!isAuthenticated ? (
            <>
              <Link
                to="/login"
                className="hidden rounded-lg px-3 py-2 text-sm font-semibold text-slate-700 transition hover:text-primary-600 dark:text-slate-200 dark:hover:text-primary-300 sm:inline-flex"
              >
                Sign In
              </Link>
              <Link to="/register" className="btn-primary rounded-lg px-4 py-2 text-sm">
                Get Started
              </Link>
            </>
          ) : (
            <>
              {!isWholesaler ? (
                <Link
                  to="/cart"
                  className="relative grid h-10 w-10 place-items-center rounded-full border border-slate-300 bg-white text-slate-700 transition hover:border-primary-300 hover:text-primary-700 dark:border-white/15 dark:bg-slate-900 dark:text-slate-200 dark:hover:border-primary-400/40 dark:hover:text-primary-300"
                  aria-label="Cart"
                  title="Cart"
                >
                  <ShoppingCart className="h-4 w-4" />
                  {itemCount > 0 ? (
                    <span className="absolute -right-1 -top-1 grid min-h-5 min-w-5 place-items-center rounded-full bg-primary-600 px-1 text-[10px] font-bold leading-none text-white">
                      {itemCount > 99 ? '99+' : itemCount}
                    </span>
                  ) : null}
                </Link>
              ) : null}

              <div ref={accountMenuRef} className="relative">
                <button
                  type="button"
                  onClick={() => setIsAccountOpen((open) => !open)}
                  className="inline-flex items-center gap-2 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-semibold text-slate-700 transition hover:border-primary-300 hover:text-primary-700 dark:border-white/15 dark:bg-slate-900 dark:text-slate-200 dark:hover:border-primary-400/40 dark:hover:text-primary-300"
                  aria-haspopup="menu"
                  aria-expanded={isAccountOpen}
                >
                  <span className="grid h-7 w-7 place-items-center rounded-full bg-primary-500/20 text-[11px] font-bold text-primary-700 dark:text-primary-300">
                    {avatarInitials || 'A'}
                  </span>
                  <span className="max-w-[13rem] truncate text-left">{displayName}</span>
                </button>

                {isAccountOpen ? (
                  <div className="absolute right-0 z-50 mt-2 w-52 rounded-xl border border-slate-200 bg-white p-1 shadow-lg dark:border-white/10 dark:bg-slate-900">
                    <Link
                      to="/settings"
                      onClick={() => setIsAccountOpen(false)}
                      className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-slate-700 transition hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800"
                    >
                      <Settings className="h-4 w-4 text-primary-600 dark:text-primary-300" />
                      Settings
                    </Link>
                    <button
                      type="button"
                      onClick={() => void handleLogout()}
                      className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-red-600 transition hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-500/10"
                    >
                      <LogOut className="h-4 w-4" />
                      Logout
                    </button>
                  </div>
                ) : null}
              </div>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
