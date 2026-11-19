export default function UpcomingModules({ items }) {
  if (!items?.length) return null;
  return (
    <section className="card muted">
      <p className="eyebrow">Coming in later increments</p>
      <ul>{items.map((i) => <li key={i}>{i}</li>)}</ul>
    </section>
  );
}
