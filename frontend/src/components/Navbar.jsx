import { useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Target, LogOut, KeyRound } from 'lucide-react';
import ChangePasswordModal from './ChangePasswordModal';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const isAdmin = user?.role === 'ADMIN';
  const isCoach = user?.role === 'COACH' || isAdmin;
  const isPlayer = user?.role === 'PLAYER';
  const [showChangePassword, setShowChangePassword] = useState(false);

  function handleLogout() {
    logout();
    navigate('/login');
  }

  const linkCls = ({ isActive }) =>
    `text-sm font-medium transition-colors pb-0.5 ${
      isActive
        ? 'text-accent border-b-2 border-accent'
        : 'text-muted hover:text-white'
    }`;

  const roleBadge = isAdmin
    ? 'bg-orange-500/20 text-orange-300 border-orange-500/30'
    : isPlayer
      ? 'bg-blue-500/20 text-blue-300 border-blue-500/30'
      : 'bg-accent/20 text-accent border-accent/30';

  const roleLabel = isAdmin ? 'Admin' : isPlayer ? 'Zawodnik' : 'Trener';

  return (
    <>
      {showChangePassword && (
        <ChangePasswordModal onClose={() => setShowChangePassword(false)} />
      )}

      <nav className="bg-surface border-b border-border sticky top-0 z-50">
        <div className="max-w-6xl mx-auto px-6 h-14 flex items-center gap-8">
          <span className="text-white font-bold text-lg whitespace-nowrap flex items-center gap-2">
            <Target size={18} className="text-accent" />
            TTManager
          </span>

          <div className="flex items-center gap-6 flex-1">
            <NavLink to="/dashboard" className={linkCls}>Dashboard</NavLink>
            <NavLink to="/trainings" className={linkCls}>Treningi</NavLink>
            <NavLink to="/calendar" className={linkCls}>Kalendarz</NavLink>
            {isCoach && <NavLink to="/players" className={linkCls}>Zawodnicy</NavLink>}
            {isAdmin && <NavLink to="/coaches" className={linkCls}>Trenerzy</NavLink>}
            {isCoach && <NavLink to="/finances" className={linkCls}>Finanse</NavLink>}
            {isCoach && <NavLink to="/reports" className={linkCls}>Raporty</NavLink>}
            {isPlayer && <NavLink to="/invitations" className={linkCls}>Zaproszenia</NavLink>}
          </div>

          <div className="flex items-center gap-3">
            <span className={`text-xs font-semibold px-2.5 py-1 rounded-full border ${roleBadge}`}>
              {roleLabel}
            </span>
            <span className="text-sm text-muted hidden sm:block">{user?.email}</span>
            <button
              onClick={() => setShowChangePassword(true)}
              className="text-muted hover:text-white p-1.5 rounded-lg hover:bg-white/10 transition-colors"
              title="Zmień hasło"
            >
              <KeyRound size={16} />
            </button>
            <button
              onClick={handleLogout}
              className="text-muted hover:text-white p-1.5 rounded-lg hover:bg-white/10 transition-colors"
              title="Wyloguj"
            >
              <LogOut size={16} />
            </button>
          </div>
        </div>
      </nav>
    </>
  );
}
