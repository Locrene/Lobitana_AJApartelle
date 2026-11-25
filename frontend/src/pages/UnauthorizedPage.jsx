import { Link } from 'react-router-dom';
import AuthBrand from '../components/AuthBrand';
import { DASHBOARD_BY_ROLE, useAuth } from '../context/AuthContext';

export default function UnauthorizedPage() {
  const { user } = useAuth();
  return (
    <main className="auth-page">
      <AuthBrand />
      <div className="card auth-card">
        <div>
          <h2 className="auth-title">Access denied (403)</h2>
          <p className="auth-intro">Your account can&apos;t open this page.</p>
        </div>
        <Link className="button-link" to={user ? DASHBOARD_BY_ROLE[user.role] : '/login'}>Go to my dashboard</Link>
      </div>
    </main>
  );
}
