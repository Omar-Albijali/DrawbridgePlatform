import { useCallback, useEffect, useMemo, useState } from 'react';
import { Activity, ArrowDown, ArrowUp, Clock3, Package, RefreshCw, ShoppingBag, Terminal, X } from 'lucide-react';
import {
  inventoryAuditService,
  type InventoryAuditLog,
  type InventoryAuditSourceType,
  type InventoryStockTargetType,
} from '../services/inventoryAuditService';

interface InventoryHistoryPanelProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  subtitle?: string;
  productId?: string;
  inventoryItemId?: string;
  stockTargetType?: InventoryStockTargetType;
}

const PAGE_SIZE = 10;

const sourceStyles: Record<InventoryAuditSourceType, { label: string; className: string; icon: JSX.Element }> = {
  MANUAL: {
    label: 'Manual',
    className: 'bg-blue-50 text-blue-700 border-blue-200',
    icon: <Activity className="w-3.5 h-3.5" />,
  },
  ORDER: {
    label: 'Order',
    className: 'bg-indigo-50 text-indigo-700 border-indigo-200',
    icon: <ShoppingBag className="w-3.5 h-3.5" />,
  },
  RESTOCK: {
    label: 'Restock',
    className: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    icon: <RefreshCw className="w-3.5 h-3.5" />,
  },
  POS: {
    label: 'POS',
    className: 'bg-fuchsia-50 text-fuchsia-700 border-fuchsia-200',
    icon: <Terminal className="w-3.5 h-3.5" />,
  },
  SYSTEM: {
    label: 'System',
    className: 'bg-slate-100 text-slate-700 border-slate-200',
    icon: <Package className="w-3.5 h-3.5" />,
  },
};

function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function formatAmount(amount: number): string {
  return amount > 0 ? `+${amount}` : String(amount);
}

function getAmountClass(amount: number): string {
  if (amount > 0) return 'bg-emerald-50 text-emerald-700 border-emerald-200';
  if (amount < 0) return 'bg-red-50 text-red-700 border-red-200';
  return 'bg-slate-100 text-slate-700 border-slate-200';
}

function getAmountIcon(amount: number): JSX.Element {
  if (amount > 0) return <ArrowUp className="w-4 h-4" />;
  if (amount < 0) return <ArrowDown className="w-4 h-4" />;
  return <Activity className="w-4 h-4" />;
}

export default function InventoryHistoryPanel({
  isOpen,
  onClose,
  title,
  subtitle,
  productId,
  inventoryItemId,
  stockTargetType,
}: InventoryHistoryPanelProps): JSX.Element | null {
  const [logs, setLogs] = useState<InventoryAuditLog[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const canLoad = Boolean(productId || inventoryItemId);
  const hasMore = page + 1 < totalPages;

  const queryKey = useMemo(
    () => `${productId ?? ''}:${inventoryItemId ?? ''}:${stockTargetType ?? ''}`,
    [inventoryItemId, productId, stockTargetType],
  );

  const loadLogs = useCallback(
    async (nextPage: number, replace: boolean): Promise<void> => {
      if (!canLoad) {
        return;
      }

      if (replace) {
        setIsLoading(true);
      } else {
        setIsLoadingMore(true);
      }
      setError(null);

      try {
        const result = await inventoryAuditService.getLogs({
          productId,
          inventoryItemId,
          stockTargetType,
          page: nextPage,
          size: PAGE_SIZE,
        });

        setLogs((prev) => (replace ? result.items : [...prev, ...result.items]));
        setPage(result.page);
        setTotalPages(result.totalPages);
        setTotalElements(result.totalElements);
      } catch (reason) {
        console.error('Failed to load stock history', reason);
        setError('Unable to load stock history');
      } finally {
        setIsLoading(false);
        setIsLoadingMore(false);
      }
    },
    [canLoad, inventoryItemId, productId, stockTargetType],
  );

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    setLogs([]);
    setPage(0);
    setTotalPages(0);
    setTotalElements(0);
    void loadLogs(0, true);
  }, [isOpen, loadLogs, queryKey]);

  if (!isOpen) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex">
      <button type="button" aria-label="Close history" className="absolute inset-0 bg-slate-950/40 backdrop-blur-[2px]" onClick={onClose} />

      <aside className="relative ml-auto flex h-full w-full max-w-xl flex-col bg-white shadow-2xl dark:bg-slate-950">
        <div className="border-b border-slate-200 px-6 py-5 dark:border-slate-800">
          <div className="flex items-start justify-between gap-4">
            <div className="min-w-0">
              <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-[0.14em] text-slate-500">
                <Clock3 className="h-4 w-4" />
                Stock History
              </div>
              <h2 className="mt-2 truncate text-xl font-semibold text-slate-950 dark:text-white">{title}</h2>
              {subtitle && <p className="mt-1 truncate text-sm text-slate-500 dark:text-slate-400">{subtitle}</p>}
            </div>
            <button
              type="button"
              onClick={onClose}
              className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-slate-200 text-slate-500 transition-colors hover:bg-slate-50 hover:text-slate-900 dark:border-slate-800 dark:hover:bg-slate-900 dark:hover:text-white"
              title="Close"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto px-6 py-5">
          {isLoading ? (
            <div className="space-y-3">
              {Array.from({ length: 4 }).map((_, index) => (
                <div key={index} className="h-24 animate-pulse rounded-lg bg-slate-100 dark:bg-slate-900" />
              ))}
            </div>
          ) : error ? (
            <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>
          ) : logs.length === 0 ? (
            <div className="flex h-full min-h-[280px] flex-col items-center justify-center text-center">
              <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-slate-100 text-slate-500 dark:bg-slate-900 dark:text-slate-400">
                <Package className="h-6 w-6" />
              </div>
              <h3 className="mt-4 text-base font-semibold text-slate-900 dark:text-white">No stock history yet</h3>
              <p className="mt-1 max-w-sm text-sm text-slate-500 dark:text-slate-400">Stock changes will appear here as soon as this item is updated.</p>
            </div>
          ) : (
            <div className="space-y-3">
              <div className="flex items-center justify-between text-xs font-semibold uppercase tracking-[0.12em] text-slate-400">
                <span>{totalElements} event{totalElements === 1 ? '' : 's'}</span>
                <span>Newest first</span>
              </div>

              {logs.map((log) => {
                const source = sourceStyles[log.sourceType] ?? sourceStyles.SYSTEM;
                return (
                  <article key={log.id} className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex items-center gap-3">
                        <div
                          className={`flex h-10 w-10 items-center justify-center rounded-lg border text-sm font-bold ${getAmountClass(log.changeAmount)}`}
                        >
                          {getAmountIcon(log.changeAmount)}
                        </div>
                        <div>
                          <div className="flex items-center gap-2">
                            <span className={`inline-flex items-center gap-1.5 rounded-md border px-2 py-0.5 text-xs font-semibold ${source.className}`}>
                              {source.icon}
                              {source.label}
                            </span>
                            <span className={`rounded-md border px-2 py-0.5 text-xs font-bold ${getAmountClass(log.changeAmount)}`}>
                              {formatAmount(log.changeAmount)}
                            </span>
                          </div>
                          <p className="mt-2 text-sm font-semibold text-slate-900 dark:text-white">
                            {log.quantityBefore} <span className="text-slate-400">-&gt;</span> {log.quantityAfter}
                          </p>
                        </div>
                      </div>
                      <time className="shrink-0 text-right text-xs font-medium text-slate-500 dark:text-slate-400">{formatDateTime(log.createdAt)}</time>
                    </div>

                    <div className="mt-4 grid grid-cols-1 gap-2 border-t border-slate-100 pt-3 text-xs text-slate-500 dark:border-slate-800 dark:text-slate-400">
                      <div className="flex items-center justify-between gap-3">
                        <span className="font-medium text-slate-400">Changed by</span>
                        <span className="truncate text-right font-semibold text-slate-700 dark:text-slate-300">{log.changedBy}</span>
                      </div>
                      {log.reason && (
                        <div className="flex items-start justify-between gap-3">
                          <span className="font-medium text-slate-400">Reason</span>
                          <span className="text-right font-medium text-slate-700 dark:text-slate-300">{log.reason}</span>
                        </div>
                      )}
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </div>

        {logs.length > 0 && (
          <div className="border-t border-slate-200 px-6 py-4 dark:border-slate-800">
            <button
              type="button"
              onClick={() => void loadLogs(page + 1, false)}
              disabled={!hasMore || isLoadingMore}
              className="w-full rounded-lg border border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50 dark:border-slate-800 dark:text-slate-200 dark:hover:bg-slate-900"
            >
              {isLoadingMore ? 'Loading...' : hasMore ? 'Load more' : 'All history loaded'}
            </button>
          </div>
        )}
      </aside>
    </div>
  );
}
