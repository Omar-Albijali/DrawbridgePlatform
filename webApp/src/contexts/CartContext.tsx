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
import { UserRole, type CartItem, type Product } from '../types';
import { productService } from '../services/productService';
import { cartService } from '../services/cartService';
import i18n from '../i18n';

const TAX_RATE = 0.15;

// interface ServerCartItem {
//   productId: string;
//   quantity: number;
// }
//
export interface ProductCartItem {
  cartItem: CartItem;
  product: Product;
  quantity: number;
}

interface CartContextType {
  items: ProductCartItem[];
  itemCount: number;
  subtotal: number;
  tax: number;
  total: number;
  addToCart: (product: Product, quantity?: number) => Promise<void>;
  removeFromCart: (productId: string) => Promise<void>;
  updateQuantity: (productId: string, quantity: number) => Promise<void>;
  clearCart: () => Promise<void>;
  checkout: () => Promise<{ success: boolean; message?: string }>;
}

const CartContext = createContext<CartContextType | undefined>(undefined);

export function CartProvider({ children }: { children: ReactNode }): JSX.Element {
  const { isAuthenticated, user } = useAuth();
  const isWholesaler = user?.role === UserRole.WHOLESALER;
  const [cartItems, setCartItems] = useState<ProductCartItem[]>([]);
  // const [productsById, setProductsById] = useState<Record<string, Product>>({});

/*  const normalizeApiItems = useCallback((items: ApiCartItem[]): ServerCartItem[] => {
    return items.map((item) => ({
        productId: item.productId,
        quantity: item.quantity > 0 ? item.quantity : 1,
      }));
  }, []);*/

  const refreshCartFromServer = useCallback(async (): Promise<void> => {
    if (!isAuthenticated || isWholesaler || !user?.id) {
        return;
    }

    try {
      const apiItems = await cartService.getItems(user.id);
      const products = await Promise.all(apiItems.map((item) => productService.getById(item.productId)));
      setCartItems(
          apiItems.map((item, index) => (
              {   cartItem: item,
                  product: products[index],
                  quantity: item.quantity}
              )
          )
      );
    } catch (error) {
      console.error('Failed to fetch cart from server', error);
      setCartItems([]);
    }
  }, [isAuthenticated, isWholesaler, user?.id]);

  useEffect(() => {
    void refreshCartFromServer();
  }, [refreshCartFromServer]);


  const addToCart = useCallback(
    async (product: Product, quantity = 1): Promise<void> => {
      if (!isAuthenticated || isWholesaler || !user?.id) {
        return;
      }
      try {
        await cartService.addItem(user.id, product.id, quantity);
        await refreshCartFromServer();
      } catch (error) {
        console.error('Failed to add item to cart', error);
      }
    },
    [isAuthenticated, isWholesaler, refreshCartFromServer, user?.id],
  );

  const removeFromCart = useCallback(
    async (productId: string): Promise<void> => {
      if (!isAuthenticated || isWholesaler || !user?.id) {
        return;
      }

      try {
        await cartService.removeItem(user.id, productId);
        await refreshCartFromServer();
      } catch (error) {
        console.error('Failed to remove item from cart', error);
      }
    },
    [isAuthenticated, isWholesaler, refreshCartFromServer, user?.id],
  );

  const updateQuantity = useCallback(
    async (productId: string, quantity: number): Promise<void> => {
      if (!isAuthenticated || isWholesaler || !user?.id) {
        return;
      }

      if (quantity <= 0) {
        await removeFromCart(productId);
        return;
      }

      try {
        await cartService.updateQuantity(user.id, productId, quantity);
        await refreshCartFromServer();
      } catch (error) {
        console.error('Failed to update item quantity', error);
      }
    },
    [isAuthenticated, isWholesaler, refreshCartFromServer, removeFromCart, user?.id],
  );

  const clearCart = useCallback(async (): Promise<void> => {
    if (!isAuthenticated || isWholesaler || !user?.id) {
      return;
    }

    try {
      await cartService.clear(user.id);
      await refreshCartFromServer();
    } catch (error) {
      console.error('Failed to clear cart', error);
    }
  }, [isAuthenticated, isWholesaler, user?.id]);

  const items = cartItems;

  const checkout = useCallback(async (): Promise<{ success: boolean; message?: string }> => {
    if (!isAuthenticated) {
      return { success: false, message: i18n.t('cart.checkoutErrors.signIn') };
    }

    if (isWholesaler) {
      return { success: false, message: i18n.t('cart.checkoutErrors.wholesale') };
    }

    if (!user?.id) {
      return { success: false, message: i18n.t('cart.checkoutErrors.missingSession') };
    }

    if (items.length === 0) {
      return { success: false, message: i18n.t('cart.checkoutErrors.emptyCart') };
    }

    try {
      await cartService.checkout(user.id);
      await clearCart();
      return { success: true };
    } catch (error) {
      console.error('Checkout failed', error);
      return {
        success: false,
        message: error instanceof Error ? error.message : i18n.t('cart.checkoutErrors.failed'),
      };
    }
  }, [clearCart, isAuthenticated, isWholesaler, items.length, user?.id]);

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
