import { fetchApi } from './api';
import type {
  PosIntegrationApiKeyRotate,
  PosIntegrationConfig,
  PosIntegrationConfigUpdate,
  PosIntegrationEventLog,
} from '../types';

export type PosIntegrationStatus = PosIntegrationConfigUpdate['status'];

export const posIntegrationService = {
  getConfig(): Promise<PosIntegrationConfig> {
    return fetchApi<PosIntegrationConfig>('/retailer/pos-integration');
  },

  updateConfig(body: PosIntegrationConfigUpdate): Promise<PosIntegrationConfig> {
    return fetchApi<PosIntegrationConfig>('/retailer/pos-integration', {
      method: 'PUT',
      body: JSON.stringify(body),
    });
  },

  rotateApiKey(): Promise<PosIntegrationApiKeyRotate> {
    return fetchApi<PosIntegrationApiKeyRotate>('/retailer/pos-integration/api-key/rotate', {
      method: 'POST',
    });
  },

  getEventLogs(limit = 100): Promise<PosIntegrationEventLog[]> {
    const params = new URLSearchParams({ limit: String(limit) });
    return fetchApi<PosIntegrationEventLog[]>(`/retailer/pos-integration/events?${params.toString()}`);
  },
};
