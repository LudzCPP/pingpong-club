import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Target } from 'lucide-react';
import client from '../api/client';

export default function RegisterPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();

  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const inputCls = 'w-full bg-base border border-border rounded-lg px-4 py-2.5 text-white placeholder-muted focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent transition-colors';

  if (!token) {
    return (
      <div className="min-h-screen bg-base flex items-center justify-center px-4">
        <div className="bg-surface border border-border rounded-2xl p-8 shadow-2xl w-full max-w-md text-center">
          <p className="text-red-400 font-semibold mb-2">Nieprawidłowy link</p>
          <p className="text-muted text-sm">Poproś trenera o nowy link zaproszenia.</p>
        </div>
      </div>
    );
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await client.post(`/auth/register?token=${token}`, form);
      navigate('/login', { state: { registered: true } });
    } catch (err) {
      setError(err.response?.data?.detail || 'Błąd rejestracji. Sprawdź czy link jest wciąż ważny.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-base flex items-center justify-center px-4"
         style={{ backgroundImage: 'radial-gradient(circle at 20% 50%, #1e293b 0%, transparent 50%), radial-gradient(circle at 80% 20%, #0f3460 0%, transparent 40%)' }}>
      <div className="w-full max-w-md">
        <div className="bg-surface border border-border rounded-2xl p-8 shadow-2xl">
          <div className="text-center mb-8">
            <div className="w-16 h-16 bg-accent/20 border border-accent/30 rounded-2xl flex items-center justify-center mx-auto mb-4">
              <Target size={32} className="text-accent" />
            </div>
            <h1 className="text-2xl font-bold text-white">TTManager</h1>
            <p className="text-muted text-sm mt-1">Utwórz konto zawodnika</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <label className="text-sm font-medium text-muted">Imię</label>
                <input
                  value={form.firstName}
                  onChange={e => setForm({ ...form, firstName: e.target.value })}
                  placeholder="Jan"
                  required
                  autoFocus
                  className={inputCls}
                />
              </div>
              <div className="space-y-1.5">
                <label className="text-sm font-medium text-muted">Nazwisko</label>
                <input
                  value={form.lastName}
                  onChange={e => setForm({ ...form, lastName: e.target.value })}
                  placeholder="Kowalski"
                  required
                  className={inputCls}
                />
              </div>
            </div>
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-muted">Email</label>
              <input
                type="email"
                value={form.email}
                onChange={e => setForm({ ...form, email: e.target.value })}
                placeholder="jan@example.com"
                required
                className={inputCls}
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-muted">Hasło</label>
              <input
                type="password"
                value={form.password}
                onChange={e => setForm({ ...form, password: e.target.value })}
                placeholder="min. 6 znaków"
                required
                minLength={6}
                className={inputCls}
              />
            </div>
            {error && (
              <div className="bg-red-500/10 border border-red-500/30 text-red-400 text-sm px-4 py-3 rounded-lg">
                {error}
              </div>
            )}
            <button
              type="submit"
              disabled={loading}
              className="w-full bg-accent hover:bg-green-600 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold py-2.5 rounded-lg transition-colors mt-2"
            >
              {loading ? 'Tworzenie konta...' : 'Zarejestruj się'}
            </button>
          </form>
        </div>
        <p className="text-center text-muted text-xs mt-4">Aplikacja trenera tenisa stołowego</p>
      </div>
    </div>
  );
}
