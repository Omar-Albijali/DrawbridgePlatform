import { useState } from 'react';
import { ShoppingCart, Star } from 'lucide-react';
import { useCart } from '../../contexts/CartContext';
import type { Product } from '../../types';

interface ProductCardProps {
  product: Product;
}

export default function ProductCard({ product }: ProductCardProps): JSX.Element {
  const { addToCart } = useCart();
  const [added, setAdded] = useState(false);

  const handleAddToCart = (): void => {
    void addToCart(product, 1);
    setAdded(true);
    setTimeout(() => setAdded(false), 1000);
  };

  const originalPrice = product.originalPrice ?? undefined;
  const discount =
    originalPrice && originalPrice > product.price
      ? Math.round(((originalPrice - product.price) / originalPrice) * 100)
      : 0;

  return (
    <div className="bg-white rounded-xl shadow-card overflow-hidden group hover:shadow-card-hover transition-all duration-300">
      <div className="relative aspect-[4/3] overflow-hidden bg-gray-100">
        <img
          src={product.image}
          alt={product.name}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
        />
        {discount > 0 && (
          <span className="absolute top-3 left-3 bg-red-500 text-white text-xs font-bold px-2 py-1 rounded-full">
            -{discount}%
          </span>
        )}
        {(product.stock ?? 0) < 20 && (
          <span className="absolute top-3 right-3 bg-amber-500 text-white text-xs font-bold px-2 py-1 rounded-full">
            Low Stock
          </span>
        )}
      </div>

      <div className="p-4">
        <div className="flex items-center justify-between mb-2">
          <span className="text-xs font-medium text-primary-600 bg-primary-50 px-2 py-1 rounded-full">
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
          <span className="text-xl font-bold text-navy-800">SAR {product.price.toFixed(2)}</span>
          {originalPrice && (
            <span className="text-sm text-navy-400 line-through">SAR {originalPrice.toFixed(2)}</span>
          )}
        </div>

        <p className="text-xs text-navy-500 mb-4">Supplied by: {product.supplier}</p>

        <button
          type="button"
          onClick={handleAddToCart}
          disabled={added}
          className={`w-full flex items-center justify-center gap-2 py-2.5 rounded-lg font-semibold transition-all duration-300 ${
            added ? 'bg-green-500 text-white cursor-default' : 'btn-primary'
          }`}
        >
          {added ? (
            <>
              <ShoppingCart className="w-4 h-4 fill-white" />
              Added!
            </>
          ) : (
            <>
              <ShoppingCart className="w-4 h-4" />
              Add to Cart
            </>
          )}
        </button>
      </div>
    </div>
  );
}
