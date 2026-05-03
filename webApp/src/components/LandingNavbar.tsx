import { Link } from 'react-router-dom';
import { Moon, Sun } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useTheme } from '../contexts/ThemeContext';
import LanguageToggle from './LanguageToggle';

export default function LandingNavbar(): JSX.Element {
  const { dark, toggleTheme } = useTheme();
  const { t } = useTranslation();

  const scrollToTop = (): void => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <nav className="glass-nav fixed inset-x-0 top-0 z-50 px-4 py-4 sm:px-6 lg:px-8">
      <div className="mx-auto flex w-full max-w-7xl items-center justify-between gap-4">
        <button
          type="button"
          className="hover-target inline-flex items-center gap-3 text-slate-900 transition hover:text-primary-600 dark:text-white"
          onClick={scrollToTop}
        >
          <span className="grid h-10 w-10 place-items-center rounded-xl bg-primary-500/20 text-sm font-black tracking-wider text-primary-700 dark:text-primary-300">
            DB
          </span>
          <span className="text-2xl font-black tracking-tight">Drawbridge</span>
        </button>

        <div className="hidden items-center gap-8 text-sm font-semibold text-slate-500 dark:text-slate-300 md:flex">
          <a href="#features" className="hover-target transition hover:text-primary-500">
            {t('navigation.features')}
          </a>
          <Link to="/marketplace" className="hover-target transition hover:text-primary-500">
            {t('navigation.marketplace')}
          </Link>
          <a href="#contact" className="hover-target transition hover:text-primary-500">
            {t('navigation.contact')}
          </a>
        </div>

        <div className="flex items-center gap-3 sm:gap-5">
          <LanguageToggle />

          <button
            type="button"
            onClick={toggleTheme}
            className="grid h-10 w-10 place-items-center rounded-full border border-slate-300 bg-slate-100 text-slate-700 transition hover:bg-slate-200 dark:border-white/15 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800"
            aria-label={dark ? t('common.theme.switchToLight') : t('common.theme.switchToDark')}
            title={dark ? t('common.theme.light') : t('common.theme.dark')}
          >
            {dark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
          </button>

          <Link
            to="/login"
            className="hover-target hidden text-sm font-semibold text-slate-700 transition hover:text-primary-600 dark:text-slate-100 dark:hover:text-primary-300 sm:block"
          >
            {t('navigation.signIn')}
          </Link>
          <Link to="/register" className="hover-target btn-primary rounded-lg px-5 py-2.5 text-sm">
            {t('navigation.getStarted')}
          </Link>
        </div>
      </div>
    </nav>
  );
}
