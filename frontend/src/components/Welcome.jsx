import { Link } from 'react-router-dom';

const TZ = 'Asia/Manila';

function greeting(now) {
  const hour = Number(new Intl.DateTimeFormat('en-US', { timeZone: TZ, hour: 'numeric', hourCycle: 'h23' }).format(now));
  if (hour < 12) return 'Good morning';
  if (hour < 18) return 'Good afternoon';
  return 'Good evening';
}

/** Dashboard greeting: today's date in Manila, the person's first name, and one next step to a page that exists. */
export default function Welcome({ fullName, intro, next }) {
  const now = new Date();
  const date = new Intl.DateTimeFormat('en-PH', { timeZone: TZ, weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' }).format(now);
  const first = (fullName || '').trim().split(/\s+/)[0];

  return (
    <section className="welcome">
      <div className="welcome-text">
        <h1>{greeting(now)}{first ? `, ${first}` : ''}</h1>
        <p className="welcome-date">{date}</p>
        <p className="welcome-intro">{intro}</p>
      </div>
      <div className="next-step">
        <h2>Next step</h2>
        <p>{next.text}</p>
        <Link className="button-link" to={next.to}>{next.label}</Link>
      </div>
    </section>
  );
}
