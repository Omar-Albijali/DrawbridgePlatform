import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, ArrowRight, CheckCircle2, CreditCard, Lock, MapPin, Package } from 'lucide-react';
import PageShell from '../components/PageShell';
import { useAuth } from '../contexts/AuthContext';
import { useCart } from '../contexts/CartContext';
import { addressService } from '../services/addressService';
import { paymentService } from '../services/paymentService';
import type { Address, CreateAddressRequest, CreatePaymentMethodRequest, PaymentMethodDTO } from '../types';

type CheckoutStep = 'shipping' | 'payment' | 'confirmation';

export default function Checkout(): JSX.Element {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { items, subtotal, tax, total, checkout } = useCart();
  const [currentStep, setCurrentStep] = useState<CheckoutStep>('shipping');
  const [isProcessing, setIsProcessing] = useState(false);

  const [addresses, setAddresses] = useState<Address[]>([]);
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethodDTO[]>([]);

  const [selectedAddressId, setSelectedAddressId] = useState<string | null>(null);
  const [selectedPaymentMethodId, setSelectedPaymentMethodId] = useState<string | null>(null);

  const [showAddressForm, setShowAddressForm] = useState(false);
  const [showPaymentForm, setShowPaymentForm] = useState(false);

  const [shippingForm, setShippingForm] = useState({
    street: '',
    city: '',
    state: '',
    zipCode: '',
    country: 'Saudi Arabia',
  });

  const [paymentForm, setPaymentForm] = useState({
    cardholderName: '',
    cardNumber: '',
    expiryDate: '',
    cvv: '',
  });

  useEffect(() => {
    const fetchData = async (): Promise<void> => {
      if (!user?.id) {
        return;
      }

      try {
        const [addrRes, payRes] = await Promise.all([
          addressService.getAddresses(),
          paymentService.getPaymentMethods(user.id),
        ]);
        setAddresses(addrRes);
        setPaymentMethods(payRes);

        if (addrRes.length > 0) {
          setSelectedAddressId(addrRes[0].id ?? null);
        }

        if (payRes.length > 0) {
          const defaultMethod = payRes.find((method) => method.isDefault) ?? payRes[0];
          setSelectedPaymentMethodId(defaultMethod.id);
        }
      } catch (error) {
        console.error('Failed to load checkout data', error);
      }
    };

    void fetchData();
  }, [user?.id]);

  const handleAddressSubmit = async (event: FormEvent): Promise<void> => {
    event.preventDefault();
    setIsProcessing(true);

    try {
      const request: CreateAddressRequest = {
        street: shippingForm.street,
        city: shippingForm.city,
        state: shippingForm.state,
        zipCode: shippingForm.zipCode,
        country: shippingForm.country,
      } as unknown as CreateAddressRequest;

      const newAddress = await addressService.addAddress(request);
      setAddresses([...addresses, newAddress as unknown as Address]);
      setSelectedAddressId(newAddress.id ?? null);
      setShowAddressForm(false);
    } catch (error) {
      console.error('Failed to add address', error);
      alert('Failed to add address');
    } finally {
      setIsProcessing(false);
    }
  };

  const handlePaymentSubmit = async (event: FormEvent): Promise<void> => {
    event.preventDefault();
    setIsProcessing(true);

    try {
      if (!user?.id) {
        return;
      }

      const brand = paymentForm.cardNumber.startsWith('4') ? 'Visa' : 'Mastercard';
      const last4 = paymentForm.cardNumber.replace(/\s/g, '').slice(-4);
      const maskedDetails = `${brand} **** ${last4} (Exp: ${paymentForm.expiryDate})`;

      const request: CreatePaymentMethodRequest = {
        ownerId: user.id,
        type: 'CREDIT_CARD',
        maskedDetails,
        isDefault: paymentMethods.length === 0,
      } as unknown as CreatePaymentMethodRequest;

      const newMethod = await paymentService.addPaymentMethod(request);
      setPaymentMethods([...paymentMethods, newMethod]);
      setSelectedPaymentMethodId(newMethod.id);
      setShowPaymentForm(false);
    } catch (error) {
      console.error('Failed to add payment method', error);
      alert('Failed to add payment method');
    } finally {
      setIsProcessing(false);
    }
  };

  const handlePlaceOrder = async (): Promise<void> => {
    if (!selectedAddressId || !selectedPaymentMethodId) {
      alert('Please select shipping address and payment method');
      return;
    }

    setIsProcessing(true);
    const success = await checkout();
    setIsProcessing(false);

    if (success) {
      navigate('/orders');
      return;
    }

    alert('Failed to place order. Please try again.');
  };

  const formatCardNumber = (value: string): string => {
    const numeric = value.replace(/\s+/g, '').replace(/[^0-9]/gi, '');
    const matches = numeric.match(/\d{4,16}/g);
    const match = (matches && matches[0]) || '';
    const parts: string[] = [];
    for (let i = 0; i < match.length; i += 4) {
      parts.push(match.substring(i, i + 4));
    }
    return parts.length > 0 ? parts.join(' ') : value;
  };

  const formatExpiry = (value: string): string => {
    const numeric = value.replace(/\s+/g, '').replace(/[^0-9]/gi, '');
    if (numeric.length >= 2) {
      return `${numeric.substring(0, 2)}/${numeric.substring(2, 4)}`;
    }
    return numeric;
  };

  const currentStepIndex = ['shipping', 'payment', 'confirmation'].indexOf(currentStep);
  const selectedAddress = addresses.find((address) => address.id === selectedAddressId);
  const selectedPaymentMethod = paymentMethods.find((method) => method.id === selectedPaymentMethodId);

  return (
    <PageShell title="Checkout" description="Complete your order" className="page-shell--narrow buyer-checkout">

      <div className="buyer-checkout__stepper bg-white rounded-xl shadow-card p-6">
        <div className="flex items-center justify-between">
          {[
            { id: 'shipping', label: 'Shipping', icon: MapPin },
            { id: 'payment', label: 'Payment', icon: CreditCard },
            { id: 'confirmation', label: 'Confirmation', icon: CheckCircle2 },
          ].map((step, index) => (
            <div key={step.id} className="contents">
              <div className="flex items-center gap-3">
                <div
                  className={`w-10 h-10 rounded-full flex items-center justify-center transition-colors ${
                    step.id === currentStep
                      ? 'bg-primary-600 text-white'
                      : currentStepIndex > index
                        ? 'bg-green-500 text-white'
                        : 'bg-gray-200 text-navy-500'
                  }`}
                >
                  {currentStepIndex > index ? <CheckCircle2 className="w-5 h-5" /> : <step.icon className="w-5 h-5" />}
                </div>
                <span
                  className={`buyer-checkout__step-label font-medium hidden sm:block ${step.id === currentStep ? 'text-primary-600' : 'text-navy-500'}`}
                >
                  {step.label}
                </span>
              </div>
              {index < 2 && (
                <div className={`flex-1 h-0.5 mx-4 ${currentStepIndex > index ? 'bg-green-500' : 'bg-gray-200'}`} />
              )}
            </div>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          {currentStep === 'shipping' && (
            <div className="buyer-checkout__panel bg-white rounded-xl shadow-card p-6">
              <h2 className="text-lg font-semibold text-navy-800 mb-6 flex items-center gap-2">
                <MapPin className="w-5 h-5 text-primary-600" />
                Shipping Address
              </h2>

              {!showAddressForm ? (
                <div className="space-y-4">
                  {addresses.length === 0 && <p className="text-gray-500 text-center py-4">No saved addresses found.</p>}
                  <div className="buyer-checkout__option-grid grid grid-cols-1 gap-4">
                    {addresses.map((address) => (
                      <button
                        key={address.id}
                        type="button"
                        onClick={() => setSelectedAddressId(address.id ?? null)}
                        className={`buyer-checkout__option p-4 border rounded-lg text-left transition-colors ${
                          selectedAddressId === address.id ? 'border-primary-500 bg-primary-50' : 'border-gray-200 hover:border-gray-300'
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="font-medium text-navy-800">{address.street}</p>
                            <p className="text-sm text-navy-500">
                              {address.city}, {address.state} {address.zipCode}
                            </p>
                            <p className="text-sm text-navy-500">{address.country}</p>
                          </div>
                          {selectedAddressId === address.id && <CheckCircle2 className="w-5 h-5 text-primary-600" />}
                        </div>
                      </button>
                    ))}
                    <button
                      type="button"
                      onClick={() => setShowAddressForm(true)}
                      className="p-4 border border-dashed border-gray-300 rounded-lg text-gray-500 hover:border-primary-500 hover:text-primary-600 transition-colors flex items-center justify-center gap-2"
                    >
                      <MapPin className="w-5 h-5" />
                      Add New Address
                    </button>
                  </div>
                  <div className="flex gap-4 mt-6">
                    <button type="button" onClick={() => navigate('/cart')} className="btn-secondary flex items-center gap-2">
                      <ArrowLeft className="w-4 h-4" />
                      Back to Cart
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        if (selectedAddressId) {
                          setCurrentStep('payment');
                        } else {
                          alert('Please select an address');
                        }
                      }}
                      className="btn-primary flex-1 flex items-center justify-center gap-2"
                    >
                      Continue to Payment
                      <ArrowRight className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ) : (
                <form onSubmit={(event) => void handleAddressSubmit(event)} className="space-y-4">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="md:col-span-2">
                      <label htmlFor="street" className="label">
                        Street Address
                      </label>
                      <input
                        id="street"
                        type="text"
                        value={shippingForm.street}
                        onChange={(event) => setShippingForm({ ...shippingForm, street: event.target.value })}
                        className="input"
                        placeholder="Street address..."
                        required
                      />
                    </div>
                    <div>
                      <label htmlFor="city" className="label">
                        City
                      </label>
                      <input
                        id="city"
                        type="text"
                        value={shippingForm.city}
                        onChange={(event) => setShippingForm({ ...shippingForm, city: event.target.value })}
                        className="input"
                        required
                      />
                    </div>
                    <div>
                      <label htmlFor="state" className="label">
                        State/Region
                      </label>
                      <input
                        id="state"
                        type="text"
                        value={shippingForm.state}
                        onChange={(event) => setShippingForm({ ...shippingForm, state: event.target.value })}
                        className="input"
                        required
                      />
                    </div>
                    <div>
                      <label htmlFor="zipCode" className="label">
                        Postal Code
                      </label>
                      <input
                        id="zipCode"
                        type="text"
                        value={shippingForm.zipCode}
                        onChange={(event) => setShippingForm({ ...shippingForm, zipCode: event.target.value })}
                        className="input"
                        required
                      />
                    </div>
                    <div>
                      <label htmlFor="country" className="label">
                        Country
                      </label>
                      <input
                        id="country"
                        type="text"
                        value={shippingForm.country}
                        onChange={(event) => setShippingForm({ ...shippingForm, country: event.target.value })}
                        className="input"
                        required
                      />
                    </div>
                  </div>
                  <div className="flex gap-4 mt-6">
                    <button type="button" onClick={() => setShowAddressForm(false)} className="btn-secondary">
                      Cancel
                    </button>
                    <button type="submit" className="btn-primary" disabled={isProcessing}>
                      {isProcessing ? 'Saving...' : 'Save Address'}
                    </button>
                  </div>
                </form>
              )}
            </div>
          )}

          {currentStep === 'payment' && (
            <div className="buyer-checkout__panel bg-white rounded-xl shadow-card p-6">
              <h2 className="text-lg font-semibold text-navy-800 mb-6 flex items-center gap-2">
                <CreditCard className="w-5 h-5 text-primary-600" />
                Payment Method
              </h2>

              {!showPaymentForm ? (
                <div className="space-y-4">
                  {paymentMethods.length === 0 && (
                    <p className="text-gray-500 text-center py-4">No saved payment methods found.</p>
                  )}
                  <div className="buyer-checkout__option-grid grid grid-cols-1 gap-4">
                    {paymentMethods.map((method) => (
                      <button
                        key={method.id}
                        type="button"
                        onClick={() => setSelectedPaymentMethodId(method.id)}
                        className={`buyer-checkout__option p-4 border rounded-lg text-left transition-colors ${
                          selectedPaymentMethodId === method.id ? 'border-primary-500 bg-primary-50' : 'border-gray-200 hover:border-gray-300'
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="font-medium text-navy-800">{method.maskedDetails.split(' ')[0]} Card</p>
                            <p className="text-sm text-navy-500">{method.maskedDetails}</p>
                          </div>
                          {selectedPaymentMethodId === method.id && <CheckCircle2 className="w-5 h-5 text-primary-600" />}
                        </div>
                      </button>
                    ))}
                    <button
                      type="button"
                      onClick={() => setShowPaymentForm(true)}
                      className="p-4 border border-dashed border-gray-300 rounded-lg text-gray-500 hover:border-primary-500 hover:text-primary-600 transition-colors flex items-center justify-center gap-2"
                    >
                      <CreditCard className="w-5 h-5" />
                      Add New Payment Method
                    </button>
                  </div>
                  <div className="flex gap-4 mt-6">
                    <button type="button" onClick={() => setCurrentStep('shipping')} className="btn-secondary flex items-center gap-2">
                      <ArrowLeft className="w-4 h-4" />
                      Back
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        if (selectedPaymentMethodId) {
                          setCurrentStep('confirmation');
                        } else {
                          alert('Please select a payment method');
                        }
                      }}
                      className="btn-primary flex-1 flex items-center justify-center gap-2"
                    >
                      Review Order
                      <ArrowRight className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ) : (
                <form onSubmit={(event) => void handlePaymentSubmit(event)} className="space-y-4">
                  <div>
                    <label htmlFor="cardholderName" className="label">
                      Cardholder Name
                    </label>
                    <input
                      id="cardholderName"
                      type="text"
                      value={paymentForm.cardholderName}
                      onChange={(event) => setPaymentForm({ ...paymentForm, cardholderName: event.target.value })}
                      className="input"
                      placeholder="Name as it appears on card"
                      required
                    />
                  </div>
                  <div>
                    <label htmlFor="cardNumber" className="label">
                      Card Number
                    </label>
                    <input
                      id="cardNumber"
                      type="text"
                      value={paymentForm.cardNumber}
                      onChange={(event) => setPaymentForm({ ...paymentForm, cardNumber: formatCardNumber(event.target.value) })}
                      className="input"
                      placeholder="1234 5678 9012 3456"
                      maxLength={19}
                      required
                    />
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label htmlFor="expiryDate" className="label">
                        Expiry Date
                      </label>
                      <input
                        id="expiryDate"
                        type="text"
                        value={paymentForm.expiryDate}
                        onChange={(event) => setPaymentForm({ ...paymentForm, expiryDate: formatExpiry(event.target.value) })}
                        className="input"
                        placeholder="MM/YY"
                        maxLength={5}
                        required
                      />
                    </div>
                    <div>
                      <label htmlFor="cvv" className="label">
                        CVV
                      </label>
                      <input
                        id="cvv"
                        type="text"
                        value={paymentForm.cvv}
                        onChange={(event) =>
                          setPaymentForm({
                            ...paymentForm,
                            cvv: event.target.value.replace(/\D/g, '').slice(0, 4),
                          })
                        }
                        className="input"
                        placeholder="123"
                        maxLength={4}
                        required
                      />
                    </div>
                  </div>
                  <div className="buyer-checkout__secure-note mt-6 p-4 bg-gray-50 rounded-lg flex items-center gap-3">
                    <Lock className="w-5 h-5 text-green-600" />
                    <p className="text-sm text-navy-600">Your payment is secure. We use 256-bit SSL encryption.</p>
                  </div>

                  <div className="flex gap-4 mt-6">
                    <button type="button" onClick={() => setShowPaymentForm(false)} className="btn-secondary">
                      Cancel
                    </button>
                    <button type="submit" className="btn-primary" disabled={isProcessing}>
                      {isProcessing ? 'Saving...' : 'Save Card'}
                    </button>
                  </div>
                </form>
              )}
            </div>
          )}

          {currentStep === 'confirmation' && (
            <div className="buyer-checkout__panel bg-white rounded-xl shadow-card p-6">
              <div className="text-center mb-8">
                <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                  <CheckCircle2 className="w-8 h-8 text-green-600" />
                </div>
                <h2 className="text-2xl font-bold text-navy-800 mb-2">Order Review</h2>
                <p className="text-navy-500">Please review your order before placing it</p>
              </div>

              <div className="buyer-checkout__review-card border border-gray-200 rounded-lg p-4 mb-4">
                <h3 className="font-semibold text-navy-800 mb-2 flex items-center gap-2">
                  <MapPin className="w-4 h-4 text-primary-600" />
                  Shipping Address
                </h3>
                {selectedAddress && (
                  <>
                    <p className="text-navy-600">{selectedAddress.street}</p>
                    <p className="text-navy-500 text-sm">
                      {selectedAddress.city}, {selectedAddress.state} {selectedAddress.zipCode}
                    </p>
                    <p className="text-navy-500 text-sm">{selectedAddress.country}</p>
                  </>
                )}
              </div>

              <div className="buyer-checkout__review-card border border-gray-200 rounded-lg p-4 mb-4">
                <h3 className="font-semibold text-navy-800 mb-2 flex items-center gap-2">
                  <CreditCard className="w-4 h-4 text-primary-600" />
                  Payment Method
                </h3>
                {selectedPaymentMethod && <p className="text-navy-600">{selectedPaymentMethod.maskedDetails}</p>}
              </div>

              <div className="buyer-checkout__review-card border border-gray-200 rounded-lg p-4">
                <h3 className="font-semibold text-navy-800 mb-4 flex items-center gap-2">
                  <Package className="w-4 h-4 text-primary-600" />
                  Order Items ({items.length})
                </h3>
                <div className="space-y-3 max-h-48 overflow-y-auto">
                  {items.map((item) => (
                    <div key={item.product.id} className="flex items-center gap-3">
                      <img src={item.product.image} alt={item.product.name} className="w-12 h-12 rounded-lg object-cover" />
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-navy-800 truncate">{item.product.name}</p>
                        <p className="text-sm text-navy-500">Qty: {item.quantity}</p>
                      </div>
                      <p className="font-medium text-navy-800">SAR {(item.product.price * item.quantity).toFixed(2)}</p>
                    </div>
                  ))}
                </div>
              </div>

              <button
                type="button"
                onClick={() => void handlePlaceOrder()}
                className="w-full btn-primary py-3 mt-6 flex items-center justify-center gap-2"
                disabled={isProcessing}
              >
                <CheckCircle2 className="w-5 h-5" />
                {isProcessing ? 'Processing Order...' : `Place Order - SAR ${total.toFixed(2)}`}
              </button>
            </div>
          )}
        </div>

        <div className="lg:col-span-1">
          <div className="buyer-checkout__summary bg-white rounded-xl shadow-card p-6 sticky top-24">
            <h3 className="text-lg font-semibold text-navy-800 mb-4">Order Summary</h3>

            <div className="space-y-3 mb-6">
              {items.slice(0, 3).map((item) => (
                <div key={item.product.id} className="flex items-center gap-3">
                  <img src={item.product.image} alt={item.product.name} className="w-12 h-12 rounded-lg object-cover" />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-navy-800 truncate">{item.product.name}</p>
                    <p className="text-xs text-navy-500">x{item.quantity}</p>
                  </div>
                  <p className="text-sm font-medium text-navy-800">SAR {(item.product.price * item.quantity).toFixed(2)}</p>
                </div>
              ))}
              {items.length > 3 && <p className="text-sm text-navy-500 text-center">+ {items.length - 3} more items</p>}
            </div>

            <hr className="border-gray-200 my-4" />

            <div className="space-y-2 text-sm">
              <div className="flex items-center justify-between text-navy-600">
                <span>Subtotal</span>
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
            </div>

            <hr className="border-gray-200 my-4" />

            <div className="flex items-center justify-between text-lg font-bold text-navy-800">
              <span>Total</span>
              <span>SAR {total.toFixed(2)}</span>
            </div>
          </div>
        </div>
      </div>
    </PageShell>
  );
}
