import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, ChevronLeft, ChevronRight, Package, Plus, Save, Star, Trash2, Upload } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { productService } from '../services/productService';
import type { Category, CreateProductRequest } from '../types';

interface ImageItem {
  id?: string;
  url: string;
  file?: File;
  isExisting: boolean;
}

export default function ProductForm(): JSX.Element {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { id: productId } = useParams<{ id: string }>();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const isEditMode = Boolean(productId);

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

  useEffect(() => {
    void productService
      .getCategories()
      .then(setCategories)
      .catch((reason) => {
        console.error(reason);
      });
  }, []);

  useEffect(() => {
    if (!isEditMode || !productId) {
      return;
    }

    setIsLoading(true);
    void Promise.all([productService.getById(productId), productService.getImages(productId)])
      .then(([product, serverImages]) => {
        setName(product.name);
        setDescription(product.description);
        setPrice(String(product.originalPrice ?? product.price));
        setStock(String(product.stock));
        const category = categories.find((item) => item.name === product.category);
        setCategoryId(category?.id ?? '');

        const existingImages: ImageItem[] = (serverImages ?? []).map((image) => ({
          id: image.id ?? undefined,
          url: image.url,
          isExisting: true,
        }));
        setImages(existingImages);
      })
      .catch((reason) => {
        console.error('Failed to load product:', reason);
        setError('Failed to load product data');
      })
      .finally(() => setIsLoading(false));
  }, [categories, isEditMode, productId]);

  const addFiles = (files: FileList | File[]): void => {
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

    const newImages: ImageItem[] = [];
    for (const file of fileArray) {
      const reader = new FileReader();
      reader.onloadend = () => {
        newImages.push({ url: reader.result as string, file, isExisting: false });
        if (newImages.length === fileArray.length) {
          setImages((prev) => [...prev, ...newImages]);
        }
      };
      reader.readAsDataURL(file);
    }
  };

  const removeImage = (index: number): void => {
    const image = images[index];
    if (image?.isExisting && image.id) {
      setRemovedImageIds((prev) => [...prev, image.id as string]);
    }
    setImages((prev) => prev.filter((_, currentIndex) => currentIndex !== index));
  };

  const setAsMain = (index: number): void => {
    if (index === 0) {
      return;
    }
    setImages((prev) => {
      const copy = [...prev];
      const [moved] = copy.splice(index, 1);
      copy.unshift(moved);
      return copy;
    });
  };

  const moveImage = (fromIndex: number, direction: 'left' | 'right'): void => {
    const toIndex = direction === 'left' ? fromIndex - 1 : fromIndex + 1;
    if (toIndex < 0 || toIndex >= images.length) {
      return;
    }
    setImages((prev) => {
      const copy = [...prev];
      [copy[fromIndex], copy[toIndex]] = [copy[toIndex], copy[fromIndex]];
      return copy;
    });
  };

  const handleDrop = (event: React.DragEvent): void => {
    event.preventDefault();
    setIsDragOver(false);
    if (event.dataTransfer.types.includes('Files') && event.dataTransfer.files.length > 0) {
      addFiles(event.dataTransfer.files);
    }
  };

  const handleDragOver = (event: React.DragEvent): void => {
    event.preventDefault();
    if (event.dataTransfer.types.includes('Files')) {
      setIsDragOver(true);
    }
  };

  const handleSubmit = async (): Promise<void> => {
    if (!name.trim()) {
      setError('Product name is required');
      return;
    }
    if (!description.trim()) {
      setError('Description is required');
      return;
    }
    if (!price || Number(price) <= 0) {
      setError('Valid price is required');
      return;
    }
    if (!stock || Number(stock) < 0) {
      setError('Valid stock quantity is required');
      return;
    }
    if (!categoryId) {
      setError('Please select a category');
      return;
    }
    if (!user?.id) {
      setError('You must be logged in');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      const categoryName = categories.find((category) => category.id === categoryId)?.name ?? '';
      let savedProductId: string;

      if (isEditMode && productId) {
        const updateRequest = {
          name: name.trim(),
          description: description.trim(),
          price: Number(price),
          originalPrice: null,
          image: '',
          category: categoryName,
          categoryId,
          wholesalerId: user.id,
          brand: '',
          stock: Number(stock),
        };

        await productService.update(productId, updateRequest);
        savedProductId = productId;

        for (const imageId of removedImageIds) {
          if (imageId) {
            await productService.deleteImage(imageId);
          }
        }
      } else {
        const request = {
          name: name.trim(),
          description: description.trim(),
          price: Number(price),
          originalPrice: null,
          image: '',
          category: categoryName,
          categoryId,
          wholesalerId: user.id,
          brand: '',
          stock: Number(stock),
        } as unknown as CreateProductRequest;

        const created = await productService.create(request);
        savedProductId = created?.id ?? '';
      }

      const allImageIds: string[] = [];
      for (let index = 0; index < images.length; index += 1) {
        const image = images[index];
        if (image.isExisting && image.id) {
          allImageIds.push(image.id);
        } else if (!image.isExisting && image.file && savedProductId) {
          const uploaded = await productService.uploadImage(savedProductId, image.file, name.trim(), index);
          if (uploaded?.id) {
            allImageIds.push(uploaded.id);
          }
        }
      }

      if (allImageIds.length > 0 && savedProductId) {
        await productService.reorderImages(savedProductId, allImageIds);
      }

      navigate('/products');
    } catch (reason) {
      const message = reason instanceof Error ? reason.message : 'Failed to save product';
      setError(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <button
            type="button"
            onClick={() => navigate('/products')}
            className="w-10 h-10 flex items-center justify-center rounded-xl bg-white shadow-card hover:bg-gray-50 transition-colors text-navy-600"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-navy-800">{isEditMode ? 'Edit Product' : 'Add New Product'}</h1>
            <p className="text-navy-500 mt-0.5">{isEditMode ? 'Update product details and images' : 'Fill in the product details below'}</p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <button type="button" onClick={() => navigate('/products')} className="btn-secondary" disabled={isSubmitting}>
            Cancel
          </button>
          <button type="button" onClick={() => void handleSubmit()} className="btn-primary flex items-center gap-2" disabled={isSubmitting}>
            <Save className="w-4 h-4" />
            {isSubmitting ? (isEditMode ? 'Saving...' : 'Creating...') : isEditMode ? 'Save Changes' : 'Create Product'}
          </button>
        </div>
      </div>

      {error && <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-sm text-red-700">{error}</div>}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="card">
            <div className="flex items-center gap-3 mb-5">
              <div className="w-9 h-9 bg-primary-100 rounded-lg flex items-center justify-center">
                <Package className="w-4.5 h-4.5 text-primary-600" />
              </div>
              <h2 className="text-lg font-semibold text-navy-800">Basic Information</h2>
            </div>

            <div className="space-y-4">
              <div>
                <label className="label">
                  Product Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  placeholder="e.g. Premium Wireless Headphones"
                  className="input"
                />
              </div>

              <div>
                <label className="label">
                  Description <span className="text-red-500">*</span>
                </label>
                <textarea
                  value={description}
                  onChange={(event) => setDescription(event.target.value)}
                  placeholder="Describe your product..."
                  rows={4}
                  className="input resize-none"
                />
              </div>

              <div>
                <label className="label">
                  Category <span className="text-red-500">*</span>
                </label>
                <select value={categoryId} onChange={(event) => setCategoryId(event.target.value)} className="input">
                  <option value="">Select a category</option>
                  {categories.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>

          <div className="card">
            <h2 className="text-lg font-semibold text-navy-800 mb-5">Pricing &amp; Stock</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="label">
                  Price (SAR) <span className="text-red-500">*</span>
                </label>
                <input
                  type="number"
                  value={price}
                  onChange={(event) => setPrice(event.target.value)}
                  placeholder="0.00"
                  min="0"
                  step="0.01"
                  className="input"
                />
              </div>
              <div>
                <label className="label">
                  Stock Quantity <span className="text-red-500">*</span>
                </label>
                <input
                  type="number"
                  value={stock}
                  onChange={(event) => setStock(event.target.value)}
                  placeholder="0"
                  min="0"
                  className="input"
                />
              </div>
            </div>
          </div>
        </div>

        <div className="lg:col-span-1">
          <div className="card sticky top-6">
            <div className="flex items-center justify-between mb-5">
              <h2 className="text-lg font-semibold text-navy-800">Product Images</h2>
              <span className="text-sm text-navy-400 font-medium">{images.length}/10</span>
            </div>

            <div
              onDrop={handleDrop}
              onDragOver={handleDragOver}
              onDragLeave={() => setIsDragOver(false)}
              className={`rounded-xl transition-colors ${isDragOver ? 'ring-2 ring-primary-500 bg-primary-50/50' : ''}`}
            >
              {images.length > 0 && (
                <div className="grid grid-cols-2 gap-3 mb-3">
                  {images.map((image, index) => (
                    <div key={image.id ?? `${image.url}-${index}`} className="relative group">
                      <img
                        src={image.url}
                        alt={`Product ${index + 1}`}
                        draggable={false}
                        onDragStart={(event) => event.preventDefault()}
                        className={`w-full h-28 object-cover rounded-lg shadow-sm select-none ${
                          index === 0 ? 'border-2 border-primary-500' : 'border border-gray-200'
                        }`}
                      />

                      <button
                        type="button"
                        onClick={() => removeImage(index)}
                        className="absolute -top-2 -right-2 w-6 h-6 bg-red-500 hover:bg-red-600 text-white rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity shadow-md"
                      >
                        <Trash2 className="w-3 h-3" />
                      </button>

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

              {images.length === 0 && (
                <div
                  className={`border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-colors ${
                    isDragOver ? 'border-primary-500 bg-primary-50' : 'border-gray-300 hover:border-primary-400 hover:bg-gray-50'
                  }`}
                  onClick={() => fileInputRef.current?.click()}
                >
                  <div className="space-y-3">
                    <div className="w-14 h-14 bg-gray-100 rounded-full flex items-center justify-center mx-auto">
                      <Upload className="w-7 h-7 text-gray-400" />
                    </div>
                    <div>
                      <p className="text-sm text-navy-600 font-medium">Click to upload or drag and drop</p>
                      <p className="text-xs text-navy-400 mt-1">PNG, JPG, WebP up to 5MB · Up to 10 images</p>
                    </div>
                  </div>
                </div>
              )}

              {isDragOver && images.length > 0 && (
                <div className="border-2 border-dashed border-primary-500 rounded-xl p-3 text-center bg-primary-50">
                  <p className="text-sm text-primary-600 font-medium">Drop images here to add</p>
                </div>
              )}
            </div>

            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              multiple
              className="hidden"
              onChange={(event) => {
                if (event.target.files && event.target.files.length > 0) {
                  addFiles(event.target.files);
                }
                event.target.value = '';
              }}
            />

            {images.length > 0 && (
              <p className="text-xs text-navy-400 mt-3">
                The first image is the main product image. Drag and drop files or use the arrows to reorder.
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
