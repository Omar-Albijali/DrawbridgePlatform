import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react';
import { useAuth } from './AuthContext';
import { type Product } from '../types';
import { productService } from '../services/productService';
import { wishlistService, type WishlistItemDto } from '../services/wishlistService';

export interface ProductWishlistItem {
  wishlistItem: WishlistItemDto;
  product: Product;
}

interface WishlistContextType {
  items: ProductWishlistItem[];
  itemCount: number;
  isLoading: boolean;
  addToWishlist: (productId: string) => Promise<void>;
  removeFromWishlist: (productId: string) => Promise<void>;
  isInWishlist: (productId: string) => boolean;
  toggleWishlist: (productId: string) => Promise<void>;
}

const WishlistContext = createContext<WishlistContextType | undefined>(undefined);

export function WishlistProvider({ children }: { children: ReactNode }): JSX.Element {
  const { isAuthenticated, user } = useAuth();
  const [wishlistItems, setWishlistItems] = useState<ProductWishlistItem[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const refreshWishlistFromServer = useCallback(async (): Promise<void> => {
    if (!isAuthenticated || !user?.id) {
      setWishlistItems([]);
      return;
    }

    setIsLoading(true);
    try {
      const apiItems = await wishlistService.getByUser(user.id);
      const products = await Promise.all(apiItems.map((item) => productService.getById(item.productId)));

      setWishlistItems(
        apiItems.map((item, index) => ({
          wishlistItem: item,
          product: products[index],
        })),
      );
    } catch (error) {
      console.error('Failed to fetch wishlist:', error);
      setWishlistItems([]);
    } finally {
      setIsLoading(false);
    }
  }, [isAuthenticated, user?.id]);

  useEffect(() => {
    void refreshWishlistFromServer();
  }, [refreshWishlistFromServer]);

  const addToWishlist = useCallback(
    async (productId: string): Promise<void> => {
      if (!isAuthenticated || !user?.id) {
        return;
      }

      try {
        await wishlistService.add(user.id, productId);
        await refreshWishlistFromServer();
      } catch (error) {
        console.error('Failed to add to wishlist:', error);
      }
    },
    [isAuthenticated, refreshWishlistFromServer, user?.id],
  );

  const removeFromWishlist = useCallback(
    async (productId: string): Promise<void> => {
      if (!isAuthenticated || !user?.id) {
        return;
      }

      try {
        await wishlistService.remove(user.id, productId);
        await refreshWishlistFromServer();
      } catch (error) {
        console.error('Failed to remove from wishlist:', error);
      }
    },
    [isAuthenticated, refreshWishlistFromServer, user?.id],
  );

  const isInWishlist = useCallback(
    (productId: string): boolean => {
      return wishlistItems.some((item) => item.wishlistItem.productId === productId);
    },
    [wishlistItems],
  );

  const toggleWishlist = useCallback(
    async (productId: string): Promise<void> => {
      if (isInWishlist(productId)) {
        await removeFromWishlist(productId);
      } else {
        await addToWishlist(productId);
      }
    },
    [isInWishlist, addToWishlist, removeFromWishlist],
  );

  const items = wishlistItems;
  const itemCount = items.length;

  const value: WishlistContextType = {
    items,
    itemCount,
    isLoading,
    addToWishlist,
    removeFromWishlist,
    isInWishlist,
    toggleWishlist,
  };

  return <WishlistContext.Provider value={value}>{children}</WishlistContext.Provider>;
}

export function useWishlist(): WishlistContextType {
  const context = useContext(WishlistContext);
  if (!context) {
    throw new Error('useWishlist must be used within a WishlistProvider');
  }
  return context;
}
