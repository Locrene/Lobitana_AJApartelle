// Client-side validation mirrors the backend rules (the backend re-validates everything).
export const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export const MOBILE_RE = /^09\d{9}$/;
export const PASSWORD_RE = /^(?=.*[A-Za-z])(?=.*\d).{8,72}$/;

export function validateAccount(f, { confirm = true } = {}) {
  const e = {};
  if (!f.fullName?.trim() || f.fullName.trim().length < 2) e.fullName = 'Full name is required (at least 2 characters).';
  if (!EMAIL_RE.test(f.email || '')) e.email = 'Enter a valid email address.';
  if (!MOBILE_RE.test(f.mobileNumber || '')) e.mobileNumber = 'Mobile number must be in the format 09XXXXXXXXX (11 digits).';
  if (!PASSWORD_RE.test(f.password || '')) e.password = 'Password must be 8-72 characters with at least one letter and one number.';
  if (confirm && f.password !== f.confirmPassword) e.confirmPassword = 'Passwords do not match.';
  return e;
}
