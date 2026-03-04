import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { authService } from '../services/authService';
import { Mail, Lock, Eye, EyeOff, ArrowRight } from 'lucide-react';

const Login: React.FC = () => {
	const [email, setEmail] = useState('');
	const [password, setPassword] = useState('');
	const [rememberMe, setRememberMe] = useState(false);
	const [showPassword, setShowPassword] = useState(false);
	const [error, setError] = useState('');
	const [info, setInfo] = useState('');
	const [isSubmitting, setIsSubmitting] = useState(false);
	const [isResending, setIsResending] = useState(false);
	const [showResend, setShowResend] = useState(false);

	const { login } = useAuth();
	const navigate = useNavigate();
	const location = useLocation();

	const getReturnPath = (): string => {
		const fromState = location.state as { from?: { pathname?: string } } | null;
		const statePath = fromState?.from?.pathname;
		const queryPath = new URLSearchParams(location.search).get('returnTo');
		const candidate = queryPath ?? statePath ?? '/dashboard';
		return candidate.startsWith('/') ? candidate : '/dashboard';
	};

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault();
		setError('');
		setInfo('');
		setShowResend(false);
		setIsSubmitting(true);

		const result = await login(email, password, rememberMe);

		if (result.success) {
			navigate(getReturnPath(), { replace: true });
		} else if (result.reason === 'unverified') {
			setError('Your email is not verified yet.');
			setShowResend(Boolean(email.trim()));
		} else {
			setError('Invalid email or password. Use retailer@test.com / wholesaler@test.com and password: password');
		}

		setIsSubmitting(false);
	};

	const handleResendVerification = async () => {
		setError('');
		setInfo('');

		if (!email.trim()) {
			setError('Enter your email to resend the verification link.');
			return;
		}

		setIsResending(true);
		try {
			await authService.resendVerification(email.trim());
			setInfo("If your account exists and is unverified, we've sent a new verification link.");
		} catch {
			setError('Unable to resend the verification link. Please try again.');
		} finally {
			setIsResending(false);
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
					<p className="mt-1 text-slate-500 dark:text-slate-300">B2B Commerce Platform</p>
				</div>

				<div className="glass-panel rounded-3xl p-6 sm:p-8">
					<h2 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Welcome back</h2>
					<p className="mt-1 text-sm text-slate-500 dark:text-slate-300">Sign in to your account to continue</p>

					{error && (
						<div className="mt-4 rounded-xl border border-red-300/60 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-400/30 dark:bg-red-500/10 dark:text-red-300">
							<span>{error}</span>
							{showResend && (
								<button
									type="button"
									onClick={handleResendVerification}
									disabled={isResending}
									className="ml-1 inline-flex items-center font-semibold underline underline-offset-2 disabled:opacity-50"
								>
									{isResending ? 'Sending verification link...' : 'Resend verification link'}
								</button>
							)}
						</div>
					)}
					{info && (
						<div className="mt-4 rounded-xl border border-green-300/60 bg-green-50 px-4 py-3 text-sm text-green-700 dark:border-green-400/30 dark:bg-green-500/10 dark:text-green-300">
							{info}
						</div>
					)}

					<form onSubmit={handleSubmit} className="mt-5 space-y-5">
						<div>
							<label htmlFor="email" className="label">Email Address</label>
							<div className="relative">
								<Mail className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
								<input
									id="email"
									type="email"
									value={email}
									onChange={(e) => setEmail(e.target.value)}
									className="input pl-10"
									placeholder="Enter your email"
									required
								/>
							</div>
						</div>

						<div>
							<label htmlFor="password" className="label">Password</label>
							<div className="relative">
								<Lock className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
								<input
									id="password"
									type={showPassword ? 'text' : 'password'}
									value={password}
									onChange={(e) => setPassword(e.target.value)}
									className="input pl-10 pr-10"
									placeholder="Enter your password"
									required
								/>
								<button
									type="button"
									onClick={() => setShowPassword(!showPassword)}
									className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 transition hover:text-slate-600 dark:hover:text-slate-200"
								>
									{showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
								</button>
							</div>
						</div>

						<div className="flex items-center justify-between">
							<label className="flex items-center gap-2 text-sm text-slate-600 dark:text-slate-300">
								<input
									type="checkbox"
									checked={rememberMe}
									onChange={(e) => setRememberMe(e.target.checked)}
									className="h-4 w-4 rounded border-slate-300 text-primary-600"
								/>
								<span>Remember me</span>
							</label>
							<Link to="/forgot-password" className="text-sm font-semibold text-primary-700 hover:text-primary-600 dark:text-primary-300">
								Forgot password?
							</Link>
						</div>

						<button type="submit" disabled={isSubmitting} className="btn-primary w-full gap-2 rounded-xl py-3 disabled:opacity-50">
							{isSubmitting ? (
								<div className="h-6 w-6 animate-spin rounded-full border-2 border-slate-900 border-t-transparent" />
							) : (
								<>
									Sign In
									<ArrowRight className="h-5 w-5" />
								</>
							)}
						</button>
					</form>

					<div className="mt-6 text-center text-sm text-slate-500 dark:text-slate-300">
						<p>
							Don&apos;t have an account?{' '}
							<Link to="/register" className="font-semibold text-primary-700 hover:text-primary-600 dark:text-primary-300">
								Sign Up
							</Link>
						</p>
					</div>

					<div className="mt-6 rounded-xl border border-slate-200 bg-white/70 p-4 dark:border-white/10 dark:bg-slate-900/70">
						<p className="text-center text-xs text-slate-500 dark:text-slate-300">
							<strong>Demo Email:</strong> retailer@test.com or wholesaler@test.com
							<br />
							<strong>Password:</strong> password
						</p>
					</div>
				</div>
			</div>
		</div>
	);
};

export default Login;
