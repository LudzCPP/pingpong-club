import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Target } from 'lucide-react';
import client from '../api/client';
import { usePageTitle } from '../hooks/usePageTitle';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const inputCls = 'w-full bg-base border border-border rounded-lg px-4 py-2.5 text-white placeholder-muted focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent transition-colors';

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await client.post('/auth/forgot-password', { email });
      setSent(true);
    } catch {
      setError('Wystąpił błąd. Spróbuj ponownie.');
    } finally {
      setLoading(false);
    }
  }

  usePageTitle('Resetowanie hasła');

  return (
    <div className="min-h-screen bg-base flex items-center justify-center px-4"
         style={{ backgroundImage: 'radial-gradient(circle at 20% 50%, #1e293b 0%, transparent 50%), radial-gradient(circle at 80% 20%, #0f3460 0%, transparent 40%)' }}>
      <div className="w-full max-w-md">
        <div className="bg-surface border border-border rounded-2xl p-8 shadow-2xl">
          <div className="text-center mb-8">
            <div className="w-16 h-16 bg-accent/20 border border-accent/30 rounded-2xl flex items-center justify-center mx-auto mb-4">
              <Target size={32} className="text-accent" />
            </div>
            <h1 className="text-2xl font-bold text-white">Zapomniałem hasła</h1>
            <p className="text-muted text-sm mt-1">Wyślemy Ci link do ustawienia nowego hasła</p>
          </div>

          {sent ? (
            <div className="text-center space-y-4">
              <div className="bg-green-500/10 border border-green-500/30 text-green-400 text-sm px-4 py-4 rounded-lg">
                Jeśli podany adres istnieje w systemie, wysłaliśmy na niego link do resetowania hasła.
                Sprawdź swoją skrzynkę (i folder spam).
              </div>
              <Link to="/login" className="block text-accent hover:text-green-400 text-sm transition-colors">
                Wróć do logowania
              </Link>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-5">
              <div className="space-y-1.5">
                <label className="text-sm font-medium text-muted">Adres email</label>
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
                {loading ? 'Wysyłanie...' : 'Wyślij link'}
              </button>
              <div className="text-center">
                <Link to="/login" className="text-muted hover:text-white text-sm transition-colors">
                  Wróć do logowania
                </Link>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
