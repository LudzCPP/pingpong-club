import { useEffect, useState, useMemo } from 'react';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';
import Avatar from '../components/Avatar';
import { usePageTitle } from '../hooks/usePageTitle';
import { Banknote, Dumbbell, Trophy, Users, Download, CalendarDays } from 'lucide-react';

function fmtShort(amount) {
  return Number(amount ?? 0).toFixed(0);
}

const PRESETS = [
  { label: 'Ten miesiąc', key: 'this_month' },
  { label: 'Poprzedni miesiąc', key: 'last_month' },
  { label: 'Ostatnie 3 miesiące', key: 'last_3_months' },
  { label: 'Ten rok', key: 'this_year' },
  { label: 'Własny zakres', key: 'custom' },
];

function getPresetRange(key) {
  const now = new Date();
  const y = now.getFullYear();
  const m = now.getMonth();
  const todayStr = now.toISOString().split('T')[0];
  switch (key) {
    case 'this_month':
      return [`${y}-${String(m + 1).padStart(2, '0')}-01`, todayStr];
    case 'last_month': {
      const lm = m === 0 ? 12 : m;
      const ly = m === 0 ? y - 1 : y;
      const lastDay = new Date(ly, lm, 0).getDate();
      return [`${ly}-${String(lm).padStart(2, '0')}-01`, `${ly}-${String(lm).padStart(2, '0')}-${lastDay}`];
    }
    case 'last_3_months': {
      const d = new Date(now);
      d.setMonth(d.getMonth() - 3);
      return [d.toISOString().split('T')[0], todayStr];
    }
    case 'this_year':
      return [`${y}-01-01`, todayStr];
    default:
      return [todayStr, todayStr];
  }
}

function exportCsv(filename, headers, rows) {
  const lines = [headers.join(','), ...rows.map(r => r.map(v => `"${String(v).replace(/"/g, '""')}"`).join(','))];
  const blob = new Blob(['﻿' + lines.join('\r\n')], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

const inputCls = 'bg-base border border-border rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent transition-colors';

export default function ReportsPage() {
  const { user } = useAuth();
  const isCoach = user?.role === 'COACH' || user?.role === 'ADMIN';

  const [trainings, setTrainings] = useState([]);
  const [matches, setMatches] = useState([]);
  const [loading, setLoading] = useState(true);

  const [preset, setPreset] = useState('this_month');
  const [from, setFrom] = useState(() => getPresetRange('this_month')[0]);
  const [to, setTo] = useState(() => getPresetRange('this_month')[1]);

  useEffect(() => {
    Promise.all([
      client.get('/trainings'),
      client.get('/finances/matches'),
    ]).then(([t, m]) => {
      setTrainings(t.data);
      setMatches(m.data);
    }).finally(() => setLoading(false));
  }, []);

  function applyPreset(key) {
    setPreset(key);
    if (key !== 'custom') {
      const [f, t] = getPresetRange(key);
      setFrom(f);
      setTo(t);
    }
  }

  const filteredTrainings = useMemo(() =>
    trainings.filter(t => {
      const d = t.scheduledAt?.split('T')[0];
      return d >= from && d <= to;
    }),
    [trainings, from, to]
  );

  const filteredMatches = useMemo(() =>
    matches.filter(m => m.matchDate >= from && m.matchDate <= to),
    [matches, from, to]
  );

  const completedTrainings = filteredTrainings.filter(t => t.status === 'COMPLETED');
  const cancelledTrainings = filteredTrainings.filter(t => t.status === 'CANCELLED');
  const scheduledTrainings = filteredTrainings.filter(t => t.status === 'SCHEDULED');

  const trainingEarnings = completedTrainings.reduce((sum, t) => sum + Number(t.totalPrice ?? 0), 0);
  const matchEarnings = filteredMatches.reduce((sum, m) => sum + Number(m.payment ?? 0), 0);
  const totalEarnings = trainingEarnings + matchEarnings;

  const playerBreakdown = useMemo(() => {
    const map = {};
    completedTrainings.forEach(t => {
      const name = t.playerFullName;
      if (!map[name]) map[name] = { name, completedTrainings: 0, trainingEarnings: 0, matches: 0, matchEarnings: 0 };
      map[name].completedTrainings++;
      map[name].trainingEarnings += Number(t.totalPrice ?? 0);
    });
    filteredMatches.forEach(m => {
      const name = m.playerFullName;
      if (!map[name]) map[name] = { name, completedTrainings: 0, trainingEarnings: 0, matches: 0, matchEarnings: 0 };
      map[name].matches++;
      map[name].matchEarnings += Number(m.payment ?? 0);
    });
    return Object.values(map)
      .map(p => ({ ...p, total: p.trainingEarnings + p.matchEarnings }))
      .sort((a, b) => b.total - a.total);
  }, [completedTrainings, filteredMatches]);

  const monthlyBreakdown = useMemo(() => {
    const map = {};
    completedTrainings.forEach(t => {
      const month = t.scheduledAt?.slice(0, 7);
      if (!map[month]) map[month] = { month, trainingEarnings: 0, matchEarnings: 0 };
      map[month].trainingEarnings += Number(t.totalPrice ?? 0);
    });
    filteredMatches.forEach(m => {
      const month = m.matchDate?.slice(0, 7);
      if (!map[month]) map[month] = { month, trainingEarnings: 0, matchEarnings: 0 };
      map[month].matchEarnings += Number(m.payment ?? 0);
    });
    return Object.values(map)
      .map(m => ({ ...m, total: m.trainingEarnings + m.matchEarnings }))
      .sort((a, b) => a.month.localeCompare(b.month));
  }, [completedTrainings, filteredMatches]);

  const maxMonthlyTotal = Math.max(...monthlyBreakdown.map(m => m.total), 1);

  const totalTrainings = filteredTrainings.length;

  function handleExportTrainings() {
    exportCsv(
      `treningi_${from}_${to}.csv`,
      ['Data', 'Zawodnik', 'Czas (min)', 'Kwota (zł)', 'Status'],
      filteredTrainings.map(t => [
        t.scheduledAt?.slice(0, 10),
        t.playerFullName,
        t.durationMinutes,
        Number(t.totalPrice ?? 0).toFixed(2),
        t.status,
      ])
    );
  }

  function handleExportMatches() {
    exportCsv(
      `mecze_${from}_${to}.csv`,
      ['Data', 'Zawodnik', 'Przeciwnik', 'Wynik', 'Wypłata (zł)'],
      filteredMatches.map(m => [
        m.matchDate,
        m.playerFullName,
        m.opponent,
        m.result,
        Number(m.payment ?? 0).toFixed(2),
      ])
    );
  }

  usePageTitle('Raporty');

  if (loading) return <div className="max-w-6xl mx-auto px-6 py-12 text-center text-muted">Ładowanie danych...</div>;

  return (
    <div className="max-w-6xl mx-auto px-6 py-8 space-y-8">

      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Raporty</h1>
          <p className="text-sm text-muted mt-1">Analiza finansowa i statystyki klubu</p>
        </div>
        <div className="flex gap-2 flex-shrink-0">
          <button onClick={handleExportTrainings}
            className="flex items-center gap-2 bg-surface border border-border hover:border-accent/50 text-sm text-muted hover:text-white px-3 py-2 rounded-lg transition-colors">
            <Download size={14} />
            <span className="hidden sm:inline">Treningi CSV</span>
          </button>
          <button onClick={handleExportMatches}
            className="flex items-center gap-2 bg-surface border border-border hover:border-accent/50 text-sm text-muted hover:text-white px-3 py-2 rounded-lg transition-colors">
            <Download size={14} />
            <span className="hidden sm:inline">Mecze CSV</span>
          </button>
        </div>
      </div>

      {/* Period selector */}
      <div className="bg-surface border border-border rounded-xl p-5">
        <p className="text-xs font-medium text-muted uppercase tracking-wide mb-3 flex items-center gap-2">
          <CalendarDays size={13} />
          Okres analizy
        </p>
        <div className="flex flex-wrap gap-2 mb-4">
          {PRESETS.map(p => (
            <button key={p.key} onClick={() => applyPreset(p.key)}
              className={`px-3 py-1.5 rounded-full text-sm font-medium transition-colors ${
                preset === p.key
                  ? 'bg-accent text-white'
                  : 'bg-base border border-border text-muted hover:text-white hover:border-slate-500'
              }`}>
              {p.label}
            </button>
          ))}
        </div>
        {preset === 'custom' && (
          <div className="flex items-end gap-4 flex-wrap pt-2 border-t border-border">
            <div className="space-y-1.5">
              <p className="text-xs text-muted uppercase tracking-wide">Od</p>
              <input type="date" value={from} onChange={e => setFrom(e.target.value)} className={inputCls} />
            </div>
            <div className="space-y-1.5">
              <p className="text-xs text-muted uppercase tracking-wide">Do</p>
              <input type="date" value={to} onChange={e => setTo(e.target.value)} className={inputCls} />
            </div>
          </div>
        )}
        {preset !== 'custom' && (
          <p className="text-xs text-muted">{from} — {to}</p>
        )}
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-accent/20 border border-accent/40 rounded-xl p-5 flex items-center gap-4 col-span-2 lg:col-span-1">
          <div className="w-11 h-11 rounded-lg bg-accent/30 flex items-center justify-center flex-shrink-0">
            <Banknote size={20} className="text-accent" />
          </div>
          <div>
            <p className="text-2xl font-bold text-accent">{fmtShort(totalEarnings)} zł</p>
            <p className="text-sm text-muted leading-tight mt-0.5">Łączne zarobki</p>
          </div>
        </div>
        <div className="bg-surface border border-border rounded-xl p-5 flex items-center gap-4">
          <div className="w-11 h-11 rounded-lg bg-white/10 flex items-center justify-center flex-shrink-0">
            <Dumbbell size={20} className="text-slate-400" />
          </div>
          <div>
            <p className="text-2xl font-bold text-white">{fmtShort(trainingEarnings)} zł</p>
            <p className="text-sm text-muted leading-tight mt-0.5">Z treningów</p>
          </div>
        </div>
        <div className="bg-surface border border-border rounded-xl p-5 flex items-center gap-4">
          <div className="w-11 h-11 rounded-lg bg-white/10 flex items-center justify-center flex-shrink-0">
            <Trophy size={20} className="text-slate-400" />
          </div>
          <div>
            <p className="text-2xl font-bold text-white">{fmtShort(matchEarnings)} zł</p>
            <p className="text-sm text-muted leading-tight mt-0.5">Z meczów</p>
          </div>
        </div>
        <div className="bg-surface border border-border rounded-xl p-5 flex items-center gap-4">
          <div className="w-11 h-11 rounded-lg bg-white/10 flex items-center justify-center flex-shrink-0">
            <Users size={20} className="text-slate-400" />
          </div>
          <div>
            <p className="text-2xl font-bold text-white">{completedTrainings.length}</p>
            <p className="text-sm text-muted leading-tight mt-0.5">Treningi zrealizowane</p>
          </div>
        </div>
      </div>

      {/* Training stats */}
      <div className="bg-surface border border-border rounded-xl p-6">
        <h2 className="text-white font-semibold mb-5">Statystyki treningów</h2>
        {totalTrainings === 0 ? (
          <p className="text-muted text-sm">Brak treningów w wybranym okresie.</p>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
            {[
              { label: 'Zaplanowane', count: scheduledTrainings.length, colorBar: 'bg-blue-500', colorText: 'text-blue-300' },
              { label: 'Zrealizowane', count: completedTrainings.length, colorBar: 'bg-accent', colorText: 'text-accent' },
              { label: 'Odwołane', count: cancelledTrainings.length, colorBar: 'bg-slate-500', colorText: 'text-slate-400' },
            ].map(({ label, count, colorBar, colorText }) => {
              const pct = totalTrainings ? Math.round(count / totalTrainings * 100) : 0;
              return (
                <div key={label} className="space-y-2">
                  <div className="flex justify-between items-baseline">
                    <span className="text-sm text-muted">{label}</span>
                    <span className={`text-xl font-bold ${colorText}`}>{count}</span>
                  </div>
                  <div className="h-1.5 bg-white/10 rounded-full overflow-hidden">
                    <div className={`h-full ${colorBar} rounded-full`} style={{ width: `${pct}%` }} />
                  </div>
                  <p className="text-xs text-muted">{pct}% z {totalTrainings} treningów</p>
                </div>
              );
            })}
          </div>
        )}
      </div>

      <div className="grid lg:grid-cols-2 gap-6">

        {/* Player breakdown */}
        <div className="bg-surface border border-border rounded-xl overflow-hidden">
          <div className="px-6 py-4 border-b border-border">
            <h2 className="text-white font-semibold">Zestawienie zawodników</h2>
            <p className="text-xs text-muted mt-0.5">Zarobki wygenerowane przez każdego zawodnika</p>
          </div>
          {playerBreakdown.length === 0 ? (
            <p className="text-muted text-sm py-8 text-center">Brak danych w wybranym okresie</p>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  <th className="text-left px-4 py-3 text-xs text-muted uppercase tracking-wide">Zawodnik</th>
                  <th className="text-right px-4 py-3 text-xs text-muted uppercase tracking-wide hidden sm:table-cell">Treningi</th>
                  <th className="text-right px-4 py-3 text-xs text-muted uppercase tracking-wide hidden sm:table-cell">Mecze</th>
                  <th className="text-right px-4 py-3 text-xs text-muted uppercase tracking-wide">Łącznie</th>
                </tr>
              </thead>
              <tbody>
                {playerBreakdown.map(p => (
                  <tr key={p.name} className="border-b border-border/50 last:border-0 hover:bg-white/5 transition-colors">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2.5">
                        <Avatar firstName={p.name?.split(' ')[0]} lastName={p.name?.split(' ')[1]} size="sm" />
                        <span className="text-white font-medium">{p.name}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-right text-muted hidden sm:table-cell">{fmtShort(p.trainingEarnings)} zł</td>
                    <td className="px-4 py-3 text-right text-muted hidden sm:table-cell">{fmtShort(p.matchEarnings)} zł</td>
                    <td className="px-4 py-3 text-right text-white font-semibold">{fmtShort(p.total)} zł</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Monthly breakdown */}
        <div className="bg-surface border border-border rounded-xl overflow-hidden">
          <div className="px-6 py-4 border-b border-border">
            <h2 className="text-white font-semibold">Zestawienie miesięczne</h2>
            <p className="text-xs text-muted mt-0.5">Przychody z podziałem na miesiące</p>
          </div>
          {monthlyBreakdown.length === 0 ? (
            <p className="text-muted text-sm py-8 text-center">Brak danych w wybranym okresie</p>
          ) : (
            <div className="p-5 space-y-4">
              {monthlyBreakdown.map(m => {
                const monthLabel = new Date(m.month + '-02').toLocaleDateString('pl-PL', { month: 'long', year: 'numeric' });
                const pct = Math.round(m.total / maxMonthlyTotal * 100);
                return (
                  <div key={m.month} className="space-y-1.5">
                    <div className="flex justify-between items-baseline">
                      <span className="text-sm text-white capitalize">{monthLabel}</span>
                      <span className="text-sm font-bold text-accent">{fmtShort(m.total)} zł</span>
                    </div>
                    <div className="h-2 bg-white/10 rounded-full overflow-hidden">
                      <div className="h-full bg-accent rounded-full transition-all duration-300" style={{ width: `${pct}%` }} />
                    </div>
                    <div className="flex gap-4 text-xs text-muted">
                      <span>Treningi: {fmtShort(m.trainingEarnings)} zł</span>
                      <span>Mecze: {fmtShort(m.matchEarnings)} zł</span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

      </div>
    </div>
  );
}
