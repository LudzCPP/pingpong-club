import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(email, password);
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.detail || 'Błąd logowania. Sprawdź dane.');
    } finally {
      setLoading(false);
    }
  }

  const inputCls = 'w-full bg-base border border-border rounded-lg px-4 py-2.5 text-white placeholder-muted focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent transition-colors';

  return (
    <div className="min-h-screen bg-base flex items-center justify-center px-4"
         style={{ backgroundImage: 'radial-gradient(circle at 20% 50%, #1e293b 0%, transparent 50%), radial-gradient(circle at 80% 20%, #0f3460 0%, transparent 40%)' }}>

      <div className="w-full max-w-md">
        <div className="bg-surface border border-border rounded-2xl p-8 shadow-2xl">
          <div className="text-center mb-8">
            <div className="text-5xl mb-3">🏓</div>
            <h1 className="text-2xl font-bold text-white">PingPong Club</h1>
            <p className="text-muted text-sm mt-1">Zaloguj się do systemu trenera</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-muted">Email</label>
              <input
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                placeholder="trener@pingpong.pl"
                required
                autoFocus
                className={inputCls}
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-muted">Hasło</label>
              <input
                type="password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                placeholder="••••••••"
                required
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
              {loading ? 'Logowanie...' : 'Zaloguj się'}
            </button>
          </form>
        </div>
        <p className="text-center text-muted text-xs mt-4">Aplikacja trenera tenisa stołowego</p>
      </div>
    </div>
  );
}
