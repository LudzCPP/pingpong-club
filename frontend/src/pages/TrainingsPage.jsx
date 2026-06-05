import { useEffect, useState, useRef } from 'react';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';
import Avatar from '../components/Avatar';
import StatusBadge from '../components/StatusBadge';
import { Check, X, Plus, Mic, MicOff, Sparkles, Loader, Banknote } from 'lucide-react';

const FILTERS = ['Wszystkie', 'SCHEDULED', 'COMPLETED', 'CANCELLED'];
const FILTER_LABEL = { Wszystkie: 'Wszystkie', SCHEDULED: 'Zaplanowane', COMPLETED: 'Zrealizowane', CANCELLED: 'Odwołane' };

const inputCls = 'bg-base border border-border rounded-lg px-3 py-2 text-white text-sm placeholder-muted focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent transition-colors w-full';
const labelCls = 'text-xs font-medium text-muted uppercase tracking-wide';

const EMPTY_FORM = { playerId: '', scheduledAt: '', durationMinutes: 60, totalPrice: '', notes: '' };

function formatForDatetimeInput(isoString) {
  if (!isoString) return '';
  return isoString.slice(0, 16);
}

export default function TrainingsPage() {
  const { user } = useAuth();
  const isCoach = user?.role === 'COACH';

  const [trainings, setTrainings] = useState([]);
  const [players, setPlayers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('Wszystkie');

  // Manual form
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [aiFilledLabel, setAiFilledLabel] = useState('');

  // Complete with notes modal
  const [completing, setCompleting] = useState(null); // { id, notes }

  // AI panel
  const [showAiPanel, setShowAiPanel] = useState(false);
  const [aiText, setAiText] = useState('');
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState('');
  const [isRecording, setIsRecording] = useState(false);
  const recognitionRef = useRef(null);

  async function fetchTrainings() {
    const { data } = await client.get('/trainings');
    setTrainings(data);
  }

  useEffect(() => {
    async function load() {
      try {
        await fetchTrainings();
        if (isCoach) {
          const { data } = await client.get('/users/players');
          setPlayers(data);
        }
      } finally { setLoading(false); }
    }
    load();
  }, [isCoach]);

  // ── Voice recording ───────────────────────────────────────────────────────

  function toggleRecording() {
    const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SR) {
      setAiError('Przeglądarka nie obsługuje rozpoznawania mowy. Użyj Chrome na telefonie lub wpisz tekst ręcznie.');
      return;
    }

    if (isRecording) {
      recognitionRef.current?.stop();
      setIsRecording(false);
      return;
    }

    const rec = new SR();
    rec.lang = 'pl-PL';
    rec.interimResults = false;
    rec.maxAlternatives = 1;

    rec.onresult = (e) => {
      const transcript = e.results[0][0].transcript;
      setAiText(prev => prev ? prev + ' ' + transcript : transcript);
    };
    rec.onerror = () => setIsRecording(false);
    rec.onend = () => setIsRecording(false);

    recognitionRef.current = rec;
    rec.start();
    setIsRecording(true);
    setAiError('');
  }

  // ── AI parse ──────────────────────────────────────────────────────────────

  async function handleAiParse() {
    if (!aiText.trim()) return;
    setAiLoading(true);
    setAiError('');
    try {
      const { data } = await client.post('/trainings/parse', { text: aiText.trim() });

      setForm({
        playerId:        data.playerId ?? '',
        scheduledAt:     formatForDatetimeInput(data.scheduledAt),
        durationMinutes: data.durationMinutes ?? 60,
        totalPrice:      data.totalPrice != null ? String(data.totalPrice) : '',
        notes:           data.notes ?? '',
      });

      setAiFilledLabel(data.playerName
        ? `Dane z AI dla: ${data.playerName}`
        : 'Dane z AI — sprawdź i uzupełnij brakujące pola');

      setShowAiPanel(false);
      setShowForm(true);
      setAiText('');
    } catch (err) {
      setAiError(err.response?.data?.detail || 'Błąd analizy tekstu.');
    } finally {
      setAiLoading(false);
    }
  }

  // ── Manual form submit ────────────────────────────────────────────────────

  async function handleCreate(e) {
    e.preventDefault();
    setFormError('');
    setSaving(true);
    try {
      const duration = Number(form.durationMinutes);
      const total = Number(form.totalPrice);
      await client.post('/trainings', {
        playerId: form.playerId,
        scheduledAt: form.scheduledAt,
        durationMinutes: duration,
        hourlyRate: duration > 0 ? (total * 60) / duration : total,
        notes: form.notes,
      });
      setShowForm(false);
      setShowAiPanel(false);
      setAiFilledLabel('');
      setForm(EMPTY_FORM);
      await fetchTrainings();
    } catch (err) {
      setFormError(err.response?.data?.detail || 'Błąd zapisu treningu.');
    } finally { setSaving(false); }
  }

  async function handleComplete(id, notes) {
    await client.patch(`/trainings/${id}/complete`, notes?.trim() ? { notes } : null);
    setCompleting(null);
    await fetchTrainings();
  }

  async function handleCancel(id) {
    await client.patch(`/trainings/${id}/cancel`);
    await fetchTrainings();
  }

  async function handleTogglePaid(id) {
    await client.patch(`/trainings/${id}/paid`);
    await fetchTrainings();
  }

  function openManualForm() {
    setShowAiPanel(false);
    setAiFilledLabel('');
    setForm(EMPTY_FORM);
    setShowForm(s => !s);
  }

  function openAiPanel() {
    setShowForm(false);
    setAiFilledLabel('');
    setShowAiPanel(s => !s);
    setAiError('');
  }

  const visible = filter === 'Wszystkie' ? trainings : trainings.filter(t => t.status === filter);
  const scheduled = trainings.filter(t => t.status === 'SCHEDULED').length;
  const completed = trainings.filter(t => t.status === 'COMPLETED').length;

  if (loading) return <div className="max-w-6xl mx-auto px-6 py-12 text-center text-muted">Ładowanie...</div>;

  return (
    <>
    {/* Complete training modal */}
    {completing && (
      <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
        <div className="bg-surface border border-border rounded-xl p-6 w-full max-w-md shadow-2xl">
          <h3 className="text-white font-semibold mb-1">Zakończ trening</h3>
          <p className="text-sm text-muted mb-4">Dodaj notatkę z sesji — co ćwiczyliście? (opcjonalnie)</p>
          <textarea
            value={completing.notes}
            onChange={e => setCompleting(c => ({ ...c, notes: e.target.value }))}
            placeholder="np. Skupiliśmy się na bekhendie, świetna praca nad serwisem..."
            rows={3}
            className="w-full bg-base border border-border rounded-lg px-3 py-2 text-white text-sm placeholder-muted focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent transition-colors resize-none"
          />
          <div className="flex gap-3 mt-4">
            <button
              onClick={() => handleComplete(completing.id, completing.notes)}
              className="flex items-center gap-2 bg-accent hover:bg-green-600 text-white font-semibold px-5 py-2 rounded-lg transition-colors text-sm"
            >
              <Check size={14} /> Zakończ trening
            </button>
            <button
              onClick={() => setCompleting(null)}
              className="text-sm text-muted hover:text-white px-3 py-2 rounded-lg transition-colors"
            >
              Anuluj
            </button>
          </div>
        </div>
      </div>
    )}
    <div className="max-w-6xl mx-auto px-6 py-8 space-y-6">

      {/* Header */}
      <div className="flex items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-white">Treningi</h1>
          <p className="text-sm text-muted mt-1">
            <span className="text-blue-300">{scheduled} zaplanowanych</span>
            <span className="mx-2 text-border">·</span>
            <span className="text-green-300">{completed} zrealizowanych</span>
          </p>
        </div>
        {isCoach && (
          <div className="flex items-center gap-2">
            <button
              onClick={openAiPanel}
              title="Dodaj trening głosowo lub przez AI"
              className={`flex items-center gap-2 px-3 py-2 rounded-lg border text-sm font-medium transition-colors ${
                showAiPanel
                  ? 'bg-blue-500/20 border-blue-500/50 text-blue-300'
                  : 'bg-surface border-border text-muted hover:text-white hover:border-slate-500'
              }`}
            >
              <Sparkles size={14} />
              <span className="hidden sm:inline">AI / Głos</span>
            </button>
            <button
              onClick={openManualForm}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold transition-colors ${
                showForm && !showAiPanel
                  ? 'bg-surface border border-border text-muted'
                  : 'bg-accent hover:bg-green-600 text-white'
              }`}
            >
              {showForm ? <><X size={14} /> Anuluj</> : <><Plus size={14} /> Nowy trening</>}
            </button>
          </div>
        )}
      </div>

      {/* AI panel */}
      {showAiPanel && (
        <div className="bg-surface border border-blue-500/30 rounded-xl p-6 border-l-4 border-l-blue-500">
          <div className="flex items-center gap-2 mb-1">
            <Sparkles size={16} className="text-blue-400" />
            <h2 className="text-white font-semibold">Dodaj trening głosowo lub tekstem</h2>
          </div>
          <p className="text-xs text-muted mb-4">
            Np. <span className="text-slate-300 italic">"Łukasz jutro o 16, godzina, 120 złotych"</span>
            {' '}lub{' '}
            <span className="text-slate-300 italic">"Kowalski w środę o piętnastej trzydzieści, 45 minut, stówa"</span>
          </p>

          <div className="flex gap-3 items-start">
            <textarea
              value={aiText}
              onChange={e => setAiText(e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleAiParse(); } }}
              placeholder="Wpisz lub podyktuj opis treningu..."
              rows={2}
              className="flex-1 bg-base border border-border rounded-lg px-3 py-2 text-white text-sm placeholder-muted focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-colors resize-none"
            />
            <button
              onClick={toggleRecording}
              title={isRecording ? 'Zatrzymaj nagrywanie' : 'Nagraj głosowo'}
              className={`flex-shrink-0 w-10 h-10 rounded-lg flex items-center justify-center transition-colors border ${
                isRecording
                  ? 'bg-red-500/20 border-red-500/50 text-red-400 animate-pulse'
                  : 'bg-base border-border text-muted hover:text-white hover:border-slate-500'
              }`}
            >
              {isRecording ? <MicOff size={16} /> : <Mic size={16} />}
            </button>
          </div>

          {isRecording && (
            <p className="text-xs text-red-400 mt-2 flex items-center gap-1">
              <span className="w-1.5 h-1.5 bg-red-400 rounded-full inline-block animate-pulse" />
              Nagrywanie... mów po polsku
            </p>
          )}

          {aiError && (
            <div className="mt-3 bg-red-500/10 border border-red-500/30 text-red-400 text-sm px-4 py-3 rounded-lg">
              {aiError}
            </div>
          )}

          <div className="flex gap-3 mt-4">
            <button
              onClick={handleAiParse}
              disabled={!aiText.trim() || aiLoading}
              className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold px-5 py-2 rounded-lg transition-colors text-sm"
            >
              {aiLoading ? <><Loader size={14} className="animate-spin" /> Analizuję...</> : <><Sparkles size={14} /> Analizuj</>}
            </button>
            <button
              onClick={() => { setShowAiPanel(false); setAiText(''); setAiError(''); }}
              className="text-sm text-muted hover:text-white px-3 py-2 rounded-lg transition-colors"
            >
              Anuluj
            </button>
          </div>
        </div>
      )}

      {/* Manual form */}
      {showForm && (
        <div className="bg-surface border border-border rounded-xl p-6 border-l-4 border-l-accent">
          {aiFilledLabel && (
            <div className="flex items-center gap-2 mb-4 bg-blue-500/10 border border-blue-500/30 text-blue-300 text-xs px-3 py-2 rounded-lg">
              <Sparkles size={12} />
              {aiFilledLabel}
            </div>
          )}
          <h2 className="text-white font-semibold mb-5">Zaplanuj trening</h2>
          <form onSubmit={handleCreate} className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className={labelCls}>Zawodnik</label>
              <select value={form.playerId} onChange={e => setForm({ ...form, playerId: e.target.value })} required className={inputCls}>
                <option value="">-- wybierz --</option>
                {players.map(p => <option key={p.id} value={p.id}>{p.firstName} {p.lastName}</option>)}
              </select>
            </div>
            <div className="space-y-1.5">
              <label className={labelCls}>Data i godzina</label>
              <input type="datetime-local" value={form.scheduledAt} onChange={e => setForm({ ...form, scheduledAt: e.target.value })} required className={inputCls} />
            </div>
            <div className="space-y-1.5">
              <label className={labelCls}>Czas trwania (min)</label>
              <input type="number" min="15" max="480" value={form.durationMinutes} onChange={e => setForm({ ...form, durationMinutes: e.target.value })} required className={inputCls} />
            </div>
            <div className="space-y-1.5">
              <label className={labelCls}>Kwota za trening (zł)</label>
              <input type="number" min="0" step="0.01" value={form.totalPrice} onChange={e => setForm({ ...form, totalPrice: e.target.value })} required className={inputCls} />
            </div>
            <div className="space-y-1.5 sm:col-span-2">
              <label className={labelCls}>Notatki</label>
              <input type="text" value={form.notes} onChange={e => setForm({ ...form, notes: e.target.value })} placeholder="opcjonalnie" className={inputCls} />
            </div>
            {formError && <div className="sm:col-span-2 bg-red-500/10 border border-red-500/30 text-red-400 text-sm px-4 py-3 rounded-lg">{formError}</div>}
            <div className="sm:col-span-2 flex gap-3">
              <button type="submit" disabled={saving} className="bg-accent hover:bg-green-600 disabled:opacity-50 text-white font-semibold px-6 py-2 rounded-lg transition-colors text-sm">
                {saving ? 'Zapisywanie...' : 'Zaplanuj trening'}
              </button>
              <button type="button" onClick={() => { setShowForm(false); setAiFilledLabel(''); }} className="text-sm text-muted hover:text-white px-3 py-2 rounded-lg transition-colors">
                Anuluj
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Filter bar */}
      <div className="flex gap-2 flex-wrap">
        {FILTERS.map(f => (
          <button key={f} onClick={() => setFilter(f)}
            className={`px-4 py-1.5 rounded-full text-sm font-medium transition-colors ${
              filter === f
                ? 'bg-accent text-white'
                : 'bg-surface border border-border text-muted hover:text-white'
            }`}>
            {FILTER_LABEL[f]}
          </button>
        ))}
      </div>

      {/* Table */}
      <div className="bg-surface border border-border rounded-xl overflow-hidden">
        {visible.length === 0 ? (
          <p className="text-center text-muted py-12">Brak treningów</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border">
                <th className="text-left px-4 py-3 text-xs text-muted uppercase tracking-wide">Zawodnik</th>
                <th className="text-left px-4 py-3 text-xs text-muted uppercase tracking-wide">Data</th>
                <th className="text-left px-4 py-3 text-xs text-muted uppercase tracking-wide hidden sm:table-cell">Czas</th>
                <th className="text-left px-4 py-3 text-xs text-muted uppercase tracking-wide">Kwota</th>
                <th className="text-left px-4 py-3 text-xs text-muted uppercase tracking-wide">Status</th>
                {isCoach && <th className="px-4 py-3 text-xs text-muted uppercase tracking-wide">Akcje</th>}
              </tr>
            </thead>
            <tbody>
              {visible.map(t => (
                <tr key={t.id} className="border-b border-border/50 hover:bg-white/5 transition-colors last:border-0">
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2.5">
                      <Avatar firstName={t.playerFullName?.split(' ')[0]} lastName={t.playerFullName?.split(' ')[1]} size="sm" />
                      <div>
                        <p className="text-white font-medium">{t.playerFullName}</p>
                        <p className="text-muted text-xs">{t.name}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-muted whitespace-nowrap">
                    {new Date(t.scheduledAt).toLocaleString('pl-PL', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}
                  </td>
                  <td className="px-4 py-3 text-muted hidden sm:table-cell">{t.durationMinutes} min</td>
                  <td className="px-4 py-3 text-white font-semibold">
                    {t.totalPrice != null
                      ? `${Number(t.totalPrice).toFixed(0)} zł`
                      : `${((t.hourlyRate * t.durationMinutes) / 60).toFixed(0)} zł`}
                  </td>
                  <td className="px-4 py-3"><StatusBadge status={t.status} /></td>
                  {isCoach && (
                    <td className="px-4 py-3">
                      {t.status === 'SCHEDULED' && (
                        <div className="flex gap-1">
                          <button onClick={() => setCompleting({ id: t.id, notes: '' })} title="Zakończ"
                            className="text-green-400 hover:bg-green-400/10 p-1.5 rounded transition-colors">
                            <Check size={14} />
                          </button>
                          <button onClick={() => handleCancel(t.id)} title="Odwołaj"
                            className="text-red-400 hover:bg-red-400/10 p-1.5 rounded transition-colors">
                            <X size={14} />
                          </button>
                        </div>
                      )}
                      {t.status === 'COMPLETED' && (
                        <button
                          onClick={() => handleTogglePaid(t.id)}
                          title={t.paid ? 'Oznacz jako niezapłacone' : 'Oznacz jako zapłacone'}
                          className={`flex items-center gap-1.5 text-xs font-semibold px-2.5 py-1 rounded-full border transition-colors ${
                            t.paid
                              ? 'bg-accent/20 text-accent border-accent/30 hover:bg-accent/10'
                              : 'bg-amber-500/10 text-amber-400 border-amber-500/30 hover:bg-amber-500/20'
                          }`}
                        >
                          <Banknote size={11} />
                          {t.paid ? 'Zapłacono' : 'Oczekuje'}
                        </button>
                      )}
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
    </>
  );
}
