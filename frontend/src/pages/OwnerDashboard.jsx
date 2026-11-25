import UpcomingModules from '../components/UpcomingModules';
import Welcome from '../components/Welcome';
import { dashboardService } from '../services/authService';
import useDashboard from './useDashboard';

export default function OwnerDashboard() {
  const { data, error } = useDashboard(dashboardService.owner);
  if (error) return <p className="alert">{error}</p>;
  if (!data) return <p>Loading...</p>;
  const a = data.accounts;
  return (
    <>
      <Welcome fullName={data.fullName} intro="Here is who can use the AJ Apartelle app today."
        next={{ text: 'Add front-desk staff, or turn off an account that should no longer sign in.', to: '/owner/staff', label: 'Manage staff accounts' }} />
      <section className="stats">
        <div className="stat"><strong>{a.guests}</strong><span>Guest accounts</span></div>
        <div className="stat"><strong>{a.activeStaff} / {a.staff}</strong><span>Active staff</span></div>
        <div className="stat"><strong>{a.owners}</strong><span>Owner accounts</span></div>
      </section>
      <UpcomingModules items={data.upcomingModules} />
    </>
  );
}
