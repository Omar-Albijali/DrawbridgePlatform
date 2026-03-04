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
            <div className="min-h-screen bg-gradient-to-br from-navy-900 via-navy-800 to-primary-900 flex items-center justify-center p-4">
                <div className="bg-white rounded-2xl shadow-2xl p-8 max-w-md w-full text-center">
                    <h2 className="text-xl font-bold text-navy-900 mb-2">Invalid Link</h2>
                    <p className="text-navy-500 mb-4">No reset token was provided.</p>
                    <Link to="/forgot-password" className="btn-primary px-6 py-2">Request a new link</Link>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gradient-to-br from-navy-900 via-navy-800 to-primary-900 flex items-center justify-center p-4">
            <div className="absolute inset-0 bg-[url('data:image/svg+xml,%3Csvg%20width%3D%2260%22%20height%3D%2260%22%20viewBox%3D%220%200%2060%2060%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Cg%20fill%3D%22none%22%20fill-rule%3D%22evenodd%22%3E%3Cg%20fill%3D%22%23ffffff%22%20fill-opacity%3D%220.03%22%3E%3Cpath%20d%3D%22M36%2034v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zM36%2034v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zM6%2034v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6%204V0H4v4H0v2h4v4h2V6h4V4H6z%22%2F%3E%3C%2Fg%3E%3C%2Fg%3E%3C%2Fsvg%3E')] opacity-50" />

            <div className="relative w-full max-w-md">
                {/* Logo */}
                <div className="text-center mb-8">
                    <div className="inline-flex items-center justify-center w-16 h-16 bg-white rounded-2xl shadow-lg mb-4">
                        <svg className="w-10 h-10 text-primary-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M3 21h18M5 21V7l8-4 8 4v14M9 21v-8h6v8" />
                        </svg>
                    </div>
                    <h1 className="text-3xl font-bold text-white mb-2">Drawbridge</h1>
                    <p className="text-navy-300">B2B Commerce Platform</p>
                </div>

                <div className="bg-white rounded-2xl shadow-2xl p-8">
                    {success ? (
                        <div className="text-center space-y-4">
                            <div className="flex justify-center">
                                <CheckCircle className="w-16 h-16 text-green-500" />
                            </div>
                            <h2 className="text-2xl font-bold text-navy-900">Password updated!</h2>
                            <p className="text-navy-500">
                                Your password has been changed successfully. Redirecting you to sign in…
                            </p>
                            <Link to="/login" className="inline-block mt-2 text-primary-600 hover:text-primary-700 font-medium">
                                Go to Sign In
                            </Link>
                        </div>
                    ) : (
                        <>
                            <h2 className="text-2xl font-bold text-navy-900 mb-2">Set a new password</h2>
                            <p className="text-navy-500 mb-6">
                                Choose a strong password of at least 8 characters.
                            </p>

                            {error && (
                                <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
                                    {error}
                                </div>
                            )}

                            <form onSubmit={handleSubmit} className="space-y-5">
                                {/* New Password */}
                                <div>
                                    <label htmlFor="newPassword" className="label">New Password</label>
                                    <div className="relative">
                                        <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-navy-400" />
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
                                            className="absolute right-3 top-1/2 -translate-y-1/2 text-navy-400 hover:text-navy-600"
                                        >
                                            {showPasswords.newPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                                        </button>
                                    </div>
                                </div>

                                {/* Confirm Password */}
                                <div>
                                    <label htmlFor="confirm" className="label">Confirm New Password</label>
                                    <div className="relative">
                                        <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-navy-400" />
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
                                            className="absolute right-3 top-1/2 -translate-y-1/2 text-navy-400 hover:text-navy-600"
                                        >
                                            {showPasswords.confirm ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                                        </button>
                                    </div>
                                    {mismatch && (
                                        <p className="text-xs text-red-500 mt-1">Passwords do not match</p>
                                    )}
                                </div>

                                <button
                                    type="submit"
                                    disabled={!isValid || isSubmitting}
                                    className="w-full btn-primary flex items-center justify-center gap-2 py-3 text-lg disabled:opacity-50"
                                >
                                    {isSubmitting ? (
                                        <div className="w-6 h-6 border-2 border-white border-t-transparent rounded-full animate-spin" />
                                    ) : (
                                        'Set New Password'
                                    )}
                                </button>
                            </form>

                            <div className="mt-6 text-center">
                                <Link to="/login" className="text-sm text-navy-500 hover:text-navy-700 font-medium">
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

