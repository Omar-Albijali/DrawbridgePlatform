import { useState } from 'react';
import { Heart, ShoppingCart, Star } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useCart } from '../../contexts/CartContext';
import { useWishlist } from '../../contexts/WishlistContext';
import { useAuth } from '../../contexts/AuthContext';
import { formatCurrency } from '../../i18n/display';
import { UserRole, type Product } from '../../types';

interface ProductCardProps {
  product: Product;
  isAuthenticated: boolean;
  canAddToCart?: boolean;
  onAuthRequired?: (returnPath?: string) => void;
}

export default function ProductCard({
                                      product,
                                      isAuthenticated,
                                      canAddToCart = true,
                                      onAuthRequired,
                                    }: ProductCardProps): JSX.Element {
  const { t } = useTranslation();
  const { addToCart } = useCart();
  const { isInWishlist, toggleWishlist } = useWishlist();
  const { user } = useAuth();
  const navigate = useNavigate();
  const isRetailer = user?.role === UserRole.RETAILER;
  const [added, setAdded] = useState(false);
  const inWishlist = isInWishlist(product.id);

  const handleCardClick = (e: React.MouseEvent<HTMLDivElement>): void => {
    if ((e.target as HTMLElement).closest('button')) return;
    navigate(`/marketplace/product/${product.id}`);
  };

  const handleAddToCart = (): void => {
    if (!canAddToCart) return;
    if (!isAuthenticated) {
      onAuthRequired?.('/marketplace');
      return;
    }
    void addToCart(product, 1);
    setAdded(true);
    setTimeout(() => setAdded(false), 1000);
  };

  const handleToggleWishlist = (): void => {
    if (!isAuthenticated) {
      onAuthRequired?.('/marketplace');
      return;
    }
    void toggleWishlist(product.id);
  };

  return (
      <div
          onClick={handleCardClick}
          className="buyer-product-card bg-white rounded-xl shadow-card overflow-hidden group hover:shadow-card-hover transition-all duration-300 relative cursor-pointer"
      >
        <div className="buyer-product-card__media relative aspect-[4/3] overflow-hidden bg-gray-100">
          <img
              src={product.image}
              alt={product.name}
              className="buyer-product-card__image w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          />
          {(product.stock ?? 0) < 20 && (
              <span className="buyer-product-card__badge absolute top-3 right-3 bg-amber-500 text-white text-xs font-bold px-2 py-1 rounded-full">
            {t('marketplace.card.lowStock')}
          </span>
          )}
        </div>

        {isRetailer && (
            <button
                type="button"
                onClick={handleToggleWishlist}
                className={`absolute top-3 right-3 z-20 pointer-events-auto w-8 h-8 rounded-full flex items-center justify-center shadow-md transition-all duration-200 ${
                    inWishlist
                        ? 'bg-red-500 text-white scale-110'
                        : 'bg-white text-navy-400 hover:text-red-500 hover:scale-110'
                }`}
                title={inWishlist ? t('marketplace.card.removeWishlist') : t('marketplace.card.addWishlist')}
            >
              <Heart className={`w-4 h-4 transition-all duration-200 ${inWishlist ? 'fill-white' : ''}`} />
            </button>
        )}

        <div className="buyer-product-card__body p-4">
          <div className="flex items-center justify-between mb-2">
          <span className="buyer-product-card__category text-xs font-medium text-primary-600 bg-primary-50 px-2 py-1 rounded-full">
            {product.category}
          </span>
            <span className="text-xs text-navy-500">{product.brand}</span>
          </div>

          <h3 className="font-semibold text-navy-800 mb-2 line-clamp-2 min-h-[48px]">{product.name}</h3>

          <div className="flex items-center gap-1 mb-3">
            <Star className="w-4 h-4 fill-amber-400 text-amber-400" />
            <span className="text-sm font-medium text-navy-700">{product.rating ?? 0}</span>
            <span className="text-sm text-navy-400">({product.reviews ?? 0})</span>
          </div>

          <div className="flex items-baseline gap-2 mb-4">
          <span className="buyer-product-card__price text-xl font-bold text-navy-800">
            {formatCurrency(product.price)}
          </span>
          </div>

          <p className="text-xs text-navy-500 mb-4">{t('marketplace.card.suppliedBy', { supplier: product.supplier })}</p>

          {canAddToCart ? (
              <button
                  type="button"
                  onClick={handleAddToCart}
                  disabled={added}
                  className={`buyer-product-card__cta w-full flex items-center justify-center gap-2 py-2.5 rounded-lg font-semibold transition-all duration-300 ${
                      added
                          ? 'cursor-default bg-green-500 text-white'
                          : 'bg-primary-600 text-white hover:bg-primary-500 hover:shadow-md active:bg-primary-700'
                  }`}
              >
                {added ? (
                    <>
                      <ShoppingCart className="w-4 h-4 fill-white" />
                      {t('marketplace.card.added')}
                    </>
                ) : (
                    <>
                      <ShoppingCart className="w-4 h-4" />
                      {isAuthenticated ? t('marketplace.card.addToCart') : t('marketplace.card.signInToAdd')}
                    </>
                )}
              </button>
          ) : null}
        </div>
      </div>
  );
}
