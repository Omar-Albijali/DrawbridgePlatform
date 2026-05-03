import { KeyboardEvent, useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { AlertTriangle, Package, Plus, RefreshCw, Search, Trash2, XCircle, Settings2, Calendar, Activity, X, Check, Slash, History } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import AddInventoryModal from '../components/AddInventoryModal/AddInventoryModal';
import AutoRestockModal from '../components/AutoRestockModal/AutoRestockModal';
import InventoryHistoryPanel from '../components/InventoryHistoryPanel';
import PageShell from '../components/PageShell';
import { useAuth } from '../contexts/AuthContext';
import { inventoryService } from '../services/inventoryService';
import { dayOfWeekLabel, formatDate } from '../i18n/display';
import type { AutoOrderConfigDTO, CreateInventoryItemRequest, InventoryItem } from '../types';
import { InventoryStatus, ScheduleType, UserRole } from '../types';

export default function Inventory(): JSX.Element {
  const { t } = useTranslation();
  const { user } = useAuth();

  const isRetailer = user?.role === UserRole.RETAILER;
  if (!isRetailer) {
    return <Navigate to="/dashboard" replace />;
  }

  const [inventory, setInventory] = useState<InventoryItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedItem, setSelectedItem] = useState<InventoryItem | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [historyItem, setHistoryItem] = useState<InventoryItem | null>(null);
  const [editingStockItemId, setEditingStockItemId] = useState<string | null>(null);
  const [stockDraft, setStockDraft] = useState('');
  const [isSavingStock, setIsSavingStock] = useState(false);

  useEffect(() => {
    const fetchInventory = async (): Promise<void> => {
      if (!user?.id) {
        setIsLoading(false);
        return;
      }

      try {
        const data = await inventoryService.getByRetailer(user.id);
        setInventory(data);
      } catch (error) {
        console.error('Failed to fetch inventory', error);
      } finally {
        setIsLoading(false);
      }
    };

    void fetchInventory();
  }, [user]);

  const getStatusName = (status: InventoryItem['status']): string => {
    if (!status) {
      return '';
    }
    return (status as { name?: string }).name ?? String(status);
  };

  const filteredInventory = inventory.filter(
    (item) =>
      (item.name ?? '').toLowerCase().includes(searchQuery.toLowerCase()) ||
      (item.supplier ?? '').toLowerCase().includes(searchQuery.toLowerCase()),
  );

  const toggleAutoRestock = async (id: string): Promise<void> => {
    const item = inventory.find((inventoryItem) => inventoryItem.id === id);
    if (!item) {
      return;
    }

    const originalState = [...inventory];
    const updatedItem = { ...item, autoRestock: !item.autoRestock } as unknown as InventoryItem;
    setInventory((prev) => prev.map((inventoryItem) => (inventoryItem.id === id ? updatedItem : inventoryItem)));

    try {
      await inventoryService.toggleAutoOrder(id, !item.autoRestock);
    } catch (error) {
      console.error('Failed to toggle auto restock', error);
      setInventory(originalState);
    }
  };

  const handleDelete = async (item: InventoryItem): Promise<void> => {
    if (!window.confirm(t('inventory.confirmRemove', { name: item.name }))) {
      return;
    }

    try {
      await inventoryService.delete(item.id);
      setInventory((prev) => prev.filter((inventoryItem) => inventoryItem.id !== item.id));
    } catch (error) {
      console.error('Failed to delete item', error);
    }
  };

  const beginStockEdit = (item: InventoryItem): void => {
    setEditingStockItemId(item.id);
    setStockDraft(String(item.currentStock));
  };

  const cancelStockEdit = (): void => {
    setEditingStockItemId(null);
    setStockDraft('');
  };

  const saveStockEdit = async (item: InventoryItem): Promise<void> => {
    if (isSavingStock) {
      return;
    }

    const parsed = Number.parseInt(stockDraft.trim(), 10);
    if (!Number.isInteger(parsed) || parsed < 0) {
      return;
    }

    setIsSavingStock(true);
    try {
      const updated = await inventoryService.updateQuantity(item.id, parsed);
      setInventory((prev) => prev.map((inventoryItem) => (inventoryItem.id === item.id ? updated : inventoryItem)));
      cancelStockEdit();
    } catch (error) {
      console.error('Failed to update stock level', error);
    } finally {
      setIsSavingStock(false);
    }
  };

  const handleStockEditorKeyDown = (event: KeyboardEvent<HTMLInputElement>, item: InventoryItem): void => {
    if (event.key === 'Enter') {
      event.preventDefault();
      void saveStockEdit(item);
      return;
    }

    if (event.key === 'Escape') {
      event.preventDefault();
      cancelStockEdit();
    }
  };

  const openConfigModal = (item: InventoryItem): void => {
    setSelectedItem(item);
    setIsModalOpen(true);
  };

  const handleSaveConfig = async (config: Partial<AutoOrderConfigDTO>): Promise<void> => {
    if (!selectedItem) {
      return;
    }

    try {
      await inventoryService.saveAutoOrderConfig(selectedItem.id, true, {
        minThreshold: config.minThreshold ?? 0,
        reorderQuantity: config.reorderQuantity ?? 0,
        scheduleType: config.scheduleType ?? ScheduleType.THRESHOLD_BASED,
        intervalDays: config.intervalDays,
        dayOfWeek: config.dayOfWeek,
        dayOfMonth: config.dayOfMonth,
      });

      const updatedItem = await inventoryService.getById(selectedItem.id);
      setInventory((prev) => prev.map((item) => (item.id === selectedItem.id ? updatedItem : item)));
    } catch (error) {
      console.error('Failed to save auto-order config', error);
    }
  };

  const handleAddInventory = async (request: CreateInventoryItemRequest): Promise<void> => {
    try {
      await inventoryService.create(request);
      if (user?.id) {
        const data = await inventoryService.getByRetailer(user.id);
        setInventory(data);
      }
    } catch (error) {
      console.error('Failed to create inventory item', error);
    }
  };


  const ScheduleSummary = (item: InventoryItem): string => {
    const config = item.autoOrderConfig;
    const scheduleType = config?.scheduleType;

    if (scheduleType == ScheduleType.THRESHOLD_BASED) {
      return t('inventory.schedule.units', { count: config?.minThreshold ?? 0 });
    }

    if (scheduleType == ScheduleType.DAILY) {
      return t('inventory.schedule.daily');
    }

    if (scheduleType == ScheduleType.WEEKLY) {
      return t('inventory.schedule.weekly', { day: dayOfWeekLabel(t, config?.dayOfWeek ?? '') });
    }

    if (scheduleType == ScheduleType.MONTHLY) {
      return t('inventory.schedule.monthly', { day: config?.dayOfMonth });
    }

    if (scheduleType == ScheduleType.INTERVAL_DAYS) {
      return t('inventory.schedule.interval', { count: config?.intervalDays ?? 0 });
    }

    return t('inventory.schedule.notScheduled');
  };

  const formatNextRestockDate = (value?: string | null): string | null => {
    if (!value) {
      return null;
    }

    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
      return null;
    }

    return formatDate(parsed, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  const lowStockCount = inventory.filter((item) => getStatusName(item.status) === InventoryStatus.LOW_STOCK.name).length;
  const outOfStockCount = inventory.filter((item) => getStatusName(item.status) === InventoryStatus.OUT_OF_STOCK.name).length;
  const autoRestockEnabled = inventory.filter((item) => item.autoRestock).length;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600" />
      </div>
    );
  }

  return (
    <PageShell
      title={t('inventory.title')}
      description={t('inventory.description')}
      actions={
        <button type="button" onClick={() => setIsAddModalOpen(true)} className="btn-primary flex items-center gap-2">
          <Plus className="w-4 h-4" />
          {t('inventory.addProduct')}
        </button>
      }
    >



      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="card !p-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-primary-100 rounded-lg flex items-center justify-center">
              <Package className="w-5 h-5 text-primary-600" />
            </div>
            <div>
              <p className="text-sm text-navy-500">{t('inventory.totalItems')}</p>
              <p className="text-xl font-bold text-navy-800">{inventory.length}</p>
            </div>
          </div>
        </div>
        <div className="card !p-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-amber-100 rounded-lg flex items-center justify-center">
              <AlertTriangle className="w-5 h-5 text-amber-600" />
            </div>
            <div>
              <p className="text-sm text-navy-500">{t('inventory.lowStock')}</p>
              <p className="text-xl font-bold text-amber-600">{lowStockCount}</p>
            </div>
          </div>
        </div>
        <div className="card !p-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-red-100 rounded-lg flex items-center justify-center">
              <XCircle className="w-5 h-5 text-red-600" />
            </div>
            <div>
              <p className="text-sm text-navy-500">{t('inventory.outOfStock')}</p>
              <p className="text-xl font-bold text-red-600">{outOfStockCount}</p>
            </div>
          </div>
        </div>
        <div className="card !p-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
              <RefreshCw className="w-5 h-5 text-green-600" />
            </div>
            <div>
              <p className="text-sm text-navy-500">{t('inventory.autoRestock')}</p>
              <p className="text-xl font-bold text-green-600">{autoRestockEnabled}</p>
            </div>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-card p-4 mt-6">
        <div className="relative max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-navy-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            placeholder={t('inventory.searchPlaceholder')}
            className="w-full pl-10 pr-4 py-2.5 bg-gray-100 border border-transparent rounded-lg focus:bg-white focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
          />
        </div>
      </div>

      <div className="border border-slate-200 dark:border-slate-800 rounded-lg overflow-hidden bg-white dark:bg-[#0f1219] shadow-2xl mt-6">
        <div className="grid grid-cols-12 gap-4 px-4 py-2.5 bg-slate-50 dark:bg-slate-900/50 border-b border-slate-200 dark:border-slate-800 text-[10px] font-bold text-slate-500 uppercase tracking-[0.15em]">
          <div className="col-span-4">{t('inventory.productDescriptor')}</div>
          <div className="col-span-2 text-center">{t('inventory.stockQty')}</div>
          <div className="col-span-1 text-center">{t('inventory.stockStatus')}</div>
          <div className="col-span-2 text-center">{t('inventory.nextRestock')}</div>
          <div className="col-span-3 text-right pr-6">{t('inventory.automation')}</div>
        </div>

        <div className="divide-y divide-slate-100 dark:divide-slate-800/50">
          {filteredInventory.map((item) => {
            const isEditingStock = editingStockItemId === item.id;
            const isLow = item.currentStock <= (item.autoOrderConfig?.minThreshold ?? 0);
            const hasChanged = isEditingStock && String(item.currentStock) !== stockDraft;
            const nextRestockDate = formatNextRestockDate(item.autoOrderConfig?.nextScheduledAt);

            return (
              <div key={item.id} className="grid grid-cols-12 gap-4 items-center px-4 py-4 hover:bg-slate-50 dark:hover:bg-slate-800/20 transition-all group">
                <div className="col-span-4 flex items-center gap-3">
                  <div className="w-10 h-10 rounded bg-slate-900 flex items-center justify-center border border-slate-800 group-hover:border-slate-600 transition-colors overflow-hidden">
                    {item.imageUrl ? (
                      <img src={item.imageUrl} alt={item.name} className="w-full h-full object-cover" />
                    ) : (
                      <Package className="w-5 h-5 text-slate-500 group-hover:text-slate-300 transition-colors" />
                    )}
                  </div>
                  <div className="truncate flex-1">
                    <div className="flex items-center gap-1">
                      <div className="text-sm font-semibold text-slate-900 dark:text-slate-200 truncate">{item.name}</div>
                      <button
                        type="button"
                        onClick={() => setHistoryItem(item)}
                        className="p-1 text-slate-400 hover:text-blue-500 hover:bg-blue-500/10 rounded-lg transition-all opacity-0 group-hover:opacity-100"
                        title={t('inventory.viewStockHistory')}
                      >
                        <History className="w-3.5 h-3.5" />
                      </button>
                      <button
                        type="button"
                        onClick={() => void handleDelete(item)}
                        className="p-1 text-slate-400 hover:text-red-500 hover:bg-red-500/10 rounded-lg transition-all opacity-0 group-hover:opacity-100"
                        title={t('inventory.deleteFromInventory')}
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                    <div className="text-[10px] text-slate-500 font-mono mt-0.5 flex items-center gap-2">
                      <span>{t('inventory.itemId', { id: item.id ? item.id.substring(0, 8).toUpperCase() : t('common.unknown') })}</span>
                      <span className="w-1 h-1 rounded-full bg-slate-300 dark:bg-slate-700" />
                      <span className="uppercase">{item.supplier ?? t('inventory.unknownSupplier')}</span>
                    </div>
                  </div>
                </div>

                <div className="col-span-2 flex flex-col items-center">
                  <div className="relative flex items-center gap-1">
                    <input
                      type="number"
                      value={isEditingStock ? stockDraft : item.currentStock}
                      onFocus={() => {
                        if (!isEditingStock) beginStockEdit(item);
                      }}
                      onBlur={() => {
                        if (isEditingStock && !hasChanged) cancelStockEdit();
                      }}
                      onKeyDown={(event) => handleStockEditorKeyDown(event, item)}
                      onChange={(e) => {
                        const val = e.target.value;
                        if (!isEditingStock) beginStockEdit(item);
                        setStockDraft(val);
                      }}
                      className={`w-14 text-center bg-transparent border-b transition-all outline-none py-0.5 text-base font-semibold tabular-nums ${isEditingStock || hasChanged
                          ? 'border-blue-500 text-slate-900 dark:text-white'
                          : isLow
                            ? 'border-transparent text-amber-600 dark:text-amber-400'
                            : 'border-transparent text-slate-900 dark:text-slate-200'
                        } hover:border-slate-400 focus:border-blue-500 focus:ring-0`}
                    />

                    {hasChanged && (
                      <div className="flex flex-col gap-0.5 absolute -right-6 animate-in fade-in slide-in-from-left-1 duration-200 z-10">
                        <button
                          type="button"
                          onClick={() => void saveStockEdit(item)}
                          disabled={isSavingStock}
                          className="p-0.5 text-emerald-600 hover:bg-emerald-500/10 rounded transition-colors disabled:opacity-50"
                          title={t('inventory.confirmChange')}
                        >
                          <Check className="w-3.5 h-3.5" />
                        </button>
                        <button
                          type="button"
                          onClick={cancelStockEdit}
                          disabled={isSavingStock}
                          className="p-0.5 text-slate-500 hover:bg-slate-800/20 rounded transition-colors disabled:opacity-50"
                          title={t('common.cancel')}
                        >
                          <X className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    )}
                  </div>
                </div>

                <div className="col-span-1 flex justify-center">
                  <span
                    className={`inline-flex items-center rounded-md px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide whitespace-nowrap ${isLow
                      ? 'bg-amber-50 text-amber-700 border border-amber-200 dark:bg-amber-900/20 dark:text-amber-300 dark:border-amber-800'
                      : 'bg-slate-100 text-slate-600 border border-slate-200 dark:bg-slate-800/60 dark:text-slate-300 dark:border-slate-700'
                      }`}
                  >
                    {isLow ? t('common.criticalLow') : t('common.stable')}
                  </span>
                </div>

                <div className="col-span-2 flex justify-center">
                  {nextRestockDate ? (
                    <span className="inline-flex items-center gap-1 rounded-md border border-slate-300 bg-slate-50 px-2 py-0.5 text-[11px] font-medium text-slate-700 whitespace-nowrap dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300">
                      <Calendar className="w-3 h-3" />
                      {nextRestockDate}
                    </span>
                  ) : (
                    <span className="text-[11px] font-medium text-slate-400 dark:text-slate-600">-</span>
                  )}
                </div>

                <div className="col-span-3 flex justify-end pr-2">
                  <div
                    className={`flex items-center p-1 rounded-lg border transition-all duration-300 min-w-[280px] min-h-[46px] ${item.autoRestock ? 'bg-blue-500/5 border-blue-500/20' : 'bg-slate-50 dark:bg-slate-900 border-slate-200 dark:border-slate-800 opacity-60'}`}
                  >
                    <button
                      type="button"
                      onClick={() => void toggleAutoRestock(item.id)}
                      className={`flex items-center gap-2 px-3 py-1.5 rounded-md text-[10px] font-bold uppercase tracking-wider transition-all min-w-[100px] justify-center ${item.autoRestock
                          ? 'bg-blue-600 text-white shadow-lg shadow-blue-900/40'
                          : 'bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                        }`}
                    >
                      {item.autoRestock ? (
                        <>
                          <div className="w-1.5 h-1.5 rounded-full bg-white animate-pulse" />
                          {t('common.active')}
                        </>
                      ) : (
                        <>
                          <Slash className="w-3 h-3 rotate-12" />
                          {t('common.notActive')}
                        </>
                      )}
                    </button>

                    <div className="px-4 flex items-center gap-4 border-l border-slate-800/50 ml-1">
                      <div className="flex items-center gap-2.5">
                        <div
                          className={`p-1.5 rounded bg-slate-100 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 ${item.autoRestock ? 'text-blue-400' : 'text-slate-600'}`}
                        >
                          {item.autoOrderConfig?.scheduleType == ScheduleType.THRESHOLD_BASED ? (
                            <Activity className="w-3.5 h-3.5" />
                          ) : (
                            <Calendar className="w-3.5 h-3.5" />
                          )}
                        </div>
                        <div className="flex flex-col">
                          <span className="text-[11px] font-bold text-slate-900 dark:text-slate-200 whitespace-nowrap">{ScheduleSummary(item)}</span>
                          <span className="text-[9px] text-slate-500 font-bold uppercase tracking-widest whitespace-nowrap">
                            {item.autoOrderConfig?.scheduleType == ScheduleType.THRESHOLD_BASED ? t('inventory.threshold') : t('inventory.recurrence')}
                          </span>
                        </div>
                      </div>

                      <button
                        type="button"
                        onClick={() => openConfigModal(item)}
                        className="p-1.5 hover:bg-slate-800 rounded text-slate-500 hover:text-white transition-colors"
                      >
                        <Settings2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {filteredInventory.length === 0 && (
          <div className="text-center py-12">
            <Package className="w-12 h-12 text-slate-600 mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-300 mb-2">{t('inventory.noItems')}</h3>
            <p className="text-slate-500">{t('inventory.adjustSearch')}</p>
          </div>
        )}
      </div>

      <AutoRestockModal item={selectedItem} isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} onSave={handleSaveConfig} />

      <AddInventoryModal isOpen={isAddModalOpen} onClose={() => setIsAddModalOpen(false)} onAdd={handleAddInventory} />

      <InventoryHistoryPanel
        isOpen={historyItem !== null}
        onClose={() => setHistoryItem(null)}
        title={historyItem?.name ?? t('inventory.stockHistory')}
        subtitle={historyItem?.supplier ?? undefined}
        inventoryItemId={historyItem?.id}
        stockTargetType="RETAILER_INVENTORY"
      />
    </PageShell>
  );
}
