import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import FormField from '../components/FormField';
import { authService } from '../services/authService';
import { validateAccount } from '../services/validation';

const EMPTY = { fullName: '', email: '', mobileNumber: '', password: '', confirmPassword: '' };

export default function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setMessage('');
    const errs = validateAccount(form);
    setErrors(errs);
    if (Object.keys(errs).length) return;

    setBusy(true);
    try {
      await authService.register(form);
      navigate('/login', { replace: true, state: { registered: true } });
    } catch (err) {
      setErrors(err.fieldErrors || {});
      setMessage(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={onSubmit} noValidate>
        <h1>AJ Apartelle</h1>
        <h2>Create a guest account</h2>
        {message && <p className="alert">{message}</p>}
        <FormField label="Full name" name="fullName" value={form.fullName} onChange={onChange} error={errors.fullName} autoComplete="name" />
        <FormField label="Email" name="email" type="email" value={form.email} onChange={onChange} error={errors.email} autoComplete="email" />
        <FormField label="Mobile number (09XXXXXXXXX)" name="mobileNumber" value={form.mobileNumber} onChange={onChange} error={errors.mobileNumber} inputMode="numeric" maxLength={11} />
        <FormField label="Password" name="password" type="password" value={form.password} onChange={onChange} error={errors.password} autoComplete="new-password" />
        <FormField label="Confirm password" name="confirmPassword" type="password" value={form.confirmPassword} onChange={onChange} error={errors.confirmPassword} autoComplete="new-password" />
        <button type="submit" disabled={busy}>{busy ? 'Creating account...' : 'Create account'}</button>
        <p>Already have an account? <Link to="/login">Sign in</Link></p>
      </form>
    </div>
  );
}
