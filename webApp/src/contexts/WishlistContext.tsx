import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { useAuth } from './AuthContext';
import { type Product } from '../types';
import { productService } from '../services/productService';

interface WishlistApiItem {
  id: string;
  userId: string;
  productId: string;
  createdAt: string;
}

export interface WishlistItem {
  id: string;
  userId: string;
  productId: string;
  createdAt: string;
  product: Product | null;
}

interface WishlistContextType {
  items: WishlistItem[];
  itemCount: number;
  isLoading: boolean;
  addToWishlist: (productId: string) => Promise<void>;
  removeFromWishlist: (productId: string) => Promise<void>;
  isInWishlist: (productId: string) => boolean;
  toggleWishlist: (productId: string) => Promise<void>;
}

const WishlistContext = createContext<WishlistContextType | undefined>(undefined);

const API_BASE = 'http://localhost:8080/api/wishlist';

function toApiItem(raw: Partial<WishlistApiItem>, fallbackUserId: string, fallbackProductId: string): WishlistApiItem {
  return {
    id: raw.id ?? `${fallbackUserId}-${fallbackProductId}`,
    userId: raw.userId ?? fallbackUserId,
    productId: raw.productId ?? fallbackProductId,
    createdAt: raw.createdAt ?? new Date().toISOString(),
  };
}

export function WishlistProvider({ children }: { children: ReactNode }): JSX.Element {
  const { isAuthenticated, user } = useAuth();
  const [entries, setEntries] = useState<WishlistApiItem[]>([]);
  const [productsById, setProductsById] = useState<Record<string, Product>>({});
  const [isLoading, setIsLoading] = useState(false);

  const fetchWishlist = useCallback(async (): Promise<void> => {
    if (!isAuthenticated || !user?.id) {
      setEntries([]);
      return;
    }

    setIsLoading(true);
    try {
      const token = localStorage.getItem('drawbridge_token') ?? sessionStorage.getItem('drawbridge_token') ?? '';
      const response = await fetch(`${API_BASE}/${user.id}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (response.ok) {
        const data = (await response.json()) as WishlistApiItem[];
        setEntries(data);
      }
    } catch (error) {
      console.error('Failed to fetch wishlist:', error);
    } finally {
      setIsLoading(false);
    }
  }, [isAuthenticated, user?.id]);

  useEffect(() => {
    void fetchWishlist();
  }, [fetchWishlist]);

  useEffect(() => {
    if (entries.length === 0) {
      return;
    }

    const missingIds = entries
      .map((entry) => entry.productId)
      .filter((productId) => !productsById[productId]);

    if (missingIds.length === 0) {
      return;
    }

    let isCancelled = false;
    void Promise.allSettled(missingIds.map((productId) => productService.getById(productId))).then((results) => {
      if (isCancelled) {
        return;
      }

      const nextProducts: Record<string, Product> = {};
      results.forEach((result, index) => {
        if (result.status === 'fulfilled') {
          nextProducts[missingIds[index]] = result.value;
        }
      });

      if (Object.keys(nextProducts).length > 0) {
        setProductsById((prev) => ({ ...prev, ...nextProducts }));
      }
    });

    return () => {
      isCancelled = true;
    };
  }, [entries, productsById]);

  const addToWishlist = useCallback(
    async (productId: string): Promise<void> => {
      if (!isAuthenticated || !user?.id) {
        return;
      }

      try {
        const token = localStorage.getItem('drawbridge_token') ?? sessionStorage.getItem('drawbridge_token') ?? '';
        const response = await fetch(`${API_BASE}/${user.id}/${productId}`, {
          method: 'POST',
          headers: { Authorization: `Bearer ${token}` },
        });

        if (response.ok) {
          const raw = (await response.json()) as Partial<WishlistApiItem>;
          const nextItem = toApiItem(raw, user.id, productId);
          setEntries((prev) => (prev.some((item) => item.productId === nextItem.productId) ? prev : [...prev, nextItem]));
        }
      } catch (error) {
        console.error('Failed to add to wishlist:', error);
      }
    },
    [isAuthenticated, user?.id],
  );

  const removeFromWishlist = useCallback(
    async (productId: string): Promise<void> => {
      if (!isAuthenticated || !user?.id) {
        return;
      }

      try {
        const token = localStorage.getItem('drawbridge_token') ?? sessionStorage.getItem('drawbridge_token') ?? '';
        await fetch(`${API_BASE}/${user.id}/${productId}`, {
          method: 'DELETE',
          headers: { Authorization: `Bearer ${token}` },
        });
        setEntries((prev) => prev.filter((item) => item.productId !== productId));
      } catch (error) {
        console.error('Failed to remove from wishlist:', error);
      }
    },
    [isAuthenticated, user?.id],
  );

  const isInWishlist = useCallback(
    (productId: string): boolean => {
      return entries.some((item) => item.productId === productId);
    },
    [entries],
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

  const items = useMemo<WishlistItem[]>(
    () =>
      entries.map((entry) => ({
        ...entry,
        product: productsById[entry.productId] ?? null,
      })),
    [entries, productsById],
  );

  const itemCount = entries.length;

  const value = useMemo<WishlistContextType>(
    () => ({
      items,
      itemCount,
      isLoading,
      addToWishlist,
      removeFromWishlist,
      isInWishlist,
      toggleWishlist,
    }),
    [items, itemCount, isLoading, addToWishlist, removeFromWishlist, isInWishlist, toggleWishlist],
  );

  return <WishlistContext.Provider value={value}>{children}</WishlistContext.Provider>;
}

export function useWishlist(): WishlistContextType {
  const context = useContext(WishlistContext);
  if (!context) {
    throw new Error('useWishlist must be used within a WishlistProvider');
  }
  return context;
}
