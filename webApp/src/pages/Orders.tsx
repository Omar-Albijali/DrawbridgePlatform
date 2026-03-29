import { createPortal } from 'react-dom';
import { useEffect, useState, type MouseEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ArrowUpDown,
  Copy,
  CheckCircle2,
  ChevronDown,
  Clock,
  Eye,
  FileText,
  MessageSquare,
  MoreVertical,
  Package,
  Search,
  Settings,
  Store,
  Tags,
  Truck,
  XCircle,
} from 'lucide-react';
import PageShell from '../components/PageShell';
import { useAuth } from '../contexts/AuthContext';
import { orderService } from '../services/orderService';
import { reorderOrderToCart } from '../utils/reorderOrder';
import { Order, OrderStatus, UserRole } from '../types';

type SortField = 'id' | 'placedAt' | 'subtotal' | 'status' | 'retailerName';
type SortOrder = 'asc' | 'desc';

function getStatusName(status: OrderStatus | string | null | undefined): string {
  if (!status) return '';
  return (status as { name?: string }).name ?? String(status);
}

export default function Orders(): JSX.Element {
  const { user } = useAuth();
  const navigate = useNavigate();
  const isWholesaler = user?.role === UserRole.WHOLESALER;

  const [orders, setOrders] = useState<Order[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [sortField, setSortField] = useState<SortField>('placedAt');
  const [sortOrder, setSortOrder] = useState<SortOrder>('desc');
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);
  const [menuPosition, setMenuPosition] = useState<{ top: number; left: number } | null>(null);
  const [reorderingOrderId, setReorderingOrderId] = useState<string | null>(null);

  useEffect(() => {
    const handleScroll = () => setOpenMenuId(null);
    const handleClickOutside = () => setOpenMenuId(null);

    window.addEventListener('scroll', handleScroll, true);
    window.addEventListener('click', handleClickOutside);
    return () => {
      window.removeEventListener('scroll', handleScroll, true);
      window.removeEventListener('click', handleClickOutside);
    };
  }, []);

  useEffect(() => {
    const fetchOrders = async () => {
      if (!user?.id) return;

      try {
        const data = isWholesaler
          ? await orderService.getByWholesaler(user.id)
          : await orderService.getByRetailer(user.id);
        setOrders(Array.isArray(data) ? data : []);
      } catch (error) {
        console.error('Failed to fetch orders', error);
        setOrders([]);
      } finally {
        setIsLoading(false);
      }
    };

    fetchOrders();
  }, [isWholesaler, user?.id]);

  const handleAction = async (action: OrderStatus | 'cancel', orderId: string, e: MouseEvent) => {
    e.stopPropagation();

    try {
      let updatedOrder: Order | null = null;

      if (action === 'cancel') {
        if (window.confirm('Are you sure you want to cancel this order?')) {
          updatedOrder = await orderService.cancel(orderId);
        }
      } else {
        updatedOrder = await orderService.updateStatus(orderId, action);
      }

      if (updatedOrder) {
        setOrders((prev) => prev.map((order) => (order.id === orderId ? updatedOrder as Order : order)));
        return;
      }

      setOrders((prev) =>
        prev.map((order) => {
          if (order.id !== orderId) return order;
          if (action === 'cancel') return { ...order, status: OrderStatus.CANCELLED } as Order;

          const actionName = getStatusName(action);
          const statusMap: Record<string, OrderStatus> = {
            [OrderStatus.CONFIRMED.name]: OrderStatus.CONFIRMED,
            [OrderStatus.PROCESSING.name]: OrderStatus.PROCESSING,
            [OrderStatus.SHIPPED.name]: OrderStatus.SHIPPED,
            [OrderStatus.DELIVERED.name]: OrderStatus.DELIVERED,
          };

          const newStatus = statusMap[actionName];
          return newStatus ? ({ ...order, status: newStatus } as Order) : order;
        }),
      );
    } catch (error) {
      console.error(`Failed to ${String(action)} order`, error);
      alert(`Failed to ${String(action)} order`);
    }
  };

  const handleReorder = async (order: Order, e: MouseEvent): Promise<void> => {
    e.stopPropagation();
    if (isWholesaler || !user?.id) return;

    if (reorderingOrderId === order.id) {
      return;
    }

    setReorderingOrderId(order.id);
    try {
      const { addedItems, failedItems, failedProductNames } = await reorderOrderToCart(user.id, order);

      if (addedItems === 0 && failedItems === 0) {
        alert('This order has no items to re-order.');
        return;
      }

      if (addedItems === 0) {
        alert('Unable to re-order items from this order right now.');
        return;
      }

      if (failedItems > 0) {
        const preview = failedProductNames.slice(0, 2).join(', ');
        const suffix = failedItems > 2 ? ', and more' : '';
        const details = preview ? ` Unavailable: ${preview}${suffix}.` : '';
        alert(`Added ${addedItems} item(s) to your cart. ${failedItems} item(s) could not be added.${details}`);
      } else {
        alert('Order items were added to your cart.');
      }

      navigate('/cart');
    } catch (error) {
      console.error('Failed to reorder order', error);
      alert('Failed to re-order this order');
    } finally {
      setReorderingOrderId(null);
      setOpenMenuId(null);
    }
  };

  const getStatusIcon = (status: OrderStatus | string) => {
    const statusName = getStatusName(status);
    switch (statusName) {
      case 'PENDING':
        return <Clock className="w-4 h-4" />;
      case 'CONFIRMED':
        return <CheckCircle2 className="w-4 h-4" />;
      case 'PROCESSING':
        return <Settings className="w-4 h-4 animate-spin" />;
      case 'SHIPPED':
        return <Truck className="w-4 h-4" />;
      case 'DELIVERED':
        return <CheckCircle2 className="w-4 h-4" />;
      case 'CANCELLED':
        return <XCircle className="w-4 h-4" />;
      case 'RETURNED':
        return <Package className="w-4 h-4" />;
      default:
        return <Package className="w-4 h-4" />;
    }
  };

  const getStatusColor = (status: OrderStatus | string) => {
    const statusName = getStatusName(status);
    switch (statusName) {
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
      case 'RETURNED':
        return 'bg-orange-100 text-orange-700';
      default:
        return 'bg-gray-100 text-gray-700';
    }
  };

  const filteredOrders = orders
    .filter((order) => {
      const matchesSearch =
        String(order.id).includes(searchTerm) ||
        order.retailerName?.toLowerCase().includes(searchTerm.toLowerCase());
      const matchesStatus = statusFilter === 'all' || getStatusName(order.status) === statusFilter;
      return matchesSearch && matchesStatus;
    })
    .sort((a, b) => {
      const aValue = a[sortField as keyof Order] as string | number | null | undefined;
      const bValue = b[sortField as keyof Order] as string | number | null | undefined;

      if (aValue == null) return 1;
      if (bValue == null) return -1;

      let comparison = 0;
      if (aValue > bValue) comparison = 1;
      if (aValue < bValue) comparison = -1;
      return sortOrder === 'asc' ? comparison : -comparison;
    });

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortOrder((prev) => (prev === 'asc' ? 'desc' : 'asc'));
      return;
    }
    setSortField(field);
    setSortOrder('desc');
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
      title={isWholesaler ? 'Customer Orders' : 'My Orders'}
      description={isWholesaler ? 'Manage and fulfill customer orders' : 'Track your order history and status'}
    >

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[
          { label: 'All Orders', count: orders.length, color: 'bg-navy-100 text-navy-700' },
          {
            label: 'Pending',
            count: orders.filter((order) => getStatusName(order.status) === 'PENDING').length,
            color: 'bg-gray-100 text-gray-700',
          },
          {
            label: 'Active',
            count: orders.filter((order) => ['CONFIRMED', 'PROCESSING', 'SHIPPED'].includes(getStatusName(order.status)))
              .length,
            color: 'bg-blue-100 text-blue-700',
          },
          {
            label: 'Completed',
            count: orders.filter((order) => getStatusName(order.status) === 'DELIVERED').length,
            color: 'bg-green-100 text-green-700',
          },
        ].map((stat) => (
          <div key={stat.label} className="bg-white rounded-xl shadow-card p-4">
            <p className="text-sm text-navy-500">{stat.label}</p>
            <p className="text-2xl font-bold text-navy-800 mt-1">{stat.count}</p>
          </div>
        ))}
      </div>

      <div className="flex flex-col md:flex-row gap-4 bg-white p-4 rounded-xl shadow-card">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            type="text"
            placeholder="Search by Order ID or Retailer..."
            className="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
        <div className="flex gap-4">
          <div className="relative">
            <select
              className="appearance-none bg-gray-50 border border-gray-200 rounded-lg px-4 py-2 pr-8 focus:outline-none focus:ring-2 focus:ring-primary-500"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="all">All Status</option>
              <option value={OrderStatus.PENDING.name}>Pending</option>
              <option value={OrderStatus.CONFIRMED.name}>Confirmed</option>
              <option value={OrderStatus.PROCESSING.name}>Processing</option>
              <option value={OrderStatus.SHIPPED.name}>Shipped</option>
              <option value={OrderStatus.DELIVERED.name}>Delivered</option>
              <option value={OrderStatus.CANCELLED.name}>Cancelled</option>
            </select>
            <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none" />
          </div>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th
                  className="text-left px-6 py-4 text-sm font-semibold text-navy-700 cursor-pointer hover:bg-gray-100"
                  onClick={() => handleSort('id')}
                >
                  Order ID
                </th>
                <th
                  className="text-left px-6 py-4 text-sm font-semibold text-navy-700 cursor-pointer hover:bg-gray-100"
                  onClick={() => handleSort('placedAt')}
                >
                  <div className="flex items-center gap-2">
                    Date
                    <ArrowUpDown className="w-3 h-3" />
                  </div>
                </th>
                {isWholesaler && (
                  <th
                    className="text-left px-6 py-4 text-sm font-semibold text-navy-700 cursor-pointer hover:bg-gray-100"
                    onClick={() => handleSort('retailerName')}
                  >
                    <div className="flex items-center gap-2">
                      <Store className="w-4 h-4" />
                      Retailer
                    </div>
                  </th>
                )}
                <th className="text-left px-6 py-4 text-sm font-semibold text-navy-700">
                  <div className="flex items-center gap-2">
                    <Tags className="w-4 h-4" />
                    Categories
                  </div>
                </th>
                <th
                  className="text-right px-6 py-4 text-sm font-semibold text-navy-700 cursor-pointer hover:bg-gray-100"
                  onClick={() => handleSort('subtotal')}
                >
                  Total
                </th>
                <th className="text-center px-6 py-4 text-sm font-semibold text-navy-700">Status</th>
                <th className="text-center px-6 py-4 text-sm font-semibold text-navy-700">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filteredOrders.map((order) => {
                const categories = Array.from(new Set(order.items.map((item) => item.productCategory))).slice(0, 3);
                const status = getStatusName(order.status);

                return (
                  <tr
                    key={order.id}
                    className="hover:bg-gray-50 transition-colors cursor-pointer"
                    onClick={() => navigate(`/orders/${order.id}`)}
                  >
                    <td className="px-6 py-4">
                      <p className="font-medium text-navy-800">#{order.id}</p>
                    </td>
                    <td className="px-6 py-4 text-navy-600">{new Date(order.placedAt).toLocaleDateString()}</td>
                    {isWholesaler && <td className="px-6 py-4 text-navy-800 font-medium">{order.retailerName}</td>}
                    <td className="px-6 py-4 text-navy-600">
                      <div className="flex flex-wrap gap-1">
                        {categories.map((category) => (
                          <span key={category} className="px-2 py-0.5 bg-gray-100 rounded text-xs">
                            {category}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="px-6 py-4 text-right font-semibold text-navy-800">SAR {order.subtotal.toFixed(2)}</td>
                    <td className="px-6 py-4 text-center">
                      <span
                        className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-sm font-medium capitalize ${getStatusColor(order.status)}`}
                      >
                        {getStatusIcon(order.status)}
                        {status.toLowerCase().replace('_', ' ')}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center justify-center gap-2" onClick={(e) => e.stopPropagation()}>
                        {isWholesaler ? (
                          <>
                            {status === 'PENDING' && (
                              <button
                                className="px-3 py-1 bg-indigo-50 text-indigo-600 rounded-lg text-xs font-medium hover:bg-indigo-100"
                                onClick={(e) => handleAction(OrderStatus.CONFIRMED, order.id, e)}
                              >
                                Accept
                              </button>
                            )}
                            {status === 'CONFIRMED' && (
                              <button
                                className="px-3 py-1 bg-amber-50 text-amber-600 rounded-lg text-xs font-medium hover:bg-amber-100"
                                onClick={(e) => handleAction(OrderStatus.PROCESSING, order.id, e)}
                              >
                                Process
                              </button>
                            )}
                            {status === 'PROCESSING' && (
                              <button
                                className="px-3 py-1 bg-blue-50 text-blue-600 rounded-lg text-xs font-medium hover:bg-blue-100"
                                onClick={(e) => handleAction(OrderStatus.SHIPPED, order.id, e)}
                              >
                                Ship
                              </button>
                            )}
                            {status === 'SHIPPED' && (
                              <button
                                className="px-3 py-1 bg-green-50 text-green-600 rounded-lg text-xs font-medium hover:bg-green-100"
                                onClick={(e) => handleAction(OrderStatus.DELIVERED, order.id, e)}
                              >
                                Deliver
                              </button>
                            )}
                          </>
                        ) : (
                          !['DELIVERED', 'SHIPPED', 'CANCELLED'].includes(status) && (
                            <button
                              className="p-2 text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                              title="Cancel Order"
                              onClick={(e) => handleAction('cancel', order.id, e)}
                            >
                              <XCircle className="w-4 h-4" />
                            </button>
                          )
                        )}

                        <div className="relative">
                          <button
                            className={`p-2 rounded-lg transition-colors ${openMenuId === order.id ? 'bg-gray-100 text-navy-800' : 'text-navy-500 hover:text-navy-700 hover:bg-gray-100'}`}
                            title="More Actions"
                            onClick={(e) => {
                              e.stopPropagation();
                              const rect = e.currentTarget.getBoundingClientRect();
                              setMenuPosition({ top: rect.bottom + 8, left: rect.right - 192 });
                              setOpenMenuId((prev) => (prev === order.id ? null : order.id));
                            }}
                          >
                            <MoreVertical className="w-4 h-4" />
                          </button>

                          {openMenuId === order.id &&
                            menuPosition &&
                            createPortal(
                              <div
                                className="fixed w-48 bg-white rounded-xl shadow-xl border border-gray-100 py-2 z-[9999] animate-in fade-in slide-in-from-top-2 duration-200 dark:border-white/10 dark:bg-slate-900"
                                style={{ top: menuPosition.top, left: menuPosition.left }}
                                onClick={(e) => e.stopPropagation()}
                              >
                                <button
                                  className="w-full flex items-center gap-3 px-4 py-2 text-sm text-navy-700 hover:bg-gray-50 transition-colors dark:text-slate-200 dark:hover:bg-slate-800"
                                  onClick={() => {
                                    setOpenMenuId(null);
                                    navigate(`/orders/${order.id}`);
                                  }}
                                >
                                  <Eye className="w-4 h-4 text-navy-400 dark:text-slate-400" />
                                  View Details
                                </button>
                                {!isWholesaler && (
                                  <button
                                    disabled={reorderingOrderId === order.id}
                                    className={`w-full flex items-center gap-3 px-4 py-2 text-sm transition-colors dark:hover:bg-slate-800 ${
                                      reorderingOrderId === order.id
                                        ? 'text-navy-400 cursor-not-allowed dark:text-slate-500'
                                        : 'text-navy-700 hover:bg-gray-50 dark:text-slate-200'
                                    }`}
                                    onClick={(e) => {
                                      void handleReorder(order, e);
                                    }}
                                  >
                                    <Copy className="w-4 h-4 text-navy-400 dark:text-slate-400" />
                                    {reorderingOrderId === order.id ? 'Re-ordering...' : 'Re-order'}
                                  </button>
                                )}
                                <button
                                  className="w-full flex items-center gap-3 px-4 py-2 text-sm text-navy-700 hover:bg-gray-50 transition-colors dark:text-slate-200 dark:hover:bg-slate-800"
                                  onClick={() => {
                                  setOpenMenuId(null);
                                      navigate(`/orders/${order.id}`);
                                        }}
                                >
                                  <FileText className="w-4 h-4 text-navy-400 dark:text-slate-400" />
                                  Download Invoice
                                </button>
                                <button
                                  className="w-full flex items-center gap-3 px-4 py-2 text-sm text-navy-700 hover:bg-gray-50 transition-colors dark:text-slate-200 dark:hover:bg-slate-800"
                                  onClick={() => {
                                    alert('Support feature coming soon!');
                                    setOpenMenuId(null);
                                  }}
                                >
                                  <MessageSquare className="w-4 h-4 text-navy-400 dark:text-slate-400" />
                                  Contact Support
                                </button>
                                {!isWholesaler && !['DELIVERED', 'SHIPPED', 'CANCELLED'].includes(status) && (
                                  <button
                                    className="w-full flex items-center gap-3 px-4 py-2 text-sm text-red-600 hover:bg-red-50 transition-colors border-t border-gray-50 mt-1 dark:border-white/10 dark:hover:bg-red-500/15"
                                    onClick={(e) => {
                                      void handleAction('cancel', order.id, e);
                                      setOpenMenuId(null);
                                    }}
                                  >
                                    <XCircle className="w-4 h-4" />
                                    Cancel Order
                                  </button>
                                )}
                              </div>,
                              document.body,
                            )}
                        </div>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>

          {filteredOrders.length === 0 && <div className="p-8 text-center text-navy-500">No orders found matching your criteria.</div>}
        </div>
      </div>
    </PageShell>
  );
}
