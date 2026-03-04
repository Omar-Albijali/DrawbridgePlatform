import { fetchApi } from './api';
import { UserRole } from 'shared';
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

const getRoleName = (role: unknown): string => {
    const enumName = (role as { name?: string } | null | undefined)?.name;
    if (enumName) {
        return enumName;
    }

    if (typeof role === 'string' && role.trim().length > 0) {
        return role;
    }

    // Runtime fallback to a known shared enum value.
    return UserRole.RETAILER.name;
};

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
            role: getRoleName(request.role)
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
