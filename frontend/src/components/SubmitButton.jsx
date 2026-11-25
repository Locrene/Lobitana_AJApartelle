/** A submit button whose label stays readable while the request runs. */
export default function SubmitButton({ busy, busyLabel, children }) {
  return <button type="submit" disabled={busy} aria-busy={busy}>{busy ? busyLabel : children}</button>;
}
