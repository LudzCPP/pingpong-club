import { useEffect, useState } from 'react';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';
import Avatar from '../components/Avatar';
import ConfirmDialog from '../components/ConfirmDialog';
import { Link, Copy, Check, UserX, UserMinus, Mail, Send, Ghost, UserPlus, X } from 'lucide-react';

const inputCls = 'flex-1 bg-base border border-border rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-accent placeholder-slate-600';

function VirtualInviteModal({ player, onClose, onSent }) {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await client.post(`/users/players/${player.id}/invite`, { email });
      onSent(email);
      onClose();
    } catch (err) {
      setError(err.response?.data?.detail || 'Nie udało się wysłać zaproszenia.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 px-4">
      <div className="bg-surface border border-border rounded-2xl p-6 w-full max-w-md shadow-2xl">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-white font-semibold">Zaproś do konta</h2>
          <button onClick={onClose} className="text-muted hover:text-white transition-colors"><X size={18} /></button>
        </div>
        <p className="text-sm text-muted mb-4">
          Wyślij zaproszenie do <strong className="text-white">{player.firstName} {player.lastName}</strong> —
          po rejestracji przejmie historię treningów.
        </p>
        <form onSubmit={handleSubmit} className="space-y-4">
          <input
            type="email"
            required
            autoFocus
            placeholder="email@zawodnik.pl"
            value={email}
            onChange={e => setEmail(e.target.value)}
            className="w-full bg-base border border-border rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-accent placeholder-slate-600"
          />
          {error && <p className="text-red-400 text-sm">{error}</p>}
          <div className="flex gap-3 justify-end">
            <button type="button" onClick={onClose} className="text-sm text-muted hover:text-white px-4 py-2 transition-colors">
              Anuluj
            </button>
            <button
              type="submit"
              disabled={loading || !email}
              className="flex items-center gap-2 bg-accent hover:bg-green-600 disabled:opacity-50 text-white font-semibold px-4 py-2 rounded-lg transition-colors text-sm"
            >
              <Send size={13} />
              {loading ? 'Wysyłanie...' : 'Wyślij link'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function AddVirtualPlayerModal({ onClose, onCreated }) {
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const { data } = await client.post('/users/players/virtual', { firstName, lastName });
      onCreated(data);
      onClose();
    } catch (err) {
      setError(err.response?.data?.detail || 'Nie udało się dodać zawodnika.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 px-4">
      <div className="bg-surface border border-border rounded-2xl p-6 w-full max-w-md shadow-2xl">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-white font-semibold">Dodaj zawodnika</h2>
          <button onClick={onClose} className="text-muted hover:text-white transition-colors"><X size={18} /></button>
        </div>
        <p className="text-sm text-muted mb-4">
          Zawodnik nie potrzebuje konta — możesz go zaprosić do rejestracji później.
        </p>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="flex gap-3">
            <input
              type="text"
              required
              autoFocus
              placeholder="Imię"
              value={firstName}
              onChange={e => setFirstName(e.target.value)}
              className="flex-1 bg-base border border-border rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-accent placeholder-slate-600"
            />
            <input
              type="text"
              required
              placeholder="Nazwisko"
              value={lastName}
              onChange={e => setLastName(e.target.value)}
              className="flex-1 bg-base border border-border rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-accent placeholder-slate-600"
            />
          </div>
          {error && <p className="text-red-400 text-sm">{error}</p>}
          <div className="flex gap-3 justify-end">
            <button type="button" onClick={onClose} className="text-sm text-muted hover:text-white px-4 py-2 transition-colors">
              Anuluj
            </button>
            <button
              type="submit"
              disabled={loading || !firstName || !lastName}
              className="flex items-center gap-2 bg-accent hover:bg-green-600 disabled:opacity-50 text-white font-semibold px-4 py-2 rounded-lg transition-colors text-sm"
            >
              <UserPlus size={13} />
              {loading ? 'Dodawanie...' : 'Dodaj'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function PlayersPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const isCoach = user?.role === 'COACH';

  const [players, setPlayers] = useState([]);
  const [loading, setLoading] = useState(true);

  const [invite, setInvite] = useState(null);
  const [inviteLoading, setInviteLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  const [joinEmail, setJoinEmail] = useState('');
  const [joinLoading, setJoinLoading] = useState(false);
  const [joinResult, setJoinResult] = useState(null);

  const [confirmDeactivate, setConfirmDeactivate] = useState(null);
  const [confirmRemove, setConfirmRemove] = useState(null);
  const [showAddVirtual, setShowAddVirtual] = useState(false);
  const [virtualInvitePlayer, setVirtualInvitePlayer] = useState(null);
  const [inviteSentMsg, setInviteSentMsg] = useState('');

  async function fetchPlayers() {
    const { data } = await client.get('/users/players');
    setPlayers(data);
  }

  useEffect(() => {
    async function load() {
      try { await fetchPlayers(); } finally { setLoading(false); }
    }
    load();
  }, []);

  async function handleGenerateInvite() {
    setInviteLoading(true);
    setInvite(null);
    try {
      const { data } = await client.post('/auth/invite');
      setInvite(data);
      setCopied(false);
    } finally {
      setInviteLoading(false);
    }
  }

  function handleCopy() {
    navigator.clipboard.writeText(invite.inviteUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  async function handleSendJoinRequest(e) {
    e.preventDefault();
    setJoinLoading(true);
    setJoinResult(null);
    try {
      await client.post('/join-requests', { playerEmail: joinEmail });
      setJoinResult({ ok: true, message: `Zaproszenie wysłane do ${joinEmail}` });
      setJoinEmail('');
    } catch (err) {
      const msg = err.response?.data?.detail || 'Nie udało się wysłać zaproszenia';
      setJoinResult({ ok: false, message: msg });
    } finally {
      setJoinLoading(false);
    }
  }

  async function handleDeactivate(id) {
    await client.delete(`/users/${id}`);
    setConfirmDeactivate(null);
    await fetchPlayers();
  }

  async function handleRemove(id) {
    await client.delete(`/users/players/${id}`);
    setConfirmRemove(null);
    await fetchPlayers();
  }

  if (loading) return <div className="max-w-6xl mx-auto px-6 py-12 text-center text-muted">Ładowanie...</div>;

  const active = players.filter(p => p.active && !p.virtual).length;
  const virtual = players.filter(p => p.virtual).length;

  return (
    <>
      {confirmDeactivate && (
        <ConfirmDialog
          message={`Dezaktywować konto zawodnika ${confirmDeactivate.firstName} ${confirmDeactivate.lastName}?`}
          confirmLabel="Dezaktywuj"
          onConfirm={() => handleDeactivate(confirmDeactivate.id)}
          onCancel={() => setConfirmDeactivate(null)}
        />
      )}
      {confirmRemove && (
        <ConfirmDialog
          message={`Usunąć ${confirmRemove.firstName} ${confirmRemove.lastName} ze swoich zawodników?`}
          confirmLabel="Usuń"
          onConfirm={() => handleRemove(confirmRemove.id)}
          onCancel={() => setConfirmRemove(null)}
        />
      )}
      {showAddVirtual && (
        <AddVirtualPlayerModal
          onClose={() => setShowAddVirtual(false)}
          onCreated={p => { setPlayers(prev => [...prev, p]); }}
        />
      )}
      {virtualInvitePlayer && (
        <VirtualInviteModal
          player={virtualInvitePlayer}
          onClose={() => setVirtualInvitePlayer(null)}
          onSent={email => setInviteSentMsg(`Link do rejestracji wysłany na ${email}`)}
        />
      )}

      <div className="max-w-6xl mx-auto px-6 py-8 space-y-6">

        {/* Header */}
        <div className="flex items-center justify-between flex-wrap gap-3">
          <div>
            <h1 className="text-2xl font-bold text-white">Zawodnicy</h1>
            <p className="text-sm text-muted mt-1">
              {active} aktywnych · {virtual > 0 && `${virtual} bez konta · `}{players.length} łącznie
            </p>
          </div>
          <div className="flex gap-2">
            {isCoach && (
              <button
                onClick={() => setShowAddVirtual(true)}
                className="flex items-center gap-2 bg-surface border border-border hover:border-accent/50 text-white font-semibold px-4 py-2 rounded-lg transition-colors text-sm"
              >
                <Ghost size={14} className="text-slate-400" />
                Dodaj bez konta
              </button>
            )}
            <button
              onClick={handleGenerateInvite}
              disabled={inviteLoading}
              className="flex items-center gap-2 bg-accent hover:bg-green-600 disabled:opacity-50 text-white font-semibold px-4 py-2 rounded-lg transition-colors text-sm"
            >
              <Link size={14} />
              {inviteLoading ? 'Generowanie...' : 'Nowe konto (link)'}
            </button>
          </div>
        </div>

        {inviteSentMsg && (
          <div className="bg-green-500/10 border border-green-500/30 text-green-400 text-sm px-4 py-3 rounded-lg flex items-center justify-between">
            {inviteSentMsg}
            <button onClick={() => setInviteSentMsg('')} className="text-green-400 hover:text-white"><X size={14} /></button>
          </div>
        )}

        {/* Invite link panel */}
        {invite && (
          <div className="bg-surface border border-border border-l-4 border-l-accent rounded-xl p-5">
            <p className="text-xs font-medium text-muted uppercase tracking-wide mb-3">Link zaproszenia (ważny 48h)</p>
            <div className="flex items-center gap-3">
              <input
                readOnly
                value={invite.inviteUrl}
                className="flex-1 bg-base border border-border rounded-lg px-3 py-2 text-white text-sm focus:outline-none font-mono truncate"
              />
              <button
                onClick={handleCopy}
                title="Kopiuj link"
                className={`flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors border ${
                  copied
                    ? 'bg-accent/20 border-accent/40 text-accent'
                    : 'bg-base border-border text-muted hover:text-white hover:border-slate-500'
                }`}
              >
                {copied ? <><Check size={14} /> Skopiowano</> : <><Copy size={14} /> Kopiuj</>}
              </button>
            </div>
            <p className="text-xs text-muted mt-2">
              Wygasa: {new Date(invite.expiresAt).toLocaleString('pl-PL', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}
            </p>
          </div>
        )}

        {/* Join request by email */}
        <div className="bg-surface border border-border rounded-xl p-5">
          <p className="text-xs font-medium text-muted uppercase tracking-wide mb-3 flex items-center gap-2">
            <Mail size={13} />
            Zaproś istniejącego zawodnika
          </p>
          <form onSubmit={handleSendJoinRequest} className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
            <input
              type="email"
              required
              placeholder="email@zawodnik.pl"
              value={joinEmail}
              onChange={e => { setJoinEmail(e.target.value); setJoinResult(null); }}
              className={inputCls}
            />
            <button
              type="submit"
              disabled={joinLoading || !joinEmail}
              className="flex items-center justify-center gap-2 bg-accent hover:bg-green-600 disabled:opacity-50 text-white font-semibold px-4 py-2 rounded-lg transition-colors text-sm whitespace-nowrap"
            >
              <Send size={13} />
              {joinLoading ? 'Wysyłanie...' : 'Wyślij zaproszenie'}
            </button>
          </form>
          {joinResult && (
            <p className={`text-sm mt-2 ${joinResult.ok ? 'text-green-400' : 'text-red-400'}`}>
              {joinResult.message}
            </p>
          )}
        </div>

        {/* Player cards grid */}
        {players.length === 0 ? (
          <div className="text-center text-muted py-16">Brak zawodników</div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {players.map(p => (
              <div key={p.id} className={`bg-surface border rounded-xl p-5 flex flex-col gap-4 transition-colors ${
                p.virtual ? 'border-slate-600 hover:border-slate-500' : 'border-border hover:border-accent/40'
              }`}>
                <div className="flex items-center gap-4">
                  <div className="relative">
                    <Avatar firstName={p.firstName} lastName={p.lastName} size="lg" />
                    {p.virtual && (
                      <div className="absolute -bottom-1 -right-1 w-5 h-5 bg-slate-700 border border-slate-500 rounded-full flex items-center justify-center">
                        <Ghost size={10} className="text-slate-400" />
                      </div>
                    )}
                  </div>
                  <div className="min-w-0">
                    <p className="text-white font-semibold text-lg leading-tight">{p.firstName} {p.lastName}</p>
                    {p.virtual
                      ? <p className="text-slate-500 text-sm mt-0.5 italic">Bez konta</p>
                      : <p className="text-muted text-sm truncate mt-0.5">{p.email}</p>
                    }
                  </div>
                </div>
                <div className="flex items-center justify-between pt-2 border-t border-border">
                  <span className={`text-xs font-semibold px-2.5 py-1 rounded-full border ${
                    p.virtual
                      ? 'bg-slate-500/20 text-slate-400 border-slate-500/30'
                      : p.active
                        ? 'bg-green-500/20 text-green-300 border-green-500/30'
                        : 'bg-slate-500/20 text-slate-400 border-slate-500/30'
                  }`}>
                    {p.virtual ? 'Bez konta' : p.active ? 'Aktywny' : 'Nieaktywny'}
                  </span>
                  <div className="flex items-center gap-1">
                    {p.virtual && isCoach && (
                      <button
                        onClick={() => setVirtualInvitePlayer(p)}
                        className="flex items-center gap-1.5 text-xs text-muted hover:text-accent transition-colors px-2 py-1"
                        title="Wyślij zaproszenie do rejestracji"
                      >
                        <Send size={12} />
                        Zaproś
                      </button>
                    )}
                    {isCoach && (
                      <button
                        onClick={() => setConfirmRemove(p)}
                        className="flex items-center gap-1.5 text-xs text-muted hover:text-orange-400 transition-colors px-2 py-1"
                        title="Usuń ze swoich zawodników"
                      >
                        <UserMinus size={12} />
                        Usuń
                      </button>
                    )}
                    {p.active && !p.virtual && isAdmin && (
                      <button
                        onClick={() => setConfirmDeactivate(p)}
                        className="flex items-center gap-1.5 text-xs text-muted hover:text-red-400 transition-colors px-2 py-1"
                      >
                        <UserX size={12} />
                        Dezaktywuj
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </>
  );
}
