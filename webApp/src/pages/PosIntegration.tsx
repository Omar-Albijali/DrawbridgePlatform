import { useEffect, useMemo, useState } from 'react';
import { Navigate } from 'react-router-dom';
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

function formatDateTime(value: string | null | undefined): string {
  if (!value) return 'Never';
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return parsed.toLocaleString(undefined, {
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
  const { user } = useAuth();

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

  const statusChip = useMemo(() => {
    if (config.status === 'ACTIVE') {
      return 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300';
    }
    return 'bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-300';
  }, [config.status]);

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
      setError('Unable to load POS integration data');
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
      setError('Unable to refresh event logs');
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
      setInfo('API key rotated. Copy it now — it is shown once.');
    } catch (reason) {
      console.error('Failed to rotate POS API key', reason);
      setError('Unable to rotate API key');
    } finally {
      setIsRotating(false);
    }
  };

  const handleSave = async (): Promise<void> => {
    if (!config.integrationExists) {
      setError('Generate an API key first');
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
      setInfo('POS configuration saved');
    } catch (reason) {
      console.error('Failed to save POS config', reason);
      setError('Unable to save POS configuration');
    } finally {
      setIsSaving(false);
    }
  };

  const handleCopyKey = async (): Promise<void> => {
    if (!generatedKey?.apiKey) return;
    try {
      await navigator.clipboard.writeText(generatedKey.apiKey);
      setGeneratedKey(null);
      setInfo('API key copied to clipboard');
    } catch {
      setError('Could not copy API key');
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
      title="POS Integration"
      description="Configure API key auth, outbound webhooks, and inspect POS sync event logs."
      actions={
        <button type="button" onClick={() => void loadInitial()} className="btn-secondary gap-2">
          <RefreshCw className="h-4 w-4" />
          Reload
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
      <div className="card space-y-4">
        <div>
          <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">Integration Contract</h2>
          <p className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">
            Use `X-API-Key` auth. Inbound payload `retailerId` must match this page retailer scope.
          </p>
        </div>

        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          <div className="rounded-xl border border-slate-200 bg-slate-50/50 p-4 dark:border-white/10 dark:bg-slate-800/30">
            <div className="flex items-center gap-2 text-sm font-semibold text-slate-800 dark:text-slate-100">
              <ArrowDownToLine className="h-4 w-4" />
              Inbound (POS → Platform)
            </div>
            <p className="mt-3 text-xs text-slate-500 dark:text-slate-400">Endpoint</p>
            <p className="font-mono text-xs text-slate-800 dark:text-slate-200">POST /api/pos/webhooks/inventory.changed</p>
            <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">Required headers</p>
            <p className="font-mono text-xs text-slate-800 dark:text-slate-200">X-API-Key: pos_live_...</p>
            <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">Required body</p>
            <p className="font-mono text-xs text-slate-800 dark:text-slate-200">eventId, retailerId, gtin, changeType + (quantityDelta or quantityAfter)</p>
          </div>

          <div className="rounded-xl border border-slate-200 bg-slate-50/50 p-4 dark:border-white/10 dark:bg-slate-800/30">
            <div className="flex items-center gap-2 text-sm font-semibold text-slate-800 dark:text-slate-100">
              <ArrowUpFromLine className="h-4 w-4" />
              Outbound (Platform → POS)
            </div>
            <p className="mt-3 text-xs text-slate-500 dark:text-slate-400">Webhook event</p>
            <p className="font-mono text-xs text-slate-800 dark:text-slate-200">inventory.changed</p>
            <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">If webhook disabled</p>
            <p className="font-mono text-xs text-slate-800 dark:text-slate-200">GET /api/pos/inventory/events</p>
            <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">Change coverage</p>
            <p className="text-xs text-slate-700 dark:text-slate-300">Manual + order-complete inventory changes.</p>
          </div>
        </div>
      </div>

      {/* Credentials + Webhook — single card, two columns */}
      <div className="card">
        <div className="grid grid-cols-1 divide-y divide-slate-100 dark:divide-white/10 lg:grid-cols-2 lg:divide-x lg:divide-y-0">

          {/* Left — API Credentials */}
          <div className="flex flex-col gap-4 pb-6 lg:pb-0 lg:pr-6">
            <div className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-2 text-sm font-semibold text-slate-800 dark:text-slate-100">
                <KeyRound className="h-4 w-4 shrink-0 text-slate-500 dark:text-slate-400" />
                API Credentials
              </div>
              <button
                type="button"
                onClick={() => void handleRotateKey()}
                disabled={isRotating}
                className="btn-secondary gap-1.5 py-1.5 text-xs"
              >
                <KeyRound className="h-3.5 w-3.5" />
                {config.integrationExists
                  ? isRotating ? 'Rotating...' : 'Rotate Key'
                  : isRotating ? 'Generating...' : 'Generate Key'}
              </button>
            </div>

            <label className="flex cursor-pointer items-center justify-between gap-4 rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-3 transition hover:bg-slate-100/60 dark:border-white/10 dark:bg-slate-800/30 dark:hover:bg-slate-800/60">
              <div>
                <p className="text-sm font-semibold text-slate-800 dark:text-slate-100">Active</p>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  {statusDraft === 'ACTIVE' ? 'POS requests are accepted.' : 'Integration is paused.'}
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
                  className={`flex h-6 w-11 items-center rounded-full p-0.5 transition-colors duration-200 ${
                    statusDraft === 'ACTIVE' ? 'bg-primary-500' : 'bg-slate-300 dark:bg-slate-600'
                  }`}
                >
                  <div
                    className={`h-5 w-5 rounded-full bg-white shadow transition-transform duration-200 ${
                      statusDraft === 'ACTIVE' ? 'translate-x-5' : 'translate-x-0'
                    }`}
                  />
                </div>
              </div>
            </label>

            {/* Key prefix + retailer ID + timestamps */}
            <div className="divide-y divide-slate-100 rounded-xl border border-slate-200 bg-slate-50/50 dark:divide-white/10 dark:border-white/10 dark:bg-slate-800/30">
              <div className="flex items-center justify-between px-3.5 py-2.5">
                <span className="text-xs text-slate-500 dark:text-slate-400">Key prefix</span>
                <span className="font-mono text-xs font-semibold text-slate-800 dark:text-slate-100">
                  {config.apiKeyPrefix ?? 'Not generated'}
                </span>
              </div>
              <div className="flex items-center justify-between px-3.5 py-2.5">
                <span className="text-xs text-slate-500 dark:text-slate-400">Retailer ID</span>
                <span className="font-mono text-xs text-slate-700 dark:text-slate-300">{config.retailerId || '-'}</span>
              </div>
              <div className="flex items-center justify-between px-3.5 py-2.5">
                <span className="text-xs text-slate-500 dark:text-slate-400">Created</span>
                <span className="text-xs text-slate-700 dark:text-slate-300">{formatDateTime(config.createdAt)}</span>
              </div>
              {config.rotatedAt && (
                <div className="flex items-center justify-between px-3.5 py-2.5">
                  <span className="text-xs text-slate-500 dark:text-slate-400">Last rotated</span>
                  <span className="text-xs text-slate-700 dark:text-slate-300">{formatDateTime(config.rotatedAt)}</span>
                </div>
              )}
            </div>

            {generatedKey ? (
              <div className="rounded-xl border border-amber-300 bg-amber-50 p-3 dark:border-amber-700/50 dark:bg-amber-900/20">
                <p className="text-xs font-semibold text-amber-900 dark:text-amber-300">
                  Shown once — copy now
                </p>
                <div className="mt-1.5 flex items-center gap-2">
                  <p className="flex-1 break-all font-mono text-xs text-amber-800 dark:text-amber-400">
                    {generatedKey.apiKey}
                  </p>
                  <button type="button" onClick={() => void handleCopyKey()} className="btn-secondary gap-1.5 py-1.5 text-xs shrink-0">
                    <Copy className="h-3.5 w-3.5" />
                    Copy
                  </button>
                </div>
              </div>
            ) : null}
          </div>

          {/* Right — Outbound Webhook */}
          <div className="flex flex-col gap-4 pt-6 lg:pl-6 lg:pt-0">
            <div className="flex items-center gap-2 text-sm font-semibold text-slate-800 dark:text-slate-100">
              <Webhook className="h-4 w-4 shrink-0 text-slate-500 dark:text-slate-400" />
              Outbound Webhook
            </div>


            <label className="flex cursor-pointer items-center justify-between gap-4 rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-3 transition hover:bg-slate-100/60 dark:border-white/10 dark:bg-slate-800/30 dark:hover:bg-slate-800/60">
              <div>
                <p className="text-sm font-semibold text-slate-800 dark:text-slate-100">Enable webhooks</p>
                <p className="text-xs text-slate-500 dark:text-slate-400">POST events to your endpoint.</p>
              </div>
              <div className="relative shrink-0">
                <input
                  type="checkbox"
                  checked={webhookEnabledDraft}
                  onChange={(event) => setWebhookEnabledDraft(event.target.checked)}
                  className="sr-only"
                />
                <div
                  className={`flex h-6 w-11 items-center rounded-full p-0.5 transition-colors duration-200 ${
                    webhookEnabledDraft ? 'bg-primary-500' : 'bg-slate-300 dark:bg-slate-600'
                  }`}
                >
                  <div
                    className={`h-5 w-5 rounded-full bg-white shadow transition-transform duration-200 ${
                      webhookEnabledDraft ? 'translate-x-5' : 'translate-x-0'
                    }`}
                  />
                </div>
              </div>
            </label>

            {webhookEnabledDraft ? (
              <>
                <label className="space-y-1.5 block">
                  <span className="label mb-0 text-xs">Webhook URL</span>
                  <input
                    type="url"
                    value={webhookUrlDraft}
                    onChange={(event) => setWebhookUrlDraft(event.target.value)}
                    className="input py-2 text-sm"
                    placeholder="https://pos.example.com/webhooks/inventory"
                  />
                </label>
                <label className="space-y-1.5 block">
                  <span className="label mb-0 text-xs">Webhook Secret</span>
                  <input
                    type="text"
                    value={webhookSecretDraft}
                    onChange={(event) => setWebhookSecretDraft(event.target.value)}
                    className="input py-2 text-sm"
                    placeholder={
                      config.webhookSecretConfigured
                        ? 'Leave empty to keep current secret'
                        : 'Set a signing secret'
                    }
                  />
                </label>
              </>
            ) : (
              <div className="flex items-start gap-2.5 rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-3 dark:border-white/10 dark:bg-slate-800/40">
                <Webhook className="mt-0.5 h-4 w-4 shrink-0 text-slate-400 dark:text-slate-500" />
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  Pull updates via{' '}
                  <code className="rounded bg-slate-200 px-1 py-0.5 font-mono dark:bg-slate-700 dark:text-slate-300">
                    GET /api/pos/inventory/events
                  </code>
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Shared save */}
        <div className="mt-5 flex items-center justify-end border-t border-slate-100 pt-4 dark:border-white/10">
          <button
            type="button"
            onClick={() => void handleSave()}
            disabled={isSaving || !config.integrationExists}
            className="btn-primary gap-2"
          >
            <Save className="h-4 w-4" />
            {isSaving ? 'Saving...' : 'Save Changes'}
          </button>
        </div>
      </div>

      {/* Event Logs Section */}
      <div className="card space-y-4">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">Event Logs</h2>
            <p className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">Last 100 inbound and outbound sync events.</p>
          </div>
          <button
            type="button"
            onClick={() => void refreshEvents()}
            disabled={isRefreshingEvents}
            className="btn-secondary gap-2 shrink-0"
          >
            <RefreshCw className="h-4 w-4" />
            {isRefreshingEvents ? 'Refreshing...' : 'Refresh'}
          </button>
        </div>

        {events.length === 0 ? (
          <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-8 text-center text-sm text-slate-500 dark:border-white/10 dark:bg-slate-800/40 dark:text-slate-400">
            No POS events recorded yet.
          </div>
        ) : (
          <div className="overflow-x-auto rounded-xl border border-slate-200 dark:border-white/10">
            <table className="min-w-full divide-y divide-slate-200 text-sm dark:divide-white/10">
              <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-800/60 dark:text-slate-400">
                <tr>
                  <th className="px-3 py-2 text-left font-semibold">Time</th>
                  <th className="px-3 py-2 text-left font-semibold">Direction</th>
                  <th className="px-3 py-2 text-left font-semibold">Event</th>
                  <th className="px-3 py-2 text-left font-semibold">Status</th>
                  <th className="px-3 py-2 text-left font-semibold">GTIN</th>
                  <th className="px-3 py-2 text-left font-semibold">Delta</th>
                  <th className="px-3 py-2 text-left font-semibold">Error</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-white/5">
                {events.map((event) => (
                  <tr
                    key={`${event.direction}-${event.eventId}`}
                    className="bg-white transition-colors hover:bg-slate-50 dark:bg-slate-900/50 dark:hover:bg-slate-800/60"
                  >
                    <td className="px-3 py-2 text-slate-600 dark:text-slate-300">{formatDateTime(event.eventTime)}</td>
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
                        {event.direction}
                      </span>
                    </td>
                    <td className="px-3 py-2">
                      <p className="font-mono text-xs text-slate-500 dark:text-slate-300">{event.eventId}</p>
                      <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">{event.eventType}</p>
                    </td>
                    <td className="px-3 py-2">
                      <span className="inline-flex rounded-full bg-slate-200 px-2 py-0.5 text-xs font-semibold text-slate-700 dark:bg-slate-700 dark:text-slate-300">
                        {event.status}
                      </span>
                    </td>
                    <td className="px-3 py-2 font-mono text-xs text-slate-600 dark:text-slate-300">
                      {event.gtin ?? '-'}
                    </td>
                    <td className="px-3 py-2 text-slate-700 dark:text-slate-200">{formatDelta(event.changeAmount)}</td>
                    <td className="px-3 py-2 text-xs text-red-600 dark:text-red-400">{event.lastError ?? '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
          <ShieldCheck className="h-4 w-4 shrink-0" />
          Outbound events include manual and order-triggered inventory changes. Inbound logs track POS-delivered updates.
        </div>
      </div>
    </PageShell>
  );
}
