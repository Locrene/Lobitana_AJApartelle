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
      <h1>Owner Dashboard</h1>
      <p>Welcome, {data.fullName}.</p>
      <section className="card stats">
        <div><strong>{a.guests}</strong><span>Guest accounts</span></div>
        <div><strong>{a.activeStaff} / {a.staff}</strong><span>Active staff</span></div>
        <div><strong>{a.owners}</strong><span>Owner accounts</span></div>
      </section>
      <p><Link to="/owner/staff">Manage staff accounts &rarr;</Link></p>
      <UpcomingModules items={data.upcomingModules} />
    </>
  );
}
