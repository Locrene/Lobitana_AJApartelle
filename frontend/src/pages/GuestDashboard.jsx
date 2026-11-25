import UpcomingModules from '../components/UpcomingModules';
import Welcome from '../components/Welcome';
import { dashboardService } from '../services/authService';
import useDashboard from './useDashboard';

export default function GuestDashboard() {
  const { data, error } = useDashboard(dashboardService.guest);
  if (error) return <p className="alert">{error}</p>;
  if (!data) return <p>Loading...</p>;
  return (
    <>
      <Welcome fullName={data.fullName} intro="This is your AJ Apartelle guest account."
        next={{ text: 'Check that your name and mobile number are up to date.', to: '/profile', label: 'Open my profile' }} />
      <section className="card">
        <h2 className="card-title">Your details</h2>
        <dl>
          <dt>Email</dt><dd>{data.email}</dd>
          <dt>Mobile number</dt><dd>{data.mobileNumber}</dd>
          <dt>Member since</dt><dd>{new Date(data.memberSince).toLocaleDateString()}</dd>
        </dl>
      </section>
      <UpcomingModules items={data.upcomingModules} />
    </>
  );
}
