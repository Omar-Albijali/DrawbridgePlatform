import { fetchApi } from './api';
import { SupportTicket } from '../types';

export interface CreateSupportTicketInput {
    subject: string;
    category: string;
    description: string;
    attachment?: File | null;
}

export const supportService = {
    createTicket: (input: CreateSupportTicketInput) => {
        const formData = new FormData();
        formData.append('subject', input.subject.trim());
        formData.append('category', input.category);
        formData.append('description', input.description.trim());

        if (input.attachment) {
            formData.append('attachment', input.attachment);
        }

        return fetchApi<SupportTicket>('/support', {
            method: 'POST',
            body: formData
        });
    },

    getMyTickets: () => fetchApi<SupportTicket[]>('/support/my'),

    getTicket: (id: string) => fetchApi<SupportTicket>(`/support/${id}`)
};
