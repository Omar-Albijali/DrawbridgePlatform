import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { useAuth } from './AuthContext';
import type { Product } from '../types';

const STORAGE_CART_KEY = 'drawbridge_cart';
const TAX_RATE = 0.15;

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

function readStoredCart(): CartItem[] {
  const rawValue = localStorage.getItem(STORAGE_CART_KEY);
  if (!rawValue) {
    return [];
  }

  try {
    return JSON.parse(rawValue) as CartItem[];
  } catch {
    return [];
  }
}

export function CartProvider({ children }: { children: ReactNode }): JSX.Element {
  const { isAuthenticated } = useAuth();
  const [items, setItems] = useState<CartItem[]>(() => readStoredCart());

  const saveItems = useCallback((nextItems: CartItem[]) => {
    setItems(nextItems);
    localStorage.setItem(STORAGE_CART_KEY, JSON.stringify(nextItems));
  }, []);

  const addToCart = useCallback(
    async (product: Product, quantity = 1): Promise<void> => {
      const existingIndex = items.findIndex((item) => item.product.id === product.id);
      if (existingIndex >= 0) {
        const nextItems = [...items];
        const currentItem = nextItems[existingIndex];
        nextItems[existingIndex] = {
          ...currentItem,
          quantity: currentItem.quantity + quantity,
        };
        saveItems(nextItems);
        return;
      }

      saveItems([...items, { product, quantity }]);
    },
    [items, saveItems],
  );

  const removeFromCart = useCallback(
    async (productId: string): Promise<void> => {
      saveItems(items.filter((item) => item.product.id !== productId));
    },
    [items, saveItems],
  );

  const updateQuantity = useCallback(
    async (productId: string, quantity: number): Promise<void> => {
      if (quantity <= 0) {
        await removeFromCart(productId);
        return;
      }

      saveItems(
        items.map((item) =>
          item.product.id === productId
            ? {
                ...item,
                quantity,
              }
            : item,
        ),
      );
    },
    [items, removeFromCart, saveItems],
  );

  const clearCart = useCallback(async (): Promise<void> => {
    saveItems([]);
  }, [saveItems]);

  const checkout = useCallback(async (): Promise<boolean> => {
    if (!isAuthenticated || items.length === 0) {
      return false;
    }

    saveItems([]);
    return true;
  }, [isAuthenticated, items.length, saveItems]);

  const itemCount = items.reduce((totalItems, item) => totalItems + item.quantity, 0);
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
