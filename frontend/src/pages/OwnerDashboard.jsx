import { Link } from 'react-router-dom';
import UpcomingModules from '../components/UpcomingModules';
import { dashboardService } from '../services/authService';
import useDashboard from './useDashboard';

export default function OwnerDashboard() {
  const { data, error } = useDashboard(dashboardService.owner);
  if (error) return <p className="alert">{error}</p>;
  if (!data) return <p>Loading...</p>;
  const a = data.accounts;
  return (
    <>
      <div className="page-head">
        <p className="eyebrow">Owner</p>
        <h1>Owner Dashboard</h1>
        <p className="meta">Welcome, {data.fullName}.</p>
      </div>
      <section className="stats">
        <div className="stat"><strong>{a.guests}</strong><span>Guest accounts</span></div>
        <div className="stat"><strong>{a.activeStaff} / {a.staff}</strong><span>Active staff</span></div>
        <div className="stat"><strong>{a.owners}</strong><span>Owner accounts</span></div>
      </section>
      <Link className="linkrow" to="/owner/staff">Manage staff accounts &rarr;</Link>
      <UpcomingModules items={data.upcomingModules} />
    </>
  );
}
