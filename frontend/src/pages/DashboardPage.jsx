import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import client from '../api/client';
import StatCard from '../components/StatCard';
import StatusBadge from '../components/StatusBadge';
import Avatar from '../components/Avatar';
import { useAuth } from '../context/AuthContext';
import { CalendarCheck, CalendarDays, Banknote, Users, Check, X, ClipboardList } from 'lucide-react';

function todayStr() {
  return new Date().toISOString().split('T')[0];
}
function monthStart() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`;
}
function weekStart() {
  const d = new Date();
  d.setDate(d.getDate() - d.getDay() + 1);
  return d.toISOString().split('T')[0];
}
function formatDate(iso) {
  return new Date(iso).toLocaleDateString('pl-PL', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
}
function todayPolish() {
  return new Date().toLocaleDateString('pl-PL', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
}

export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const isAdmin = user?.role === 'ADMIN';
  const isCoach = user?.role === 'COACH';

  useEffect(() => {
    if (isAdmin) navigate('/admin/dashboard', { replace: true });
  }, [isAdmin, navigate]);

  const [trainings, setTrainings] = useState([]);
  const [players, setPlayers] = useState([]);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const today = todayStr();
    const ms = monthStart();
    Promise.all([
      client.get('/trainings'),
      isCoach ? client.get('/users/players') : Promise.resolve({ data: [] }),
      client.get(`/finances/summary?from=${ms}&to=${today}`),
    ]).then(([t, p, s]) => {
      setTrainings(t.data);
      setPlayers(p.data);
      setSummary(s.data);
    }).finally(() => setLoading(false));
  }, [isCoach]);

  const todayTrainings = trainings.filter(t => {
    const d = t.scheduledAt?.split('T')[0];
    return d === todayStr() && t.status === 'SCHEDULED';
  });
  const weekTrainings = trainings.filter(t => {
    const d = t.scheduledAt?.split('T')[0];
    return d >= weekStart() && t.status === 'SCHEDULED';
  });
  const recent = [...trainings]
    .sort((a, b) => new Date(b.scheduledAt) - new Date(a.scheduledAt))
    .slice(0, 5);

  async function handleComplete(id) {
    await client.patch(`/trainings/${id}/complete`);
    setTrainings(ts => ts.map(t => t.id === id ? { ...t, status: 'COMPLETED' } : t));
  }
  async function handleCancel(id) {
    if (!confirm('Odwołać trening?')) return;
    await client.patch(`/trainings/${id}/cancel`);
    setTrainings(ts => ts.map(t => t.id === id ? { ...t, status: 'CANCELLED' } : t));
  }

  const firstName = isCoach ? 'Trenerze' : (user?.email?.split('@')[0] ?? '');

  if (loading) {
    return (
      <div className="max-w-6xl mx-auto px-6 py-12 text-center text-muted">
        Ładowanie dashboardu...
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto px-6 py-8 space-y-8">

      {/* Hero */}
      <div className="bg-surface border border-border rounded-2xl px-6 py-5 flex items-center justify-between">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-white">Dzień dobry, {firstName}</h1>
          <p className="text-muted mt-1 capitalize text-sm sm:text-base">{todayPolish()}</p>
        </div>
        {isCoach && (
          <button
            onClick={() => navigate('/trainings')}
            className="bg-accent hover:bg-green-600 text-white font-semibold px-5 py-2.5 rounded-lg transition-colors hidden sm:block"
          >
            + Nowy trening
          </button>
        )}
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard icon={CalendarCheck} value={todayTrainings.length} label="Treningi dziś" />
        <StatCard icon={CalendarDays} value={weekTrainings.length} label="W tym tygodniu" />
        {isCoach && (
          <StatCard
            icon={Banknote}
            value={`${Number(summary?.grandTotal ?? 0).toFixed(0)} zł`}
            label="Zarobki w tym miesiącu"
            accent
          />
        )}
        {isCoach && (
          <StatCard
            icon={Users}
            value={players.filter(p => p.active).length}
            label="Aktywni zawodnicy"
          />
        )}
      </div>

      <div className="grid lg:grid-cols-2 gap-6">
        {/* Dziś na korcie */}
        <div className="bg-surface border border-border rounded-xl p-6">
          <h2 className="text-white font-semibold text-lg mb-4 flex items-center gap-2">
            <CalendarCheck size={18} className="text-accent" />
            Dziś na korcie
            <span className="text-xs bg-accent/20 text-accent px-2 py-0.5 rounded-full ml-1">{todayTrainings.length}</span>
          </h2>
          {todayTrainings.length === 0 ? (
            <p className="text-muted text-sm py-4 text-center">Brak treningów na dziś</p>
          ) : (
            <ul className="space-y-3">
              {todayTrainings.map(t => (
                <li key={t.id} className="flex items-center gap-3 bg-base rounded-lg px-4 py-3">
                  <Avatar firstName={t.playerFullName?.split(' ')[0]} lastName={t.playerFullName?.split(' ')[1]} size="sm" />
                  <div className="flex-1 min-w-0">
                    <p className="text-white text-sm font-medium truncate">{t.playerFullName}</p>
                    <p className="text-muted text-xs">
                      {new Date(t.scheduledAt).toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit' })} · {t.durationMinutes} min
                    </p>
                  </div>
                  {isCoach && (
                    <div className="flex gap-1">
                      <button onClick={() => handleComplete(t.id)} title="Zakończ"
                        className="text-green-400 hover:bg-green-400/10 p-1.5 rounded transition-colors">
                        <Check size={14} />
                      </button>
                      <button onClick={() => handleCancel(t.id)} title="Odwołaj"
                        className="text-red-400 hover:bg-red-400/10 p-1.5 rounded transition-colors">
                        <X size={14} />
                      </button>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* Ostatnie treningi */}
        <div className="bg-surface border border-border rounded-xl p-6">
          <h2 className="text-white font-semibold text-lg mb-4 flex items-center gap-2">
            <ClipboardList size={18} className="text-slate-400" />
            Ostatnie treningi
          </h2>
          {recent.length === 0 ? (
            <p className="text-muted text-sm py-4 text-center">Brak treningów</p>
          ) : (
            <ul className="space-y-2">
              {recent.map(t => (
                <li key={t.id} className="flex items-center gap-3 py-2 border-b border-border last:border-0">
                  <Avatar firstName={t.playerFullName?.split(' ')[0]} lastName={t.playerFullName?.split(' ')[1]} size="sm" />
                  <div className="flex-1 min-w-0">
                    <p className="text-white text-sm truncate">{t.name}</p>
                    <p className="text-muted text-xs">{formatDate(t.scheduledAt)}</p>
                  </div>
                  <StatusBadge status={t.status} />
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
