import { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Target } from 'lucide-react';
import { Button, Card, Input, Alert } from '../components/ui';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
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

  return (
    <div className="min-h-screen bg-base flex items-center justify-center px-4"
         style={{ backgroundImage: 'radial-gradient(circle at 20% 50%, #1e293b 0%, transparent 50%), radial-gradient(circle at 80% 20%, #0f3460 0%, transparent 40%)' }}>

      <div className="w-full max-w-md">
        <Card className="rounded-2xl p-8 shadow-2xl">
          <div className="text-center mb-8">
            <div className="w-16 h-16 bg-accent/20 border border-accent/30 rounded-2xl flex items-center justify-center mx-auto mb-4">
              <Target size={32} className="text-accent" />
            </div>
            <h1 className="text-2xl font-bold text-white">TTManager</h1>
            <p className="text-muted text-sm mt-1">Zaloguj się do systemu trenera</p>
          </div>

          {location.state?.passwordReset && (
            <Alert variant="success" className="mb-6">
              Hasło zostało zmienione. Możesz się teraz zalogować.
            </Alert>
          )}

          {location.state?.registered && (
            <Alert variant="success" className="mb-6">
              Konto zostało utworzone. Możesz się teraz zalogować.
            </Alert>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <Input
              label="Email"
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              placeholder="trener@pingpong.pl"
              required
              autoFocus
            />
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label htmlFor="login-password" className="text-sm font-medium text-muted">Hasło</label>
                <Link to="/forgot-password" className="text-xs text-muted hover:text-accent transition-colors">
                  Nie pamiętasz hasła?
                </Link>
              </div>
              <Input
                id="login-password"
                type="password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                placeholder="••••••••"
                required
              />
            </div>
            {error && <Alert variant="danger">{error}</Alert>}
            <Button type="submit" disabled={loading} className="w-full mt-2">
              {loading ? 'Logowanie...' : 'Zaloguj się'}
            </Button>
          </form>
        </Card>
        <p className="text-center text-muted text-xs mt-4">Aplikacja trenera tenisa stołowego</p>
      </div>
    </div>
  );
}
