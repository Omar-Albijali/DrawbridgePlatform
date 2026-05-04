import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Mail, ArrowLeft, CheckCircle } from 'lucide-react';
import { authService } from '../services/authService';

const ForgotPassword: React.FC = () => {
  const { t } = useTranslation();
  const [email, setEmail] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsSubmitting(true);

    try {
      await authService.forgotPassword(email);
      setSubmitted(true);
    } catch {
      setError(t('auth.errors.generic'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="relative grid min-h-screen place-items-center overflow-hidden px-4 py-10">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_8%_20%,rgba(16,185,129,0.2),transparent_40%),radial-gradient(circle_at_86%_14%,rgba(14,165,233,0.18),transparent_44%)]" />

      <div className="relative z-10 w-full max-w-xl space-y-5">
        <div className="text-center">
          <div className="mx-auto mb-3 grid h-14 w-14 place-items-center rounded-2xl border border-slate-200 bg-white/70 text-primary-600 shadow-lg shadow-primary-500/20 dark:border-white/10 dark:bg-slate-900/70 dark:text-primary-300">
            <svg className="h-7 w-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M3 21h18M5 21V7l8-4 8 4v14M9 21v-8h6v8" />
            </svg>
          </div>
          <h1 className="text-3xl font-black tracking-tight text-slate-900 dark:text-white">Drawbridge</h1>
          <p className="mt-1 text-slate-500 dark:text-slate-300">{t('common.tagline')}</p>
        </div>

        <div className="glass-panel rounded-3xl p-6 sm:p-8">
          {submitted ? (
            <div className="space-y-4 text-center">
              <div className="flex justify-center">
                <CheckCircle className="h-16 w-16 text-green-500" />
              </div>
              <h2 className="text-2xl font-bold text-slate-900 dark:text-slate-100">{t('auth.forgot.checkEmailTitle')}</h2>
              <p className="text-slate-500 dark:text-slate-300">
                {t('auth.forgot.checkEmailMessage', { email })}
              </p>
              <Link to="/login" className="mt-4 inline-flex items-center gap-2 font-semibold text-primary-700 hover:text-primary-600 dark:text-primary-300">
                <ArrowLeft className="h-4 w-4" />
                {t('auth.forgot.backToSignIn')}
              </Link>
            </div>
          ) : (
            <>
              <h2 className="text-2xl font-bold text-slate-900 dark:text-slate-100">{t('auth.forgot.title')}</h2>
              <p className="mb-6 mt-1 text-sm text-slate-500 dark:text-slate-300">
                {t('auth.forgot.description')}
              </p>

              {error && (
                <div className="mb-4 rounded-xl border border-red-300/60 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-400/30 dark:bg-red-500/10 dark:text-red-300">
                  {error}
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-5">
                <div>
                  <label htmlFor="email" className="label">{t('auth.emailAddress')}</label>
                  <div className="relative">
                    <Mail className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                    <input
                      id="email"
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      className="input pl-10"
                      placeholder={t('auth.enterEmail')}
                      required
                    />
                  </div>
                </div>

                <button type="submit" disabled={isSubmitting} className="btn-primary w-full rounded-xl py-3 disabled:opacity-50">
                  {isSubmitting ? (
                    <div className="mx-auto h-6 w-6 animate-spin rounded-full border-2 border-slate-900 border-t-transparent" />
                  ) : (
                    t('auth.forgot.sendResetLink')
                  )}
                </button>
              </form>

              <div className="mt-6 text-center">
                <Link to="/login" className="inline-flex items-center gap-2 text-sm font-semibold text-primary-700 hover:text-primary-600 dark:text-primary-300">
                  <ArrowLeft className="h-4 w-4" />
                  {t('auth.forgot.backToSignIn')}
                </Link>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default ForgotPassword;
