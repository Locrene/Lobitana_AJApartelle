import { Navigate, Outlet } from 'react-router-dom';
import { DASHBOARD_BY_ROLE, useAuth } from '../context/AuthContext';

/** Login/Register pages: a signed-in user is sent to his or her own dashboard instead. */
export default function PublicOnlyRoute() {
  const { user, loading } = useAuth();
  if (loading) return <p className="center">Loading...</p>;
  if (user) return <Navigate to={DASHBOARD_BY_ROLE[user.role]} replace />;
  return <Outlet />;
}
