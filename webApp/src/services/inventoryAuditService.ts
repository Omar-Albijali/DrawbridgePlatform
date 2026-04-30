import { fetchApi } from './api';

export type InventoryStockTargetType = 'PRODUCT_CATALOG' | 'RETAILER_INVENTORY';
export type InventoryAuditChangeType = 'INCREASE' | 'DECREASE' | 'UPDATE';
export type InventoryAuditSourceType = 'MANUAL' | 'ORDER' | 'RESTOCK' | 'POS' | 'SYSTEM';

export interface InventoryAuditLog {
  id: string;
  productId: string;
  inventoryItemId: string | null;
  stockTargetType: InventoryStockTargetType;
  changeType: InventoryAuditChangeType;
  sourceType: InventoryAuditSourceType;
  sourceId: string | null;
  quantityBefore: number;
  quantityAfter: number;
  changeAmount: number;
  changedBy: string;
  reason: string | null;
  createdAt: string;
}

export interface InventoryAuditLogPage {
  items: InventoryAuditLog[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface InventoryAuditLogQuery {
  productId?: string;
  inventoryItemId?: string;
  stockTargetType?: InventoryStockTargetType;
  sourceType?: InventoryAuditSourceType;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export const inventoryAuditService = {
  getLogs: (query: InventoryAuditLogQuery): Promise<InventoryAuditLogPage> => {
    const params = new URLSearchParams();

    if (query.productId) params.set('productId', query.productId);
    if (query.inventoryItemId) params.set('inventoryItemId', query.inventoryItemId);
    if (query.stockTargetType) params.set('stockTargetType', query.stockTargetType);
    if (query.sourceType) params.set('sourceType', query.sourceType);
    if (query.from) params.set('from', query.from);
    if (query.to) params.set('to', query.to);
    params.set('page', String(query.page ?? 0));
    params.set('size', String(query.size ?? 20));

    return fetchApi<InventoryAuditLogPage>(`/inventory/logs?${params.toString()}`);
  },
};
