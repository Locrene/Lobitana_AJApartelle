import UpcomingModules from '../components/UpcomingModules';
import { dashboardService } from '../services/authService';
import useDashboard from './useDashboard';

export default function StaffDashboard() {
  const { data, error } = useDashboard(dashboardService.staff);
  if (error) return <p className="alert">{error}</p>;
  if (!data) return <p>Loading...</p>;
  return (
    <>
      <div className="page-head">
        <p className="eyebrow">Front desk</p>
        <h1>Staff Dashboard</h1>
        <p className="meta">Signed in as {data.fullName} ({data.role}).</p>
      </div>
      <section className="stats">
        <div className="stat"><strong>{data.activeStaff}</strong><span>Active staff accounts</span></div>
      </section>
      <UpcomingModules items={data.upcomingModules} />
    </>
  );
}
