import { fetchApi } from './api';
import { AuthResponse, LoginRequest, RegisterRequest } from '../types';

export const authService = {
    login: (email: string, password: string) =>
        fetchApi<AuthResponse>('/auth/login', {
            method: 'POST',
            body: JSON.stringify(new LoginRequest(email, password)),
            token: ''
        }),

    register: (request: RegisterRequest) => {
        const body = {
            ...request,
            role: (request.role as any).name
        };
        return fetchApi<AuthResponse>('/auth/register', {
            method: 'POST',
            body: JSON.stringify(body),
            token: ''
        });
    },
};


