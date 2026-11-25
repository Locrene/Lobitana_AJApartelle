import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import { DASHBOARD_BY_ROLE, useAuth } from './context/AuthContext';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import GuestDashboard from './pages/GuestDashboard';
import LoginPage from './pages/LoginPage';
import OwnerDashboard from './pages/OwnerDashboard';
import ProfilePage from './pages/ProfilePage';
import RegisterPage from './pages/RegisterPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import StaffAccountsPage from './pages/StaffAccountsPage';
import StaffDashboard from './pages/StaffDashboard';
import UnauthorizedPage from './pages/UnauthorizedPage';
import ProtectedRoute from './routes/ProtectedRoute';
import PublicOnlyRoute from './routes/PublicOnlyRoute';

function Home() {
  const { user, loading } = useAuth();
  if (loading) return <p className="center">Loading...</p>;
  return <Navigate to={user ? DASHBOARD_BY_ROLE[user.role] : '/login'} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route element={<PublicOnlyRoute />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>
      <Route path="/unauthorized" element={<UnauthorizedPage />} />
      {/* Not public-only: an emailed link must also open while someone is signed in. */}
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/profile" element={<ProfilePage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute roles={['GUEST']} />}>
        <Route element={<Layout />}>
          <Route path="/guest" element={<GuestDashboard />} />
        </Route>
      </Route>
      <Route element={<ProtectedRoute roles={['STAFF', 'OWNER']} />}>
        <Route element={<Layout />}>
          <Route path="/staff" element={<StaffDashboard />} />
        </Route>
      </Route>
      <Route element={<ProtectedRoute roles={['OWNER']} />}>
        <Route element={<Layout />}>
          <Route path="/owner" element={<OwnerDashboard />} />
          <Route path="/owner/staff" element={<StaffAccountsPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Home />} />
    </Routes>
  );
}
