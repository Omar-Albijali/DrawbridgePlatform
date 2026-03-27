import type { Notification } from '../types';

const ORDER_UUID_REGEX = /\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\b/gi;

export function getNotificationTitle(notification: Notification): string {
  const maybeTitle = (notification as Notification & { title?: string }).title;
  return maybeTitle && maybeTitle.trim().length > 0 ? maybeTitle : 'Notification';
}

export function shortenOrderIds(text: string): string {
  return text.replace(ORDER_UUID_REGEX, (uuid) => uuid.slice(0, 8));
}

export function notificationDestination(notification: Notification): string {
  const withDeepLink = notification as Notification & { deepLink?: string };
  return withDeepLink.deepLink ?? '/notifications';
}

