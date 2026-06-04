import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <nav className="navbar">
      <div className="navbar-brand">🏓 PingPong Club</div>
      <div className="navbar-links">
        <Link to="/trainings">Treningi</Link>
        {user?.role === 'COACH' && <Link to="/players">Zawodnicy</Link>}
        <Link to="/finances">Finanse</Link>
      </div>
      <div className="navbar-user">
        <span className={`role-badge ${user?.role === 'COACH' ? 'coach' : 'player'}`}>
          {user?.role === 'COACH' ? 'Trener' : 'Zawodnik'}
        </span>
        <span className="user-email">{user?.email}</span>
        <button onClick={handleLogout} className="btn btn-sm btn-outline">Wyloguj</button>
      </div>
    </nav>
  );
}
