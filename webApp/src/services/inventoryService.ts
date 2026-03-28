import { fetchApi } from './api';
import { InventoryItem, AutoOrderConfig, CreateInventoryItemRequest, UpdateAutoOrderConfigRequest } from '../types';
import {ScheduleType} from "shared";


export const inventoryService = {
    getAll: () => fetchApi<InventoryItem[]>('/inventory'),

    getById: (id: string) => fetchApi<InventoryItem>(`/inventory/${id}`),

    getByRetailer: (retailerId: string) => fetchApi<InventoryItem[]>(`/inventory/retailer/${retailerId}`),

    getLowStock: (threshold: number = 10) => fetchApi<InventoryItem[]>(`/inventory/low-stock?threshold=${threshold}`),

    create: (request: CreateInventoryItemRequest) => {
        // Cast as unknown because request is a plain object here, but type is a class
        const body = request as unknown as CreateInventoryItemRequest;
        return fetchApi<InventoryItem>('/inventory', {
            method: 'POST',
            body: JSON.stringify(body)
        });
    },

    update: (id: string, request: CreateInventoryItemRequest) => {
        const body = request as unknown as CreateInventoryItemRequest;
        return fetchApi<InventoryItem>(`/inventory/${id}`, {
            method: 'PUT',
            body: JSON.stringify(body)
        });
    },

    updateQuantity: (id: string, quantity: number) => fetchApi<InventoryItem>(`/inventory/${id}/quantity?quantity=${quantity}`, {
        method: 'PATCH'
    }),

    delete: (id: string) => fetchApi<void>(`/inventory/${id}`, {
        method: 'DELETE'
    }),

    // Auto-Order Config
    toggleAutoOrder: (id: string, enabled: boolean) => fetchApi<AutoOrderConfig>(`/inventory/auto-order/${id}/toggle?enabled=${enabled}`, {
        method: 'PATCH'
    }),

    saveAutoOrderConfig: (id: string, hasConfig: boolean, config: Partial<AutoOrderConfig> & { minThreshold: number, reorderQuantity: number }) => {
        // Construct UpdateAutoOrderConfigRequest
        const request = {
            enabled: hasConfig,
            minThreshold: config.minThreshold ?? 0,
            reorderQuantity: config.reorderQuantity ?? 0,
            scheduleType: config.scheduleType?.name || ScheduleType.THRESHOLD_BASED.name,
            intervalDays: config.intervalDays ?? null,
            dayOfWeek: config.dayOfWeek ?? null,
            dayOfMonth: config.dayOfMonth ?? null
        } as unknown as UpdateAutoOrderConfigRequest;

        return fetchApi<AutoOrderConfig>(`/inventory/auto-order/${id}`, {
            method: 'PUT',
            body: JSON.stringify(request)
        });
    }
};
