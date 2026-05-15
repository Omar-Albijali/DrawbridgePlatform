import {
  NotificationEntityType,
  NotificationEventKey,
  NotificationType,
  type Notification,
} from '../types';
import { fetchApi } from './api';

const ENV_VAPID_PUBLIC_KEY = import.meta.env.VITE_VAPID_PUBLIC_KEY;

export type NotificationChannelValue = 'SYSTEM' | 'SMS' | 'EMAIL' | 'PUSH';
export type NotificationPreferenceKeyValue =
  | 'ORDER_CONFIRMATION'
  | 'SHIPPING_STATUS'
  | 'LOW_STOCK_WARNING'
  | 'AUTO_RESTOCK_CONFIRMATION'
  | 'NEW_WHOLESALERS'
  | 'PAYMENT_STATUS'
  | 'SUPPORT_UPDATES';

export interface NotificationPreference {
  userId: string;
  preferenceKey: NotificationPreferenceKeyValue;
  channel: NotificationChannelValue;
  enabled: boolean;
}

export interface UpsertNotificationPreferenceRequest {
  preferenceKey: NotificationPreferenceKeyValue;
  channel: NotificationChannelValue;
  enabled: boolean;
}

export interface WebPushSubscription {
  id: string;
  userId: string;
  endpoint: string;
  p256dh: string;
  auth: string;
  userAgent?: string;
  createdAt: string;
}

export interface RegisterWebPushSubscriptionRequest {
  userId: string;
  endpoint: string;
  p256dh: string;
  auth: string;
  userAgent?: string;
}

type NotificationApiResponse = Omit<Notification, 'type' | 'eventKey' | 'entityType'> & {
  type: string;
  eventKey: string;
  entityType: string;
};

function normalizeNotification(item: NotificationApiResponse): Notification {
  const type = NotificationType.valueOf(item.type);
  const eventKey = NotificationEventKey.valueOf(item.eventKey);
  const entityType = NotificationEntityType.valueOf(item.entityType);
  return {
    ...item,
    type,
    eventKey,
    entityType,
  } as Notification;
}

function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);
  for (let i = 0; i < rawData.length; i += 1) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}

function normalizePublicKey(value: string | undefined): string | null {
  const trimmed = (value ?? '').trim();
  if (!trimmed) {
    return null;
  }
  if (
    (trimmed.startsWith('"') && trimmed.endsWith('"')) ||
    (trimmed.startsWith("'") && trimmed.endsWith("'"))
  ) {
    const unwrapped = trimmed.slice(1, -1).trim();
    return unwrapped || null;
  }
  return trimmed;
}

export const notificationService = {
  getNotifications: async (recipientId: string) => {
    const data = await fetchApi<NotificationApiResponse[]>(`/notifications/recipient/${recipientId}`);
    return Array.isArray(data) ? data.map(normalizeNotification) : [];
  },

  getUnreadCount: async (recipientId: string): Promise<number> => {
    const response = await fetchApi<{ recipientId: string; count: number }>(
      `/notifications/recipient/${recipientId}/unread-count`,
    );
    return response.count ?? 0;
  },

  markAsRead: (notificationId: string) =>
    fetchApi<Notification>(`/notifications/${notificationId}/read`, {
      method: 'PUT',
    }),

  markAllAsRead: (recipientId: string) =>
    fetchApi<{ recipientId: string; count: number }>(`/notifications/recipient/${recipientId}/read-all`, {
      method: 'PUT',
    }),

  getPreferences: (userId: string) =>
    fetchApi<NotificationPreference[]>(`/notifications/preferences/${userId}`),

  upsertPreference: (userId: string, request: UpsertNotificationPreferenceRequest) =>
    fetchApi<NotificationPreference>(`/notifications/preferences/${userId}`, {
      method: 'PUT',
      body: JSON.stringify(request),
    }),

  getPushSubscriptions: (userId: string) =>
    fetchApi<WebPushSubscription[]>(`/notifications/push-subscriptions/${userId}`),

  registerPushSubscription: (request: RegisterWebPushSubscriptionRequest) =>
    fetchApi<WebPushSubscription>('/notifications/push-subscriptions', {
      method: 'POST',
      body: JSON.stringify(request),
    }),

  unregisterPushSubscription: (endpoint: string) =>
    fetchApi<void>(`/notifications/push-subscriptions?endpoint=${encodeURIComponent(endpoint)}`, {
      method: 'DELETE',
    }),

  async subscribeBrowserPush(userId: string): Promise<boolean> {
    if (!window.isSecureContext || !('Notification' in window)) {
      return false;
    }

    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
      return false;
    }

    const vapidPublicKey = normalizePublicKey(ENV_VAPID_PUBLIC_KEY);
    if (!vapidPublicKey) {
      console.warn(
        'VITE_VAPID_PUBLIC_KEY is not configured; set it in your environment configuration to enable browser push subscription.',
      );
      return false;
    }

    const permission = await Notification.requestPermission();
    if (permission !== 'granted') {
      return false;
    }

    const serviceWorkerUrl = `${import.meta.env.BASE_URL}notification-sw.js`;
    const registration = await navigator.serviceWorker.register(serviceWorkerUrl);
    await navigator.serviceWorker.ready;
    const existing = await registration.pushManager.getSubscription();
    const subscription =
      existing ??
      (await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(vapidPublicKey) as BufferSource,
      }));

    const raw = subscription.toJSON();
    if (!raw.endpoint || !raw.keys?.p256dh || !raw.keys.auth) {
      return false;
    }

    await this.registerPushSubscription({
      userId,
      endpoint: raw.endpoint,
      p256dh: raw.keys.p256dh,
      auth: raw.keys.auth,
      userAgent: navigator.userAgent,
    });

    return true;
  },

  async unsubscribeBrowserPush(): Promise<boolean> {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
      return false;
    }

    const registration = await navigator.serviceWorker.ready;
    const subscription = await registration.pushManager.getSubscription();
    if (!subscription) {
      return true;
    }

    const endpoint = subscription.endpoint;
    await subscription.unsubscribe();

    if (endpoint) {
      await this.unregisterPushSubscription(endpoint);
    }

    return true;
  },

  buildPreferenceRequest(
    preferenceKey: NotificationPreferenceKeyValue,
    channel: NotificationChannelValue,
    enabled: boolean,
  ): UpsertNotificationPreferenceRequest {
    return { preferenceKey, channel, enabled };
  },
};
