import { useEffect, useRef, useState, type ChangeEvent, type FormEvent } from 'react';
import {
  CheckCircle2,
  Clock3,
  FileImage,
  FileText,
  LifeBuoy,
  LoaderCircle,
  Paperclip,
  RefreshCw,
  SendHorizontal,
  Ticket,
} from 'lucide-react';
import PageShell from '../components/PageShell';
import { useAuth } from '../contexts/AuthContext';
import { supportService } from '../services/supportService';
import { type SupportTicket } from '../types';

type SupportTab = 'create' | 'tickets';
type SupportCategoryToken = 'ORDER' | 'POS' | 'PAYMENT' | 'OTHER';

const categoryOptions: Array<{ value: SupportCategoryToken; label: string; hint: string }> = [
  { value: 'ORDER', label: 'Order', hint: 'Questions about order status, delivery, or order issues.' },
  { value: 'POS', label: 'POS', hint: 'Issues related to POS flows, terminals, or store checkout.' },
  { value: 'PAYMENT', label: 'Payment', hint: 'Payment failures, settlement issues, or payment methods.' },
  { value: 'OTHER', label: 'Other', hint: 'Anything else that needs the support team.' },
];

function enumToken(value: unknown): string {
  if (typeof value === 'string') {
    return value;
  }

  if (typeof value === 'object' && value !== null && 'name' in value) {
    const token = (value as { name?: unknown }).name;
    if (typeof token === 'string') {
      return token;
    }
  }

  return String(value ?? '');
}

function categoryLabel(value: unknown): string {
  const token = enumToken(value);
  switch (token) {
    case 'ORDER':
      return 'Order';
    case 'POS':
      return 'POS';
    case 'PAYMENT':
      return 'Payment';
    default:
      return 'Other';
  }
}

function statusLabel(value: unknown): string {
  const token = enumToken(value);
  switch (token) {
    case 'OPEN':
      return 'Open';
    case 'IN_PROGRESS':
      return 'In Progress';
    case 'CLOSED':
      return 'Closed';
    default:
      return token || 'Unknown';
  }
}

function statusClassName(value: unknown): string {
  const token = enumToken(value);
  switch (token) {
    case 'OPEN':
      return 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300';
    case 'IN_PROGRESS':
      return 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300';
    case 'CLOSED':
      return 'bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-200';
    default:
      return 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-200';
  }
}

function formatDateTime(value: string): string {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function isImageAttachment(url: string | null | undefined): boolean {
  if (!url) {
    return false;
  }

  const lower = url.toLowerCase();
  return ['.png', '.jpg', '.jpeg', '.gif', '.webp', '.svg'].some((extension) => lower.includes(extension));
}

export default function Support(): JSX.Element {
  const { user } = useAuth();
  const attachmentInputRef = useRef<HTMLInputElement | null>(null);

  const [activeTab, setActiveTab] = useState<SupportTab>('create');
  const [subject, setSubject] = useState('');
  const [category, setCategory] = useState<SupportCategoryToken>('ORDER');
  const [description, setDescription] = useState('');
  const [attachment, setAttachment] = useState<File | null>(null);
  const [tickets, setTickets] = useState<SupportTicket[]>([]);
  const [selectedTicketId, setSelectedTicketId] = useState<string | null>(null);
  const [selectedTicket, setSelectedTicket] = useState<SupportTicket | null>(null);
  const [isLoadingTickets, setIsLoadingTickets] = useState(true);
  const [isLoadingDetails, setIsLoadingDetails] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitMessage, setSubmitMessage] = useState<string | null>(null);
  const [formErrorMessage, setFormErrorMessage] = useState<string | null>(null);
  const [ticketErrorMessage, setTicketErrorMessage] = useState<string | null>(null);

  const loadTicketDetails = async (ticketId: string): Promise<void> => {
    try {
      setIsLoadingDetails(true);
      setTicketErrorMessage(null);
      const details = await supportService.getTicket(ticketId);
      setSelectedTicket(details);
    } catch (error) {
      console.error('Failed to fetch support ticket details', error);
      setSelectedTicket(null);
      setTicketErrorMessage('Failed to load ticket details.');
    } finally {
      setIsLoadingDetails(false);
    }
  };

  const loadTickets = async (preferredTicketId?: string | null): Promise<void> => {
    try {
      setIsLoadingTickets(true);
      setTicketErrorMessage(null);

      const data = await supportService.getMyTickets();
      const normalizedTickets = Array.isArray(data) ? data : [];
      setTickets(normalizedTickets);

      const requestedTicketId = preferredTicketId ?? selectedTicketId;
      const fallbackTicketId = requestedTicketId && normalizedTickets.some((ticket) => ticket.id === requestedTicketId)
        ? requestedTicketId
        : normalizedTickets[0]?.id ?? null;

      setSelectedTicketId(fallbackTicketId);

      if (fallbackTicketId) {
        await loadTicketDetails(fallbackTicketId);
      } else {
        setSelectedTicket(null);
      }
    } catch (error) {
      console.error('Failed to fetch support tickets', error);
      setTickets([]);
      setSelectedTicket(null);
      setSelectedTicketId(null);
      setTicketErrorMessage('Failed to load your support tickets.');
    } finally {
      setIsLoadingTickets(false);
    }
  };

  useEffect(() => {
    if (!user?.id) {
      setIsLoadingTickets(false);
      return;
    }

    void loadTickets();
  }, [user?.id]);

  useEffect(() => {
    if (!submitMessage) {
      return undefined;
    }

    const timer = window.setTimeout(() => {
      setSubmitMessage(null);
    }, 4500);

    return () => window.clearTimeout(timer);
  }, [submitMessage]);

  const handleAttachmentChange = (event: ChangeEvent<HTMLInputElement>): void => {
    const nextFile = event.target.files?.[0] ?? null;
    setAttachment(nextFile);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault();

    if (!subject.trim() || !description.trim()) {
      setFormErrorMessage('Subject and description are required.');
      return;
    }

    try {
      setIsSubmitting(true);
      setFormErrorMessage(null);

      const createdTicket = await supportService.createTicket({
        subject,
        category,
        description,
        attachment,
      });

      setSubject('');
      setCategory('ORDER');
      setDescription('');
      setAttachment(null);
      if (attachmentInputRef.current) {
        attachmentInputRef.current.value = '';
      }

      setSubmitMessage(
        '\u062A\u0645 \u0627\u0633\u062A\u0644\u0627\u0645 \u062A\u0630\u0643\u0631\u062A\u0643\u060C \u0648\u0633\u064A\u062A\u0645 \u0627\u0644\u0631\u062F \u0639\u0644\u064A\u0643 \u062E\u0644\u0627\u0644 24 \u0625\u0644\u0649 48 \u0633\u0627\u0639\u0629 \u0639\u0645\u0644.'
      );
      setActiveTab('tickets');
      await loadTickets(createdTicket.id);
    } catch (error) {
      console.error('Failed to create support ticket', error);
      setFormErrorMessage('Failed to submit your ticket. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <PageShell
      title="Support"
      description="Create a support ticket, attach helpful context, and track previous requests from one place."
    >
      {submitMessage ? (
        <div className="fixed right-4 top-24 z-50 max-w-md rounded-2xl border border-emerald-200 bg-white px-4 py-3 shadow-xl dark:border-emerald-800/60 dark:bg-slate-900">
          <div className="flex items-start gap-3">
            <CheckCircle2 className="mt-0.5 h-5 w-5 text-emerald-600 dark:text-emerald-300" />
            <p className="text-sm font-medium text-slate-800 dark:text-slate-100">{submitMessage}</p>
          </div>
        </div>
      ) : null}

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1.2fr)_minmax(320px,0.8fr)]">
        <div className="space-y-6">
          <div className="card overflow-hidden p-0">
            <div className="grid grid-cols-2 gap-0 border-b border-slate-200/80 dark:border-white/10">
              <button
                type="button"
                onClick={() => setActiveTab('create')}
                className={`flex items-center justify-center gap-2 px-4 py-4 text-sm font-semibold transition ${
                  activeTab === 'create'
                    ? 'bg-primary-500/10 text-primary-700 dark:text-primary-300'
                    : 'text-slate-500 hover:bg-slate-50 dark:text-slate-300 dark:hover:bg-slate-800/70'
                }`}
              >
                <SendHorizontal className="h-4 w-4" />
                Create Ticket
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('tickets')}
                className={`flex items-center justify-center gap-2 px-4 py-4 text-sm font-semibold transition ${
                  activeTab === 'tickets'
                    ? 'bg-primary-500/10 text-primary-700 dark:text-primary-300'
                    : 'text-slate-500 hover:bg-slate-50 dark:text-slate-300 dark:hover:bg-slate-800/70'
                }`}
              >
                <Ticket className="h-4 w-4" />
                My Tickets
              </button>
            </div>

            {activeTab === 'create' ? (
              <div className="space-y-6 p-6">
                <div className="rounded-2xl border border-primary-200/70 bg-gradient-to-r from-primary-50 to-blue-50/70 p-4 dark:border-primary-800/40 dark:from-primary-900/20 dark:to-slate-900">
                  <div className="flex items-start gap-3">
                    <LifeBuoy className="mt-0.5 h-5 w-5 text-primary-700 dark:text-primary-300" />
                    <div>
                      <p className="font-semibold text-slate-900 dark:text-slate-100">Share enough detail for a faster resolution</p>
                      <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
                        Include what happened, when it happened, and any order or payment context you already have.
                      </p>
                    </div>
                  </div>
                </div>

                <form className="space-y-5" onSubmit={(event) => void handleSubmit(event)}>
                  <div>
                    <label className="label" htmlFor="support-subject">
                      Subject
                    </label>
                    <input
                      id="support-subject"
                      type="text"
                      className="input"
                      placeholder="Short summary of the issue"
                      value={subject}
                      onChange={(event) => setSubject(event.target.value)}
                    />
                  </div>

                  <div>
                    <label className="label" htmlFor="support-category">
                      Category
                    </label>
                    <select
                      id="support-category"
                      className="input"
                      value={category}
                      onChange={(event) => setCategory(event.target.value as SupportCategoryToken)}
                    >
                      {categoryOptions.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                    <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">
                      {categoryOptions.find((option) => option.value === category)?.hint}
                    </p>
                  </div>

                  <div>
                    <label className="label" htmlFor="support-description">
                      Description
                    </label>
                    <textarea
                      id="support-description"
                      className="input min-h-36 resize-y"
                      placeholder="Describe the issue in detail"
                      value={description}
                      onChange={(event) => setDescription(event.target.value)}
                    />
                  </div>

                  <div>
                    <label className="label" htmlFor="support-attachment">
                      Attachment (optional)
                    </label>
                    <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50/70 p-4 dark:border-white/15 dark:bg-slate-950/30">
                      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                        <div className="flex items-center gap-3">
                          <Paperclip className="h-5 w-5 text-slate-500 dark:text-slate-300" />
                          <div>
                            <p className="text-sm font-medium text-slate-800 dark:text-slate-100">
                              {attachment ? attachment.name : 'Add a screenshot, invoice, or related file'}
                            </p>
                            <p className="text-xs text-slate-500 dark:text-slate-400">Maximum upload size follows the platform upload settings.</p>
                          </div>
                        </div>
                        <label className="btn-secondary cursor-pointer">
                          Choose File
                          <input
                            ref={attachmentInputRef}
                            id="support-attachment"
                            type="file"
                            className="hidden"
                            onChange={handleAttachmentChange}
                          />
                        </label>
                      </div>
                    </div>
                  </div>

                  {formErrorMessage ? (
                    <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900/50 dark:bg-red-950/20 dark:text-red-300">
                      {formErrorMessage}
                    </div>
                  ) : null}

                  <button type="submit" className="btn-primary w-full sm:w-auto" disabled={isSubmitting}>
                    {isSubmitting ? (
                      <>
                        <LoaderCircle className="mr-2 h-4 w-4 animate-spin" />
                        Submitting...
                      </>
                    ) : (
                      <>
                        <SendHorizontal className="mr-2 h-4 w-4" />
                        Submit Ticket
                      </>
                    )}
                  </button>
                </form>
              </div>
            ) : (
              <div className="space-y-4 p-6">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="text-lg font-semibold text-slate-900 dark:text-slate-100">My Tickets</p>
                    <p className="text-sm text-slate-500 dark:text-slate-300">Review your previous requests and open the full details of each ticket.</p>
                  </div>
                  <button
                    type="button"
                    onClick={() => void loadTickets()}
                    className="btn-secondary"
                    disabled={isLoadingTickets}
                  >
                    <RefreshCw className={`mr-2 h-4 w-4 ${isLoadingTickets ? 'animate-spin' : ''}`} />
                    Refresh
                  </button>
                </div>

                {ticketErrorMessage ? (
                  <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900/50 dark:bg-red-950/20 dark:text-red-300">
                    {ticketErrorMessage}
                  </div>
                ) : null}

                {isLoadingTickets ? (
                  <div className="flex min-h-56 items-center justify-center rounded-2xl border border-dashed border-slate-300 dark:border-white/10">
                    <LoaderCircle className="h-6 w-6 animate-spin text-primary-600" />
                  </div>
                ) : tickets.length === 0 ? (
                  <div className="rounded-2xl border border-dashed border-slate-300 p-8 text-center dark:border-white/10">
                    <Ticket className="mx-auto h-10 w-10 text-slate-400" />
                    <p className="mt-3 text-base font-semibold text-slate-900 dark:text-slate-100">No tickets yet</p>
                    <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Create your first support request and it will appear here.</p>
                  </div>
                ) : (
                  <div className="space-y-3">
                    {tickets.map((ticket) => (
                      <button
                        key={ticket.id}
                        type="button"
                        onClick={() => {
                          setSelectedTicketId(ticket.id);
                          void loadTicketDetails(ticket.id);
                        }}
                        className={`w-full rounded-2xl border p-4 text-left transition ${
                          selectedTicketId === ticket.id
                            ? 'border-primary-300 bg-primary-50/60 shadow-card dark:border-primary-700/60 dark:bg-primary-900/15'
                            : 'border-slate-200 bg-white hover:border-slate-300 dark:border-white/10 dark:bg-slate-900/70'
                        }`}
                      >
                        <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                          <div className="space-y-2">
                            <div className="flex flex-wrap items-center gap-2">
                              <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700 dark:bg-slate-800 dark:text-slate-200">
                                {ticket.ticketNumber}
                              </span>
                              <span className={`rounded-full px-3 py-1 text-xs font-semibold ${statusClassName(ticket.status)}`}>
                                {statusLabel(ticket.status)}
                              </span>
                            </div>
                            <p className="text-base font-semibold text-slate-900 dark:text-slate-100">{ticket.subject}</p>
                            <div className="flex flex-wrap gap-4 text-sm text-slate-500 dark:text-slate-300">
                              <span>Category: {categoryLabel(ticket.category)}</span>
                              <span>Created: {formatDateTime(ticket.createdAt)}</span>
                              <span>Updated: {formatDateTime(ticket.updatedAt)}</span>
                            </div>
                          </div>
                        </div>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        <div className="space-y-6">
          <div className="card space-y-4">
            <div className="flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-primary-100 text-primary-700 dark:bg-primary-900/30 dark:text-primary-300">
                <Clock3 className="h-5 w-5" />
              </div>
              <div>
                <p className="font-semibold text-slate-900 dark:text-slate-100">Response Window</p>
                <p className="text-sm text-slate-500 dark:text-slate-300">We aim to respond within 24 to 48 business hours.</p>
              </div>
            </div>
          </div>

          <div className="card min-h-[28rem] space-y-4">
            <div>
              <p className="text-lg font-semibold text-slate-900 dark:text-slate-100">Ticket Details</p>
              <p className="text-sm text-slate-500 dark:text-slate-300">Select a ticket from My Tickets to inspect the full request.</p>
            </div>

            {isLoadingDetails ? (
              <div className="flex min-h-72 items-center justify-center">
                <LoaderCircle className="h-6 w-6 animate-spin text-primary-600" />
              </div>
            ) : !selectedTicket ? (
              <div className="flex min-h-72 items-center justify-center rounded-2xl border border-dashed border-slate-300 p-6 text-center dark:border-white/10">
                <div>
                  <Ticket className="mx-auto h-10 w-10 text-slate-400" />
                  <p className="mt-3 font-semibold text-slate-900 dark:text-slate-100">No ticket selected</p>
                  <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Open My Tickets and choose one ticket to view its description and attachment.</p>
                </div>
              </div>
            ) : (
              <div className="space-y-5">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700 dark:bg-slate-800 dark:text-slate-200">
                    {selectedTicket.ticketNumber}
                  </span>
                  <span className={`rounded-full px-3 py-1 text-xs font-semibold ${statusClassName(selectedTicket.status)}`}>
                    {statusLabel(selectedTicket.status)}
                  </span>
                </div>

                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="rounded-2xl bg-slate-50 p-4 dark:bg-slate-950/40">
                    <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Subject</p>
                    <p className="mt-2 text-sm font-medium text-slate-900 dark:text-slate-100">{selectedTicket.subject}</p>
                  </div>
                  <div className="rounded-2xl bg-slate-50 p-4 dark:bg-slate-950/40">
                    <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Category</p>
                    <p className="mt-2 text-sm font-medium text-slate-900 dark:text-slate-100">{categoryLabel(selectedTicket.category)}</p>
                  </div>
                  <div className="rounded-2xl bg-slate-50 p-4 dark:bg-slate-950/40">
                    <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Created</p>
                    <p className="mt-2 text-sm font-medium text-slate-900 dark:text-slate-100">{formatDateTime(selectedTicket.createdAt)}</p>
                  </div>
                  <div className="rounded-2xl bg-slate-50 p-4 dark:bg-slate-950/40">
                    <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Updated</p>
                    <p className="mt-2 text-sm font-medium text-slate-900 dark:text-slate-100">{formatDateTime(selectedTicket.updatedAt)}</p>
                  </div>
                </div>

                <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-white/10 dark:bg-slate-950/30">
                  <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Description</p>
                  <p className="mt-3 whitespace-pre-wrap text-sm leading-7 text-slate-700 dark:text-slate-200">{selectedTicket.description}</p>
                </div>

                <div className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-white/10 dark:bg-slate-950/30">
                  <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Attachment</p>
                  {selectedTicket.attachmentUrl ? (
                    <div className="mt-3 space-y-3">
                      <a
                        href={selectedTicket.attachmentUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="inline-flex items-center gap-2 text-sm font-semibold text-primary-600 hover:text-primary-700 dark:text-primary-300 dark:hover:text-primary-200"
                      >
                        <FileText className="h-4 w-4" />
                        Open attachment
                      </a>
                      {isImageAttachment(selectedTicket.attachmentUrl) ? (
                        <div className="overflow-hidden rounded-2xl border border-slate-200 dark:border-white/10">
                          <img
                            src={selectedTicket.attachmentUrl}
                            alt={selectedTicket.subject}
                            className="max-h-72 w-full object-cover"
                          />
                        </div>
                      ) : (
                        <div className="flex items-center gap-3 rounded-2xl bg-slate-50 p-4 dark:bg-slate-950/40">
                          <FileImage className="h-5 w-5 text-slate-400" />
                          <p className="text-sm text-slate-600 dark:text-slate-300">Preview is unavailable for this file type, but the attachment link is ready.</p>
                        </div>
                      )}
                    </div>
                  ) : (
                    <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">No attachment was included with this ticket.</p>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </PageShell>
  );
}
