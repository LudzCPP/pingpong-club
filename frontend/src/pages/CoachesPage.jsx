import { useEffect, useState } from 'react';
import client from '../api/client';
import Avatar from '../components/Avatar';
import ConfirmDialog from '../components/ConfirmDialog';
import { Link, Copy, Check, UserX } from 'lucide-react';

export default function CoachesPage() {
  const [coaches, setCoaches] = useState([]);
  const [loading, setLoading] = useState(true);

  const [invite, setInvite] = useState(null);
  const [inviteLoading, setInviteLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  const [confirmDeactivate, setConfirmDeactivate] = useState(null);

  async function fetchCoaches() {
    const { data } = await client.get('/users/coaches');
    setCoaches(data);
  }

  useEffect(() => {
    async function load() {
      try { await fetchCoaches(); } finally { setLoading(false); }
    }
    load();
  }, []);

  async function handleGenerateInvite() {
    setInviteLoading(true);
    setInvite(null);
    try {
      const { data } = await client.post('/auth/invite/coaches');
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
    await fetchCoaches();
  }

  if (loading) return <div className="max-w-6xl mx-auto px-6 py-12 text-center text-muted">Ładowanie...</div>;

  const active = coaches.filter(c => c.active).length;

  return (
    <>
      {confirmDeactivate && (
        <ConfirmDialog
          message={`Dezaktywować konto trenera ${confirmDeactivate.firstName} ${confirmDeactivate.lastName}?`}
          confirmLabel="Dezaktywuj"
          onConfirm={() => handleDeactivate(confirmDeactivate.id)}
          onCancel={() => setConfirmDeactivate(null)}
        />
      )}

      <div className="max-w-6xl mx-auto px-6 py-8 space-y-6">

        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-white">Trenerzy</h1>
            <p className="text-sm text-muted mt-1">{active} aktywnych · {coaches.length} łącznie</p>
          </div>
          <button
            onClick={handleGenerateInvite}
            disabled={inviteLoading}
            className="flex items-center gap-2 bg-orange-500 hover:bg-orange-600 disabled:opacity-50 text-white font-semibold px-4 py-2 rounded-lg transition-colors text-sm"
          >
            <Link size={14} />
            {inviteLoading ? 'Generowanie...' : 'Nowe konto trenera'}
          </button>
        </div>

        {/* Invite link panel */}
        {invite && (
          <div className="bg-surface border border-border border-l-4 border-l-orange-500 rounded-xl p-5">
            <p className="text-xs font-medium text-muted uppercase tracking-wide mb-3">Link zaproszenia dla trenera (ważny 48h)</p>
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
                    ? 'bg-orange-500/20 border-orange-500/40 text-orange-300'
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

        {/* Coaches grid */}
        {coaches.length === 0 ? (
          <div className="text-center text-muted py-16">Brak trenerów</div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {coaches.map(c => (
              <div key={c.id} className="bg-surface border border-border rounded-xl p-5 flex flex-col gap-4 hover:border-orange-500/40 transition-colors">
                <div className="flex items-center gap-4">
                  <Avatar firstName={c.firstName} lastName={c.lastName} size="lg" />
                  <div className="min-w-0">
                    <p className="text-white font-semibold text-lg leading-tight">{c.firstName} {c.lastName}</p>
                    <p className="text-muted text-sm truncate mt-0.5">{c.email}</p>
                  </div>
                </div>
                <div className="flex items-center justify-between pt-2 border-t border-border">
                  <span className={`text-xs font-semibold px-2.5 py-1 rounded-full border ${
                    c.active
                      ? 'bg-accent/20 text-accent border-accent/30'
                      : 'bg-slate-500/20 text-slate-400 border-slate-500/30'
                  }`}>
                    {c.active ? 'Aktywny' : 'Nieaktywny'}
                  </span>
                  {c.active && (
                    <button
                      onClick={() => setConfirmDeactivate(c)}
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
