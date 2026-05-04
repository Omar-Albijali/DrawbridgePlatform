import { useEffect, useRef, useState, type ChangeEvent } from 'react';
import { BadgeCheck, Building2, Camera, Check, FileText } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../contexts/AuthContext';
import { userService } from '../../services/userService';
import { UpdateUserProfileRequest, UserRole } from '../../types';

export default function Profile(): JSX.Element {
  const { t } = useTranslation();
  const { user, refreshUser } = useAuth();
  const isRetailer = user?.role === UserRole.RETAILER;

  const avatarInputRef = useRef<HTMLInputElement | null>(null);
  const [isUploading, setIsUploading] = useState(false);

  const [formData, setFormData] = useState({
    repName: user?.representative?.name || '',
    repJobTitle: user?.representative?.jobTitle || '',
    repEmail: user?.representative?.email || '',
    repPhone: user?.representative?.phoneNumber || '',
    businessName: user?.company || '',
    businessPhone: user?.phone || '',
    crNumber: user?.commercialRegister || '',
  });

  const [initialData, setInitialData] = useState(formData);
  const [hasChanges, setHasChanges] = useState(false);

  useEffect(() => {
    setHasChanges(JSON.stringify(formData) !== JSON.stringify(initialData));
  }, [formData, initialData]);

  const handleInputChange = (event: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSave = async () => {
    if (!user?.id) return;

    try {
      const request = {
        company: formData.businessName,
        phone: formData.businessPhone,
        commercialRegister: formData.crNumber,
        representative: {
          name: formData.repName,
          jobTitle: formData.repJobTitle,
          phoneNumber: formData.repPhone,
          email: formData.repEmail,
        },
      } as unknown as UpdateUserProfileRequest;

      await userService.update(user.id, request);
      await refreshUser();

      setInitialData(formData);
      setHasChanges(false);
      alert(t('settings.profile.updated'));
    } catch (error) {
      console.error('Failed to update profile', error);
      alert(t('settings.profile.updateFailed'));
    }
  };

  const triggerAvatarUpload = () => {
    if (!isUploading) {
      avatarInputRef.current?.click();
    }
  };

  const handleProfileImageChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file || !user?.id) return;

    try {
      setIsUploading(true);
      await userService.uploadProfileImage(user.id, file);
      await refreshUser();
    } catch (error) {
      console.error('Failed to upload profile image', error);
      alert(t('settings.profile.uploadFailed'));
    } finally {
      setIsUploading(false);
      event.target.value = '';
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-navy-800">{t('settings.profile.title')}</h1>
          <p className="text-navy-500 mt-1">{t('settings.profile.description')}</p>
        </div>
        <button
          onClick={handleSave}
          disabled={!hasChanges}
          className={`btn-primary px-6 py-2.5 flex items-center gap-2 ${
            !hasChanges ? 'opacity-50 cursor-not-allowed' : ''
          }`}
        >
          <Check className="w-4 h-4" />
          {t('settings.profile.saveChanges')}
        </button>
      </div>

      <div className="card">
        <div className="flex items-center gap-6">
          <div className="relative">
            <div className="w-24 h-24 rounded-full bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center overflow-hidden">
              {user?.avatar ? (
                <img src={user.avatar} alt={user.name} className="w-full h-full object-cover" />
              ) : (
                <span className="text-3xl font-bold text-white">{user?.name?.charAt(0) || 'U'}</span>
              )}
            </div>
            <button
              type="button"
              onClick={triggerAvatarUpload}
              disabled={isUploading}
              className="absolute -bottom-1 -right-1 w-8 h-8 bg-primary-600 rounded-full flex items-center justify-center text-white shadow-lg hover:bg-primary-700 transition-colors disabled:opacity-60"
              title={t('settings.profile.uploadAvatar')}
            >
              <Camera className="w-4 h-4" />
            </button>
            <input
              ref={avatarInputRef}
              type="file"
              accept="image/*"
              className="hidden"
              onChange={handleProfileImageChange}
            />
          </div>
          <div>
            <h3 className="font-semibold text-navy-800">{formData.businessName || t('settings.profile.organizationName')}</h3>
            <p className="text-sm text-navy-500 capitalize">
              {t('settings.profile.accountType', { role: isRetailer ? t('auth.register.retailer') : t('auth.register.wholesaler') })}
            </p>
            <p className="text-sm text-navy-400 mt-1">{t('settings.profile.representative', { name: formData.repName })}</p>
          </div>
        </div>
      </div>

      <div className="card">
        <h3 className="text-lg font-semibold text-navy-800 mb-6">{t('settings.profile.representativeInfo')}</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="label">{t('settings.profile.representativeName')}</label>
            <input
              type="text"
              name="repName"
              value={formData.repName}
              onChange={handleInputChange}
              className="input"
              placeholder={t('settings.profile.placeholders.fullName')}
            />
          </div>
          <div>
            <label className="label">{t('settings.profile.jobTitle')}</label>
            <input
              type="text"
              name="repJobTitle"
              value={formData.repJobTitle}
              onChange={handleInputChange}
              className="input"
              placeholder={t('settings.profile.placeholders.manager')}
            />
          </div>
          <div>
            <label className="label">{t('settings.profile.workEmail')}</label>
            <input
              type="email"
              name="repEmail"
              value={formData.repEmail}
              onChange={handleInputChange}
              className="input"
              placeholder={t('settings.profile.placeholders.email')}
            />
          </div>
          <div>
            <label className="label">{t('settings.profile.mobileNumber')}</label>
            <input
              type="tel"
              name="repPhone"
              value={formData.repPhone}
              onChange={handleInputChange}
              className="input"
              placeholder={t('settings.profile.placeholders.phone')}
            />
          </div>
        </div>
      </div>

      <div className="card">
        <div className="flex items-center gap-2 mb-6">
          <Building2 className="w-5 h-5 text-primary-600" />
          <h3 className="text-lg font-semibold text-navy-800">{t('settings.profile.organizationDetails')}</h3>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="md:col-span-2">
            <label className="label">{t('settings.profile.businessName')}</label>
            <input
              type="text"
              name="businessName"
              value={formData.businessName}
              onChange={handleInputChange}
              className="input"
              placeholder={t('settings.profile.placeholders.organizationName')}
            />
          </div>
          <div>
            <label className="label">{t('settings.profile.accountEmail')}</label>
            <input
              type="email"
              value={user?.email || ''}
              className="input bg-gray-100 text-navy-500 cursor-not-allowed"
              disabled
              readOnly
            />
            <p className="text-xs text-navy-400 mt-1">{t('settings.profile.emailCannotChange')}</p>
          </div>
          <div>
            <label className="label">{t('settings.profile.businessPhone')}</label>
            <input
              type="tel"
              name="businessPhone"
              value={formData.businessPhone}
              onChange={handleInputChange}
              className="input"
              placeholder={t('settings.profile.placeholders.contactNumber')}
            />
          </div>
          <div>
            <label className="label flex items-center gap-1">
              <FileText className="w-4 h-4" />
              {t('settings.profile.crNumber')}
            </label>
            <input
              type="text"
              name="crNumber"
              value={formData.crNumber}
              onChange={handleInputChange}
              className="input"
              placeholder={t('settings.profile.placeholders.commercialRegistration')}
            />
          </div>
        </div>
      </div>

      {!isRetailer && (
        <div className="card">
          <div className="flex items-center gap-2 mb-4">
            <BadgeCheck className="w-5 h-5 text-primary-600" />
            <h3 className="text-lg font-semibold text-navy-800">{t('settings.profile.companyVerification')}</h3>
          </div>
          <div className="flex items-center gap-4 p-4 bg-green-50 rounded-xl border border-green-200">
            <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center">
              <BadgeCheck className="w-6 h-6 text-green-600" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-semibold text-green-700">{t('settings.profile.verifiedSupplier')}</span>
                <span className="badge badge-success">{t('common.active')}</span>
              </div>
              <p className="text-sm text-green-600 mt-1">
                {t('settings.profile.verifiedDescription')}
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
