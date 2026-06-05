import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AlertTriangle, Edit2, Eye, EyeOff, History,
  Package, PackagePlus, Plus, Search, Star, Tag, Trash2
} from 'lucide-react';
import { useTranslation } from 'react-i18next';
import InventoryHistoryPanel from '../components/InventoryHistoryPanel';
import PageShell from '../components/PageShell';
import { useAuth } from '../contexts/AuthContext';
import { productService } from '../services/productService';
import { formatCurrency } from '../i18n/display';
import type { Product } from '../types';
import DiscountModal from '../components/DiscountModal/DiscountModal';

export default function ManageProducts(): JSX.Element {
  const { i18n, t } = useTranslation();
  const { user } = useAuth();
  const navigate = useNavigate();
  const isRtl = i18n.dir() === 'rtl';

  const [products, setProducts] = useState<Product[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [historyProduct, setHistoryProduct] = useState<Product | null>(null);
  const [discountProduct, setDiscountProduct] = useState<Product | null>(null);

  const fetchProducts = async (): Promise<void> => {
    if (!user?.id) { setIsLoading(false); return; }
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

  useEffect(() => { void fetchProducts(); }, [user]);

  const filteredProducts = products.filter(
      (product) =>
          product.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
          (product.category ?? '').toLowerCase().includes(searchQuery.toLowerCase()),
  );

  const handleDelete = async (product: Product): Promise<void> => {
    if (!window.confirm(t('products.confirmDelete', { name: product.name }))) return;
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
      if (updated) setProducts((prev) => prev.map((item) => (item.id === product.id ? updated : item)));
    } catch (error) {
      console.error('Failed to toggle published status', error);
    }
  };

  const handleDiscountSuccess = (updated: Product): void => {
    setProducts((prev) => prev.map((item) => (item.id === updated.id ? updated : item)));
  };

  const handleRemoveDiscount = async (product: Product): Promise<void> => {
    try {
      const updated = await productService.removeDiscount(product.id);
      if (updated) setProducts((prev) => prev.map((item) => (item.id === updated.id ? updated : item)));
    } catch (error) {
      console.error('Failed to remove discount', error);
    }
  };

  const totalProducts = products.length;
  const publishedCount = products.filter((p) => p.published).length;
  const outOfStock = products.filter((p) => p.stock === 0).length;
  const avgRating =
      products.length > 0
          ? (products.reduce((sum, p) => sum + (p.rating ?? 0), 0) / products.length).toFixed(1)
          : '0.0';

  if (isLoading) {
    return (
        <div className="flex items-center justify-center min-h-[400px]">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600" />
        </div>
    );
  }

  return (
      <PageShell
          title={t('products.manageTitle')}
          description={t('products.manageDescription')}
          actions={
            <button type="button" onClick={() => navigate('/products/new')} className="btn-primary flex items-center gap-2">
              <Plus className="w-4 h-4" />
              {t('products.addProduct')}
            </button>
          }
      >
        {/* Stats */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="card !p-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-primary-100 rounded-lg flex items-center justify-center">
                <Package className="w-5 h-5 text-primary-600" />
              </div>
              <div>
                <p className="text-sm text-navy-500">{t('products.totalProducts')}</p>
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
                <p className="text-sm text-navy-500">{t('products.published')}</p>
                <p className="text-xl font-bold text-blue-600">{publishedCount} / {totalProducts}</p>
              </div>
            </div>
          </div>
          <div className="card !p-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-red-100 rounded-lg flex items-center justify-center">
                <AlertTriangle className="w-5 h-5 text-red-600" />
              </div>
              <div>
                <p className="text-sm text-navy-500">{t('products.outOfStock')}</p>
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
                <p className="text-sm text-navy-500">{t('products.avgRating')}</p>
                <p className="text-xl font-bold text-amber-600">{avgRating}</p>
              </div>
            </div>
          </div>
        </div>

        {/* Search */}
        <div className="bg-white rounded-xl shadow-card p-4">
          <div className="relative max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-navy-400" />
            <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder={t('products.searchPlaceholder')}
                className="w-full pl-10 pr-4 py-2.5 bg-gray-100 border border-transparent rounded-lg focus:bg-white focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
            />
          </div>
        </div>

        {/* Table */}
        <div className="bg-white rounded-xl shadow-card overflow-hidden">
          <div className="overflow-x-auto">
            <table
                dir={isRtl ? 'rtl' : undefined}
                className={isRtl ? 'w-full min-w-[1100px] table-fixed' : 'w-full min-w-[900px]'}
            >
              <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className={`${isRtl ? 'w-[320px] text-right' : 'text-left'} px-4 py-4 text-sm font-semibold text-navy-700`}>
                  {t('common.product')}
                </th>
                <th className={`${isRtl ? 'w-[130px] text-right' : 'text-left'} px-4 py-4 text-sm font-semibold text-navy-700`}>
                  {t('common.category')}
                </th>
                <th className="w-[110px] px-4 py-4 text-center text-sm font-semibold text-navy-700">
                  {t('common.status')}
                </th>
                <th className="w-[130px] px-4 py-4 text-right text-sm font-semibold text-navy-700">
                  {t('common.price')}
                </th>
                <th className="w-[80px] px-4 py-4 text-center text-sm font-semibold text-navy-700">
                  {t('common.stock')}
                </th>
                <th className="w-[110px] px-4 py-4 text-center text-sm font-semibold text-navy-700">
                  {t('common.rating')}
                </th>
                <th className="w-[180px] px-4 py-4 text-center text-sm font-semibold text-navy-700">
                  {t('common.actions')}
                </th>
              </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
              {filteredProducts.map((product) => (
                  <tr
                      key={product.id}
                      className={`hover:bg-gray-50 transition-colors ${product.stock === 0 ? 'bg-red-50/50' : ''}`}
                  >
                    {/* Product */}
                    <td className="px-4 py-4">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 bg-gray-100 rounded-lg flex items-center justify-center overflow-hidden shrink-0">
                          {product.image ? (
                              <img src={product.image} alt={product.name} className="w-full h-full object-cover" />
                          ) : (
                              <Package className="w-5 h-5 text-navy-400" />
                          )}
                        </div>
                        <div className={`min-w-0 ${isRtl ? 'text-right' : ''}`}>
                          <p className="font-medium text-navy-800 truncate">{product.name}</p>
                          <p className="text-sm text-navy-500 truncate">{product.description}</p>
                        </div>
                      </div>
                    </td>

                    {/* Category */}
                    <td className="px-4 py-4">
                      <span className="badge badge-info text-xs">{product.category ?? t('products.uncategorized')}</span>
                    </td>

                    {/* Status */}
                    <td className="px-4 py-4 text-center">
                      {product.published ? (
                          <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-700">
                                                <Eye className="w-3 h-3" /> {t('products.published')}
                                            </span>
                      ) : (
                          <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-600">
                                                <EyeOff className="w-3 h-3" /> {t('products.draft')}
                                            </span>
                      )}
                    </td>

                    {/* Price */}
                    <td className="px-4 py-4 text-right">
                      {product.discountedPrice != null ? (
                          <div>
                            <span className="font-semibold text-primary-600 text-sm">{formatCurrency(product.discountedPrice)}</span>
                            <span className="block text-xs text-navy-400 line-through">{formatCurrency(product.price)}</span>
                            <span className="inline-block mt-0.5 text-xs bg-primary-100 text-primary-700 px-1.5 py-0.5 rounded-full font-medium">
                                                    -{product.discountPercentage}%
                                                </span>
                            <button
                                type="button"
                                onClick={() => void handleRemoveDiscount(product)}
                                className="block mt-1 text-xs text-red-500 hover:text-red-700 underline"
                                title={t('discount.removeDiscount', 'Remove Discount')}
                            >
                              {t('discount.remove', 'Remove')}
                            </button>
                          </div>
                      ) : (
                          <span className="font-semibold text-navy-800 text-sm">{formatCurrency(product.price)}</span>
                      )}
                    </td>

                    {/* Stock */}
                    <td className="px-4 py-4 text-center">
                                        <span className={`font-semibold text-sm ${
                                            product.stock === 0 ? 'text-red-600' : product.stock < 10 ? 'text-amber-600' : 'text-navy-800'
                                        }`}>
                                            {product.stock}
                                        </span>
                    </td>

                    {/* Rating */}
                    <td className="px-4 py-4 text-center">
                      <div className="flex items-center justify-center gap-1">
                        <Star className="w-3.5 h-3.5 text-amber-400 fill-amber-400 shrink-0" />
                        <span className="text-sm font-medium text-navy-700">{(product.rating ?? 0).toFixed(1)}</span>
                        <span className="text-xs text-navy-400">({product.reviews})</span>
                      </div>
                    </td>

                    {/* Actions */}
                    <td className="px-4 py-4">
                      <div className="flex items-center justify-center gap-1">
                        <button
                            type="button"
                            onClick={() => void handleTogglePublished(product)}
                            className={`p-1.5 rounded-lg transition-colors ${
                                product.published
                                    ? 'text-green-600 hover:text-amber-600 hover:bg-amber-50'
                                    : 'text-navy-400 hover:text-green-600 hover:bg-green-50'
                            }`}
                            title={product.published ? t('products.unpublish') : t('products.publish')}
                        >
                          {product.published ? <Eye className="w-4 h-4" /> : <EyeOff className="w-4 h-4" />}
                        </button>
                        <button
                            type="button"
                            onClick={() => setHistoryProduct(product)}
                            className="p-1.5 text-navy-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                            title={t('products.viewStockHistory')}
                        >
                          <History className="w-4 h-4" />
                        </button>
                        <button
                            type="button"
                            onClick={() => setDiscountProduct(product)}
                            className={`p-1.5 rounded-lg transition-colors ${
                                product.discountedPrice != null
                                    ? 'text-primary-600 bg-primary-50 hover:bg-primary-100'
                                    : 'text-navy-500 hover:text-primary-600 hover:bg-primary-50'
                            }`}
                            title={t('discount.applyDiscount', 'Apply Discount')}
                        >
                          <Tag className="w-4 h-4" />
                        </button>
                        <button
                            type="button"
                            onClick={() => navigate(`/products/edit/${product.id}`)}
                            className="p-1.5 text-navy-500 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
                            title={t('common.edit')}
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button
                            type="button"
                            onClick={() => void handleDelete(product)}
                            className="p-1.5 text-navy-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                            title={t('common.delete')}
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
                <h3 className="text-lg font-semibold text-navy-800 mb-2">
                  {products.length === 0 ? t('products.noProductsYet') : t('products.noProductsFound')}
                </h3>
                <p className="text-navy-500 mb-6">
                  {products.length === 0 ? t('products.addFirstPrompt') : t('products.adjustSearch')}
                </p>
                {products.length === 0 && (
                    <button type="button" onClick={() => navigate('/products/new')} className="btn-primary inline-flex items-center gap-2">
                      <Plus className="w-4 h-4" />
                      {t('products.addFirst')}
                    </button>
                )}
              </div>
          )}
        </div>

        <InventoryHistoryPanel
            isOpen={historyProduct !== null}
            onClose={() => setHistoryProduct(null)}
            title={historyProduct?.name ?? t('products.stockHistory')}
            subtitle={t('products.catalogStock')}
            productId={historyProduct?.id}
            stockTargetType="PRODUCT_CATALOG"
        />

        <DiscountModal
            product={discountProduct}
            onClose={() => setDiscountProduct(null)}
            onSuccess={handleDiscountSuccess}
        />
      </PageShell>
  );
}