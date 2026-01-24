import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { orderService } from '../services/orderService';
import { Order, Notification, OrderStatus, NotificationType } from '../types';
import {
    monthlyExpensesData,
    monthlySalesData,
    inventoryCompositionData,
    orderStatusData
} from '../data/constants';
import {
    Package,
    TrendingUp,
    TrendingDown,
    AlertTriangle,
    Truck,
    DollarSign,
    ShoppingBag,
    BarChart3
} from 'lucide-react';
import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
    PieChart,
    Pie,
    Cell,
    Legend
} from 'recharts';

interface KPICardProps {
    title: string;
    value: string | number;
    change?: number;
    icon: React.ReactNode;
    iconBg: string;
    valueColor?: string;
}

const KPICard: React.FC<KPICardProps> = ({ title, value, change, icon, iconBg, valueColor }) => (
    <div className="card">
        <div className="flex items-start justify-between">
            <div>
                <p className="text-sm font-medium text-navy-500 mb-1">{title}</p>
                <p className={`text-3xl font-bold ${valueColor || 'text-navy-800'}`}>{value}</p>
                {change !== undefined && (
                    <div className={`flex items-center gap-1 mt-2 text-sm ${change >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                        {change >= 0 ? <TrendingUp className="w-4 h-4" /> : <TrendingDown className="w-4 h-4" />}
                        <span>{Math.abs(change)}% vs last month</span>
                    </div>
                )}
            </div>
            <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${iconBg}`}>
                {icon}
            </div>
        </div>
    </div>
);

const Dashboard: React.FC = () => {
    const { user } = useAuth();
    const isRetailer = (user?.role as any) === 'RETAILER' || user?.role?.name === 'RETAILER';
    const [orders, setOrders] = useState<Order[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    // Mock notifications for now as there is no backend service yet
    const [notifications] = useState<Notification[]>([
        { id: '1', type: NotificationType.SYSTEM, message: 'Welcome to your new dashboard', time: 'Just now', read: false } as unknown as Notification,
        { id: '2', type: NotificationType.SYSTEM, message: 'System maintenance scheduled for Sunday', time: '1 day ago', read: true } as unknown as Notification,
    ]);

    useEffect(() => {
        const fetchData = async () => {
            if (user?.id) {
                try {
                    let data: Order[] = [];
                    const userRole = (user.role as any)?.name || (user.role as any);
                    if (userRole === 'WHOLESALER') {
                        data = await orderService.getByWholesaler(user.id);
                    } else {
                        data = await orderService.getByRetailer(user.id);
                    }
                    setOrders(Array.isArray(data) ? data : []);
                } catch (error) {
                    console.error("Failed to fetch dashboard data", error);
                } finally {
                    setIsLoading(false);
                }
            }
        };
        fetchData();
    }, [user]);

    // Calculate KPIs
    const totalOrders = orders.length;
    const totalAmount = orders.reduce((sum, order) => sum + (order.subtotal), 0);
    const getStatusStr = (status: OrderStatus | string): string => {
        if (!status) return "";
        return (status as any).name || (status as string);
    };

    const pendingCount = orders.filter(o => getStatusStr(o.status) === 'PENDING').length;
    const processingCount = orders.filter(o => getStatusStr(o.status) === 'PROCESSING').length;

    // Retailer KPIs
    const retailerKPIs = [
        {
            title: 'Total Orders',
            value: totalOrders.toString(),
            // change: 12, // TODO: calculate change
            icon: <ShoppingBag className="w-6 h-6 text-primary-600" />,
            iconBg: 'bg-primary-100'
        },
        {
            title: 'Pending Orders',
            value: pendingCount.toString(),
            icon: <AlertTriangle className="w-6 h-6 text-amber-600" />,
            iconBg: 'bg-amber-100',
            valueColor: 'text-amber-600'
        },
        {
            title: 'Total Spend',
            value: `SAR ${totalAmount.toLocaleString()}`,
            // change: -5,
            icon: <DollarSign className="w-6 h-6 text-green-600" />,
            iconBg: 'bg-green-100'
        },
        {
            title: 'Processing',
            value: processingCount.toString(),
            icon: <Truck className="w-6 h-6 text-blue-600" />,
            iconBg: 'bg-blue-100'
        },
    ];

    // Wholesaler KPIs
    const wholesalerKPIs = [
        {
            title: 'Total Orders',
            value: totalOrders.toString(),
            // change: 18,
            icon: <Package className="w-6 h-6 text-primary-600" />,
            iconBg: 'bg-primary-100'
        },
        {
            title: 'Pending Orders',
            value: pendingCount.toString(),
            icon: <AlertTriangle className="w-6 h-6 text-amber-600" />,
            iconBg: 'bg-amber-100',
            valueColor: 'text-amber-600'
        },
        {
            title: 'Total Revenue',
            value: `SAR ${totalAmount.toLocaleString()}`,
            // change: 24,
            icon: <DollarSign className="w-6 h-6 text-green-600" />,
            iconBg: 'bg-green-100'
        },
        {
            title: 'Processing',
            value: processingCount.toString(),
            icon: <BarChart3 className="w-6 h-6 text-purple-600" />,
            iconBg: 'bg-purple-100'
        },
    ];

    const kpis = isRetailer ? retailerKPIs : wholesalerKPIs;
    const chartData = isRetailer ? monthlyExpensesData : monthlySalesData;
    const pieData = isRetailer ? inventoryCompositionData : orderStatusData;

    const getStatusColor = (status: OrderStatus | string) => {
        const name = getStatusStr(status);
        switch (name) {
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
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* Page Header */}
            <div>
                <h1 className="text-2xl font-bold text-navy-800">
                    Welcome back, {user?.name?.split(' ')[0]}!
                </h1>
                <p className="text-navy-500 mt-1">
                    Here's what's happening with your {isRetailer ? 'business' : 'store'} today.
                </p>
            </div>

            {/* KPI Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                {kpis.map((kpi, index) => (
                    <KPICard key={index} {...kpi} />
                ))}
            </div>

            {/* Charts Row */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Bar Chart */}
                <div className="lg:col-span-2 card">
                    <h3 className="text-lg font-semibold text-navy-800 mb-4">
                        {isRetailer ? 'Monthly Expenses' : 'Monthly Sales'}
                    </h3>
                    <div className="h-80">
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart data={chartData}>
                                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                                <XAxis dataKey="month" stroke="#627d98" />
                                <YAxis stroke="#627d98" />
                                <Tooltip
                                    contentStyle={{
                                        backgroundColor: '#fff',
                                        border: '1px solid #e5e7eb',
                                        borderRadius: '8px'
                                    }}
                                    formatter={(value) => {
                                        const n = typeof value === 'number' ? value : 0;
                                        return [`SAR ${n.toLocaleString()}`, isRetailer ? 'Expenses' : 'Sales'];
                                    }}
                                />
                                <Bar
                                    dataKey="amount"
                                    fill="#3b82f6"
                                    radius={[4, 4, 0, 0]}
                                />
                            </BarChart>
                        </ResponsiveContainer>
                    </div>
                </div>

                {/* Pie Chart */}
                <div className="card">
                    <h3 className="text-lg font-semibold text-navy-800 mb-4">
                        {isRetailer ? 'Inventory Composition' : 'Order Status'}
                    </h3>
                    <div className="h-80">
                        <ResponsiveContainer width="100%" height="100%">
                            <PieChart>
                                <Pie
                                    data={pieData}
                                    cx="50%"
                                    cy="50%"
                                    innerRadius={60}
                                    outerRadius={80}
                                    paddingAngle={5}
                                    dataKey="value"
                                >
                                    {pieData.map((entry, index) => (
                                        <Cell key={`cell-${index}`} fill={entry.color} />
                                    ))}
                                </Pie>
                                <Tooltip
                                    contentStyle={{
                                        backgroundColor: '#fff',
                                        border: '1px solid #e5e7eb',
                                        borderRadius: '8px'
                                    }}
                                    formatter={(value) => {
                                        const n = typeof value === 'number' ? value : 0;
                                        return [`${n}%`, ''];
                                    }}
                                />
                                <Legend />
                            </PieChart>
                        </ResponsiveContainer>
                    </div>
                </div>
            </div>

            {/* Bottom Row */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Recent Orders */}
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
                                    <p className="font-semibold text-navy-800">SAR {order.subtotal.toFixed(2)}</p>
                                    <span className={`text-xs px-2 py-1 rounded-full capitalize ${getStatusColor(order.status)}`}>
                                        {getStatusStr(order.status).toLowerCase().replace('_', ' ')}
                                    </span>
                                </div>
                            </div>
                        ))}
                        {orders.length === 0 && (
                            <p className="text-center text-navy-500 py-4">No recent orders</p>
                        )}
                    </div>
                </div>

                {/* Recent Activity */}
                <div className="card">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="text-lg font-semibold text-navy-800">Recent Activity</h3>
                        <button className="text-sm text-primary-600 hover:text-primary-700 font-medium">
                            View all
                        </button>
                    </div>
                    <div className="space-y-4">
                        {notifications.slice(0, 5).map((notification) => (
                            <div key={notification.id} className="flex items-start gap-3">
                                <div className={`w-2 h-2 rounded-full mt-2 ${notification.type === NotificationType.ORDER ? 'bg-primary-500' :
                                    notification.type === NotificationType.STOCK ? 'bg-amber-500' :
                                        notification.type === NotificationType.PAYMENT ? 'bg-green-500' : 'bg-gray-400'
                                    }`} />
                                <div className="flex-1">
                                    <p className="text-sm text-navy-700">{notification.message}</p>
                                    <p className="text-xs text-navy-400 mt-1">{notification.time}</p>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Dashboard;

