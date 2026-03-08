import type { ReactNode } from 'react';

interface PageShellProps {
  title: string;
  description?: string;
  actions?: ReactNode;
  children?: ReactNode;
  className?: string;
  bodyClassName?: string;
}

export default function PageShell({
  title,
  description,
  actions,
  children,
  className,
  bodyClassName,
}: PageShellProps): JSX.Element {
  const shellClassName = ['theme-surface-scope mx-auto w-full space-y-6', className].filter(Boolean).join(' ');
  const bodyClass = ['space-y-6', bodyClassName].filter(Boolean).join(' ');

  return (
    <section className={shellClassName}>
      <div className="glass-panel flex flex-col gap-4 rounded-2xl p-5 sm:flex-row sm:items-start sm:justify-between sm:p-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100 sm:text-3xl">{title}</h1>
          {description ? <p className="mt-1 text-sm text-slate-500 dark:text-slate-300 sm:text-base">{description}</p> : null}
        </div>
        {actions ? <div className="flex shrink-0 items-center gap-2">{actions}</div> : null}
      </div>
      {children ? <div className={bodyClass}>{children}</div> : null}
    </section>
  );
}
