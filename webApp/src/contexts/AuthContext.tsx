import React, { createContext, useContext, useState, useCallback, ReactNode } from 'react';
import { User, AuthResponse, ErrorResponse, UserRole, VerificationStatus } from '../types';

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

interface AuthContextType {
    user: User | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    login: (email: string, password: string) => Promise<boolean>;
    register: (userData: Partial<User> & { password: string }) => Promise<boolean>;
    logout: () => void;
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
    const [user, setUser] = useState<User | null>(() => {
        // Check for saved user in localStorage
        return safeParseJson<User>(localStorage.getItem(STORAGE_USER_KEY));
    });
    const [isLoading, setIsLoading] = useState(0);

    const login = useCallback(async (email: string, password: string): Promise<boolean> => {
        setIsLoading(prev => prev + 1);

        try {
            const res = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });

            if (!res.ok) {
                // Best-effort to read server error for debugging, but keep UI behavior simple.
                try {
                    await res.json() as ErrorResponse;
                } catch {
                    // ignore
                }
                return false;
            }

            const authData = await res.json() as AuthResponse;
            const token = authData.token;
            let finalUser: User;

            // Fetch full profile
            try {
                const userRes = await fetch(`/api/users/${authData.userId}`, {
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });

                if (userRes.ok) {
                    // We need to properly instantiate the User object or at least map Enums
                    // The JSON will return strings for Enums, but our TypeScript types expect Enum objects
                    const userJson = await userRes.json();
                    finalUser = {
                        ...userJson,
                        role: UserRole.valueOf(userJson.role as unknown as string),
                        verificationStatus: userJson.verificationStatus ? VerificationStatus.valueOf(userJson.verificationStatus as unknown as string) : null
                    } as unknown as User;
                } else {
                    // Fallback to auth data if fetch fails
                    throw new Error('Failed to fetch profile');
                }
            } catch (e) {
                console.warn("Falling back to basic auth data", e);
                // Fallback: Cast plain object to User (imperfect but better than nothing)
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

            localStorage.setItem(STORAGE_TOKEN_KEY, token);
            localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(finalUser));
            setUser(finalUser);
            return true;
        } catch {
            return false;
        } finally {
            setIsLoading(prev => prev - 1);
        }
    }, []);

    const register = useCallback(async (userData: Partial<User> & { password: string }): Promise<boolean> => {
        setIsLoading(prev => prev + 1);

        try {
            const res = await fetch('/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    email: userData.email ?? '',
                    password: userData.password,
                    phoneNumber: userData.phone ?? '',
                    role: userData.role,
                    businessName: userData.company ?? null,
                    commercialRegistrationNumber: userData.commercialRegister ?? '',
                    // Representative fields (flattened)
                    repName: userData.representative?.name ?? '',
                    repJobTitle: userData.representative?.jobTitle ?? '',
                    repPhoneNumber: userData.representative?.phoneNumber ?? '',
                    repEmail: userData.representative?.email ?? '',
                    addresses: userData.addresses ?? []
                })
            });

            if (!res.ok) {
                try {
                    await res.json() as ErrorResponse;
                } catch {
                    // ignore
                }
                return false;
            }

            const data = await res.json() as AuthResponse;

            const newUser: User = {
                id: data.userId,
                name: data.name,
                email: data.email,
                role: data.role,
                company: userData.company ?? '',
                phone: userData.phone ?? null,
                addresses: userData.addresses ?? null,
                representative: userData.representative ?? null,
                commercialRegister: userData.commercialRegister ?? null,
                verificationStatus: userData.verificationStatus ?? null,
                avatar: userData.avatar ?? null
            } as unknown as User;

            localStorage.setItem(STORAGE_TOKEN_KEY, data.token);
            localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(newUser));
            setUser(newUser);
            return true;
        } catch {
            return false;
        } finally {
            setIsLoading(prev => prev - 1);
        }
    }, []);

    const logout = useCallback(() => {
        setUser(null);
        localStorage.removeItem(STORAGE_USER_KEY);
        localStorage.removeItem(STORAGE_TOKEN_KEY);
    }, []);

    const refreshUser = useCallback(async () => {
        if (!user?.id) return;

        setIsLoading(prev => prev + 1);
        try {
            const token = localStorage.getItem(STORAGE_TOKEN_KEY);
            if (!token) return;

            const userRes = await fetch(`/api/users/${user.id}`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (userRes.ok) {
                const userJson = await userRes.json();
                const updatedUser = {
                    ...userJson,
                    role: UserRole.valueOf(userJson.role as unknown as string),
                    verificationStatus: userJson.verificationStatus ? VerificationStatus.valueOf(userJson.verificationStatus as unknown as string) : null
                } as unknown as User;

                localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(updatedUser));
                setUser(updatedUser);
            }
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
