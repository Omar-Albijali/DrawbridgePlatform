import { fetchApi } from './api';
import { Product, Category, CreateProductRequest } from '../types';


export const productService = {
    getAll: () => fetchApi<Product[]>('/products'),

    getById: (id: string) => fetchApi<Product>(`/products/${id}`),

    search: (query: string) => fetchApi<Product[]>(`/products/search?q=${encodeURIComponent(query)}`),

    getByCategory: (categoryId: string) => fetchApi<Product[]>(`/products/category/${categoryId}`),

    getByWholesaler: (wholesalerId: string) => fetchApi<Product[]>(`/products/wholesaler/${wholesalerId}`),

    create: (request: CreateProductRequest) => {
        const body = request as unknown as CreateProductRequest;
        return fetchApi<Product>('/products', {
            method: 'POST',
            body: JSON.stringify(body)
        });
    },

    update: (id: string, product: Partial<Product>) => fetchApi<Product>(`/products/${id}`, {
        method: 'PUT',
        body: JSON.stringify(product)
    }),

    delete: (id: string) => fetchApi<void>(`/products/${id}`, {
        method: 'DELETE'
    }),

    // Categories
    getCategories: () => fetchApi<Category[]>('/products/categories'),

    getCategoryById: (id: string) => fetchApi<Category>(`/products/categories/${id}`)
};
