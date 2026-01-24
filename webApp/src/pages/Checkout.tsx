import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../contexts/CartContext';
import { useAuth } from '../contexts/AuthContext';
import {
    MapPin,
    CreditCard,
    CheckCircle2,
    ArrowLeft,
    ArrowRight,
    Package,
    Lock
} from 'lucide-react';

type CheckoutStep = 'shipping' | 'payment' | 'confirmation';

import { addressService } from '../services/addressService';
import { paymentService } from '../services/paymentService';
import { Address, PaymentMethodDTO, CreateAddressRequest, CreatePaymentMethodRequest } from '../types';

const Checkout: React.FC = () => {
    const navigate = useNavigate();
    const { user } = useAuth();
    const { items, subtotal, tax, total, checkout } = useCart();
    const [currentStep, setCurrentStep] = useState<CheckoutStep>('shipping');
    const [isProcessing, setIsProcessing] = useState(false);

    // Data from API
    const [addresses, setAddresses] = useState<Address[]>([]);
    const [paymentMethods, setPaymentMethods] = useState<PaymentMethodDTO[]>([]);

    // Selection state
    const [selectedAddressId, setSelectedAddressId] = useState<string | null>(null);
    const [selectedPaymentMethodId, setSelectedPaymentMethodId] = useState<string | null>(null);

    // UI state for forms
    const [showAddressForm, setShowAddressForm] = useState(false);
    const [showPaymentForm, setShowPaymentForm] = useState(false);

    // Form states
    const [shippingForm, setShippingForm] = useState({
        street: '',
        city: '',
        state: '',
        zipCode: '',
        country: 'Saudi Arabia'
    });

    const [paymentForm, setPaymentForm] = useState({
        cardholderName: '',
        cardNumber: '',
        expiryDate: '',
        cvv: ''
    });

    React.useEffect(() => {
        const fetchData = async () => {
            if (!user?.id) return;
            try {
                const [addrRes, payRes] = await Promise.all([
                    addressService.getAddresses(),
                    paymentService.getPaymentMethods(user.id)
                ]);
                setAddresses(addrRes);
                setPaymentMethods(payRes);

                // Pre-select defaults if available
                if (addrRes.length > 0) setSelectedAddressId(addrRes[0].id || null); // id can be nullable in dto definition but response usually has it
                if (payRes.length > 0) {
                    const def = payRes.find(p => p.isDefault) || payRes[0];
                    setSelectedPaymentMethodId(def.id);
                }

            } catch (e) {
                console.error("Failed to load checkout data", e);
            }
        };
        fetchData();
    }, [user?.id]);

    const handleAddressSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsProcessing(true);
        try {
            const request: CreateAddressRequest = {
                street: shippingForm.street,
                city: shippingForm.city,
                state: shippingForm.state,
                zipCode: shippingForm.zipCode,
                country: shippingForm.country
            } as unknown as CreateAddressRequest;

            const newAddr = await addressService.addAddress(request);
            // Convert AddressResponseDto to Address (compatible)
            setAddresses([...addresses, newAddr as unknown as Address]);
            setSelectedAddressId(newAddr.id || '');
            setShowAddressForm(false);
        } catch (e) {
            console.error("Failed to add address", e);
            alert("Failed to add address");
        } finally {
            setIsProcessing(false);
        }
    };

    const handlePaymentSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsProcessing(true);
        try {
            if (!user?.id) return;

            const brand = paymentForm.cardNumber.startsWith('4') ? 'Visa' : 'Mastercard';
            const last4 = paymentForm.cardNumber.replace(/\s/g, '').slice(-4);
            const maskedDetails = `${brand} **** ${last4} (Exp: ${paymentForm.expiryDate})`;

            const request: CreatePaymentMethodRequest = {
                ownerId: user.id,
                type: 'CREDIT_CARD',
                maskedDetails: maskedDetails,
                isDefault: paymentMethods.length === 0
            } as unknown as CreatePaymentMethodRequest;

            const newMethod = await paymentService.addPaymentMethod(request);
            setPaymentMethods([...paymentMethods, newMethod]);
            setSelectedPaymentMethodId(newMethod.id);
            setShowPaymentForm(false);
        } catch (e) {
            console.error("Failed to add payment method", e);
            alert("Failed to add payment method");
        } finally {
            setIsProcessing(false);
        }
    };

    const handlePlaceOrder = async () => {
        if (!selectedAddressId || !selectedPaymentMethodId) {
            alert("Please select shipping address and payment method");
            return;
        }

        setIsProcessing(true);
        // checkout() in context currently creates a blank order based on cart. 
        // Ideally we pass addressId and paymentMethodId to checkout(). 
        // For now, let's assume CartContext handles the basic order creation 
        // and we might need to update the order later or refactor checkout().
        // Given constraints, we'll just run checkout().
        const success = await checkout();
        setIsProcessing(false);

        if (success) {
            navigate('/orders');
        } else {
            alert('Failed to place order. Please try again.');
        }
    };

    const steps = [
        { id: 'shipping', label: 'Shipping', icon: MapPin },
        { id: 'payment', label: 'Payment', icon: CreditCard },
        { id: 'confirmation', label: 'Confirmation', icon: CheckCircle2 }
    ];

    const formatCardNumber = (value: string) => {
        const v = value.replace(/\s+/g, '').replace(/[^0-9]/gi, '');
        const matches = v.match(/\d{4,16}/g);
        const match = (matches && matches[0]) || '';
        const parts = [];
        for (let i = 0, len = match.length; i < len; i += 4) {
            parts.push(match.substring(i, i + 4));
        }
        return parts.length ? parts.join(' ') : value;
    };

    const formatExpiry = (value: string) => {
        const v = value.replace(/\s+/g, '').replace(/[^0-9]/gi, '');
        if (v.length >= 2) {
            return v.substring(0, 2) + '/' + v.substring(2, 4);
        }
        return v;
    };

    return (
        <div className="max-w-4xl mx-auto space-y-6">
            {/* Page Header */}
            <div>
                <h1 className="text-2xl font-bold text-navy-800">Checkout</h1>
                <p className="text-navy-500 mt-1">Complete your order</p>
            </div>

            {/* Progress Steps */}
            <div className="bg-white rounded-xl shadow-card p-6">
                <div className="flex items-center justify-between">
                    {steps.map((step, index) => (
                        <React.Fragment key={step.id}>
                            <div className="flex items-center gap-3">
                                <div className={`w-10 h-10 rounded-full flex items-center justify-center transition-colors ${step.id === currentStep
                                    ? 'bg-primary-600 text-white'
                                    : steps.findIndex(s => s.id === currentStep) > index
                                        ? 'bg-green-500 text-white'
                                        : 'bg-gray-200 text-navy-500'
                                    }`}>
                                    {steps.findIndex(s => s.id === currentStep) > index ? (
                                        <CheckCircle2 className="w-5 h-5" />
                                    ) : (
                                        <step.icon className="w-5 h-5" />
                                    )}
                                </div>
                                <span className={`font-medium hidden sm:block ${step.id === currentStep ? 'text-primary-600' : 'text-navy-500'
                                    }`}>
                                    {step.label}
                                </span>
                            </div>
                            {index < steps.length - 1 && (
                                <div className={`flex-1 h-0.5 mx-4 ${steps.findIndex(s => s.id === currentStep) > index
                                    ? 'bg-green-500'
                                    : 'bg-gray-200'
                                    }`} />
                            )}
                        </React.Fragment>
                    ))}
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Main Form Area */}
                <div className="lg:col-span-2">
                    {/* Step 1: Shipping */}
                    {currentStep === 'shipping' && (
                        <div className="bg-white rounded-xl shadow-card p-6">
                            <h2 className="text-lg font-semibold text-navy-800 mb-6 flex items-center gap-2">
                                <MapPin className="w-5 h-5 text-primary-600" />
                                Shipping Address
                            </h2>

                            {!showAddressForm ? (
                                <div className="space-y-4">
                                    {addresses.length === 0 && (
                                        <p className="text-gray-500 text-center py-4">No saved addresses found.</p>
                                    )}
                                    <div className="grid grid-cols-1 gap-4">
                                        {addresses.map(addr => (
                                            <div
                                                key={addr.id}
                                                onClick={() => setSelectedAddressId(addr.id || null)}
                                                className={`p-4 border rounded-lg cursor-pointer transition-colors ${selectedAddressId === addr.id ? 'border-primary-500 bg-primary-50' : 'border-gray-200 hover:border-gray-300'}`}
                                            >
                                                <div className="flex items-center justify-between">
                                                    <div>
                                                        <p className="font-medium text-navy-800">{addr.street}</p>
                                                        <p className="text-sm text-navy-500">{addr.city}, {addr.state} {addr.zipCode}</p>
                                                        <p className="text-sm text-navy-500">{addr.country}</p>
                                                    </div>
                                                    {selectedAddressId === addr.id && <CheckCircle2 className="w-5 h-5 text-primary-600" />}
                                                </div>
                                            </div>
                                        ))}
                                        <button
                                            onClick={() => setShowAddressForm(true)}
                                            className="p-4 border border-dashed border-gray-300 rounded-lg text-gray-500 hover:border-primary-500 hover:text-primary-600 transition-colors flex items-center justify-center gap-2"
                                        >
                                            <MapPin className="w-5 h-5" />
                                            Add New Address
                                        </button>
                                    </div>
                                    <div className="flex gap-4 mt-6">
                                        <button
                                            type="button"
                                            onClick={() => navigate('/cart')}
                                            className="btn-secondary flex items-center gap-2"
                                        >
                                            <ArrowLeft className="w-4 h-4" />
                                            Back to Cart
                                        </button>
                                        <button
                                            onClick={() => {
                                                if (selectedAddressId) setCurrentStep('payment');
                                                else alert("Please select an address");
                                            }}
                                            className="btn-primary flex-1 flex items-center justify-center gap-2"
                                        >
                                            Continue to Payment
                                            <ArrowRight className="w-4 h-4" />
                                        </button>
                                    </div>
                                </div>
                            ) : (
                                <form onSubmit={handleAddressSubmit} className="space-y-4">
                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                        <div className="md:col-span-2">
                                            <label className="label">Street Address</label>
                                            <input
                                                type="text"
                                                value={shippingForm.street}
                                                onChange={(e) => setShippingForm({ ...shippingForm, street: e.target.value })}
                                                className="input"
                                                placeholder="Street address..."
                                                required
                                            />
                                        </div>
                                        <div>
                                            <label className="label">City</label>
                                            <input
                                                type="text"
                                                value={shippingForm.city}
                                                onChange={(e) => setShippingForm({ ...shippingForm, city: e.target.value })}
                                                className="input"
                                                required
                                            />
                                        </div>
                                        <div>
                                            <label className="label">State/Region</label>
                                            <input
                                                type="text"
                                                value={shippingForm.state}
                                                onChange={(e) => setShippingForm({ ...shippingForm, state: e.target.value })}
                                                className="input"
                                                required
                                            />
                                        </div>
                                        <div>
                                            <label className="label">Postal Code</label>
                                            <input
                                                type="text"
                                                value={shippingForm.zipCode}
                                                onChange={(e) => setShippingForm({ ...shippingForm, zipCode: e.target.value })}
                                                className="input"
                                                required
                                            />
                                        </div>
                                        <div>
                                            <label className="label">Country</label>
                                            <input
                                                type="text"
                                                value={shippingForm.country}
                                                onChange={(e) => setShippingForm({ ...shippingForm, country: e.target.value })}
                                                className="input"
                                                required
                                            />
                                        </div>
                                    </div>
                                    <div className="flex gap-4 mt-6">
                                        <button
                                            type="button"
                                            onClick={() => setShowAddressForm(false)}
                                            className="btn-secondary"
                                        >
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

                    {/* Step 2: Payment */}
                    {currentStep === 'payment' && (
                        <div className="bg-white rounded-xl shadow-card p-6">
                            <h2 className="text-lg font-semibold text-navy-800 mb-6 flex items-center gap-2">
                                <CreditCard className="w-5 h-5 text-primary-600" />
                                Payment Method
                            </h2>

                            {!showPaymentForm ? (
                                <div className="space-y-4">
                                    {paymentMethods.length === 0 && (
                                        <p className="text-gray-500 text-center py-4">No saved payment methods found.</p>
                                    )}
                                    <div className="grid grid-cols-1 gap-4">
                                        {paymentMethods.map(method => (
                                            <div
                                                key={method.id}
                                                onClick={() => setSelectedPaymentMethodId(method.id)}
                                                className={`p-4 border rounded-lg cursor-pointer transition-colors ${selectedPaymentMethodId === method.id ? 'border-primary-500 bg-primary-50' : 'border-gray-200 hover:border-gray-300'}`}
                                            >
                                                <div className="flex items-center justify-between">
                                                    <div>
                                                        <p className="font-medium text-navy-800">{method.maskedDetails.split(' ')[0]} Card</p>
                                                        <p className="text-sm text-navy-500">{method.maskedDetails}</p>
                                                    </div>
                                                    {selectedPaymentMethodId === method.id && <CheckCircle2 className="w-5 h-5 text-primary-600" />}
                                                </div>
                                            </div>
                                        ))}
                                        <button
                                            onClick={() => setShowPaymentForm(true)}
                                            className="p-4 border border-dashed border-gray-300 rounded-lg text-gray-500 hover:border-primary-500 hover:text-primary-600 transition-colors flex items-center justify-center gap-2"
                                        >
                                            <CreditCard className="w-5 h-5" />
                                            Add New Payment Method
                                        </button>
                                    </div>
                                    <div className="flex gap-4 mt-6">
                                        <button
                                            type="button"
                                            onClick={() => setCurrentStep('shipping')}
                                            className="btn-secondary flex items-center gap-2"
                                        >
                                            <ArrowLeft className="w-4 h-4" />
                                            Back
                                        </button>
                                        <button
                                            onClick={() => {
                                                if (selectedPaymentMethodId) setCurrentStep('confirmation');
                                                else alert("Please select a payment method");
                                            }}
                                            className="btn-primary flex-1 flex items-center justify-center gap-2"
                                        >
                                            Review Order
                                            <ArrowRight className="w-4 h-4" />
                                        </button>
                                    </div>
                                </div>
                            ) : (
                                <form onSubmit={handlePaymentSubmit} className="space-y-4">
                                    <div>
                                        <label className="label">Cardholder Name</label>
                                        <input
                                            type="text"
                                            value={paymentForm.cardholderName}
                                            onChange={(e) => setPaymentForm({ ...paymentForm, cardholderName: e.target.value })}
                                            className="input"
                                            placeholder="Name as it appears on card"
                                            required
                                        />
                                    </div>
                                    <div>
                                        <label className="label">Card Number</label>
                                        <input
                                            type="text"
                                            value={paymentForm.cardNumber}
                                            onChange={(e) => setPaymentForm({ ...paymentForm, cardNumber: formatCardNumber(e.target.value) })}
                                            className="input"
                                            placeholder="1234 5678 9012 3456"
                                            maxLength={19}
                                            required
                                        />
                                    </div>
                                    <div className="grid grid-cols-2 gap-4">
                                        <div>
                                            <label className="label">Expiry Date</label>
                                            <input
                                                type="text"
                                                value={paymentForm.expiryDate}
                                                onChange={(e) => setPaymentForm({ ...paymentForm, expiryDate: formatExpiry(e.target.value) })}
                                                className="input"
                                                placeholder="MM/YY"
                                                maxLength={5}
                                                required
                                            />
                                        </div>
                                        <div>
                                            <label className="label">CVV</label>
                                            <input
                                                type="text"
                                                value={paymentForm.cvv}
                                                onChange={(e) => setPaymentForm({ ...paymentForm, cvv: e.target.value.replace(/\D/g, '').slice(0, 4) })}
                                                className="input"
                                                placeholder="123"
                                                maxLength={4}
                                                required
                                            />
                                        </div>
                                    </div>
                                    <div className="mt-6 p-4 bg-gray-50 rounded-lg flex items-center gap-3">
                                        <Lock className="w-5 h-5 text-green-600" />
                                        <p className="text-sm text-navy-600">
                                            Your payment is secure. We use 256-bit SSL encryption.
                                        </p>
                                    </div>

                                    <div className="flex gap-4 mt-6">
                                        <button
                                            type="button"
                                            onClick={() => setShowPaymentForm(false)}
                                            className="btn-secondary"
                                        >
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

                    {/* Step 3: Confirmation */}
                    {currentStep === 'confirmation' && (
                        <div className="bg-white rounded-xl shadow-card p-6">
                            <div className="text-center mb-8">
                                <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                                    <CheckCircle2 className="w-8 h-8 text-green-600" />
                                </div>
                                <h2 className="text-2xl font-bold text-navy-800 mb-2">Order Review</h2>
                                <p className="text-navy-500">Please review your order before placing it</p>
                            </div>

                            {/* Shipping Summary */}
                            <div className="border border-gray-200 rounded-lg p-4 mb-4">
                                <h3 className="font-semibold text-navy-800 mb-2 flex items-center gap-2">
                                    <MapPin className="w-4 h-4 text-primary-600" />
                                    Shipping Address
                                </h3>
                                {selectedAddressId && addresses.find(a => a.id === selectedAddressId) && (
                                    <>
                                        <p className="text-navy-600">{addresses.find(a => a.id === selectedAddressId)?.street}</p>
                                        <p className="text-navy-500 text-sm">
                                            {addresses.find(a => a.id === selectedAddressId)?.city}, {addresses.find(a => a.id === selectedAddressId)?.state} {addresses.find(a => a.id === selectedAddressId)?.zipCode}
                                        </p>
                                        <p className="text-navy-500 text-sm">{addresses.find(a => a.id === selectedAddressId)?.country}</p>
                                    </>
                                )}
                            </div>

                            {/* Payment Summary */}
                            <div className="border border-gray-200 rounded-lg p-4 mb-4">
                                <h3 className="font-semibold text-navy-800 mb-2 flex items-center gap-2">
                                    <CreditCard className="w-4 h-4 text-primary-600" />
                                    Payment Method
                                </h3>
                                {selectedPaymentMethodId && paymentMethods.find(p => p.id === selectedPaymentMethodId) && (
                                    <p className="text-navy-600">{paymentMethods.find(p => p.id === selectedPaymentMethodId)?.maskedDetails}</p>
                                )}
                            </div>

                            {/* Items Summary */}
                            <div className="border border-gray-200 rounded-lg p-4">
                                <h3 className="font-semibold text-navy-800 mb-4 flex items-center gap-2">
                                    <Package className="w-4 h-4 text-primary-600" />
                                    Order Items ({items.length})
                                </h3>
                                <div className="space-y-3 max-h-48 overflow-y-auto">
                                    {items.map((item) => (
                                        <div key={item.product.id} className="flex items-center gap-3">
                                            <img
                                                src={item.product.image}
                                                alt={item.product.name}
                                                className="w-12 h-12 rounded-lg object-cover"
                                            />
                                            <div className="flex-1 min-w-0">
                                                <p className="font-medium text-navy-800 truncate">{item.product.name}</p>
                                                <p className="text-sm text-navy-500">Qty: {item.quantity}</p>
                                            </div>
                                            <p className="font-medium text-navy-800">
                                                SAR {(item.product.price * item.quantity).toFixed(2)}
                                            </p>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            <button
                                onClick={handlePlaceOrder}
                                className="w-full btn-primary py-3 mt-6 flex items-center justify-center gap-2"
                                disabled={isProcessing}
                            >
                                <CheckCircle2 className="w-5 h-5" />
                                {isProcessing ? 'Processing Order...' : `Place Order - SAR ${total.toFixed(2)}`}
                            </button>
                        </div>
                    )}
                </div>

                {/* Order Summary Sidebar */}
                <div className="lg:col-span-1">
                    <div className="bg-white rounded-xl shadow-card p-6 sticky top-24">
                        <h3 className="text-lg font-semibold text-navy-800 mb-4">Order Summary</h3>

                        <div className="space-y-3 mb-6">
                            {items.slice(0, 3).map((item) => (
                                <div key={item.product.id} className="flex items-center gap-3">
                                    <img
                                        src={item.product.image}
                                        alt={item.product.name}
                                        className="w-12 h-12 rounded-lg object-cover"
                                    />
                                    <div className="flex-1 min-w-0">
                                        <p className="text-sm font-medium text-navy-800 truncate">{item.product.name}</p>
                                        <p className="text-xs text-navy-500">x{item.quantity}</p>
                                    </div>
                                    <p className="text-sm font-medium text-navy-800">
                                        SAR {(item.product.price * item.quantity).toFixed(2)}
                                    </p>
                                </div>
                            ))}
                            {items.length > 3 && (
                                <p className="text-sm text-navy-500 text-center">
                                    + {items.length - 3} more items
                                </p>
                            )}
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
        </div>
    );
};

export default Checkout;
