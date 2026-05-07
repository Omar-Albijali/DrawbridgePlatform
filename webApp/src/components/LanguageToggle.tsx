import { Languages } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import type { SupportedLanguage } from '../i18n';

export default function LanguageToggle(): JSX.Element {
  const { i18n, t } = useTranslation();
  const currentLanguage: SupportedLanguage = i18n.resolvedLanguage === 'ar' ? 'ar' : 'en';
  const nextLanguage: SupportedLanguage = currentLanguage === 'ar' ? 'en' : 'ar';
  const visibleLabel = t(nextLanguage === 'ar' ? 'common.language.arabicShort' : 'common.language.englishShort');
  const accessibleLabel = t(
    nextLanguage === 'ar' ? 'common.language.switchToArabic' : 'common.language.switchToEnglish',
  );

  const handleToggle = (): void => {
    void i18n.changeLanguage(nextLanguage);
  };

  return (
    <button
      type="button"
      onClick={handleToggle}
      className="inline-flex h-10 items-center justify-center gap-1.5 rounded-full border border-slate-300 bg-slate-100 px-2.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-200 dark:border-white/15 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800"
      aria-label={accessibleLabel}
      title={accessibleLabel}
    >
      <Languages className="h-4 w-4" />
      <span>{visibleLabel}</span>
    </button>
  );
}
