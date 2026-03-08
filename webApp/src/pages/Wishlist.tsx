import { Heart, ShoppingCart, Trash2, Package } from 'lucide-react';
import PageShell from '../components/PageShell';
import { useWishlist } from '../contexts/WishlistContext';
import { useCart } from '../contexts/CartContext';
import { useAuth } from '../contexts/AuthContext';
import { UserRole } from '../types';

export default function Wishlist(): JSX.Element {
  const { items, isLoading, removeFromWishlist } = useWishlist();
  const { addToCart } = useCart();
  const { user } = useAuth();
  const isRetailer = user?.role === UserRole.RETAILER;

  const handleMoveToCart = async (item: (typeof items)[0]): Promise<void> => {
    await addToCart(
      {
        id: item.productId,
        name: item.productName,
        price: item.productPrice,
        images: item.productImage ? [{ url: item.productImage, altText: '', sortIndex: 0 }] : [],
      } as never,
      1,
    );
    await removeFromWishlist(item.productId);
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600" />
      </div>
    );
  }

  return (
    <PageShell
      title="Wishlist"
      description={items.length > 0 ? `${items.length} saved item${items.length !== 1 ? 's' : ''}` : 'Items you have saved for later'}
    >
      {items.length === 0 ? (
        <div className="flex flex-col items-center justify-center min-h-[400px] text-center">
          <div className="w-20 h-20 bg-gray-100 rounded-full flex items-center justify-center mb-4">
            <Heart className="w-10 h-10 text-gray-300" />
          </div>
          <h3 className="text-lg font-semibold text-navy-800 mb-2">Your wishlist is empty</h3>
          <p className="text-navy-400 text-sm max-w-xs">
            Save products you're interested in and come back to them later.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {items.map((item) => (
            <div key={item.id} className="card group hover:shadow-lg transition-shadow duration-200">
              <div className="relative mb-4">
                {item.productImage ? (
                  <img
                    src={item.productImage}
                    alt={item.productName}
                    className="w-full h-48 object-cover rounded-xl"
                  />
                ) : (
                  <div className="w-full h-48 bg-gray-100 rounded-xl flex items-center justify-center">
                    <Package className="w-12 h-12 text-gray-300" />
                  </div>
                )}
                <button
                  type="button"
                  onClick={() => void removeFromWishlist(item.productId)}
                  className="absolute top-2 right-2 w-8 h-8 bg-white rounded-full shadow-md flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity hover:bg-red-50"
                >
                  <Trash2 className="w-4 h-4 text-red-500" />
                </button>
              </div>

              <div className="space-y-3">
                <div>
                  <h3 className="font-semibold text-navy-800 text-sm line-clamp-2 leading-snug">
                    {item.productName}
                  </h3>
                  <p className="text-primary-600 font-bold text-base mt-1">
                    SAR {Number(item.productPrice).toFixed(2)}
                  </p>
                </div>

                <div className="flex gap-2">
                  {isRetailer && (
                    <button
                      type="button"
                      onClick={() => void handleMoveToCart(item)}
                      className="flex-1 btn-primary text-xs py-2 flex items-center justify-center gap-1.5"
                    >
                      <ShoppingCart className="w-3.5 h-3.5" />
                      Move to Cart
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={() => void removeFromWishlist(item.productId)}
                    className="w-9 h-9 flex items-center justify-center rounded-lg border border-gray-200 hover:bg-red-50 hover:border-red-200 transition-colors"
                    title="Remove from wishlist"
                  >
                    <Heart className="w-4 h-4 text-red-400 fill-red-400" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </PageShell>
  );
}