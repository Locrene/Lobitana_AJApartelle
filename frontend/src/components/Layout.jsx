import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Logo from './Logo';

// Navigation per role (SRS Appendix F). Only links to pages that exist and are allowed for the role.
const NAV = {
  GUEST: [{ to: '/guest', label: 'My Dashboard' }],
  STAFF: [{ to: '/staff', label: 'Staff Dashboard' }],
  OWNER: [
    { to: '/owner', label: 'Owner Dashboard' },
    { to: '/owner/staff', label: 'Staff Accounts' },
    { to: '/staff', label: 'Staff Workspace' },
  ],
};

const initials = (name = '') => name.split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0].toUpperCase()).join('');

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const onLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand"><Logo size={36} /><span>AJ Apartelle</span></div>
        <nav>
          {(NAV[user.role] || []).map((n) => (
            <NavLink key={n.to} to={n.to} end>{n.label}</NavLink>
          ))}
        </nav>
        <div className="who">
          <span className="avatar" aria-hidden="true">{initials(user.fullName)}</span>
          <span className="name">{user.fullName}</span>
          <span className="badge">{user.role}</span>
          <button className="small outline" onClick={onLogout}>Log out</button>
        </div>
      </header>
      <main className="content"><Outlet /></main>
    </div>
  );
}
