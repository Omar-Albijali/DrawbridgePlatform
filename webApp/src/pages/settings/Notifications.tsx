import { useState, type ReactNode } from 'react';
import { AlertTriangle, Bell, Mail, Megaphone, MessageSquare, Package } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { UserRole } from '../../types';

interface ToggleSwitchProps {
  enabled: boolean;
  onChange: (enabled: boolean) => void;
  label: string;
  description: string;
  channel: string;
}

function ToggleSwitch({ enabled, onChange, label, description, channel }: ToggleSwitchProps): JSX.Element {
  return (
    <div className="flex items-center justify-between border-b border-gray-100 py-4 last:border-0 dark:border-white/10">
      <div className="flex-1">
        <div className="flex items-center gap-2">
          <h4 className="font-medium text-navy-800 dark:text-slate-100">{label}</h4>
          <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs text-navy-500 dark:bg-slate-800 dark:text-slate-300">{channel}</span>
        </div>
        <p className="mt-0.5 text-sm text-navy-500 dark:text-slate-300">{description}</p>
      </div>
      <button
        type="button"
        onClick={() => onChange(!enabled)}
        className={`relative w-12 h-6 rounded-full transition-colors duration-200 flex-shrink-0 ml-4 ${
          enabled ? 'bg-primary-600' : 'bg-gray-300 dark:bg-slate-600'
        }`}
      >
        <span
          className={`absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full shadow transition-transform duration-200 ${
            enabled ? 'translate-x-6' : 'translate-x-0'
          }`}
        />
      </button>
    </div>
  );
}

interface NotificationSection {
  id: string;
  title: string;
  icon: ReactNode;
  items: {
    id: string;
    label: string;
    description: string;
    channel: string;
  }[];
}

export default function Notifications(): JSX.Element {
  const { user } = useAuth();
  const isRetailer = user?.role === UserRole.RETAILER;

  const [preferences, setPreferences] = useState<Record<string, boolean>>({
    orderConfirmation: true,
    shippingStatus: true,
    lowStockWarning: true,
    autoRestockConfirmation: false,
    newWholesalers: true,
  });

  const handleToggle = (id: string, enabled: boolean) => {
    setPreferences((prev) => ({ ...prev, [id]: enabled }));
  };

  const sections: NotificationSection[] = [
    {
      id: 'order',
      title: 'Order Updates',
      icon: <Package className="w-5 h-5 text-primary-600" />,
      items: [
        {
          id: 'orderConfirmation',
          label: 'Order Confirmation',
          description: 'Receive confirmation when your order is placed',
          channel: 'Email',
        },
        {
          id: 'shippingStatus',
          label: 'Shipping Status Changes',
          description: 'Get notified when your order ships or arrives',
          channel: 'SMS & Email',
        },
      ],
    },
    ...(isRetailer
      ? [
          {
            id: 'inventory',
            title: 'Inventory Alerts',
            icon: <AlertTriangle className="w-5 h-5 text-amber-500" />,
            items: [
              {
                id: 'lowStockWarning',
                label: 'Low Stock Warning',
                description: 'Alert when inventory falls below threshold',
                channel: 'Push Notification',
              },
              {
                id: 'autoRestockConfirmation',
                label: 'Auto-Restock Confirmation',
                description: 'Confirmation when auto-restock orders are placed',
                channel: 'Email',
              },
            ],
          },
        ]
      : []),
    {
      id: 'marketing',
      title: 'Marketing',
      icon: <Megaphone className="w-5 h-5 text-green-500" />,
      items: [
        {
          id: 'newWholesalers',
          label: 'New Wholesaler Recommendations',
          description: 'Discover new suppliers that match your needs',
          channel: 'Email',
        },
      ],
    },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-navy-800 dark:text-slate-100">Notification Preferences</h1>
        <p className="mt-1 text-navy-500 dark:text-slate-300">Control what emails and notifications you receive</p>
      </div>

      <div className="card border border-primary-100 bg-gradient-to-r from-primary-50 to-blue-50 dark:border-white/10 dark:from-slate-900 dark:to-slate-800">
        <div className="flex items-center gap-4">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-primary-100 dark:bg-primary-500/20">
            <Bell className="w-6 h-6 text-primary-600" />
          </div>
          <div>
            <h3 className="font-semibold text-navy-800 dark:text-slate-100">Notification Channels</h3>
            <div className="flex items-center gap-4 mt-1">
              <span className="flex items-center gap-1.5 text-sm text-navy-600 dark:text-slate-300">
                <Mail className="w-4 h-4" /> Email
              </span>
              <span className="flex items-center gap-1.5 text-sm text-navy-600 dark:text-slate-300">
                <MessageSquare className="w-4 h-4" /> SMS
              </span>
              <span className="flex items-center gap-1.5 text-sm text-navy-600 dark:text-slate-300">
                <Bell className="w-4 h-4" /> Push
              </span>
            </div>
          </div>
        </div>
      </div>

      {sections.map((section) => (
        <div key={section.id} className="card">
          <div className="mb-4 flex items-center gap-2 border-b border-gray-200 pb-4 dark:border-white/10">
            {section.icon}
            <h3 className="text-lg font-semibold text-navy-800 dark:text-slate-100">{section.title}</h3>
          </div>
          <div>
            {section.items.map((item) => (
              <ToggleSwitch
                key={item.id}
                enabled={preferences[item.id] ?? false}
                onChange={(enabled) => handleToggle(item.id, enabled)}
                label={item.label}
                description={item.description}
                channel={item.channel}
              />
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
