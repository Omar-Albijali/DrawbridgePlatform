import React, { useState } from 'react';
import { Lock, Shield, Eye, EyeOff, Smartphone } from 'lucide-react';

const Security: React.FC = () => {
    const [showPasswords, setShowPasswords] = useState({
        current: false,
        new: false,
        confirm: false,
    });

    const [passwords, setPasswords] = useState({
        current: '',
        new: '',
        confirm: '',
    });

    const [twoFactorEnabled, setTwoFactorEnabled] = useState(false);

    const handlePasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setPasswords(prev => ({ ...prev, [name]: value }));
    };

    const togglePasswordVisibility = (field: 'current' | 'new' | 'confirm') => {
        setShowPasswords(prev => ({ ...prev, [field]: !prev[field] }));
    };

    const handleUpdatePassword = () => {
        // Mock password update
        alert('Password updated successfully!');
        setPasswords({ current: '', new: '', confirm: '' });
    };

    const isPasswordValid = passwords.current && passwords.new && passwords.confirm && passwords.new === passwords.confirm;

    return (
        <div className="space-y-6">
            {/* Header */}
            <div>
                <h1 className="text-2xl font-bold text-navy-800">Login & Security</h1>
                <p className="text-navy-500 mt-1">Manage your password and account security</p>
            </div>

            {/* Change Password Card */}
            <div className="card">
                <div className="flex items-center gap-2 mb-6">
                    <Lock className="w-5 h-5 text-primary-600" />
                    <h3 className="text-lg font-semibold text-navy-800">Change Password</h3>
                </div>
                <div className="space-y-4 max-w-md">
                    {/* Current Password */}
                    <div>
                        <label className="label">Current Password</label>
                        <div className="relative">
                            <input
                                type={showPasswords.current ? 'text' : 'password'}
                                name="current"
                                value={passwords.current}
                                onChange={handlePasswordChange}
                                className="input pr-10"
                                placeholder="Enter current password"
                            />
                            <button
                                type="button"
                                onClick={() => togglePasswordVisibility('current')}
                                className="absolute right-3 top-1/2 -translate-y-1/2 text-navy-400 hover:text-navy-600"
                            >
                                {showPasswords.current ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                            </button>
                        </div>
                    </div>

                    {/* New Password */}
                    <div>
                        <label className="label">New Password</label>
                        <div className="relative">
                            <input
                                type={showPasswords.new ? 'text' : 'password'}
                                name="new"
                                value={passwords.new}
                                onChange={handlePasswordChange}
                                className="input pr-10"
                                placeholder="Enter new password"
                            />
                            <button
                                type="button"
                                onClick={() => togglePasswordVisibility('new')}
                                className="absolute right-3 top-1/2 -translate-y-1/2 text-navy-400 hover:text-navy-600"
                            >
                                {showPasswords.new ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                            </button>
                        </div>
                    </div>

                    {/* Confirm New Password */}
                    <div>
                        <label className="label">Confirm New Password</label>
                        <div className="relative">
                            <input
                                type={showPasswords.confirm ? 'text' : 'password'}
                                name="confirm"
                                value={passwords.confirm}
                                onChange={handlePasswordChange}
                                className="input pr-10"
                                placeholder="Confirm new password"
                            />
                            <button
                                type="button"
                                onClick={() => togglePasswordVisibility('confirm')}
                                className="absolute right-3 top-1/2 -translate-y-1/2 text-navy-400 hover:text-navy-600"
                            >
                                {showPasswords.confirm ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                            </button>
                        </div>
                        {passwords.new && passwords.confirm && passwords.new !== passwords.confirm && (
                            <p className="text-xs text-red-500 mt-1">Passwords do not match</p>
                        )}
                    </div>

                    <button
                        onClick={handleUpdatePassword}
                        disabled={!isPasswordValid}
                        className={`btn-primary px-6 py-2.5 mt-2 ${!isPasswordValid ? 'opacity-50 cursor-not-allowed' : ''}`}
                    >
                        Update Password
                    </button>
                </div>
            </div>

            {/* Two-Factor Authentication */}
            <div className="card">
                <div className="flex items-center gap-2 mb-6">
                    <Shield className="w-5 h-5 text-primary-600" />
                    <h3 className="text-lg font-semibold text-navy-800">Two-Factor Authentication</h3>
                </div>
                <div className="flex items-center justify-between p-4 bg-gray-50 rounded-xl">
                    <div className="flex items-center gap-4">
                        <div className="w-12 h-12 bg-primary-100 rounded-full flex items-center justify-center">
                            <Smartphone className="w-6 h-6 text-primary-600" />
                        </div>
                        <div>
                            <h4 className="font-medium text-navy-800">Authenticator App</h4>
                            <p className="text-sm text-navy-500">
                                Secure your account with an authenticator app
                            </p>
                        </div>
                    </div>
                    <div className="flex items-center gap-4">
                        {/* Toggle Switch */}
                        <button
                            onClick={() => setTwoFactorEnabled(!twoFactorEnabled)}
                            className={`relative w-12 h-6 rounded-full transition-colors duration-200 ${twoFactorEnabled ? 'bg-primary-600' : 'bg-gray-300'
                                }`}
                        >
                            <span
                                className={`absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full shadow transition-transform duration-200 ${twoFactorEnabled ? 'translate-x-6' : 'translate-x-0'
                                    }`}
                            />
                        </button>
                        {twoFactorEnabled && (
                            <button className="btn-secondary px-4 py-2 text-sm">
                                Setup
                            </button>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Security;
