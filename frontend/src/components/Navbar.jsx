import { useState, useEffect } from 'react';
import { NavLink, Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import client from '../api/client';
import Avatar from './Avatar';
import { Target, LogOut, Menu, X } from 'lucide-react';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const isAdmin = user?.role === 'ADMIN';
  const isCoach = user?.role === 'COACH' || isAdmin;
  const isPlayer = user?.role === 'PLAYER';
  const [menuOpen, setMenuOpen] = useState(false);
  const [pendingCount, setPendingCount] = useState(0);

  useEffect(() => { setMenuOpen(false); }, [location.pathname]);

  useEffect(() => {
    if (!isPlayer) return;
    client.get('/join-requests/pending')
      .then(r => setPendingCount(r.data.length))
      .catch(() => {});
  }, [location.pathname, isPlayer]);

  function handleLogout() {
    logout();
    navigate('/login');
  }

  const desktopLinkCls = ({ isActive }) =>
    `text-sm font-medium transition-colors pb-0.5 whitespace-nowrap inline-flex items-center gap-1.5 ${
      isActive ? 'text-accent border-b-2 border-accent' : 'text-muted hover:text-white'
    }`;

  const mobileLinkCls = ({ isActive }) =>
    `flex items-center gap-2 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
      isActive ? 'bg-accent/20 text-accent' : 'text-muted hover:text-white hover:bg-white/5'
    }`;

  const roleBadge = isAdmin
    ? 'bg-orange-500/20 text-orange-300 border-orange-500/30'
    : isPlayer
      ? 'bg-blue-500/20 text-blue-300 border-blue-500/30'
      : 'bg-accent/20 text-accent border-accent/30';
  const roleLabel = isAdmin ? 'Admin' : isPlayer ? 'Zawodnik' : 'Trener';

  const fullName = [user?.firstName, user?.lastName].filter(Boolean).join(' ');
  const displayName = fullName || user?.email;

  const dashboardTo = isAdmin ? '/admin/dashboard' : '/dashboard';

  const navLinks = [
    { to: dashboardTo, label: 'Dashboard', show: true },
    { to: '/trainings', label: 'Treningi', show: true },
    { to: '/calendar', label: 'Kalendarz', show: true },
    { to: '/players', label: 'Zawodnicy', show: isCoach },
    { to: '/coaches', label: 'Trenerzy', show: isAdmin },
    { to: '/finances', label: 'Finanse', show: isCoach },
    { to: '/reports', label: 'Raporty', show: isCoach },
    { to: '/invitations', label: 'Zaproszenia', show: isPlayer, badge: pendingCount },
    { to: '/profile', label: 'Profil', show: true, mobileOnly: true },
  ];

  const visibleLinks = navLinks.filter(l => l.show);
  const desktopLinks = visibleLinks.filter(l => !l.mobileOnly);

  return (
    <nav className="bg-surface border-b border-border sticky top-0 z-50">
      <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between gap-6">

        {/* Logo */}
        <Link to={dashboardTo} className="flex items-center gap-3 shrink-0 group">
          <div className="bg-accent/15 border border-accent/30 p-2 rounded-xl group-hover:bg-accent/25 transition-colors">
            <Target size={17} className="text-accent" />
          </div>
          <span className="font-bold tracking-tight leading-none">
            <span className="text-accent text-lg">TT</span><span className="text-white text-lg">Manager</span>
          </span>
        </Link>

        {/* Desktop links */}
        <div className="hidden md:flex items-center gap-5 flex-1 overflow-hidden">
          {desktopLinks.map(l => (
            <NavLink key={l.to} to={l.to} className={desktopLinkCls}>
              {l.label}
              {l.badge > 0 && (
                <span className="bg-red-500 text-white text-[10px] font-bold rounded-full min-w-[16px] h-4 flex items-center justify-center px-1 leading-none">
                  {l.badge}
                </span>
              )}
            </NavLink>
          ))}
        </div>

        {/* Desktop right */}
        <div className="hidden md:flex items-center gap-2 shrink-0">
          <Link
            to="/profile"
            className="flex items-center gap-2.5 pl-1 pr-2 py-1 rounded-lg hover:bg-white/5 transition-colors group"
          >
            <Avatar firstName={user?.firstName} lastName={user?.lastName} size="sm" />
            <span className="hidden lg:flex flex-col leading-tight">
              <span className="text-sm font-medium text-white group-hover:text-accent transition-colors">
                {displayName}
              </span>
              <span className="text-xs text-muted">{roleLabel}</span>
            </span>
          </Link>
          <button
            onClick={handleLogout}
            className="text-muted hover:text-white p-1.5 rounded-lg hover:bg-white/10 transition-colors"
            title="Wyloguj"
          >
            <LogOut size={16} />
          </button>
        </div>

        {/* Mobile: badge + hamburger */}
        <div className="flex md:hidden items-center gap-2 shrink-0">
          <span className={`text-xs font-semibold px-2.5 py-1 rounded-full border ${roleBadge}`}>
            {roleLabel}
          </span>
          <button
            onClick={() => setMenuOpen(o => !o)}
            className="relative text-muted hover:text-white p-1.5 rounded-lg hover:bg-white/10 transition-colors"
            aria-label="Otwórz menu"
          >
            {menuOpen ? <X size={20} /> : <Menu size={20} />}
            {pendingCount > 0 && !menuOpen && (
              <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full" />
            )}
          </button>
        </div>
      </div>

      {/* Mobile dropdown menu */}
      {menuOpen && (
        <div className="md:hidden border-t border-border bg-surface shadow-lg">
          <div className="max-w-6xl mx-auto px-4 py-3 space-y-0.5">
            {visibleLinks.map(l => (
              <NavLink key={l.to} to={l.to} className={mobileLinkCls}>
                {l.label}
                {l.badge > 0 && (
                  <span className="bg-red-500 text-white text-[10px] font-bold rounded-full min-w-[16px] h-4 flex items-center justify-center px-1 leading-none ml-auto">
                    {l.badge}
                  </span>
                )}
              </NavLink>
            ))}
            <div className="border-t border-border pt-3 mt-3 flex items-center justify-between px-3">
              <Link to="/profile" className="flex items-center gap-2.5 min-w-0 mr-4">
                <Avatar firstName={user?.firstName} lastName={user?.lastName} size="sm" />
                <span className="flex flex-col leading-tight min-w-0">
                  <span className="text-white text-sm font-medium truncate">{displayName}</span>
                  <span className="text-muted text-xs">{roleLabel}</span>
                </span>
              </Link>
              <button
                onClick={handleLogout}
                className="flex items-center gap-2 text-muted hover:text-white text-sm transition-colors shrink-0"
              >
                <LogOut size={14} />
                Wyloguj
              </button>
            </div>
          </div>
        </div>
      )}
    </nav>
  );
}
