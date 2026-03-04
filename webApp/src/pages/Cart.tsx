import { ArrowLeft, ArrowRight, Minus, Plus, ShoppingBag, Trash2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import PageShell from '../components/PageShell';
import { useCart } from '../contexts/CartContext';

export default function Cart(): JSX.Element {
  const { items, itemCount, subtotal, tax, total, updateQuantity, removeFromCart, clearCart } = useCart();

  if (items.length === 0) {
    return (
      <PageShell title="Shopping Cart" description="Your cart is currently empty." className="buyer-cart">
        <div className="buyer-cart__empty text-center py-16">
          <div className="w-24 h-24 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <ShoppingBag className="w-12 h-12 text-navy-400" />
          </div>
          <h2 className="text-2xl font-bold text-navy-800 mb-3">Your cart is empty</h2>
          <p className="text-navy-500 mb-6 max-w-md mx-auto">
            Looks like you haven&apos;t added anything to your cart yet. Browse our marketplace to find products for your
            business.
          </p>
          <Link to="/marketplace" className="btn-primary inline-flex items-center gap-2">
            <ShoppingBag className="w-4 h-4" />
            Browse Marketplace
          </Link>
        </div>
      </PageShell>
    );
  }

  return (
    <PageShell
      title="Shopping Cart"
      description={`${itemCount} items in your cart`}
      className="buyer-cart"
      actions={
        <button
          type="button"
          onClick={() => void clearCart()}
          className="text-red-600 hover:text-red-700 text-sm font-medium flex items-center gap-1"
        >
          <Trash2 className="w-4 h-4" />
          Clear Cart
        </button>
      }
    >

      <div className="buyer-cart__layout grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-4">
          {items.map((item) => (
            <div key={item.product.id} className="buyer-cart__item bg-white rounded-xl shadow-card p-4 flex gap-4">
              <div className="w-24 h-24 rounded-lg overflow-hidden bg-gray-100 flex-shrink-0">
                <img src={item.product.image} alt={item.product.name} className="w-full h-full object-cover" />
              </div>

              <div className="flex-1 min-w-0">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <h3 className="font-semibold text-navy-800">{item.product.name}</h3>
                    <p className="text-sm text-navy-500">{item.product.brand}</p>
                    <p className="text-sm text-navy-400 mt-1">Supplier: {item.product.supplier}</p>
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
                    <p className="text-sm text-navy-500">SAR {item.product.price.toFixed(2)} each</p>
                    <p className="font-bold text-navy-800">SAR {(item.product.price * item.quantity).toFixed(2)}</p>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>

        <div className="lg:col-span-1">
          <div className="buyer-cart__summary bg-white rounded-xl shadow-card p-6 sticky top-24">
            <h3 className="text-lg font-semibold text-navy-800 mb-4">Order Summary</h3>

            <div className="space-y-3 mb-6">
              <div className="flex items-center justify-between text-navy-600">
                <span>Subtotal ({itemCount} items)</span>
                <span>SAR {subtotal.toFixed(2)}</span>
              </div>
              <div className="flex items-center justify-between text-navy-600">
                <span>VAT (15%)</span>
                <span>SAR {tax.toFixed(2)}</span>
              </div>
              <div className="flex items-center justify-between text-navy-600">
                <span>Shipping</span>
                <span className="text-green-600 font-medium">Free</span>
              </div>
              <hr className="border-gray-200" />
              <div className="flex items-center justify-between text-lg font-bold text-navy-800">
                <span>Total</span>
                <span>SAR {total.toFixed(2)}</span>
              </div>
            </div>

            <Link to="/checkout" className="w-full btn-primary flex items-center justify-center gap-2 py-3">
              Proceed to Checkout
              <ArrowRight className="w-4 h-4" />
            </Link>

            <Link to="/marketplace" className="w-full btn-secondary flex items-center justify-center gap-2 py-3 mt-3">
              <ArrowLeft className="w-4 h-4" />
              Continue Shopping
            </Link>

            <div className="mt-6 pt-6 border-t border-gray-200">
              <label htmlFor="promo-code" className="label">
                Promo Code
              </label>
              <div className="flex gap-2">
                <input id="promo-code" type="text" placeholder="Enter code" className="input flex-1" />
                <button type="button" className="btn-secondary">
                  Apply
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </PageShell>
  );
}
