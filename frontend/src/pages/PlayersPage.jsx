import { useEffect, useState } from 'react';
import client from '../api/client';

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

  useEffect(() => {
    fetchPlayers().finally(() => setLoading(false));
  }, []);

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
    } finally {
      setSaving(false);
    }
  }

  async function handleDeactivate(id) {
    if (!confirm('Dezaktywować konto tego zawodnika?')) return;
    await client.delete(`/users/${id}`);
    await fetchPlayers();
  }

  if (loading) return <div className="page-loading">Ładowanie...</div>;

  return (
    <div className="page">
      <div className="page-header">
        <h2>Zawodnicy</h2>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Anuluj' : '+ Nowy zawodnik'}
        </button>
      </div>

      {showForm && (
        <div className="card form-card">
          <h3>Dodaj zawodnika</h3>
          <form onSubmit={handleCreate} className="form-grid">
            <div className="form-group">
              <label>Imię</label>
              <input value={form.firstName} onChange={e => setForm({ ...form, firstName: e.target.value })}
                placeholder="Jan" required />
            </div>
            <div className="form-group">
              <label>Nazwisko</label>
              <input value={form.lastName} onChange={e => setForm({ ...form, lastName: e.target.value })}
                placeholder="Kowalski" required />
            </div>
            <div className="form-group">
              <label>Email</label>
              <input type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })}
                placeholder="jan@example.com" required />
            </div>
            <div className="form-group">
              <label>Hasło</label>
              <input type="password" value={form.password} onChange={e => setForm({ ...form, password: e.target.value })}
                placeholder="min. 6 znaków" required />
            </div>
            {formError && <div className="alert alert-error form-group-full">{formError}</div>}
            <div className="form-group-full">
              <button type="submit" className="btn btn-primary" disabled={saving}>
                {saving ? 'Tworzenie...' : 'Utwórz konto'}
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="table-wrapper">
        {players.length === 0 ? (
          <div className="empty-state">Brak zawodników</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Imię i nazwisko</th>
                <th>Email</th>
                <th>Status</th>
                <th>Akcje</th>
              </tr>
            </thead>
            <tbody>
              {players.map(p => (
                <tr key={p.id}>
                  <td><strong>{p.firstName} {p.lastName}</strong></td>
                  <td>{p.email}</td>
                  <td>
                    <span className={`badge ${p.active ? 'badge-green' : 'badge-gray'}`}>
                      {p.active ? 'Aktywny' : 'Nieaktywny'}
                    </span>
                  </td>
                  <td>
                    {p.active && (
                      <button className="btn btn-sm btn-outline" onClick={() => handleDeactivate(p.id)}>
                        Dezaktywuj
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
