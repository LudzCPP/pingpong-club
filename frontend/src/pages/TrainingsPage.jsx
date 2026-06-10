import { useEffect, useState, useRef } from 'react';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';
import Avatar from '../components/Avatar';
import StatusBadge from '../components/StatusBadge';
import ConfirmDialog from '../components/ConfirmDialog';
import { Check, X, Plus, Mic, MicOff, Sparkles, Loader, Banknote, Search, MapPin, ChevronDown, RefreshCw } from 'lucide-react';

const FILTERS = ['Wszystkie', 'SCHEDULED', 'COMPLETED', 'CANCELLED'];
const FILTER_LABEL = { Wszystkie: 'Wszystkie', SCHEDULED: 'Zaplanowane', COMPLETED: 'Zrealizowane', CANCELLED: 'Odwołane' };

const inputCls = 'bg-base border border-border rounded-lg px-3 py-2 text-white text-sm placeholder-muted focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent transition-colors w-full';
const labelCls = 'text-xs font-medium text-muted uppercase tracking-wide';

const EMPTY_FORM = { playerId: '', scheduledAt: '', durationMinutes: 60, totalPrice: '', notes: '', location: '', recurring: false, recurrenceWeeks: 4 };

function formatForDatetimeInput(isoString) {
  if (!isoString) return '';
  return isoString.slice(0, 16);
}

export default function TrainingsPage() {
  const { user } = useAuth();
  const isCoach = user?.role === 'COACH' || user?.role === 'ADMIN';

  const [trainings, setTrainings] = useState([]);
  const [players, setPlayers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('Wszystkie');
  const [search, setSearch] = useState('');
  const [expanded, setExpanded] = useState(new Set());

  function toggleExpand(id) {
    setExpanded(prev => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }

  // Manual form
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [aiFilledLabel, setAiFilledLabel] = useState('');

  // Complete with notes modal
  const [completing, setCompleting] = useState(null); // { id, notes }

  // Cancel confirm dialog — { id, recurringGroupId } lub null
  const [confirmCancel, setConfirmCancel] = useState(null);

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
        totalPrice: total,
        notes: form.notes,
        location: form.location || null,
        recurrenceWeeks: form.recurring ? Number(form.recurrenceWeeks) : null,
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

  async function handleCancel(cancelGroup = false) {
    if (cancelGroup && confirmCancel.recurringGroupId) {
      await client.patch(`/trainings/group/${confirmCancel.recurringGroupId}/cancel`);
    } else {
      await client.patch(`/trainings/${confirmCancel.id}/cancel`);
    }
    setConfirmCancel(null);
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

  const searchField = isCoach ? 'playerFullName' : 'coachFullName';
  const visible = trainings
    .filter(t => filter === 'Wszystkie' || t.status === filter)
    .filter(t => !search.trim() || t[searchField]?.toLowerCase().includes(search.trim().toLowerCase()));
  const scheduled = trainings.filter(t => t.status === 'SCHEDULED').length;
  const completed = trainings.filter(t => t.status === 'COMPLETED').length;

  if (loading) return <div className="max-w-6xl mx-auto px-6 py-12 text-center text-muted">Ładowanie...</div>;

  return (
    <>
    {confirmCancel && (
      confirmCancel.recurringGroupId ? (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
          <div className="bg-surface border border-border rounded-xl p-6 w-full max-w-sm shadow-2xl space-y-4">
            <div className="flex items-center gap-2">
              <RefreshCw size={16} className="text-amber-400 shrink-0" />
              <h3 className="text-white font-semibold">Odwołaj trening z cyklu</h3>
            </div>
            <p className="text-sm text-muted">To jest trening cykliczny. Co chcesz odwołać?</p>
            <div className="flex flex-col gap-2">
              <button
                onClick={() => handleCancel(false)}
                className="w-full text-sm font-medium bg-amber-500/10 border border-amber-500/30 text-amber-400 hover:bg-amber-500/20 px-4 py-2.5 rounded-lg transition-colors text-left"
              >
                Tylko ten trening
              </button>
              <button
                onClick={() => handleCancel(true)}
                className="w-full text-sm font-medium bg-red-500/10 border border-red-500/30 text-red-400 hover:bg-red-500/20 px-4 py-2.5 rounded-lg transition-colors text-left"
              >
                Wszystkie zaplanowane w tym cyklu
              </button>
              <button
                onClick={() => setConfirmCancel(null)}
                className="text-sm text-muted hover:text-white px-3 py-2 rounded-lg transition-colors text-center"
              >
                Anuluj
              </button>
            </div>
          </div>
        </div>
      ) : (
        <ConfirmDialog
          message="Odwołać ten trening? Tej akcji nie można cofnąć."
          confirmLabel="Odwołaj trening"
          onConfirm={() => handleCancel(false)}
          onCancel={() => setConfirmCancel(null)}
        />
      )
    )}
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
      <div className="space-y-2">
        <div className="flex items-center justify-between gap-3">
          <h1 className="text-2xl font-bold text-white">Treningi</h1>
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
        <p className="text-sm text-muted">
          <span className="text-blue-300">{scheduled} zaplanowanych</span>
          <span className="mx-2 text-border">·</span>
          <span className="text-green-300">{completed} zrealizowanych</span>
        </p>
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
            <div className="space-y-1.5">
              <label className={labelCls}>Lokalizacja</label>
              <input type="text" value={form.location} onChange={e => setForm({ ...form, location: e.target.value })} placeholder="np. Sala A, ul. Sportowa 5 (opcjonalnie)" maxLength={200} className={inputCls} />
            </div>
            <div className="space-y-1.5">
              <label className={labelCls}>Notatki</label>
              <input type="text" value={form.notes} onChange={e => setForm({ ...form, notes: e.target.value })} placeholder="opcjonalnie" className={inputCls} />
            </div>
            <div className="sm:col-span-2 space-y-2">
              <label className="flex items-center gap-2.5 cursor-pointer group">
                <input
                  type="checkbox"
                  checked={form.recurring}
                  onChange={e => setForm({ ...form, recurring: e.target.checked })}
                  className="w-4 h-4 accent-accent cursor-pointer"
                />
                <span className="flex items-center gap-1.5 text-sm text-muted group-hover:text-white transition-colors">
                  <RefreshCw size={13} />
                  Powtarzaj co tydzień (trening cykliczny)
                </span>
              </label>
              {form.recurring && (
                <div className="flex items-center gap-3 pl-6">
                  <input
                    type="number"
                    min="2"
                    max="12"
                    value={form.recurrenceWeeks}
                    onChange={e => setForm({ ...form, recurrenceWeeks: e.target.value })}
                    className={`${inputCls} w-20`}
                  />
                  <span className="text-sm text-muted">tygodni łącznie</span>
                  <span className="text-xs text-muted">
                    (stworzy {form.recurrenceWeeks} treningów co 7 dni)
                  </span>
                </div>
              )}
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
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1 max-w-xs">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted pointer-events-none" />
          <input
            type="text"
            placeholder={isCoach ? 'Szukaj zawodnika...' : 'Szukaj trenera...'}
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="w-full bg-surface border border-border rounded-lg pl-8 pr-3 py-1.5 text-sm text-white placeholder-muted focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent transition-colors"
          />
        </div>
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
      </div>

      {/* Trainings list */}
      <div className="bg-surface border border-border rounded-xl overflow-hidden">
        {visible.length === 0 ? (
          <p className="text-center text-muted py-12">Brak treningów</p>
        ) : (
          <>
            {/* Mobile: cards */}
            <div className="sm:hidden divide-y divide-border">
              {visible.map(t => (
                <div key={t.id} className="divide-y divide-border/50">
                  {/* Collapsed row */}
                  <div className="p-4 space-y-2.5">
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex items-center gap-3 min-w-0">
                        <Avatar
                          firstName={isCoach ? t.playerFullName?.split(' ')[0] : t.coachFullName?.split(' ')[0]}
                          lastName={isCoach ? t.playerFullName?.split(' ')[1] : t.coachFullName?.split(' ')[1]}
                          size="sm"
                        />
                        <div className="min-w-0">
                          <p className="text-white font-medium truncate">
                            {isCoach ? t.playerFullName : t.coachFullName}
                          </p>
                          <p className="text-muted text-xs">
                            {new Date(t.scheduledAt).toLocaleString('pl-PL', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}
                            {' · '}{t.durationMinutes} min
                          </p>
                        </div>
                      </div>
                      <StatusBadge status={t.status} />
                    </div>
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <span className="text-white font-semibold">
                          {t.totalPrice != null ? `${Number(t.totalPrice).toFixed(0)} zł` : '—'}
                        </span>
                        <button
                          onClick={() => toggleExpand(t.id)}
                          className="flex items-center gap-1 text-xs text-muted hover:text-white transition-colors"
                        >
                          <ChevronDown size={14} className={`transition-transform ${expanded.has(t.id) ? 'rotate-180' : ''}`} />
                          {expanded.has(t.id) ? 'Zwiń' : 'Szczegóły'}
                        </button>
                      </div>
                      {isCoach && (
                        <div>
                          {t.status === 'SCHEDULED' && (
                            <div className="flex gap-2">
                              <button onClick={() => setCompleting({ id: t.id, notes: '' })}
                                className="flex items-center gap-1.5 text-xs font-medium text-green-400 border border-green-500/30 bg-green-500/10 hover:bg-green-500/20 px-2.5 py-1.5 rounded-lg transition-colors">
                                <Check size={12} /> Zakończ
                              </button>
                              <button onClick={() => setConfirmCancel({ id: t.id, recurringGroupId: t.recurringGroupId ?? null })}
                                className="flex items-center gap-1.5 text-xs font-medium text-red-400 border border-red-500/30 bg-red-500/10 hover:bg-red-500/20 px-2.5 py-1.5 rounded-lg transition-colors">
                                <X size={12} /> Odwołaj
                              </button>
                            </div>
                          )}
                          {t.status === 'COMPLETED' && (
                            <button onClick={() => handleTogglePaid(t.id)}
                              className={`flex items-center gap-1.5 text-xs font-semibold px-2.5 py-1.5 rounded-full border transition-colors ${
                                t.paid
                                  ? 'bg-accent/20 text-accent border-accent/30'
                                  : 'bg-amber-500/10 text-amber-400 border-amber-500/30'
                              }`}>
                              <Banknote size={11} />
                              {t.paid ? 'Zapłacono' : 'Oczekuje'}
                            </button>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                  {/* Expanded details */}
                  {expanded.has(t.id) && (
                    <div className="px-4 py-3 bg-base/50 space-y-1.5">
                      {t.location && (
                        <p className="text-sm text-muted flex items-center gap-2">
                          <MapPin size={13} className="text-accent shrink-0" />
                          {t.location}
                        </p>
                      )}
                      {t.notes && (
                        <p className="text-sm text-muted italic">"{t.notes}"</p>
                      )}
                      {!t.location && !t.notes && (
                        <p className="text-xs text-muted">Brak dodatkowych informacji</p>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>

            {/* Desktop: table */}
            <table className="w-full text-sm hidden sm:table">
              <thead>
                <tr className="border-b border-border">
                  <th className="text-left px-4 py-3 text-xs text-muted uppercase tracking-wide">{isCoach ? 'Zawodnik' : 'Trener'}</th>
                  <th className="text-left px-4 py-3 text-xs text-muted uppercase tracking-wide">Data</th>
                  <th className="text-left px-4 py-3 text-xs text-muted uppercase tracking-wide">Czas</th>
                  <th className="text-left px-4 py-3 text-xs text-muted uppercase tracking-wide">Kwota</th>
                  <th className="text-left px-4 py-3 text-xs text-muted uppercase tracking-wide">Status</th>
                  <th className="px-4 py-3 text-xs text-muted uppercase tracking-wide">{isCoach ? 'Akcje' : ''}</th>
                </tr>
              </thead>
              <tbody>
                {visible.map(t => (
                  <>
                    <tr key={t.id} className={`border-b transition-colors cursor-pointer ${expanded.has(t.id) ? 'bg-white/5 border-border/30' : 'border-border/50 hover:bg-white/5'}`}
                      onClick={() => toggleExpand(t.id)}>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2.5">
                          <Avatar
                            firstName={isCoach ? t.playerFullName?.split(' ')[0] : t.coachFullName?.split(' ')[0]}
                            lastName={isCoach ? t.playerFullName?.split(' ')[1] : t.coachFullName?.split(' ')[1]}
                            size="sm"
                          />
                          <div>
                          <div className="flex items-center gap-1.5">
                            <p className="text-white font-medium">{isCoach ? t.playerFullName : t.coachFullName}</p>
                            {t.recurringGroupId && <RefreshCw size={11} className="text-muted shrink-0" title="Trening cykliczny" />}
                          </div>
                          <p className="text-muted text-xs">{t.name}</p>
                        </div>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-muted whitespace-nowrap">
                        {new Date(t.scheduledAt).toLocaleString('pl-PL', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}
                      </td>
                      <td className="px-4 py-3 text-muted">{t.durationMinutes} min</td>
                      <td className="px-4 py-3 text-white font-semibold">
                        {t.totalPrice != null ? `${Number(t.totalPrice).toFixed(0)} zł` : '—'}
                      </td>
                      <td className="px-4 py-3"><StatusBadge status={t.status} /></td>
                      <td className="px-4 py-3" onClick={e => e.stopPropagation()}>
                        <div className="flex items-center gap-1">
                          {isCoach && t.status === 'SCHEDULED' && (
                            <>
                              <button onClick={() => setCompleting({ id: t.id, notes: '' })} title="Zakończ"
                                className="text-green-400 hover:bg-green-400/10 p-1.5 rounded transition-colors">
                                <Check size={14} />
                              </button>
                              <button onClick={() => setConfirmCancel({ id: t.id, recurringGroupId: t.recurringGroupId ?? null })} title="Odwołaj"
                                className="text-red-400 hover:bg-red-400/10 p-1.5 rounded transition-colors">
                                <X size={14} />
                              </button>
                            </>
                          )}
                          {isCoach && t.status === 'COMPLETED' && (
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
                          <ChevronDown size={14} className={`text-muted ml-auto transition-transform ${expanded.has(t.id) ? 'rotate-180' : ''}`} />
                        </div>
                      </td>
                    </tr>
                    {expanded.has(t.id) && (
                      <tr key={`${t.id}-detail`} className="border-b border-border/30 bg-base/60">
                        <td colSpan={6} className="px-6 py-3">
                          <div className="flex flex-wrap gap-x-6 gap-y-1.5 text-sm">
                            {t.location && (
                              <span className="flex items-center gap-1.5 text-muted">
                                <MapPin size={13} className="text-accent shrink-0" />
                                {t.location}
                              </span>
                            )}
                            {t.notes && (
                              <span className="text-muted italic">"{t.notes}"</span>
                            )}
                            {!t.location && !t.notes && (
                              <span className="text-muted text-xs">Brak dodatkowych informacji</span>
                            )}
                          </div>
                        </td>
                      </tr>
                    )}
                  </>
                ))}
              </tbody>
            </table>
          </>
        )}
      </div>
    </div>
    </>
  );
}
