import { useEffect, useState } from 'react';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function FinancesPage() {
  const { user } = useAuth();
  const isCoach = user?.role === 'COACH';

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
    } finally {
      setLoadingSummary(false);
    }
  }

  async function fetchMatches() {
    const { data } = await client.get('/finances/matches');
    setMatches(data);
  }

  useEffect(() => {
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
    } finally {
      setSavingMatch(false);
    }
  }

  async function handleMatchDelete(id) {
    if (!confirm('Usunąć mecz?')) return;
    await client.delete(`/finances/matches/${id}`);
    await fetchMatches();
  }

  return (
    <div className="page">
      <h2>Finanse</h2>

      {/* Podsumowanie */}
      <div className="card">
        <h3>Zestawienie zarobków</h3>
        <div className="filter-row">
          <div className="form-group">
            <label>Od</label>
            <input type="date" value={from} onChange={e => setFrom(e.target.value)} />
          </div>
          <div className="form-group">
            <label>Do</label>
            <input type="date" value={to} onChange={e => setTo(e.target.value)} />
          </div>
          <button className="btn btn-primary" onClick={fetchSummary} disabled={loadingSummary}>
            {loadingSummary ? 'Obliczanie...' : 'Oblicz'}
          </button>
        </div>

        {summary && (
          <div className="summary-grid">
            <div className="summary-card">
              <div className="summary-label">Treningi zrealizowane</div>
              <div className="summary-value">{summary.completedTrainingsCount}</div>
              <div className="summary-sub">{summary.totalTrainingEarnings} zł</div>
            </div>
            <div className="summary-card">
              <div className="summary-label">Mecze ligowe</div>
              <div className="summary-value">{summary.matchesCount}</div>
              <div className="summary-sub">{summary.totalMatchPayments} zł</div>
            </div>
            <div className="summary-card summary-total">
              <div className="summary-label">Łącznie</div>
              <div className="summary-value">{summary.grandTotal} zł</div>
            </div>
          </div>
        )}
      </div>

      {/* Mecze ligowe */}
      <div className="page-header" style={{ marginTop: '2rem' }}>
        <h3>Mecze ligowe</h3>
        {isCoach && (
          <button className="btn btn-primary" onClick={() => setShowMatchForm(!showMatchForm)}>
            {showMatchForm ? 'Anuluj' : '+ Dodaj mecz'}
          </button>
        )}
      </div>

      {showMatchForm && (
        <div className="card form-card">
          <form onSubmit={handleMatchCreate} className="form-grid">
            <div className="form-group">
              <label>Zawodnik</label>
              <select value={matchForm.playerId} onChange={e => setMatchForm({ ...matchForm, playerId: e.target.value })} required>
                <option value="">-- wybierz --</option>
                {players.map(p => <option key={p.id} value={p.id}>{p.firstName} {p.lastName}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label>Data meczu</label>
              <input type="date" value={matchForm.matchDate}
                onChange={e => setMatchForm({ ...matchForm, matchDate: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>Przeciwnik</label>
              <input value={matchForm.opponent}
                onChange={e => setMatchForm({ ...matchForm, opponent: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>Wynik (np. 3:1)</label>
              <input value={matchForm.result} placeholder="3:1"
                onChange={e => setMatchForm({ ...matchForm, result: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>Wypłata (zł)</label>
              <input type="number" min="0" step="0.01" value={matchForm.payment}
                onChange={e => setMatchForm({ ...matchForm, payment: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>Notatki</label>
              <input value={matchForm.notes} onChange={e => setMatchForm({ ...matchForm, notes: e.target.value })} />
            </div>
            {matchError && <div className="alert alert-error form-group-full">{matchError}</div>}
            <div className="form-group-full">
              <button type="submit" className="btn btn-primary" disabled={savingMatch}>
                {savingMatch ? 'Zapisywanie...' : 'Zapisz mecz'}
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="table-wrapper">
        {matches.length === 0 ? (
          <div className="empty-state">Brak meczów ligowych</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Zawodnik</th>
                <th>Data</th>
                <th>Przeciwnik</th>
                <th>Wynik</th>
                <th>Wypłata</th>
                {isCoach && <th>Akcje</th>}
              </tr>
            </thead>
            <tbody>
              {matches.map(m => (
                <tr key={m.id}>
                  <td>{m.playerFullName}</td>
                  <td>{new Date(m.matchDate).toLocaleDateString('pl-PL')}</td>
                  <td>{m.opponent}</td>
                  <td><strong>{m.result}</strong></td>
                  <td>{m.payment} zł</td>
                  {isCoach && (
                    <td>
                      <button className="btn btn-sm btn-outline" onClick={() => handleMatchDelete(m.id)}>Usuń</button>
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
