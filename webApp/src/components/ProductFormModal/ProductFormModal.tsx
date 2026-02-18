import React, { useState, useEffect, useRef } from 'react';
import { X, Upload, Package, Trash2, Plus, Star, ChevronLeft, ChevronRight } from 'lucide-react';
import { Product, Category, CreateProductRequest } from '../../types';
import { productService } from '../../services/productService';
import { useAuth } from '../../contexts/AuthContext';

interface ProductFormModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSave: () => void;
    product?: Product | null; // null = create mode, product = edit mode
}

interface ImageItem {
    id?: string;        // existing image ID (for deletion)
    url: string;        // preview URL (data URL for new, server URL for existing)
    file?: File;        // file to upload (only for new images)
    isExisting: boolean;
}

const ProductFormModal: React.FC<ProductFormModalProps> = ({ isOpen, onClose, onSave, product }) => {
    const { user } = useAuth();
    const fileInputRef = useRef<HTMLInputElement>(null);
    const isEditMode = !!product;

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
    const [error, setError] = useState<string | null>(null);
    const [isDragOver, setIsDragOver] = useState(false);

    // Fetch categories on open
    useEffect(() => {
        if (isOpen) {
            productService.getCategories().then(setCategories).catch(console.error);
        }
    }, [isOpen]);

    // Pre-fill form for edit mode
    useEffect(() => {
        if (isOpen && product) {
            setName(product.name);
            setDescription(product.description);
            setPrice(String(product.originalPrice ?? product.price));
            setStock(String(product.stock));
            // Find category ID by name
            const cat = categories.find(c => c.name === product.category);
            setCategoryId(cat?.id || '');
            setRemovedImageIds([]);
            // Fetch real image data with IDs from the server
            productService.getImages(product.id).then(serverImages => {
                const existingImages: ImageItem[] = (serverImages || []).map(img => ({
                    id: img.id || undefined,
                    url: img.url,
                    isExisting: true,
                }));
                setImages(existingImages);
            }).catch(() => {
                // Fallback: use product.images URLs without real IDs
                const existingImages: ImageItem[] = (product.images || [])
                    .filter((url: string) => url && url.length > 0)
                    .map((url: string) => ({
                        url,
                        isExisting: true,
                    }));
                setImages(existingImages);
            });
        } else if (isOpen && !product) {
            resetForm();
        }
    }, [isOpen, product, categories]);

    const resetForm = () => {
        setName('');
        setDescription('');
        setPrice('');
        setStock('');
        setCategoryId('');
        setImages([]);
        setRemovedImageIds([]);
        setError(null);
    };

    const handleClose = () => {
        resetForm();
        onClose();
    };

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
                // When all files have been read, update state
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
        // Only handle actual file drops (not internal element drags)
        if (e.dataTransfer.types.includes('Files') && e.dataTransfer.files.length > 0) {
            addFiles(e.dataTransfer.files);
        }
    };

    const handleDragOver = (e: React.DragEvent) => {
        e.preventDefault();
        // Only show drop zone indicator for file drags
        if (e.dataTransfer.types.includes('Files')) {
            setIsDragOver(true);
        }
    };

    const handleSubmit = async () => {
        // Validation
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
            let productId: string;

            if (isEditMode && product) {
                // Update product
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

                await productService.update(product.id, updateRequest as any);
                productId = product.id;

                // Delete removed images
                for (const imageId of removedImageIds) {
                    if (imageId) {
                        await productService.deleteImage(imageId);
                    }
                }
            } else {
                // Create product
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
                productId = created?.id || '';
            }

            // Upload all new images with their sortIndex
            for (let i = 0; i < images.length; i++) {
                const img = images[i];
                if (!img.isExisting && img.file && productId) {
                    await productService.uploadImage(productId, img.file, name.trim(), i);
                }
            }

            handleClose();
            onSave();
        } catch (err: any) {
            setError(err.message || 'Failed to save product');
        } finally {
            setIsSubmitting(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden flex flex-col max-h-[90vh]">
                {/* Header */}
                <div className="bg-gradient-to-r from-primary-600 to-primary-700 p-6 text-white shrink-0">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div className="w-10 h-10 bg-white/20 rounded-lg flex items-center justify-center">
                                <Package className="w-5 h-5" />
                            </div>
                            <div>
                                <h2 className="text-lg font-semibold">
                                    {isEditMode ? 'Edit Product' : 'Add New Product'}
                                </h2>
                                <p className="text-primary-100 text-sm">
                                    {isEditMode ? 'Update product details' : 'Fill in the product details'}
                                </p>
                            </div>
                        </div>
                        <button
                            onClick={handleClose}
                            className="w-8 h-8 bg-white/20 hover:bg-white/30 rounded-lg flex items-center justify-center transition-colors"
                        >
                            <X className="w-4 h-4" />
                        </button>
                    </div>
                </div>

                {/* Content */}
                <div className="flex-1 overflow-y-auto p-6">
                    <div className="space-y-4">
                        {error && (
                            <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-sm text-red-700">
                                {error}
                            </div>
                        )}

                        {/* Product Name */}
                        <div>
                            <label className="label">Product Name <span className="text-red-500">*</span></label>
                            <input
                                type="text"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                placeholder="Enter product name"
                                className="input"
                            />
                        </div>

                        {/* Description */}
                        <div>
                            <label className="label">Description <span className="text-red-500">*</span></label>
                            <textarea
                                value={description}
                                onChange={(e) => setDescription(e.target.value)}
                                placeholder="Enter product description"
                                className="input"
                                rows={3}
                                style={{ resize: 'vertical' }}
                            />
                        </div>

                        {/* Price & Stock */}
                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <label className="label">Price (SAR) <span className="text-red-500">*</span></label>
                                <input
                                    type="number"
                                    value={price}
                                    onChange={(e) => setPrice(e.target.value)}
                                    placeholder="0.00"
                                    className="input"
                                    min="0"
                                    step="0.01"
                                />
                            </div>
                            <div>
                                <label className="label">Stock Quantity <span className="text-red-500">*</span></label>
                                <input
                                    type="number"
                                    value={stock}
                                    onChange={(e) => setStock(e.target.value)}
                                    placeholder="0"
                                    className="input"
                                    min="0"
                                />
                            </div>
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

                        {/* Image Upload - Multiple */}
                        <div>
                            <label className="label">
                                Product Images
                                <span className="text-navy-400 font-normal ml-1">({images.length}/10)</span>
                            </label>

                            {/* Entire section is a drop target */}
                            <div
                                onDrop={handleDrop}
                                onDragOver={handleDragOver}
                                onDragLeave={() => setIsDragOver(false)}
                                className={`rounded-xl transition-colors ${isDragOver ? 'ring-2 ring-primary-500 bg-primary-50/50' : ''}`}
                            >
                                {/* Image Grid */}
                                {images.length > 0 && (
                                    <div className="grid grid-cols-3 gap-3 mb-3">
                                        {images.map((img, index) => (
                                            <div key={index} className="relative group">
                                                <img
                                                    src={img.url}
                                                    alt={`Product ${index + 1}`}
                                                    draggable={false}
                                                    onDragStart={(e) => e.preventDefault()}
                                                    className={`w-full h-24 object-cover rounded-lg shadow-sm select-none ${index === 0
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
                                                className="w-full h-24 border-2 border-dashed border-gray-300 hover:border-primary-400 rounded-lg flex flex-col items-center justify-center gap-1 transition-colors hover:bg-gray-50"
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
                                        className={`border-2 border-dashed rounded-xl p-6 text-center cursor-pointer transition-colors ${isDragOver
                                            ? 'border-primary-500 bg-primary-50'
                                            : 'border-gray-300 hover:border-primary-400 hover:bg-gray-50'
                                            }`}
                                        onClick={() => fileInputRef.current?.click()}
                                    >
                                        <div className="space-y-2">
                                            <div className="w-12 h-12 bg-gray-100 rounded-full flex items-center justify-center mx-auto">
                                                <Upload className="w-6 h-6 text-gray-400" />
                                            </div>
                                            <p className="text-sm text-navy-600 font-medium">
                                                Click to upload or drag and drop
                                            </p>
                                            <p className="text-xs text-navy-400">
                                                PNG, JPG, WebP up to 5MB · Up to 10 images
                                            </p>
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

                            {/* Hidden file input - multiple */}
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
                        </div>
                    </div>
                </div>

                {/* Footer */}
                <div className="px-6 py-4 bg-gray-50 flex gap-3 shrink-0">
                    <button
                        onClick={handleClose}
                        className="flex-1 btn-secondary"
                        disabled={isSubmitting}
                    >
                        Cancel
                    </button>
                    <button
                        onClick={handleSubmit}
                        className="flex-1 btn-primary"
                        disabled={isSubmitting}
                    >
                        {isSubmitting
                            ? (isEditMode ? 'Saving...' : 'Creating...')
                            : (isEditMode ? 'Save Changes' : 'Create Product')
                        }
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ProductFormModal;
