import { useEffect, useState } from 'react';
import {
  AlertTriangle,
  BarChart3,
  DollarSign,
  Package,
  ShoppingBag,
  TrendingDown,
  TrendingUp,
  Truck,
} from 'lucide-react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { Link, useNavigate } from 'react-router-dom';
import PageShell from '../components/PageShell';
import { useAuth } from '../contexts/AuthContext';
import { inventoryCompositionData, monthlyExpensesData, monthlySalesData, orderStatusData } from '../data/constants';
import { notificationService } from '../services/notificationService';
import { orderService } from '../services/orderService';
import { getNotificationTitle, notificationDestination, shortenOrderIds } from '../utils/notificationHelpers';
import { NotificationType, UserRole, type Notification, type Order, type OrderStatus } from '../types';

interface KpiCardProps {
  title: string;
  value: string;
  change?: number;
  icon: JSX.Element;
  iconBg: string;
  valueColor?: string;
}

function KpiCard({ title, value, change, icon, iconBg, valueColor }: KpiCardProps): JSX.Element {
  return (
    <div className="card">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm font-medium text-navy-500 mb-1">{title}</p>
          <p className={`text-3xl font-bold ${valueColor ?? 'text-navy-800'}`}>{value}</p>
          {change !== undefined && (
            <div className={`flex items-center gap-1 mt-2 text-sm ${change >= 0 ? 'text-green-600' : 'text-red-600'}`}>
              {change >= 0 ? <TrendingUp className="w-4 h-4" /> : <TrendingDown className="w-4 h-4" />}
              <span>{Math.abs(change)}% vs last month</span>
            </div>
          )}
        </div>
        <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${iconBg}`}>{icon}</div>
      </div>
    </div>
  );
}

export default function Dashboard(): JSX.Element {
  const { user } = useAuth();
  const navigate = useNavigate();
  const isRetailer = user?.role === UserRole.RETAILER;
  const [orders, setOrders] = useState<Order[]>([]);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchData = async (): Promise<void> => {
      if (!user?.id) {
        setIsLoading(false);
        return;
      }

      try {
        const data = user.role === UserRole.WHOLESALER
          ? await orderService.getByWholesaler(user.id)
          : await orderService.getByRetailer(user.id);
        const notificationData = await notificationService.getNotifications(user.id);
        setOrders(Array.isArray(data) ? data : []);
        setNotifications(Array.isArray(notificationData) ? notificationData : []);
      } catch (error) {
        console.error('Failed to fetch dashboard data', error);
      } finally {
        setIsLoading(false);
      }
    };

    void fetchData();
  }, [user]);

  const getStatusName = (status: OrderStatus | string | null | undefined): string => {
    if (!status) {
      return '';
    }
    return (status as { name?: string }).name ?? String(status);
  };

  const totalOrders = orders.length;
  const totalAmount = orders.reduce((sum, order) => sum + (order.subtotal ?? 0), 0);
  const pendingCount = orders.filter((order) => getStatusName(order.status) === 'PENDING').length;
  const processingCount = orders.filter((order) => getStatusName(order.status) === 'PROCESSING').length;

  const retailerKpis: KpiCardProps[] = [
    {
      title: 'Total Orders',
      value: totalOrders.toString(),
      icon: <ShoppingBag className="w-6 h-6 text-primary-600" />,
      iconBg: 'bg-primary-100',
    },
    {
      title: 'Pending Orders',
      value: pendingCount.toString(),
      icon: <AlertTriangle className="w-6 h-6 text-amber-600" />,
      iconBg: 'bg-amber-100',
      valueColor: 'text-amber-600',
    },
    {
      title: 'Total Spend',
      value: `SAR ${totalAmount.toLocaleString()}`,
      icon: <DollarSign className="w-6 h-6 text-green-600" />,
      iconBg: 'bg-green-100',
    },
    {
      title: 'Processing',
      value: processingCount.toString(),
      icon: <Truck className="w-6 h-6 text-blue-600" />,
      iconBg: 'bg-blue-100',
    },
  ];

  const wholesalerKpis: KpiCardProps[] = [
    {
      title: 'Total Orders',
      value: totalOrders.toString(),
      icon: <Package className="w-6 h-6 text-primary-600" />,
      iconBg: 'bg-primary-100',
    },
    {
      title: 'Pending Orders',
      value: pendingCount.toString(),
      icon: <AlertTriangle className="w-6 h-6 text-amber-600" />,
      iconBg: 'bg-amber-100',
      valueColor: 'text-amber-600',
    },
    {
      title: 'Total Revenue',
      value: `SAR ${totalAmount.toLocaleString()}`,
      icon: <DollarSign className="w-6 h-6 text-green-600" />,
      iconBg: 'bg-green-100',
    },
    {
      title: 'Processing',
      value: processingCount.toString(),
      icon: <BarChart3 className="w-6 h-6 text-purple-600" />,
      iconBg: 'bg-purple-100',
    },
  ];

  const kpis = isRetailer ? retailerKpis : wholesalerKpis;
  const chartData = isRetailer ? monthlyExpensesData : monthlySalesData;
  const pieData = isRetailer ? inventoryCompositionData : orderStatusData;

  const getStatusColor = (status: OrderStatus | string): string => {
    switch (getStatusName(status)) {
      case 'DELIVERED':
        return 'bg-green-100 text-green-700';
      case 'SHIPPED':
        return 'bg-blue-100 text-blue-700';
      case 'PROCESSING':
        return 'bg-amber-100 text-amber-700';
      case 'CONFIRMED':
        return 'bg-indigo-100 text-indigo-700';
      case 'PENDING':
        return 'bg-gray-100 text-gray-700';
      case 'CANCELLED':
        return 'bg-red-100 text-red-700';
      default:
        return 'bg-gray-100 text-gray-700';
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600" />
      </div>
    );
  }

  return (
    <PageShell
      title={`Welcome back, ${user?.name?.split(' ')[0] ?? 'there'}!`}
      description={`Here's what's happening with your ${isRetailer ? 'business' : 'store'} today.`}
    >

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {kpis.map((kpi) => (
          <KpiCard key={kpi.title} {...kpi} />
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 card">
          <h3 className="text-lg font-semibold text-navy-800 mb-4">{isRetailer ? 'Monthly Expenses' : 'Monthly Sales'}</h3>
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis dataKey="month" stroke="#627d98" />
                <YAxis stroke="#627d98" />
                <Tooltip
                  contentStyle={{ backgroundColor: '#fff', border: '1px solid #e5e7eb', borderRadius: '8px' }}
                  formatter={(value) => {
                    const normalized = typeof value === 'number' ? value : 0;
                    return [`SAR ${normalized.toLocaleString()}`, isRetailer ? 'Expenses' : 'Sales'];
                  }}
                />
                <Bar dataKey="amount" fill="#3b82f6" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="card">
          <h3 className="text-lg font-semibold text-navy-800 mb-4">{isRetailer ? 'Inventory Composition' : 'Order Status'}</h3>
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={pieData} cx="50%" cy="50%" innerRadius={60} outerRadius={80} paddingAngle={5} dataKey="value">
                  {pieData.map((entry) => (
                    <Cell key={entry.name} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip
                  contentStyle={{ backgroundColor: '#fff', border: '1px solid #e5e7eb', borderRadius: '8px' }}
                  formatter={(value) => {
                    const normalized = typeof value === 'number' ? value : 0;
                    return [`${normalized}%`, ''];
                  }}
                />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-navy-800">Recent Orders</h3>
            <a href="/orders" className="text-sm text-primary-600 hover:text-primary-700 font-medium">
              View all
            </a>
          </div>
          <div className="space-y-3">
            {orders.slice(0, 5).map((order) => (
              <div key={order.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div>
                  <p className="font-medium text-navy-800">{order.id}</p>
                  <p className="text-sm text-navy-500">
                    {new Date(order.placedAt).toLocaleDateString()} • {order.items?.length ?? 0} items
                  </p>
                </div>
                <div className="text-right">
                  <p className="font-semibold text-navy-800">SAR {(order.subtotal ?? 0).toFixed(2)}</p>
                  <span className={`text-xs px-2 py-1 rounded-full capitalize ${getStatusColor(order.status)}`}>
                    {getStatusName(order.status).toLowerCase().replace('_', ' ')}
                  </span>
                </div>
              </div>
            ))}
            {orders.length === 0 && <p className="text-center text-navy-500 py-4">No recent orders</p>}
          </div>
        </div>

        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-navy-800">Recent Activity</h3>
            <Link to="/notifications" className="text-sm text-primary-600 hover:text-primary-700 font-medium">
              View all
            </Link>
          </div>
          <div className="space-y-4">
            {notifications.slice(0, 5).map((notification) => (
              <button
                key={notification.id}
                type="button"
                className="flex w-full items-start gap-3 text-left"
                onClick={() => {
                  if (!notification.read) {
                    void notificationService.markAsRead(notification.id);
                    setNotifications((prev) =>
                      prev.map((item) =>
                        item.id === notification.id ? ({ ...(item as object), read: true } as Notification) : item,
                      ),
                    );
                  }
                  navigate(notificationDestination(notification));
                }}
              >
                <div
                  className={`w-2 h-2 rounded-full mt-2 ${
                    notification.type === NotificationType.ORDER
                      ? 'bg-primary-500'
                      : notification.type === NotificationType.STOCK
                        ? 'bg-amber-500'
                        : notification.type === NotificationType.PAYMENT
                          ? 'bg-green-500'
                          : 'bg-gray-400'
                  }`}
                />
                <div className="flex-1">
                  <p className={`text-sm ${notification.read ? 'text-navy-500' : 'text-navy-700 font-medium'}`}>
                    {shortenOrderIds(getNotificationTitle(notification))}
                  </p>
                  <p className="text-sm text-navy-700">{shortenOrderIds(notification.message)}</p>
                  <p className="text-xs text-navy-400 mt-1">{notification.time}</p>
                </div>
              </button>
            ))}
            {notifications.length === 0 ? <p className="text-center text-navy-500 py-4">No recent activity</p> : null}
          </div>
        </div>
      </div>
    </PageShell>
  );
}
