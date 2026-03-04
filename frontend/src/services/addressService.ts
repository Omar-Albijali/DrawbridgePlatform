import { fetchApi } from './api';
import { Address, CreateAddressRequest } from '../types';

export const addressService = {
    getAddresses: () => fetchApi<Address[]>('/addresses'),
    addAddress: (data: CreateAddressRequest) => fetchApi<Address>('/addresses', {
        method: 'POST',
        body: JSON.stringify(data as unknown as CreateAddressRequest)
    }),
    updateAddress: (id: string, data: CreateAddressRequest) => fetchApi<Address>(`/addresses/${id}`, {
        method: 'PUT',
        body: JSON.stringify(data as unknown as CreateAddressRequest)
    }),
    deleteAddress: (id: string) => fetchApi<void>(`/addresses/${id}`, {
        method: 'DELETE'
    })
};
