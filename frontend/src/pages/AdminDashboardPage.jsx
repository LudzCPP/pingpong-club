import { useState, useEffect } from 'react';
import client from '../api/client';
import StatCard from '../components/StatCard';
import Avatar from '../components/Avatar';
import { Users, UserCheck, CalendarCheck, CalendarDays, Banknote, XCircle } from 'lucide-react';

function todayPolish() {
  return new Date().toLocaleDateString('pl-PL', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
}

export default function AdminDashboardPage() {
  const [stats, setStats] = useState(null);
  const [coaches, setCoaches] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      client.get('/admin/stats'),
      client.get('/users/coaches'),
    ]).then(([s, c]) => {
      setStats(s.data);
      setCoaches(c.data);
    }).finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <div className="max-w-6xl mx-auto px-6 py-12 text-center text-muted">Ładowanie...</div>;
  }

  return (
    <div className="max-w-6xl mx-auto px-6 py-8 space-y-8">

      {/* Hero */}
      <div className="bg-surface border border-border rounded-2xl px-8 py-6">
        <h1 className="text-3xl font-bold text-white">Panel administratora</h1>
        <p className="text-muted mt-1 capitalize">{todayPolish()}</p>
      </div>

      {/* Główne statystyki */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard icon={Users} value={stats.activeCoaches} label="Aktywni trenerzy" />
        <StatCard icon={UserCheck} value={stats.activePlayers} label="Aktywni zawodnicy" />
        <StatCard icon={CalendarCheck} value={stats.completedTrainings} label="Treningi ukończone" />
        <StatCard icon={Banknote} value={`${Number(stats.totalEarnings).toFixed(0)} zł`} label="Łączne przychody" accent />
      </div>

      {/* Dodatkowe statystyki */}
      <div className="grid grid-cols-3 gap-4">
        <StatCard icon={CalendarDays} value={stats.scheduledTrainings} label="Zaplanowane treningi" />
        <StatCard icon={XCircle} value={stats.cancelledTrainings} label="Odwołane treningi" />
        <StatCard icon={Users} value={stats.totalPlayers} label="Wszyscy zawodnicy" />
      </div>

      {/* Lista trenerów */}
      <div className="bg-surface border border-border rounded-xl p-6">
        <h2 className="text-white font-semibold text-lg mb-4 flex items-center gap-2">
          <Users size={18} className="text-orange-400" />
          Trenerzy
          <span className="text-xs bg-orange-500/20 text-orange-300 px-2 py-0.5 rounded-full ml-1">{coaches.length}</span>
        </h2>
        {coaches.length === 0 ? (
          <p className="text-muted text-sm py-4 text-center">Brak trenerów</p>
        ) : (
          <div className="space-y-1">
            {coaches.map(c => (
              <div key={c.id} className="flex items-center gap-4 py-3 border-b border-border last:border-0">
                <Avatar firstName={c.firstName} lastName={c.lastName} size="sm" />
                <div className="flex-1 min-w-0">
                  <p className="text-white font-medium">{c.firstName} {c.lastName}</p>
                  <p className="text-muted text-sm truncate">{c.email}</p>
                </div>
                <span className={`text-xs font-semibold px-2.5 py-1 rounded-full border ${
                  c.active
                    ? 'bg-accent/20 text-accent border-accent/30'
                    : 'bg-slate-500/20 text-slate-400 border-slate-500/30'
                }`}>
                  {c.active ? 'Aktywny' : 'Nieaktywny'}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
