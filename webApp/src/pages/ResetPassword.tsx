import React, { useState } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import { Lock, Eye, EyeOff, CheckCircle } from 'lucide-react';
import { authService } from '../services/authService';

const ResetPassword: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token') ?? '';

  const [passwords, setPasswords] = useState({ newPassword: '', confirm: '' });
  const [showPasswords, setShowPasswords] = useState({ newPassword: false, confirm: false });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const mismatch = passwords.newPassword && passwords.confirm && passwords.newPassword !== passwords.confirm;
  const isValid = passwords.newPassword.length >= 8 && passwords.newPassword === passwords.confirm;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isValid || !token) return;
    setError('');
    setIsSubmitting(true);

    try {
      await authService.resetPassword(token, passwords.newPassword);
      setSuccess(true);
      setTimeout(() => navigate('/login'), 3000);
    } catch {
      setError('This reset link is invalid or has expired. Please request a new one.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!token) {
    return (
      <div className="relative grid min-h-screen place-items-center overflow-hidden px-4 py-10">
        <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_8%_20%,rgba(16,185,129,0.2),transparent_40%),radial-gradient(circle_at_86%_14%,rgba(14,165,233,0.18),transparent_44%)]" />
        <div className="glass-panel relative z-10 w-full max-w-md rounded-3xl p-6 text-center sm:p-8">
          <h2 className="mb-2 text-2xl font-bold text-slate-900 dark:text-slate-100">Invalid Link</h2>
          <p className="mb-4 text-slate-500 dark:text-slate-300">No reset token was provided.</p>
          <Link to="/forgot-password" className="btn-primary rounded-xl px-5 py-2.5">Request a new link</Link>
        </div>
      </div>
    );
  }

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

        <div className="glass-panel rounded-3xl p-6 sm:p-8">
          {success ? (
            <div className="space-y-4 text-center">
              <div className="flex justify-center">
                <CheckCircle className="h-16 w-16 text-green-500" />
              </div>
              <h2 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Password updated!</h2>
              <p className="text-slate-500 dark:text-slate-300">
                Your password has been changed successfully. Redirecting you to sign in...
              </p>
              <Link to="/login" className="inline-block font-semibold text-primary-700 hover:text-primary-600 dark:text-primary-300">
                Go to Sign In
              </Link>
            </div>
          ) : (
            <>
              <h2 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Set a new password</h2>
              <p className="mb-6 mt-1 text-sm text-slate-500 dark:text-slate-300">Choose a strong password of at least 8 characters.</p>

              {error && (
                <div className="mb-4 rounded-xl border border-red-300/60 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-400/30 dark:bg-red-500/10 dark:text-red-300">
                  {error}
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-5">
                <div>
                  <label htmlFor="newPassword" className="label">New Password</label>
                  <div className="relative">
                    <Lock className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                    <input
                      id="newPassword"
                      type={showPasswords.newPassword ? 'text' : 'password'}
                      value={passwords.newPassword}
                      onChange={e => setPasswords(p => ({ ...p, newPassword: e.target.value }))}
                      className="input pl-10 pr-10"
                      placeholder="At least 8 characters"
                      required
                      minLength={8}
                    />
                    <button
                      type="button"
                      onClick={() => setShowPasswords(p => ({ ...p, newPassword: !p.newPassword }))}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
                    >
                      {showPasswords.newPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </div>

                <div>
                  <label htmlFor="confirm" className="label">Confirm New Password</label>
                  <div className="relative">
                    <Lock className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                    <input
                      id="confirm"
                      type={showPasswords.confirm ? 'text' : 'password'}
                      value={passwords.confirm}
                      onChange={e => setPasswords(p => ({ ...p, confirm: e.target.value }))}
                      className="input pl-10 pr-10"
                      placeholder="Repeat new password"
                      required
                    />
                    <button
                      type="button"
                      onClick={() => setShowPasswords(p => ({ ...p, confirm: !p.confirm }))}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
                    >
                      {showPasswords.confirm ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                  {mismatch && (
                    <p className="mt-1 text-xs text-red-500">Passwords do not match</p>
                  )}
                </div>

                <button
                  type="submit"
                  disabled={!isValid || isSubmitting}
                  className="btn-primary w-full rounded-xl py-3 disabled:opacity-50"
                >
                  {isSubmitting ? (
                    <div className="mx-auto h-6 w-6 animate-spin rounded-full border-2 border-slate-900 border-t-transparent" />
                  ) : (
                    'Set New Password'
                  )}
                </button>
              </form>

              <div className="mt-6 text-center">
                <Link to="/login" className="text-sm font-semibold text-primary-700 hover:text-primary-600 dark:text-primary-300">
                  Back to Sign In
                </Link>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default ResetPassword;

