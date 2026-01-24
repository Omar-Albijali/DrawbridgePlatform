import React, { useState, useMemo, useEffect } from 'react';
import { Product } from '../types';
import { productService } from '../services/productService';
import ProductCard from '../components/ProductCard/ProductCard';
import FilterSidebar from '../components/FilterSidebar/FilterSidebar';
import { Search, SlidersHorizontal, Grid3X3, List, X } from 'lucide-react';

const Marketplace: React.FC = () => {
    const [searchQuery, setSearchQuery] = useState('');
    const [selectedCategories, setSelectedCategories] = useState<string[]>([]);
    const [selectedBrands, setSelectedBrands] = useState<string[]>([]);
    const [priceRange, setPriceRange] = useState<[number, number]>([0, 5000]);
    const [sortBy, setSortBy] = useState('featured');
    const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
    const [showMobileFilters, setShowMobileFilters] = useState(false);
    const [products, setProducts] = useState<Product[]>([]);

    useEffect(() => {
        const fetchProducts = async () => {
            try {
                const data = await productService.getAll();
                setProducts(data);
            } catch (error) {
                console.error("Failed to fetch products", error);
            }
        };
        fetchProducts();
    }, []);

    const filteredProducts = useMemo(() => {
        return products.filter(product => {
            // Search filter
            if (searchQuery && !product.name.toLowerCase().includes(searchQuery.toLowerCase()) &&
                !product.description.toLowerCase().includes(searchQuery.toLowerCase())) {
                return false;
            }

            // Category filter
            if (selectedCategories.length > 0 && !selectedCategories.includes(product.category)) {
                return false;
            }

            // Brand filter
            if (selectedBrands.length > 0 && !selectedBrands.includes(product.brand)) {
                return false;
            }

            // Price filter
            if (product.price < priceRange[0] || product.price > priceRange[1]) {
                return false;
            }

            return true;
        }).sort((a, b) => {
            switch (sortBy) {
                case 'price-low': return a.price - b.price;
                case 'price-high': return b.price - a.price;
                case 'rating': return b.rating - a.rating;
                case 'newest': return b.id.localeCompare(a.id);
                default: return 0;
            }
        });
    }, [products, searchQuery, selectedCategories, selectedBrands, priceRange, sortBy]);

    const clearFilters = () => {
        setSelectedCategories([]);
        setSelectedBrands([]);
        setPriceRange([0, 5000]);
        setSearchQuery('');
    };

    const activeFilterCount = selectedCategories.length + selectedBrands.length +
        (priceRange[0] > 0 || priceRange[1] < 5000 ? 1 : 0);

    return (
        <div className="space-y-6">
            {/* Page Header */}
            <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-navy-800">Marketplace</h1>
                    <p className="text-navy-500 mt-1">Discover products from trusted wholesalers</p>
                </div>
            </div>

            {/* Search and Controls Bar */}
            <div className="bg-white rounded-xl shadow-card p-4">
                <div className="flex flex-col md:flex-row gap-4">
                    {/* Search */}
                    <div className="flex-1 relative">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-navy-400" />
                        <input
                            type="text"
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            placeholder="Search products..."
                            className="w-full pl-10 pr-4 py-2.5 bg-gray-100 border border-transparent rounded-lg focus:bg-white focus:border-primary-500 focus:ring-1 focus:ring-primary-500 transition-colors"
                        />
                        {searchQuery && (
                            <button
                                onClick={() => setSearchQuery('')}
                                className="absolute right-3 top-1/2 -translate-y-1/2 text-navy-400 hover:text-navy-600"
                            >
                                <X className="w-4 h-4" />
                            </button>
                        )}
                    </div>

                    {/* Sort */}
                    <select
                        value={sortBy}
                        onChange={(e) => setSortBy(e.target.value)}
                        className="px-4 py-2.5 bg-gray-100 border border-transparent rounded-lg focus:bg-white focus:border-primary-500 focus:ring-1 focus:ring-primary-500 text-navy-700"
                    >
                        <option value="featured">Featured</option>
                        <option value="price-low">Price: Low to High</option>
                        <option value="price-high">Price: High to Low</option>
                        <option value="rating">Top Rated</option>
                        <option value="newest">Newest</option>
                    </select>

                    {/* View Toggle */}
                    <div className="flex items-center bg-gray-100 rounded-lg p-1">
                        <button
                            onClick={() => setViewMode('grid')}
                            className={`p-2 rounded-md transition-colors ${viewMode === 'grid' ? 'bg-white shadow-sm text-primary-600' : 'text-navy-500'}`}
                        >
                            <Grid3X3 className="w-5 h-5" />
                        </button>
                        <button
                            onClick={() => setViewMode('list')}
                            className={`p-2 rounded-md transition-colors ${viewMode === 'list' ? 'bg-white shadow-sm text-primary-600' : 'text-navy-500'}`}
                        >
                            <List className="w-5 h-5" />
                        </button>
                    </div>

                    {/* Mobile Filter Toggle */}
                    <button
                        onClick={() => setShowMobileFilters(true)}
                        className="lg:hidden flex items-center gap-2 px-4 py-2.5 bg-gray-100 rounded-lg text-navy-700"
                    >
                        <SlidersHorizontal className="w-5 h-5" />
                        Filters
                        {activeFilterCount > 0 && (
                            <span className="w-5 h-5 bg-primary-600 text-white text-xs rounded-full flex items-center justify-center">
                                {activeFilterCount}
                            </span>
                        )}
                    </button>
                </div>
            </div>

            {/* Main Content */}
            <div className="flex gap-6">
                {/* Filter Sidebar - Desktop */}
                <div className="hidden lg:block w-72 flex-shrink-0">
                    <FilterSidebar
                        selectedCategories={selectedCategories}
                        setSelectedCategories={setSelectedCategories}
                        selectedBrands={selectedBrands}
                        setSelectedBrands={setSelectedBrands}
                        priceRange={priceRange}
                        setPriceRange={setPriceRange}
                        onClearFilters={clearFilters}
                    />
                </div>

                {/* Product Grid */}
                <div className="flex-1">
                    {/* Results Count */}
                    <div className="flex items-center justify-between mb-4">
                        <p className="text-navy-600">
                            Showing <span className="font-semibold">{filteredProducts.length}</span> products
                        </p>
                    </div>

                    {filteredProducts.length > 0 ? (
                        <div className={`grid gap-6 ${viewMode === 'grid'
                            ? 'grid-cols-1 sm:grid-cols-2 xl:grid-cols-3'
                            : 'grid-cols-1'
                            }`}>
                            {filteredProducts.map((product) => (
                                <ProductCard key={product.id} product={product} />
                            ))}
                        </div>
                    ) : (
                        <div className="text-center py-16">
                            <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
                                <Search className="w-8 h-8 text-navy-400" />
                            </div>
                            <h3 className="text-lg font-semibold text-navy-800 mb-2">No products found</h3>
                            <p className="text-navy-500 mb-4">Try adjusting your search or filter criteria</p>
                            <button onClick={clearFilters} className="btn-primary">
                                Clear Filters
                            </button>
                        </div>
                    )}
                </div>
            </div>

            {/* Mobile Filter Overlay */}
            {showMobileFilters && (
                <div className="fixed inset-0 bg-black/50 z-50 lg:hidden">
                    <div className="absolute right-0 top-0 h-full w-80 bg-white overflow-y-auto">
                        <div className="p-4 border-b border-gray-200 flex items-center justify-between">
                            <h3 className="font-semibold text-navy-800">Filters</h3>
                            <button onClick={() => setShowMobileFilters(false)}>
                                <X className="w-5 h-5 text-navy-600" />
                            </button>
                        </div>
                        <div className="p-4">
                            <FilterSidebar
                                selectedCategories={selectedCategories}
                                setSelectedCategories={setSelectedCategories}
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
        </div>
    );
};

export default Marketplace;
