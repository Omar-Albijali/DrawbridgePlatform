import { createBrowserRouter, Navigate } from 'react-router-dom';

// Layouts
import MainLayout from './layouts/MainLayout';
import SettingsLayout from './pages/settings/SettingsLayout';

// Pages
import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import Dashboard from './pages/Dashboard';
import Marketplace from './pages/Marketplace';
import Inventory from './pages/Inventory';
import Orders from './pages/Orders';
import OrderDetails from './pages/OrderDetails';
import Cart from './pages/Cart';
import Checkout from './pages/Checkout';
import ManageProducts from './pages/ManageProducts';
import ProductForm from './pages/ProductForm';

// Settings Pages
import Profile from './pages/settings/Profile';
import Security from './pages/settings/Security';
import Payments from './pages/settings/Payments';
import Notifications from './pages/settings/Notifications';
import Addresses from './pages/settings/Addresses';

// Placeholder pages for routes we haven't fully implemented
const SupportPage = () => (
    <div className="text-center py-16">
        <h1 className="text-2xl font-bold text-navy-800 mb-2">Support</h1>
        <p className="text-navy-500">Contact our support team for assistance.</p>
    </div>
);


const ReportsPage = () => (
    <div className="text-center py-16">
        <h1 className="text-2xl font-bold text-navy-800 mb-2">Reports</h1>
        <p className="text-navy-500">View detailed analytics and reports.</p>
    </div>
);

export const router = createBrowserRouter([
    {
        path: '/',
        element: <Navigate to="/login" replace />
    },
    {
        path: '/login',
        element: <Login />
    },
    {
        path: '/register',
        element: <Register />
    },
    {
        path: '/forgot-password',
        element: <ForgotPassword />
    },
    {
        path: '/reset-password',
        element: <ResetPassword />
    },
    {
        path: '/',
        element: <MainLayout />,
        children: [
            {
                path: 'dashboard',
                element: <Dashboard />
            },
            {
                path: 'marketplace',
                element: <Marketplace />
            },
            {
                path: 'inventory',
                element: <Inventory />
            },
            {
                path: 'orders',
                element: <Orders />
            },
            {
                path: 'orders/:id',
                element: <OrderDetails />
            },
            {
                path: 'cart',
                element: <Cart />
            },
            {
                path: 'checkout',
                element: <Checkout />
            },
            {
                path: 'support',
                element: <SupportPage />
            },
            {
                path: 'products',
                element: <ManageProducts />
            },
            {
                path: 'products/new',
                element: <ProductForm />
            },
            {
                path: 'products/edit/:id',
                element: <ProductForm />
            },
            {
                path: 'reports',
                element: <ReportsPage />
            },
            {
                path: 'settings',
                element: <SettingsLayout />,
                children: [
                    { index: true, element: <Navigate to="profile" replace /> },
                    { path: 'profile', element: <Profile /> },
                    { path: 'security', element: <Security /> },
                    { path: 'payments', element: <Payments /> },
                    { path: 'addresses', element: <Addresses /> },
                    { path: 'notifications', element: <Notifications /> }
                ]
            }
        ]
    }
]);

export default router;
