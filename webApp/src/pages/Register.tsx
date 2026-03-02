import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import {
    User, Building2, Phone, MapPin, FileText, Mail, Lock,
    Eye, EyeOff, ArrowRight, ArrowLeft, Briefcase
} from 'lucide-react';

import { VerificationStatus, UserRole, AddressDto, RegisterRequest } from '../types';

interface FormData {
    role: UserRole;
    // Account / Company
    companyName: string;
    email: string; // Account/Login email
    phone: string; // Business phone
    password: string;
    confirmPassword: string;
    commercialRegister: string;

    // Representative
    repName: string;
    repJob: string;
    repPhone: string;
    repEmail: string;

    // Address
    street: string;
    city: string;
    state: string;
    zipCode: string; // zipCode
    country: string;

    verificationStatus: VerificationStatus;
}

const Register: React.FC = () => {
    const [formData, setFormData] = useState<FormData>({
        role: UserRole.RETAILER,
        companyName: '',
        email: '',
        phone: '',
        password: '',
        confirmPassword: '',
        commercialRegister: '',

        repName: '',
        repJob: '',
        repPhone: '',
        repEmail: '',

        street: '',
        city: '',
        state: '',
        zipCode: '',
        country: 'Saudi Arabia',

        verificationStatus: VerificationStatus.PENDING
    });

    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    const { register } = useAuth();
    const navigate = useNavigate();

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
        setFormData(prev => ({
            ...prev,
            [e.target.name]: e.target.value
        }));
    };

    const handleRoleChange = (role: UserRole) => {
        setFormData(prev => ({ ...prev, role }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        if (formData.password !== formData.confirmPassword) {
            setError('Passwords do not match');
            return;
        }

        if (formData.password.length < 6) {
            setError('Password must be at least 6 characters');
            return;
        }

        setIsSubmitting(true);

        const success = await register({
            email: formData.email,
            password: formData.password,
            phoneNumber: formData.phone,
            role: formData.role,
            businessName: formData.companyName || null,
            commercialRegistrationNumber: formData.commercialRegister,
            repName: formData.repName,
            repJobTitle: formData.repJob,
            repPhoneNumber: formData.repPhone,
            repEmail: formData.repEmail,
            addresses: [{
                id: null,
                street: formData.street,
                city: formData.city,
                state: formData.state,
                zipCode: formData.zipCode,
                country: formData.country
            } as unknown as AddressDto]
        } as RegisterRequest);

        if (success) {
            const emailParam = encodeURIComponent(formData.email);
            navigate(`/verify-email?sent=1&email=${emailParam}`);
        } else {
            setError('Registration failed. Please try again.');
        }

        setIsSubmitting(false);
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-navy-900 via-navy-800 to-primary-900 flex items-center justify-center p-4 py-8">
            <div className="absolute inset-0 bg-[url('data:image/svg+xml,%3Csvg%20width%3D%2260%22%20height%3D%2260%22%20viewBox%3D%220%200%2060%2060%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Cg%20fill%3D%22none%22%20fill-rule%3D%22evenodd%22%3E%3Cg%20fill%3D%22%23ffffff%22%20fill-opacity%3D%220.03%22%3E%3Cpath%20d%3D%22M36%2034v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6%2034v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6%204V0H4v4H0v2h4v4h2V6h4V4H6z%22%2F%3E%3C%2Fg%3E%3C%2Fg%3E%3C%2Fsvg%3E')] opacity-50"></div>

            <div className="relative w-full max-w-4xl">
                <div className="text-center mb-8">
                    <div className="inline-flex items-center justify-center w-16 h-16 bg-white rounded-2xl shadow-lg mb-4">
                        <svg className="w-10 h-10 text-primary-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M3 21h18M5 21V7l8-4 8 4v14M9 21v-8h6v8" />
                        </svg>
                    </div>
                    <h1 className="text-3xl font-bold text-white mb-2">Create Account</h1>
                    <p className="text-navy-300">Join Drawbridge B2B Commerce Platform</p>
                </div>

                <div className="bg-white rounded-2xl shadow-2xl p-8">
                    <div className="mb-8">
                        <label className="block text-sm font-medium text-navy-700 mb-3">I am a:</label>
                        <div className="grid grid-cols-2 gap-4">
                            <button
                                type="button"
                                onClick={() => handleRoleChange(UserRole.RETAILER)}
                                className={`p-4 rounded-xl border-2 transition-all duration-200 ${formData.role === UserRole.RETAILER
                                    ? 'border-primary-500 bg-primary-50'
                                    : 'border-gray-200 hover:border-gray-300'
                                    }`}
                            >
                                <div className={`w-12 h-12 mx-auto rounded-full flex items-center justify-center mb-3 ${formData.role === UserRole.RETAILER ? 'bg-primary-500 text-white' : 'bg-gray-100 text-gray-500'
                                    }`}>
                                    <User className="w-6 h-6" />
                                </div>
                                <h3 className={`font-semibold ${formData.role === UserRole.RETAILER ? 'text-primary-700' : 'text-navy-700'}`}>Retailer</h3>
                                <p className="text-sm text-navy-500 mt-1">Buy products</p>
                            </button>

                            <button
                                type="button"
                                onClick={() => handleRoleChange(UserRole.WHOLESALER)}
                                className={`p-4 rounded-xl border-2 transition-all duration-200 ${formData.role === UserRole.WHOLESALER
                                    ? 'border-primary-500 bg-primary-50'
                                    : 'border-gray-200 hover:border-gray-300'
                                    }`}
                            >
                                <div className={`w-12 h-12 mx-auto rounded-full flex items-center justify-center mb-3 ${formData.role === UserRole.WHOLESALER ? 'bg-primary-500 text-white' : 'bg-gray-100 text-gray-500'
                                    }`}>
                                    <Building2 className="w-6 h-6" />
                                </div>
                                <h3 className={`font-semibold ${formData.role === UserRole.WHOLESALER ? 'text-primary-700' : 'text-navy-700'}`}>Wholesaler</h3>
                                <p className="text-sm text-navy-500 mt-1">Sell products</p>
                            </button>
                        </div>
                    </div>

                    {error && (
                        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">{error}</div>
                    )}

                    <form onSubmit={handleSubmit} className="space-y-8">
                        {/* Company / Account Information */}
                        <div>
                            <h3 className="text-lg font-semibold text-navy-800 mb-4 border-b pb-2">Business Information</h3>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                                <div>
                                    <label className="label">Company Name</label>
                                    <div className="relative">
                                        <Building2 className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-navy-400" />
                                        <input name="companyName" type="text" value={formData.companyName} onChange={handleChange} className="input pl-10" required placeholder="Business Name" />
                                    </div>
                                </div>
                                <div>
                                    <label className="label">Business Phone</label>
                                    <div className="relative">
                                        <Phone className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-navy-400" />
                                        <input name="phone" type="tel" value={formData.phone} onChange={handleChange} className="input pl-10" required placeholder="Business Contact Number" />
                                    </div>
                                </div>
                                <div>
                                    <label className="label">Login Email</label>
                                    <div className="relative">
                                        <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-navy-400" />
                                        <input name="email" type="email" value={formData.email} onChange={handleChange} className="input pl-10" required placeholder="Account Email" />
                                    </div>
                                </div>
                                <div>
                                    <label className="label">Commercial Register</label>
                                    <div className="relative">
                                        <FileText className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-navy-400" />
                                        <input name="commercialRegister" type="text" value={formData.commercialRegister} onChange={handleChange} className="input pl-10" placeholder="CR Number" />
                                    </div>
                                </div>
                                <div>
                                    <label className="label">Password</label>
                                    <div className="relative">
                                        <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-navy-400" />
                                        <input name="password" type={showPassword ? 'text' : 'password'} value={formData.password} onChange={handleChange} className="input pl-10 pr-10" required placeholder="Password" />
                                        <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-navy-400 hover:text-navy-600">
                                            {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                                        </button>
                                    </div>
                                </div>
                                <div>
                                    <label className="label">Confirm Password</label>
                                    <div className="relative">
                                        <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-navy-400" />
                                        <input name="confirmPassword" type={showPassword ? 'text' : 'password'} value={formData.confirmPassword} onChange={handleChange} className="input pl-10" required placeholder="Confirm Password" />
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Representative Information */}
                        <div>
                            <h3 className="text-lg font-semibold text-navy-800 mb-4 border-b pb-2">Representative Information</h3>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                                <div>
                                    <label className="label">Representative Name</label>
                                    <div className="relative">
                                        <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-navy-400" />
                                        <input name="repName" type="text" value={formData.repName} onChange={handleChange} className="input pl-10" required placeholder="Full Name" />
                                    </div>
                                </div>
                                <div>
                                    <label className="label">Job Title</label>
                                    <div className="relative">
                                        <Briefcase className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-navy-400" />
                                        <input name="repJob" type="text" value={formData.repJob} onChange={handleChange} className="input pl-10" required placeholder="Job Title" />
                                    </div>
                                </div>
                                <div>
                                    <label className="label">Personal Phone</label>
                                    <div className="relative">
                                        <Phone className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-navy-400" />
                                        <input name="repPhone" type="tel" value={formData.repPhone} onChange={handleChange} className="input pl-10" required placeholder="Mobile Number" />
                                    </div>
                                </div>
                                <div>
                                    <label className="label">Personal Email</label>
                                    <div className="relative">
                                        <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-navy-400" />
                                        <input name="repEmail" type="email" value={formData.repEmail} onChange={handleChange} className="input pl-10" required placeholder="Work Email" />
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Address Information */}
                        <div>
                            <h3 className="text-lg font-semibold text-navy-800 mb-4 border-b pb-2">Address</h3>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                                <div className="md:col-span-2">
                                    <label className="label">Street Address</label>
                                    <div className="relative">
                                        <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-navy-400" />
                                        <input name="street" type="text" value={formData.street} onChange={handleChange} className="input pl-10" required placeholder="Street" />
                                    </div>
                                </div>
                                <div>
                                    <label className="label">City</label>
                                    <input name="city" type="text" value={formData.city} onChange={handleChange} className="input" required placeholder="City" />
                                </div>
                                <div>
                                    <label className="label">State / Province</label>
                                    <input name="state" type="text" value={formData.state} onChange={handleChange} className="input" required placeholder="State" />
                                </div>
                                <div>
                                    <label className="label">Zip / Postal Code</label>
                                    <input name="zipCode" type="text" value={formData.zipCode} onChange={handleChange} className="input" required placeholder="Zip Code" />
                                </div>
                                <div>
                                    <label className="label">Country</label>
                                    <input name="country" type="text" value={formData.country} onChange={handleChange} className="input" required placeholder="Country" />
                                </div>
                            </div>
                        </div>

                        <label className="flex items-start gap-3">
                            <input type="checkbox" className="w-4 h-4 mt-1 rounded border-gray-300 text-primary-600 focus:ring-primary-500" required />
                            <span className="text-sm text-navy-600">I agree to the <a href="#" className="text-primary-600 hover:text-primary-700 font-medium">Terms of Service</a> and <a href="#" className="text-primary-600 hover:text-primary-700 font-medium">Privacy Policy</a></span>
                        </label>

                        <button type="submit" disabled={isSubmitting} className="w-full btn-primary flex items-center justify-center gap-2 py-3 text-lg disabled:opacity-50">
                            {isSubmitting ? (
                                <div className="w-6 h-6 border-2 border-white border-t-transparent rounded-full animate-spin" />
                            ) : (
                                <>
                                    Create Account
                                    <ArrowRight className="w-5 h-5" />
                                </>
                            )}
                        </button>
                    </form>

                    <div className="mt-6 text-center">
                        <p className="text-navy-500">
                            Already have an account?{' '}
                            <Link to="/login" className="text-primary-600 hover:text-primary-700 font-semibold inline-flex items-center gap-1">
                                <ArrowLeft className="w-4 h-4" /> Sign In
                            </Link>
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Register;
