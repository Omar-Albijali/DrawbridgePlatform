import { fetchApi } from './api';
import {
    AuthResponse,
    LoginRequest,
    RegisterRequest,
    ForgotPasswordRequest,
    ResetPasswordRequest,
    VerifyEmailRequest,
    ResendVerificationRequest,
    LogoutRequest
} from '../types';

export const authService = {
    login: (email: string, password: string, rememberMe: boolean) =>
        fetchApi<AuthResponse>('/auth/login', {
            method: 'POST',
            body: JSON.stringify(new LoginRequest(email, password, rememberMe)),
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

    forgotPassword: (email: string) =>
        fetchApi<void>('/auth/forgot-password', {
            method: 'POST',
            body: JSON.stringify(new ForgotPasswordRequest(email)),
            token: ''
        }),

    resetPassword: (token: string, newPassword: string) =>
        fetchApi<void>('/auth/reset-password', {
            method: 'POST',
            body: JSON.stringify(new ResetPasswordRequest(token, newPassword)),
            token: ''
        }),

    verifyEmail: (token: string) =>
        fetchApi<void>('/auth/verify-email', {
            method: 'POST',
            body: JSON.stringify(new VerifyEmailRequest(token)),
            token: ''
        }),

    resendVerification: (email: string) =>
        fetchApi<void>('/auth/resend-verification', {
            method: 'POST',
            body: JSON.stringify(new ResendVerificationRequest(email)),
            token: ''
        }),

    logout: (token: string) =>
        fetchApi<void>('/auth/logout', {
            method: 'POST',
            body: JSON.stringify(new LogoutRequest(token))
        })
};
