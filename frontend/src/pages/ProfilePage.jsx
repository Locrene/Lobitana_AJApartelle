import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import FormField from '../components/FormField';
import Icon from '../components/Icon';
import Notice from '../components/Notice';
import SubmitButton from '../components/SubmitButton';
import useFocusFirstError from '../components/useFocusFirstError';
import { useAuth } from '../context/AuthContext';
import { profileService } from '../services/authService';
import { validatePasswordChange, validateProfile } from '../services/validation';

const ROLE_LABEL = { GUEST: 'Guest', STAFF: 'Front desk', OWNER: 'Owner' };
const PASSWORD_HINT = '8 to 72 characters (fewer if you use emoji or accented letters), with at least one letter and one number. It must differ from your current password.';
const EMPTY_PASSWORDS = { currentPassword: '', newPassword: '', confirmPassword: '' };

/** One form's values, errors and submit state. A 401 refreshes auth, and the route guard goes to sign-in. */
function useAccountForm(initial, validate, save) {
  const { refresh } = useAuth();
  const [form, setForm] = useState(initial);
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setMessage(''); setNotice('');
    const errs = validate(form);
    setErrors(errs);
    if (Object.keys(errs).length) return;

    setBusy(true);
    try {
      const { values, done } = await save(form);
      setForm(values);
      setNotice(done);
    } catch (err) {
      if (err.status === 401) { await refresh(); return; }
      setErrors(err.fieldErrors || {});
      setMessage(err.message);
    } finally {
      setBusy(false);
    }
  };

  return { form, errors, message, notice, busy, onChange, onSubmit };
}

/** FR-072: edit name and mobile number; email and role are read-only. */
function ProfileForm({ profile }) {
  const { updateUser } = useAuth();
  const { form, errors, message, notice, busy, onChange, onSubmit } = useAccountForm(
    { fullName: profile.fullName, mobileNumber: profile.mobileNumber || '' },
    validateProfile,
    async (values) => {
      const saved = await profileService.update(values);
      updateUser(saved);
      return { values: { fullName: saved.fullName, mobileNumber: saved.mobileNumber }, done: 'Profile saved.' };
    },
  );

  const formRef = useFocusFirstError(errors);

  return (
    <form ref={formRef} className="card stack" onSubmit={onSubmit} noValidate aria-labelledby="profile-heading">
      <h2 id="profile-heading" className="card-title"><span className="title-icon"><Icon name="user" /></span>Personal details</h2>
      <div className="readonly">
        <dl>
          <dt>Email</dt><dd>{profile.email}</dd>
          <dt>Role</dt><dd><span className="badge">{ROLE_LABEL[profile.role] || profile.role}</span></dd>
        </dl>
        <p className="meta">Your email and role can&apos;t be changed.</p>
      </div>
      <FormField label="Full name" name="fullName" value={form.fullName} onChange={onChange} error={errors.fullName} autoComplete="name" />
      <FormField label="Mobile number" name="mobileNumber" value={form.mobileNumber} onChange={onChange} error={errors.mobileNumber} hint="11 digits, starting with 09." inputMode="numeric" maxLength={11} autoComplete="tel-national" />
      {message && <Notice tone="error" title="Your profile was not saved"><p>{message}</p></Notice>}
      {notice && <Notice tone="success" title={notice} />}
      <SubmitButton busy={busy} busyLabel="Saving...">Save profile</SubmitButton>
    </form>
  );
}

/** FR-073: the server renews this session's cookie and ends all the others. */
function ChangePasswordForm() {
  const { form, errors, message, notice, busy, onChange, onSubmit } = useAccountForm(
    EMPTY_PASSWORDS,
    validatePasswordChange,
    async (values) => {
      await profileService.changePassword(values);
      return { values: EMPTY_PASSWORDS, done: 'Password changed. Your other sessions have been signed out.' };
    },
  );

  const formRef = useFocusFirstError(errors);

  return (
    <form ref={formRef} className="card stack" onSubmit={onSubmit} noValidate aria-labelledby="password-heading">
      <div>
        <h2 id="password-heading" className="card-title"><span className="title-icon"><Icon name="key" /></span>Change password</h2>
        <p className="meta">You&apos;ll stay signed in here and be signed out everywhere else.</p>
      </div>
      <div>
        <FormField label="Current password" name="currentPassword" type="password" value={form.currentPassword} onChange={onChange} error={errors.currentPassword} autoComplete="current-password" />
        <Link className="text-link start" to="/forgot-password">Forgot your current password?</Link>
      </div>
      <FormField label="New password" name="newPassword" type="password" value={form.newPassword} onChange={onChange} error={errors.newPassword} hint={PASSWORD_HINT} autoComplete="new-password" />
      <FormField label="Confirm new password" name="confirmPassword" type="password" value={form.confirmPassword} onChange={onChange} error={errors.confirmPassword} autoComplete="new-password" />
      {/* A rejected field (such as a wrong current password) is already shown under that field. */}
      {message && !Object.keys(errors).length && <Notice tone="error" title="Your password was not changed"><p>{message}</p></Notice>}
      {notice && <Notice tone="success" title={notice} />}
      <SubmitButton busy={busy} busyLabel="Saving...">Change password</SubmitButton>
    </form>
  );
}

export default function ProfilePage() {
  const { refresh } = useAuth();
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    profileService.get()
      .then(setProfile)
      .catch((err) => (err.status === 401 ? refresh() : setError(err.message)));
  }, [refresh]);

  return (
    <>
      <div className="page-head">
        <h1>My Profile</h1>
        <p className="meta">Update your contact details or change your password.</p>
      </div>
      {error && <Notice tone="error" title="We couldn't load your profile"><p>{error}</p><p>Refresh the page to try again.</p></Notice>}
      {!error && !profile && <p className="card meta" role="status">Loading your profile...</p>}
      {!error && profile && (
        <div className="profile-grid">
          <ProfileForm profile={profile} />
          <ChangePasswordForm />
        </div>
      )}
    </>
  );
}
