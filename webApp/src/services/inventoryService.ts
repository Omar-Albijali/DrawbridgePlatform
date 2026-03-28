import { fetchApi } from './api';
import { InventoryItem, AutoOrderConfig, CreateInventoryItemRequest, UpdateAutoOrderConfigRequest } from '../types';
import { InventoryStatus, ScheduleType } from 'shared';

const toScheduleType = (value: any): ScheduleType => {
    const enumName = value?.name ?? value;

    if (enumName == ScheduleType.DAILY || enumName == ScheduleType.DAILY.name) {
        return ScheduleType.DAILY;
    }

    if (enumName == ScheduleType.WEEKLY || enumName == ScheduleType.WEEKLY.name) {
        return ScheduleType.WEEKLY;
    }

    if (enumName == ScheduleType.MONTHLY || enumName == ScheduleType.MONTHLY.name) {
        return ScheduleType.MONTHLY;
    }

    if (enumName == ScheduleType.INTERVAL_DAYS || enumName == ScheduleType.INTERVAL_DAYS.name) {
        return ScheduleType.INTERVAL_DAYS;
    }

    return ScheduleType.THRESHOLD_BASED;
};

const toInventoryStatus = (value: any): InventoryStatus => {
    const enumName = value?.name ?? value;

    if (enumName == InventoryStatus.LOW_STOCK || enumName == InventoryStatus.LOW_STOCK.name) {
        return InventoryStatus.LOW_STOCK;
    }

    if (enumName == InventoryStatus.OUT_OF_STOCK || enumName == InventoryStatus.OUT_OF_STOCK.name) {
        return InventoryStatus.OUT_OF_STOCK;
    }

    return InventoryStatus.OPTIMAL;
};

const normalizeAutoOrderConfig = (config: AutoOrderConfig | null | undefined): AutoOrderConfig | null | undefined => {
    if (!config) {
        return config;
    }

    return {
        ...config,
        scheduleType: toScheduleType((config as any).scheduleType)
    } as AutoOrderConfig;
};

const normalizeInventoryItem = (item: InventoryItem): InventoryItem => ({
    ...(item as any),
    status: toInventoryStatus((item as any).status),
    autoOrderConfig: normalizeAutoOrderConfig((item as any).autoOrderConfig)
} as InventoryItem);


export const inventoryService = {
    getAll: () => fetchApi<InventoryItem[]>('/inventory').then((items) => items.map(normalizeInventoryItem)),

    getById: (id: string) => fetchApi<InventoryItem>(`/inventory/${id}`).then(normalizeInventoryItem),

    getByRetailer: (retailerId: string) => fetchApi<InventoryItem[]>(`/inventory/retailer/${retailerId}`).then((items) => items.map(normalizeInventoryItem)),

    getLowStock: (threshold: number = 10) => fetchApi<InventoryItem[]>(`/inventory/low-stock?threshold=${threshold}`).then((items) => items.map(normalizeInventoryItem)),

    create: (request: CreateInventoryItemRequest) => {
        // Cast as unknown because request is a plain object here, but type is a class
        const body = request as unknown as CreateInventoryItemRequest;
        return fetchApi<InventoryItem>('/inventory', {
            method: 'POST',
            body: JSON.stringify(body)
        }).then(normalizeInventoryItem);
    },

    update: (id: string, request: CreateInventoryItemRequest) => {
        const body = request as unknown as CreateInventoryItemRequest;
        return fetchApi<InventoryItem>(`/inventory/${id}`, {
            method: 'PUT',
            body: JSON.stringify(body)
        }).then(normalizeInventoryItem);
    },

    updateQuantity: (id: string, quantity: number) => fetchApi<InventoryItem>(`/inventory/${id}/quantity?quantity=${quantity}`, {
        method: 'PATCH'
    }).then(normalizeInventoryItem),

    delete: (id: string) => fetchApi<void>(`/inventory/${id}`, {
        method: 'DELETE'
    }),

    // Auto-Order Config
    toggleAutoOrder: (id: string, enabled: boolean) => fetchApi<AutoOrderConfig>(`/inventory/auto-order/${id}/toggle?enabled=${enabled}`, {
        method: 'PATCH'
    }).then((config) => normalizeAutoOrderConfig(config) as AutoOrderConfig),

    saveAutoOrderConfig: (id: string, hasConfig: boolean, config: Partial<AutoOrderConfig> & { minThreshold: number, reorderQuantity: number }) => {
        // Construct UpdateAutoOrderConfigRequest
        const request = {
            enabled: hasConfig,
            minThreshold: config.minThreshold ?? 0,
            reorderQuantity: config.reorderQuantity ?? 0,
            scheduleType: toScheduleType(config.scheduleType).name,
            intervalDays: config.intervalDays ?? null,
            dayOfWeek: config.dayOfWeek ?? null,
            dayOfMonth: config.dayOfMonth ?? null
        } as unknown as UpdateAutoOrderConfigRequest;

        return fetchApi<AutoOrderConfig>(`/inventory/auto-order/${id}`, {
            method: 'PUT',
            body: JSON.stringify(request)
        }).then((savedConfig) => normalizeAutoOrderConfig(savedConfig) as AutoOrderConfig);
    }
};
