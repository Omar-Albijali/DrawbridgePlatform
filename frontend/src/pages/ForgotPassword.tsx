import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Mail, ArrowLeft, CheckCircle } from 'lucide-react';
import { authService } from '../services/authService';

const ForgotPassword: React.FC = () => {
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
            setError('Something went wrong. Please try again later.');
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-navy-900 via-navy-800 to-primary-900 flex items-center justify-center p-4">
            {/* Background Pattern */}
            <div className="absolute inset-0 bg-[url('data:image/svg+xml,%3Csvg%20width%3D%2260%22%20height%3D%2260%22%20viewBox%3D%220%200%2060%2060%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Cg%20fill%3D%22none%22%20fill-rule%3D%22evenodd%22%3E%3Cg%20fill%3D%22%23ffffff%22%20fill-opacity%3D%220.03%22%3E%3Cpath%20d%3D%22M36%2034v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6%2034v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6%204V0H4v4H0v2h4v4h2V6h4V4H6z%22%2F%3E%3C%2Fg%3E%3C%2Fg%3E%3C%2Fsvg%3E')] opacity-50" />

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
                    {submitted ? (
                        <div className="text-center space-y-4">
                            <div className="flex justify-center">
                                <CheckCircle className="w-16 h-16 text-green-500" />
                            </div>
                            <h2 className="text-2xl font-bold text-navy-900">Check your email</h2>
                            <p className="text-navy-500">
                                If an account exists for <strong>{email}</strong>, we've sent a password reset link to that address.
                            </p>
                            <Link
                                to="/login"
                                className="inline-flex items-center gap-2 text-primary-600 hover:text-primary-700 font-medium mt-4"
                            >
                                <ArrowLeft className="w-4 h-4" />
                                Back to Sign In
                            </Link>
                        </div>
                    ) : (
                        <>
                            <h2 className="text-2xl font-bold text-navy-900 mb-2">Forgot your password?</h2>
                            <p className="text-navy-500 mb-6">
                                Enter your email address and we'll send you a link to reset your password.
                            </p>

                            {error && (
                                <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
                                    {error}
                                </div>
                            )}

                            <form onSubmit={handleSubmit} className="space-y-5">
                                <div>
                                    <label htmlFor="email" className="label">Email Address</label>
                                    <div className="relative">
                                        <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-navy-400" />
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

                                <button
                                    type="submit"
                                    disabled={isSubmitting}
                                    className="w-full btn-primary flex items-center justify-center gap-2 py-3 text-lg disabled:opacity-50"
                                >
                                    {isSubmitting ? (
                                        <div className="w-6 h-6 border-2 border-white border-t-transparent rounded-full animate-spin" />
                                    ) : (
                                        'Send Reset Link'
                                    )}
                                </button>
                            </form>

                            <div className="mt-6 text-center">
                                <Link
                                    to="/login"
                                    className="inline-flex items-center gap-2 text-navy-500 hover:text-navy-700 text-sm font-medium"
                                >
                                    <ArrowLeft className="w-4 h-4" />
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

export default ForgotPassword;

