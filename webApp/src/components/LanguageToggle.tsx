import { Languages } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import type { SupportedLanguage } from '../i18n';

export default function LanguageToggle(): JSX.Element {
  const { i18n, t } = useTranslation();
  const currentLanguage: SupportedLanguage = i18n.resolvedLanguage === 'ar' ? 'ar' : 'en';
  const nextLanguage: SupportedLanguage = currentLanguage === 'ar' ? 'en' : 'ar';
  const nextLanguageLabel = nextLanguage === 'ar' ? t('common.language.arabic') : t('common.language.english');

  const handleToggle = (): void => {
    void i18n.changeLanguage(nextLanguage);
  };

  return (
    <button
      type="button"
      onClick={handleToggle}
      className="inline-flex h-10 w-10 items-center justify-center gap-2 rounded-full border border-slate-300 bg-slate-100 px-0 text-sm font-semibold text-slate-700 transition hover:bg-slate-200 sm:w-auto sm:px-3 dark:border-white/15 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800"
      aria-label={t('common.language.switchTo', { language: nextLanguageLabel })}
      title={t('common.language.switchTo', { language: nextLanguageLabel })}
    >
      <Languages className="h-4 w-4" />
      <span className="hidden sm:inline">{nextLanguageLabel}</span>
    </button>
  );
}
