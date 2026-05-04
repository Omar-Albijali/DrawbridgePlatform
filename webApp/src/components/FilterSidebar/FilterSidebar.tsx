import { ChevronDown, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';

export interface FilterOption {
  label: string;
  value: string;
}

interface FilterSidebarProps {
  categoryOptions: FilterOption[];
  selectedCategories: string[];
  setSelectedCategories: (categories: string[]) => void;
  brandOptions: FilterOption[];
  selectedBrands: string[];
  setSelectedBrands: (brands: string[]) => void;
  priceRange: [number, number];
  setPriceRange: (range: [number, number]) => void;
  onClearFilters: () => void;
}

export default function FilterSidebar({
  categoryOptions,
  selectedCategories,
  setSelectedCategories,
  brandOptions,
  selectedBrands,
  setSelectedBrands,
  priceRange,
  setPriceRange,
  onClearFilters,
}: FilterSidebarProps): JSX.Element {
  const { t } = useTranslation();
  const hasActiveFilters =
    selectedCategories.length > 0 ||
    selectedBrands.length > 0 ||
    priceRange[0] > 0 ||
    priceRange[1] < 5000;

  const toggleCategory = (category: string): void => {
    if (selectedCategories.includes(category)) {
      setSelectedCategories(selectedCategories.filter((c) => c !== category));
      return;
    }
    setSelectedCategories([...selectedCategories, category]);
  };

  const toggleBrand = (brand: string): void => {
    if (selectedBrands.includes(brand)) {
      setSelectedBrands(selectedBrands.filter((b) => b !== brand));
      return;
    }
    setSelectedBrands([...selectedBrands, brand]);
  };

  return (
    <div className="bg-white rounded-xl shadow-card p-6 sticky top-24">
      <div className="flex items-center justify-between mb-6">
        <h3 className="font-semibold text-navy-800">{t('marketplace.filters')}</h3>
        {hasActiveFilters && (
          <button
            type="button"
            onClick={onClearFilters}
            className="text-sm text-primary-600 hover:text-primary-700 flex items-center gap-1"
          >
            <X className="w-4 h-4" />
            {t('marketplace.clearAll')}
          </button>
        )}
      </div>

      <div className="mb-6">
        <button type="button" className="w-full flex items-center justify-between text-left mb-3">
          <span className="font-medium text-navy-700">{t('marketplace.categories')}</span>
          <ChevronDown className="w-4 h-4 text-navy-400" />
        </button>
        <div className="space-y-2">
          {categoryOptions.length > 0 ? (
            categoryOptions.map((category) => (
              <label key={category.value} className="flex items-center gap-3 cursor-pointer group">
                <input
                  type="checkbox"
                  checked={selectedCategories.includes(category.value)}
                  onChange={() => toggleCategory(category.value)}
                  className="w-4 h-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                />
                <span className="text-sm text-navy-600 group-hover:text-navy-800">{category.label}</span>
              </label>
            ))
          ) : (
            <p className="text-sm text-navy-400">{t('marketplace.noCategories')}</p>
          )}
        </div>
      </div>

      <div className="mb-6">
        <button type="button" className="w-full flex items-center justify-between text-left mb-3">
          <span className="font-medium text-navy-700">{t('marketplace.priceRange')}</span>
          <ChevronDown className="w-4 h-4 text-navy-400" />
        </button>
        <div className="space-y-4">
          <div className="flex items-center gap-3">
            <div className="flex-1">
              <label className="text-xs text-navy-500 mb-1 block" htmlFor="price-min">
                {t('marketplace.min')}
              </label>
              <input
                id="price-min"
                type="number"
                value={priceRange[0]}
                onChange={(event) => {
                  const nextMin = Math.max(0, Number(event.target.value) || 0);
                  setPriceRange([nextMin, Math.max(nextMin, priceRange[1])]);
                }}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                placeholder="0"
              />
            </div>
            <span className="text-navy-400 pt-5">-</span>
            <div className="flex-1">
              <label className="text-xs text-navy-500 mb-1 block" htmlFor="price-max">
                {t('marketplace.max')}
              </label>
              <input
                id="price-max"
                type="number"
                value={priceRange[1]}
                onChange={(event) => {
                  const nextMax = Math.max(0, Number(event.target.value) || 0);
                  setPriceRange([Math.min(priceRange[0], nextMax), nextMax]);
                }}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                placeholder="5000"
              />
            </div>
          </div>
          <input
            type="range"
            min="0"
            max="5000"
            value={priceRange[1]}
            onChange={(event) => setPriceRange([priceRange[0], Number(event.target.value)])}
            className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-primary-600"
          />
          <div className="flex justify-between text-xs text-navy-500">
            <span>{t('common.currency.sarAmount', { amount: '0' })}</span>
            <span>{t('common.currency.sarAmount', { amount: '5,000' })}</span>
          </div>
        </div>
      </div>

      <div>
        <button type="button" className="w-full flex items-center justify-between text-left mb-3">
          <span className="font-medium text-navy-700">{t('marketplace.brands')}</span>
          <ChevronDown className="w-4 h-4 text-navy-400" />
        </button>
        <div className="space-y-2 max-h-48 overflow-y-auto">
          {brandOptions.length > 0 ? (
            brandOptions.map((brand) => (
              <label key={brand.value} className="flex items-center gap-3 cursor-pointer group">
                <input
                  type="checkbox"
                  checked={selectedBrands.includes(brand.value)}
                  onChange={() => toggleBrand(brand.value)}
                  className="w-4 h-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                />
                <span className="text-sm text-navy-600 group-hover:text-navy-800">{brand.label}</span>
              </label>
            ))
          ) : (
            <p className="text-sm text-navy-400">{t('marketplace.noBrands')}</p>
          )}
        </div>
      </div>
    </div>
  );
}
