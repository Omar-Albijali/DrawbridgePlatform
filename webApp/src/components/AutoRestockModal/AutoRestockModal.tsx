import { useEffect, useState } from 'react';
import { Calendar, RefreshCw, Settings, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { dayOfWeekLabel } from '../../i18n/display';
import type { AutoOrderConfigDTO, InventoryItem } from '../../types';
import { ScheduleType } from '../../types';

interface AutoRestockModalProps {
  item: InventoryItem | null;
  isOpen: boolean;
  onClose: () => void;
  onSave: (config: Partial<AutoOrderConfigDTO>) => void | Promise<void>;
}

export default function AutoRestockModal({
  item,
  isOpen,
  onClose,
  onSave,
}: AutoRestockModalProps): JSX.Element | null {
  const { t } = useTranslation();
  const [threshold, setThreshold] = useState(10);
  const [reorderQuantity, setReorderQuantity] = useState(25);
  const [scheduleType, setScheduleType] = useState<ScheduleType>(ScheduleType.THRESHOLD_BASED);
  const [intervalDays, setIntervalDays] = useState(7);
  const [dayOfWeek, setDayOfWeek] = useState('MONDAY');
  const [dayOfMonth, setDayOfMonth] = useState('1');
  const [validationError, setValidationError] = useState<string | null>(null);

  useEffect(() => {
    if (!item) {
      return;
    }

    const minimumOrderQuantity = Math.max(1, item.minimumOrderQuantity ?? 1);
    setThreshold(item.autoOrderConfig?.minThreshold ?? 10);
    setReorderQuantity(Math.max(item.autoOrderConfig?.reorderQuantity ?? minimumOrderQuantity, minimumOrderQuantity));
    setScheduleType(item.autoOrderConfig?.scheduleType ?? ScheduleType.THRESHOLD_BASED);
    setIntervalDays(item.autoOrderConfig?.intervalDays ?? 7);
    setDayOfWeek(item.autoOrderConfig?.dayOfWeek ?? 'MONDAY');
    setDayOfMonth(item.autoOrderConfig?.dayOfMonth ?? '1');
    setValidationError(null);
  }, [item]);

  if (!isOpen || !item) {
    return null;
  }

  const minimumOrderQuantity = Math.max(1, item.minimumOrderQuantity ?? 1);

  const handleSave = async (): Promise<void> => {
    if (!Number.isInteger(reorderQuantity) || reorderQuantity < minimumOrderQuantity) {
      setValidationError(t('inventory.restock.minimumOrderError', { count: minimumOrderQuantity }));
      return;
    }

    const config: Partial<AutoOrderConfigDTO> = {
      minThreshold: threshold,
      reorderQuantity,
      scheduleType,
      intervalDays: scheduleType == ScheduleType.INTERVAL_DAYS ? intervalDays : null,
      dayOfWeek: scheduleType == ScheduleType.WEEKLY ? dayOfWeek : null,
      dayOfMonth: scheduleType == ScheduleType.MONTHLY ? dayOfMonth : null,
      enabled: true,
    };

    try {
      await onSave(config);
      onClose();
    } catch (error) {
      setValidationError(error instanceof Error ? error.message : t('inventory.restock.saveFailed'));
    }
  };

  const daysOfWeek = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
      <div className="bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-700/70 w-full max-w-[40vw] overflow-hidden flex flex-col max-h-[90vh]">
        <div className="bg-gradient-to-r from-primary-600 to-primary-700 p-6 text-white shrink-0">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-white/20 rounded-lg flex items-center justify-center">
                <Settings className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-lg font-semibold">{t('inventory.restock.title')}</h2>
                <p className="text-primary-100 text-sm">{item.name}</p>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="w-8 h-8 bg-white/20 hover:bg-white/30 rounded-lg flex items-center justify-center transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        <div className="p-6 space-y-6 overflow-y-auto text-slate-800 dark:text-slate-100">
          <div>
            <label className="label">{t('inventory.restock.strategy')}</label>
            <div className="grid grid-cols-1 gap-2">
              <button
                type="button"
                className={`p-3 rounded-lg border text-left transition-all ${scheduleType == ScheduleType.THRESHOLD_BASED
                    ? 'border-primary-500 bg-primary-50 text-primary-700 dark:bg-primary-500/15 dark:text-primary-200'
                    : 'border-slate-200 hover:border-slate-300 dark:border-slate-700 dark:hover:border-slate-600'
                  }`}
                onClick={() => setScheduleType(ScheduleType.THRESHOLD_BASED)}
              >
                <div className="font-medium flex items-center gap-2">
                  <Settings className="w-4 h-4" />
                  {t('inventory.restock.thresholdBased')}
                </div>
                <p className="text-xs opacity-80 mt-1">{t('inventory.restock.thresholdHint')}</p>
              </button>

              <button
                type="button"
                className={`p-3 rounded-lg border text-left transition-all ${scheduleType == ScheduleType.WEEKLY
                    ? 'border-primary-500 bg-primary-50 text-primary-700 dark:bg-primary-500/15 dark:text-primary-200'
                    : 'border-slate-200 hover:border-slate-300 dark:border-slate-700 dark:hover:border-slate-600'
                  }`}
                onClick={() => setScheduleType(ScheduleType.WEEKLY)}
              >
                <div className="font-medium flex items-center gap-2">
                  <Calendar className="w-4 h-4" />
                  {t('inventory.restock.weekly')}
                </div>
                <p className="text-xs opacity-80 mt-1">{t('inventory.restock.weeklyHint')}</p>
              </button>

              <button
                type="button"
                className={`p-3 rounded-lg border text-left transition-all ${scheduleType == ScheduleType.MONTHLY
                    ? 'border-primary-500 bg-primary-50 text-primary-700 dark:bg-primary-500/15 dark:text-primary-200'
                    : 'border-slate-200 hover:border-slate-300 dark:border-slate-700 dark:hover:border-slate-600'
                  }`}
                onClick={() => setScheduleType(ScheduleType.MONTHLY)}
              >
                <div className="font-medium flex items-center gap-2">
                  <Calendar className="w-4 h-4" />
                  {t('inventory.restock.monthly')}
                </div>
                <p className="text-xs opacity-80 mt-1">{t('inventory.restock.monthlyHint')}</p>
              </button>

              <button
                type="button"
                className={`p-3 rounded-lg border text-left transition-all ${scheduleType == ScheduleType.INTERVAL_DAYS
                    ? 'border-primary-500 bg-primary-50 text-primary-700 dark:bg-primary-500/15 dark:text-primary-200'
                    : 'border-slate-200 hover:border-slate-300 dark:border-slate-700 dark:hover:border-slate-600'
                  }`}
                onClick={() => setScheduleType(ScheduleType.INTERVAL_DAYS)}
              >
                <div className="font-medium flex items-center gap-2">
                  <RefreshCw className="w-4 h-4" />
                  {t('inventory.restock.interval')}
                </div>
                <p className="text-xs opacity-80 mt-1">{t('inventory.restock.intervalHint')}</p>
              </button>
            </div>
          </div>

          <div className="space-y-4 pt-4 border-t border-slate-200 dark:border-slate-700/70">
            <h3 className="font-medium text-slate-900 dark:text-slate-100">{t('inventory.restock.configurationDetails')}</h3>
            <p className="rounded-lg bg-slate-50 px-3 py-2 text-sm font-medium text-slate-600 dark:bg-slate-800 dark:text-slate-300">
              {t('inventory.restock.minimumOrderQuantity', { count: minimumOrderQuantity })}
            </p>

            {scheduleType == ScheduleType.THRESHOLD_BASED && (
              <div>
                <label className="label">{t('inventory.restock.minimumThreshold')}</label>
                <div className="relative">
                  <input
                    type="number"
                    value={threshold}
                    onChange={(event) => setThreshold(Number(event.target.value))}
                    className="input pr-16"
                    min="1"
                  />
                  <span className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-500 dark:text-slate-400 text-sm">{t('inventory.restock.units')}</span>
                </div>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">{t('inventory.restock.triggerHint')}</p>
              </div>
            )}

            {scheduleType == ScheduleType.WEEKLY && (
              <div>
                <label className="label">{t('inventory.restock.dayOfWeek')}</label>
                <select className="input" value={dayOfWeek} onChange={(event) => setDayOfWeek(event.target.value)}>
                  {daysOfWeek.map((day) => (
                    <option key={day} value={day}>
                      {dayOfWeekLabel(t, day)}
                    </option>
                  ))}
                </select>
              </div>
            )}

            {scheduleType == ScheduleType.MONTHLY && (
              <div>
                <label className="label">{t('inventory.restock.dayOfMonth')}</label>
                <select className="input" value={dayOfMonth} onChange={(event) => setDayOfMonth(event.target.value)}>
                  {Array.from({ length: 28 }, (_, index) => index + 1).map((day) => (
                    <option key={day} value={day.toString()}>
                      {t('inventory.restock.ordinal', { day })}
                    </option>
                  ))}
                </select>
              </div>
            )}

            {scheduleType == ScheduleType.INTERVAL_DAYS && (
              <div>
                <label className="label">{t('inventory.restock.intervalDays')}</label>
                <div className="relative">
                  <input
                    type="number"
                    value={intervalDays}
                    onChange={(event) => setIntervalDays(Number(event.target.value))}
                    className="input pr-16"
                    min="1"
                  />
                  <span className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-500 dark:text-slate-400 text-sm">{t('inventory.restock.days')}</span>
                </div>
              </div>
            )}

            <div>
              <label className="label">{t('inventory.restock.reorderQuantity')}</label>
              <div className="relative">
                <input
                  type="number"
                  value={reorderQuantity}
                  onChange={(event) => setReorderQuantity(Number(event.target.value))}
                  className="input pr-16"
                  min={minimumOrderQuantity}
                  step="1"
                />
                <span className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-500 dark:text-slate-400 text-sm">{t('inventory.restock.units')}</span>
              </div>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">{t('inventory.restock.amountHint')}</p>
              {validationError && <p className="text-xs font-medium text-red-600 mt-2">{validationError}</p>}
            </div>
          </div>
        </div>

        <div className="px-6 py-4 bg-slate-50 border-t border-slate-200 dark:bg-slate-900/60 dark:border-slate-700/70 flex gap-3 shrink-0">
          <button type="button" onClick={onClose} className="flex-1 btn-secondary">
            {t('common.cancel')}
          </button>
          <button type="button" onClick={() => void handleSave()} className="flex-1 btn-primary">
            {t('inventory.restock.saveConfiguration')}
          </button>
        </div>
      </div>
    </div>
  );
}
