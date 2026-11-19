import { Link } from 'react-router-dom';
import { DASHBOARD_BY_ROLE, useAuth } from '../context/AuthContext';
import Logo from '../components/Logo';

export default function UnauthorizedPage() {
  const { user } = useAuth();
  return (
    <div className="auth-page">
      <div className="card auth-card">
        <div className="auth-brand"><Logo size={56} /></div>
        <h2>403 - Access denied</h2>
        <p>You do not have permission to view this page.</p>
        <Link to={user ? DASHBOARD_BY_ROLE[user.role] : '/login'}>Go to my dashboard</Link>
      </div>
    </div>
  );
}
