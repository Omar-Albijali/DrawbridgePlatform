import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { AlertTriangle, Bell, CreditCard, Mail, MessageSquare, Package } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../contexts/AuthContext';
import {
  notificationService,
  type NotificationChannelValue,
  type NotificationPreferenceKeyValue,
} from '../../services/notificationService';
import { UserRole } from '../../types';

interface ChannelChipProps {
  enabled: boolean;
  loading: boolean;
  onToggle: () => void;
  channel: NotificationChannelValue;
}

function channelLabel(channel: NotificationChannelValue, t: ReturnType<typeof useTranslation>['t']): string {
  if (channel === 'EMAIL') return t('notifications.email');
  if (channel === 'SMS') return t('notifications.sms');
  if (channel === 'PUSH') return t('notifications.push');
  return t('notifications.system');
}

function channelIcon(channel: NotificationChannelValue): ReactNode {
  if (channel === 'EMAIL') return <Mail className="h-3.5 w-3.5" />;
  if (channel === 'SMS') return <MessageSquare className="h-3.5 w-3.5" />;
  if (channel === 'PUSH') return <Bell className="h-3.5 w-3.5" />;
  return <Bell className="h-3.5 w-3.5" />;
}

function ChannelChip({ enabled, loading, onToggle, channel }: ChannelChipProps): JSX.Element {
  const { t } = useTranslation();
  const label = channelLabel(channel, t);
  const activeColorClass =
    channel === 'EMAIL'
      ? 'border-blue-300 bg-blue-50 text-blue-700 dark:border-blue-400/40 dark:bg-blue-500/15 dark:text-blue-200'
      : channel === 'SMS'
        ? 'border-orange-300 bg-orange-50 text-orange-700 dark:border-orange-400/40 dark:bg-orange-500/15 dark:text-orange-200'
        : 'border-emerald-300 bg-emerald-50 text-emerald-700 dark:border-emerald-400/40 dark:bg-emerald-500/15 dark:text-emerald-200';

  return (
    <button
      type="button"
      disabled={loading}
      onClick={onToggle}
      className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-semibold transition ${
        enabled
          ? activeColorClass
          : 'border-slate-200 bg-white text-slate-600 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300'
      } disabled:cursor-not-allowed disabled:opacity-60`}
      aria-pressed={enabled}
      aria-label={enabled ? t('notifications.disableChannel', { channel: label }) : t('notifications.enableChannel', { channel: label })}
    >
      {channelIcon(channel)}
      <span>{enabled ? '✓' : ''}</span>
      <span>{label}</span>
    </button>
  );
}

interface NotificationSection {
  id: string;
  title: string;
  icon: ReactNode;
  sectionClass: string;
  iconWrapClass: string;
  items: {
    id: string;
    labelKey: string;
    descriptionKey: string;
    preferenceKey: NotificationPreferenceKeyValue;
    channels: NotificationChannelValue[];
  }[];
}

export default function Notifications(): JSX.Element {
  const { t } = useTranslation();
  const { user } = useAuth();
  const isRetailer = user?.role === UserRole.RETAILER;
  const [status, setStatus] = useState<string>('');
  const [isSaving, setIsSaving] = useState<Record<string, boolean>>({});
  const [pushEnabled, setPushEnabled] = useState(false);
  const [isPushLoading, setIsPushLoading] = useState(false);

  const [preferences, setPreferences] = useState<Record<string, boolean>>({});

  const preferenceItems = useMemo(
    () => [
      {
        id: 'orderConfirmation',
        labelKey: 'notifications.preferences.orderConfirmation.label',
        descriptionKey: 'notifications.preferences.orderConfirmation.description',
        preferenceKey: 'ORDER_CONFIRMATION' as NotificationPreferenceKeyValue,
        channels: ['EMAIL', 'SMS', 'PUSH'] as NotificationChannelValue[],
      },
      {
        id: 'shippingStatus',
        labelKey: 'notifications.preferences.shippingStatus.label',
        descriptionKey: 'notifications.preferences.shippingStatus.description',
        preferenceKey: 'SHIPPING_STATUS' as NotificationPreferenceKeyValue,
        channels: ['EMAIL', 'SMS', 'PUSH'] as NotificationChannelValue[],
      },
      {
        id: 'lowStockWarning',
        labelKey: 'notifications.preferences.lowStockWarning.label',
        descriptionKey: 'notifications.preferences.lowStockWarning.description',
        preferenceKey: 'LOW_STOCK_WARNING' as NotificationPreferenceKeyValue,
        channels: ['EMAIL', 'SMS', 'PUSH'] as NotificationChannelValue[],
      },
      {
        id: 'autoRestockConfirmation',
        labelKey: 'notifications.preferences.autoRestockConfirmation.label',
        descriptionKey: 'notifications.preferences.autoRestockConfirmation.description',
        preferenceKey: 'AUTO_RESTOCK_CONFIRMATION' as NotificationPreferenceKeyValue,
        channels: ['EMAIL', 'SMS', 'PUSH'] as NotificationChannelValue[],
      },
      {
        id: 'paymentStatus',
        labelKey: 'notifications.preferences.paymentStatus.label',
        descriptionKey: 'notifications.preferences.paymentStatus.description',
        preferenceKey: 'PAYMENT_STATUS' as NotificationPreferenceKeyValue,
        channels: ['EMAIL', 'SMS', 'PUSH'] as NotificationChannelValue[],
      },
    ],
    [],
  );

  const preferenceDefaults = useMemo(() => {
    const defaults: Record<string, boolean> = {};
    preferenceItems.forEach((item) => {
      item.channels.forEach((channel) => {
        defaults[`${item.preferenceKey}:${channel}`] = channel !== 'SMS';
      });
    });
    return defaults;
  }, [preferenceItems]);

  const handleToggle = async (
    preferenceKey: NotificationPreferenceKeyValue,
    channelValue: NotificationChannelValue,
    enabled: boolean,
  ): Promise<void> => {
    if (!user?.id) {
      return;
    }

    const stateKey = `${preferenceKey}:${channelValue}`;

    const previous = preferences[stateKey] ?? false;
    setPreferences((prev) => ({ ...prev, [stateKey]: enabled }));
    setIsSaving((prev) => ({ ...prev, [stateKey]: true }));

    try {
      await notificationService.upsertPreference(
        user.id,
        notificationService.buildPreferenceRequest(preferenceKey, channelValue, enabled),
      );
      setStatus(t('notifications.saved'));
    } catch (_error) {
      setPreferences((prev) => ({ ...prev, [stateKey]: previous }));
      setStatus(t('notifications.saveFailed'));
    } finally {
      setIsSaving((prev) => ({ ...prev, [stateKey]: false }));
      window.setTimeout(() => setStatus(''), 2200);
    }
  };

  useEffect(() => {
    const load = async (): Promise<void> => {
      if (!user?.id) {
        return;
      }

      try {
        const [savedPreferences, subscriptions] = await Promise.all([
          notificationService.getPreferences(user.id),
          notificationService.getPushSubscriptions(user.id),
        ]);

        const nextPrefs: Record<string, boolean> = { ...preferenceDefaults };

        savedPreferences.forEach((item) => {
          const key = `${item.preferenceKey}:${item.channel}`;
          if (Object.prototype.hasOwnProperty.call(nextPrefs, key)) {
            nextPrefs[key] = item.enabled;
          }
        });

        setPreferences(nextPrefs);
        setPushEnabled(subscriptions.length > 0);
      } catch (_error) {
      setStatus(t('notifications.loadFailed'));
      }
    };

    void load();
  }, [preferenceDefaults, user?.id]);

  const handlePushToggle = async (): Promise<void> => {
    if (!user?.id) return;
    setIsPushLoading(true);
    try {
      if (!pushEnabled) {
        const subscribed = await notificationService.subscribeBrowserPush(user.id);
        setPushEnabled(subscribed);
        setStatus(subscribed ? t('notifications.pushEnabled') : t('notifications.pushEnableFailed'));
      } else {
        const unsubscribed = await notificationService.unsubscribeBrowserPush();
        setPushEnabled(!unsubscribed ? pushEnabled : false);
        setStatus(unsubscribed ? t('notifications.pushDisabled') : t('notifications.pushDisableFailed'));
      }
    } catch (_error) {
      setStatus(pushEnabled ? t('notifications.pushDisableFailed') : t('notifications.pushEnableFailed'));
    } finally {
      setIsPushLoading(false);
      window.setTimeout(() => setStatus(''), 2200);
    }
  };

  const sections: NotificationSection[] = [
    {
      id: 'order',
      title: t('notifications.sections.order'),
      icon: <Package className="w-5 h-5 text-primary-600" />,
      sectionClass: 'border-primary-100 bg-gradient-to-r from-primary-50 to-blue-50/80 dark:border-white/10 dark:from-slate-900 dark:to-slate-900/80',
      iconWrapClass: 'bg-primary-100 dark:bg-primary-500/20',
      items: [
        preferenceItems[0],
        preferenceItems[1],
      ],
    },
    ...(isRetailer
      ? [
          {
            id: 'inventory',
            title: t('notifications.sections.inventory'),
            icon: <AlertTriangle className="w-5 h-5 text-amber-500" />,
            sectionClass: 'border-amber-100 bg-gradient-to-r from-amber-50 to-orange-50/80 dark:border-white/10 dark:from-slate-900 dark:to-slate-900/80',
            iconWrapClass: 'bg-amber-100 dark:bg-amber-500/20',
            items: [
              preferenceItems[2],
              preferenceItems[3],
            ],
          },
        ]
      : []),
    {
      id: 'payments',
      title: t('notifications.sections.payments'),
      icon: <CreditCard className="w-5 h-5 text-emerald-600" />,
      sectionClass: 'border-emerald-100 bg-gradient-to-r from-emerald-50 to-lime-50/80 dark:border-white/10 dark:from-slate-900 dark:to-slate-900/80',
      iconWrapClass: 'bg-emerald-100 dark:bg-emerald-500/20',
      items: [
        preferenceItems[4],
      ],
    },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-navy-800 dark:text-slate-100">{t('notifications.preferencesTitle')}</h1>
        <p className="mt-1 text-navy-500 dark:text-slate-300">{t('notifications.preferencesDescription')}</p>
        {status ? <p className="mt-2 text-sm font-medium text-primary-700 dark:text-primary-300">{status}</p> : null}
      </div>

      <div className="card border border-primary-100 bg-gradient-to-r from-primary-50 to-blue-50 dark:border-white/10 dark:from-slate-900 dark:to-slate-800">
        <div className="flex items-center gap-4">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-primary-100 dark:bg-primary-500/20">
            <Bell className="w-6 h-6 text-primary-600" />
          </div>
          <div>
            <h3 className="font-semibold text-navy-800 dark:text-slate-100">{t('notifications.channels')}</h3>
            <div className="flex items-center gap-4 mt-1">
              <span className="flex items-center gap-1.5 text-sm text-navy-600 dark:text-slate-300">
                <Mail className="w-4 h-4" /> {t('notifications.email')}
              </span>
              <span className="flex items-center gap-1.5 text-sm text-navy-600 dark:text-slate-300">
                <MessageSquare className="w-4 h-4" /> {t('notifications.sms')}
              </span>
              <span className="flex items-center gap-1.5 text-sm text-navy-600 dark:text-slate-300">
                <Bell className="w-4 h-4" /> {t('notifications.push')}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-semibold text-navy-800 dark:text-slate-100">{t('notifications.browserPush')}</h3>
            <p className="mt-1 text-sm text-navy-500 dark:text-slate-300">
              {t('notifications.browserPushDescription')}
            </p>
          </div>
          <button
            type="button"
            disabled={isPushLoading}
            onClick={() => void handlePushToggle()}
            className={`rounded-lg px-3 py-2 text-sm font-semibold transition ${
              pushEnabled
                ? 'bg-slate-200 text-slate-800 hover:bg-slate-300'
                : 'bg-primary-600 text-white hover:bg-primary-700'
            } disabled:cursor-not-allowed disabled:opacity-60`}
          >
            {isPushLoading ? t('notifications.saving') : pushEnabled ? t('notifications.disablePush') : t('notifications.enablePush')}
          </button>
        </div>
      </div>

      {sections.map((section) => (
        <div key={section.id} className={`card border ${section.sectionClass}`}>
          <div className="mb-4 flex items-center gap-2 border-b border-gray-200 pb-4 dark:border-white/10">
            <div className={`flex h-9 w-9 items-center justify-center rounded-full ${section.iconWrapClass}`}>
              {section.icon}
            </div>
            <h3 className="text-lg font-semibold text-navy-800 dark:text-slate-100">{section.title}</h3>
          </div>
          <div>
            {section.items.map((item) => (
              <div key={item.id} className="border-b border-gray-100 py-4 last:border-0 dark:border-white/10">
                <h4 className="font-medium text-navy-800 dark:text-slate-100">{t(item.labelKey)}</h4>
                <p className="mt-0.5 text-sm text-navy-500 dark:text-slate-300">{t(item.descriptionKey)}</p>
                <div className="mt-3 flex flex-wrap gap-2">
                  {item.channels.map((channel) => {
                    const key = `${item.preferenceKey}:${channel}`;
                    return (
                      <ChannelChip
                        key={key}
                        channel={channel}
                        enabled={preferences[key] ?? false}
                        loading={isSaving[key] ?? false}
                        onToggle={() =>
                          void handleToggle(item.preferenceKey, channel, !(preferences[key] ?? false))
                        }
                      />
                    );
                  })}
                </div>
                {item.channels.some((channel) => isSaving[`${item.preferenceKey}:${channel}`]) ? (
                  <p className="pb-1 pt-2 text-xs text-navy-500">{t('notifications.saving')}</p>
                ) : null}
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
