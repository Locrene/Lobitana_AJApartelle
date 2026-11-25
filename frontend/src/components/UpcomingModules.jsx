export default function UpcomingModules({ items }) {
  if (!items?.length) return null;
  return (
    <section className="card muted">
      <h2 className="card-title">Coming in later increments</h2>
      <ul>{items.map((i) => <li key={i}>{i}</li>)}</ul>
    </section>
  );
}
