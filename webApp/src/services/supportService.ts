import { fetchApi } from './api';
import { SupportTicket, SupportTicketChat, CreateTicketRequest, AddMessageRequest } from '../types';

export const supportService = {
    getAllTickets: () => fetchApi<SupportTicket[]>('/support/tickets'),

    getTicket: (id: string) => fetchApi<SupportTicket>(`/support/tickets/${id}`),

    getUserTickets: (userId: string) => fetchApi<SupportTicket[]>(`/support/tickets/user/${userId}`),

    createTicket: (userId: string, subject: string, description: string) => {
        const request = { userId, subject, description } as unknown as CreateTicketRequest;
        return fetchApi<SupportTicket>('/support/tickets', {
            method: 'POST',
            body: JSON.stringify(request)
        });
    },

    closeTicket: (id: string) => fetchApi<SupportTicket>(`/support/tickets/${id}/close`, {
        method: 'POST'
    }),

    // Chat
    getChatHistory: (ticketId: string) => fetchApi<SupportTicketChat[]>(`/support/tickets/${ticketId}/chat`),

    sendMessage: (ticketId: string, message: string, adminId?: string) => {
        const request = { message, adminId } as unknown as AddMessageRequest;
        return fetchApi<SupportTicketChat>(`/support/tickets/${ticketId}/chat`, {
            method: 'POST',
            body: JSON.stringify(request)
        });
    }
};
