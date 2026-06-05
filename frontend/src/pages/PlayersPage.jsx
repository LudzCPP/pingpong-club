import { useEffect, useState } from 'react';
import client from '../api/client';
import Avatar from '../components/Avatar';
import ConfirmDialog from '../components/ConfirmDialog';
import { Link, Copy, Check, UserX } from 'lucide-react';

export default function PlayersPage() {
  const [players, setPlayers] = useState([]);
  const [loading, setLoading] = useState(true);

  const [invite, setInvite] = useState(null); // { inviteUrl, expiresAt }
  const [inviteLoading, setInviteLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  const [confirmDeactivate, setConfirmDeactivate] = useState(null); // player object

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

  async function handleDeactivate(id) {
    await client.delete(`/users/${id}`);
    setConfirmDeactivate(null);
    await fetchPlayers();
  }

  if (loading) return <div className="max-w-6xl mx-auto px-6 py-12 text-center text-muted">Ładowanie...</div>;

  const active = players.filter(p => p.active).length;

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

      <div className="max-w-6xl mx-auto px-6 py-8 space-y-6">

        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-white">Zawodnicy</h1>
            <p className="text-sm text-muted mt-1">{active} aktywnych · {players.length} łącznie</p>
          </div>
          <button
            onClick={handleGenerateInvite}
            disabled={inviteLoading}
            className="flex items-center gap-2 bg-accent hover:bg-green-600 disabled:opacity-50 text-white font-semibold px-4 py-2 rounded-lg transition-colors text-sm"
          >
            <Link size={14} />
            {inviteLoading ? 'Generowanie...' : 'Wygeneruj zaproszenie'}
          </button>
        </div>

        {/* Invite panel */}
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
                      onClick={() => setConfirmDeactivate(p)}
                      className="flex items-center gap-1.5 text-xs text-muted hover:text-red-400 transition-colors px-2 py-1"
                    >
                      <UserX size={12} />
                      Dezaktywuj
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </>
  );
}
