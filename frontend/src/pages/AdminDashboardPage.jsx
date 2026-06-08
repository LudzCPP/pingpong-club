import { useState, useEffect, useCallback } from 'react';
import client from '../api/client';
import StatCard from '../components/StatCard';
import Avatar from '../components/Avatar';
import ConfirmDialog from '../components/ConfirmDialog';
import { Users, UserCheck, CalendarCheck, CalendarDays, Banknote, XCircle, Link, Copy, Check, UserX, UserPlus, Ghost } from 'lucide-react';

function todayPolish() {
  return new Date().toLocaleDateString('pl-PL', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
}

export default function AdminDashboardPage() {
  const [activeTab, setActiveTab] = useState('overview');
  const [stats, setStats] = useState(null);
  const [players, setPlayers] = useState([]);
  const [loading, setLoading] = useState(true);

  const [invite, setInvite] = useState(null);
  const [inviteLoading, setInviteLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  const [confirmAction, setConfirmAction] = useState(null); // { user, action: 'deactivate'|'activate' }

  const fetchAll = useCallback(async () => {
    const [s, p] = await Promise.all([
      client.get('/admin/stats'),
      client.get('/users/players'),
    ]);
    setStats(s.data);
    setPlayers(p.data);
  }, []);

  useEffect(() => {
    fetchAll().finally(() => setLoading(false));
  }, [fetchAll]);

  async function handleGenerateInvite() {
    setInviteLoading(true);
    setInvite(null);
    try {
      const { data } = await client.post('/auth/invite/coaches');
      setInvite(data);
      setCopied(false);
    } finally {
      setInviteLoading(false);
    }
  }

  function handleCopy() {
    navigator.clipboard.writeText(invite.inviteUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  async function handleConfirmAction() {
    const { user, action } = confirmAction;
    if (action === 'deactivate') {
      await client.delete(`/users/${user.id}`);
    } else {
      await client.patch(`/users/${user.id}/activate`);
    }
    setConfirmAction(null);
    await fetchAll();
  }

  const tabs = [
    { id: 'overview', label: 'Przegląd' },
    { id: 'coaches', label: `Trenerzy${stats ? ` (${stats.totalCoaches})` : ''}` },
    { id: 'players', label: `Zawodnicy${stats ? ` (${stats.totalPlayers})` : ''}` },
  ];

  if (loading) {
    return <div className="max-w-6xl mx-auto px-6 py-12 text-center text-muted">Ładowanie...</div>;
  }

  return (
    <>
      {confirmAction && (
        <ConfirmDialog
          message={
            confirmAction.action === 'deactivate'
              ? `Dezaktywować konto ${confirmAction.user.firstName} ${confirmAction.user.lastName}?`
              : `Reaktywować konto ${confirmAction.user.firstName} ${confirmAction.user.lastName}?`
          }
          confirmLabel={confirmAction.action === 'deactivate' ? 'Dezaktywuj' : 'Reaktywuj'}
          onConfirm={handleConfirmAction}
          onCancel={() => setConfirmAction(null)}
        />
      )}

      <div className="max-w-6xl mx-auto px-6 py-8 space-y-6">

        {/* Hero */}
        <div className="bg-surface border border-border rounded-2xl px-8 py-6">
          <h1 className="text-3xl font-bold text-white">Panel administratora</h1>
          <p className="text-muted mt-1 capitalize">{todayPolish()}</p>
        </div>

        {/* Tabs */}
        <div className="flex gap-1 bg-surface border border-border rounded-xl p-1 overflow-x-auto">
          {tabs.map(tab => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                activeTab === tab.id
                  ? 'bg-orange-500 text-white'
                  : 'text-muted hover:text-white'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* === PRZEGLĄD === */}
        {activeTab === 'overview' && (
          <div className="space-y-4">
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
              <StatCard icon={Users} value={stats.activeCoaches} label="Aktywni trenerzy" />
              <StatCard icon={UserCheck} value={stats.activePlayers} label="Aktywni zawodnicy" />
              <StatCard icon={CalendarCheck} value={stats.completedTrainings} label="Treningi ukończone" />
              <StatCard icon={Banknote} value={`${Number(stats.totalEarnings).toFixed(0)} zł`} label="Łączne przychody" accent />
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <StatCard icon={CalendarDays} value={stats.scheduledTrainings} label="Zaplanowane treningi" />
              <StatCard icon={XCircle} value={stats.cancelledTrainings} label="Odwołane treningi" />
              <StatCard icon={Users} value={stats.totalPlayers} label="Wszyscy zawodnicy" />
            </div>
          </div>
        )}

        {/* === TRENERZY === */}
        {activeTab === 'coaches' && (
          <div className="space-y-4">
            {/* Generate invite */}
            <div className="bg-surface border border-border rounded-xl p-5 flex flex-col gap-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-white font-semibold">Dodaj nowego trenera</p>
                  <p className="text-muted text-sm mt-0.5">Wygeneruj jednorazowy link rejestracyjny (ważny 48h)</p>
                </div>
                <button
                  onClick={handleGenerateInvite}
                  disabled={inviteLoading}
                  className="flex items-center gap-2 bg-orange-500 hover:bg-orange-600 disabled:opacity-50 text-white font-semibold px-4 py-2 rounded-lg transition-colors text-sm"
                >
                  <Link size={14} />
                  {inviteLoading ? 'Generowanie...' : 'Nowy link'}
                </button>
              </div>
              {invite && (
                <div>
                  <div className="flex items-center gap-3">
                    <input
                      readOnly
                      value={invite.inviteUrl}
                      className="flex-1 bg-base border border-border rounded-lg px-3 py-2 text-white text-sm focus:outline-none font-mono truncate"
                    />
                    <button
                      onClick={handleCopy}
                      className={`flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors border ${
                        copied
                          ? 'bg-orange-500/20 border-orange-500/40 text-orange-300'
                          : 'bg-base border-border text-muted hover:text-white hover:border-slate-500'
                      }`}
                    >
                      {copied ? <><Check size={14} /> Skopiowano</> : <><Copy size={14} /> Kopiuj</>}
                    </button>
                  </div>
                  <p className="text-xs text-muted mt-2">
                    Wygasa: {new Date(invite.expiresAt).toLocaleString('pl-PL', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}
                  </p>
                </div>
              )}
            </div>

            {/* Coaches list */}
            <div className="bg-surface border border-border rounded-xl p-6">
              <h2 className="text-white font-semibold mb-4">
                Lista trenerów
                <span className="text-muted text-sm font-normal ml-2">
                  {stats.activeCoaches} aktywnych · {stats.totalCoaches} łącznie
                </span>
              </h2>
              {stats.coachStats.length === 0 ? (
                <p className="text-muted text-sm py-4 text-center">Brak trenerów</p>
              ) : (
                <div className="space-y-1">
                  {stats.coachStats.map(c => (
                    <div key={c.id} className="flex items-center gap-4 py-3 border-b border-border last:border-0">
                      <Avatar firstName={c.firstName} lastName={c.lastName} size="sm" />
                      <div className="flex-1 min-w-0">
                        <p className="text-white font-medium">{c.firstName} {c.lastName}</p>
                        <p className="text-muted text-sm truncate">{c.email}</p>
                      </div>
                      <span className="text-muted text-sm flex items-center gap-1">
                        <Users size={13} />
                        {c.playerCount}
                      </span>
                      <span className={`text-xs font-semibold px-2.5 py-1 rounded-full border ${
                        c.active
                          ? 'bg-accent/20 text-accent border-accent/30'
                          : 'bg-slate-500/20 text-slate-400 border-slate-500/30'
                      }`}>
                        {c.active ? 'Aktywny' : 'Nieaktywny'}
                      </span>
                      {c.active ? (
                        <button
                          onClick={() => setConfirmAction({ user: c, action: 'deactivate' })}
                          className="flex items-center gap-1.5 text-xs text-muted hover:text-red-400 transition-colors px-2 py-1 rounded"
                          title="Dezaktywuj"
                        >
                          <UserX size={13} />
                        </button>
                      ) : (
                        <button
                          onClick={() => setConfirmAction({ user: c, action: 'activate' })}
                          className="flex items-center gap-1.5 text-xs text-muted hover:text-green-400 transition-colors px-2 py-1 rounded"
                          title="Reaktywuj"
                        >
                          <UserPlus size={13} />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* === ZAWODNICY === */}
        {activeTab === 'players' && (
          <div className="bg-surface border border-border rounded-xl p-6">
            <h2 className="text-white font-semibold mb-4">
              Wszyscy zawodnicy
              <span className="text-muted text-sm font-normal ml-2">
                {players.filter(p => p.active).length} aktywnych · {players.length} łącznie
              </span>
            </h2>
            {players.length === 0 ? (
              <p className="text-muted text-sm py-4 text-center">Brak zawodników</p>
            ) : (
              <div className="space-y-1">
                {players.map(p => (
                  <div key={p.id} className="flex items-center gap-4 py-3 border-b border-border last:border-0">
                    <Avatar firstName={p.firstName} lastName={p.lastName} size="sm" />
                    <div className="flex-1 min-w-0">
                      <p className="text-white font-medium flex items-center gap-2">
                        {p.firstName} {p.lastName}
                        {p.virtual && (
                          <span className="flex items-center gap-1 text-xs text-slate-400">
                            <Ghost size={11} /> wirtualny
                          </span>
                        )}
                      </p>
                      <p className="text-muted text-sm truncate">{p.virtual ? '—' : p.email}</p>
                    </div>
                    <span className={`text-xs font-semibold px-2.5 py-1 rounded-full border ${
                      p.active
                        ? 'bg-accent/20 text-accent border-accent/30'
                        : 'bg-slate-500/20 text-slate-400 border-slate-500/30'
                    }`}>
                      {p.active ? 'Aktywny' : 'Nieaktywny'}
                    </span>
                    {p.active ? (
                      <button
                        onClick={() => setConfirmAction({ user: p, action: 'deactivate' })}
                        className="flex items-center gap-1.5 text-xs text-muted hover:text-red-400 transition-colors px-2 py-1 rounded"
                        title="Dezaktywuj"
                      >
                        <UserX size={13} />
                      </button>
                    ) : (
                      <button
                        onClick={() => setConfirmAction({ user: p, action: 'activate' })}
                        className="flex items-center gap-1.5 text-xs text-muted hover:text-green-400 transition-colors px-2 py-1 rounded"
                        title="Reaktywuj"
                      >
                        <UserPlus size={13} />
                      </button>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

      </div>
    </>
  );
}
