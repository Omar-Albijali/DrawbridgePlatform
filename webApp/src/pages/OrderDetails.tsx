import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Calendar, CheckCircle2, Copy, Download, MapPin, Package, Truck } from 'lucide-react';import { useTranslation } from 'react-i18next';
import PageShell from '../components/PageShell';
import { useAuth } from '../contexts/AuthContext';
import { orderService } from '../services/orderService';
import { reorderOrderToCart } from '../utils/reorderOrder';
import { enumToken, formatCurrency, formatDate, formatDateTime, orderStatusLabel, shippingMethodLabel } from '../i18n/display';import { UserRole, type Order, type OrderItem } from '../types';
import { useCart } from '../contexts/CartContext';

export default function OrderDetails(): JSX.Element {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const roleName = enumToken(user?.role);
  const isRetailer = roleName === UserRole.RETAILER.name;

  const [order, setOrder] = useState<Order | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isReordering, setIsReordering] = useState(false);
  const [isConfirmingDelivery, setIsConfirmingDelivery] = useState(false);
  const { addToCart } = useCart();

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
        <p className="text-navy-500">{t('orders.detail.notFound')}</p>
        <button onClick={() => navigate('/orders')} className="text-primary-600 hover:underline">
          {t('orders.detail.backToOrders')}
        </button>
      </div>
    );
  }

  const handleReorder = async (): Promise<void> => {
    if (!user?.id || !isRetailer || isReordering) {
      return;
    }

    setIsReordering(true);
    try {
      const { addedItems, failedItems, failedProductNames } = await reorderOrderToCart(addToCart, order);

      if (addedItems === 0 && failedItems === 0) {
        alert(t('orders.noItemsToReorder'));
        return;
      }

      if (addedItems === 0) {
        alert(t('orders.unableToReorder'));
        return;
      }

      if (failedItems > 0) {
        const preview = failedProductNames.slice(0, 2).join(', ');
        const suffix = failedItems > 2 ? t('orders.andMore') : '';
        const details = preview ? t('orders.unavailableItems', { items: preview, suffix }) : '';
        alert(t('orders.reorderPartial', { added: addedItems, failed: failedItems, details }));
      } else {
        alert(t('orders.reorderSuccess'));
      }

      navigate('/cart');
    } catch (error) {
      console.error('Failed to reorder order', error);
      alert(t('orders.reorderFailed'));
    } finally {
      setIsReordering(false);
    }
  };

  const handleConfirmDelivery = async (): Promise<void> => {
    if (!order || !isRetailer || isConfirmingDelivery) {
      return;
    }

    if (!window.confirm(t('orders.confirmDeliveryPrompt'))) {
      return;
    }

    setIsConfirmingDelivery(true);
    try {
      const updatedOrder = await orderService.confirmDelivery(order.id);
      setOrder(updatedOrder);
      alert(t('orders.deliveryConfirmed'));
    } catch (error) {
      console.error('Failed to confirm delivery', error);
      alert(t('orders.confirmDeliveryFailed'));
    } finally {
      setIsConfirmingDelivery(false);
    }
  };

  const canConfirmDelivery = isRetailer && enumToken(order.status) === 'SHIPPED';

  return (
    <PageShell
      title={t('orders.detail.title', { id: order.id })}
      description={t('orders.detail.placedOn', { date: formatDateTime(order.placedAt) })}
      actions={
        <div className="flex items-center gap-2">
          <button
            onClick={() => navigate('/orders')}
            className="flex items-center gap-2 px-4 py-2 border border-gray-200 rounded-lg text-navy-600 hover:bg-gray-50 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            {t('orders.detail.back')}
          </button>
          <button
            onClick={() => void handleReorder()}
            disabled={!isRetailer || isReordering}
            className={`flex items-center gap-2 px-4 py-2 border rounded-lg transition-colors ${
                !isRetailer || isReordering
                ? 'border-gray-100 text-gray-400 cursor-not-allowed'
                : 'border-gray-200 text-navy-600 hover:bg-gray-50'
            }`}
          >
            <Copy className="w-4 h-4" />
            {isReordering ? t('orders.reordering') : t('orders.reorder')}
          </button>
          {canConfirmDelivery && (
              <button
                  onClick={() => void handleConfirmDelivery()}
                  disabled={isConfirmingDelivery}
                  className="flex items-center gap-2 px-4 py-2 border border-green-200 rounded-lg text-green-700 hover:bg-green-50 transition-colors disabled:cursor-not-allowed disabled:opacity-60"
              >
                <CheckCircle2 className="w-4 h-4" />
                {isConfirmingDelivery ? t('orders.confirmingDelivery') : t('orders.confirmDelivery')}
              </button>
          )}
          <button
  onClick={() => {
    const printWindow = window.open('', '_blank');
    if (!printWindow) return;
    printWindow.document.write(`
      <html>
        <head>
          <title>${t('orders.detail.invoiceTitle', { id: order.id })}</title>
          <style>
            body { font-family: Arial, sans-serif; padding: 40px; color: #1a1a2e; }
            h1 { font-size: 24px; margin-bottom: 4px; }
            .subtitle { color: #666; margin-bottom: 32px; }
            table { width: 100%; border-collapse: collapse; margin-top: 24px; }
            th { background: #f5f5f5; text-align: left; padding: 10px 12px; font-size: 13px; }
            td { padding: 10px 12px; border-bottom: 1px solid #eee; font-size: 14px; }
            .total-row td { font-weight: bold; background: #f5f5f5; }
            .header { display: flex; justify-content: space-between; margin-bottom: 32px; }
            .company { font-size: 28px; font-weight: bold; color: #16a34a; }
            .info { text-align: right; color: #666; font-size: 13px; }
          </style>
        </head>
        <body>
          <div class="header">
            <div class="company">Drawbridge</div>
            <div class="info">
              <div>${t('orders.detail.invoice', { id: order.id })}</div>
              <div>${t('orders.detail.invoiceDate', { date: formatDate(order.placedAt) })}</div>
              <div>${t('orders.detail.invoiceStatus', { status: orderStatusLabel(t, order.status) })}</div>
            </div>
          </div>
          <div><strong>${t('orders.detail.invoiceRetailer')}</strong> ${order.retailerName}</div>
          <table>
            <thead>
              <tr>
                <th>${t('common.product')}</th>
                <th>${t('common.category')}</th>
                <th>${t('orders.detail.unitPrice')}</th>
                <th>${t('orders.detail.qty')}</th>
                <th>${t('common.total')}</th>
              </tr>
            </thead>
            <tbody>
              ${order.items.map((item: OrderItem) => `
                <tr>
                  <td>${item.productName}</td>
                  <td>${item.productCategory}</td>
                  <td>${formatCurrency(item.unitPrice)}</td>
                  <td>${item.quantity}</td>
                  <td>${formatCurrency(item.unitPrice * item.quantity)}</td>
                </tr>
              `).join('')}
            </tbody>
            <tfoot>
              <tr class="total-row">
                <td colspan="4">${t('common.total')}</td>
                <td>${formatCurrency(order.subtotal)}</td>
              </tr>
            </tfoot>
          </table>
        </body>
      </html>
    `);
    printWindow.document.close();
    printWindow.focus();
    printWindow.print();
  }}
  className="flex items-center gap-2 px-4 py-2 border border-gray-200 rounded-lg text-navy-600 hover:bg-gray-50 transition-colors"
>
  <Download className="w-4 h-4" />
  {t('orders.downloadInvoice')}
</button>
        </div>
      }
    >
      <div className="inline-flex items-center gap-2 text-navy-500 text-sm -mt-2">
        <Calendar className="w-4 h-4" />
        <span className="bg-gray-100 px-2 py-0.5 rounded text-sm font-medium text-navy-700 capitalize">
          {orderStatusLabel(t, order.status)}
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="md:col-span-2 space-y-6">
          <div className="bg-white rounded-xl shadow-card overflow-hidden">
            <div className="p-6 border-b border-gray-100">
              <h2 className="font-semibold text-navy-800">{t('orders.detail.orderItems')}</h2>
            </div>
            <div className="max-w-[100vw] overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="text-left px-6 py-3 text-sm font-medium text-navy-500">{t('common.product')}</th>
                    <th className="text-center px-6 py-3 text-sm font-medium text-navy-500">{t('common.price')}</th>
                    <th className="text-center px-6 py-3 text-sm font-medium text-navy-500">{t('common.quantity')}</th>
                    <th className="text-right px-6 py-3 text-sm font-medium text-navy-500">{t('common.total')}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {order.items.map((item: OrderItem) => (
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
                      <td className="px-6 py-4 text-center text-navy-600">{formatCurrency(item.unitPrice)}</td>
                      <td className="px-6 py-4 text-center text-navy-600">{item.quantity}</td>
                      <td className="px-6 py-4 text-right font-semibold text-navy-800">
                        {formatCurrency(item.unitPrice * item.quantity)}
                      </td>
                    </tr>
                  ))}
                </tbody>
                <tfoot className="bg-gray-50">
                  <tr>
                    <td colSpan={3} className="px-6 py-4 text-right font-medium text-navy-600">
                      {t('common.subtotal')}
                    </td>
                    <td className="px-6 py-4 text-right font-bold text-navy-800">{formatCurrency(order.subtotal)}</td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>
        </div>

        <div className="space-y-6">
          <div className="bg-white rounded-xl shadow-card p-6">
            <h2 className="font-semibold text-navy-800 mb-4">{t('orders.detail.customerDetails')}</h2>
            <div className="space-y-4">
              <div className="flex items-start gap-3">
                <div className="p-2 bg-blue-50 text-blue-600 rounded-lg">
                  <Package className="w-4 h-4" />
                </div>
                <div>
                  <p className="text-sm font-medium text-navy-800">{order.retailerName}</p>
                  <p className="text-xs text-navy-500">{t('orders.detail.retailerId', { id: order.retailerId })}</p>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-card p-6">
            <h2 className="font-semibold text-navy-800 mb-4">{t('orders.detail.shippingInformation')}</h2>
            <div className="space-y-4">
              <div className="flex items-start gap-3">
                <div className="p-2 bg-purple-50 text-purple-600 rounded-lg">
                  <Truck className="w-4 h-4" />
                </div>
                <div>
                  <p className="text-sm font-medium text-navy-800">
                    {shippingMethodLabel(t, order.shippingMethod)}
                  </p>
                  {order.trackingNumber && (
                    <p className="text-xs text-navy-500 mt-1">
                      {t('orders.detail.tracking')}{' '}
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
                  <p className="text-sm text-navy-600">{t('orders.detail.shippingAddressPlaceholder')}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </PageShell>
  );
}
