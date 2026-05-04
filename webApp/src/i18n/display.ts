import type { TFunction } from 'i18next';
import i18n from './index';

export function enumToken(value: unknown): string {
  if (typeof value === 'string') {
    return value;
  }

  if (typeof value === 'object' && value !== null && 'name' in value) {
    const token = (value as { name?: unknown }).name;
    if (typeof token === 'string') {
      return token;
    }
  }

  return String(value ?? '');
}

export function humanizeToken(token: string): string {
  return token
    .toLowerCase()
    .split('_')
    .filter(Boolean)
    .map((part) => `${part.charAt(0).toUpperCase()}${part.slice(1)}`)
    .join(' ');
}

export function orderStatusLabel(t: TFunction, status: unknown): string {
  const token = enumToken(status);
  if (!token) {
    return '';
  }
  return t(`orders.status.${token}`, { defaultValue: humanizeToken(token) });
}

export function shippingMethodLabel(t: TFunction, method: unknown): string {
  const token = enumToken(method) || 'STANDARD';
  return t(`orders.shippingMethods.${token}`, { defaultValue: humanizeToken(token) });
}

export function notificationTypeLabel(t: TFunction, type: unknown): string {
  const token = enumToken(type).toLowerCase();
  if (token === 'order') return t('navigation.notificationTypes.order');
  if (token === 'stock') return t('navigation.notificationTypes.inventory');
  if (token === 'payment') return t('navigation.notificationTypes.payment');
  return t('navigation.notificationTypes.system');
}

export function supportCategoryLabel(t: TFunction, value: unknown): string {
  const token = enumToken(value) || 'OTHER';
  return t(`support.categories.${token}`, { defaultValue: humanizeToken(token) });
}

export function supportStatusLabel(t: TFunction, value: unknown): string {
  const token = enumToken(value);
  return token ? t(`support.statuses.${token}`, { defaultValue: humanizeToken(token) }) : t('common.unknown');
}

export function dayOfWeekLabel(t: TFunction, value: string | null | undefined): string {
  if (!value) {
    return '';
  }
  return t(`inventory.schedule.days.${value}`, { defaultValue: humanizeToken(value) });
}

type CurrencyFormatOptions = {
  minimumFractionDigits?: number | null;
  maximumFractionDigits?: number | null;
};

function clampFractionDigits(value: number | null | undefined, fallback: number): number {
  if (value == null || !Number.isFinite(value)) {
    return fallback;
  }

  return Math.min(20, Math.max(0, Math.trunc(value)));
}

export function formatCurrency(amount: number | null | undefined, options?: CurrencyFormatOptions): string {
  const language = i18n.resolvedLanguage === 'ar' ? 'ar-SA' : 'en-US';
  const safeAmount = amount == null || !Number.isFinite(amount) ? 0 : amount;
  const minimumFractionDigits = clampFractionDigits(options?.minimumFractionDigits, 0);
  const requestedMaximumFractionDigits = clampFractionDigits(options?.maximumFractionDigits, 2);
  const maximumFractionDigits = Math.max(requestedMaximumFractionDigits, minimumFractionDigits);

  const formattedAmount = new Intl.NumberFormat(language, {
    minimumFractionDigits,
    maximumFractionDigits,
  }).format(safeAmount);
  return i18n.t('common.currency.sarAmount', { amount: formattedAmount });
}

export function formatDate(value: string | number | Date, options?: Intl.DateTimeFormatOptions): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }

  return date.toLocaleDateString(i18n.resolvedLanguage === 'ar' ? 'ar-SA' : undefined, options);
}

export function formatDateTime(value: string | number | Date, options?: Intl.DateTimeFormatOptions): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }

  return date.toLocaleString(i18n.resolvedLanguage === 'ar' ? 'ar-SA' : undefined, options);
}
