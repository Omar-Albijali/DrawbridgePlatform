import { useTranslation } from 'react-i18next';
import PageShell from '../components/PageShell';

export default function Reports(): JSX.Element {
  const { t } = useTranslation();
  return <PageShell title={t('reports.title')} description={t('reports.description')} />;
}
