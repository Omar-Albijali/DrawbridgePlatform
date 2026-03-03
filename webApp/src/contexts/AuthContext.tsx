import React, { createContext, useContext, useState, useCallback, ReactNode } from 'react';
import { User, UserRole, VerificationStatus, RegisterRequest } from '../types';
import { authService } from '../services/authService';
import { userService } from '../services/userService';

const STORAGE_USER_KEY = 'drawbridge_user';
const STORAGE_TOKEN_KEY = 'drawbridge_token';

function safeParseJson<T>(value: string | null): T | null {
    if (!value) return null;
    try {
        return JSON.parse(value) as T;
    } catch {
        return null;
    }
}

interface LoginResult {
    success: boolean;
    reason?: 'unverified' | 'invalid' | 'unknown';
    message?: string;
}

interface AuthContextType {
    user: User | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    login: (email: string, password: string, rememberMe: boolean) => Promise<LoginResult>;
    register: (request: RegisterRequest) => Promise<boolean>;
    logout: () => Promise<void>;
    refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

interface AuthProviderProps {
    children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
    const getStorageForToken = (): Storage | null => {
        if (localStorage.getItem(STORAGE_TOKEN_KEY)) return localStorage;
        if (sessionStorage.getItem(STORAGE_TOKEN_KEY)) return sessionStorage;
        return null;
    };

    const getStoredUser = (): User | null => {
        const storage = getStorageForToken();
        if (!storage) return null;
        return safeParseJson<User>(storage.getItem(STORAGE_USER_KEY));
    };

    const clearStoredAuth = () => {
        localStorage.removeItem(STORAGE_USER_KEY);
        localStorage.removeItem(STORAGE_TOKEN_KEY);
        sessionStorage.removeItem(STORAGE_USER_KEY);
        sessionStorage.removeItem(STORAGE_TOKEN_KEY);
    };

    const setStoredAuth = (storage: Storage, token: string, nextUser: User) => {
        storage.setItem(STORAGE_TOKEN_KEY, token);
        storage.setItem(STORAGE_USER_KEY, JSON.stringify(nextUser));
    };

    const [user, setUser] = useState<User | null>(() => {
        return getStoredUser();
    });
    const [isLoading, setIsLoading] = useState(0);

    const mapUserEnums = (userJson: any): User => ({
        ...userJson,
        role: UserRole.valueOf(userJson.role as unknown as string),
        verificationStatus: userJson.verificationStatus
            ? VerificationStatus.valueOf(userJson.verificationStatus as unknown as string)
            : null
    }) as unknown as User;

    const extractErrorMessage = (error: unknown): string => {
        if (!(error instanceof Error)) return '';
        const message = error.message ?? '';
        const parts = message.split(' - ');
        const rawBody = parts[parts.length - 1];
        try {
            const parsed = JSON.parse(rawBody);
            if (typeof parsed?.message === 'string') {
                return parsed.message;
            }
        } catch {
            // Ignore JSON parse errors and fall back to raw message.
        }
        return message;
    };

    const login = useCallback(async (email: string, password: string, rememberMe: boolean): Promise<LoginResult> => {
        setIsLoading(prev => prev + 1);

        try {
            const authData = await authService.login(email, password, rememberMe);
            const token = authData.token;

            const storage = rememberMe ? localStorage : sessionStorage;
            clearStoredAuth();
            storage.setItem(STORAGE_TOKEN_KEY, token);

            let finalUser: User;
            try {
                const userJson = await userService.getById(authData.userId);
                finalUser = mapUserEnums(userJson);
            } catch (e) {
                console.warn("Falling back to basic auth data", e);
                finalUser = {
                    id: authData.userId,
                    name: authData.name,
                    email: authData.email,
                    role: UserRole.valueOf(authData.role as unknown as string),
                    company: '',
                    phone: null,
                    addresses: null,
                    representative: null,
                    commercialRegister: null,
                    verificationStatus: null,
                    avatar: null
                } as unknown as User;
            }

            setStoredAuth(storage, token, finalUser);
            setUser(finalUser);
            return { success: true };
        } catch (error) {
            const errorMessage = extractErrorMessage(error);
            if (errorMessage.toLowerCase().includes('email not verified')) {
                return { success: false, reason: 'unverified', message: errorMessage };
            }
            return { success: false, reason: 'invalid', message: errorMessage || 'Invalid email or password' };
        } finally {
            setIsLoading(prev => prev - 1);
        }
    }, []);

    const register = useCallback(async (request: RegisterRequest): Promise<boolean> => {
        setIsLoading(prev => prev + 1);

        try {
            const data = await authService.register(request);

            const newUser: User = {
                id: data.userId,
                name: data.name,
                email: data.email,
                role: UserRole.valueOf(data.role as unknown as string),
                company: request.businessName ?? '',
                phone: request.phoneNumber ?? null,
                addresses: request.addresses ?? null,
                representative: {
                    name: request.repName,
                    jobTitle: request.repJobTitle,
                    phoneNumber: request.repPhoneNumber,
                    email: request.repEmail
                },
                commercialRegister: request.commercialRegistrationNumber ?? null,
                verificationStatus: null, // Initial status
                avatar: null
            } as unknown as User;

            clearStoredAuth();
            setStoredAuth(localStorage, data.token, newUser);
            setUser(newUser);
            return true;
        } catch {
            return false;
        } finally {
            setIsLoading(prev => prev - 1);
        }
    }, []);

    const logout = useCallback(async () => {
        const token = localStorage.getItem(STORAGE_TOKEN_KEY) || sessionStorage.getItem(STORAGE_TOKEN_KEY) || '';
        try {
            if (token) {
                await authService.logout(token);
            }
        } catch (error) {
            console.warn('Failed to invalidate session on logout', error);
        } finally {
            clearStoredAuth();
            setUser(null);
        }
    }, []);

    const refreshUser = useCallback(async () => {
        if (!user?.id) return;

        setIsLoading(prev => prev + 1);
        try {
            const tokenStorage = getStorageForToken();
            const token = tokenStorage?.getItem(STORAGE_TOKEN_KEY);
            if (!tokenStorage || !token) return;

            const userJson = await userService.getById(user.id);
            const updatedUser = mapUserEnums(userJson);

            tokenStorage.setItem(STORAGE_USER_KEY, JSON.stringify(updatedUser));
            setUser(updatedUser);
        } catch (error) {
            console.error("Failed to refresh user profile", error);
        } finally {
            setIsLoading(prev => prev - 1);
        }
    }, [user?.id]);

    const value: AuthContextType = {
        user,
        isAuthenticated: !!user,
        isLoading: isLoading > 0,
        login,
        register,
        logout,
        refreshUser
    };

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
};

export default AuthContext;
