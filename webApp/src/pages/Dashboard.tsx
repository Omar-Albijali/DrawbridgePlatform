import { useEffect, useState } from 'react';
import {
  AlertTriangle,
  BarChart3,
  DollarSign,
  Package,
  ShoppingBag,
  ShoppingCart,
  TrendingDown,
  TrendingUp,
  Truck,
  Check,
} from 'lucide-react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import PageShell from '../components/PageShell';
import { useAuth } from '../contexts/AuthContext';
import { useCart } from '../contexts/CartContext';
import { inventoryService } from '../services/inventoryService';
import { notificationService } from '../services/notificationService';
import { orderService } from '../services/orderService';
import { productService } from '../services/productService';
import { getNotificationTitle, notificationDestination, shortenOrderIds } from '../utils/notificationHelpers';
import { formatCurrency, formatDate, orderStatusLabel } from '../i18n/display';
import {
  InventoryStatus,
  MostOrderedProductDTO,
  NotificationType,
  UserRole,
  type InventoryItem,
  type Notification,
  type Order,
  type OrderStatus,
} from '../types';

interface KpiCardProps {
  title: string;
  value: string;
  change?: number;
  icon: JSX.Element;
  iconBg: string;
  valueColor?: string;
}

interface MonthlyStat {
  [key: string]: string | number;
  month: string;
  amount: number;
}

interface PieStat {
  [key: string]: string | number;
  name: string;
  value: number;
  color: string;
}

interface ChartLegendProps {
  data: PieStat[];
  direction: 'ltr' | 'rtl';
}

function ChartLegend({ data, direction }: ChartLegendProps): JSX.Element {
  return (
      <div dir={direction} className="mt-4 flex flex-wrap items-center justify-center gap-x-6 gap-y-3 text-sm text-navy-600">
        {data.map((entry) => (
            <div key={entry.name} className="inline-flex items-center gap-2">
              <span className="h-3 w-3 shrink-0 rounded-sm" style={{ backgroundColor: entry.color }} />
              <span className="whitespace-nowrap">{entry.name}</span>
            </div>
        ))}
      </div>
  );
}

function KpiCard({ title, value, change, icon, iconBg, valueColor }: KpiCardProps): JSX.Element {
  const { t } = useTranslation();

  return (
      <div className="card">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-sm font-medium text-navy-500 mb-1">{title}</p>
            <p className={`text-3xl font-bold ${valueColor ?? 'text-navy-800'}`}>{value}</p>
            {change !== undefined && (
                <div className={`flex items-center gap-1 mt-2 text-sm ${change >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                  {change >= 0 ? <TrendingUp className="w-4 h-4" /> : <TrendingDown className="w-4 h-4" />}
                  <span>{t('dashboard.kpi.changeVsLastMonth', { change: Math.abs(change) })}</span>
                </div>
            )}
          </div>
          <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${iconBg}`}>{icon}</div>
        </div>
      </div>
  );
}

interface MostOrdersWidgetProps {
  items: MostOrderedProductDTO[];
}

function MostOrdersWidget({ items }: MostOrdersWidgetProps): JSX.Element {
  const { t } = useTranslation();
  const { addToCart } = useCart();
  const [addingIds, setAddingIds] = useState<Set<string>>(new Set());
  const [addedIds, setAddedIds] = useState<Set<string>>(new Set());

  const rankColors = ['text-yellow-500', 'text-gray-400', 'text-amber-600'];

  const handleAddToCart = async (item: MostOrderedProductDTO): Promise<void> => {
    if (addingIds.has(item.productId)) return;

    setAddingIds((prev) => new Set(prev).add(item.productId));
    try {
      // Fetch full product so CartContext has stock/MOQ info it needs
      const product = await productService.getById(item.productId);
      await addToCart(product);

      // Show checkmark briefly then reset
      setAddedIds((prev) => new Set(prev).add(item.productId));
      setTimeout(() => {
        setAddedIds((prev) => {
          const next = new Set(prev);
          next.delete(item.productId);
          return next;
        });
      }, 1500);
    } catch (error) {
      console.error('Failed to add product to cart', error);
    } finally {
      setAddingIds((prev) => {
        const next = new Set(prev);
        next.delete(item.productId);
        return next;
      });
    }
  };

  return (
      <div className="card">
        <div className="flex items-center gap-2 mb-4">
          <h3 className="text-lg font-semibold text-navy-800">{t('dashboard.mostOrders')}</h3>
        </div>
        <p className="text-sm text-navy-500 mb-4">{t('dashboard.mostOrdersDescription')}</p>
        {items.length === 0 ? (
            <p className="text-center text-navy-500 py-6">{t('dashboard.noMostOrders')}</p>
        ) : (
            <div className="space-y-3">
              {items.map((item, index) => {
                const isAdding = addingIds.has(item.productId);
                const isAdded = addedIds.has(item.productId);

                return (
                    <div
                        key={item.productId}
                        className="flex items-center gap-3 p-3 rounded-lg bg-gray-50"
                    >
                      {/* Rank badge */}
                      <div className="flex-shrink-0 w-7 h-7 rounded-full bg-white flex items-center justify-center shadow-sm">
                  <span className={`text-sm font-bold ${index < 3 ? rankColors[index] : 'text-navy-500'}`}>
                    {index + 1}
                  </span>
                      </div>

                      {/* Product image */}
                      <div className="flex-shrink-0 w-10 h-10 rounded-lg overflow-hidden bg-gray-100">
                        {item.productImageUrl ? (
                            <img
                                src={item.productImageUrl}
                                alt={item.productName}
                                className="w-full h-full object-cover"
                            />
                        ) : (
                            <div className="w-full h-full flex items-center justify-center">
                              <Package className="w-5 h-5 text-gray-400" />
                            </div>
                        )}
                      </div>

                      {/* Product info */}
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-navy-800 truncate">{item.productName}</p>
                        <p className="text-sm text-navy-500">{formatCurrency(item.price)}</p>
                      </div>

                      {/* Order count badge */}
                      <div className="flex-shrink-0">
                  <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-semibold bg-primary-100 text-primary-700">
                    {t('dashboard.timesOrdered', { count: item.orderCount })}
                  </span>
                      </div>

                      {/* Add to cart button */}
                      <button
                          type="button"
                          disabled={isAdding || isAdded}
                          onClick={() => { void handleAddToCart(item); }}
                          className={`flex-shrink-0 flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all duration-200
                    ${isAdded
                              ? 'bg-green-100 text-green-700 cursor-default'
                              : isAdding
                                  ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                                  : 'bg-primary-600 text-white hover:bg-primary-700 active:scale-95'
                          }`}
                      >
                        {isAdded ? (
                            <>
                              <Check className="w-3.5 h-3.5" />
                              <span>{t('marketplace.card.added')}</span>
                            </>
                        ) : isAdding ? (
                            <>
                              <div className="w-3.5 h-3.5 border-2 border-gray-300 border-t-gray-500 rounded-full animate-spin" />
                              <span>{t('common.loading')}</span>
                            </>
                        ) : (
                            <>
                              <ShoppingCart className="w-3.5 h-3.5" />
                              <span>{t('marketplace.card.addToCart')}</span>
                            </>
                        )}
                      </button>
                    </div>
                );
              })}
            </div>
        )}
      </div>
  );
}

export default function Dashboard(): JSX.Element {
  const { i18n, t } = useTranslation();
  const { user } = useAuth();
  const navigate = useNavigate();
  const isRetailer = user?.role === UserRole.RETAILER;
  const [orders, setOrders] = useState<Order[]>([]);
  const [inventory, setInventory] = useState<InventoryItem[]>([]);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [mostOrdered, setMostOrdered] = useState<MostOrderedProductDTO[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchData = async (): Promise<void> => {
      if (!user?.id) {
        setIsLoading(false);
        return;
      }

      try {
        const ordersPromise = user.role === UserRole.WHOLESALER
            ? orderService.getByWholesaler(user.id)
            : orderService.getByRetailer(user.id);
        const notificationsPromise = notificationService.getNotifications(user.id);
        const inventoryPromise = user.role === UserRole.RETAILER
            ? inventoryService.getByRetailer(user.id)
            : Promise.resolve([]);
        const mostOrderedPromise = user.role === UserRole.RETAILER
            ? orderService.getMostOrdered(user.id)
            : Promise.resolve([]);

        const [ordersData, notificationData, inventoryData, mostOrderedData] = await Promise.all([
          ordersPromise,
          notificationsPromise,
          inventoryPromise,
          mostOrderedPromise,
        ]);

        setOrders(Array.isArray(ordersData) ? ordersData : []);
        setNotifications(Array.isArray(notificationData) ? notificationData : []);
        setInventory(Array.isArray(inventoryData) ? inventoryData : []);
        setMostOrdered(Array.isArray(mostOrderedData) ? mostOrderedData : []);
      } catch (error) {
        console.error('Failed to fetch dashboard data', error);
      } finally {
        setIsLoading(false);
      }
    };

    void fetchData();
  }, [user]);

  const getStatusName = (status: OrderStatus | string | null | undefined): string => {
    if (!status) return '';
    return (status as { name?: string }).name ?? String(status);
  };

  const totalOrders = orders.length;
  const totalAmount = orders.reduce((sum, order) => sum + (order.subtotal ?? 0), 0);
  const pendingCount = orders.filter((order) => getStatusName(order.status) === 'PENDING').length;
  const processingCount = orders.filter((order) => getStatusName(order.status) === 'PROCESSING').length;
  const inventoryStatusName = (status: InventoryItem['status']): string => {
    if (!status) return '';
    return (status as { name?: string }).name ?? String(status);
  };

  const retailerKpis: KpiCardProps[] = [
    {
      title: t('dashboard.kpi.totalOrders'),
      value: totalOrders.toString(),
      icon: <ShoppingBag className="w-6 h-6 text-primary-600" />,
      iconBg: 'bg-primary-100',
    },
    {
      title: t('dashboard.kpi.pendingOrders'),
      value: pendingCount.toString(),
      icon: <AlertTriangle className="w-6 h-6 text-amber-600" />,
      iconBg: 'bg-amber-100',
      valueColor: 'text-amber-600',
    },
    {
      title: t('dashboard.kpi.totalSpend'),
      value: formatCurrency(totalAmount, { maximumFractionDigits: 0 }),
      icon: <DollarSign className="w-6 h-6 text-green-600" />,
      iconBg: 'bg-green-100',
    },
    {
      title: t('dashboard.kpi.processing'),
      value: processingCount.toString(),
      icon: <Truck className="w-6 h-6 text-blue-600" />,
      iconBg: 'bg-blue-100',
    },
  ];

  const wholesalerKpis: KpiCardProps[] = [
    {
      title: t('dashboard.kpi.totalOrders'),
      value: totalOrders.toString(),
      icon: <Package className="w-6 h-6 text-primary-600" />,
      iconBg: 'bg-primary-100',
    },
    {
      title: t('dashboard.kpi.pendingOrders'),
      value: pendingCount.toString(),
      icon: <AlertTriangle className="w-6 h-6 text-amber-600" />,
      iconBg: 'bg-amber-100',
      valueColor: 'text-amber-600',
    },
    {
      title: t('dashboard.kpi.totalRevenue'),
      value: formatCurrency(totalAmount, { maximumFractionDigits: 0 }),
      icon: <DollarSign className="w-6 h-6 text-green-600" />,
      iconBg: 'bg-green-100',
    },
    {
      title: t('dashboard.kpi.processing'),
      value: processingCount.toString(),
      icon: <BarChart3 className="w-6 h-6 text-purple-600" />,
      iconBg: 'bg-purple-100',
    },
  ];

  const kpis = isRetailer ? retailerKpis : wholesalerKpis;
  const chartDirection = i18n.resolvedLanguage === 'ar' ? 'rtl' : 'ltr';
  const monthFormatter = new Intl.DateTimeFormat(i18n.resolvedLanguage === 'ar' ? 'ar-SA' : 'en', { month: 'short' });

  const chartData: MonthlyStat[] = (() => {
    const now = new Date();
    const keyFor = (date: Date): string => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
    const months = Array.from({ length: 7 }, (_, index) => {
      const date = new Date(now.getFullYear(), now.getMonth() - (6 - index), 1);
      return { key: keyFor(date), month: monthFormatter.format(date), amount: 0 };
    });
    const monthIndexMap = new Map(months.map((entry, index) => [entry.key, index]));

    orders.forEach((order) => {
      const placedAt = new Date(order.placedAt);
      if (Number.isNaN(placedAt.getTime())) return;
      const key = keyFor(placedAt);
      const index = monthIndexMap.get(key);
      if (index !== undefined) {
        months[index].amount += order.subtotal ?? 0;
      }
    });

    return months.map((entry) => ({ ...entry, amount: Number(entry.amount.toFixed(2)) }));
  })();

  const pieData: PieStat[] = isRetailer
      ? (() => {
        const totalItems = inventory.length;
        const lowStock = inventory.filter((item) => inventoryStatusName(item.status) === InventoryStatus.LOW_STOCK.name).length;
        const outOfStock = inventory.filter((item) => inventoryStatusName(item.status) === InventoryStatus.OUT_OF_STOCK.name).length;
        const optimal = Math.max(totalItems - lowStock - outOfStock, 0);
        const asPercentage = (count: number): number =>
            totalItems > 0 ? Number(((count / totalItems) * 100).toFixed(1)) : 0;
        return [
          { name: t('dashboard.inventoryState.optimal'), value: asPercentage(optimal), color: '#10b981' },
          { name: t('dashboard.inventoryState.lowStock'), value: asPercentage(lowStock), color: '#f59e0b' },
          { name: t('dashboard.inventoryState.outOfStock'), value: asPercentage(outOfStock), color: '#ef4444' },
        ];
      })()
      : (() => {
        const statusBuckets = [
          { name: t('orders.status.DELIVERED'), key: 'DELIVERED', color: '#10b981' },
          { name: t('orders.status.SHIPPED'), key: 'SHIPPED', color: '#3b82f6' },
          { name: t('orders.status.PROCESSING'), key: 'PROCESSING', color: '#f59e0b' },
          { name: t('orders.status.PENDING'), key: 'PENDING', color: '#6b7280' },
          { name: t('orders.status.CANCELLED'), key: 'CANCELLED', color: '#ef4444' },
        ];
        return statusBuckets.map((bucket) => ({
          name: bucket.name,
          value: orders.filter((order) => getStatusName(order.status) === bucket.key).length,
          color: bucket.color,
        }));
      })();

  const getStatusColor = (status: OrderStatus | string): string => {
    switch (getStatusName(status)) {
      case 'DELIVERED': return 'bg-green-100 text-green-700';
      case 'SHIPPED': return 'bg-blue-100 text-blue-700';
      case 'PROCESSING': return 'bg-amber-100 text-amber-700';
      case 'CONFIRMED': return 'bg-indigo-100 text-indigo-700';
      case 'PENDING': return 'bg-gray-100 text-gray-700';
      case 'CANCELLED': return 'bg-red-100 text-red-700';
      default: return 'bg-gray-100 text-gray-700';
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
          title={t('dashboard.welcome', { name: user?.name?.split(' ')[0] ?? t('dashboard.fallbackName') })}
          description={t('dashboard.description', { scope: isRetailer ? t('dashboard.business') : t('dashboard.store') })}
      >
        {/* KPI Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {kpis.map((kpi) => (
              <KpiCard key={kpi.title} {...kpi} />
          ))}
        </div>

        {/* Charts */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 card">
            <h3 className="text-lg font-semibold text-navy-800 mb-4">
              {isRetailer ? t('dashboard.monthlyExpenses') : t('dashboard.monthlySales')}
            </h3>
            <div dir={chartDirection === 'rtl' ? 'ltr' : undefined} className="h-80">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                  <XAxis dataKey="month" stroke="#627d98" />
                  <YAxis
                      stroke="#627d98"
                      orientation="left"
                      mirror={false}
                      tickMargin={chartDirection === 'rtl' ? 20 : 8}
                      width={chartDirection === 'rtl' ? 84 : 60}
                      tick={chartDirection === 'rtl' ? { dx: -8 } : undefined}
                  />
                  <Tooltip
                      contentStyle={{ backgroundColor: '#fff', border: '1px solid #e5e7eb', borderRadius: '8px' }}
                      formatter={(value) => {
                        const normalized = typeof value === 'number' ? value : 0;
                        return [formatCurrency(normalized), isRetailer ? t('dashboard.expenses') : t('dashboard.sales')];
                      }}
                  />
                  <Bar dataKey="amount" fill="#3b82f6" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="card">
            <h3 className="text-lg font-semibold text-navy-800 mb-4">
              {isRetailer ? t('dashboard.inventoryComposition') : t('dashboard.orderStatus')}
            </h3>
            <div className="flex h-80 flex-col">
              <div className="min-h-0 flex-1">
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
                          return isRetailer
                              ? [`${normalized}%`, t('dashboard.share')]
                              : [normalized, t('navigation.orders')];
                        }}
                    />
                  </PieChart>
                </ResponsiveContainer>
              </div>
              <ChartLegend data={pieData} direction={chartDirection} />
            </div>
          </div>
        </div>

        {/* Recent Orders + Activity */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="card">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-navy-800">{t('dashboard.recentOrders')}</h3>
              <a href="/orders" className="text-sm text-primary-600 hover:text-primary-700 font-medium">
                {t('dashboard.viewAll')}
              </a>
            </div>
            <div className="space-y-3">
              {orders.slice(0, 5).map((order) => (
                  <div key={order.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                    <div>
                      <p className="font-medium text-navy-800">{order.id}</p>
                      <p className="text-sm text-navy-500">
                        {formatDate(order.placedAt)} • {t('dashboard.items', { count: order.items?.length ?? 0 })}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="font-semibold text-navy-800">{formatCurrency(order.subtotal ?? 0)}</p>
                      <span className={`text-xs px-2 py-1 rounded-full capitalize ${getStatusColor(order.status)}`}>
                    {orderStatusLabel(t, order.status)}
                  </span>
                    </div>
                  </div>
              ))}
              {orders.length === 0 && (
                  <p className="text-center text-navy-500 py-4">{t('dashboard.noRecentOrders')}</p>
              )}
            </div>
          </div>

          <div className="card">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-navy-800">{t('dashboard.recentActivity')}</h3>
              <Link to="/notifications" className="text-sm text-primary-600 hover:text-primary-700 font-medium">
                {t('dashboard.viewAll')}
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
              {notifications.length === 0 && (
                  <p className="text-center text-navy-500 py-4">{t('dashboard.noRecentActivity')}</p>
              )}
            </div>
          </div>
        </div>

        {/* Most Orders — Retailer only */}
        {isRetailer && <MostOrdersWidget items={mostOrdered} />}
      </PageShell>
  );
}