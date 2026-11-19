/** AJ Apartelle house mark, from the AJ Apartelle prototype design. */
export default function Logo({ size = 40 }) {
  return (
    <span className="logo" style={{ width: size, height: size, borderRadius: size * 0.27 }} aria-hidden="true">
      <svg width={size * 0.46} height={size * 0.46} viewBox="0 0 48 48" fill="none" stroke="currentColor"
        strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round">
        <path d="M6 22 24 8l18 14" /><path d="M11 19v21h26V19" /><path d="M20 40V29h8v11" />
      </svg>
      {size >= 64 && <span className="logo-text">AJ</span>}
    </span>
  );
}
