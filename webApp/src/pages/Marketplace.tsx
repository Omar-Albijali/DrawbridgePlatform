import { useEffect, useMemo, useRef, useState } from 'react';
import { ChevronLeft, ChevronRight, Grid3X3, List, Search, SlidersHorizontal, X } from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import FilterSidebar, { type FilterOption } from '../components/FilterSidebar/FilterSidebar';
import PageShell from '../components/PageShell';
import ProductCard from '../components/ProductCard/ProductCard';
import { useAuth } from '../contexts/AuthContext';
import { productService } from '../services/productService';
import { UserRole, type Category, type PaginatedResponse, type Product } from '../types';

const PAGE_SIZE = 12;
const DEFAULT_MAX_PRICE = 5000;
const SEARCH_DEBOUNCE_MS = 350;
const PLACEHOLDER_CARDS = Array.from({ length: PAGE_SIZE }, (_, index) => index);

type PaginationItem = number | 'ellipsis-start' | 'ellipsis-end';

function buildPaginationItems(totalPages: number, currentPage: number): PaginationItem[] {
  if (totalPages <= 1) {
    return [];
  }

  const items: PaginationItem[] = [];
  const start = Math.max(0, currentPage - 1);
  const end = Math.min(totalPages - 1, currentPage + 1);

  items.push(0);

  if (start > 1) {
    items.push('ellipsis-start');
  }

  for (let page = start; page <= end; page += 1) {
    if (page !== 0 && page !== totalPages - 1) {
      items.push(page);
    }
  }

  if (end < totalPages - 2) {
    items.push('ellipsis-end');
  }

  if (totalPages > 1) {
    items.push(totalPages - 1);
  }

  return Array.from(new Set(items));
}

export default function Marketplace(): JSX.Element {
  const { t } = useTranslation();
  const { isAuthenticated, user } = useAuth();
  const isWholesaler = user?.role === UserRole.WHOLESALER;
  const navigate = useNavigate();
  const location = useLocation();
  const resultsRef = useRef<HTMLDivElement | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedSearchQuery, setDebouncedSearchQuery] = useState('');
  const [selectedCategories, setSelectedCategories] = useState<string[]>([]);
  const [selectedBrands, setSelectedBrands] = useState<string[]>([]);
  const [priceRange, setPriceRange] = useState<[number, number]>([0, DEFAULT_MAX_PRICE]);
  const [sortBy, setSortBy] = useState('featured');
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [showMobileFilters, setShowMobileFilters] = useState(false);
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [brands, setBrands] = useState<string[]>([]);
  const [pagination, setPagination] = useState<PaginatedResponse<Product> | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [reloadNonce, setReloadNonce] = useState(0);
  const [isInitialLoading, setIsInitialLoading] = useState(true);
  const [isFetchingProducts, setIsFetchingProducts] = useState(false);
  const [productsError, setProductsError] = useState<string | null>(null);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setDebouncedSearchQuery(searchQuery.trim());
    }, SEARCH_DEBOUNCE_MS);

    return () => window.clearTimeout(timeoutId);
  }, [searchQuery]);

  useEffect(() => {
    const fetchMarketplaceFilters = async (): Promise<void> => {
      const [categoriesResult, brandsResult] = await Promise.allSettled([
        productService.getCategories(),
        productService.getBrands(),
      ]);

      if (categoriesResult.status === 'fulfilled') {
        setCategories(Array.isArray(categoriesResult.value) ? categoriesResult.value : []);
      } else {
        console.error('Failed to fetch categories', categoriesResult.reason);
      }

      if (brandsResult.status === 'fulfilled') {
        setBrands(Array.isArray(brandsResult.value) ? brandsResult.value : []);
      } else {
        console.error('Failed to fetch brands', brandsResult.reason);
      }
    };

    void fetchMarketplaceFilters();
  }, []);

  useEffect(() => {
    const controller = new AbortController();

    const fetchMarketplaceProducts = async (): Promise<void> => {
      setIsFetchingProducts(true);
      setProductsError(null);

      try {
        const data = await productService.getMarketplacePage(
          {
            page: currentPage,
            size: PAGE_SIZE,
            search: debouncedSearchQuery || undefined,
            category: selectedCategories.length > 0 ? selectedCategories : undefined,
            brand: selectedBrands.length > 0 ? selectedBrands : undefined,
            minPrice: priceRange[0] > 0 ? priceRange[0] : undefined,
            maxPrice: priceRange[1] < DEFAULT_MAX_PRICE ? priceRange[1] : undefined,
            sort: sortBy,
          },
          controller.signal,
        );

        const safeProducts = Array.isArray(data.content) ? data.content : [];
        setProducts(safeProducts);
        setPagination({ ...data, content: safeProducts });
      } catch (error) {
        if (controller.signal.aborted) {
          return;
        }

        console.error('Failed to fetch marketplace products', error);
        setProducts([]);
        setPagination(null);
        setProductsError(t('marketplace.loadError'));
      } finally {
        if (!controller.signal.aborted) {
          setIsFetchingProducts(false);
          setIsInitialLoading(false);
        }
      }
    };

    void fetchMarketplaceProducts();

    return () => controller.abort();
  }, [currentPage, debouncedSearchQuery, priceRange, reloadNonce, selectedBrands, selectedCategories, sortBy, t]);

  const categoryOptions = useMemo(
    () =>
      categories
        .map((category) => ({
          label: category.name.trim(),
          value: category.id,
        }))
        .filter((category) => category.label.length > 0 && category.value.length > 0)
        .sort((a, b) => a.label.localeCompare(b.label)),
    [categories],
  );

  const brandOptions = useMemo(
    () => {
      const knownBrands = [...brands, ...products.map((product) => product.brand)];

      return Array.from(
        new Map(
          knownBrands
            .map((brand) => brand.trim())
            .filter((brand) => brand.length > 0)
            .sort((a, b) => a.localeCompare(b))
            .map((brand) => [brand.toLowerCase(), { label: brand, value: brand }] satisfies [string, FilterOption]),
        ).values(),
      );
    },
    [brands, products],
  );

  const clearFilters = (): void => {
    setSelectedCategories([]);
    setSelectedBrands([]);
    setPriceRange([0, DEFAULT_MAX_PRICE]);
    setSearchQuery('');
    setDebouncedSearchQuery('');
    setCurrentPage(0);
  };

  const activeFilterCount =
    selectedCategories.length + selectedBrands.length + (priceRange[0] > 0 || priceRange[1] < DEFAULT_MAX_PRICE ? 1 : 0);

  const totalPages = pagination?.totalPages ?? 0;
  const paginationItems = useMemo(() => buildPaginationItems(totalPages, currentPage), [currentPage, totalPages]);
  const showingStart = pagination && pagination.totalElements > 0 ? pagination.currentPage * pagination.pageSize + 1 : 0;
  const showingEnd = pagination && pagination.totalElements > 0 ? pagination.currentPage * pagination.pageSize + products.length : 0;

  const redirectToLogin = (returnPath?: string): void => {
    const currentPath = `${location.pathname}${location.search}${location.hash}`;
    const nextPath = returnPath ?? currentPath;
    navigate(`/login?returnTo=${encodeURIComponent(nextPath)}`, {
      state: { from: { pathname: nextPath } },
    });
  };

  const handleSearchChange = (value: string): void => {
    setSearchQuery(value);
    setCurrentPage(0);
  };

  const handleSortChange = (value: string): void => {
    setSortBy(value);
    setCurrentPage(0);
  };

  const handleCategoriesChange = (nextCategories: string[]): void => {
    setSelectedCategories(nextCategories);
    setCurrentPage(0);
  };

  const handleBrandsChange = (nextBrands: string[]): void => {
    setSelectedBrands(nextBrands);
    setCurrentPage(0);
  };

  const handlePriceRangeChange = (nextRange: [number, number]): void => {
    setPriceRange(nextRange);
    setCurrentPage(0);
  };

  const goToPage = (page: number): void => {
    if (page < 0 || !pagination || page >= pagination.totalPages || page === currentPage) {
      return;
    }

    setCurrentPage(page);
    resultsRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  return (
    <PageShell
      title={t('marketplace.title')}
      description={t('marketplace.description')}
      actions={
        !isAuthenticated ? (
          <button
            type="button"
            onClick={() => redirectToLogin()}
            className="rounded-full border border-primary-200 bg-primary-50 px-4 py-2 text-sm font-semibold text-primary-700 transition-colors hover:bg-primary-100"
          >
            {t('marketplace.signInToCheckout')}
          </button>
        ) : undefined
      }
    >
      <div className="card p-4">
        <div className="flex flex-col gap-4 md:flex-row">
          <div className="relative flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-navy-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(event) => handleSearchChange(event.target.value)}
              placeholder={t('marketplace.searchPlaceholder')}
              className="w-full rounded-lg border border-transparent bg-gray-100 py-2.5 pl-10 pr-4 transition-colors focus:border-primary-500 focus:bg-white focus:ring-1 focus:ring-primary-500"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => handleSearchChange('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-navy-400 hover:text-navy-600"
              >
                <X className="h-4 w-4" />
              </button>
            )}
          </div>

          <select
            value={sortBy}
            onChange={(event) => handleSortChange(event.target.value)}
            className="rounded-lg border border-transparent bg-gray-100 px-4 py-2.5 text-navy-700 focus:border-primary-500 focus:bg-white focus:ring-1 focus:ring-primary-500"
          >
            <option value="featured">{t('marketplace.sort.featured')}</option>
            <option value="price-low">{t('marketplace.sort.priceLow')}</option>
            <option value="price-high">{t('marketplace.sort.priceHigh')}</option>
            <option value="rating">{t('marketplace.sort.rating')}</option>
            <option value="newest">{t('marketplace.sort.newest')}</option>
          </select>

          <div className="flex items-center rounded-lg bg-gray-100 p-1">
            <button
              type="button"
              onClick={() => setViewMode('grid')}
              className={`rounded-md p-2 transition-colors ${
                viewMode === 'grid' ? 'bg-white text-primary-600 shadow-sm' : 'text-navy-500'
              }`}
            >
              <Grid3X3 className="h-5 w-5" />
            </button>
            <button
              type="button"
              onClick={() => setViewMode('list')}
              className={`rounded-md p-2 transition-colors ${
                viewMode === 'list' ? 'bg-white text-primary-600 shadow-sm' : 'text-navy-500'
              }`}
            >
              <List className="h-5 w-5" />
            </button>
          </div>

          <button
            type="button"
            onClick={() => setShowMobileFilters(true)}
            className="flex items-center gap-2 rounded-lg bg-gray-100 px-4 py-2.5 text-navy-700 lg:hidden"
          >
            <SlidersHorizontal className="h-5 w-5" />
            {t('marketplace.filters')}
            {activeFilterCount > 0 && (
              <span className="flex h-5 w-5 items-center justify-center rounded-full bg-primary-600 text-xs text-white">
                {activeFilterCount}
              </span>
            )}
          </button>
        </div>
      </div>

      <div className="flex gap-6">
        <div className="hidden w-72 shrink-0 lg:block">
          <FilterSidebar
            categoryOptions={categoryOptions}
            selectedCategories={selectedCategories}
            setSelectedCategories={handleCategoriesChange}
            brandOptions={brandOptions}
            selectedBrands={selectedBrands}
            setSelectedBrands={handleBrandsChange}
            priceRange={priceRange}
            setPriceRange={handlePriceRangeChange}
            onClearFilters={clearFilters}
          />
        </div>

        <div ref={resultsRef} className="flex-1">
          <div className="glass-panel mb-4 flex flex-col gap-3 rounded-2xl px-5 py-4 shadow-card sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-sm font-medium text-navy-800 dark:text-slate-100">
                {pagination && pagination.totalElements > 0 ? (
                  <>
                    {t('marketplace.showing', { start: showingStart, end: showingEnd, total: pagination.totalElements })}
                  </>
                ) : (
                  t('marketplace.description')
                )}
              </p>
              <p className="mt-1 text-xs text-navy-500 dark:text-slate-300">
                {pagination && pagination.totalPages > 0
                  ? t('common.pageOf', { page: pagination.currentPage + 1, total: pagination.totalPages })
                  : t('marketplace.freshResults')}
              </p>
            </div>
            {isFetchingProducts && !isInitialLoading && (
              <div className="inline-flex items-center gap-2 rounded-full bg-primary-50 px-3 py-1.5 text-xs font-semibold text-primary-700 dark:bg-primary-500/15 dark:text-primary-200">
                <span className="h-2 w-2 animate-pulse rounded-full bg-primary-500" />
                {t('marketplace.updatingResults')}
              </div>
            )}
          </div>

          <div className="relative min-h-[320px]">
            {isInitialLoading ? (
              <div className={viewMode === 'grid' ? 'grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3' : 'space-y-4'}>
                {PLACEHOLDER_CARDS.map((card) => (
                  <div key={card} className="overflow-hidden rounded-2xl border border-gray-200/70 bg-white shadow-card dark:border-white/10 dark:bg-slate-900/70">
                    <div className="aspect-[4/3] animate-pulse bg-gray-200 dark:bg-slate-800" />
                    <div className="space-y-3 p-4">
                      <div className="h-4 w-24 animate-pulse rounded-full bg-gray-200 dark:bg-slate-800" />
                      <div className="h-5 w-3/4 animate-pulse rounded-full bg-gray-200 dark:bg-slate-800" />
                      <div className="h-4 w-1/2 animate-pulse rounded-full bg-gray-200 dark:bg-slate-800" />
                      <div className="h-10 animate-pulse rounded-xl bg-gray-200 dark:bg-slate-800" />
                    </div>
                  </div>
                ))}
              </div>
            ) : productsError ? (
              <div className="card py-16 text-center">
                <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-red-50">
                  <X className="h-8 w-8 text-red-400" />
                </div>
                <h3 className="mb-2 text-lg font-semibold text-navy-800">{t('marketplace.loadErrorTitle')}</h3>
                <p className="mb-4 text-navy-500">{productsError}</p>
                <button type="button" onClick={() => setReloadNonce((value) => value + 1)} className="btn-primary">
                  {t('marketplace.tryAgain')}
                </button>
              </div>
            ) : products.length > 0 ? (
              <>
                {isFetchingProducts && (
                  <div className="pointer-events-none absolute inset-0 z-10 rounded-2xl bg-white/65 backdrop-blur-[1px] dark:bg-slate-950/60" />
                )}
                <div className={viewMode === 'grid' ? 'grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3' : 'space-y-4'}>
                  {products.map((product) => (
                    <ProductCard
                      key={product.id}
                      product={product}
                      isAuthenticated={isAuthenticated}
                      canAddToCart={!isWholesaler}
                      onAuthRequired={redirectToLogin}
                    />
                  ))}
                </div>
              </>
            ) : (
              <div className="card py-16 text-center">
                <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-gray-100">
                  <Search className="h-8 w-8 text-navy-400" />
                </div>
                <h3 className="mb-2 text-lg font-semibold text-navy-800">{t('marketplace.noProductsFound')}</h3>
                <p className="mb-4 text-navy-500">{t('marketplace.adjustSearch')}</p>
                <button type="button" onClick={clearFilters} className="btn-primary">
                  {t('marketplace.clearFilters')}
                </button>
              </div>
            )}
          </div>

          {pagination && pagination.totalPages > 1 && (
            <div className="glass-panel mt-6 flex justify-center rounded-2xl px-3 py-3 shadow-card">
              <div className="flex flex-wrap items-center justify-center gap-2">
                <button
                  type="button"
                  onClick={() => goToPage(currentPage - 1)}
                  disabled={pagination.isFirst || isFetchingProducts}
                  className="inline-flex items-center gap-2 rounded-xl border border-gray-200 bg-white px-3 py-2 text-sm font-semibold text-navy-700 transition-all hover:border-primary-200 hover:text-primary-700 disabled:cursor-not-allowed disabled:opacity-50 dark:border-white/10 dark:bg-slate-950/70 dark:text-slate-200 dark:hover:border-primary-400/30 dark:hover:text-primary-200"
                >
                  <ChevronLeft className="h-4 w-4" />
                  {t('marketplace.previous')}
                </button>

                {paginationItems.map((item) =>
                  typeof item === 'number' ? (
                    <button
                      key={item}
                      type="button"
                      onClick={() => goToPage(item)}
                      disabled={isFetchingProducts}
                      className={`h-10 min-w-[40px] rounded-xl px-2.5 text-sm font-semibold transition-all ${
                        item === currentPage
                          ? 'bg-primary-600 text-white'
                          : 'border border-gray-200 bg-white text-navy-700 hover:border-primary-200 hover:text-primary-700 dark:border-white/10 dark:bg-slate-950/70 dark:text-slate-200 dark:hover:border-primary-400/30 dark:hover:text-primary-200'
                      }`}
                    >
                      {item + 1}
                    </button>
                  ) : (
                    <span key={item} className="px-1 text-sm font-semibold text-navy-300 dark:text-slate-500">
                      ...
                    </span>
                  ),
                )}

                <button
                  type="button"
                  onClick={() => goToPage(currentPage + 1)}
                  disabled={pagination.isLast || isFetchingProducts}
                  className="inline-flex items-center gap-2 rounded-xl border border-gray-200 bg-white px-3 py-2 text-sm font-semibold text-navy-700 transition-all hover:border-primary-200 hover:text-primary-700 disabled:cursor-not-allowed disabled:opacity-50 dark:border-white/10 dark:bg-slate-950/70 dark:text-slate-200 dark:hover:border-primary-400/30 dark:hover:text-primary-200"
                >
                  {t('marketplace.next')}
                  <ChevronRight className="h-4 w-4" />
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {showMobileFilters && (
        <div className="fixed inset-0 z-50 bg-black/50 lg:hidden">
          <div className="absolute right-0 top-0 h-full w-80 overflow-y-auto bg-white dark:bg-slate-900">
            <div className="flex items-center justify-between border-b border-gray-200 p-4 dark:border-white/10">
              <h3 className="font-semibold text-navy-800 dark:text-slate-100">{t('marketplace.filters')}</h3>
              <button type="button" onClick={() => setShowMobileFilters(false)}>
                <X className="h-5 w-5 text-navy-600 dark:text-slate-300" />
              </button>
            </div>
            <div className="p-4">
              <FilterSidebar
                categoryOptions={categoryOptions}
                selectedCategories={selectedCategories}
                setSelectedCategories={handleCategoriesChange}
                brandOptions={brandOptions}
                selectedBrands={selectedBrands}
                setSelectedBrands={handleBrandsChange}
                priceRange={priceRange}
                setPriceRange={handlePriceRangeChange}
                onClearFilters={clearFilters}
              />
            </div>
          </div>
        </div>
      )}
    </PageShell>
  );
}
