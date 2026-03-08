import { createBrowserRouter, Navigate } from 'react-router-dom';
import type { ReactElement } from 'react';
import PageTransition from './components/PageTransition';
import { useAuth } from './contexts/AuthContext';
import Wishlist from './pages/Wishlist';
import MainLayout from './layouts/MainLayout';
import Cart from './pages/Cart';
import Checkout from './pages/Checkout';
import Dashboard from './pages/Dashboard';
import ForgotPassword from './pages/ForgotPassword';
import Inventory from './pages/Inventory';
import Login from './pages/Login';
import ManageProducts from './pages/ManageProducts';
import Marketplace from './pages/Marketplace';
import OrderDetails from './pages/OrderDetails';
import Orders from './pages/Orders';
import ProductForm from './pages/ProductForm';
import Register from './pages/Register';
import ResetPassword from './pages/ResetPassword';
import VerifyEmail from './pages/VerifyEmail';
import Addresses from './pages/settings/Addresses';
import Notifications from './pages/settings/Notifications';
import Payments from './pages/settings/Payments';
import Profile from './pages/settings/Profile';
import Security from './pages/settings/Security';
import SettingsLayout from './pages/settings/SettingsLayout';
import Support from './pages/Support';
import Reports from './pages/Reports';
import Landing from './pages/Landing';
import { UserRole } from './types';

function withTransition(element: ReactElement): ReactElement {
  return <PageTransition>{element}</PageTransition>;
}

function RedirectIfAuthenticated({ children }: { children: ReactElement }): ReactElement {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <Navigate to="/dashboard" replace /> : children;
}

function BlockWholesalerInventory({ children }: { children: ReactElement }): ReactElement {
  const { user } = useAuth();
  if (user?.role !== UserRole.RETAILER) {
    return <Navigate to="/dashboard" replace />;
  }
  return children;
}

function BlockRetailerProducts({ children }: { children: ReactElement }): ReactElement {
  const { user } = useAuth();
  if (user?.role !== UserRole.WHOLESALER) {
    return <Navigate to="/dashboard" replace />;
  }
  return children;
}

function BlockWholesalerCart({ children }: { children: ReactElement }): ReactElement {
  const { user } = useAuth();
  if (user?.role === UserRole.WHOLESALER) {
    return <Navigate to="/dashboard" replace />;
  }
  return children;
}

export const router = createBrowserRouter([
  {
    path: '/',
    element: withTransition(<Landing />),
  },
  {
    path: '/login',
    element: withTransition(
      <RedirectIfAuthenticated>
        <Login />
      </RedirectIfAuthenticated>,
    ),
  },
  {
    path: '/register',
    element: withTransition(
      <RedirectIfAuthenticated>
        <Register />
      </RedirectIfAuthenticated>,
    ),
  },
  {
    path: '/forgot-password',
    element: withTransition(<ForgotPassword />),
  },
  {
    path: '/reset-password',
    element: withTransition(<ResetPassword />),
  },
  {
    path: '/verify-email',
    element: withTransition(<VerifyEmail />),
  },
  {
    path: '/',
    element: <MainLayout requireAuth={false} />,
    children: [
      {
        path: 'marketplace',
        element: withTransition(<Marketplace />),
      },
    ],
  },
  {
    path: '/',
    element: <MainLayout />,
    children: [
      { path: 'dashboard', element: withTransition(<Dashboard />) },
      {
        path: 'inventory',
        element: withTransition(
          <BlockWholesalerInventory>
            <Inventory />
          </BlockWholesalerInventory>,
        ),
      },
      { path: 'orders', element: withTransition(<Orders />) },
      { path: 'orders/:id', element: withTransition(<OrderDetails />) },
      {
        path: 'cart',
        element: withTransition(
          <BlockWholesalerCart>
            <Cart />
          </BlockWholesalerCart>,
        ),
      },
      {
        path: 'checkout',
        element: withTransition(
          <BlockWholesalerCart>
            <Checkout />
          </BlockWholesalerCart>,
        ),
      },
      { path: 'support', element: withTransition(<Support />) },
      {
        path: 'products',
        element: withTransition(
          <BlockRetailerProducts>
            <ManageProducts />
          </BlockRetailerProducts>,
        ),
      },
      {
        path: 'products/new',
        element: withTransition(
          <BlockRetailerProducts>
            <ProductForm />
          </BlockRetailerProducts>,
        ),
      },
      {
        path: 'products/edit/:id',
        element: withTransition(
          <BlockRetailerProducts>
            <ProductForm />
          </BlockRetailerProducts>,
        ),
      },
      { path: 'reports', element: withTransition(<Reports />) },
      {
  path: 'wishlist',
  element: withTransition(
    <BlockWholesalerCart>
      <Wishlist />
    </BlockWholesalerCart>,
  ),
},
      {
        path: 'settings',
        element: withTransition(<SettingsLayout />),
        children: [
          { index: true, element: <Navigate to="profile" replace /> },
          { path: 'profile', element: withTransition(<Profile />) },
          { path: 'security', element: withTransition(<Security />) },
          { path: 'payments', element: withTransition(<Payments />) },
          { path: 'addresses', element: withTransition(<Addresses />) },
          { path: 'notifications', element: withTransition(<Notifications />) },
        ],
      },
    ],
  },
]);
