import { useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { Target } from 'lucide-react';
import client from '../api/client';

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token') || '';

  const [newPassword, setNewPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const inputCls = 'w-full bg-base border border-border rounded-lg px-4 py-2.5 text-white placeholder-muted focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent transition-colors';

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');

    if (newPassword.length < 8) {
      setError('Hasło musi mieć co najmniej 8 znaków.');
      return;
    }
    if (newPassword !== confirm) {
      setError('Hasła nie są identyczne.');
      return;
    }

    setLoading(true);
    try {
      await client.post('/auth/reset-password', { token, newPassword });
      navigate('/login', { state: { passwordReset: true } });
    } catch (err) {
      setError(err.response?.data?.detail || 'Link jest nieprawidłowy lub wygasł.');
    } finally {
      setLoading(false);
    }
  }

  if (!token) {
    return (
      <div className="min-h-screen bg-base flex items-center justify-center px-4">
        <div className="bg-surface border border-border rounded-2xl p-8 shadow-2xl text-center">
          <p className="text-red-400 mb-4">Brak tokenu resetowania hasła.</p>
          <Link to="/login" className="text-accent hover:text-green-400 text-sm">Wróć do logowania</Link>
        </div>
      </div>
    );
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
            <h1 className="text-2xl font-bold text-white">Nowe hasło</h1>
            <p className="text-muted text-sm mt-1">Ustaw nowe hasło do swojego konta</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-muted">Nowe hasło</label>
              <input
                type="password"
                value={newPassword}
                onChange={e => setNewPassword(e.target.value)}
                placeholder="Minimum 8 znaków"
                required
                autoFocus
                className={inputCls}
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-muted">Powtórz hasło</label>
              <input
                type="password"
                value={confirm}
                onChange={e => setConfirm(e.target.value)}
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
              className="w-full bg-accent hover:bg-green-600 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold py-2.5 rounded-lg transition-colors"
            >
              {loading ? 'Zapisywanie...' : 'Ustaw hasło'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
