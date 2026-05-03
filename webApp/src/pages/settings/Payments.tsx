import { useEffect, useState, type ChangeEvent } from 'react';
import { CreditCard, Plus, Star, Trash2, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../contexts/AuthContext';
import { paymentService } from '../../services/paymentService';
import { CreatePaymentMethodRequest, PaymentMethodDTO } from '../../types';

export default function Payments(): JSX.Element {
  const { t } = useTranslation();
  const { user } = useAuth();
  const [methods, setMethods] = useState<PaymentMethodDTO[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newCard, setNewCard] = useState({
    name: '',
    number: '',
    expiry: '',
    cvv: '',
  });

  useEffect(() => {
    if (user?.id) {
      void loadPaymentMethods();
    }
  }, [user?.id]);

  const loadPaymentMethods = async () => {
    if (!user?.id) return;

    try {
      const data = await paymentService.getPaymentMethods(user.id);
      setMethods(data);
    } catch (error) {
      console.error('Failed to load payment methods', error);
    } finally {
      setIsLoading(false);
    }
  };

  const formatCardNumber = (value: string) => {
    const numbers = value.replace(/\D/g, '');
    const groups = numbers.match(/.{1,4}/g);
    return groups ? groups.join(' ').slice(0, 19) : '';
  };

  const formatExpiry = (value: string) => {
    const numbers = value.replace(/\D/g, '');
    if (numbers.length >= 2) {
      return `${numbers.slice(0, 2)}/${numbers.slice(2, 4)}`;
    }
    return numbers;
  };

  const handleInputChange = (event: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;

    if (name === 'number') {
      setNewCard((prev) => ({ ...prev, number: formatCardNumber(value) }));
      return;
    }

    if (name === 'expiry') {
      setNewCard((prev) => ({ ...prev, expiry: formatExpiry(value) }));
      return;
    }

    if (name === 'cvv') {
      setNewCard((prev) => ({ ...prev, cvv: value.replace(/\D/g, '').slice(0, 4) }));
      return;
    }

    setNewCard((prev) => ({ ...prev, [name]: value }));
  };

  const handleAddCard = async () => {
    if (!user?.id) return;

    const brand = newCard.number.startsWith('4') ? 'Visa' : 'Mastercard';
    const last4 = newCard.number.replace(/\s/g, '').slice(-4);
    const maskedDetails = `${brand} **** ${last4} (Exp: ${newCard.expiry})`;
    const isFirst = methods.length === 0;

    try {
      await paymentService.addPaymentMethod({
        ownerId: user.id,
        type: 'CREDIT_CARD',
        maskedDetails,
        isDefault: isFirst,
      } as unknown as CreatePaymentMethodRequest);

      await loadPaymentMethods();
      setNewCard({ name: '', number: '', expiry: '', cvv: '' });
      setIsModalOpen(false);
    } catch (error) {
      console.error('Failed to add payment method', error);
      alert(t('payments.addFailed'));
    }
  };

  const handleDeleteCard = async (id: string) => {
    if (!window.confirm(t('payments.confirmDelete'))) return;

    try {
      await paymentService.deletePaymentMethod(id);
      setMethods((prev) => prev.filter((method) => method.id !== id));
    } catch (error) {
      console.error('Failed to delete payment method', error);
    }
  };

  const handleSetDefault = async (id: string) => {
    try {
      await paymentService.setDefaultPaymentMethod(id);
      await loadPaymentMethods();
    } catch (error) {
      console.error('Failed to set default payment method', error);
    }
  };

  const isFormValid =
    Boolean(newCard.name) &&
    newCard.number.length === 19 &&
    newCard.expiry.length === 5 &&
    newCard.cvv.length >= 3;

  const parseCardDetails = (maskedDetails: string) => {
    const lower = maskedDetails.toLowerCase();
    const isVisa = lower.includes('visa');
    const isMaster = lower.includes('master') || lower.includes('mc');

    const match = maskedDetails.match(/(\d{4})/);
    const last4 = match ? match[1] : '????';

    const expMatch = maskedDetails.match(/Exp: (\d{2}\/\d{2})/);
    const expiry = expMatch ? expMatch[1] : '';

    return {
      brand: isVisa ? 'visa' : isMaster ? 'mastercard' : 'other',
      last4,
      expiry,
    };
  };

  const CardBrandIcon = ({ brand }: { brand: string }) => (
    <div
      className={`px-2 py-1 rounded text-xs font-bold ${
        brand === 'visa'
          ? 'bg-blue-600 text-white'
          : brand === 'mastercard'
            ? 'bg-orange-500 text-white'
            : 'bg-gray-500 text-white'
      }`}
    >
      {brand === 'visa' ? 'VISA' : brand === 'mastercard' ? 'MC' : 'CARD'}
    </div>
  );

  if (isLoading && methods.length === 0) {
    return <div className="text-center py-8">{t('payments.loading')}</div>;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-navy-800">{t('payments.title')}</h1>
        <p className="text-navy-500 mt-1">{t('payments.description')}</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {methods.map((method) => {
          const parsed = parseCardDetails(method.maskedDetails);
          return (
            <div
              key={method.id}
              className="relative p-6 rounded-2xl bg-gradient-to-br from-navy-700 to-navy-900 text-white overflow-hidden"
            >
              <div className="absolute inset-0 opacity-10">
                <div className="absolute -right-8 -top-8 w-32 h-32 border-4 border-white rounded-full" />
                <div className="absolute -right-4 -top-4 w-24 h-24 border-2 border-white rounded-full" />
              </div>

              <div className="relative">
                <div className="flex items-start justify-between mb-8">
                  <CardBrandIcon brand={parsed.brand} />
                  {method.isDefault && (
                    <span className="flex items-center gap-1 text-xs bg-white/20 px-2 py-1 rounded-full">
                      <Star className="w-3 h-3 fill-current" />
                      {t('common.default')}
                    </span>
                  )}
                </div>

                <div className="mb-6">
                  <p className="text-lg tracking-widest font-mono">•••• •••• •••• {parsed.last4}</p>
                </div>

                <div className="flex items-end justify-between">
                  <div>
                    {parsed.expiry && (
                      <>
                        <p className="text-xs text-white/60 uppercase">{t('payments.expires')}</p>
                        <p className="font-medium">{parsed.expiry}</p>
                      </>
                    )}
                  </div>
                  <div className="flex gap-2">
                    {!method.isDefault && (
                      <button
                        onClick={() => handleSetDefault(method.id)}
                        className="p-2 bg-white/10 hover:bg-white/20 rounded-lg transition-colors"
                        title={t('payments.setDefault')}
                      >
                        <Star className="w-4 h-4" />
                      </button>
                    )}
                    <button
                      onClick={() => handleDeleteCard(method.id)}
                      className="p-2 bg-red-500/20 hover:bg-red-500/40 rounded-lg transition-colors"
                      title={t('payments.deleteCard')}
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          );
        })}

        <button
          onClick={() => setIsModalOpen(true)}
          className="flex flex-col items-center justify-center gap-3 p-8 rounded-2xl border-2 border-dashed border-navy-300 hover:border-primary-400 hover:bg-primary-50 transition-colors group min-h-[220px]"
        >
          <div className="w-12 h-12 rounded-full bg-navy-100 group-hover:bg-primary-100 flex items-center justify-center transition-colors">
            <Plus className="w-6 h-6 text-navy-500 group-hover:text-primary-600" />
          </div>
          <span className="font-medium text-navy-600 group-hover:text-primary-600">{t('payments.addMethod')}</span>
        </button>
      </div>

      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/50" onClick={() => setIsModalOpen(false)} />
          <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-md p-6">
            <div className="flex items-center justify-between mb-6">
              <div className="flex items-center gap-2">
                <CreditCard className="w-5 h-5 text-primary-600" />
                <h3 className="text-lg font-semibold text-navy-800">{t('payments.addNewCard')}</h3>
              </div>
              <button onClick={() => setIsModalOpen(false)} className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
                <X className="w-5 h-5 text-navy-500" />
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="label">{t('payments.cardholderName')}</label>
                <input
                  type="text"
                  name="name"
                  value={newCard.name}
                  onChange={handleInputChange}
                  className="input"
                  placeholder={t('payments.nameOnCard')}
                />
              </div>
              <div>
                <label className="label">{t('payments.cardNumber')}</label>
                <input
                  type="text"
                  name="number"
                  value={newCard.number}
                  onChange={handleInputChange}
                  className="input font-mono"
                  placeholder="1234 5678 9012 3456"
                  maxLength={19}
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">{t('payments.expiryDate')}</label>
                  <input
                    type="text"
                    name="expiry"
                    value={newCard.expiry}
                    onChange={handleInputChange}
                    className="input"
                    placeholder={t('payments.expiryPlaceholder')}
                    maxLength={5}
                  />
                </div>
                <div>
                  <label className="label">{t('payments.cvv')}</label>
                  <input
                    type="text"
                    name="cvv"
                    value={newCard.cvv}
                    onChange={handleInputChange}
                    className="input"
                    placeholder="123"
                    maxLength={4}
                  />
                </div>
              </div>
            </div>

            <div className="flex gap-3 mt-6">
              <button onClick={() => setIsModalOpen(false)} className="btn-secondary flex-1 py-2.5">
                {t('common.cancel')}
              </button>
              <button
                onClick={handleAddCard}
                disabled={!isFormValid}
                className={`btn-primary flex-1 py-2.5 ${!isFormValid ? 'opacity-50 cursor-not-allowed' : ''}`}
              >
                {t('payments.addCard')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
