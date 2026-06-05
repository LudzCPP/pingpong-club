import { useEffect, useState } from 'react';
import client from '../api/client';
import Avatar from '../components/Avatar';
import { Plus, X } from 'lucide-react';

const inputCls = 'bg-base border border-border rounded-lg px-3 py-2 text-white text-sm placeholder-muted focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent transition-colors w-full';
const labelCls = 'text-xs font-medium text-muted uppercase tracking-wide';

export default function PlayersPage() {
  const [players, setPlayers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '' });
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  async function fetchPlayers() {
    const { data } = await client.get('/users/players');
    setPlayers(data);
  }

  useEffect(() => { fetchPlayers().finally(() => setLoading(false)); }, []);

  async function handleCreate(e) {
    e.preventDefault();
    setFormError('');
    setSaving(true);
    try {
      await client.post('/auth/register', form);
      setShowForm(false);
      setForm({ firstName: '', lastName: '', email: '', password: '' });
      await fetchPlayers();
    } catch (err) {
      setFormError(err.response?.data?.detail || 'Błąd tworzenia konta.');
    } finally { setSaving(false); }
  }

  async function handleDeactivate(id) {
    if (!confirm('Dezaktywować konto tego zawodnika?')) return;
    await client.delete(`/users/${id}`);
    await fetchPlayers();
  }

  if (loading) return <div className="max-w-6xl mx-auto px-6 py-12 text-center text-muted">Ładowanie...</div>;

  const active = players.filter(p => p.active).length;

  return (
    <div className="max-w-6xl mx-auto px-6 py-8 space-y-6">

      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Zawodnicy</h1>
          <p className="text-sm text-muted mt-1">{active} aktywnych · {players.length} łącznie</p>
        </div>
        <button
          onClick={() => setShowForm(!showForm)}
          className="flex items-center gap-2 bg-accent hover:bg-green-600 text-white font-semibold px-4 py-2 rounded-lg transition-colors text-sm"
        >
          {showForm ? <><X size={14} /> Anuluj</> : <><Plus size={14} /> Dodaj zawodnika</>}
        </button>
      </div>

      {/* Form */}
      {showForm && (
        <div className="bg-surface border border-border border-l-4 border-l-accent rounded-xl p-6">
          <h2 className="text-white font-semibold mb-5">Nowy zawodnik</h2>
          <form onSubmit={handleCreate} className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className={labelCls}>Imię</label>
              <input value={form.firstName} onChange={e => setForm({ ...form, firstName: e.target.value })} placeholder="Jan" required className={inputCls} />
            </div>
            <div className="space-y-1.5">
              <label className={labelCls}>Nazwisko</label>
              <input value={form.lastName} onChange={e => setForm({ ...form, lastName: e.target.value })} placeholder="Kowalski" required className={inputCls} />
            </div>
            <div className="space-y-1.5">
              <label className={labelCls}>Email</label>
              <input type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} placeholder="jan@example.com" required className={inputCls} />
            </div>
            <div className="space-y-1.5">
              <label className={labelCls}>Hasło</label>
              <input type="password" value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} placeholder="min. 6 znaków" required className={inputCls} />
            </div>
            {formError && (
              <div className="sm:col-span-2 bg-red-500/10 border border-red-500/30 text-red-400 text-sm px-4 py-3 rounded-lg">{formError}</div>
            )}
            <div className="sm:col-span-2">
              <button type="submit" disabled={saving} className="bg-accent hover:bg-green-600 disabled:opacity-50 text-white font-semibold px-6 py-2 rounded-lg transition-colors text-sm">
                {saving ? 'Tworzenie...' : 'Utwórz konto'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Player cards grid */}
      {players.length === 0 ? (
        <div className="text-center text-muted py-16">Brak zawodników</div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {players.map(p => (
            <div key={p.id} className="bg-surface border border-border rounded-xl p-5 flex flex-col gap-4 hover:border-accent/40 transition-colors">
              <div className="flex items-center gap-4">
                <Avatar firstName={p.firstName} lastName={p.lastName} size="lg" />
                <div className="min-w-0">
                  <p className="text-white font-semibold text-lg leading-tight">{p.firstName} {p.lastName}</p>
                  <p className="text-muted text-sm truncate mt-0.5">{p.email}</p>
                </div>
              </div>
              <div className="flex items-center justify-between pt-2 border-t border-border">
                <span className={`text-xs font-semibold px-2.5 py-1 rounded-full border ${
                  p.active
                    ? 'bg-green-500/20 text-green-300 border-green-500/30'
                    : 'bg-slate-500/20 text-slate-400 border-slate-500/30'
                }`}>
                  {p.active ? 'Aktywny' : 'Nieaktywny'}
                </span>
                {p.active && (
                  <button
                    onClick={() => handleDeactivate(p.id)}
                    className="text-xs text-muted hover:text-red-400 transition-colors px-2 py-1"
                  >
                    Dezaktywuj
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
