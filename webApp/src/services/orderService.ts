import { fetchApi } from './api';
import { Order, OrderGroup, OrderStatus, UpdateOrderTrackingRequest } from '../types';


export const orderService = {
    getAll: () => fetchApi<Order[]>('/orders'),

    getById: (id: string) => fetchApi<Order>(`/orders/${id}`),

    getByRetailer: (retailerId: string) => fetchApi<Order[]>(`/orders/retailer/${retailerId}`),

    getByWholesaler: (wholesalerId: string) => fetchApi<Order[]>(`/orders/wholesaler/${wholesalerId}`),

    updateStatus: (id: string, status: OrderStatus | any) => {
        const statusName = status?.name || status;
        return fetchApi<Order>(`/orders/${id}/status?status=${statusName}`, {
            method: 'PATCH'
        });
    },

    confirmDelivery: (id: string) => fetchApi<Order>(`/orders/${id}/confirm-delivery`, {
        method: 'PATCH'
    }),

    updateTracking: (id: string, request: UpdateOrderTrackingRequest) => fetchApi<Order>(`/orders/${id}/tracking`, {
        method: 'PATCH',
        body: JSON.stringify(request as unknown as UpdateOrderTrackingRequest)
    }),

    cancel: (id: string) => fetchApi<Order>(`/orders/${id}`, {
        method: 'DELETE'
    }),

    // Order Groups
    getGroupsByRetailer: (retailerId: string) => fetchApi<OrderGroup[]>(`/orders/groups/retailer/${retailerId}`),

    getGroupById: (id: string) => fetchApi<OrderGroup>(`/orders/groups/${id}`)
};
