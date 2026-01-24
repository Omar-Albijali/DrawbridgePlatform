import React from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { User, Shield, CreditCard, Bell, MapPin } from 'lucide-react';

interface NavItemProps {
    to: string;
    icon: React.ReactNode;
    label: string;
}

const NavItem: React.FC<NavItemProps> = ({ to, icon, label }) => (
    <NavLink
        to={to}
        className={({ isActive }) =>
            `flex items-center gap-3 px-4 py-3 rounded-lg font-medium transition-all duration-200 ${isActive
                ? 'bg-primary-600 text-white shadow-md'
                : 'text-navy-600 hover:bg-navy-100 hover:text-navy-800'
            }`
        }
    >
        {icon}
        <span>{label}</span>
    </NavLink>
);

const SettingsLayout: React.FC = () => {
    const navItems = [
        { to: '/settings/profile', icon: <User className="w-5 h-5" />, label: 'My Profile' },
        { to: '/settings/security', icon: <Shield className="w-5 h-5" />, label: 'Login & Security' },
        { to: '/settings/payments', icon: <CreditCard className="w-5 h-5" />, label: 'Payment Methods' },
        { to: '/settings/addresses', icon: <MapPin className="w-5 h-5" />, label: 'Address Management' },
        { to: '/settings/notifications', icon: <Bell className="w-5 h-5" />, label: 'Notification Preferences' },
    ];

    return (
        <div className="flex gap-8">
            {/* Settings Sidebar */}
            <aside className="w-72 flex-shrink-0">
                <div className="card sticky top-6">
                    <h2 className="text-lg font-semibold text-navy-800 mb-4 px-4">Settings</h2>
                    <nav className="space-y-1">
                        {navItems.map((item) => (
                            <NavItem key={item.to} {...item} />
                        ))}
                    </nav>
                </div>
            </aside>

            {/* Content Area */}
            <main className="flex-1 min-w-0">
                <Outlet />
            </main>
        </div>
    );
};

export default SettingsLayout;
