import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import AuthBrand from '../components/AuthBrand';
import FormField from '../components/FormField';
import Notice from '../components/Notice';
import SubmitButton from '../components/SubmitButton';
import useFocusFirstError from '../components/useFocusFirstError';
import { useAuth } from '../context/AuthContext';
import { authService } from '../services/authService';
import { validateNewPassword } from '../services/validation';

const PASSWORD_HINT = '8 to 72 characters (fewer if you use emoji or accented letters), with at least one letter and one number.';
const EMPTY = { newPassword: '', confirmPassword: '' };

/** FR-075: set a new password with the token from the emailed link. */
export default function ResetPasswordPage() {
  const [params] = useSearchParams();
  const token = params.get('token');
  const { refresh, user } = useAuth();
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const [linkDead, setLinkDead] = useState(false);
  const formRef = useFocusFirstError(errors);

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setMessage('');
    const errs = validateNewPassword(form);
    setErrors(errs);
    if (Object.keys(errs).length) return;

    setBusy(true);
    try {
      const result = await authService.resetPassword({ token, ...form });
      setForm(EMPTY);
      setNotice(result.message);
      await refresh(); // the reset ended this user's sessions
    } catch (err) {
      setErrors(err.fieldErrors || {});
      setMessage(err.message);
      setLinkDead(err.code === 'INVALID_RESET_TOKEN');
    } finally {
      setBusy(false);
    }
  };

  // /login sends a signed-in user on to the dashboard.
  const exitLabel = user ? 'Go to my dashboard' : 'Go to sign in';

  return (
    <main className="auth-page">
      <AuthBrand />
      <div className="card auth-card">
        <div>
          <h2 id="reset-heading" className="auth-title">Reset password</h2>
          {!notice && token && !linkDead && <p className="auth-intro">Choose a new password. This signs you out on all your devices.</p>}
        </div>
        {notice && (
          <>
            <Notice tone="success" title="All done"><p>{notice}</p></Notice>
            <Link className="button-link" to="/login">{exitLabel}</Link>
          </>
        )}
        {!notice && !token && (
          <>
            <Notice tone="error" title="This link won't work"><p>This reset link is incomplete. Please request a new one.</p></Notice>
            <Link className="button-link" to="/forgot-password">Request a new link</Link>
          </>
        )}
        {!notice && token && linkDead && (
          <>
            <Notice tone="error" title="This link won't work"><p>{message}</p></Notice>
            <Link className="button-link" to="/forgot-password">Request a new link</Link>
          </>
        )}
        {!notice && token && !linkDead && (
          <>
            {message && <Notice tone="error" title="Your password was not reset"><p>{message}</p></Notice>}
            <form ref={formRef} className="stack" onSubmit={onSubmit} noValidate aria-labelledby="reset-heading">
              <FormField label="New password" name="newPassword" type="password" value={form.newPassword} onChange={onChange} error={errors.newPassword} hint={PASSWORD_HINT} autoComplete="new-password" />
              <FormField label="Confirm new password" name="confirmPassword" type="password" value={form.confirmPassword} onChange={onChange} error={errors.confirmPassword} autoComplete="new-password" />
              <SubmitButton busy={busy} busyLabel="Saving...">Reset password</SubmitButton>
            </form>
          </>
        )}
        {!notice && (
          <div className="auth-links">
            {token && !linkDead && <Link className="text-link" to="/forgot-password">Request a new link</Link>}
            <Link className="text-link" to="/login">{exitLabel}</Link>
          </div>
        )}
      </div>
    </main>
  );
}
