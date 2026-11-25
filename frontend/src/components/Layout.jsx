import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Icon from './Icon';
import Logo from './Logo';

// Navigation per role (SRS Appendix F). Only links to pages that exist and are allowed for the role.
const PROFILE = { to: '/profile', label: 'My Profile', icon: 'user' };
const NAV = {
  GUEST: [{ to: '/guest', label: 'My Dashboard', icon: 'dashboard' }, PROFILE],
  STAFF: [{ to: '/staff', label: 'Staff Dashboard', icon: 'desk' }, PROFILE],
  OWNER: [
    { to: '/owner', label: 'Owner Dashboard', icon: 'dashboard' },
    { to: '/owner/staff', label: 'Staff Accounts', icon: 'users' },
    { to: '/staff', label: 'Staff Workspace', icon: 'desk' },
    PROFILE,
  ],
};

const ROLE_LABEL = { GUEST: 'Guest', STAFF: 'Front desk', OWNER: 'Owner' };

const initials = (name = '') => name.split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0].toUpperCase()).join('');

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const onLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className={user.role === 'OWNER' ? 'app with-side' : 'app'}>
      <header className="topbar">
        <div className="brand"><Logo size={36} /><span className="wordmark">AJ Apartelle</span></div>
        <nav aria-label="Main">
          {(NAV[user.role] || []).map((n) => (
            <NavLink key={n.to} to={n.to} end><Icon name={n.icon} />{n.label}</NavLink>
          ))}
        </nav>
        <div className="who">
          <span className="avatar" aria-hidden="true">{initials(user.fullName)}</span>
          <span className="name">{user.fullName}</span>
          <span className="badge">{ROLE_LABEL[user.role] || user.role}</span>
          <button onClick={onLogout}><Icon name="logout" />Log out</button>
        </div>
      </header>
      <main className="content"><Outlet /></main>
    </div>
  );
}
