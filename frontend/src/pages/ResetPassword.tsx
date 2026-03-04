import { Link } from 'react-router-dom';
import PageShell from '../components/PageShell';

export default function ResetPassword(): JSX.Element {
  return (
    <PageShell
      title="Reset Password"
      description="Use your reset token link to submit a new password."
      actions={<Link to="/login">Return to Login</Link>}
    />
  );
}
