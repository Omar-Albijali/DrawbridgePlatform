import { useEffect, useRef, useState, type ChangeEvent } from 'react';
import { BadgeCheck, Building2, Camera, Check, FileText } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { userService } from '../../services/userService';
import { UpdateUserProfileRequest, UserRole } from '../../types';

export default function Profile(): JSX.Element {
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
      alert('Profile updated successfully');
    } catch (error) {
      console.error('Failed to update profile', error);
      alert('Failed to update profile');
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
      alert('Failed to upload image');
    } finally {
      setIsUploading(false);
      event.target.value = '';
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-navy-800">Account Settings</h1>
          <p className="text-navy-500 mt-1">Manage organization profile and representative details</p>
        </div>
        <button
          onClick={handleSave}
          disabled={!hasChanges}
          className={`btn-primary px-6 py-2.5 flex items-center gap-2 ${
            !hasChanges ? 'opacity-50 cursor-not-allowed' : ''
          }`}
        >
          <Check className="w-4 h-4" />
          Save Changes
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
              title="Upload avatar"
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
            <h3 className="font-semibold text-navy-800">{formData.businessName || 'Organization Name'}</h3>
            <p className="text-sm text-navy-500 capitalize">{String(user?.role)} Account</p>
            <p className="text-sm text-navy-400 mt-1">Rep: {formData.repName}</p>
          </div>
        </div>
      </div>

      <div className="card">
        <h3 className="text-lg font-semibold text-navy-800 mb-6">Representative Information</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="label">Representative Name</label>
            <input
              type="text"
              name="repName"
              value={formData.repName}
              onChange={handleInputChange}
              className="input"
              placeholder="Full Name"
            />
          </div>
          <div>
            <label className="label">Job Title</label>
            <input
              type="text"
              name="repJobTitle"
              value={formData.repJobTitle}
              onChange={handleInputChange}
              className="input"
              placeholder="e.g. Manager"
            />
          </div>
          <div>
            <label className="label">Work Email (Personal)</label>
            <input
              type="email"
              name="repEmail"
              value={formData.repEmail}
              onChange={handleInputChange}
              className="input"
              placeholder="name@company.com"
            />
          </div>
          <div>
            <label className="label">Mobile Number</label>
            <input
              type="tel"
              name="repPhone"
              value={formData.repPhone}
              onChange={handleInputChange}
              className="input"
              placeholder="+966 5XX XXX XXXX"
            />
          </div>
        </div>
      </div>

      <div className="card">
        <div className="flex items-center gap-2 mb-6">
          <Building2 className="w-5 h-5 text-primary-600" />
          <h3 className="text-lg font-semibold text-navy-800">Organization Details</h3>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="md:col-span-2">
            <label className="label">Business / Company Name</label>
            <input
              type="text"
              name="businessName"
              value={formData.businessName}
              onChange={handleInputChange}
              className="input"
              placeholder="Organization Name"
            />
          </div>
          <div>
            <label className="label">Account Email (Login)</label>
            <input
              type="email"
              value={user?.email || ''}
              className="input bg-gray-100 text-navy-500 cursor-not-allowed"
              disabled
              readOnly
            />
            <p className="text-xs text-navy-400 mt-1">Login email cannot be changed</p>
          </div>
          <div>
            <label className="label">Business Phone</label>
            <input
              type="tel"
              name="businessPhone"
              value={formData.businessPhone}
              onChange={handleInputChange}
              className="input"
              placeholder="General contact number"
            />
          </div>
          <div>
            <label className="label flex items-center gap-1">
              <FileText className="w-4 h-4" />
              CR Number
            </label>
            <input
              type="text"
              name="crNumber"
              value={formData.crNumber}
              onChange={handleInputChange}
              className="input"
              placeholder="Commercial Registration Number"
            />
          </div>
        </div>
      </div>

      {!isRetailer && (
        <div className="card">
          <div className="flex items-center gap-2 mb-4">
            <BadgeCheck className="w-5 h-5 text-primary-600" />
            <h3 className="text-lg font-semibold text-navy-800">Company Verification</h3>
          </div>
          <div className="flex items-center gap-4 p-4 bg-green-50 rounded-xl border border-green-200">
            <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center">
              <BadgeCheck className="w-6 h-6 text-green-600" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-semibold text-green-700">Verified Supplier</span>
                <span className="badge badge-success">Active</span>
              </div>
              <p className="text-sm text-green-600 mt-1">
                Your company has been verified and approved to sell on Drawbridge.
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
