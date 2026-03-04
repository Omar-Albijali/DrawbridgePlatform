import type { ReactNode } from 'react';

interface PageShellProps {
  title: string;
  description: string;
  actions?: ReactNode;
}

export default function PageShell({ title, description, actions }: PageShellProps): JSX.Element {
  return (
    <section className="page-shell">
      <div className="page-shell__header">
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      {actions ? <div className="page-shell__actions">{actions}</div> : null}
    </section>
  );
}
