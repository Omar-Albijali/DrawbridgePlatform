import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import {
  AlertTriangle,
  Bell,
  Cable,
  CheckCheck,
  CreditCard,
  Heart,
  LayoutDashboard,
  LifeBuoy,
  LogOut,
  Moon,
  Package,
  ReceiptText,
  Settings,
  Shield,
  ShoppingCart,
  Store,
  Sun,
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { useCart } from '../contexts/CartContext';
import { useTheme } from '../contexts/ThemeContext';
import { notificationService } from '../services/notificationService';
import {
  getNotificationTitle,
  notificationDestination,
  shortenOrderIds,
} from '../utils/notificationHelpers';
import { NotificationType, UserRole, type Notification } from '../types';

function itemStyles(type: NotificationType): { icon: JSX.Element; wrap: string; label: string; labelClass: string } {
  if (type === NotificationType.ORDER) {
    return {
      icon: <Package className="h-3.5 w-3.5 text-primary-700 dark:text-primary-300" />,
      wrap: 'bg-primary-100 ring-1 ring-primary-200 dark:bg-primary-900/35 dark:ring-primary-800/70',
      label: 'Order',
      labelClass: 'bg-primary-100 text-primary-700 dark:bg-primary-900/40 dark:text-primary-300',
    };
  }
  if (type === NotificationType.STOCK) {
    return {
      icon: <AlertTriangle className="h-3.5 w-3.5 text-amber-700 dark:text-amber-300" />,
      wrap: 'bg-amber-100 ring-1 ring-amber-200 dark:bg-amber-900/30 dark:ring-amber-800/70',
      label: 'Inventory',
      labelClass: 'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300',
    };
  }
  if (type === NotificationType.PAYMENT) {
    return {
      icon: <CreditCard className="h-3.5 w-3.5 text-emerald-700 dark:text-emerald-300" />,
      wrap: 'bg-emerald-100 ring-1 ring-emerald-200 dark:bg-emerald-900/30 dark:ring-emerald-800/70',
      label: 'Payment',
      labelClass: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300',
    };
  }
  return {
    icon: <Shield className="h-3.5 w-3.5 text-slate-700 dark:text-slate-300" />,
    wrap: 'bg-slate-100 ring-1 ring-slate-200 dark:bg-slate-800 dark:ring-slate-700',
    label: 'System',
    labelClass: 'bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-200',
  };
}

const retailerLinks = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/marketplace', label: 'Marketplace', icon: Store },
  { to: '/inventory', label: 'Inventory', icon: Store },
  { to: '/pos-integration', label: 'POS', icon: Cable },
  { to: '/orders', label: 'Orders', icon: ReceiptText },
  { to: '/support', label: 'Support', icon: LifeBuoy },
] as const;

const wholesalerLinks = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/marketplace', label: 'Marketplace', icon: Store },
  { to: '/products', label: 'Products', icon: Store },
  { to: '/orders', label: 'Orders', icon: ReceiptText },
  { to: '/support', label: 'Support', icon: LifeBuoy },
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
  const [isNotificationsOpen, setIsNotificationsOpen] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const accountMenuRef = useRef<HTMLDivElement | null>(null);
  const notificationMenuRef = useRef<HTMLDivElement | null>(null);
  const displayName = (user?.name ?? '').trim() || 'Account';

  useEffect(() => {
    const onPointerDown = (event: MouseEvent): void => {
      if (!accountMenuRef.current?.contains(event.target as Node)) {
        setIsAccountOpen(false);
      }
      if (!notificationMenuRef.current?.contains(event.target as Node)) {
        setIsNotificationsOpen(false);
      }
    };

    document.addEventListener('mousedown', onPointerDown);
    return () => document.removeEventListener('mousedown', onPointerDown);
  }, []);

  useEffect(() => {
    let cancelled = false;

    const refreshNotifications = async (): Promise<void> => {
      if (!user?.id) {
        if (!cancelled) {
          setNotifications([]);
          setUnreadCount(0);
        }
        return;
      }

      try {
        const [inbox, unread] = await Promise.all([
          notificationService.getNotifications(user.id),
          notificationService.getUnreadCount(user.id),
        ]);
        if (!cancelled) {
          setNotifications(inbox.slice(0, 6));
          setUnreadCount(unread);
        }
      } catch (_error) {
        if (!cancelled) {
          setNotifications([]);
          setUnreadCount(0);
        }
      }
    };

    void refreshNotifications();
    const interval = window.setInterval(() => {
      void refreshNotifications();
    }, 20000);

    return () => {
      cancelled = true;
      window.clearInterval(interval);
    };
  }, [user?.id]);

  const handleLogout = async (): Promise<void> => {
    setIsAccountOpen(false);
    await logout();
    navigate('/login');
  };

  const handleMarkAllRead = async (): Promise<void> => {
    if (!user?.id) {
      return;
    }
    await notificationService.markAllAsRead(user.id);
    setNotifications((prev) => prev.map((item) => ({ ...(item as object), read: true } as Notification)));
    setUnreadCount(0);
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
              <div ref={notificationMenuRef} className="relative">
                <button
                  type="button"
                  onClick={() => setIsNotificationsOpen((open) => !open)}
                  className="relative grid h-10 w-10 place-items-center rounded-full border border-slate-300 bg-white text-slate-700 transition hover:border-primary-300 hover:text-primary-700 dark:border-white/15 dark:bg-slate-900 dark:text-slate-200"
                  aria-label="Notifications"
                  title="Notifications"
                >
                  <Bell className="h-4 w-4" />
                  {unreadCount > 0 ? (
                    <span className="absolute -right-1 -top-1 grid min-h-5 min-w-5 place-items-center rounded-full bg-primary-600 px-1 text-[10px] font-bold leading-none text-white">
                      {unreadCount > 99 ? '99+' : unreadCount}
                    </span>
                  ) : null}
                </button>

                {isNotificationsOpen ? (
                  <div className="absolute right-0 z-50 mt-2 w-96 rounded-xl border border-slate-200 bg-white p-2 shadow-lg dark:border-white/10 dark:bg-slate-900">
                    <div className="mb-2 flex items-center justify-between px-2 py-1">
                      <p className="text-sm font-semibold text-slate-800 dark:text-slate-200">Notifications</p>
                      <button
                        type="button"
                        onClick={() => void handleMarkAllRead()}
                        className="inline-flex items-center gap-1 text-xs font-semibold text-primary-600 hover:text-primary-700 dark:text-primary-300 dark:hover:text-primary-200"
                      >
                        <CheckCheck className="h-3.5 w-3.5" />
                        Mark all read
                      </button>
                    </div>

                    <div className="max-h-96 space-y-1 overflow-y-auto">
                      {notifications.length === 0 ? (
                        <p className="rounded-lg px-3 py-6 text-center text-sm text-slate-500 dark:text-slate-400">No notifications yet.</p>
                      ) : (
                        notifications.map((notification) => (
                          (() => {
                            const styles = itemStyles(notification.type);
                            return (
                          <button
                            key={notification.id}
                            type="button"
                            onClick={() => {
                              void notificationService.markAsRead(notification.id);
                              setNotifications((prev) =>
                                prev.map((item) =>
                                  item.id === notification.id
                                    ? ({ ...(item as object), read: true } as Notification)
                                    : item,
                                ),
                              );
                              setUnreadCount((prev) => Math.max(0, prev - (notification.read ? 0 : 1)));
                              navigate(notificationDestination(notification));
                              setIsNotificationsOpen(false);
                            }}
                            className={`w-full rounded-lg px-3 py-2 text-left transition ${
                              notification.read
                                ? 'hover:bg-slate-100 dark:hover:bg-slate-800/70'
                                : 'bg-primary-50 hover:bg-primary-100 dark:bg-primary-500/10 dark:hover:bg-primary-500/20'
                            }`}
                          >
                            <div className="flex items-start gap-2">
                              <div className={`mt-0.5 flex h-6 w-6 items-center justify-center rounded-full ${styles.wrap}`}>
                                {styles.icon}
                              </div>
                              <div className="flex-1">
                                <div className="flex items-center gap-2">
                                  <p className={`text-sm ${notification.read ? 'text-slate-700 dark:text-slate-300' : 'font-semibold text-slate-900 dark:text-slate-100'}`}>
                                    {shortenOrderIds(getNotificationTitle(notification))}
                                  </p>
                                  <span className={`rounded-full px-1.5 py-0.5 text-[10px] font-semibold ${styles.labelClass}`}>
                                    {styles.label}
                                  </span>
                                </div>
                                <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">{shortenOrderIds(notification.message)}</p>
                                <p className="mt-1 text-[11px] text-slate-400 dark:text-slate-500">{notification.time}</p>
                              </div>
                            </div>
                          </button>
                            );
                          })()
                        ))
                      )}
                    </div>

                    <Link
                      to="/notifications"
                      onClick={() => setIsNotificationsOpen(false)}
                      className="mt-2 block rounded-lg px-3 py-2 text-center text-sm font-semibold text-primary-600 hover:bg-primary-50 hover:text-primary-700 dark:text-primary-300 dark:hover:bg-primary-500/10 dark:hover:text-primary-200"
                    >
                      Open full inbox
                    </Link>
                  </div>
                ) : null}
              </div>

             {!isWholesaler ? (
  <>
    <Link
      to="/wishlist"
      className="relative grid h-10 w-10 place-items-center rounded-full border border-slate-300 bg-white text-slate-700 transition hover:border-red-300 hover:text-red-500 dark:border-white/15 dark:bg-slate-900 dark:text-slate-200"
      aria-label="Wishlist"
      title="Wishlist"
    >
      <Heart className="h-4 w-4" />
    </Link>
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
  </>
) : null}

              <div ref={accountMenuRef} className="relative">
                <button
                  type="button"
                  onClick={() => setIsAccountOpen((open) => !open)}
                  className="inline-flex items-center gap-2 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-semibold text-slate-700 transition hover:border-primary-300 hover:text-primary-700 dark:border-white/15 dark:bg-slate-900 dark:text-slate-200 dark:hover:border-primary-400/40 dark:hover:text-primary-300"
                  aria-haspopup="menu"
                  aria-expanded={isAccountOpen}
                >
                  {user?.avatar ? (
                      <img src={user.avatar} alt={user.name} className="object-cover max-w-8" />
                  ) : (
                      <span className="text-3xl font-bold text-white">{user?.name?.charAt(0) || 'U'}</span>
                  )}
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
