import React, { useState, useEffect } from 'react';
import { Plus, MapPin, Edit2, Trash2, X, Check } from 'lucide-react';
import { addressService } from '../../services/addressService';
import { Address, CreateAddressRequest } from '../../types';

const Addresses: React.FC = () => {
    const [addresses, setAddresses] = useState<Address[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isEditing, setIsEditing] = useState(false);
    const [editId, setEditId] = useState<string | null>(null);

    const [formData, setFormData] = useState<CreateAddressRequest>({
        street: '',
        city: '',
        state: '',
        zipCode: '',
        country: ''
    } as unknown as CreateAddressRequest);

    useEffect(() => {
        loadAddresses();
    }, []);

    const loadAddresses = async () => {
        setIsLoading(true);
        try {
            const data = await addressService.getAddresses();
            setAddresses(data);
        } catch (error) {
            console.error('Failed to load addresses', error);
        } finally {
            setIsLoading(false);
        }
    };
    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData((prev: CreateAddressRequest) => ({ ...prev, [name]: value } as unknown as CreateAddressRequest));
    };

    const resetForm = () => {
        setFormData({
            street: '',
            city: '',
            state: '',
            zipCode: '',
            country: ''
        } as unknown as CreateAddressRequest);
        setIsEditing(false);
        setEditId(null);
    };

    const handleEdit = (address: Address) => {
        setFormData({
            street: address.street,
            city: address.city,
            state: address.state,
            zipCode: address.zipCode,
            country: address.country
        } as unknown as CreateAddressRequest);
        setEditId(address.id!);
        setIsEditing(true);
    };

    const handleDelete = async (id: string) => {
        if (!window.confirm('Are you sure you want to delete this address?')) return;
        try {
            await addressService.deleteAddress(id);
            setAddresses(prev => prev.filter(addr => addr.id !== id));
        } catch (error) {
            console.error('Failed to delete address', error);
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            if (editId) {
                const updated = await addressService.updateAddress(editId, formData);
                setAddresses(prev => prev.map(addr => addr.id === editId ? updated : addr));
            } else {
                const newAddr = await addressService.addAddress(formData);
                setAddresses(prev => [...prev, newAddr]);
            }
            resetForm();
        } catch (error) {
            console.error('Failed to save address', error);
        }
    };

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold text-navy-800">Address Management</h1>
                    <p className="text-navy-500 mt-1">Manage your shipping and billing addresses</p>
                </div>
                {!isEditing && (
                    <button
                        onClick={() => setIsEditing(true)}
                        className="btn-primary px-4 py-2 flex items-center gap-2"
                    >
                        <Plus className="w-4 h-4" />
                        Add New Address
                    </button>
                )}
            </div>

            {isEditing && (
                <div className="card border-2 border-primary-100 animate-fade-in-up">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="text-lg font-semibold text-navy-800">
                            {editId ? 'Edit Address' : 'New Address'}
                        </h3>
                        <button onClick={resetForm} className="text-navy-400 hover:text-navy-600">
                            <X className="w-5 h-5" />
                        </button>
                    </div>
                    <form onSubmit={handleSubmit}>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div className="md:col-span-2">
                                <label className="label">Street Address</label>
                                <input
                                    type="text"
                                    name="street"
                                    value={formData.street}
                                    onChange={handleInputChange}
                                    required
                                    className="input"
                                    placeholder="123 Main St"
                                />
                            </div>
                            <div>
                                <label className="label">City</label>
                                <input
                                    type="text"
                                    name="city"
                                    value={formData.city}
                                    onChange={handleInputChange}
                                    required
                                    className="input"
                                    placeholder="City"
                                />
                            </div>
                            <div>
                                <label className="label">State / Province</label>
                                <input
                                    type="text"
                                    name="state"
                                    value={formData.state}
                                    onChange={handleInputChange}
                                    required
                                    className="input"
                                    placeholder="State"
                                />
                            </div>
                            <div>
                                <label className="label">Zip Code</label>
                                <input
                                    type="text"
                                    name="zipCode"
                                    value={formData.zipCode}
                                    onChange={handleInputChange}
                                    required
                                    className="input"
                                    placeholder="Zip Code"
                                />
                            </div>
                            <div>
                                <label className="label">Country</label>
                                <input
                                    type="text"
                                    name="country"
                                    value={formData.country}
                                    onChange={handleInputChange}
                                    required
                                    className="input"
                                    placeholder="Country"
                                />
                            </div>
                        </div>
                        <div className="mt-6 flex justify-end gap-3">
                            <button
                                type="button"
                                onClick={resetForm}
                                className="px-4 py-2 text-navy-600 hover:bg-navy-50 rounded-lg transition-colors"
                            >
                                Cancel
                            </button>
                            <button
                                type="submit"
                                className="btn-primary px-6 py-2 flex items-center gap-2"
                            >
                                <Check className="w-4 h-4" />
                                Save Address
                            </button>
                        </div>
                    </form>
                </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {isLoading ? (
                    <div className="col-span-full py-12 text-center text-navy-400">
                        Loading addresses...
                    </div>
                ) : addresses.length === 0 && !isEditing ? (
                    <div className="col-span-full py-12 text-center text-navy-400 bg-gray-50 rounded-xl border-dashed border-2 border-gray-200">
                        <MapPin className="w-12 h-12 mx-auto mb-3 opacity-20" />
                        <p>No addresses found. Add one to get started.</p>
                    </div>
                ) : (
                    addresses.map((addr) => (
                        <div key={addr.id} className="card group hover:border-primary-200 transition-colors relative">
                            <div className="flex items-start gap-4">
                                <div className="w-10 h-10 bg-primary-50 text-primary-600 rounded-full flex items-center justify-center flex-shrink-0">
                                    <MapPin className="w-5 h-5" />
                                </div>
                                <div>
                                    <h4 className="font-semibold text-navy-800">{addr.street}</h4>
                                    <p className="text-navy-500 text-sm mt-1">
                                        {addr.city}, {addr.state} {addr.zipCode}
                                    </p>
                                    <p className="text-navy-500 text-sm">{addr.country}</p>
                                </div>
                            </div>
                            <div className="absolute top-4 right-4 flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                <button
                                    onClick={() => handleEdit(addr)}
                                    className="p-2 text-navy-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
                                >
                                    <Edit2 className="w-4 h-4" />
                                </button>
                                <button
                                    onClick={() => handleDelete(addr.id!)}
                                    className="p-2 text-navy-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                                >
                                    <Trash2 className="w-4 h-4" />
                                </button>
                            </div>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
};

export default Addresses;
