import React, { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { CheckCircle, AlertTriangle, MailCheck } from 'lucide-react';
import { authService } from '../services/authService';

const VerifyEmail: React.FC = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') ?? '';
  const sentEmail = searchParams.get('email');
  const sentFlag = searchParams.get('sent') === '1';

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [status, setStatus] = useState<'idle' | 'success' | 'error'>('idle');

  const shouldVerify = useMemo(() => Boolean(token), [token]);

  useEffect(() => {
    if (!shouldVerify) return;

    const verify = async () => {
      setIsSubmitting(true);
      try {
        await authService.verifyEmail(token);
        setStatus('success');
      } catch {
        setStatus('error');
      } finally {
        setIsSubmitting(false);
      }
    };

    verify();
  }, [shouldVerify, token]);

  const showSentMessage = sentFlag && !token;

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
          <p className="mt-1 text-slate-500 dark:text-slate-300">B2B Commerce Platform</p>
        </div>

        <div className="glass-panel rounded-3xl p-6 text-center sm:p-8">
          {showSentMessage ? (
            <>
              <div className="mb-4 flex justify-center">
                <MailCheck className="h-16 w-16 text-primary-600 dark:text-primary-300" />
              </div>
              <h2 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Check your email</h2>
              <p className="mt-2 text-slate-500 dark:text-slate-300">
                We sent a verification link to <strong>{sentEmail ?? 'your email'}</strong>. Open the link to verify your account.
              </p>
            </>
          ) : token ? (
            status === 'success' ? (
              <>
                <div className="mb-4 flex justify-center">
                  <CheckCircle className="h-16 w-16 text-green-500" />
                </div>
                <h2 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Email verified!</h2>
                <p className="mt-2 text-slate-500 dark:text-slate-300">Your account is ready to use.</p>
              </>
            ) : status === 'error' ? (
              <>
                <div className="mb-4 flex justify-center">
                  <AlertTriangle className="h-16 w-16 text-red-500" />
                </div>
                <h2 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Verification failed</h2>
                <p className="mt-2 text-slate-500 dark:text-slate-300">This verification link is invalid or has expired.</p>
              </>
            ) : (
              <>
                <div className="mb-4 flex justify-center">
                  <div className="h-16 w-16 animate-spin rounded-full border-4 border-primary-600 border-t-transparent" />
                </div>
                <h2 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Verifying...</h2>
                <p className="mt-2 text-slate-500 dark:text-slate-300">Please wait while we confirm your email.</p>
              </>
            )
          ) : (
            <>
              <div className="mb-4 flex justify-center">
                <AlertTriangle className="h-16 w-16 text-amber-500" />
              </div>
              <h2 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Missing token</h2>
              <p className="mt-2 text-slate-500 dark:text-slate-300">No verification token was provided.</p>
            </>
          )}

          <div className="mt-6 flex flex-col gap-3">
            <Link to="/login" className="btn-primary w-full rounded-xl py-3">
              Go to Sign In
            </Link>
            {isSubmitting && (
              <p className="text-xs text-slate-500 dark:text-slate-300">Finishing verification...</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default VerifyEmail;

