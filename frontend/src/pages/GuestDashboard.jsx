import UpcomingModules from '../components/UpcomingModules';
import { dashboardService } from '../services/authService';
import useDashboard from './useDashboard';

export default function GuestDashboard() {
  const { data, error } = useDashboard(dashboardService.guest);
  if (error) return <p className="alert">{error}</p>;
  if (!data) return <p>Loading...</p>;
  return (
    <>
      <h1>Welcome, {data.fullName}</h1>
      <section className="card">
        <h3>My profile</h3>
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
