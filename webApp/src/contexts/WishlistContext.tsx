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

export interface WishlistItem {
  id: string;
  userId: string;
  productId: string;
  productName: string;
  productPrice: number;
  productImage: string;
  createdAt: string;
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

export function WishlistProvider({ children }: { children: ReactNode }): JSX.Element {
  const { isAuthenticated, user } = useAuth();
  const [items, setItems] = useState<WishlistItem[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const fetchWishlist = useCallback(async (): Promise<void> => {
    if (!isAuthenticated || !user?.id) return;
    setIsLoading(true);
    try {
    const token = localStorage.getItem('drawbridge_token') ?? sessionStorage.getItem('drawbridge_token') ?? '';      const response = await fetch(`${API_BASE}/${user.id}`, {
        headers: { Authorization: `Bearer ${token ?? ''}` },
      });
      if (response.ok) {
        const data = (await response.json()) as WishlistItem[];
        setItems(data);
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

  const addToWishlist = useCallback(
    async (productId: string): Promise<void> => {
      if (!isAuthenticated || !user?.id) return;
      try {
        const token = localStorage.getItem('drawbridge_token') ?? sessionStorage.getItem('drawbridge_token') ?? '';
        const response = await fetch(`${API_BASE}/${user.id}/${productId}`, {
          method: 'POST',
          headers: { Authorization: `Bearer ${token ?? ''}` },
        });
        if (response.ok) {
          const item = (await response.json()) as WishlistItem;
          setItems((prev) => [...prev, item]);
        }
      } catch (error) {
        console.error('Failed to add to wishlist:', error);
      }
    },
    [isAuthenticated, user?.id],
  );

  const removeFromWishlist = useCallback(
    async (productId: string): Promise<void> => {
      if (!isAuthenticated || !user?.id) return;
      try {
        const token = localStorage.getItem('drawbridge_token') ?? sessionStorage.getItem('drawbridge_token') ?? '';
        await fetch(`${API_BASE}/${user.id}/${productId}`, {
          method: 'DELETE',
          headers: { Authorization: `Bearer ${token ?? ''}` },
        });
        setItems((prev) => prev.filter((item) => item.productId !== productId));
      } catch (error) {
        console.error('Failed to remove from wishlist:', error);
      }
    },
    [isAuthenticated, user?.id],
  );

  const isInWishlist = useCallback(
    (productId: string): boolean => {
      return items.some((item) => item.productId === productId);
    },
    [items],
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

  const itemCount = items.length;

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