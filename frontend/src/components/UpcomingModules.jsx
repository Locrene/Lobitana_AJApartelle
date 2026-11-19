export default function UpcomingModules({ items }) {
  if (!items?.length) return null;
  return (
    <section className="card muted">
      <h3>Coming in later increments</h3>
      <ul>{items.map((i) => <li key={i}>{i}</li>)}</ul>
    </section>
  );
}
