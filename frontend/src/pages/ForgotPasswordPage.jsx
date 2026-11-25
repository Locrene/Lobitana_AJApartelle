import { useState } from 'react';
import { Link } from 'react-router-dom';
import AuthBrand from '../components/AuthBrand';
import FormField from '../components/FormField';
import Notice from '../components/Notice';
import SubmitButton from '../components/SubmitButton';
import useFocusFirstError from '../components/useFocusFirstError';
import { useAuth } from '../context/AuthContext';
import { authService } from '../services/authService';
import { validateEmail } from '../services/validation';

/** FR-074: ask for a reset link. The answer is the same whether or not the account exists. */
export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const { user } = useAuth();
  const formRef = useFocusFirstError(errors);

  const onSubmit = async (e) => {
    e.preventDefault();
    setMessage(''); setNotice('');
    const address = email.trim();
    const errs = validateEmail(address);
    setErrors(errs);
    if (Object.keys(errs).length) return;

    setBusy(true);
    try {
      setNotice((await authService.forgotPassword(address)).message);
    } catch (err) {
      setErrors(err.fieldErrors || {});
      setMessage(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="auth-page">
      <AuthBrand />
      <form ref={formRef} className="card auth-card" onSubmit={onSubmit} noValidate aria-labelledby="forgot-heading">
        <div>
          <h2 id="forgot-heading" className="auth-title">Forgot password</h2>
          <p className="auth-intro">Enter your email address. The link we send works once, for 30 minutes.</p>
        </div>
        {message && <Notice tone="error" title="We couldn't send your request"><p>{message}</p></Notice>}
        {notice && (
          <Notice tone="success" title="Check your email">
            <p>{notice}</p>
            <p>Not there after a few minutes? Look in your spam folder.</p>
          </Notice>
        )}
        <FormField label="Email" name="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} error={errors.email} autoComplete="email" />
        <SubmitButton busy={busy} busyLabel="Sending...">Send reset link</SubmitButton>
        <Link className="text-link" to={user ? '/profile' : '/login'}>{user ? 'Back to My Profile' : 'Back to sign in'}</Link>
      </form>
    </main>
  );
}
