import React from 'react';
import { categories, brands } from '../../data/constants';
import { X, ChevronDown } from 'lucide-react';

interface FilterSidebarProps {
    selectedCategories: string[];
    setSelectedCategories: (categories: string[]) => void;
    selectedBrands: string[];
    setSelectedBrands: (brands: string[]) => void;
    priceRange: [number, number];
    setPriceRange: (range: [number, number]) => void;
    onClearFilters: () => void;
}

const FilterSidebar: React.FC<FilterSidebarProps> = ({
    selectedCategories,
    setSelectedCategories,
    selectedBrands,
    setSelectedBrands,
    priceRange,
    setPriceRange,
    onClearFilters
}) => {
    const hasActiveFilters = selectedCategories.length > 0 || selectedBrands.length > 0 || priceRange[0] > 0 || priceRange[1] < 5000;

    const toggleCategory = (category: string) => {
        if (selectedCategories.includes(category)) {
            setSelectedCategories(selectedCategories.filter(c => c !== category));
        } else {
            setSelectedCategories([...selectedCategories, category]);
        }
    };

    const toggleBrand = (brand: string) => {
        if (selectedBrands.includes(brand)) {
            setSelectedBrands(selectedBrands.filter(b => b !== brand));
        } else {
            setSelectedBrands([...selectedBrands, brand]);
        }
    };

    return (
        <div className="bg-white rounded-xl shadow-card p-6 sticky top-24">
            {/* Header */}
            <div className="flex items-center justify-between mb-6">
                <h3 className="font-semibold text-navy-800">Filters</h3>
                {hasActiveFilters && (
                    <button
                        onClick={onClearFilters}
                        className="text-sm text-primary-600 hover:text-primary-700 flex items-center gap-1"
                    >
                        <X className="w-4 h-4" />
                        Clear all
                    </button>
                )}
            </div>

            {/* Categories */}
            <div className="mb-6">
                <button className="w-full flex items-center justify-between text-left mb-3">
                    <span className="font-medium text-navy-700">Categories</span>
                    <ChevronDown className="w-4 h-4 text-navy-400" />
                </button>
                <div className="space-y-2">
                    {categories.map((category) => (
                        <label key={category} className="flex items-center gap-3 cursor-pointer group">
                            <input
                                type="checkbox"
                                checked={selectedCategories.includes(category)}
                                onChange={() => toggleCategory(category)}
                                className="w-4 h-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                            />
                            <span className="text-sm text-navy-600 group-hover:text-navy-800">
                                {category}
                            </span>
                        </label>
                    ))}
                </div>
            </div>

            {/* Price Range */}
            <div className="mb-6">
                <button className="w-full flex items-center justify-between text-left mb-3">
                    <span className="font-medium text-navy-700">Price Range</span>
                    <ChevronDown className="w-4 h-4 text-navy-400" />
                </button>
                <div className="space-y-4">
                    <div className="flex items-center gap-3">
                        <div className="flex-1">
                            <label className="text-xs text-navy-500 mb-1 block">Min</label>
                            <input
                                type="number"
                                value={priceRange[0]}
                                onChange={(e) => setPriceRange([Number(e.target.value), priceRange[1]])}
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                                placeholder="0"
                            />
                        </div>
                        <span className="text-navy-400 pt-5">—</span>
                        <div className="flex-1">
                            <label className="text-xs text-navy-500 mb-1 block">Max</label>
                            <input
                                type="number"
                                value={priceRange[1]}
                                onChange={(e) => setPriceRange([priceRange[0], Number(e.target.value)])}
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
                        onChange={(e) => setPriceRange([priceRange[0], Number(e.target.value)])}
                        className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-primary-600"
                    />
                    <div className="flex justify-between text-xs text-navy-500">
                        <span>SAR 0</span>
                        <span>SAR 5,000</span>
                    </div>
                </div>
            </div>

            {/* Brands */}
            <div>
                <button className="w-full flex items-center justify-between text-left mb-3">
                    <span className="font-medium text-navy-700">Brands</span>
                    <ChevronDown className="w-4 h-4 text-navy-400" />
                </button>
                <div className="space-y-2 max-h-48 overflow-y-auto">
                    {brands.map((brand) => (
                        <label key={brand} className="flex items-center gap-3 cursor-pointer group">
                            <input
                                type="checkbox"
                                checked={selectedBrands.includes(brand)}
                                onChange={() => toggleBrand(brand)}
                                className="w-4 h-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                            />
                            <span className="text-sm text-navy-600 group-hover:text-navy-800">
                                {brand}
                            </span>
                        </label>
                    ))}
                </div>
            </div>
        </div>
    );
};

export default FilterSidebar;
