import { useEffect, useState } from 'react';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';
import Avatar from '../components/Avatar';
import StatusBadge from '../components/StatusBadge';

const FILTERS = ['Wszystkie', 'SCHEDULED', 'COMPLETED', 'CANCELLED'];
const FILTER_LABEL = { Wszystkie: 'Wszystkie', SCHEDULED: 'Zaplanowane', COMPLETED: 'Zrealizowane', CANCELLED: 'Odwołane' };

const inputCls = 'bg-base border border-border rounded-lg px-3 py-2 text-white text-sm placeholder-muted focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent transition-colors w-full';
const labelCls = 'text-xs font-medium text-muted uppercase tracking-wide';

export default function TrainingsPage() {
  const { user } = useAuth();
  const isCoach = user?.role === 'COACH';

  const [trainings, setTrainings] = useState([]);
  const [players, setPlayers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('Wszystkie');
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ playerId: '', scheduledAt: '', durationMinutes: 60, hourlyRate: '', notes: '' });
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  async function fetchTrainings() {
    const { data } = await client.get('/trainings');
    setTrainings(data);
  }

  useEffect(() => {
    async function load() {
      try {
        await fetchTrainings();
        if (isCoach) {
          const { data } = await client.get('/users/players');
          setPlayers(data);
        }
      } finally { setLoading(false); }
    }
    load();
  }, [isCoach]);

  async function handleCreate(e) {
    e.preventDefault();
    setFormError('');
    setSaving(true);
    try {
      await client.post('/trainings', {
        ...form,
        durationMinutes: Number(form.durationMinutes),
        hourlyRate: Number(form.hourlyRate),
      });
      setShowForm(false);
      setForm({ playerId: '', scheduledAt: '', durationMinutes: 60, hourlyRate: '', notes: '' });
      await fetchTrainings();
    } catch (err) {
      setFormError(err.response?.data?.detail || 'Błąd zapisu treningu.');
    } finally { setSaving(false); }
  }

  async function handleStatus(id, action) {
    await client.patch(`/trainings/${id}/${action}`);
    await fetchTrainings();
  }

  const visible = filter === 'Wszystkie' ? trainings : trainings.filter(t => t.status === filter);
  const scheduled = trainings.filter(t => t.status === 'SCHEDULED').length;
  const completed = trainings.filter(t => t.status === 'COMPLETED').length;

  if (loading) return <div className="max-w-6xl mx-auto px-6 py-12 text-center text-muted">Ładowanie...</div>;

  return (
    <div className="max-w-6xl mx-auto px-6 py-8 space-y-6">

      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Treningi</h1>
          <p className="text-sm text-muted mt-1">
            <span className="text-blue-300">{scheduled} zaplanowanych</span>
            <span className="mx-2 text-border">·</span>
            <span className="text-green-300">{completed} zrealizowanych</span>
          </p>
        </div>
        {isCoach && (
          <button
            onClick={() => setShowForm(!showForm)}
            className="bg-accent hover:bg-green-600 text-white font-semibold px-4 py-2 rounded-lg transition-colors text-sm"
          >
            {showForm ? '✕ Anuluj' : '+ Nowy trening'}
          </button>
        )}
      </div>

      {/* Form */}
      {showForm && (
        <div className="bg-surface border border-border rounded-xl p-6 border-l-4 border-l-accent">
          <h2 className="text-white font-semibold mb-5">Zaplanuj trening</h2>
          <form onSubmit={handleCreate} className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className={labelCls}>Zawodnik</label>
              <select value={form.playerId} onChange={e => setForm({ ...form, playerId: e.target.value })} required className={inputCls}>
                <option value="">-- wybierz --</option>
                {players.map(p => <option key={p.id} value={p.id}>{p.firstName} {p.lastName}</option>)}
              </select>
            </div>
            <div className="space-y-1.5">
              <label className={labelCls}>Data i godzina</label>
              <input type="datetime-local" value={form.scheduledAt} onChange={e => setForm({ ...form, scheduledAt: e.target.value })} required className={inputCls} />
            </div>
            <div className="space-y-1.5">
              <label className={labelCls}>Czas trwania (min)</label>
              <input type="number" min="15" max="480" value={form.durationMinutes} onChange={e => setForm({ ...form, durationMinutes: e.target.value })} required className={inputCls} />
            </div>
            <div className="space-y-1.5">
              <label className={labelCls}>Stawka (zł/h)</label>
              <input type="number" min="0" step="0.01" value={form.hourlyRate} onChange={e => setForm({ ...form, hourlyRate: e.target.value })} required className={inputCls} />
            </div>
            <div className="space-y-1.5 sm:col-span-2">
              <label className={labelCls}>Notatki</label>
              <input type="text" value={form.notes} onChange={e => setForm({ ...form, notes: e.target.value })} placeholder="opcjonalnie" className={inputCls} />
            </div>
            {formError && <div className="sm:col-span-2 bg-red-500/10 border border-red-500/30 text-red-400 text-sm px-4 py-3 rounded-lg">{formError}</div>}
            <div className="sm:col-span-2">
              <button type="submit" disabled={saving} className="bg-accent hover:bg-green-600 disabled:opacity-50 text-white font-semibold px-6 py-2 rounded-lg transition-colors text-sm">
                {saving ? 'Zapisywanie...' : 'Zaplanuj trening'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Filter bar */}
      <div className="flex gap-2 flex-wrap">
        {FILTERS.map(f => (
          <button key={f} onClick={() => setFilter(f)}
            className={`px-4 py-1.5 rounded-full text-sm font-medium transition-colors ${
              filter === f
                ? 'bg-accent text-white'
                : 'bg-surface border border-border text-muted hover:text-white'
            }`}>
            {FILTER_LABEL[f]}
          </button>
        ))}
      </div>

      {/* Table */}
      <div className="bg-surface border border-border rounded-xl overflow-hidden">
        {visible.length === 0 ? (
          <p className="text-center text-muted py-12">Brak treningów</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border">
                <th className="text-left px-4 py-3 text-xs text-muted uppercase tracking-wide">Zawodnik</th>
                <th className="text-left px-4 py-3 text-xs text-muted uppercase tracking-wide">Data</th>
                <th className="text-left px-4 py-3 text-xs text-muted uppercase tracking-wide hidden sm:table-cell">Czas</th>
                <th className="text-left px-4 py-3 text-xs text-muted uppercase tracking-wide hidden md:table-cell">Stawka</th>
                <th className="text-left px-4 py-3 text-xs text-muted uppercase tracking-wide">Koszt</th>
                <th className="text-left px-4 py-3 text-xs text-muted uppercase tracking-wide">Status</th>
                {isCoach && <th className="px-4 py-3 text-xs text-muted uppercase tracking-wide">Akcje</th>}
              </tr>
            </thead>
            <tbody>
              {visible.map(t => (
                <tr key={t.id} className="border-b border-border/50 hover:bg-white/5 transition-colors last:border-0">
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2.5">
                      <Avatar firstName={t.playerFullName?.split(' ')[0]} lastName={t.playerFullName?.split(' ')[1]} size="sm" />
                      <div>
                        <p className="text-white font-medium">{t.playerFullName}</p>
                        <p className="text-muted text-xs">{t.name}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-muted whitespace-nowrap">
                    {new Date(t.scheduledAt).toLocaleString('pl-PL', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}
                  </td>
                  <td className="px-4 py-3 text-muted hidden sm:table-cell">{t.durationMinutes} min</td>
                  <td className="px-4 py-3 text-muted hidden md:table-cell">{t.hourlyRate} zł/h</td>
                  <td className="px-4 py-3 text-white font-semibold">{t.totalPrice ?? '—'} zł</td>
                  <td className="px-4 py-3"><StatusBadge status={t.status} /></td>
                  {isCoach && (
                    <td className="px-4 py-3">
                      {t.status === 'SCHEDULED' && (
                        <div className="flex gap-1">
                          <button onClick={() => handleStatus(t.id, 'complete')} title="Zakończ"
                            className="text-green-400 hover:bg-green-400/10 px-2 py-1 rounded transition-colors">✓</button>
                          <button onClick={() => handleStatus(t.id, 'cancel')} title="Odwołaj"
                            className="text-red-400 hover:bg-red-400/10 px-2 py-1 rounded transition-colors">✕</button>
                        </div>
                      )}
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
