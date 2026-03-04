import { fetchApi } from './api';
import { PaymentMethodDTO, CreatePaymentMethodRequest } from '../types';

export const paymentService = {
    getPaymentMethods: (userId: string) => fetchApi<PaymentMethodDTO[]>(`/payments/methods/owner/${userId}`),
    addPaymentMethod: (data: CreatePaymentMethodRequest) => fetchApi<PaymentMethodDTO>('/payments/methods', {
        method: 'POST',
        body: JSON.stringify(data as unknown as CreatePaymentMethodRequest)
    }),
    deletePaymentMethod: (id: string) => fetchApi<void>(`/payments/methods/${id}`, {
        method: 'DELETE'
    }),
    setDefaultPaymentMethod: (id: string) => fetchApi<PaymentMethodDTO>(`/payments/methods/${id}/default`, {
        method: 'POST'
    })
};
