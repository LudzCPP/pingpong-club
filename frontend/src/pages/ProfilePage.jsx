import { useState, useEffect } from 'react';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';
import Avatar from '../components/Avatar';
import StatCard from '../components/StatCard';
import ChangePasswordModal from '../components/ChangePasswordModal';
import { Users, CalendarCheck, Banknote, Pencil, X, Check, KeyRound, ShieldCheck } from 'lucide-react';

function todayStr() {
  return new Date().toISOString().split('T')[0];
}

const inputCls = 'w-full bg-base border border-border rounded-lg px-3 py-2.5 text-white text-sm placeholder-muted focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent transition-colors';

export default function ProfilePage() {
  const { user: authUser, updateUser } = useAuth();
  const isCoach = authUser?.role === 'COACH';

  const [profile, setProfile] = useState(null);
  const [players, setPlayers] = useState([]);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);

  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({ firstName: '', lastName: '' });
  const [saveLoading, setSaveLoading] = useState(false);
  const [saveError, setSaveError] = useState('');

  const [showChangePassword, setShowChangePassword] = useState(false);

  useEffect(() => {
    const today = todayStr();
    const requests = [
      client.get('/users/me'),
      client.get(`/finances/summary?from=2020-01-01&to=${today}`),
    ];
    if (isCoach) requests.push(client.get('/users/players'));

    Promise.all(requests).then(([p, s, pl]) => {
      setProfile(p.data);
      setSummary(s.data);
      if (pl) setPlayers(pl.data);
    }).finally(() => setLoading(false));
  }, [isCoach]);

  function startEdit() {
    setForm({ firstName: profile.firstName, lastName: profile.lastName });
    setSaveError('');
    setEditing(true);
  }

  async function handleSave(e) {
    e.preventDefault();
    if (!form.firstName.trim() || !form.lastName.trim()) {
      setSaveError('Imię i nazwisko nie mogą być puste.');
      return;
    }
    setSaveLoading(true);
    setSaveError('');
    try {
      const { data } = await client.patch('/users/me', {
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
      });
      setProfile(data);
      updateUser({ firstName: data.firstName });
      setEditing(false);
    } catch (err) {
      setSaveError(err.response?.data?.detail || 'Nie udało się zapisać zmian.');
    } finally {
      setSaveLoading(false);
    }
  }

  const roleLabel = { ADMIN: 'Administrator', COACH: 'Trener', PLAYER: 'Zawodnik' }[authUser?.role] ?? '';
  const roleBadge = {
    ADMIN: 'bg-orange-500/20 text-orange-300 border-orange-500/30',
    COACH: 'bg-accent/20 text-accent border-accent/30',
    PLAYER: 'bg-blue-500/20 text-blue-300 border-blue-500/30',
  }[authUser?.role] ?? '';

  if (loading) {
    return <div className="max-w-3xl mx-auto px-6 py-12 text-center text-muted">Ładowanie...</div>;
  }

  const activePlayers = players.filter(p => p.active).length;

  return (
    <>
      {showChangePassword && <ChangePasswordModal onClose={() => setShowChangePassword(false)} />}

      <div className="max-w-3xl mx-auto px-6 py-8 space-y-6">

        {/* Hero */}
        <div className="bg-surface border border-border rounded-2xl px-6 py-6 flex items-center gap-5">
          <Avatar firstName={profile.firstName} lastName={profile.lastName} size="xl" />
          <div className="min-w-0">
            <h1 className="text-2xl font-bold text-white">{profile.firstName} {profile.lastName}</h1>
            <p className="text-muted mt-0.5">{profile.email}</p>
            <span className={`inline-block mt-2 text-xs font-semibold px-2.5 py-1 rounded-full border ${roleBadge}`}>
              {roleLabel}
            </span>
          </div>
        </div>

        {/* Stat cards — tylko dla COACH */}
        {isCoach && (
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <StatCard icon={Users} value={activePlayers} label="Aktywni zawodnicy" />
            <StatCard icon={CalendarCheck} value={summary?.completedTrainingsCount ?? 0} label="Treningi ukończone" />
            <StatCard
              icon={Banknote}
              value={`${Number(summary?.grandTotal ?? 0).toFixed(0)} zł`}
              label="Łączne zarobki"
              accent
            />
          </div>
        )}

        {/* Dane osobowe */}
        <div className="bg-surface border border-border rounded-xl p-6">
          <div className="flex items-center justify-between mb-5">
            <h2 className="text-white font-semibold">Dane osobowe</h2>
            {!editing && (
              <button
                onClick={startEdit}
                className="flex items-center gap-1.5 text-sm text-muted hover:text-white transition-colors px-3 py-1.5 rounded-lg hover:bg-white/5"
              >
                <Pencil size={13} />
                Edytuj
              </button>
            )}
          </div>

          {editing ? (
            <form onSubmit={handleSave} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <label className="text-xs font-medium text-muted uppercase tracking-wide">Imię</label>
                  <input
                    type="text"
                    value={form.firstName}
                    onChange={e => setForm(f => ({ ...f, firstName: e.target.value }))}
                    maxLength={50}
                    autoFocus
                    className={inputCls}
                  />
                </div>
                <div className="space-y-1.5">
                  <label className="text-xs font-medium text-muted uppercase tracking-wide">Nazwisko</label>
                  <input
                    type="text"
                    value={form.lastName}
                    onChange={e => setForm(f => ({ ...f, lastName: e.target.value }))}
                    maxLength={50}
                    className={inputCls}
                  />
                </div>
              </div>
              <div className="space-y-1.5">
                <label className="text-xs font-medium text-muted uppercase tracking-wide">Email</label>
                <input
                  type="text"
                  value={profile.email}
                  disabled
                  className={`${inputCls} opacity-50 cursor-not-allowed`}
                />
                <p className="text-xs text-muted">Adres email nie może być zmieniony.</p>
              </div>
              {saveError && (
                <div className="bg-red-500/10 border border-red-500/30 text-red-400 text-sm px-4 py-3 rounded-lg">
                  {saveError}
                </div>
              )}
              <div className="flex gap-3 pt-1">
                <button
                  type="submit"
                  disabled={saveLoading}
                  className="flex items-center gap-1.5 bg-accent hover:bg-green-600 disabled:opacity-50 text-white font-semibold px-4 py-2 rounded-lg transition-colors text-sm"
                >
                  <Check size={14} />
                  {saveLoading ? 'Zapisywanie...' : 'Zapisz'}
                </button>
                <button
                  type="button"
                  onClick={() => setEditing(false)}
                  className="flex items-center gap-1.5 text-sm text-muted hover:text-white px-4 py-2 rounded-lg hover:bg-white/5 transition-colors"
                >
                  <X size={14} />
                  Anuluj
                </button>
              </div>
            </form>
          ) : (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-xs font-medium text-muted uppercase tracking-wide mb-1">Imię</p>
                  <p className="text-white">{profile.firstName}</p>
                </div>
                <div>
                  <p className="text-xs font-medium text-muted uppercase tracking-wide mb-1">Nazwisko</p>
                  <p className="text-white">{profile.lastName}</p>
                </div>
              </div>
              <div>
                <p className="text-xs font-medium text-muted uppercase tracking-wide mb-1">Email</p>
                <p className="text-white">{profile.email}</p>
              </div>
            </div>
          )}
        </div>

        {/* Bezpieczeństwo */}
        <div className="bg-surface border border-border rounded-xl p-6">
          <div className="flex items-center gap-2 mb-5">
            <ShieldCheck size={16} className="text-muted" />
            <h2 className="text-white font-semibold">Bezpieczeństwo</h2>
          </div>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-white text-sm font-medium">Hasło</p>
              <p className="text-muted text-sm mt-0.5">Zmień hasło do swojego konta</p>
            </div>
            <button
              onClick={() => setShowChangePassword(true)}
              className="flex items-center gap-2 text-sm font-medium text-muted hover:text-white bg-base hover:bg-white/10 border border-border px-4 py-2 rounded-lg transition-colors"
            >
              <KeyRound size={14} />
              Zmień hasło
            </button>
          </div>
        </div>

      </div>
    </>
  );
}
