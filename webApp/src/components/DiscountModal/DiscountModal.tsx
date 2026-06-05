import { useState } from 'react';
import { X, Tag } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import type { Product } from '../../types';
import { productService } from '../../services/productService';
import { formatCurrency } from '../../i18n/display';

const DISCOUNT_PERCENTAGES = [5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100, 109];

interface Props {
    product: Product | null;
    onClose: () => void;
    onSuccess: (updated: Product) => void;
}

export default function DiscountModal({ product, onClose, onSuccess }: Props): JSX.Element | null {
    const { t } = useTranslation();
    const today = new Date().toISOString().split('T')[0];

    const [discountPercentage, setDiscountPercentage] = useState<number>(10);
    const [startDate, setStartDate] = useState<string>(today);
    const [endDate, setEndDate] = useState<string>('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    if (!product) return null;

    const discountedPrice = product.price * (1 - discountPercentage / 100);

    const handleApply = async (): Promise<void> => {
        if (!endDate) { setError(t('discount.endDateRequired', 'End date is required')); return; }
        if (endDate < startDate) { setError(t('discount.endDateInvalid', 'End date must be after start date')); return; }
        setError(null);
        setIsSubmitting(true);
        try {
            const updated = await productService.applyDiscount(product.id, {
                discountPercentage,
                startDate,
                endDate
            });
            if (updated) onSuccess(updated);
            onClose();
        } catch {
            setError(t('discount.applyError', 'Failed to apply discount'));
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm">
            <div className="bg-white rounded-2xl shadow-xl w-full max-w-sm mx-4 p-6">

                {/* Header */}
                <div className="flex items-center justify-between mb-5">
                    <div className="flex items-center gap-2">
                        <Tag className="w-5 h-5 text-primary-600" />
                        <h2 className="text-lg font-semibold text-navy-800">
                            {t('discount.applyDiscount', 'Apply Discount')}
                        </h2>
                    </div>
                    <button type="button" onClick={onClose} className="p-1 rounded-lg hover:bg-gray-100 transition-colors">
                        <X className="w-5 h-5 text-navy-500" />
                    </button>
                </div>

                {/* Product name */}
                <p className="text-sm text-navy-500 mb-4 truncate font-medium">
                    {product.name}
                </p>

                <div className="space-y-4">

                    {/* Discount percentage */}
                    <div>
                        <label className="block text-sm font-medium text-navy-700 mb-1">
                            {t('discount.percentage', 'Discount Percentage')}
                        </label>
                        <select
                            value={discountPercentage}
                            onChange={(e) => setDiscountPercentage(Number(e.target.value))}
                            className="w-full border border-gray-300 bg-white rounded-lg px-3 py-2 text-navy-800 text-sm focus:border-primary-500 focus:ring-1 focus:ring-primary-500 focus:outline-none appearance-none cursor-pointer"
                            style={{ colorScheme: 'light' }}
                        >
                            {DISCOUNT_PERCENTAGES.map((pct) => (
                                <option key={pct} value={pct} className="bg-white text-navy-800 py-1">
                                    {pct}%
                                </option>
                            ))}
                        </select>
                    </div>

                    {/* New price preview */}
                    <div className="bg-primary-50 rounded-lg px-4 py-3 flex items-center justify-between">
                        <span className="text-sm text-navy-600">{t('discount.newPrice', 'New Price')}</span>
                        <div className="flex items-baseline gap-2">
                            <span className="text-lg font-bold text-primary-700">{formatCurrency(discountedPrice)}</span>
                            <span className="text-xs text-navy-400 line-through">{formatCurrency(product.price)}</span>
                        </div>
                    </div>

                    {/* Date range */}
                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="block text-sm font-medium text-navy-700 mb-1">
                                {t('discount.startDate', 'Start Date')}
                            </label>
                            <input
                                type="date"
                                value={startDate}
                                min={today}
                                onChange={(e) => setStartDate(e.target.value)}
                                className="w-full border border-gray-300 bg-white rounded-lg px-3 py-2 text-navy-800 text-sm focus:border-primary-500 focus:ring-1 focus:ring-primary-500 focus:outline-none"
                                style={{ colorScheme: 'light' }}
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-navy-700 mb-1">
                                {t('discount.endDate', 'End Date')}
                            </label>
                            <input
                                type="date"
                                value={endDate}
                                min={startDate || today}
                                onChange={(e) => setEndDate(e.target.value)}
                                className="w-full border border-gray-300 bg-white rounded-lg px-3 py-2 text-navy-800 text-sm focus:border-primary-500 focus:ring-1 focus:ring-primary-500 focus:outline-none"
                                style={{ colorScheme: 'light' }}
                            />
                        </div>
                    </div>

                    {/* Error */}
                    {error && (
                        <p className="text-sm text-red-600 bg-red-50 px-3 py-2 rounded-lg">{error}</p>
                    )}

                    {/* Actions */}
                    <div className="flex gap-3 pt-1">
                        <button type="button" onClick={onClose} className="flex-1 btn-secondary">
                            {t('common.cancel', 'Cancel')}
                        </button>
                        <button
                            type="button"
                            onClick={() => void handleApply()}
                            disabled={isSubmitting}
                            className="flex-1 btn-primary disabled:opacity-60"
                        >
                            {isSubmitting ? t('common.saving', 'Saving…') : t('discount.apply', 'Apply')}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}