import React, { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { CheckCircle, AlertTriangle, MailCheck } from 'lucide-react';

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
                const res = await fetch('/api/auth/verify-email', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ token })
                });

                if (!res.ok) {
                    setStatus('error');
                    return;
                }

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
        <div className="min-h-screen bg-gradient-to-br from-navy-900 via-navy-800 to-primary-900 flex items-center justify-center p-4">
            <div className="absolute inset-0 bg-[url('data:image/svg+xml,%3Csvg%20width%3D%2260%22%20height%3D%2260%22%20viewBox%3D%220%200%2060%2060%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Cg%20fill%3D%22none%22%20fill-rule%3D%22evenodd%22%3E%3Cg%20fill%3D%22%23ffffff%22%20fill-opacity%3D%220.03%22%3E%3Cpath%20d%3D%22M36%2034v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6%2034v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6%204V0H4v4H0v2h4v4h2V6h4V4H6z%22%2F%3E%3C%2Fg%3E%3C%2Fg%3E%3C%2Fsvg%3E')] opacity-50" />

            <div className="relative w-full max-w-md">
                <div className="text-center mb-8">
                    <div className="inline-flex items-center justify-center w-16 h-16 bg-white rounded-2xl shadow-lg mb-4">
                        <svg className="w-10 h-10 text-primary-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M3 21h18M5 21V7l8-4 8 4v14M9 21v-8h6v8" />
                        </svg>
                    </div>
                    <h1 className="text-3xl font-bold text-white mb-2">Drawbridge</h1>
                    <p className="text-navy-300">B2B Commerce Platform</p>
                </div>

                <div className="bg-white rounded-2xl shadow-2xl p-8 text-center">
                    {showSentMessage ? (
                        <>
                            <div className="flex justify-center mb-4">
                                <MailCheck className="w-16 h-16 text-primary-600" />
                            </div>
                            <h2 className="text-2xl font-bold text-navy-900">Check your email</h2>
                            <p className="text-navy-500 mt-2">
                                We sent a verification link to <strong>{sentEmail ?? 'your email'}</strong>.
                                Open the link to verify your account.
                            </p>
                        </>
                    ) : token ? (
                        status === 'success' ? (
                            <>
                                <div className="flex justify-center mb-4">
                                    <CheckCircle className="w-16 h-16 text-green-500" />
                                </div>
                                <h2 className="text-2xl font-bold text-navy-900">Email verified!</h2>
                                <p className="text-navy-500 mt-2">
                                    Your account is ready to use.
                                </p>
                            </>
                        ) : status === 'error' ? (
                            <>
                                <div className="flex justify-center mb-4">
                                    <AlertTriangle className="w-16 h-16 text-red-500" />
                                </div>
                                <h2 className="text-2xl font-bold text-navy-900">Verification failed</h2>
                                <p className="text-navy-500 mt-2">
                                    This verification link is invalid or has expired.
                                </p>
                            </>
                        ) : (
                            <>
                                <div className="flex justify-center mb-4">
                                    <div className="w-16 h-16 border-4 border-primary-600 border-t-transparent rounded-full animate-spin" />
                                </div>
                                <h2 className="text-2xl font-bold text-navy-900">Verifying...</h2>
                                <p className="text-navy-500 mt-2">Please wait while we confirm your email.</p>
                            </>
                        )
                    ) : (
                        <>
                            <div className="flex justify-center mb-4">
                                <AlertTriangle className="w-16 h-16 text-amber-500" />
                            </div>
                            <h2 className="text-2xl font-bold text-navy-900">Missing token</h2>
                            <p className="text-navy-500 mt-2">No verification token was provided.</p>
                        </>
                    )}

                    <div className="mt-6 flex flex-col gap-3">
                        <Link to="/login" className="btn-primary px-6 py-2">
                            Go to Sign In
                        </Link>
                        {isSubmitting && (
                            <p className="text-xs text-navy-400">Finishing verification...</p>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default VerifyEmail;

