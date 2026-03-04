import { fetchApi } from './api';
import { ShoppingCart, CartItem, OrderGroup, AddToCartRequest } from '../types';

export const cartService = {
    getCart: (retailerId: string) => fetchApi<ShoppingCart>(`/cart/${retailerId}`),

    getItems: (retailerId: string) => fetchApi<CartItem[]>(`/cart/${retailerId}/items`),

    getCount: (retailerId: string) => fetchApi<{ count: number }>(`/cart/${retailerId}/count`),

    addItem: (retailerId: string, productId: string, quantity: number) => {
        const request = { productId, quantity } as unknown as AddToCartRequest;
        return fetchApi<CartItem>(`/cart/${retailerId}/items`, {
            method: 'POST',
            body: JSON.stringify(request)
        });
    },

    updateQuantity: (retailerId: string, productId: string, quantity: number) => fetchApi<CartItem>(`/cart/${retailerId}/items/${productId}?quantity=${quantity}`, {
        method: 'PUT'
    }),

    removeItem: (retailerId: string, productId: string) => fetchApi<void>(`/cart/${retailerId}/items/${productId}`, {
        method: 'DELETE'
    }),

    clear: (retailerId: string) => fetchApi<void>(`/cart/${retailerId}`, {
        method: 'DELETE'
    }),

    checkout: (retailerId: string) => fetchApi<OrderGroup>(`/cart/${retailerId}/checkout`, {
        method: 'POST'
    })
};
