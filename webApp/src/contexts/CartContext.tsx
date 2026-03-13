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
import { UserRole, type Product } from '../types';
import { productService } from '../services/productService';

const STORAGE_CART_KEY = 'drawbridge_cart';
const TAX_RATE = 0.15;

interface StoredCartItem {
  productId: string;
  quantity: number;
}

export interface CartItem {
  product: Product;
  quantity: number;
}

interface CartContextType {
  items: CartItem[];
  itemCount: number;
  subtotal: number;
  tax: number;
  total: number;
  addToCart: (product: Product, quantity?: number) => Promise<void>;
  removeFromCart: (productId: string) => Promise<void>;
  updateQuantity: (productId: string, quantity: number) => Promise<void>;
  clearCart: () => Promise<void>;
  checkout: () => Promise<boolean>;
}

const CartContext = createContext<CartContextType | undefined>(undefined);

function readStoredCart(): StoredCartItem[] {
  const rawValue = localStorage.getItem(STORAGE_CART_KEY);
  if (!rawValue) {
    return [];
  }

  try {
    const parsed = JSON.parse(rawValue) as unknown;
    if (!Array.isArray(parsed)) {
      return [];
    }

    return parsed
      .filter(
        (item): item is StoredCartItem =>
          typeof item === 'object' &&
          item !== null &&
          'productId' in item &&
          typeof item.productId === 'string' &&
          item.productId.length > 0,
      )
      .map((item) => ({
        productId: item.productId,
        quantity: item.quantity > 0 ? item.quantity : 1,
      }));
  } catch {
    return [];
  }
}

function writeStoredCart(items: StoredCartItem[]): void {
  localStorage.setItem(STORAGE_CART_KEY, JSON.stringify(items));
}

export function CartProvider({ children }: { children: ReactNode }): JSX.Element {
  const { isAuthenticated, user } = useAuth();
  const isWholesaler = user?.role === UserRole.WHOLESALER;
  const [storedItems, setStoredItems] = useState<StoredCartItem[]>(() => readStoredCart());
  const [productsById, setProductsById] = useState<Record<string, Product>>({});

  const saveStoredItems = useCallback((nextItems: StoredCartItem[]) => {
    setStoredItems(nextItems);
    writeStoredCart(nextItems);
  }, []);

  useEffect(() => {
    // Persist migrated shape after first read to keep local storage normalized.
    writeStoredCart(storedItems);
  }, [storedItems]);

  useEffect(() => {
    if (storedItems.length === 0) {
      return;
    }

    const missingIds = storedItems
      .map((item) => item.productId)
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
  }, [productsById, storedItems]);

  const addToCart = useCallback(
    async (product: Product, quantity = 1): Promise<void> => {
      if (!isAuthenticated || isWholesaler) {
        return;
      }

      setProductsById((prev) => ({ ...prev, [product.id]: product }));

      const existingIndex = storedItems.findIndex((item) => item.productId === product.id);
      if (existingIndex >= 0) {
        const nextItems = [...storedItems];
        const currentItem = nextItems[existingIndex];
        nextItems[existingIndex] = {
          ...currentItem,
          quantity: currentItem.quantity + quantity,
        };
        saveStoredItems(nextItems);
        return;
      }

      saveStoredItems([...storedItems, { productId: product.id, quantity }]);
    },
    [isAuthenticated, isWholesaler, saveStoredItems, storedItems],
  );

  const removeFromCart = useCallback(
    async (productId: string): Promise<void> => {
      saveStoredItems(storedItems.filter((item) => item.productId !== productId));
    },
    [saveStoredItems, storedItems],
  );

  const updateQuantity = useCallback(
    async (productId: string, quantity: number): Promise<void> => {
      if (quantity <= 0) {
        await removeFromCart(productId);
        return;
      }

      saveStoredItems(
        storedItems.map((item) =>
          item.productId === productId
            ? {
                ...item,
                quantity,
              }
            : item,
        ),
      );
    },
    [removeFromCart, saveStoredItems, storedItems],
  );

  const clearCart = useCallback(async (): Promise<void> => {
    saveStoredItems([]);
  }, [saveStoredItems]);

  const checkout = useCallback(async (): Promise<boolean> => {
    if (!isAuthenticated || isWholesaler || storedItems.length === 0) {
      return false;
    }

    saveStoredItems([]);
    return true;
  }, [isAuthenticated, isWholesaler, saveStoredItems, storedItems.length]);

  const items = useMemo<CartItem[]>(
    () =>
      storedItems
        .map((item) => {
          const product = productsById[item.productId];
          if (!product) {
            return null;
          }

          return {
            product,
            quantity: item.quantity,
          };
        })
        .filter((item): item is CartItem => item !== null),
    [productsById, storedItems],
  );

  const itemCount = storedItems.reduce((totalItems, item) => totalItems + item.quantity, 0);
  const subtotal = items.reduce((sum, item) => sum + item.product.price * item.quantity, 0);
  const tax = subtotal * TAX_RATE;
  const total = subtotal + tax;

  const value = useMemo<CartContextType>(
    () => ({
      items,
      itemCount,
      subtotal,
      tax,
      total,
      addToCart,
      removeFromCart,
      updateQuantity,
      clearCart,
      checkout,
    }),
    [addToCart, checkout, clearCart, itemCount, items, removeFromCart, subtotal, tax, total, updateQuantity],
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart(): CartContextType {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
}
