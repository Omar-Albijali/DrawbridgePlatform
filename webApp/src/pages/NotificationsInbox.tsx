import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AlertTriangle, Bell, CheckCheck, CreditCard, Package, Shield } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import PageShell from '../components/PageShell';
import { useAuth } from '../contexts/AuthContext';
import { notificationService } from '../services/notificationService';
import {
  getNotificationTitle,
  notificationDestination,
  shortenOrderIds,
} from '../utils/notificationHelpers';
import { notificationTypeLabel } from '../i18n/display';
import { NotificationType, type Notification } from '../types';

function typeStyles(type: NotificationType): {
  dot: string;
  icon: JSX.Element;
  card: string;
  iconWrap: string;
  chip: string;
} {
  if (type === NotificationType.ORDER) {
    return {
      dot: 'bg-primary-500',
      icon: <Package className="h-4 w-4 text-primary-700 dark:text-primary-300" />,
      card: 'border-primary-200 bg-gradient-to-r from-primary-50 to-blue-50/70 dark:border-primary-800/60 dark:from-primary-900/30 dark:to-blue-900/20',
      iconWrap: 'bg-primary-100 ring-1 ring-primary-200 dark:bg-primary-900/35 dark:ring-primary-800/70',
      chip: 'bg-primary-100 text-primary-700 dark:bg-primary-900/40 dark:text-primary-300',
    };
  }
  if (type === NotificationType.STOCK) {
    return {
      dot: 'bg-amber-500',
      icon: <AlertTriangle className="h-4 w-4 text-amber-700 dark:text-amber-300" />,
      card: 'border-amber-200 bg-gradient-to-r from-amber-50 to-orange-50/70 dark:border-amber-800/60 dark:from-amber-900/25 dark:to-orange-900/20',
      iconWrap: 'bg-amber-100 ring-1 ring-amber-200 dark:bg-amber-900/30 dark:ring-amber-800/70',
      chip: 'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300',
    };
  }
  if (type === NotificationType.PAYMENT) {
    return {
      dot: 'bg-emerald-500',
      icon: <CreditCard className="h-4 w-4 text-emerald-700 dark:text-emerald-300" />,
      card: 'border-emerald-200 bg-gradient-to-r from-emerald-50 to-lime-50/70 dark:border-emerald-800/60 dark:from-emerald-900/25 dark:to-lime-900/20',
      iconWrap: 'bg-emerald-100 ring-1 ring-emerald-200 dark:bg-emerald-900/30 dark:ring-emerald-800/70',
      chip: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300',
    };
  }
  return {
    dot: 'bg-slate-400',
    icon: <Shield className="h-4 w-4 text-slate-700 dark:text-slate-300" />,
    card: 'border-slate-200 bg-gradient-to-r from-slate-50 to-slate-100/70 dark:border-slate-700 dark:from-slate-800/70 dark:to-slate-900/70',
    iconWrap: 'bg-slate-100 ring-1 ring-slate-200 dark:bg-slate-800 dark:ring-slate-700',
    chip: 'bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-200',
  };
}


export default function NotificationsInbox(): JSX.Element {
  const { t } = useTranslation();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const load = async (): Promise<void> => {
      if (!user?.id) {
        setIsLoading(false);
        return;
      }

      try {
        const data = await notificationService.getNotifications(user.id);
        setNotifications(Array.isArray(data) ? data : []);
      } finally {
        setIsLoading(false);
      }
    };

    void load();
  }, [user?.id]);

  const markAll = async (): Promise<void> => {
    if (!user?.id) return;
    await notificationService.markAllAsRead(user.id);
    setNotifications((prev) => prev.map((n) => ({ ...(n as object), read: true } as Notification)));
  };

  const markOne = async (notification: Notification): Promise<void> => {
    if (!notification.read) {
      await notificationService.markAsRead(notification.id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === notification.id ? ({ ...(n as object), read: true } as Notification) : n)),
      );
    }
    navigate(notificationDestination(notification));
  };

  return (
    <PageShell title={t('notifications.title')} description={t('notifications.description')}>
      <div className="card space-y-4 border border-slate-200 bg-gradient-to-b from-white to-slate-50/50 dark:border-white/10 dark:from-slate-900 dark:to-slate-900/60">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Bell className="h-5 w-5 text-primary-600" />
            <p className="font-semibold text-slate-900 dark:text-slate-100">{t('notifications.inbox')}</p>
          </div>
          <button
            type="button"
            className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-sm font-semibold text-slate-700 hover:border-primary-300 hover:text-primary-700 dark:border-slate-700 dark:text-slate-200 dark:hover:border-primary-600 dark:hover:text-primary-300"
            onClick={() => void markAll()}
          >
            <CheckCheck className="h-4 w-4" />
            {t('navigation.markAllRead')}
          </button>
        </div>

        {isLoading ? <p className="text-sm text-slate-500 dark:text-slate-400">{t('notifications.loading')}</p> : null}

        {!isLoading && notifications.length === 0 ? (
          <div className="rounded-lg border border-dashed border-slate-300 p-8 text-center text-slate-500 dark:border-slate-700 dark:text-slate-400">
            {t('notifications.none')}
            <div className="mt-2">
              <Link className="text-primary-600 hover:text-primary-700" to="/dashboard">
                {t('notifications.backToDashboard')}
              </Link>
            </div>
          </div>
        ) : null}

        <div className="space-y-3">
          {notifications.map((notification) => {
            const styles = typeStyles(notification.type);
            return (
              <button
                key={notification.id}
                type="button"
                onClick={() => void markOne(notification)}
                className={`w-full rounded-lg border p-4 text-left transition ${
                  notification.read
                    ? 'border-slate-200 bg-white hover:border-slate-300 dark:border-white/10 dark:bg-slate-900/60'
                    : styles.card
                }`}
              >
                <div className="flex items-start gap-3">
                  <span className={`mt-2 h-2 w-2 rounded-full ${styles.dot}`} />
                  <div className={`mt-0.5 flex h-8 w-8 items-center justify-center rounded-full shadow-sm ${styles.iconWrap}`}>
                    {styles.icon}
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <p className={`text-sm ${notification.read ? 'text-slate-600 dark:text-slate-300' : 'font-semibold text-slate-900 dark:text-slate-100'}`}>
                        {shortenOrderIds(getNotificationTitle(notification))}
                      </p>
                      <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${styles.chip}`}>
                        {notificationTypeLabel(t, notification.type)}
                      </span>
                    </div>
                    <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">{shortenOrderIds(notification.message)}</p>
                    <p className="mt-2 text-xs text-slate-400 dark:text-slate-500">{notification.time}</p>
                  </div>
                  {!notification.read ? (
                    <span className="rounded-full bg-primary-100 px-2 py-0.5 text-xs font-semibold text-primary-700 dark:bg-primary-900/40 dark:text-primary-300">
                      {t('notifications.new')}
                    </span>
                  ) : null}
                </div>
              </button>
            );
          })}
        </div>
      </div>
    </PageShell>
  );
}
