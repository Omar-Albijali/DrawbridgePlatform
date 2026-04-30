import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertTriangle, Edit2, Eye, EyeOff, History, Package, PackagePlus, Plus, Search, Star, Trash2 } from 'lucide-react';
import InventoryHistoryPanel from '../components/InventoryHistoryPanel';
import PageShell from '../components/PageShell';
import { useAuth } from '../contexts/AuthContext';
import { productService } from '../services/productService';
import type { Product } from '../types';

export default function ManageProducts(): JSX.Element {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [products, setProducts] = useState<Product[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [historyProduct, setHistoryProduct] = useState<Product | null>(null);

  const fetchProducts = async (): Promise<void> => {
    if (!user?.id) {
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      const data = await productService.getByWholesaler(user.id);
      setProducts(data);
    } catch (error) {
      console.error('Failed to fetch products', error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void fetchProducts();
  }, [user]);

  const filteredProducts = products.filter(
    (product) =>
      product.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (product.category ?? '').toLowerCase().includes(searchQuery.toLowerCase()),
  );

  const handleDelete = async (product: Product): Promise<void> => {
    if (!window.confirm(`Are you sure you want to delete "${product.name}"? This action cannot be undone.`)) {
      return;
    }

    try {
      await productService.delete(product.id);
      setProducts((prev) => prev.filter((item) => item.id !== product.id));
    } catch (error) {
      console.error('Failed to delete product', error);
    }
  };

  const handleTogglePublished = async (product: Product): Promise<void> => {
    try {
      const updated = await productService.togglePublished(product.id);
      if (updated) {
        setProducts((prev) => prev.map((item) => (item.id === product.id ? updated : item)));
      }
    } catch (error) {
      console.error('Failed to toggle published status', error);
    }
  };

  const totalProducts = products.length;
  const publishedCount = products.filter((product) => product.published).length;
  const outOfStock = products.filter((product) => product.stock === 0).length;
  const avgRating =
    products.length > 0 ? (products.reduce((sum, product) => sum + (product.rating ?? 0), 0) / products.length).toFixed(1) : '0.0';

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600" />
      </div>
    );
  }

  return (
    <PageShell
      title="Manage Products"
      description="Create, edit, and manage your product listings"
      actions={
        <button type="button" onClick={() => navigate('/products/new')} className="btn-primary flex items-center gap-2">
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
              <p className="text-sm text-navy-500">Total Products</p>
              <p className="text-xl font-bold text-navy-800">{totalProducts}</p>
            </div>
          </div>
        </div>
        <div className="card !p-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
              <Eye className="w-5 h-5 text-blue-600" />
            </div>
            <div>
              <p className="text-sm text-navy-500">Published</p>
              <p className="text-xl font-bold text-blue-600">
                {publishedCount} / {totalProducts}
              </p>
            </div>
          </div>
        </div>
        <div className="card !p-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-red-100 rounded-lg flex items-center justify-center">
              <AlertTriangle className="w-5 h-5 text-red-600" />
            </div>
            <div>
              <p className="text-sm text-navy-500">Out of Stock</p>
              <p className="text-xl font-bold text-red-600">{outOfStock}</p>
            </div>
          </div>
        </div>
        <div className="card !p-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-amber-100 rounded-lg flex items-center justify-center">
              <Star className="w-5 h-5 text-amber-600" />
            </div>
            <div>
              <p className="text-sm text-navy-500">Avg Rating</p>
              <p className="text-xl font-bold text-amber-600">{avgRating}</p>
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
            placeholder="Search products by name or category..."
            className="w-full pl-10 pr-4 py-2.5 bg-gray-100 border border-transparent rounded-lg focus:bg-white focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
          />
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="text-left px-6 py-4 text-sm font-semibold text-navy-700">Product</th>
                <th className="text-left px-6 py-4 text-sm font-semibold text-navy-700">Category</th>
                <th className="text-center px-6 py-4 text-sm font-semibold text-navy-700">Status</th>
                <th className="text-right px-6 py-4 text-sm font-semibold text-navy-700">Price</th>
                <th className="text-center px-6 py-4 text-sm font-semibold text-navy-700">Stock</th>
                <th className="text-center px-6 py-4 text-sm font-semibold text-navy-700">Rating</th>
                <th className="text-center px-6 py-4 text-sm font-semibold text-navy-700">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filteredProducts.map((product) => (
                <tr key={product.id} className={`hover:bg-gray-50 transition-colors ${product.stock === 0 ? 'bg-red-50/50' : ''}`}>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 bg-gray-100 rounded-lg flex items-center justify-center overflow-hidden shrink-0">
                        {product.image ? (
                          <img src={product.image} alt={product.name} className="w-full h-full object-cover" />
                        ) : (
                          <Package className="w-6 h-6 text-navy-400" />
                        )}
                      </div>
                      <div>
                        <p className="font-medium text-navy-800">{product.name}</p>
                        <p className="text-sm text-navy-500 line-clamp-1">{product.description}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <span className="badge badge-info">{product.category ?? 'Uncategorized'}</span>
                  </td>
                  <td className="px-6 py-4 text-center">
                    {product.published ? (
                      <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium bg-green-100 text-green-700">
                        <Eye className="w-3 h-3" /> Published
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-600">
                        <EyeOff className="w-3 h-3" /> Draft
                      </span>
                    )}
                  </td>
                  <td className="px-6 py-4 text-right">
                    <div>
                      <span className="font-semibold text-navy-800">{product.price.toFixed(2)} SAR</span>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-center">
                    <span
                      className={`font-semibold ${
                        product.stock === 0 ? 'text-red-600' : product.stock < 10 ? 'text-amber-600' : 'text-navy-800'
                      }`}
                    >
                      {product.stock}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-center">
                    <div className="flex items-center justify-center gap-1">
                      <Star className="w-4 h-4 text-amber-400 fill-amber-400" />
                      <span className="text-sm font-medium text-navy-700">{(product.rating ?? 0).toFixed(1)}</span>
                      <span className="text-xs text-navy-400">({product.reviews})</span>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center justify-center gap-2">
                      <button
                        type="button"
                        onClick={() => void handleTogglePublished(product)}
                        className={`p-2 rounded-lg transition-colors ${
                          product.published
                            ? 'text-green-600 hover:text-amber-600 hover:bg-amber-50'
                            : 'text-navy-400 hover:text-green-600 hover:bg-green-50'
                        }`}
                        title={product.published ? 'Unpublish' : 'Publish'}
                      >
                        {product.published ? <Eye className="w-4 h-4" /> : <EyeOff className="w-4 h-4" />}
                      </button>
                      <button
                        type="button"
                        onClick={() => setHistoryProduct(product)}
                        className="p-2 text-navy-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                        title="View stock history"
                      >
                        <History className="w-4 h-4" />
                      </button>
                      <button
                        type="button"
                        onClick={() => navigate(`/products/edit/${product.id}`)}
                        className="p-2 text-navy-500 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
                        title="Edit"
                      >
                        <Edit2 className="w-4 h-4" />
                      </button>
                      <button
                        type="button"
                        onClick={() => void handleDelete(product)}
                        className="p-2 text-navy-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                        title="Delete"
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

        {filteredProducts.length === 0 && (
          <div className="text-center py-16">
            <PackagePlus className="w-12 h-12 text-navy-300 mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-navy-800 mb-2">{products.length === 0 ? 'No products yet' : 'No products found'}</h3>
            <p className="text-navy-500 mb-6">
              {products.length === 0 ? 'Start by adding your first product to the catalog.' : 'Try adjusting your search query.'}
            </p>
            {products.length === 0 && (
              <button type="button" onClick={() => navigate('/products/new')} className="btn-primary inline-flex items-center gap-2">
                <Plus className="w-4 h-4" />
                Add Your First Product
              </button>
            )}
          </div>
        )}
      </div>

      <InventoryHistoryPanel
        isOpen={historyProduct !== null}
        onClose={() => setHistoryProduct(null)}
        title={historyProduct?.name ?? 'Stock history'}
        subtitle="Product catalog stock"
        productId={historyProduct?.id}
        stockTargetType="PRODUCT_CATALOG"
      />
    </PageShell>
  );
}
