import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';

interface ThemeContextType {
  dark: boolean;
  toggleTheme: () => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export function ThemeProvider({ children }: { children: ReactNode }): JSX.Element {
  const [dark, setDark] = useState<boolean>(() => localStorage.theme !== 'light');

  useEffect(() => {
    if (dark) {
      document.documentElement.classList.add('dark');
      localStorage.theme = 'dark';
      return;
    }

    document.documentElement.classList.remove('dark');
    localStorage.theme = 'light';
  }, [dark]);

  const value = useMemo(
    () => ({
      dark,
      toggleTheme: () => setDark((currentValue) => !currentValue),
    }),
    [dark],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme(): ThemeContextType {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
}
