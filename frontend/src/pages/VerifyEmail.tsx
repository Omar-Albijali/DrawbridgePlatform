import { Link } from 'react-router-dom';
import PageShell from '../components/PageShell';

export default function VerifyEmail(): JSX.Element {
  return (
    <PageShell
      title="Verify Email"
      description="Check your inbox and verify your account before continuing."
      actions={<Link to="/login">Go to Login</Link>}
    />
  );
}
