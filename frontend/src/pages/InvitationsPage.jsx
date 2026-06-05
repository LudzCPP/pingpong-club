import { useEffect, useState } from 'react';
import client from '../api/client';
import { Check, X, UserCheck } from 'lucide-react';

function formatDate(iso) {
  return new Date(iso).toLocaleString('pl-PL', {
    day: 'numeric', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

export default function InvitationsPage() {
  const [invitations, setInvitations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(null); // requestId being processed

  async function fetchInvitations() {
    const { data } = await client.get('/join-requests/pending');
    setInvitations(data);
  }

  useEffect(() => {
    async function load() {
      try { await fetchInvitations(); } finally { setLoading(false); }
    }
    load();
  }, []);

  async function handleAccept(id) {
    setActionLoading(id);
    try {
      await client.patch(`/join-requests/${id}/accept`);
      setInvitations(prev => prev.filter(i => i.id !== id));
    } finally {
      setActionLoading(null);
    }
  }

  async function handleReject(id) {
    setActionLoading(id);
    try {
      await client.patch(`/join-requests/${id}/reject`);
      setInvitations(prev => prev.filter(i => i.id !== id));
    } finally {
      setActionLoading(null);
    }
  }

  if (loading) return <div className="max-w-6xl mx-auto px-6 py-12 text-center text-muted">Ładowanie...</div>;

  return (
    <div className="max-w-6xl mx-auto px-6 py-8 space-y-6">

      <div>
        <h1 className="text-2xl font-bold text-white">Zaproszenia od trenerów</h1>
        <p className="text-sm text-muted mt-1">
          {invitations.length > 0
            ? `${invitations.length} oczekujących`
            : 'Brak oczekujących zaproszeń'}
        </p>
      </div>

      {invitations.length === 0 ? (
        <div className="flex flex-col items-center gap-4 py-20 text-muted">
          <UserCheck size={48} className="text-slate-600" />
          <p className="text-lg">Żaden trener nie wysłał Ci jeszcze zaproszenia</p>
        </div>
      ) : (
        <div className="flex flex-col gap-3">
          {invitations.map(inv => (
            <div
              key={inv.id}
              className="bg-surface border border-border rounded-xl p-5 flex items-center gap-5 hover:border-accent/40 transition-colors"
            >
              <div className="w-10 h-10 rounded-full bg-accent/20 flex items-center justify-center text-accent font-bold text-sm shrink-0">
                {inv.coachFirstName[0]}{inv.coachLastName[0]}
              </div>

              <div className="flex-1 min-w-0">
                <p className="text-white font-semibold">
                  {inv.coachFirstName} {inv.coachLastName}
                </p>
                <p className="text-sm text-muted truncate">{inv.coachEmail}</p>
                <p className="text-xs text-slate-600 mt-0.5">Wysłano: {formatDate(inv.createdAt)}</p>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                <button
                  onClick={() => handleReject(inv.id)}
                  disabled={actionLoading === inv.id}
                  className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium border border-border text-muted hover:text-red-400 hover:border-red-500/40 disabled:opacity-50 transition-colors"
                >
                  <X size={14} />
                  Odrzuć
                </button>
                <button
                  onClick={() => handleAccept(inv.id)}
                  disabled={actionLoading === inv.id}
                  className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium bg-accent hover:bg-green-600 disabled:opacity-50 text-white transition-colors"
                >
                  <Check size={14} />
                  Akceptuj
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
