import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { UserRole } from '../../types';
import {
    LayoutDashboard,
    ShoppingBag,
    Package,
    ClipboardList,
    HelpCircle,
    PackagePlus,
    BarChart3,
    ChevronLeft,
    ChevronRight,
} from 'lucide-react';

interface NavItem {
    label: string;
    path: string;
    icon: React.ReactNode;
}

interface SidebarProps {
    isCollapsed: boolean;
    onToggle: () => void;
}

const Sidebar: React.FC<SidebarProps> = ({ isCollapsed, onToggle }) => {
    const { user } = useAuth();

    const retailerNavItems: NavItem[] = [
        { label: 'Dashboard', path: '/dashboard', icon: <LayoutDashboard className="w-5 h-5" /> },
        { label: 'Marketplace', path: '/marketplace', icon: <ShoppingBag className="w-5 h-5" /> },
        { label: 'Inventory', path: '/inventory', icon: <Package className="w-5 h-5" /> },
        { label: 'Orders', path: '/orders', icon: <ClipboardList className="w-5 h-5" /> },
        { label: 'Support', path: '/support', icon: <HelpCircle className="w-5 h-5" /> },
    ];

    const wholesalerNavItems: NavItem[] = [
        { label: 'Dashboard', path: '/dashboard', icon: <LayoutDashboard className="w-5 h-5" /> },
        { label: 'Manage Products', path: '/products', icon: <PackagePlus className="w-5 h-5" /> },
        { label: 'Orders', path: '/orders', icon: <ClipboardList className="w-5 h-5" /> },
        { label: 'Reports', path: '/reports', icon: <BarChart3 className="w-5 h-5" /> },
    ];

    const navItems = user?.role === UserRole.WHOLESALER ? wholesalerNavItems : retailerNavItems;

    return (
        <aside
            className={`fixed left-0 top-0 h-screen bg-navy-900 text-white transition-all duration-300 z-50 flex flex-col ${isCollapsed ? 'w-20' : 'w-64'
                }`}
        >
            {/* Logo */}
            <div className={`p-6 border-b border-navy-700 flex items-center ${isCollapsed ? 'justify-center' : 'gap-3'}`}>
                <div className="w-10 h-10 bg-primary-500 rounded-xl flex items-center justify-center flex-shrink-0">
                    <svg className="w-6 h-6 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M3 21h18M5 21V7l8-4 8 4v14M9 21v-8h6v8" />
                    </svg>
                </div>
                {!isCollapsed && (
                    <div>
                        <h1 className="font-bold text-lg">Drawbridge</h1>
                        <p className="text-xs text-navy-400">B2B Commerce</p>
                    </div>
                )}
            </div>

            {/* Navigation */}
            <nav className="flex-1 py-6 px-3 overflow-y-auto">
                <ul className="space-y-2">
                    {navItems.map((item) => (
                        <li key={item.path}>
                            <NavLink
                                to={item.path}
                                className={({ isActive }) =>
                                    `flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-200 ${isActive
                                        ? 'bg-primary-600 text-white'
                                        : 'text-navy-300 hover:bg-navy-800 hover:text-white'
                                    } ${isCollapsed ? 'justify-center' : ''}`
                                }
                                title={isCollapsed ? item.label : undefined}
                            >
                                {item.icon}
                                {!isCollapsed && <span className="font-medium">{item.label}</span>}
                            </NavLink>
                        </li>
                    ))}
                </ul>
            </nav>


            {/* Collapse Button */}
            <button
                onClick={onToggle}
                className="absolute -right-3 top-20 w-6 h-6 bg-navy-700 hover:bg-navy-600 rounded-full flex items-center justify-center text-white shadow-lg transition-colors">
                {isCollapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
            </button>
        </aside>
    );
};

export default Sidebar;
