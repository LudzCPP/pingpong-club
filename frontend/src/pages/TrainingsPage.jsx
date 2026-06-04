import { useEffect, useState } from 'react';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';

const STATUS_LABEL = { SCHEDULED: 'Zaplanowany', COMPLETED: 'Zrealizowany', CANCELLED: 'Odwołany' };
const STATUS_CLASS = { SCHEDULED: 'badge-blue', COMPLETED: 'badge-green', CANCELLED: 'badge-gray' };

export default function TrainingsPage() {
  const { user } = useAuth();
  const isCoach = user?.role === 'COACH';

  const [trainings, setTrainings] = useState([]);
  const [players, setPlayers] = useState([]);
  const [loading, setLoading] = useState(true);
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
      } finally {
        setLoading(false);
      }
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
    } finally {
      setSaving(false);
    }
  }

  async function handleStatus(id, action) {
    await client.patch(`/trainings/${id}/${action}`);
    await fetchTrainings();
  }

  if (loading) return <div className="page-loading">Ładowanie...</div>;

  return (
    <div className="page">
      <div className="page-header">
        <h2>Treningi</h2>
        {isCoach && (
          <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
            {showForm ? 'Anuluj' : '+ Nowy trening'}
          </button>
        )}
      </div>

      {showForm && (
        <div className="card form-card">
          <h3>Zaplanuj trening</h3>
          <form onSubmit={handleCreate} className="form-grid">
            <div className="form-group">
              <label>Zawodnik</label>
              <select value={form.playerId} onChange={e => setForm({ ...form, playerId: e.target.value })} required>
                <option value="">-- wybierz --</option>
                {players.map(p => (
                  <option key={p.id} value={p.id}>{p.firstName} {p.lastName}</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label>Data i godzina</label>
              <input type="datetime-local" value={form.scheduledAt}
                onChange={e => setForm({ ...form, scheduledAt: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>Czas trwania (min)</label>
              <input type="number" min="15" max="480" value={form.durationMinutes}
                onChange={e => setForm({ ...form, durationMinutes: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>Stawka godzinowa (zł)</label>
              <input type="number" min="0" step="0.01" value={form.hourlyRate}
                onChange={e => setForm({ ...form, hourlyRate: e.target.value })} required />
            </div>
            <div className="form-group form-group-full">
              <label>Notatki</label>
              <input type="text" value={form.notes} onChange={e => setForm({ ...form, notes: e.target.value })}
                placeholder="opcjonalnie" />
            </div>
            {formError && <div className="alert alert-error form-group-full">{formError}</div>}
            <div className="form-group-full">
              <button type="submit" className="btn btn-primary" disabled={saving}>
                {saving ? 'Zapisywanie...' : 'Zaplanuj'}
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="table-wrapper">
        {trainings.length === 0 ? (
          <div className="empty-state">Brak treningów</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Nazwa</th>
                <th>Data</th>
                <th>Czas</th>
                <th>Stawka</th>
                <th>Koszt</th>
                <th>Status</th>
                {isCoach && <th>Akcje</th>}
              </tr>
            </thead>
            <tbody>
              {trainings.map(t => (
                <tr key={t.id}>
                  <td><strong>{t.name}</strong><br /><small>{t.coachFullName}</small></td>
                  <td>{new Date(t.scheduledAt).toLocaleString('pl-PL')}</td>
                  <td>{t.durationMinutes} min</td>
                  <td>{t.hourlyRate} zł/h</td>
                  <td><strong>{t.totalPrice} zł</strong></td>
                  <td><span className={`badge ${STATUS_CLASS[t.status]}`}>{STATUS_LABEL[t.status]}</span></td>
                  {isCoach && (
                    <td className="actions">
                      {t.status === 'SCHEDULED' && (
                        <>
                          <button className="btn btn-sm btn-green" onClick={() => handleStatus(t.id, 'complete')}>✓</button>
                          <button className="btn btn-sm btn-outline" onClick={() => handleStatus(t.id, 'cancel')}>✕</button>
                        </>
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
