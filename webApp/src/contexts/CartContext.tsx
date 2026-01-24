import React, { createContext, useContext, useState, useCallback, ReactNode, useEffect } from 'react';
import { Product, UserRole } from '../types';
import { cartService } from '../services/cartService';
import { useAuth } from './AuthContext';

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

export const useCart = () => {
    const context = useContext(CartContext);
    if (!context) {
        throw new Error('useCart must be used within a CartProvider');
    }
    return context;
};

interface CartProviderProps {
    children: ReactNode;
}

const TAX_RATE = 0.15; // 15% VAT

export const CartProvider: React.FC<CartProviderProps> = ({ children }) => {
    const { user } = useAuth();
    const [items, setItems] = useState<CartItem[]>(() => {
        // Initial state from local storage for guests or first load
        const savedCart = localStorage.getItem('drawbridge_cart');
        return savedCart ? JSON.parse(savedCart) : [];
    });

    // Helper to sync local storage, mainly for guest users or cache
    const saveLocally = (newItems: CartItem[]) => {
        setItems(newItems);
        localStorage.setItem('drawbridge_cart', JSON.stringify(newItems));
    };

    // Sync from Server on Login
    useEffect(() => {
        const fetchRemoteCart = async () => {
            if (user?.id && user.role === UserRole.RETAILER) {
                try {
                    const retailerId = user.id;
                    await cartService.getItems(retailerId);

                    // Note: Ideally we would map the remote items (which are just IDs) 
                    // to full Product objects here. For now, we trust the local optimistic state 
                    // or assume products are loaded elsewhere.
                    // This limitation is noted for future refactoring.

                } catch (e) {
                    console.error("Failed to sync cart", e);
                }
            }
        };
        fetchRemoteCart();
    }, [user]);

    const addToCart = useCallback(async (product: Product, quantity: number = 1) => {
        // Optimistic update
        const currentItems = [...items];
        const existingItemIndex = currentItems.findIndex(item => item.product.id === product.id);

        let newItems: CartItem[];
        if (existingItemIndex >= 0) {
            const updatedItem = { ...currentItems[existingItemIndex] };
            updatedItem.quantity += quantity;
            newItems = [...currentItems];
            newItems[existingItemIndex] = updatedItem;
        } else {
            newItems = [...currentItems, { product, quantity }];
        }

        saveLocally(newItems);

        // Server Sync
        if (user?.id && user.role === UserRole.RETAILER) {
            try {
                await cartService.addItem(user.id, product.id, quantity);
            } catch (error) {
                console.error("Failed to add to server cart", error);
            }
        }
    }, [items, user]);

    const removeFromCart = useCallback(async (productId: string) => {
        const newItems = items.filter(item => item.product.id !== productId);
        saveLocally(newItems);

        if (user?.id && user.role === UserRole.RETAILER) {
            try {
                await cartService.removeItem(user.id, productId);
            } catch (error) {
                console.error("Failed to remove from server cart", error);
            }
        }
    }, [items, user]);

    const updateQuantity = useCallback(async (productId: string, quantity: number) => {
        if (quantity <= 0) {
            await removeFromCart(productId);
            return;
        }

        const newItems = items.map(item =>
            item.product.id === productId
                ? { ...item, quantity }
                : item
        );
        saveLocally(newItems);

        if (user?.id && user.role === UserRole.RETAILER) {
            try {
                await cartService.updateQuantity(user.id, productId, quantity);
            } catch (error) {
                console.error("Failed to update server cart quantity", error);
            }
        }
    }, [items, user, removeFromCart]);

    const clearCart = useCallback(async () => {
        saveLocally([]);

        if (user?.id && user.role === UserRole.RETAILER) {
            try {
                await cartService.clear(user.id);
            } catch (error) {
                console.error("Failed to clear server cart", error);
            }
        }
    }, [user]);

    const checkout = useCallback(async () => {
        if (!user?.id || user.role !== UserRole.RETAILER) return false;

        try {
            // Force sync local cart to server before checkout to ensure server has latest state
            await cartService.clear(user.id);

            let validItems: CartItem[] = [];
            let invalidItemIds: string[] = [];

            if (items.length > 0) {
                // We use a for loop or Promise.all to map results
                const syncResults = await Promise.all(items.map(async (item) => {
                    try {
                        await cartService.addItem(user.id, item.product.id, item.quantity);
                        return { item, success: true };
                    } catch (e) {
                        console.warn(`Removing stale item from cart: ${item.product.id} (${item.product.name})`, e);
                        return { item, success: false };
                    }
                }));

                validItems = syncResults.filter(r => r.success).map(r => r.item);
                invalidItemIds = syncResults.filter(r => !r.success).map(r => r.item.product.id);
            }

            // If we found invalid items, update local state immediately
            if (invalidItemIds.length > 0) {
                console.warn(`Removed ${invalidItemIds.length} invalid items from cart`);
                saveLocally(validItems);
                setItems(validItems);
            }

            if (validItems.length === 0 && items.length > 0) {
                // All items failed were invalid
                alert("Local cart data was out of sync with the server. Your cart has been refreshed and invalid items were removed. Please continue shopping.");
                return false;
            }

            // If we have valid items (or originally had none, though logic prevents that path), proceed
            if (validItems.length > 0) {
                await cartService.checkout(user.id);

                // Clear local cart after successful checkout
                saveLocally([]);
                setItems([]);
                return true;
            }

            return false;
        } catch (error) {
            console.error("Checkout failed", error);
            return false;
        }
    }, [user, items]);

    const itemCount = items.reduce((sum, item) => sum + item.quantity, 0);
    const subtotal = items.reduce((sum, item) => sum + (item.product.price * item.quantity), 0);
    const tax = subtotal * TAX_RATE;
    const total = subtotal + tax;

    const value: CartContextType = {
        items,
        itemCount,
        subtotal,
        tax,
        total,
        addToCart,
        removeFromCart,
        updateQuantity,
        clearCart,
        checkout
    };

    return (
        <CartContext.Provider value={value}>
            {children}
        </CartContext.Provider>
    );
};

export default CartContext;
