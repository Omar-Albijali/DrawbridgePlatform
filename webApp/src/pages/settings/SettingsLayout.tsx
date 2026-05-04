import { Bell, CreditCard, MapPin, Shield, User } from 'lucide-react';
import { NavLink, Outlet } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import PageShell from '../../components/PageShell';

interface NavItemProps {
  to: string;
  icon: JSX.Element;
  label: string;
}

function NavItem({ to, icon, label }: NavItemProps): JSX.Element {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        `flex items-center gap-3 rounded-lg px-4 py-3 font-medium transition-all duration-200 ${
          isActive
            ? 'bg-primary-600 text-white shadow-md'
            : 'text-navy-600 hover:bg-navy-100 hover:text-navy-800 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-slate-100'
        }`
      }
    >
      {icon}
      <span>{label}</span>
    </NavLink>
  );
}

export default function SettingsLayout(): JSX.Element {
  const { t } = useTranslation();
  const navItems = [
    { to: '/settings/profile', icon: <User className="h-5 w-5" />, label: t('settings.nav.profile') },
    { to: '/settings/security', icon: <Shield className="h-5 w-5" />, label: t('settings.nav.security') },
    { to: '/settings/payments', icon: <CreditCard className="h-5 w-5" />, label: t('settings.nav.payments') },
    { to: '/settings/addresses', icon: <MapPin className="h-5 w-5" />, label: t('settings.nav.addresses') },
    { to: '/settings/notifications', icon: <Bell className="h-5 w-5" />, label: t('settings.nav.notifications') },
  ];

  return (
    <PageShell title={t('settings.title')} description={t('settings.description')}>
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-[280px_minmax(0,1fr)]">
        <aside className="card p-4">
          <nav className="space-y-1">
            {navItems.map((item) => (
              <NavItem key={item.to} {...item} />
            ))}
          </nav>
        </aside>

        <main className="space-y-6">
          <Outlet />
        </main>
      </div>
    </PageShell>
  );
}
