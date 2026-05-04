import { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { Trans, useTranslation } from 'react-i18next';
import {
  ArrowDownToLine,
  ArrowUpFromLine,
  Copy,
  KeyRound,
  RefreshCw,
  Save,
  Send,
  ShieldCheck,
  TerminalSquare,
  Webhook,
} from 'lucide-react';
import PageShell from '../components/PageShell';
import { useAuth } from '../contexts/AuthContext';
import { posIntegrationService, type PosIntegrationStatus } from '../services/posIntegrationService';
import type { PosIntegrationApiKeyRotate, PosIntegrationConfig, PosIntegrationEventLog } from '../types';
import { UserRole } from '../types';

function formatDateTime(value: string | null | undefined, language: string | undefined, fallback: string): string {
  if (!value) return fallback;
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return parsed.toLocaleString(language === 'ar' ? 'ar-SA' : undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function formatDelta(value: number | null | undefined): string {
  if (value == null) return '-';
  if (value > 0) return `+${value}`;
  return String(value);
}

const emptyConfig: PosIntegrationConfig = {
  retailerId: '',
  integrationExists: false,
  status: 'DISABLED',
  apiKeyPrefix: null,
  webhookEnabled: false,
  webhookUrl: null,
  webhookSecretConfigured: false,
  createdAt: null,
  rotatedAt: null,
};

export default function PosIntegration(): JSX.Element {
  const { i18n, t } = useTranslation();
  const { user } = useAuth();
  const isRtl = i18n.dir() === 'rtl';
  const pageDirection = isRtl ? 'rtl' : 'ltr';
  const textAlign = isRtl ? 'text-right' : 'text-left';

  const isRetailer = user?.role === UserRole.RETAILER;
  if (!isRetailer) {
    return <Navigate to="/dashboard" replace />;
  }

  const [config, setConfig] = useState<PosIntegrationConfig>(emptyConfig);
  const [statusDraft, setStatusDraft] = useState<PosIntegrationStatus>('DISABLED');
  const [webhookEnabledDraft, setWebhookEnabledDraft] = useState(false);
  const [webhookUrlDraft, setWebhookUrlDraft] = useState('');
  const [webhookSecretDraft, setWebhookSecretDraft] = useState('');
  const [events, setEvents] = useState<PosIntegrationEventLog[]>([]);
  const [generatedKey, setGeneratedKey] = useState<PosIntegrationApiKeyRotate | null>(null);

  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isRotating, setIsRotating] = useState(false);
  const [isRefreshingEvents, setIsRefreshingEvents] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  const syncDraftFromConfig = (next: PosIntegrationConfig): void => {
    setStatusDraft(next.status);
    setWebhookEnabledDraft(next.webhookEnabled);
    setWebhookUrlDraft(next.webhookUrl ?? '');
    setWebhookSecretDraft('');
  };

  const loadInitial = async (): Promise<void> => {
    setIsLoading(true);
    setError(null);
    try {
      const [nextConfig, nextEvents] = await Promise.all([
        posIntegrationService.getConfig(),
        posIntegrationService.getEventLogs(100),
      ]);
      setConfig(nextConfig);
      syncDraftFromConfig(nextConfig);
      setEvents(nextEvents);
    } catch (reason) {
      console.error('Failed to load POS integration data', reason);
      setError(t('posIntegration.errors.load'));
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void loadInitial();
  }, []);

  const refreshEvents = async (): Promise<void> => {
    setIsRefreshingEvents(true);
    try {
      const nextEvents = await posIntegrationService.getEventLogs(100);
      setEvents(nextEvents);
    } catch (reason) {
      console.error('Failed to refresh POS event logs', reason);
      setError(t('posIntegration.errors.refreshLogs'));
    } finally {
      setIsRefreshingEvents(false);
    }
  };

  const handleRotateKey = async (): Promise<void> => {
    setIsRotating(true);
    setError(null);
    setInfo(null);
    try {
      const key = await posIntegrationService.rotateApiKey();
      setGeneratedKey(key);
      const nextConfig = await posIntegrationService.getConfig();
      setConfig(nextConfig);
      syncDraftFromConfig(nextConfig);
      setInfo(t('posIntegration.messages.keyRotated'));
    } catch (reason) {
      console.error('Failed to rotate POS API key', reason);
      setError(t('posIntegration.errors.rotateKey'));
    } finally {
      setIsRotating(false);
    }
  };

  const handleSave = async (): Promise<void> => {
    if (!config.integrationExists) {
      setError(t('posIntegration.errors.generateFirst'));
      return;
    }
    setIsSaving(true);
    setError(null);
    setInfo(null);
    try {
      const updated = await posIntegrationService.updateConfig({
        status: statusDraft,
        webhookEnabled: webhookEnabledDraft,
        webhookUrl: webhookUrlDraft.trim().length > 0 ? webhookUrlDraft.trim() : null,
        webhookSecret: webhookSecretDraft.trim().length > 0 ? webhookSecretDraft.trim() : null,
      });
      setConfig(updated);
      syncDraftFromConfig(updated);
      setInfo(t('posIntegration.messages.saved'));
    } catch (reason) {
      console.error('Failed to save POS config', reason);
      setError(t('posIntegration.errors.save'));
    } finally {
      setIsSaving(false);
    }
  };

  const handleCopyKey = async (): Promise<void> => {
    if (!generatedKey?.apiKey) return;
    try {
      await navigator.clipboard.writeText(generatedKey.apiKey);
      setGeneratedKey(null);
      setInfo(t('posIntegration.messages.copied'));
    } catch {
      setError(t('posIntegration.errors.copy'));
    }
  };

  if (isLoading) {
    return (
      <div className="flex min-h-[420px] items-center justify-center">
        <div className="h-12 w-12 animate-spin rounded-full border-b-2 border-primary-600" />
      </div>
    );
  }

  return (
    <PageShell
      title={t('posIntegration.title')}
      description={t('posIntegration.description')}
      bodyClassName={textAlign}
      actions={
        <button type="button" onClick={() => void loadInitial()} className="btn-secondary gap-2">
          <RefreshCw className="h-4 w-4" />
          {t('posIntegration.reload')}
        </button>
      }
    >
      {error ? (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-800/60 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      ) : null}
      {info ? (
        <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 dark:border-emerald-800/60 dark:bg-emerald-900/20 dark:text-emerald-300">
          {info}
        </div>
      ) : null}



      {/* Inbound/Outbound Contract */}
      <div dir={pageDirection} className={`card space-y-4 ${textAlign}`}>
        <div>
          <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('posIntegration.integrationContract')}</h2>
          <p className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">
            <Trans
              i18nKey="posIntegration.contractDescription"
              components={{
                apiKey: <code dir="ltr" className="rounded bg-slate-100 px-1 py-0.5 font-mono text-xs text-slate-700 dark:bg-slate-800 dark:text-slate-300" />,
                retailerId: <code dir="ltr" className="rounded bg-slate-100 px-1 py-0.5 font-mono text-xs text-slate-700 dark:bg-slate-800 dark:text-slate-300" />,
              }}
            />
          </p>
        </div>

        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          <div className="rounded-xl border border-slate-200 bg-slate-50/50 p-4 dark:border-white/10 dark:bg-slate-800/30">
            <div className="flex items-center gap-2 text-sm font-semibold text-slate-800 dark:text-slate-100">
              <ArrowDownToLine className="h-4 w-4" />
              {t('posIntegration.inboundContract')}
            </div>
            <p className="mt-3 text-xs text-slate-500 dark:text-slate-400">{t('posIntegration.endpoint')}</p>
            <p dir="ltr" className="text-left font-mono text-xs text-slate-800 dark:text-slate-200">POST /api/pos/webhooks/inventory.changed</p>
            <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">{t('posIntegration.requiredHeaders')}</p>
            <p dir="ltr" className="text-left font-mono text-xs text-slate-800 dark:text-slate-200">X-API-Key: pos_live_...</p>
            <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">{t('posIntegration.requiredBody')}</p>
            <p dir="ltr" className="text-left font-mono text-xs text-slate-800 dark:text-slate-200">eventId, retailerId, gtin, changeType + (quantityDelta or quantityAfter)</p>
          </div>

          <div className="rounded-xl border border-slate-200 bg-slate-50/50 p-4 dark:border-white/10 dark:bg-slate-800/30">
            <div className="flex items-center gap-2 text-sm font-semibold text-slate-800 dark:text-slate-100">
              <ArrowUpFromLine className="h-4 w-4" />
              {t('posIntegration.outboundContract')}
            </div>
            <p className="mt-3 text-xs text-slate-500 dark:text-slate-400">{t('posIntegration.webhookEvent')}</p>
            <p dir="ltr" className="text-left font-mono text-xs text-slate-800 dark:text-slate-200">inventory.changed</p>
            <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">{t('posIntegration.ifWebhookDisabled')}</p>
            <p dir="ltr" className="text-left font-mono text-xs text-slate-800 dark:text-slate-200">GET /api/pos/inventory/events</p>
            <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">{t('posIntegration.changeCoverage')}</p>
            <p className="text-xs text-slate-700 dark:text-slate-300">{t('posIntegration.changeCoverageDescription')}</p>
          </div>
        </div>
      </div>

      {/* Credentials + Webhook — single card, two columns */}
      <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">

          {/* Left — API Credentials */}
          <div dir={pageDirection} className={`card flex flex-col gap-4 ${textAlign}`}>
	            <div className="flex items-center justify-between gap-3">
	              <div className="flex items-center gap-2 text-sm font-semibold text-slate-800 dark:text-slate-100">
	                <KeyRound className="h-4 w-4 shrink-0 text-slate-500 dark:text-slate-400" />
	                {t('posIntegration.apiCredentials')}
	              </div>
              <button
                type="button"
                onClick={() => void handleRotateKey()}
                disabled={isRotating}
                className="btn-secondary gap-1.5 py-1.5 text-xs"
              >
	                <KeyRound className="h-3.5 w-3.5" />
	                {config.integrationExists
	                  ? isRotating ? t('posIntegration.rotating') : t('posIntegration.rotateKey')
	                  : isRotating ? t('posIntegration.generating') : t('posIntegration.generateKey')}
	              </button>
	            </div>

            <label className="flex cursor-pointer items-center justify-between gap-4 rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-3 transition hover:bg-slate-100/60 dark:border-white/10 dark:bg-slate-800/30 dark:hover:bg-slate-800/60">
	              <div>
	                <p className="text-sm font-semibold text-slate-800 dark:text-slate-100">{t('posIntegration.active')}</p>
	                <p className="text-xs text-slate-500 dark:text-slate-400">
	                  {statusDraft === 'ACTIVE' ? t('posIntegration.requestsAccepted') : t('posIntegration.paused')}
	                </p>
	              </div>
              <div className="relative shrink-0">
                <input
                  type="checkbox"
                  checked={statusDraft === 'ACTIVE'}
                  onChange={(event) => setStatusDraft(event.target.checked ? 'ACTIVE' : 'DISABLED')}
                  className="sr-only"
                />
                <div
                  className={`relative h-6 w-11 rounded-full transition-colors duration-200 ${
                    statusDraft === 'ACTIVE' ? 'bg-primary-500' : 'bg-slate-300 dark:bg-slate-600'
                  }`}
                >
                  <div
                    className={`absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition-[left,right] duration-200 ${
                      statusDraft === 'ACTIVE'
                        ? isRtl ? 'left-0.5' : 'right-0.5'
                        : isRtl ? 'right-0.5' : 'left-0.5'
                    }`}
                  />
                </div>
              </div>
            </label>

            {/* Key prefix + retailer ID + timestamps */}
            <div className="divide-y divide-slate-100 rounded-xl border border-slate-200 bg-slate-50/50 dark:divide-white/10 dark:border-white/10 dark:bg-slate-800/30">
              <div className="flex items-center justify-between px-3.5 py-2.5">
	                <span className="text-xs text-slate-500 dark:text-slate-400">{t('posIntegration.keyPrefix')}</span>
	                <span dir="ltr" className="font-mono text-xs font-semibold text-slate-800 dark:text-slate-100">
	                  {config.apiKeyPrefix ?? t('posIntegration.notGenerated')}
	                </span>
	              </div>
	              <div className="flex items-center justify-between px-3.5 py-2.5">
	                <span className="text-xs text-slate-500 dark:text-slate-400">{t('posIntegration.retailerId')}</span>
	                <span dir="ltr" className="font-mono text-xs text-slate-700 dark:text-slate-300">{config.retailerId || '-'}</span>
	              </div>
	              <div className="flex items-center justify-between px-3.5 py-2.5">
	                <span className="text-xs text-slate-500 dark:text-slate-400">{t('posIntegration.created')}</span>
	                <span className="text-xs text-slate-700 dark:text-slate-300">{formatDateTime(config.createdAt, i18n.resolvedLanguage, t('posIntegration.never'))}</span>
	              </div>
	              {config.rotatedAt && (
	                <div className="flex items-center justify-between px-3.5 py-2.5">
	                  <span className="text-xs text-slate-500 dark:text-slate-400">{t('posIntegration.lastRotated')}</span>
	                  <span className="text-xs text-slate-700 dark:text-slate-300">{formatDateTime(config.rotatedAt, i18n.resolvedLanguage, t('posIntegration.never'))}</span>
	                </div>
	              )}
            </div>

            {generatedKey ? (
	              <div className="rounded-xl border border-amber-300 bg-amber-50 p-3 dark:border-amber-700/50 dark:bg-amber-900/20">
	                <p className="text-xs font-semibold text-amber-900 dark:text-amber-300">
	                  {t('posIntegration.shownOnce')}
	                </p>
	                <div className="mt-1.5 flex items-center gap-2">
	                  <p dir="ltr" className="flex-1 break-all text-left font-mono text-xs text-amber-800 dark:text-amber-400">
	                    {generatedKey.apiKey}
	                  </p>
	                  <button type="button" onClick={() => void handleCopyKey()} className="btn-secondary gap-1.5 py-1.5 text-xs shrink-0">
	                    <Copy className="h-3.5 w-3.5" />
	                    {t('posIntegration.copy')}
	                  </button>
	                </div>
	              </div>
            ) : null}
          </div>

          {/* Right — Outbound Webhook */}
          <div dir={pageDirection} className={`card flex flex-col gap-4 ${textAlign}`}>
	            <div className="flex items-center gap-2 text-sm font-semibold text-slate-800 dark:text-slate-100">
	              <Webhook className="h-4 w-4 shrink-0 text-slate-500 dark:text-slate-400" />
	              {t('posIntegration.outboundWebhook')}
	            </div>


            <label className="flex cursor-pointer items-center justify-between gap-4 rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-3 transition hover:bg-slate-100/60 dark:border-white/10 dark:bg-slate-800/30 dark:hover:bg-slate-800/60">
	              <div>
	                <p className="text-sm font-semibold text-slate-800 dark:text-slate-100">{t('posIntegration.enableWebhooks')}</p>
	                <p className="text-xs text-slate-500 dark:text-slate-400">{t('posIntegration.postEvents')}</p>
	              </div>
              <div className="relative shrink-0">
                <input
                  type="checkbox"
                  checked={webhookEnabledDraft}
                  onChange={(event) => setWebhookEnabledDraft(event.target.checked)}
                  className="sr-only"
                />
                <div
                  className={`relative h-6 w-11 rounded-full transition-colors duration-200 ${
                    webhookEnabledDraft ? 'bg-primary-500' : 'bg-slate-300 dark:bg-slate-600'
                  }`}
                >
                  <div
                    className={`absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition-[left,right] duration-200 ${
                      webhookEnabledDraft
                        ? isRtl ? 'left-0.5' : 'right-0.5'
                        : isRtl ? 'right-0.5' : 'left-0.5'
                    }`}
                  />
                </div>
              </div>
            </label>

            {webhookEnabledDraft ? (
              <>
	                <label className="space-y-1.5 block">
	                  <span className="label mb-0 text-xs">{t('posIntegration.webhookUrl')}</span>
	                  <input
	                    type="url"
	                    value={webhookUrlDraft}
	                    onChange={(event) => setWebhookUrlDraft(event.target.value)}
	                    dir="ltr"
	                    className="input py-2 text-left text-sm"
	                    placeholder="https://pos.example.com/webhooks/inventory"
	                  />
	                </label>
	                <label className="space-y-1.5 block">
	                  <span className="label mb-0 text-xs">{t('posIntegration.webhookSecret')}</span>
	                  <input
	                    type="text"
	                    value={webhookSecretDraft}
	                    onChange={(event) => setWebhookSecretDraft(event.target.value)}
	                    dir="ltr"
	                    className="input py-2 text-left text-sm"
	                    placeholder={
	                      config.webhookSecretConfigured
	                        ? t('posIntegration.keepCurrentSecret')
	                        : t('posIntegration.setSigningSecret')
	                    }
	                  />
                </label>
              </>
            ) : (
              <div className="flex items-start gap-2.5 rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-3 dark:border-white/10 dark:bg-slate-800/40">
                <Webhook className="mt-0.5 h-4 w-4 shrink-0 text-slate-400 dark:text-slate-500" />
	                <p className="text-xs text-slate-500 dark:text-slate-400">
	                  {t('posIntegration.pullUpdatesVia')}{' '}
	                  <code dir="ltr" className="inline-block rounded bg-slate-200 px-1 py-0.5 text-left font-mono dark:bg-slate-700 dark:text-slate-300">
	                    GET /api/pos/inventory/events
	                  </code>
	                </p>
              </div>
            )}
          </div>

        {/* Shared save */}
        <div dir={pageDirection} className="flex items-center justify-end border-t border-slate-100 pt-4 dark:border-white/10 lg:col-span-2">
          <button
            type="button"
            onClick={() => void handleSave()}
            disabled={isSaving || !config.integrationExists}
            className="btn-primary gap-2"
          >
	            <Save className="h-4 w-4" />
	            {isSaving ? t('posIntegration.saving') : t('posIntegration.saveChanges')}
	          </button>
        </div>
      </div>

      {/* Event Logs Section */}
      <div dir={pageDirection} className={`card space-y-4 ${textAlign}`}>
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('posIntegration.syncLogs')}</h2>
            <p className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">{t('posIntegration.syncLogsDescription')}</p>
          </div>
          <button
            type="button"
            onClick={() => void refreshEvents()}
            disabled={isRefreshingEvents}
            className="btn-secondary gap-2 shrink-0"
          >
            <RefreshCw className="h-4 w-4" />
            {isRefreshingEvents ? t('posIntegration.refreshing') : t('posIntegration.refresh')}
          </button>
        </div>

        {events.length === 0 ? (
          <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-8 text-center text-sm text-slate-500 dark:border-white/10 dark:bg-slate-800/40 dark:text-slate-400">
            {t('posIntegration.noEvents')}
          </div>
        ) : (
          <div className="overflow-x-auto rounded-xl border border-slate-200 dark:border-white/10">
            <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-white/10">
              <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-800/60 dark:text-slate-400">
                <tr>
                  <th className={`px-3 py-2 font-semibold ${textAlign}`}>{t('posIntegration.time')}</th>
                  <th className={`px-3 py-2 font-semibold ${textAlign}`}>{t('posIntegration.direction')}</th>
                  <th className={`px-3 py-2 font-semibold ${textAlign}`}>{t('posIntegration.event')}</th>
                  <th className={`px-3 py-2 font-semibold ${textAlign}`}>{t('posIntegration.status')}</th>
                  <th className={`px-3 py-2 font-semibold ${textAlign}`}>{t('posIntegration.gtin')}</th>
                  <th className={`px-3 py-2 font-semibold ${textAlign}`}>{t('posIntegration.delta')}</th>
                  <th className={`px-3 py-2 font-semibold ${textAlign}`}>{t('posIntegration.error')}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-white/5">
                {events.map((event) => (
                  <tr
                    key={`${event.direction}-${event.eventId}`}
                    className="bg-white transition-colors hover:bg-slate-50 dark:bg-slate-900/50 dark:hover:bg-slate-800/60"
                  >
                    <td className="px-3 py-2 text-slate-600 dark:text-slate-300">
                      {formatDateTime(event.eventTime, i18n.resolvedLanguage, t('posIntegration.never'))}
                    </td>
                    <td className="px-3 py-2">
                      <span
                        className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-semibold ${
                          event.direction === 'OUTBOUND'
                            ? 'bg-indigo-100 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-300'
                            : 'bg-fuchsia-100 text-fuchsia-700 dark:bg-fuchsia-900/30 dark:text-fuchsia-300'
                        }`}
                      >
                        {event.direction === 'OUTBOUND' ? (
                          <Send className="h-3.5 w-3.5" />
                        ) : (
                          <TerminalSquare className="h-3.5 w-3.5" />
                        )}
                        {t(`posIntegration.directions.${event.direction}`, { defaultValue: event.direction })}
                      </span>
                    </td>
                    <td className="px-3 py-2">
                      <p dir="ltr" className="text-left font-mono text-xs text-slate-500 dark:text-slate-300">{event.eventId}</p>
                      <p dir="ltr" className="mt-0.5 text-left text-xs text-slate-500 dark:text-slate-400">{event.eventType}</p>
                    </td>
                    <td className="px-3 py-2">
                      <span className="inline-flex rounded-full bg-slate-200 px-2 py-0.5 text-xs font-semibold text-slate-700 dark:bg-slate-700 dark:text-slate-300">
                        {event.status}
                      </span>
                    </td>
                    <td dir="ltr" className="px-3 py-2 text-left font-mono text-xs text-slate-600 dark:text-slate-300">
                      {event.gtin ?? '-'}
                    </td>
                    <td dir="ltr" className="px-3 py-2 text-left text-slate-700 dark:text-slate-200">{formatDelta(event.changeAmount)}</td>
                    <td className="px-3 py-2 text-xs text-red-600 dark:text-red-400">{event.lastError ?? '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
          <ShieldCheck className="h-4 w-4 shrink-0" />
          {t('posIntegration.logsFootnote')}
        </div>
      </div>
    </PageShell>
  );
}
