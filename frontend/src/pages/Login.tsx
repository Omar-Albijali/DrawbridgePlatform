import { FormEvent, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import PageShell from '../components/PageShell';
import { useAuth } from '../contexts/AuthContext';

interface LocationState {
  from?: { pathname?: string };
}

export default function Login(): JSX.Element {
  const { login, isLoading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(true);
  const [error, setError] = useState('');

  const from = (location.state as LocationState | null)?.from?.pathname ?? '/dashboard';

  const handleSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault();
    setError('');

    const result = await login(email, password, rememberMe);
    if (!result.success) {
      setError(result.message ?? 'Failed to login');
      return;
    }

    navigate(from, { replace: true });
  };

  return (
    <PageShell
      title="Login"
      description="Sign in to access Drawbridge dashboard features."
      actions={
        <form className="auth-form" onSubmit={handleSubmit}>
          <label>
            Email
            <input
              type="email"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="name@company.com"
            />
          </label>
          <label>
            Password
            <input
              type="password"
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Enter password"
            />
          </label>
          <label className="auth-form__checkbox">
            <input
              type="checkbox"
              checked={rememberMe}
              onChange={(event) => setRememberMe(event.target.checked)}
            />
            Remember me
          </label>
          {error ? <p className="auth-form__error">{error}</p> : null}
          <button type="submit" disabled={isLoading}>
            {isLoading ? 'Signing in...' : 'Sign In'}
          </button>
          <p>
            New here? <Link to="/register">Create account</Link>
          </p>
          <p>
            Forgot access? <Link to="/forgot-password">Reset password</Link>
          </p>
        </form>
      }
    />
  );
}
