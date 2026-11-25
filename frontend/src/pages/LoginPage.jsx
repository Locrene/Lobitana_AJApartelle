import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import FormField from '../components/FormField';
import AuthBrand from '../components/AuthBrand';
import { DASHBOARD_BY_ROLE, useAuth } from '../context/AuthContext';
import { EMAIL_RE } from '../services/validation';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setMessage('');
    const errs = {};
    if (!EMAIL_RE.test(form.email)) errs.email = 'Enter a valid email address.';
    if (!form.password) errs.password = 'Password is required.';
    setErrors(errs);
    if (Object.keys(errs).length) return;

    setBusy(true);
    try {
      const user = await login(form.email, form.password);
      navigate(DASHBOARD_BY_ROLE[user.role], { replace: true });
    } catch (err) {
      setMessage(err.message || 'Login failed.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="auth-page">
      <AuthBrand />
      <form className="card auth-card" onSubmit={onSubmit} noValidate>
        <div>
          <h2 className="auth-title">Sign in</h2>
          <p className="auth-intro">Good to see you again.</p>
        </div>
        {location.state?.registered && <p className="success">Account created. You can now sign in.</p>}
        {message && <p className="alert">{message}</p>}
        <FormField label="Email" name="email" type="email" value={form.email} onChange={onChange} error={errors.email} autoComplete="email" />
        <FormField label="Password" name="password" type="password" value={form.password} onChange={onChange} error={errors.password} autoComplete="current-password" />
        <button type="submit" disabled={busy}>{busy ? 'Signing in...' : 'Sign in'}</button>
        <Link className="text-link" to="/forgot-password">Forgot password?</Link>
        <p className="auth-alt">No account yet? <Link className="text-link" to="/register">Create a guest account</Link></p>
      </form>
    </main>
  );
}
