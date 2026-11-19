import UpcomingModules from '../components/UpcomingModules';
import { dashboardService } from '../services/authService';
import useDashboard from './useDashboard';

export default function StaffDashboard() {
  const { data, error } = useDashboard(dashboardService.staff);
  if (error) return <p className="alert">{error}</p>;
  if (!data) return <p>Loading...</p>;
  return (
    <>
      <h1>Staff Dashboard</h1>
      <p>Signed in as {data.fullName} ({data.role}).</p>
      <section className="card stats">
        <div><strong>{data.activeStaff}</strong><span>Active staff accounts</span></div>
      </section>
      <UpcomingModules items={data.upcomingModules} />
    </>
  );
}
