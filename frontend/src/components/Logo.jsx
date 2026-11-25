/** AJ Apartelle mark from the prototype: key fobs on a rack. Decorative only; no room numbers, so it never reads as data. */
const RACK = ['AJ', '', '', ''];

export default function Logo({ size = 40 }) {
  if (size >= 56) {
    return <span className="rack" aria-hidden="true">{RACK.map((label, i) => <span key={i} className="tag">{label}</span>)}</span>;
  }
  return <span className="tag sm" aria-hidden="true">AJ</span>;
}
