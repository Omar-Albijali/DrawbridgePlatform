import { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { AlertTriangle, CheckCircle2, Edit2, Package, Plus, RefreshCw, Search, Settings, Trash2, XCircle } from 'lucide-react';
import AddInventoryModal from '../components/AddInventoryModal/AddInventoryModal';
import AutoRestockModal from '../components/AutoRestockModal/AutoRestockModal';
import PageShell from '../components/PageShell';
import { useAuth } from '../contexts/AuthContext';
import { inventoryService } from '../services/inventoryService';
import type { AutoOrderConfigDTO, CreateInventoryItemRequest, InventoryItem } from '../types';
import { InventoryStatus, ScheduleType, UserRole } from '../types';

export default function Inventory(): JSX.Element {
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

  const getStatusBadge = (status: InventoryItem['status']): JSX.Element | null => {
    const statusName = getStatusName(status);

    switch (statusName) {
      case 'OPTIMAL':
        return (
          <span className="badge-success flex items-center gap-1">
            <CheckCircle2 className="w-3 h-3" />
            Optimal
          </span>
        );
      case 'LOW_STOCK':
        return (
          <span className="badge-warning flex items-center gap-1">
            <AlertTriangle className="w-3 h-3" />
            Low Stock
          </span>
        );
      case 'OUT_OF_STOCK':
        return (
          <span className="badge-danger flex items-center gap-1">
            <XCircle className="w-3 h-3" />
            Out of Stock
          </span>
        );
      default:
        return null;
    }
  };

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
    if (!window.confirm(`Are you sure you want to remove ${item.name} from your inventory?`)) {
      return;
    }

    try {
      await inventoryService.delete(item.id);
      setInventory((prev) => prev.filter((inventoryItem) => inventoryItem.id !== item.id));
    } catch (error) {
      console.error('Failed to delete item', error);
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
      title="Inventory Management"
      description="Track and manage your stock levels"
      actions={
        <button type="button" onClick={() => setIsAddModalOpen(true)} className="btn-primary flex items-center gap-2">
          <Plus className="w-4 h-4" />
          Add Product
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
              <p className="text-sm text-navy-500">Total Items</p>
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
              <p className="text-sm text-navy-500">Low Stock</p>
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
              <p className="text-sm text-navy-500">Out of Stock</p>
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
              <p className="text-sm text-navy-500">Auto-Restock</p>
              <p className="text-xl font-bold text-green-600">{autoRestockEnabled}</p>
            </div>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-card p-4">
        <div className="relative max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-navy-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            placeholder="Search inventory..."
            className="w-full pl-10 pr-4 py-2.5 bg-gray-100 border border-transparent rounded-lg focus:bg-white focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
          />
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="text-left px-6 py-4 text-sm font-semibold text-navy-700">Product Name</th>
                <th className="text-center px-6 py-4 text-sm font-semibold text-navy-700">Current Stock</th>
                <th className="text-center px-6 py-4 text-sm font-semibold text-navy-700">Threshold</th>
                <th className="text-center px-6 py-4 text-sm font-semibold text-navy-700">Auto-Restock</th>
                <th className="text-center px-6 py-4 text-sm font-semibold text-navy-700">Status</th>
                <th className="text-left px-6 py-4 text-sm font-semibold text-navy-700">Supplier</th>
                <th className="text-center px-6 py-4 text-sm font-semibold text-navy-700">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filteredInventory.map((item) => (
                <tr
                  key={item.id}
                  className={`hover:bg-gray-50 transition-colors ${
                    getStatusName(item.status) === InventoryStatus.LOW_STOCK.name
                      ? 'bg-amber-50/50'
                      : getStatusName(item.status) === InventoryStatus.OUT_OF_STOCK.name
                        ? 'bg-red-50/50'
                        : ''
                  }`}
                >
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 bg-gray-100 rounded-lg flex items-center justify-center">
                        <Package className="w-5 h-5 text-navy-400" />
                      </div>
                      <div>
                        <p className="font-medium text-navy-800">{item.name}</p>
                        <p className="text-sm text-navy-500">Last restocked: {item.lastRestocked ?? 'N/A'}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-center">
                    <span
                      className={`font-semibold ${
                        item.currentStock === 0
                          ? 'text-red-600'
                          : item.currentStock < (item.autoOrderConfig?.minThreshold ?? 0)
                            ? 'text-amber-600'
                            : 'text-navy-800'
                      }`}
                    >
                      {item.currentStock}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-center text-navy-600">{item.autoOrderConfig?.minThreshold ?? 'N/A'}</td>
                  <td className="px-6 py-4 text-center">
                    <button
                      type="button"
                      onClick={() => void toggleAutoRestock(item.id)}
                      className={`relative w-12 h-6 rounded-full transition-colors ${item.autoRestock ? 'bg-green-500' : 'bg-gray-300'}`}
                    >
                      <span
                        className={`absolute top-1 w-4 h-4 bg-white rounded-full shadow transition-transform ${
                          item.autoRestock ? 'left-7' : 'left-1'
                        }`}
                      />
                    </button>
                  </td>
                  <td className="px-6 py-4 text-center">{getStatusBadge(item.status)}</td>
                  <td className="px-6 py-4 text-navy-600">{item.supplier}</td>
                  <td className="px-6 py-4">
                    <div className="flex items-center justify-center gap-2">
                      <button
                        type="button"
                        onClick={() => openConfigModal(item)}
                        className="p-2 text-navy-500 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
                        title="Configure Auto-Restock"
                      >
                        <Settings className="w-4 h-4" />
                      </button>
                      <button
                        type="button"
                        className="p-2 text-navy-500 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
                        title="Edit"
                      >
                        <Edit2 className="w-4 h-4" />
                      </button>
                      <button
                        type="button"
                        className="p-2 text-navy-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                        title="Delete"
                        onClick={() => void handleDelete(item)}
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {filteredInventory.length === 0 && (
          <div className="text-center py-12">
            <Package className="w-12 h-12 text-navy-300 mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-navy-800 mb-2">No items found</h3>
            <p className="text-navy-500">Try adjusting your search query</p>
          </div>
        )}
      </div>

      <AutoRestockModal item={selectedItem} isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} onSave={handleSaveConfig} />

      <AddInventoryModal isOpen={isAddModalOpen} onClose={() => setIsAddModalOpen(false)} onAdd={handleAddInventory} />
    </PageShell>
  );
}
