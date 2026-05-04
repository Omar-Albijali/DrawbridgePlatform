import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  User, Building2, Phone, MapPin, FileText, Mail, Lock,
  Eye, EyeOff, ArrowRight, ArrowLeft, Briefcase
} from 'lucide-react';

import { useAuth } from '../contexts/AuthContext';
import { VerificationStatus, UserRole, AddressDto, RegisterRequest } from '../types';

interface FormData {
  role: UserRole;
  companyName: string;
  email: string;
  phone: string;
  password: string;
  confirmPassword: string;
  commercialRegister: string;
  repName: string;
  repJob: string;
  repPhone: string;
  repEmail: string;
  street: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
  verificationStatus: VerificationStatus;
}

const Register: React.FC = () => {
  const { t } = useTranslation();
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
    country: t('auth.register.defaultCountry'),
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
      setError(t('auth.register.errors.passwordsMismatch'));
      return;
    }

    if (formData.password.length < 6) {
      setError(t('auth.register.errors.passwordTooShort'));
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
      setError(t('auth.register.errors.registrationFailed'));
    }

    setIsSubmitting(false);
  };

  const roleButtonClass = (active: boolean): string =>
    `rounded-2xl border p-4 text-left transition ${
      active
        ? 'border-primary-400 bg-primary-50 dark:border-primary-400/50 dark:bg-primary-500/10'
        : 'border-slate-200 bg-white/70 hover:border-primary-300 dark:border-white/10 dark:bg-slate-900/70'
    }`;

  return (
    <div className="relative grid min-h-screen place-items-center overflow-hidden px-4 py-10">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_8%_20%,rgba(16,185,129,0.2),transparent_40%),radial-gradient(circle_at_86%_14%,rgba(14,165,233,0.18),transparent_44%)]" />

      <div className="relative z-10 w-full max-w-5xl space-y-5">
        <div className="text-center">
          <div className="mx-auto mb-3 grid h-14 w-14 place-items-center rounded-2xl border border-slate-200 bg-white/70 text-primary-600 shadow-lg shadow-primary-500/20 dark:border-white/10 dark:bg-slate-900/70 dark:text-primary-300">
            <svg className="h-7 w-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M3 21h18M5 21V7l8-4 8 4v14M9 21v-8h6v8" />
            </svg>
          </div>
          <h1 className="text-3xl font-black tracking-tight text-slate-900 dark:text-white">{t('auth.register.title')}</h1>
          <p className="mt-1 text-slate-500 dark:text-slate-300">{t('auth.register.subtitle')}</p>
        </div>

        <div className="glass-panel rounded-3xl p-6 sm:p-8">
          <div className="mb-8">
            <label className="label mb-3">{t('auth.register.rolePrompt')}</label>
            <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
              <button
                type="button"
                onClick={() => handleRoleChange(UserRole.RETAILER)}
                className={roleButtonClass(formData.role === UserRole.RETAILER)}
              >
                <div className="mb-3 inline-flex h-11 w-11 items-center justify-center rounded-xl bg-primary-500/10 text-primary-600 dark:text-primary-300">
                  <User className="h-6 w-6" />
                </div>
                <h3 className="font-semibold text-slate-900 dark:text-slate-100">{t('auth.register.retailer')}</h3>
                <p className="mt-1 text-sm text-slate-500 dark:text-slate-300">{t('auth.register.retailerHint')}</p>
              </button>

              <button
                type="button"
                onClick={() => handleRoleChange(UserRole.WHOLESALER)}
                className={roleButtonClass(formData.role === UserRole.WHOLESALER)}
              >
                <div className="mb-3 inline-flex h-11 w-11 items-center justify-center rounded-xl bg-primary-500/10 text-primary-600 dark:text-primary-300">
                  <Building2 className="h-6 w-6" />
                </div>
                <h3 className="font-semibold text-slate-900 dark:text-slate-100">{t('auth.register.wholesaler')}</h3>
                <p className="mt-1 text-sm text-slate-500 dark:text-slate-300">{t('auth.register.wholesalerHint')}</p>
              </button>
            </div>
          </div>

          {error && (
            <div className="mb-6 rounded-xl border border-red-300/60 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-400/30 dark:bg-red-500/10 dark:text-red-300">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-8">
            <div>
              <h3 className="mb-4 border-b border-slate-200 pb-2 text-base font-semibold text-slate-900 dark:border-white/10 dark:text-slate-100">
                {t('auth.register.businessInfo')}
              </h3>
              <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
                <div>
                  <label className="label">{t('auth.register.companyName')}</label>
                  <div className="relative">
                    <Building2 className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                    <input name="companyName" type="text" value={formData.companyName} onChange={handleChange} className="input pl-10" required placeholder={t('auth.register.businessNamePlaceholder')} />
                  </div>
                </div>
                <div>
                  <label className="label">{t('auth.register.businessPhone')}</label>
                  <div className="relative">
                    <Phone className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                    <input name="phone" type="tel" value={formData.phone} onChange={handleChange} className="input pl-10" required placeholder={t('auth.register.businessContactPlaceholder')} />
                  </div>
                </div>
                <div>
                  <label className="label">{t('auth.register.loginEmail')}</label>
                  <div className="relative">
                    <Mail className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                    <input name="email" type="email" value={formData.email} onChange={handleChange} className="input pl-10" required placeholder={t('auth.register.accountEmailPlaceholder')} />
                  </div>
                </div>
                <div>
                  <label className="label">{t('auth.register.commercialRegister')}</label>
                  <div className="relative">
                    <FileText className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                    <input name="commercialRegister" type="text" value={formData.commercialRegister} onChange={handleChange} className="input pl-10" placeholder={t('auth.register.crPlaceholder')} />
                  </div>
                </div>
                <div>
                  <label className="label">{t('auth.password')}</label>
                  <div className="relative">
                    <Lock className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                    <input name="password" type={showPassword ? 'text' : 'password'} value={formData.password} onChange={handleChange} className="input pl-10 pr-10" required placeholder={t('auth.password')} />
                    <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200">
                      {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                    </button>
                  </div>
                </div>
                <div>
                  <label className="label">{t('auth.confirmPassword')}</label>
                  <div className="relative">
                    <Lock className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                    <input name="confirmPassword" type={showPassword ? 'text' : 'password'} value={formData.confirmPassword} onChange={handleChange} className="input pl-10" required placeholder={t('auth.confirmPassword')} />
                  </div>
                </div>
              </div>
            </div>

            <div>
              <h3 className="mb-4 border-b border-slate-200 pb-2 text-base font-semibold text-slate-900 dark:border-white/10 dark:text-slate-100">
                {t('auth.register.representativeInfo')}
              </h3>
              <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
                <div>
                  <label className="label">{t('auth.register.representativeName')}</label>
                  <div className="relative">
                    <User className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                    <input name="repName" type="text" value={formData.repName} onChange={handleChange} className="input pl-10" required placeholder={t('auth.register.fullName')} />
                  </div>
                </div>
                <div>
                  <label className="label">{t('auth.register.jobTitle')}</label>
                  <div className="relative">
                    <Briefcase className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                    <input name="repJob" type="text" value={formData.repJob} onChange={handleChange} className="input pl-10" required placeholder={t('auth.register.jobTitle')} />
                  </div>
                </div>
                <div>
                  <label className="label">{t('auth.register.personalPhone')}</label>
                  <div className="relative">
                    <Phone className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                    <input name="repPhone" type="tel" value={formData.repPhone} onChange={handleChange} className="input pl-10" required placeholder={t('auth.register.mobileNumber')} />
                  </div>
                </div>
                <div>
                  <label className="label">{t('auth.register.personalEmail')}</label>
                  <div className="relative">
                    <Mail className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                    <input name="repEmail" type="email" value={formData.repEmail} onChange={handleChange} className="input pl-10" required placeholder={t('auth.register.workEmail')} />
                  </div>
                </div>
              </div>
            </div>

            <div>
              <h3 className="mb-4 border-b border-slate-200 pb-2 text-base font-semibold text-slate-900 dark:border-white/10 dark:text-slate-100">
                {t('auth.register.address')}
              </h3>
              <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
                <div className="md:col-span-2">
                  <label className="label">{t('auth.register.streetAddress')}</label>
                  <div className="relative">
                    <MapPin className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                    <input name="street" type="text" value={formData.street} onChange={handleChange} className="input pl-10" required placeholder={t('auth.register.street')} />
                  </div>
                </div>
                <div>
                  <label className="label">{t('auth.register.city')}</label>
                  <input name="city" type="text" value={formData.city} onChange={handleChange} className="input" required placeholder={t('auth.register.city')} />
                </div>
                <div>
                  <label className="label">{t('auth.register.state')}</label>
                  <input name="state" type="text" value={formData.state} onChange={handleChange} className="input" required placeholder={t('auth.register.statePlaceholder')} />
                </div>
                <div>
                  <label className="label">{t('auth.register.zipCode')}</label>
                  <input name="zipCode" type="text" value={formData.zipCode} onChange={handleChange} className="input" required placeholder={t('auth.register.zipPlaceholder')} />
                </div>
                <div>
                  <label className="label">{t('auth.register.country')}</label>
                  <input name="country" type="text" value={formData.country} onChange={handleChange} className="input" required placeholder={t('auth.register.country')} />
                </div>
              </div>
            </div>

            <label className="flex items-start gap-3 text-sm text-slate-600 dark:text-slate-300">
              <input type="checkbox" className="mt-1 h-4 w-4 rounded border-slate-300 text-primary-600" required />
              <span>
                {t('auth.register.agreePrefix')}{' '}
                <a href="#" className="font-semibold text-primary-700 hover:text-primary-600 dark:text-primary-300">{t('auth.register.terms')}</a>{' '}
                {t('auth.register.and')}{' '}
                <a href="#" className="font-semibold text-primary-700 hover:text-primary-600 dark:text-primary-300">{t('auth.register.privacy')}</a>
              </span>
            </label>

            <button type="submit" disabled={isSubmitting} className="btn-primary w-full gap-2 rounded-xl py-3 disabled:opacity-50">
              {isSubmitting ? (
                <div className="h-6 w-6 animate-spin rounded-full border-2 border-slate-900 border-t-transparent" />
              ) : (
                <>
                  {t('auth.register.createAccount')}
                  <ArrowRight className="h-5 w-5" />
                </>
              )}
            </button>
          </form>

          <div className="mt-6 text-center text-sm text-slate-500 dark:text-slate-300">
            <p>
              {t('auth.register.alreadyHaveAccount')}{' '}
              <Link to="/login" className="inline-flex items-center gap-1 font-semibold text-primary-700 hover:text-primary-600 dark:text-primary-300">
                <ArrowLeft className="h-4 w-4" /> {t('auth.signIn')}
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Register;
