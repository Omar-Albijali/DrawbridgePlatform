import { useEffect, useState, type FormEvent } from 'react';
import { Package, Plus, Search, X } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { productService } from '../../services/productService';
import type { CreateInventoryItemRequest, Product } from '../../types';

interface AddInventoryModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAdd: (request: CreateInventoryItemRequest) => Promise<void>;
}

export default function AddInventoryModal({ isOpen, onClose, onAdd }: AddInventoryModalProps): JSX.Element | null {
  const { user } = useAuth();
  const [step, setStep] = useState<1 | 2>(1);
  const [searchQuery, setSearchQuery] = useState('');
  const [products, setProducts] = useState<Product[]>([]);
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [currentStock, setCurrentStock] = useState(0);
  const [minThreshold, setMinThreshold] = useState(10);
  const [autoRestock, setAutoRestock] = useState(false);

  useEffect(() => {
    if (isOpen && step === 1) {
      void searchProducts('');
    }
  }, [isOpen, step]);

  const searchProducts = async (query: string): Promise<void> => {
    setIsLoading(true);
    try {
      const results = query ? await productService.search(query) : await productService.getAll();
      setProducts(results);
    } catch (error) {
      console.error('Failed to search products', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSearch = (event: FormEvent): void => {
    event.preventDefault();
    void searchProducts(searchQuery);
  };

  const handleSelectProduct = (product: Product): void => {
    setSelectedProduct(product);
    setStep(2);
  };

  const handleSubmit = async (): Promise<void> => {
    if (!selectedProduct || !user?.id) {
      return;
    }

    setIsSubmitting(true);
    try {
      const request = {
        productId: selectedProduct.id,
        retailerId: user.id,
        currentStock,
        minThreshold,
        autoRestock,
      } as unknown as CreateInventoryItemRequest;

      await onAdd(request);
      handleClose();
    } catch (error) {
      console.error('Failed to add inventory item', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleClose = (): void => {
    setStep(1);
    setSearchQuery('');
    setSelectedProduct(null);
    setCurrentStock(0);
    setMinThreshold(10);
    setAutoRestock(false);
    onClose();
  };

  if (!isOpen) {
    return null;
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden flex flex-col max-h-[90vh]">
        <div className="bg-gradient-to-r from-primary-600 to-primary-700 p-6 text-white shrink-0">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-white/20 rounded-lg flex items-center justify-center">
                <Plus className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-lg font-semibold">Add Product to Inventory</h2>
                <p className="text-primary-100 text-sm">{step === 1 ? 'Select a product' : 'Configure stock details'}</p>
              </div>
            </div>
            <button
              type="button"
              onClick={handleClose}
              className="w-8 h-8 bg-white/20 hover:bg-white/30 rounded-lg flex items-center justify-center transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          {step === 1 ? (
            <div className="space-y-4">
              <form onSubmit={handleSearch} className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(event) => setSearchQuery(event.target.value)}
                  placeholder="Search for products..."
                  className="w-full pl-10 pr-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:bg-white focus:border-primary-500 focus:ring-1 focus:ring-primary-500 transition-colors"
                  autoFocus
                />
              </form>

              {isLoading ? (
                <div className="flex justify-center py-8">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
                </div>
              ) : (
                <div className="space-y-2">
                  {products.length === 0 ? (
                    <div className="text-center py-8 text-gray-500">No products found</div>
                  ) : (
                    products.map((product) => (
                      <button
                        key={product.id}
                        type="button"
                        onClick={() => handleSelectProduct(product)}
                        className="w-full flex items-center gap-3 p-3 hover:bg-gray-50 rounded-xl border border-transparent hover:border-gray-200 transition-all text-left"
                      >
                        <div className="w-12 h-12 bg-gray-100 rounded-lg flex items-center justify-center shrink-0">
                          {product.image ? (
                            <img src={product.image} alt={product.name} className="w-full h-full object-cover rounded-lg" />
                          ) : (
                            <Package className="w-6 h-6 text-gray-400" />
                          )}
                        </div>
                        <div>
                          <h3 className="font-medium text-navy-800">{product.name}</h3>
                          <p className="text-sm text-navy-500">{product.brand}</p>
                        </div>
                      </button>
                    ))
                  )}
                </div>
              )}
            </div>
          ) : (
            <div className="space-y-6">
              <div className="flex items-center gap-4 p-4 bg-gray-50 rounded-xl">
                <div className="w-12 h-12 bg-white rounded-lg shadow-sm flex items-center justify-center shrink-0">
                  <Package className="w-6 h-6 text-primary-600" />
                </div>
                <div>
                  <h3 className="font-medium text-navy-800">{selectedProduct?.name}</h3>
                  <p className="text-sm text-navy-500">{selectedProduct?.brand}</p>
                </div>
                <button
                  type="button"
                  onClick={() => setStep(1)}
                  className="ml-auto text-sm text-primary-600 hover:text-primary-700 font-medium"
                >
                  Change
                </button>
              </div>

              <div className="space-y-4">
                <div>
                  <label className="label">Current Stock</label>
                  <input
                    type="number"
                    value={currentStock}
                    onChange={(event) => setCurrentStock(Math.max(0, Number.parseInt(event.target.value, 10) || 0))}
                    className="input"
                    min="0"
                  />
                </div>

                <div>
                  <label className="label">Minimum Threshold</label>
                  <p className="text-xs text-navy-500 mb-2">Alert when stock falls below this level</p>
                  <input
                    type="number"
                    value={minThreshold}
                    onChange={(event) => setMinThreshold(Math.max(0, Number.parseInt(event.target.value, 10) || 0))}
                    className="input"
                    min="0"
                  />
                </div>

                <div className="flex items-center gap-3 pt-2">
                  <button
                    type="button"
                    onClick={() => setAutoRestock(!autoRestock)}
                    className={`relative w-12 h-6 rounded-full transition-colors ${autoRestock ? 'bg-green-500' : 'bg-gray-300'}`}
                  >
                    <span
                      className={`absolute top-1 w-4 h-4 bg-white rounded-full shadow transition-transform ${
                        autoRestock ? 'left-7' : 'left-1'
                      }`}
                    />
                  </button>
                  <span className="text-sm font-medium text-navy-700">Enable Auto-Restock</span>
                </div>
              </div>
            </div>
          )}
        </div>

        {step === 2 && (
          <div className="px-6 py-4 bg-gray-50 flex gap-3 shrink-0">
            <button type="button" onClick={() => setStep(1)} className="flex-1 btn-secondary" disabled={isSubmitting}>
              Back
            </button>
            <button type="button" onClick={handleSubmit} className="flex-1 btn-primary" disabled={isSubmitting}>
              {isSubmitting ? 'Adding...' : 'Add to Inventory'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
