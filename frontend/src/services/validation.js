// Client-side validation mirrors the backend rules (the backend re-validates everything).
export const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export const MOBILE_RE = /^09\d{9}$/;
export const PASSWORD_RE = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;
// bcrypt reads at most 72 bytes, and accented letters and emoji take 2 to 4 bytes each.
export const PASSWORD_MAX_BYTES = 72;

const PASSWORD_MESSAGE = 'Password must be at least 8 characters with at least one letter and one number.';
const PASSWORD_TOO_LONG = 'Password is too long. The limit is 72 bytes: 72 plain letters or numbers, fewer with accented letters or emoji.';

/** The problem with a new password, or null (FR-071, FR-073, FR-075). */
function newPasswordError(password = '') {
  if (new TextEncoder().encode(password).length > PASSWORD_MAX_BYTES) return PASSWORD_TOO_LONG;
  return PASSWORD_RE.test(password) ? null : PASSWORD_MESSAGE;
}

/** The editable profile fields (FR-072), also part of registration. */
export function validateProfile(f) {
  const e = {};
  const name = f.fullName?.trim() || '';
  if (name.length < 2) e.fullName = 'Full name is required (at least 2 characters).';
  else if (name.length > 100) e.fullName = 'Full name must be at most 100 characters.';
  if (!MOBILE_RE.test(f.mobileNumber || '')) e.mobileNumber = 'Mobile number must be in the format 09XXXXXXXXX (11 digits).';
  return e;
}

export function validateEmail(email) {
  return EMAIL_RE.test(email) && email.length <= 255 ? {} : { email: 'Enter a valid email address.' };
}

/** New password and confirmation (FR-075). */
export function validateNewPassword(f) {
  const e = {};
  const passwordError = newPasswordError(f.newPassword);
  if (passwordError) e.newPassword = passwordError;
  if (!f.confirmPassword) e.confirmPassword = 'Please confirm your new password.';
  else if (f.newPassword !== f.confirmPassword) e.confirmPassword = 'Passwords do not match.';
  return e;
}

/** Change password also needs the current password, and a different new one (FR-073). */
export function validatePasswordChange(f) {
  const e = validateNewPassword(f);
  if (!f.currentPassword?.trim()) e.currentPassword = 'Current password is required.';
  else if (f.newPassword === f.currentPassword) e.newPassword = 'New password must be different from the current password.';
  return e;
}

export function validateAccount(f, { confirm = true } = {}) {
  const e = validateProfile(f);
  if (!EMAIL_RE.test(f.email || '')) e.email = 'Enter a valid email address.';
  const passwordError = newPasswordError(f.password);
  if (passwordError) e.password = passwordError;
  if (confirm && f.password !== f.confirmPassword) e.confirmPassword = 'Passwords do not match.';
  return e;
}
