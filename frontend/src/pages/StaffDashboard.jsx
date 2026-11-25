import UpcomingModules from '../components/UpcomingModules';
import Welcome from '../components/Welcome';
import { dashboardService } from '../services/authService';
import useDashboard from './useDashboard';

export default function StaffDashboard() {
  const { data, error } = useDashboard(dashboardService.staff);
  if (error) return <p className="alert">{error}</p>;
  if (!data) return <p>Loading...</p>;
  return (
    <>
      <Welcome fullName={data.fullName} intro="You're signed in to the front desk."
        next={{ text: 'Check that your name and mobile number are up to date, so the owner can reach you.', to: '/profile', label: 'Open my profile' }} />
      <section className="stats">
        <div className="stat"><strong>{data.activeStaff}</strong><span>Active staff accounts</span></div>
      </section>
      <UpcomingModules items={data.upcomingModules} />
    </>
  );
}
