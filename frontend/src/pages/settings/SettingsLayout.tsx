import { NavLink, Outlet } from 'react-router-dom';

const settingsLinks = [
  { to: '/settings/profile', label: 'Profile' },
  { to: '/settings/security', label: 'Security' },
  { to: '/settings/payments', label: 'Payments' },
  { to: '/settings/addresses', label: 'Addresses' },
  { to: '/settings/notifications', label: 'Notifications' },
] as const;

export default function SettingsLayout(): JSX.Element {
  return (
    <section className="settings-layout">
      <aside className="settings-layout__menu">
        {settingsLinks.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              isActive ? 'settings-layout__link settings-layout__link--active' : 'settings-layout__link'
            }
          >
            {item.label}
          </NavLink>
        ))}
      </aside>
      <div className="settings-layout__panel">
        <Outlet />
      </div>
    </section>
  );
}
