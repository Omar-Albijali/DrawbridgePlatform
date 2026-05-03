import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertTriangle, Edit2, Eye, EyeOff, History, Package, PackagePlus, Plus, Search, Star, Trash2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import InventoryHistoryPanel from '../components/InventoryHistoryPanel';
import PageShell from '../components/PageShell';
import { useAuth } from '../contexts/AuthContext';
import { productService } from '../services/productService';
import { formatCurrency } from '../i18n/display';
import type { Product } from '../types';

export default function ManageProducts(): JSX.Element {
  const { i18n, t } = useTranslation();
  const { user } = useAuth();
  const navigate = useNavigate();
  const isRtl = i18n.dir() === 'rtl';
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
    if (!window.confirm(t('products.confirmDelete', { name: product.name }))) {
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
      title={t('products.manageTitle')}
      description={t('products.manageDescription')}
      actions={
        <button type="button" onClick={() => navigate('/products/new')} className="btn-primary flex items-center gap-2">
          <Plus className="w-4 h-4" />
          {t('products.addProduct')}
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

      <div className="bg-white rounded-xl shadow-card p-4">
        <div className="relative max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-navy-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            placeholder={t('products.searchPlaceholder')}
            className="w-full pl-10 pr-4 py-2.5 bg-gray-100 border border-transparent rounded-lg focus:bg-white focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
          />
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-card overflow-hidden">
        <div className="overflow-x-auto">
          <table dir={isRtl ? 'rtl' : undefined} className={isRtl ? 'w-full min-w-[960px] table-fixed' : 'w-full'}>
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className={`${isRtl ? 'w-[360px] text-right' : 'text-left'} px-6 py-4 text-sm font-semibold text-navy-700`}>
                  {t('common.product')}
                </th>
                <th className={`${isRtl ? 'w-[150px] text-right' : 'text-left'} px-6 py-4 text-sm font-semibold text-navy-700`}>
                  {t('common.category')}
                </th>
                <th className={`${isRtl ? 'w-[140px]' : ''} px-6 py-4 text-center text-sm font-semibold text-navy-700`}>
                  {t('common.status')}
                </th>
                <th className={`${isRtl ? 'w-[140px]' : ''} px-6 py-4 text-right text-sm font-semibold text-navy-700`}>
                  {t('common.price')}
                </th>
                <th className={`${isRtl ? 'w-[100px]' : ''} px-6 py-4 text-center text-sm font-semibold text-navy-700`}>
                  {t('common.stock')}
                </th>
                <th className={`${isRtl ? 'w-[130px]' : ''} px-6 py-4 text-center text-sm font-semibold text-navy-700`}>
                  {t('common.rating')}
                </th>
                <th className={`${isRtl ? 'w-[160px]' : ''} px-6 py-4 text-center text-sm font-semibold text-navy-700`}>
                  {t('common.actions')}
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filteredProducts.map((product) => (
                <tr key={product.id} className={`hover:bg-gray-50 transition-colors ${product.stock === 0 ? 'bg-red-50/50' : ''}`}>
                  <td className={`${isRtl ? 'w-[360px]' : ''} px-6 py-4`}>
                    <div className={`${isRtl ? 'w-full min-w-0' : ''} flex items-center gap-3`}>
                      <div className="w-12 h-12 bg-gray-100 rounded-lg flex items-center justify-center overflow-hidden shrink-0">
                        {product.image ? (
                          <img src={product.image} alt={product.name} className="w-full h-full object-cover" />
                        ) : (
                          <Package className="w-6 h-6 text-navy-400" />
                        )}
                      </div>
                      <div className={isRtl ? 'min-w-0 flex-1 text-right' : ''}>
                        <p className={`${isRtl ? 'truncate' : ''} font-medium text-navy-800`}>{product.name}</p>
                        <p className={`${isRtl ? 'truncate' : 'line-clamp-1'} text-sm text-navy-500`}>{product.description}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <span className="badge badge-info">{product.category ?? t('products.uncategorized')}</span>
                  </td>
                  <td className="px-6 py-4 text-center">
                    {product.published ? (
                      <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium bg-green-100 text-green-700">
                        <Eye className="w-3 h-3" /> {t('products.published')}
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-600">
                        <EyeOff className="w-3 h-3" /> {t('products.draft')}
                      </span>
                    )}
                  </td>
                  <td className="px-6 py-4 text-right">
                    <div>
                      <span className="font-semibold text-navy-800">{formatCurrency(product.price)}</span>
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
                        title={product.published ? t('products.unpublish') : t('products.publish')}
                      >
                        {product.published ? <Eye className="w-4 h-4" /> : <EyeOff className="w-4 h-4" />}
                      </button>
                      <button
                        type="button"
                        onClick={() => setHistoryProduct(product)}
                        className="p-2 text-navy-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                        title={t('products.viewStockHistory')}
                      >
                        <History className="w-4 h-4" />
                      </button>
                      <button
                        type="button"
                        onClick={() => navigate(`/products/edit/${product.id}`)}
                        className="p-2 text-navy-500 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
                        title={t('common.edit')}
                      >
                        <Edit2 className="w-4 h-4" />
                      </button>
                      <button
                        type="button"
                        onClick={() => void handleDelete(product)}
                        className="p-2 text-navy-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
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
            <h3 className="text-lg font-semibold text-navy-800 mb-2">{products.length === 0 ? t('products.noProductsYet') : t('products.noProductsFound')}</h3>
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
    </PageShell>
  );
}
