import { fetchApi } from './api';
import { ImageUploadResponse, ProductImageResponse } from '../types';
import { Product, Category, CreateProductRequest, PaginatedResponse } from '../types';

interface MarketplaceProductQuery {
    page?: number;
    size?: number;
    search?: string;
    category?: string[];
    brand?: string[];
    minPrice?: number;
    maxPrice?: number;
    sort?: string;
}

function buildMarketplaceQuery(params: MarketplaceProductQuery = {}): string {
    const searchParams = new URLSearchParams();

    searchParams.set('page', String(params.page ?? 0));
    searchParams.set('size', String(params.size ?? 12));

    if (params.search?.trim()) {
        searchParams.set('search', params.search.trim());
    }

    params.category?.filter((value) => value.trim().length > 0).forEach((value) => {
        searchParams.append('category', value);
    });

    params.brand?.filter((value) => value.trim().length > 0).forEach((value) => {
        searchParams.append('brand', value);
    });

    if (params.sort?.trim()) {
        searchParams.set('sort', params.sort.trim());
    }

    if (typeof params.minPrice === 'number') {
        searchParams.set('minPrice', String(params.minPrice));
    }

    if (typeof params.maxPrice === 'number') {
        searchParams.set('maxPrice', String(params.maxPrice));
    }

    const queryString = searchParams.toString();
    return queryString.length > 0 ? `?${queryString}` : '';
}

export const productService = {
    getAll: () => fetchApi<Product[]>('/products/all'),

    getMarketplacePage: (params: MarketplaceProductQuery = {}, signal?: AbortSignal) =>
        fetchApi<PaginatedResponse<Product>>(`/products${buildMarketplaceQuery(params)}`, signal ? { signal } : undefined),

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

    // Images
    uploadImage: async (productId: string, file: File, altText: string = '', sortIndex?: number): Promise<ImageUploadResponse> => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('altText', altText);
        if (sortIndex !== undefined) {
            formData.append('sortIndex', String(sortIndex));
        }

        return fetchApi<ImageUploadResponse>(`/products/${productId}/images`, {
            method: 'POST',
            body: formData
        });
    },

    reorderImages: (productId: string, orderedImageIds: string[]) => fetchApi<void>(`/products/${productId}/images/reorder`, {
        method: 'PUT',
        body: JSON.stringify(orderedImageIds)
    }),

    getImages: (productId: string) => fetchApi<ProductImageResponse[]>(`/products/${productId}/images`),

    deleteImage: (imageId: string) => fetchApi<void>(`/images/${imageId}`, {
        method: 'DELETE'
    }),

    // Published
    togglePublished: (productId: string) => fetchApi<Product>(`/products/${productId}/toggle-published`, {
        method: 'PATCH'
    }),

    // Categories
    getCategories: () => fetchApi<Category[]>('/products/categories'),

    getCategoryById: (id: string) => fetchApi<Category>(`/products/categories/${id}`),

    getBrands: () => fetchApi<string[]>('/products/brands')
};
