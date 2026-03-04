import { Link } from 'react-router-dom';
import PageShell from '../components/PageShell';

export default function ForgotPassword(): JSX.Element {
  return (
    <PageShell
      title="Forgot Password"
      description="Password recovery flow will be connected in Task 4 migration waves."
      actions={<Link to="/login">Back to Login</Link>}
    />
  );
}
