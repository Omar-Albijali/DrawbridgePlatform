import { ArrowLeft, ArrowRight, Minus, Plus, ShoppingBag, Trash2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import PageShell from '../components/PageShell';
import { useCart } from '../contexts/CartContext';
import { formatCurrency } from '../i18n/display';

export default function Cart(): JSX.Element {
  const { t } = useTranslation();
  const { items, itemCount, subtotal, tax, total, updateQuantity, removeFromCart, clearCart } = useCart();

  if (itemCount === 0) {
    return (
      <PageShell title={t('cart.title')} description={t('cart.emptyDescription')} className="buyer-cart">
        <div className="buyer-cart__empty text-center py-16">
          <div className="w-24 h-24 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <ShoppingBag className="w-12 h-12 text-navy-400" />
          </div>
          <h2 className="text-2xl font-bold text-navy-800 mb-3">{t('cart.emptyTitle')}</h2>
          <p className="text-navy-500 mb-6 max-w-md mx-auto">
            {t('cart.emptyText')}
          </p>
          <Link to="/marketplace" className="btn-primary inline-flex items-center gap-2">
            <ShoppingBag className="w-4 h-4" />
            {t('cart.browseMarketplace')}
          </Link>
        </div>
      </PageShell>
    );
  }

  return (
    <PageShell
      title={t('cart.title')}
      description={t('cart.description', { count: itemCount })}
      className="buyer-cart"
      actions={
        <button
          type="button"
          onClick={() => void clearCart()}
          className="text-red-600 hover:text-red-700 text-sm font-medium flex items-center gap-1"
        >
          <Trash2 className="w-4 h-4" />
          {t('cart.clear')}
        </button>
      }
    >

      <div className="buyer-cart__layout grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-4">
          {itemCount > 0 && items.length === 0 && (
            <div className="bg-white rounded-xl shadow-card p-6 text-sm text-navy-500">{t('cart.loadingProducts')}</div>
          )}
          {items.map((item) => (
            <div key={item.product.id} className="buyer-cart__item bg-white rounded-xl shadow-card p-4 flex gap-4">
              <div className="w-24 h-24 rounded-lg overflow-hidden bg-gray-100 flex-shrink-0">
                {item.product.image ? (
                  <img src={item.product.image} alt={item.product.name} className="w-full h-full object-cover" />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-xs text-navy-400">{t('cart.noImage')}</div>
                )}
              </div>

              <div className="flex-1 min-w-0">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <h3 className="font-semibold text-navy-800">{item.product.name}</h3>
                    <p className="text-sm text-navy-500">{item.product.brand}</p>
                    <p className="text-sm text-navy-400 mt-1">{t('cart.supplier', { supplier: item.product.supplier })}</p>
                  </div>
                  <button
                    type="button"
                    onClick={() => void removeFromCart(item.product.id)}
                    className="p-2 text-navy-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>

                <div className="flex items-center justify-between mt-4">
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => void updateQuantity(item.product.id, item.quantity - 1)}
                      className="w-8 h-8 flex items-center justify-center bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
                    >
                      <Minus className="w-4 h-4" />
                    </button>
                    <span className="w-12 text-center font-semibold text-navy-800">{item.quantity}</span>
                    <button
                      type="button"
                      onClick={() => void updateQuantity(item.product.id, item.quantity + 1)}
                      className="w-8 h-8 flex items-center justify-center bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
                    >
                      <Plus className="w-4 h-4" />
                    </button>
                  </div>

                  <div className="buyer-cart__price text-right">
                    <p className="text-sm text-navy-500">{t('cart.each', { amount: formatCurrency(item.product.price) })}</p>
                    <p className="font-bold text-navy-800">{formatCurrency(item.product.price * item.quantity)}</p>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>

        <div className="lg:col-span-1">
          <div className="buyer-cart__summary bg-white rounded-xl shadow-card p-6 sticky top-24">
            <h3 className="text-lg font-semibold text-navy-800 mb-4">{t('cart.summary')}</h3>

            <div className="space-y-3 mb-6">
              <div className="flex items-center justify-between text-navy-600">
                <span>{t('cart.subtotalItems', { count: itemCount })}</span>
                <span>{formatCurrency(subtotal)}</span>
              </div>
              <div className="flex items-center justify-between text-navy-600">
                <span>{t('common.vat')}</span>
                <span>{formatCurrency(tax)}</span>
              </div>
              <div className="flex items-center justify-between text-navy-600">
                <span>{t('common.shipping')}</span>
                <span className="text-green-600 font-medium">{t('common.free')}</span>
              </div>
              <hr className="border-gray-200" />
              <div className="flex items-center justify-between text-lg font-bold text-navy-800">
                <span>{t('common.total')}</span>
                <span>{formatCurrency(total)}</span>
              </div>
            </div>

            <Link to="/checkout" className="w-full btn-primary flex items-center justify-center gap-2 py-3">
              {t('cart.proceed')}
              <ArrowRight className="w-4 h-4" />
            </Link>

            <Link to="/marketplace" className="w-full btn-secondary flex items-center justify-center gap-2 py-3 mt-3">
              <ArrowLeft className="w-4 h-4" />
              {t('cart.continueShopping')}
            </Link>

            <div className="mt-6 pt-6 border-t border-gray-200">
              <label htmlFor="promo-code" className="label">
                {t('cart.promoCode')}
              </label>
              <div className="flex gap-2">
                <input id="promo-code" type="text" placeholder={t('cart.enterCode')} className="input flex-1" />
                <button type="button" className="btn-secondary">
                  {t('common.apply')}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </PageShell>
  );
}
