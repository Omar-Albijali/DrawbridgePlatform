import { useEffect, useMemo, useState } from 'react';
import { Grid3X3, List, Search, SlidersHorizontal, X } from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import FilterSidebar from '../components/FilterSidebar/FilterSidebar';
import PageShell from '../components/PageShell';
import ProductCard from '../components/ProductCard/ProductCard';
import { useAuth } from '../contexts/AuthContext';
import { productService } from '../services/productService';
import { UserRole, type Category, type Product } from '../types';

export default function Marketplace(): JSX.Element {
  const { isAuthenticated, user } = useAuth();
  const isWholesaler = user?.role === UserRole.WHOLESALER;
  const navigate = useNavigate();
  const location = useLocation();
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategories, setSelectedCategories] = useState<string[]>([]);
  const [selectedBrands, setSelectedBrands] = useState<string[]>([]);
  const [priceRange, setPriceRange] = useState<[number, number]>([0, 5000]);
  const [sortBy, setSortBy] = useState('featured');
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [showMobileFilters, setShowMobileFilters] = useState(false);
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);

  useEffect(() => {
    const fetchMarketplaceData = async (): Promise<void> => {
      const [productsResult, categoriesResult] = await Promise.allSettled([
        productService.getAll(),
        productService.getCategories(),
      ]);

      if (productsResult.status === 'fulfilled') {
        setProducts(productsResult.value);
      } else {
        console.error('Failed to fetch products', productsResult.reason);
      }

      if (categoriesResult.status === 'fulfilled') {
        setCategories(categoriesResult.value);
      } else {
        console.error('Failed to fetch categories', categoriesResult.reason);
      }
    };

    void fetchMarketplaceData();
  }, []);

  const filteredProducts = useMemo(() => {
    return products
      .filter((product) => {
        if (
          searchQuery &&
          !product.name.toLowerCase().includes(searchQuery.toLowerCase()) &&
          !product.description.toLowerCase().includes(searchQuery.toLowerCase())
        ) {
          return false;
        }

        if (selectedCategories.length > 0 && !selectedCategories.includes(product.category)) {
          return false;
        }

        if (selectedBrands.length > 0 && !selectedBrands.includes(product.brand)) {
          return false;
        }

        if (product.price < priceRange[0] || product.price > priceRange[1]) {
          return false;
        }

        return true;
      })
      .sort((a, b) => {
        switch (sortBy) {
          case 'price-low':
            return a.price - b.price;
          case 'price-high':
            return b.price - a.price;
          case 'rating':
            return (b.rating ?? 0) - (a.rating ?? 0);
          case 'newest':
            return b.id.localeCompare(a.id);
          case 'featured':
  const aScore = (a.rating ?? 0) * Math.log1p(a.reviews ?? 0);
  const bScore = (b.rating ?? 0) * Math.log1p(b.reviews ?? 0);
  return bScore - aScore;
default:
  return 0;
        }
      });
  }, [priceRange, products, searchQuery, selectedBrands, selectedCategories, sortBy]);

  const categoryOptions = useMemo(
    () => {
      const categoryNamesFromCatalog = categories.map((category) => category.name.trim()).filter((category) => category.length > 0);
      const categoryNamesFromProducts = products.map((product) => product.category.trim()).filter((category) => category.length > 0);

      return Array.from(new Set([...categoryNamesFromCatalog, ...categoryNamesFromProducts])).sort((a, b) => a.localeCompare(b));
    },
    [categories, products],
  );

  const brandOptions = useMemo(
    () => Array.from(new Set(products.map((product) => product.brand.trim()).filter((brand) => brand.length > 0))).sort((a, b) => a.localeCompare(b)),
    [products],
  );

  const clearFilters = (): void => {
    setSelectedCategories([]);
    setSelectedBrands([]);
    setPriceRange([0, 5000]);
    setSearchQuery('');
  };

  const activeFilterCount =
    selectedCategories.length + selectedBrands.length + (priceRange[0] > 0 || priceRange[1] < 5000 ? 1 : 0);

  const redirectToLogin = (returnPath?: string): void => {
    const currentPath = `${location.pathname}${location.search}${location.hash}`;
    const nextPath = returnPath ?? currentPath;
    navigate(`/login?returnTo=${encodeURIComponent(nextPath)}`, {
      state: { from: { pathname: nextPath } },
    });
  };

  return (
    <PageShell
      title="Marketplace"
      description="Discover products from trusted wholesalers"
      actions={
        !isAuthenticated ? (
          <button
            type="button"
            onClick={() => redirectToLogin()}
            className="rounded-full border border-primary-200 bg-primary-50 px-4 py-2 text-sm font-semibold text-primary-700 transition-colors hover:bg-primary-100"
          >
            Sign in to add items and checkout
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
              onChange={(event) => setSearchQuery(event.target.value)}
              placeholder="Search products..."
              className="w-full rounded-lg border border-transparent bg-gray-100 py-2.5 pl-10 pr-4 transition-colors focus:border-primary-500 focus:bg-white focus:ring-1 focus:ring-primary-500"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-navy-400 hover:text-navy-600"
              >
                <X className="h-4 w-4" />
              </button>
            )}
          </div>

          <select
            value={sortBy}
            onChange={(event) => setSortBy(event.target.value)}
            className="rounded-lg border border-transparent bg-gray-100 px-4 py-2.5 text-navy-700 focus:border-primary-500 focus:bg-white focus:ring-1 focus:ring-primary-500"
          >
            <option value="featured">Featured</option>
            <option value="price-low">Price: Low to High</option>
            <option value="price-high">Price: High to Low</option>
            <option value="rating">Top Rated</option>
            <option value="newest">Newest</option>
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
            Filters
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
            setSelectedCategories={setSelectedCategories}
            brandOptions={brandOptions}
            selectedBrands={selectedBrands}
            setSelectedBrands={setSelectedBrands}
            priceRange={priceRange}
            setPriceRange={setPriceRange}
            onClearFilters={clearFilters}
          />
        </div>

        <div className="flex-1">
          <div className="mb-4 flex items-center justify-between">
            <p className="text-navy-600">
              Showing <span className="font-semibold">{filteredProducts.length}</span> products
            </p>
          </div>

          {filteredProducts.length > 0 ? (
            <div className={viewMode === 'grid' ? 'grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3' : 'space-y-4'}>
              {filteredProducts.map((product) => (
                <ProductCard
                  key={product.id}
                  product={product}
                  isAuthenticated={isAuthenticated}
                  canAddToCart={!isWholesaler}
                  onAuthRequired={redirectToLogin}
                />
              ))}
            </div>
          ) : (
            <div className="card py-16 text-center">
              <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-gray-100">
                <Search className="h-8 w-8 text-navy-400" />
              </div>
              <h3 className="mb-2 text-lg font-semibold text-navy-800">No products found</h3>
              <p className="mb-4 text-navy-500">Try adjusting your search or filter criteria</p>
              <button type="button" onClick={clearFilters} className="btn-primary">
                Clear Filters
              </button>
            </div>
          )}
        </div>
      </div>

      {showMobileFilters && (
        <div className="fixed inset-0 z-50 bg-black/50 lg:hidden">
          <div className="absolute right-0 top-0 h-full w-80 overflow-y-auto bg-white dark:bg-slate-900">
            <div className="flex items-center justify-between border-b border-gray-200 p-4 dark:border-white/10">
              <h3 className="font-semibold text-navy-800 dark:text-slate-100">Filters</h3>
              <button type="button" onClick={() => setShowMobileFilters(false)}>
                <X className="h-5 w-5 text-navy-600 dark:text-slate-300" />
              </button>
            </div>
            <div className="p-4">
              <FilterSidebar
                categoryOptions={categoryOptions}
                selectedCategories={selectedCategories}
                setSelectedCategories={setSelectedCategories}
                brandOptions={brandOptions}
                selectedBrands={selectedBrands}
                setSelectedBrands={setSelectedBrands}
                priceRange={priceRange}
                setPriceRange={setPriceRange}
                onClearFilters={clearFilters}
              />
            </div>
          </div>
        </div>
      )}
    </PageShell>
  );
}
