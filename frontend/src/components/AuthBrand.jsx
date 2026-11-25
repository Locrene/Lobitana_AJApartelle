import Logo from './Logo';

/** The welcome panel beside the sign-in, register and password forms: the key rack and a short hello. */
export default function AuthBrand() {
  return (
    <aside className="auth-brand">
      <Logo size={72} />
      <div className="auth-brand-text">
        <h1>AJ Apartelle</h1>
        <p>Welcome. We&apos;re a small, family-run apartelle in Cebu with clean, quiet rooms.</p>
      </div>
    </aside>
  );
}
