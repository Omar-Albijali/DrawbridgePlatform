import React, { useState, useRef, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { NotificationType } from '../../types';
import { useCart } from '../../contexts/CartContext';
import {
    ShoppingCart,
    Bell,
    ChevronDown,
    Settings,
    LogOut,
    Search,
    Package,
    CreditCard,
    AlertTriangle
} from 'lucide-react';

// Demo notifications for UI
const DEMO_NOTIFICATIONS: any[] = [];

const Header: React.FC = () => {
    const { user, logout } = useAuth();
    const { itemCount } = useCart();
    const [showProfileMenu, setShowProfileMenu] = useState(false);
    const [showNotifications, setShowNotifications] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');

    const profileRef = useRef<HTMLDivElement>(null);
    const notificationRef = useRef<HTMLDivElement>(null);

    // Close dropdowns when clicking outside
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (profileRef.current && !profileRef.current.contains(event.target as Node)) {
                setShowProfileMenu(false);
            }
            if (notificationRef.current && !notificationRef.current.contains(event.target as Node)) {
                setShowNotifications(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const unreadCount = DEMO_NOTIFICATIONS.filter(n => !n.read).length;

    const getNotificationIcon = (type: NotificationType) => {
        switch (type) {
            case NotificationType.ORDER: return <Package className="w-4 h-4 text-primary-500" />;
            case NotificationType.STOCK: return <AlertTriangle className="w-4 h-4 text-amber-500" />;
            case NotificationType.PAYMENT: return <CreditCard className="w-4 h-4 text-green-500" />;
            default: return <Bell className="w-4 h-4 text-navy-500" />;
        }
    };

    return (
        <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6 sticky top-0 z-40">
            {/* Search Bar */}
            <div className="flex-1 max-w-xl">
                <div className="relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-navy-400" />
                    <input
                        type="text"
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        placeholder="Search products, orders, inventory..."
                        className="w-full pl-10 pr-4 py-2 bg-gray-100 border border-transparent rounded-lg focus:bg-white focus:border-primary-500 focus:ring-1 focus:ring-primary-500 transition-colors"
                    />
                </div>
            </div>

            {/* Right Section */}
            <div className="flex items-center gap-4">
                {/* Cart (Retailer only) */}
                {/* Cart (Retailer only) */}
                {((user?.role as any) === 'RETAILER' || user?.role?.name === 'RETAILER') && (
                    <Link
                        to="/cart"
                        className="relative p-2 text-navy-600 hover:bg-gray-100 rounded-lg transition-colors"
                    >
                        <ShoppingCart className="w-6 h-6" />
                        {itemCount > 0 && (
                            <span className="absolute -top-1 -right-1 w-5 h-5 bg-primary-600 text-white text-xs font-bold rounded-full flex items-center justify-center">
                                {itemCount > 99 ? '99+' : itemCount}
                            </span>
                        )}
                    </Link>
                )}

                {/* Notifications */}
                <div ref={notificationRef} className="relative">
                    <button
                        onClick={() => setShowNotifications(!showNotifications)}
                        className="relative p-2 text-navy-600 hover:bg-gray-100 rounded-lg transition-colors"
                    >
                        <Bell className="w-6 h-6" />
                        {unreadCount > 0 && (
                            <span className="absolute -top-1 -right-1 w-5 h-5 bg-red-500 text-white text-xs font-bold rounded-full flex items-center justify-center">
                                {unreadCount}
                            </span>
                        )}
                    </button>

                    {/* Notifications Dropdown */}
                    {showNotifications && (
                        <div className="absolute right-0 top-full mt-2 w-80 bg-white rounded-xl shadow-lg border border-gray-200 overflow-hidden">
                            <div className="p-4 border-b border-gray-100 flex items-center justify-between">
                                <h3 className="font-semibold text-navy-800">Notifications</h3>
                                <button className="text-sm text-primary-600 hover:text-primary-700">Mark all read</button>
                            </div>
                            <div className="max-h-80 overflow-y-auto">
                                {DEMO_NOTIFICATIONS.map((notification) => (
                                    <div
                                        key={notification.id}
                                        className={`p-4 border-b border-gray-50 hover:bg-gray-50 transition-colors ${!notification.read ? 'bg-primary-50/50' : ''
                                            }`}
                                    >
                                        <div className="flex gap-3">
                                            <div className="w-8 h-8 rounded-full bg-gray-100 flex items-center justify-center flex-shrink-0">
                                                {getNotificationIcon(notification.type)}
                                            </div>
                                            <div className="flex-1 min-w-0">
                                                <p className="text-sm text-navy-700">{notification.message}</p>
                                                <p className="text-xs text-navy-400 mt-1">{notification.time}</p>
                                            </div>
                                            {!notification.read && (
                                                <div className="w-2 h-2 bg-primary-500 rounded-full flex-shrink-0 mt-2" />
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                            <div className="p-3 border-t border-gray-100">
                                <button className="w-full text-center text-sm text-primary-600 hover:text-primary-700 font-medium">
                                    View all notifications
                                </button>
                            </div>
                        </div>
                    )}
                </div>

                {/* Profile Dropdown */}
                <div ref={profileRef} className="relative">
                    <button
                        onClick={() => setShowProfileMenu(!showProfileMenu)}
                        className="flex items-center gap-3 p-2 hover:bg-gray-100 rounded-lg transition-colors"
                    >
                        <img
                            src={user?.avatar || `https://ui-avatars.com/api/?name=${encodeURIComponent(user?.name || 'User')}&background=1e40af&color=fff`}
                            alt={user?.name || 'User'}
                            className="w-8 h-8 rounded-full"
                        />
                        <div className="hidden md:block text-left">
                            <p className="text-sm font-medium text-navy-800">{user?.name}</p>
                            <p className="text-xs text-navy-500 capitalize">{user?.role?.name}</p>
                        </div>
                        <ChevronDown className="w-4 h-4 text-navy-400 hidden md:block" />
                    </button>

                    {/* Profile Menu Dropdown */}
                    {showProfileMenu && (
                        <div className="absolute right-0 top-full mt-2 w-56 bg-white rounded-xl shadow-lg border border-gray-200 overflow-hidden">
                            <div className="p-4 border-b border-gray-100">
                                <p className="font-medium text-navy-800">{user?.name}</p>
                                <p className="text-sm text-navy-500">{user?.email}</p>
                            </div>
                            <div className="py-2">
                                <Link to="/settings" className="flex items-center gap-3 px-4 py-2 text-navy-600 hover:bg-gray-50 transition-colors" onClick={() => setShowProfileMenu(false)}>
                                    <Settings className="w-5 h-5" />
                                    <span>Settings</span>
                                </Link>
                            </div>
                            <div className="py-2 border-t border-gray-100">
                                <button
                                    onClick={() => void logout()}
                                    className="w-full flex items-center gap-3 px-4 py-2 text-red-600 hover:bg-red-50 transition-colors"
                                >
                                    <LogOut className="w-5 h-5" />
                                    <span>Logout</span>
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </header>
    );
};

export default Header;
