import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react';

const STORAGE_USER_KEY = 'drawbridge_user';
const STORAGE_TOKEN_KEY = 'drawbridge_token';

export type UserRole = 'RETAILER' | 'SUPPLIER' | 'ADMIN';

export interface AuthUser {
  id: string;
  name: string;
  email: string;
  role: UserRole;
}

interface LoginResult {
  success: boolean;
  reason?: 'unverified' | 'invalid' | 'unknown';
  message?: string;
}

interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

interface AuthContextType {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string, rememberMe: boolean) => Promise<LoginResult>;
  register: (request: RegisterRequest) => Promise<boolean>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

function safeParseUser(value: string | null): AuthUser | null {
  if (!value) {
    return null;
  }

  try {
    return JSON.parse(value) as AuthUser;
  } catch {
    return null;
  }
}

function getStorageForToken(): Storage | null {
  if (localStorage.getItem(STORAGE_TOKEN_KEY)) {
    return localStorage;
  }
  if (sessionStorage.getItem(STORAGE_TOKEN_KEY)) {
    return sessionStorage;
  }
  return null;
}

function clearStoredAuth(): void {
  localStorage.removeItem(STORAGE_TOKEN_KEY);
  localStorage.removeItem(STORAGE_USER_KEY);
  sessionStorage.removeItem(STORAGE_TOKEN_KEY);
  sessionStorage.removeItem(STORAGE_USER_KEY);
}

function readStoredUser(): AuthUser | null {
  const storage = getStorageForToken();
  if (!storage) {
    return null;
  }
  return safeParseUser(storage.getItem(STORAGE_USER_KEY));
}

export function AuthProvider({ children }: { children: ReactNode }): JSX.Element {
  const [user, setUser] = useState<AuthUser | null>(() => readStoredUser());
  const [pendingOps, setPendingOps] = useState(0);

  const login = useCallback(
    async (email: string, password: string, rememberMe: boolean): Promise<LoginResult> => {
      setPendingOps((count) => count + 1);

      try {
        if (!email || !password) {
          return { success: false, reason: 'invalid', message: 'Email and password are required' };
        }

        const nextUser: AuthUser = {
          id: 'local-user',
          name: email.split('@')[0] ?? 'User',
          email,
          role: 'RETAILER',
        };

        const token = `local-token-${Date.now()}`;
        const storage = rememberMe ? localStorage : sessionStorage;

        clearStoredAuth();
        storage.setItem(STORAGE_TOKEN_KEY, token);
        storage.setItem(STORAGE_USER_KEY, JSON.stringify(nextUser));
        setUser(nextUser);

        return { success: true };
      } finally {
        setPendingOps((count) => count - 1);
      }
    },
    [],
  );

  const register = useCallback(async (request: RegisterRequest): Promise<boolean> => {
    setPendingOps((count) => count + 1);

    try {
      if (!request.email || !request.password) {
        return false;
      }

      const nextUser: AuthUser = {
        id: `local-${Date.now()}`,
        name: request.name || request.email.split('@')[0] || 'User',
        email: request.email,
        role: 'RETAILER',
      };

      clearStoredAuth();
      localStorage.setItem(STORAGE_TOKEN_KEY, `local-token-${nextUser.id}`);
      localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(nextUser));
      setUser(nextUser);

      return true;
    } finally {
      setPendingOps((count) => count - 1);
    }
  }, []);

  const logout = useCallback(async (): Promise<void> => {
    clearStoredAuth();
    setUser(null);
  }, []);

  const refreshUser = useCallback(async (): Promise<void> => {
    setUser(readStoredUser());
  }, []);

  const value = useMemo<AuthContextType>(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      isLoading: pendingOps > 0,
      login,
      register,
      logout,
      refreshUser,
    }),
    [login, logout, pendingOps, refreshUser, register, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
