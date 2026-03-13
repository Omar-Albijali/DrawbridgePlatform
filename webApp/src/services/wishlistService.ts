import { fetchApi } from './api';

export interface WishlistItemDto {
  id: string;
  userId: string;
  productId: string;
  productName: string;
  productPrice: number;
  productImage: string;
  createdAt: string;
}

export const wishlistService = {
  getByUser: (userId: string) => fetchApi<WishlistItemDto[]>(`/wishlist/${userId}`),

  add: (userId: string, productId: string) => fetchApi<WishlistItemDto>(`/wishlist/${userId}/${productId}`, {
    method: 'POST',
  }),

  remove: (userId: string, productId: string) =>
    fetchApi<void>(`/wishlist/${userId}/${productId}`, {
      method: 'DELETE',
    }),
};
