import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Calendar, Download, MapPin, Package, Truck } from 'lucide-react';
import PageShell from '../components/PageShell';
import { orderService } from '../services/orderService';
import { Order } from '../types';

function statusName(status: unknown): string {
  if (!status) return '';
  return (status as { name?: string }).name ?? String(status);
}

export default function OrderDetails(): JSX.Element {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [order, setOrder] = useState<Order | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchOrder = async () => {
      if (!id) return;

      try {
        const data = await orderService.getById(String(id));
        setOrder(data);
      } catch (error) {
        console.error('Failed to fetch order', error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchOrder();
  }, [id]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600" />
      </div>
    );
  }

  if (!order) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
        <p className="text-navy-500">Order not found</p>
        <button onClick={() => navigate('/orders')} className="text-primary-600 hover:underline">
          Back to Orders
        </button>
      </div>
    );
  }

  return (
    <PageShell
      title={`Order #${order.id}`}
      description={`Placed on ${new Date(order.placedAt).toLocaleString()}`}
      actions={
        <div className="flex items-center gap-2">
          <button
            onClick={() => navigate('/orders')}
            className="flex items-center gap-2 px-4 py-2 border border-gray-200 rounded-lg text-navy-600 hover:bg-gray-50 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Back
          </button>
          <button className="flex items-center gap-2 px-4 py-2 border border-gray-200 rounded-lg text-navy-600 hover:bg-gray-50 transition-colors">
            <Download className="w-4 h-4" />
            Download Invoice
          </button>
        </div>
      }
    >
      <div className="inline-flex items-center gap-2 text-navy-500 text-sm -mt-2">
        <Calendar className="w-4 h-4" />
        <span className="bg-gray-100 px-2 py-0.5 rounded text-sm font-medium text-navy-700 capitalize">
          {statusName(order.status).toLowerCase().replace('_', ' ')}
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="md:col-span-2 space-y-6">
          <div className="bg-white rounded-xl shadow-card overflow-hidden">
            <div className="p-6 border-b border-gray-100">
              <h2 className="font-semibold text-navy-800">Order Items</h2>
            </div>
            <div className="max-w-[100vw] overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="text-left px-6 py-3 text-sm font-medium text-navy-500">Product</th>
                    <th className="text-center px-6 py-3 text-sm font-medium text-navy-500">Price</th>
                    <th className="text-center px-6 py-3 text-sm font-medium text-navy-500">Quantity</th>
                    <th className="text-right px-6 py-3 text-sm font-medium text-navy-500">Total</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {order.items.map((item) => (
                    <tr key={item.id}>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <div className="w-12 h-12 bg-gray-100 rounded-lg flex items-center justify-center shrink-0">
                            {item.productImageUrl ? (
                              <img
                                src={item.productImageUrl}
                                alt={item.productName}
                                className="w-full h-full object-cover rounded-lg"
                              />
                            ) : (
                              <Package className="w-6 h-6 text-gray-400" />
                            )}
                          </div>
                          <div>
                            <p className="font-medium text-navy-800">{item.productName}</p>
                            <p className="text-xs text-navy-500">{item.productCategory}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 text-center text-navy-600">SAR {item.unitPrice.toFixed(2)}</td>
                      <td className="px-6 py-4 text-center text-navy-600">{item.quantity}</td>
                      <td className="px-6 py-4 text-right font-semibold text-navy-800">
                        SAR {(item.unitPrice * item.quantity).toFixed(2)}
                      </td>
                    </tr>
                  ))}
                </tbody>
                <tfoot className="bg-gray-50">
                  <tr>
                    <td colSpan={3} className="px-6 py-4 text-right font-medium text-navy-600">
                      Subtotal
                    </td>
                    <td className="px-6 py-4 text-right font-bold text-navy-800">SAR {order.subtotal.toFixed(2)}</td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>
        </div>

        <div className="space-y-6">
          <div className="bg-white rounded-xl shadow-card p-6">
            <h2 className="font-semibold text-navy-800 mb-4">Customer Details</h2>
            <div className="space-y-4">
              <div className="flex items-start gap-3">
                <div className="p-2 bg-blue-50 text-blue-600 rounded-lg">
                  <Package className="w-4 h-4" />
                </div>
                <div>
                  <p className="text-sm font-medium text-navy-800">{order.retailerName}</p>
                  <p className="text-xs text-navy-500">Retailer ID: {order.retailerId}</p>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-card p-6">
            <h2 className="font-semibold text-navy-800 mb-4">Shipping Information</h2>
            <div className="space-y-4">
              <div className="flex items-start gap-3">
                <div className="p-2 bg-purple-50 text-purple-600 rounded-lg">
                  <Truck className="w-4 h-4" />
                </div>
                <div>
                  <p className="text-sm font-medium text-navy-800">
                    {statusName(order.shippingMethod || 'Standard').toLowerCase().replace('_', ' ')}
                  </p>
                  {order.trackingNumber && (
                    <p className="text-xs text-navy-500 mt-1">
                      Tracking:{' '}
                      <a href={order.trackingUrl || '#'} className="text-primary-600 hover:underline">
                        {order.trackingNumber}
                      </a>
                    </p>
                  )}
                </div>
              </div>
              <div className="flex items-start gap-3">
                <div className="p-2 bg-orange-50 text-orange-600 rounded-lg">
                  <MapPin className="w-4 h-4" />
                </div>
                <div>
                  <p className="text-sm text-navy-600">Shipping Address usually goes here (fetch from User/Profile)</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </PageShell>
  );
}
