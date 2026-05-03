import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, ShoppingCart, Star, Package } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import PageShell from '../components/PageShell';
import { useAuth } from '../contexts/AuthContext';
import { useCart } from '../contexts/CartContext';
import { productService } from '../services/productService';
import { formatCurrency } from '../i18n/display';
import { UserRole, type Product } from '../types';

export default function ProductDetail(): JSX.Element {
    const { t } = useTranslation();
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const { isAuthenticated, user } = useAuth();
    const { addToCart } = useCart();
    const isWholesaler = user?.role === UserRole.WHOLESALER;

    const [product, setProduct] = useState<Product | null>(null);
    const [loading, setLoading] = useState(true);
    const [selectedImage, setSelectedImage] = useState<string>('');
    const [quantity, setQuantity] = useState(1);
    const [added, setAdded] = useState(false);

    useEffect(() => {
        if (!id) return;
        const load = async (): Promise<void> => {
            try {
                const data = await productService.getById(id);
                setProduct(data);
                setSelectedImage(data.images?.[0] || data.image);
            } catch {
                navigate('/marketplace', { replace: true });
            } finally {
                setLoading(false);
            }
        };
        void load();
    }, [id, navigate]);

    const handleAddToCart = (): void => {
        if (!product) return;
        if (!isAuthenticated) {
            navigate(`/login?returnTo=/marketplace/product/${id}`);
            return;
        }
        void addToCart(product, quantity);
        setAdded(true);
        setTimeout(() => setAdded(false), 1500);
    };

    const allImages = (product?.images ?? []) as string[];

    if (loading) {
        return (
            <PageShell title="" description="">
                <div className="flex items-center justify-center py-32">
                    <div className="h-10 w-10 animate-spin rounded-full border-4 border-primary-200 border-t-primary-600" />
                </div>
            </PageShell>
        );
    }

    if (!product) return <></>;

    return (
        <PageShell title="" description="">
            {/* Back button */}
            <button
                type="button"
                onClick={() => navigate(-1)}
                className="mb-6 flex items-center gap-2 text-sm font-medium text-navy-500 hover:text-navy-800 transition-colors"
            >
                <ArrowLeft className="h-4 w-4" />
                {t('marketplace.detail.back')}
            </button>

            <div className="grid grid-cols-1 gap-10 lg:grid-cols-2">
                {/* ── Left: Images ── */}
                <div className="space-y-4">
                    <div className="overflow-hidden rounded-2xl bg-gray-100 aspect-square">
                        <img
                            src={selectedImage || product.image}
                            alt={product.name}
                            className="h-full w-full object-cover"
                        />
                    </div>

                    {allImages.length > 1 && (
                        <div className="flex gap-3 overflow-x-auto pb-1">
                            {allImages.map((img, i) => (
                                <button
                                    key={i}
                                    type="button"
                                    onClick={() => setSelectedImage(img)}
                                    className={`h-20 w-20 shrink-0 overflow-hidden rounded-lg border-2 transition-all ${
                                        selectedImage === img ? 'border-primary-500' : 'border-transparent'
                                    }`}
                                >
                                    <img src={img} alt={`${product.name} ${i + 1}`} className="h-full w-full object-cover" />
                                </button>
                            ))}
                        </div>
                    )}
                </div>

                {/* ── Right: Info ── */}
                <div className="flex flex-col gap-5">
                    {/* Category & Brand */}
                    <div className="flex items-center gap-2">
            <span className="rounded-full bg-primary-50 px-3 py-1 text-xs font-medium text-primary-600">
              {product.category}
            </span>
                        <span className="text-sm text-navy-400">{product.brand}</span>
                    </div>

                    {/* Name */}
                    <h1 className="text-2xl font-bold text-navy-900 leading-snug">{product.name}</h1>

                    {/* Rating */}
                    <div className="flex items-center gap-2">
                        <div className="flex items-center gap-1">
                            {[1, 2, 3, 4, 5].map((star) => (
                                <Star
                                    key={star}
                                    className={`h-4 w-4 ${
                                        star <= Math.round(product.rating ?? 0)
                                            ? 'fill-amber-400 text-amber-400'
                                            : 'fill-gray-200 text-gray-200'
                                    }`}
                                />
                            ))}
                        </div>
                        <span className="text-sm font-medium text-navy-700">{product.rating ?? 0}</span>
                        <span className="text-sm text-navy-400">({t('marketplace.detail.reviews', { count: product.reviews ?? 0 })})</span>
                    </div>

                    {/* Price */}
                    <div className="flex items-baseline gap-3">
            <span className="text-3xl font-extrabold text-navy-900">
              {formatCurrency(product.price)}
            </span>
                    </div>

                    {/* Supplier */}
                    <p className="text-sm text-navy-500">
                        {t('marketplace.detail.suppliedBy')}{' '}
                        <span className="font-semibold text-navy-700">{product.supplier}</span>
                    </p>

                    {/* Stock */}
                    <div className="flex items-center gap-2">
                        <Package className="h-4 w-4 text-navy-400" />
                        <span
                            className={`text-sm font-medium ${
                                (product.stock ?? 0) > 20
                                    ? 'text-green-600'
                                    : (product.stock ?? 0) > 0
                                        ? 'text-amber-600'
                                        : 'text-red-600'
                            }`}
                        >
              {(product.stock ?? 0) > 0
                ? t('marketplace.detail.unitsInStock', { count: product.stock ?? 0 })
                : t('marketplace.detail.outOfStock')}
            </span>
                    </div>

                    <hr className="border-gray-200" />

                    {/* Description */}
                    <div>
                        <h2 className="mb-2 text-base font-semibold text-navy-800">{t('marketplace.detail.description')}</h2>
                        <p className="text-sm leading-relaxed text-navy-600">
                            {product.description || t('marketplace.detail.noDescription')}
                        </p>
                    </div>

                    {/* Quantity + Add to Cart — only for non-wholesalers */}
                    {!isWholesaler && (
                        <div className="flex flex-col gap-3 pt-2">
                            <div className="flex items-center gap-3">
                                <span className="text-sm font-medium text-navy-700">{t('marketplace.detail.quantity')}</span>
                                <div className="flex items-center rounded-lg border border-gray-200 overflow-hidden">
                                    <button
                                        type="button"
                                        onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                                        className="px-3 py-2 text-navy-600 hover:bg-gray-100 transition-colors"
                                    >
                                        −
                                    </button>
                                    <span className="w-10 text-center text-sm font-semibold text-navy-800">
                    {quantity}
                  </span>
                                    <button
                                        type="button"
                                        onClick={() => setQuantity((q) => Math.min(product.stock ?? 99, q + 1))}
                                        className="px-3 py-2 text-navy-600 hover:bg-gray-100 transition-colors"
                                    >
                                        +
                                    </button>
                                </div>
                            </div>

                            <button
                                type="button"
                                onClick={handleAddToCart}
                                disabled={added || (product.stock ?? 0) === 0}
                                className={`flex w-full items-center justify-center gap-2 rounded-xl py-3 text-base font-semibold transition-all duration-300 ${
                                    added
                                        ? 'bg-green-500 text-white'
                                        : (product.stock ?? 0) === 0
                                            ? 'cursor-not-allowed bg-gray-200 text-gray-400'
                                            : 'bg-primary-600 text-white hover:bg-primary-500 hover:shadow-md active:bg-primary-700'
                                }`}
                            >
                                <ShoppingCart className="h-5 w-5" />
                                {added
                                  ? t('marketplace.detail.addedToCart')
                                  : (product.stock ?? 0) === 0
                                    ? t('marketplace.detail.outOfStockTitle')
                                    : isAuthenticated
                                      ? t('marketplace.card.addToCart')
                                      : t('marketplace.card.signInToAdd')}
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </PageShell>
    );
}
