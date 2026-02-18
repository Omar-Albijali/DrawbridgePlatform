import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Upload, Package, Trash2, Plus, Star, ChevronLeft, ChevronRight, Save } from 'lucide-react';
import { Category, CreateProductRequest } from '../types';
import { productService } from '../services/productService';
import { useAuth } from '../contexts/AuthContext';

interface ImageItem {
    id?: string;
    url: string;
    file?: File;
    isExisting: boolean;
}

const ProductForm: React.FC = () => {
    const { user } = useAuth();
    const navigate = useNavigate();
    const { id: productId } = useParams<{ id: string }>();
    const fileInputRef = useRef<HTMLInputElement>(null);
    const isEditMode = !!productId;

    // Form state
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [price, setPrice] = useState('');
    const [stock, setStock] = useState('');
    const [categoryId, setCategoryId] = useState('');
    const [categories, setCategories] = useState<Category[]>([]);
    const [images, setImages] = useState<ImageItem[]>([]);
    const [removedImageIds, setRemovedImageIds] = useState<string[]>([]);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [isDragOver, setIsDragOver] = useState(false);

    // Fetch categories
    useEffect(() => {
        productService.getCategories().then(setCategories).catch(console.error);
    }, []);

    // Fetch product data for edit mode
    useEffect(() => {
        if (isEditMode && productId) {
            setIsLoading(true);
            Promise.all([
                productService.getById(productId),
                productService.getImages(productId),
            ])
                .then(([product, serverImages]) => {
                    setName(product.name);
                    setDescription(product.description);
                    setPrice(String(product.originalPrice ?? product.price));
                    setStock(String(product.stock));

                    // Set category after categories load
                    const cat = categories.find(c => c.name === product.category);
                    setCategoryId(cat?.id || '');

                    const existingImages: ImageItem[] = (serverImages || []).map(img => ({
                        id: img.id || undefined,
                        url: img.url,
                        isExisting: true,
                    }));
                    setImages(existingImages);
                })
                .catch(err => {
                    console.error('Failed to load product:', err);
                    setError('Failed to load product data');
                })
                .finally(() => setIsLoading(false));
        }
    }, [isEditMode, productId, categories]);

    const addFiles = (files: FileList | File[]) => {
        const newImages: ImageItem[] = [];
        const fileArray = Array.from(files);

        for (const file of fileArray) {
            if (!file.type.startsWith('image/')) {
                setError('Please select image files only');
                return;
            }
            if (file.size > 5 * 1024 * 1024) {
                setError('Each image must be less than 5MB');
                return;
            }
        }

        if (images.length + fileArray.length > 10) {
            setError('Maximum 10 images per product');
            return;
        }

        setError(null);

        for (const file of fileArray) {
            const reader = new FileReader();
            reader.onloadend = () => {
                newImages.push({
                    url: reader.result as string,
                    file,
                    isExisting: false,
                });
                if (newImages.length === fileArray.length) {
                    setImages(prev => [...prev, ...newImages]);
                }
            };
            reader.readAsDataURL(file);
        }
    };

    const removeImage = (index: number) => {
        const img = images[index];
        if (img.isExisting && img.id) {
            setRemovedImageIds(prev => [...prev, img.id!]);
        }
        setImages(prev => prev.filter((_, i) => i !== index));
    };

    const setAsMain = (index: number) => {
        if (index === 0) return;
        setImages(prev => {
            const copy = [...prev];
            const [moved] = copy.splice(index, 1);
            copy.unshift(moved);
            return copy;
        });
    };

    const moveImage = (fromIndex: number, direction: 'left' | 'right') => {
        const toIndex = direction === 'left' ? fromIndex - 1 : fromIndex + 1;
        if (toIndex < 0 || toIndex >= images.length) return;
        setImages(prev => {
            const copy = [...prev];
            [copy[fromIndex], copy[toIndex]] = [copy[toIndex], copy[fromIndex]];
            return copy;
        });
    };

    const handleDrop = (e: React.DragEvent) => {
        e.preventDefault();
        setIsDragOver(false);
        if (e.dataTransfer.types.includes('Files') && e.dataTransfer.files.length > 0) {
            addFiles(e.dataTransfer.files);
        }
    };

    const handleDragOver = (e: React.DragEvent) => {
        e.preventDefault();
        if (e.dataTransfer.types.includes('Files')) {
            setIsDragOver(true);
        }
    };

    const handleSubmit = async () => {
        if (!name.trim()) { setError('Product name is required'); return; }
        if (!description.trim()) { setError('Description is required'); return; }
        if (!price || Number(price) <= 0) { setError('Valid price is required'); return; }
        if (!stock || Number(stock) < 0) { setError('Valid stock quantity is required'); return; }
        if (!categoryId) { setError('Please select a category'); return; }
        if (!user?.id) { setError('You must be logged in'); return; }

        setIsSubmitting(true);
        setError(null);

        try {
            const categoryName = categories.find(c => c.id === categoryId)?.name || '';
            let savedProductId: string;

            if (isEditMode && productId) {
                const updateRequest = {
                    name: name.trim(),
                    description: description.trim(),
                    price: Number(price),
                    originalPrice: null,
                    image: '',
                    category: categoryName,
                    categoryId: categoryId,
                    wholesalerId: user.id,
                    brand: '',
                    stock: Number(stock),
                } as unknown as CreateProductRequest;

                await productService.update(productId, updateRequest as any);
                savedProductId = productId;

                for (const imageId of removedImageIds) {
                    if (imageId) {
                        await productService.deleteImage(imageId);
                    }
                }
            } else {
                const request: CreateProductRequest = {
                    name: name.trim(),
                    description: description.trim(),
                    price: Number(price),
                    originalPrice: null,
                    image: '',
                    category: categoryName,
                    categoryId: categoryId,
                    wholesalerId: user.id,
                    brand: '',
                    stock: Number(stock),
                } as unknown as CreateProductRequest;

                const created = await productService.create(request);
                savedProductId = created?.id || '';
            }

            // Upload new images and collect all IDs in order
            const allImageIds: string[] = [];
            for (let i = 0; i < images.length; i++) {
                const img = images[i];
                if (img.isExisting && img.id) {
                    // Existing image — use its real ID
                    allImageIds.push(img.id);
                } else if (!img.isExisting && img.file && savedProductId) {
                    // New image — upload and capture the returned ID
                    const uploaded = await productService.uploadImage(savedProductId, img.file, name.trim(), i);
                    if (uploaded?.id) {
                        allImageIds.push(uploaded.id);
                    }
                }
            }

            // Persist the final sort order
            if (allImageIds.length > 0 && savedProductId) {
                await productService.reorderImages(savedProductId, allImageIds);
            }

            navigate('/products');
        } catch (err: any) {
            setError(err.message || 'Failed to save product');
        } finally {
            setIsSubmitting(false);
        }
    };

    if (isLoading) {
        return (
            <div className="flex items-center justify-center min-h-[400px]">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* Page Header */}
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                    <button
                        onClick={() => navigate('/products')}
                        className="w-10 h-10 flex items-center justify-center rounded-xl bg-white shadow-card hover:bg-gray-50 transition-colors text-navy-600"
                    >
                        <ArrowLeft className="w-5 h-5" />
                    </button>
                    <div>
                        <h1 className="text-2xl font-bold text-navy-800">
                            {isEditMode ? 'Edit Product' : 'Add New Product'}
                        </h1>
                        <p className="text-navy-500 mt-0.5">
                            {isEditMode ? 'Update product details and images' : 'Fill in the product details below'}
                        </p>
                    </div>
                </div>
                <div className="flex items-center gap-3">
                    <button
                        onClick={() => navigate('/products')}
                        className="btn-secondary"
                        disabled={isSubmitting}
                    >
                        Cancel
                    </button>
                    <button
                        onClick={handleSubmit}
                        className="btn-primary flex items-center gap-2"
                        disabled={isSubmitting}
                    >
                        <Save className="w-4 h-4" />
                        {isSubmitting
                            ? (isEditMode ? 'Saving...' : 'Creating...')
                            : (isEditMode ? 'Save Changes' : 'Create Product')
                        }
                    </button>
                </div>
            </div>

            {/* Error */}
            {error && (
                <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-sm text-red-700">
                    {error}
                </div>
            )}

            {/* Form Content — Two Columns */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Left Column — Form Fields */}
                <div className="lg:col-span-2 space-y-6">
                    {/* Basic Info Card */}
                    <div className="card">
                        <div className="flex items-center gap-3 mb-5">
                            <div className="w-9 h-9 bg-primary-100 rounded-lg flex items-center justify-center">
                                <Package className="w-4.5 h-4.5 text-primary-600" />
                            </div>
                            <h2 className="text-lg font-semibold text-navy-800">Basic Information</h2>
                        </div>

                        <div className="space-y-4">
                            {/* Product Name */}
                            <div>
                                <label className="label">Product Name <span className="text-red-500">*</span></label>
                                <input
                                    type="text"
                                    value={name}
                                    onChange={(e) => setName(e.target.value)}
                                    placeholder="e.g. Premium Wireless Headphones"
                                    className="input"
                                />
                            </div>

                            {/* Description */}
                            <div>
                                <label className="label">Description <span className="text-red-500">*</span></label>
                                <textarea
                                    value={description}
                                    onChange={(e) => setDescription(e.target.value)}
                                    placeholder="Describe your product..."
                                    rows={4}
                                    className="input resize-none"
                                />
                            </div>

                            {/* Category */}
                            <div>
                                <label className="label">Category <span className="text-red-500">*</span></label>
                                <select
                                    value={categoryId}
                                    onChange={(e) => setCategoryId(e.target.value)}
                                    className="input"
                                >
                                    <option value="">Select a category</option>
                                    {categories.map(cat => (
                                        <option key={cat.id} value={cat.id}>{cat.name}</option>
                                    ))}
                                </select>
                            </div>
                        </div>
                    </div>

                    {/* Pricing & Stock Card */}
                    <div className="card">
                        <h2 className="text-lg font-semibold text-navy-800 mb-5">Pricing & Stock</h2>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                            <div>
                                <label className="label">Price (SAR) <span className="text-red-500">*</span></label>
                                <input
                                    type="number"
                                    value={price}
                                    onChange={(e) => setPrice(e.target.value)}
                                    placeholder="0.00"
                                    min="0"
                                    step="0.01"
                                    className="input"
                                />
                            </div>
                            <div>
                                <label className="label">Stock Quantity <span className="text-red-500">*</span></label>
                                <input
                                    type="number"
                                    value={stock}
                                    onChange={(e) => setStock(e.target.value)}
                                    placeholder="0"
                                    min="0"
                                    className="input"
                                />
                            </div>
                        </div>
                    </div>
                </div>

                {/* Right Column — Images */}
                <div className="lg:col-span-1">
                    <div className="card sticky top-6">
                        <div className="flex items-center justify-between mb-5">
                            <h2 className="text-lg font-semibold text-navy-800">
                                Product Images
                            </h2>
                            <span className="text-sm text-navy-400 font-medium">{images.length}/10</span>
                        </div>

                        {/* Drop zone wrapper */}
                        <div
                            onDrop={handleDrop}
                            onDragOver={handleDragOver}
                            onDragLeave={() => setIsDragOver(false)}
                            className={`rounded-xl transition-colors ${isDragOver ? 'ring-2 ring-primary-500 bg-primary-50/50' : ''}`}
                        >
                            {/* Image Grid */}
                            {images.length > 0 && (
                                <div className="grid grid-cols-2 gap-3 mb-3">
                                    {images.map((img, index) => (
                                        <div key={index} className="relative group">
                                            <img
                                                src={img.url}
                                                alt={`Product ${index + 1}`}
                                                draggable={false}
                                                onDragStart={(e) => e.preventDefault()}
                                                className={`w-full h-28 object-cover rounded-lg shadow-sm select-none ${index === 0
                                                    ? 'border-2 border-primary-500'
                                                    : 'border border-gray-200'
                                                    }`}
                                            />
                                            {/* Delete button */}
                                            <button
                                                type="button"
                                                onClick={() => removeImage(index)}
                                                className="absolute -top-2 -right-2 w-6 h-6 bg-red-500 hover:bg-red-600 text-white rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity shadow-md"
                                            >
                                                <Trash2 className="w-3 h-3" />
                                            </button>
                                            {/* Move left/right buttons */}
                                            {images.length > 1 && (
                                                <div className="absolute top-1 left-1 flex gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
                                                    {index > 0 && (
                                                        <button
                                                            type="button"
                                                            onClick={() => moveImage(index, 'left')}
                                                            className="w-5 h-5 bg-black/60 hover:bg-black/80 text-white rounded flex items-center justify-center"
                                                            title="Move left"
                                                        >
                                                            <ChevronLeft className="w-3 h-3" />
                                                        </button>
                                                    )}
                                                    {index < images.length - 1 && (
                                                        <button
                                                            type="button"
                                                            onClick={() => moveImage(index, 'right')}
                                                            className="w-5 h-5 bg-black/60 hover:bg-black/80 text-white rounded flex items-center justify-center"
                                                            title="Move right"
                                                        >
                                                            <ChevronRight className="w-3 h-3" />
                                                        </button>
                                                    )}
                                                </div>
                                            )}
                                            {/* Main badge or Set as main */}
                                            {index === 0 ? (
                                                <span className="absolute bottom-1 left-1 text-[10px] bg-primary-600 text-white px-1.5 py-0.5 rounded font-medium flex items-center gap-0.5">
                                                    <Star className="w-2.5 h-2.5 fill-current" /> Main
                                                </span>
                                            ) : (
                                                <button
                                                    type="button"
                                                    onClick={() => setAsMain(index)}
                                                    className="absolute bottom-1 left-1 text-[10px] bg-black/60 hover:bg-primary-600 text-white px-1.5 py-0.5 rounded font-medium opacity-0 group-hover:opacity-100 transition-opacity flex items-center gap-0.5"
                                                >
                                                    <Star className="w-2.5 h-2.5" /> Set main
                                                </button>
                                            )}
                                        </div>
                                    ))}

                                    {/* Add more button inside grid */}
                                    {images.length < 10 && (
                                        <button
                                            type="button"
                                            onClick={() => fileInputRef.current?.click()}
                                            className="w-full h-28 border-2 border-dashed border-gray-300 hover:border-primary-400 rounded-lg flex flex-col items-center justify-center gap-1 transition-colors hover:bg-gray-50"
                                        >
                                            <Plus className="w-5 h-5 text-gray-400" />
                                            <span className="text-xs text-gray-400">Add</span>
                                        </button>
                                    )}
                                </div>
                            )}

                            {/* Empty state drop zone */}
                            {images.length === 0 && (
                                <div
                                    className={`border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-colors ${isDragOver
                                        ? 'border-primary-500 bg-primary-50'
                                        : 'border-gray-300 hover:border-primary-400 hover:bg-gray-50'
                                        }`}
                                    onClick={() => fileInputRef.current?.click()}
                                >
                                    <div className="space-y-3">
                                        <div className="w-14 h-14 bg-gray-100 rounded-full flex items-center justify-center mx-auto">
                                            <Upload className="w-7 h-7 text-gray-400" />
                                        </div>
                                        <div>
                                            <p className="text-sm text-navy-600 font-medium">
                                                Click to upload or drag and drop
                                            </p>
                                            <p className="text-xs text-navy-400 mt-1">
                                                PNG, JPG, WebP up to 5MB · Up to 10 images
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            )}

                            {/* Drag overlay hint when images exist */}
                            {isDragOver && images.length > 0 && (
                                <div className="border-2 border-dashed border-primary-500 rounded-xl p-3 text-center bg-primary-50">
                                    <p className="text-sm text-primary-600 font-medium">Drop images here to add</p>
                                </div>
                            )}
                        </div>

                        {/* Hidden file input */}
                        <input
                            ref={fileInputRef}
                            type="file"
                            accept="image/*"
                            multiple
                            className="hidden"
                            onChange={(e) => {
                                if (e.target.files && e.target.files.length > 0) {
                                    addFiles(e.target.files);
                                }
                                e.target.value = '';
                            }}
                        />

                        {/* Hint text */}
                        {images.length > 0 && (
                            <p className="text-xs text-navy-400 mt-3">
                                The first image is the main product image. Drag & drop files or use the arrows to reorder.
                            </p>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ProductForm;
