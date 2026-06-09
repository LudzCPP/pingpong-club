import { useEffect, useState } from 'react';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';
import Avatar from '../components/Avatar';
import StatCard from '../components/StatCard';
import { Dumbbell, Trophy, Banknote, Plus, X, Trash2 } from 'lucide-react';

const inputCls = 'bg-base border border-border rounded-lg px-3 py-2 text-white text-sm placeholder-muted focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent transition-colors w-full';
const labelCls = 'text-xs font-medium text-muted uppercase tracking-wide';

export default function FinancesPage() {
  const { user } = useAuth();
  const isCoach = user?.role === 'COACH' || user?.role === 'ADMIN';

  const now = new Date();
  const firstOfMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`;
  const today = now.toISOString().split('T')[0];

  const [from, setFrom] = useState(firstOfMonth);
  const [to, setTo] = useState(today);
  const [summary, setSummary] = useState(null);
  const [loadingSummary, setLoadingSummary] = useState(false);

  const [matches, setMatches] = useState([]);
  const [showMatchForm, setShowMatchForm] = useState(false);
  const [players, setPlayers] = useState([]);
  const [matchForm, setMatchForm] = useState({ playerId: '', matchDate: today, opponent: '', result: '', payment: '', notes: '' });
  const [matchError, setMatchError] = useState('');
  const [savingMatch, setSavingMatch] = useState(false);

  async function fetchSummary() {
    setLoadingSummary(true);
    try {
      const { data } = await client.get(`/finances/summary?from=${from}&to=${to}`);
      setSummary(data);
    } finally { setLoadingSummary(false); }
  }

  async function fetchMatches() {
    const { data } = await client.get('/finances/matches');
    setMatches(data);
  }

  useEffect(() => {
    fetchSummary();
    fetchMatches();
    if (isCoach) client.get('/users/players').then(r => setPlayers(r.data));
  }, [isCoach]);

  async function handleMatchCreate(e) {
    e.preventDefault();
    setMatchError('');
    setSavingMatch(true);
    try {
      await client.post('/finances/matches', { ...matchForm, payment: Number(matchForm.payment) });
      setShowMatchForm(false);
      setMatchForm({ playerId: '', matchDate: today, opponent: '', result: '', payment: '', notes: '' });
      await fetchMatches();
    } catch (err) {
      setMatchError(err.response?.data?.detail || 'Błąd zapisu meczu.');
    } finally { setSavingMatch(false); }
  }

  async function handleMatchDelete(id) {
    if (!confirm('Usunąć mecz?')) return;
    await client.delete(`/finances/matches/${id}`);
    await fetchMatches();
  }

  return (
    <div className="max-w-6xl mx-auto px-6 py-8 space-y-6">

      {/* Header + stat cards */}
      <div>
        <h1 className="text-2xl font-bold text-white mb-6">Finanse</h1>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
          <StatCard icon={Dumbbell} value={`${Number(summary?.totalTrainingEarnings ?? 0).toFixed(0)} zł`} label={`Treningi (${summary?.completedTrainingsCount ?? 0})`} />
          <StatCard icon={Trophy} value={`${Number(summary?.totalMatchPayments ?? 0).toFixed(0)} zł`} label={`Mecze (${summary?.matchesCount ?? 0})`} />
          <StatCard icon={Banknote} value={`${Number(summary?.grandTotal ?? 0).toFixed(0)} zł`} label="Łącznie" accent />
        </div>

        {/* Date filter */}
        <div className="bg-surface border border-border rounded-xl px-5 py-4 flex flex-col sm:flex-row items-stretch sm:items-end gap-4">
          <div className="space-y-1.5">
            <label className={labelCls}>Od</label>
            <input type="date" value={from} onChange={e => setFrom(e.target.value)} className={`${inputCls} w-auto`} />
          </div>
          <div className="space-y-1.5">
            <label className={labelCls}>Do</label>
            <input type="date" value={to} onChange={e => setTo(e.target.value)} className={`${inputCls} w-auto`} />
          </div>
          <button onClick={fetchSummary} disabled={loadingSummary}
            className="bg-accent hover:bg-green-600 disabled:opacity-50 text-white font-semibold px-5 py-2 rounded-lg transition-colors text-sm">
            {loadingSummary ? 'Obliczanie...' : 'Przelicz'}
          </button>
        </div>
      </div>

      {/* Matches section */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-white font-semibold text-lg">Mecze ligowe</h2>
          {isCoach && (
            <button onClick={() => setShowMatchForm(!showMatchForm)}
              className="flex items-center gap-2 bg-accent hover:bg-green-600 text-white font-semibold px-4 py-2 rounded-lg transition-colors text-sm">
              {showMatchForm ? <><X size={14} /> Anuluj</> : <><Plus size={14} /> Dodaj mecz</>}
            </button>
          )}
        </div>

        {/* Match form */}
        {showMatchForm && (
          <div className="bg-surface border border-border border-l-4 border-l-accent rounded-xl p-6">
            <form onSubmit={handleMatchCreate} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              <div className="space-y-1.5">
                <label className={labelCls}>Zawodnik</label>
                <select value={matchForm.playerId} onChange={e => setMatchForm({ ...matchForm, playerId: e.target.value })} required className={inputCls}>
                  <option value="">-- wybierz --</option>
                  {players.map(p => <option key={p.id} value={p.id}>{p.firstName} {p.lastName}</option>)}
                </select>
              </div>
              <div className="space-y-1.5">
                <label className={labelCls}>Data meczu</label>
                <input type="date" value={matchForm.matchDate} onChange={e => setMatchForm({ ...matchForm, matchDate: e.target.value })} required className={inputCls} />
              </div>
              <div className="space-y-1.5">
                <label className={labelCls}>Przeciwnik</label>
                <input value={matchForm.opponent} onChange={e => setMatchForm({ ...matchForm, opponent: e.target.value })} required className={inputCls} />
              </div>
              <div className="space-y-1.5">
                <label className={labelCls}>Wynik (np. 3:1)</label>
                <input value={matchForm.result} placeholder="3:1" onChange={e => setMatchForm({ ...matchForm, result: e.target.value })} required className={inputCls} />
              </div>
              <div className="space-y-1.5">
                <label className={labelCls}>Wypłata (zł)</label>
                <input type="number" min="0" step="0.01" value={matchForm.payment} onChange={e => setMatchForm({ ...matchForm, payment: e.target.value })} required className={inputCls} />
              </div>
              <div className="space-y-1.5">
                <label className={labelCls}>Notatki</label>
                <input value={matchForm.notes} onChange={e => setMatchForm({ ...matchForm, notes: e.target.value })} placeholder="opcjonalnie" className={inputCls} />
              </div>
              {matchError && <div className="sm:col-span-2 lg:col-span-3 bg-red-500/10 border border-red-500/30 text-red-400 text-sm px-4 py-3 rounded-lg">{matchError}</div>}
              <div className="sm:col-span-2 lg:col-span-3">
                <button type="submit" disabled={savingMatch} className="bg-accent hover:bg-green-600 disabled:opacity-50 text-white font-semibold px-6 py-2 rounded-lg transition-colors text-sm">
                  {savingMatch ? 'Zapisywanie...' : 'Zapisz mecz'}
                </button>
              </div>
            </form>
          </div>
        )}

        {/* Matches list */}
        <div className="bg-surface border border-border rounded-xl overflow-hidden">
          {matches.length === 0 ? (
            <p className="text-center text-muted py-12">Brak meczów ligowych</p>
          ) : (
            <>
              {/* Mobile: cards */}
              <div className="sm:hidden divide-y divide-border">
                {matches.map(m => (
                  <div key={m.id} className="p-4 space-y-2">
                    <div className="flex items-center justify-between gap-3">
                      <div className="flex items-center gap-3 min-w-0">
                        <Avatar firstName={m.playerFullName?.split(' ')[0]} lastName={m.playerFullName?.split(' ')[1]} size="sm" />
                        <div className="min-w-0">
                          <p className="text-white font-medium truncate">{m.playerFullName}</p>
                          <p className="text-muted text-xs">{new Date(m.matchDate).toLocaleDateString('pl-PL')}</p>
                        </div>
                      </div>
                      {isCoach && (
                        <button onClick={() => handleMatchDelete(m.id)}
                          className="text-muted hover:text-red-400 transition-colors p-1.5 rounded hover:bg-red-400/10 shrink-0">
                          <Trash2 size={14} />
                        </button>
                      )}
                    </div>
                    <div className="flex items-center gap-3 ml-11 text-sm">
                      <span className="text-muted truncate">{m.opponent}</span>
                      <span className="font-mono font-bold text-white bg-base border border-border px-2 py-0.5 rounded text-xs shrink-0">{m.result}</span>
                      <span className="ml-auto text-white font-semibold shrink-0">{m.payment} zł</span>
                    </div>
                  </div>
                ))}
              </div>

              {/* Desktop: table */}
              <table className="w-full text-sm hidden sm:table">
                <thead>
                  <tr className="border-b border-border">
                    <th className="text-left px-4 py-3 text-xs text-muted uppercase tracking-wide">Zawodnik</th>
                    <th className="text-left px-4 py-3 text-xs text-muted uppercase tracking-wide">Data</th>
                    <th className="text-left px-4 py-3 text-xs text-muted uppercase tracking-wide">Przeciwnik</th>
                    <th className="text-left px-4 py-3 text-xs text-muted uppercase tracking-wide">Wynik</th>
                    <th className="text-left px-4 py-3 text-xs text-muted uppercase tracking-wide">Wypłata</th>
                    {isCoach && <th className="px-4 py-3 text-xs text-muted uppercase tracking-wide">Akcje</th>}
                  </tr>
                </thead>
                <tbody>
                  {matches.map(m => (
                    <tr key={m.id} className="border-b border-border/50 hover:bg-white/5 transition-colors last:border-0">
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2.5">
                          <Avatar firstName={m.playerFullName?.split(' ')[0]} lastName={m.playerFullName?.split(' ')[1]} size="sm" />
                          <span className="text-white font-medium">{m.playerFullName}</span>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-muted whitespace-nowrap">
                        {new Date(m.matchDate).toLocaleDateString('pl-PL')}
                      </td>
                      <td className="px-4 py-3 text-muted">{m.opponent}</td>
                      <td className="px-4 py-3">
                        <span className="font-mono font-bold text-white bg-base border border-border px-2 py-0.5 rounded text-xs">{m.result}</span>
                      </td>
                      <td className="px-4 py-3 text-white font-semibold">{m.payment} zł</td>
                      {isCoach && (
                        <td className="px-4 py-3 text-center">
                          <button onClick={() => handleMatchDelete(m.id)}
                            className="text-muted hover:text-red-400 transition-colors p-1.5 rounded hover:bg-red-400/10" title="Usuń">
                            <Trash2 size={14} />
                          </button>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
