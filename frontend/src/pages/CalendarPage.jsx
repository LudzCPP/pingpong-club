import { useState, useEffect, useMemo } from 'react';
import client from '../api/client';
import StatusBadge from '../components/StatusBadge';
import { ChevronLeft, ChevronRight, MapPin } from 'lucide-react';

const DAYS_SHORT = ['Pon', 'Wt', 'Śr', 'Czw', 'Pt', 'Sob', 'Ndz'];
const MONTHS_PL = [
  'Styczeń', 'Luty', 'Marzec', 'Kwiecień', 'Maj', 'Czerwiec',
  'Lipiec', 'Sierpień', 'Wrzesień', 'Październik', 'Listopad', 'Grudzień',
];

function buildCalendarGrid(year, month) {
  const firstDay = new Date(year, month, 1);
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const startOffset = (firstDay.getDay() + 6) % 7; // Mon = 0
  const cells = [];
  for (let i = 0; i < startOffset; i++) cells.push(null);
  for (let d = 1; d <= daysInMonth; d++) cells.push(d);
  while (cells.length % 7 !== 0) cells.push(null);
  return cells;
}

function dateKey(year, month, day) {
  return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

export default function CalendarPage() {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth());
  const [selectedDay, setSelectedDay] = useState(now.getDate());
  const [trainings, setTrainings] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    client.get('/trainings')
      .then(r => setTrainings(r.data))
      .finally(() => setLoading(false));
  }, []);

  const byDate = useMemo(() => {
    const map = {};
    trainings.forEach(t => {
      const d = t.scheduledAt?.split('T')[0];
      if (d) {
        if (!map[d]) map[d] = [];
        map[d].push(t);
      }
    });
    return map;
  }, [trainings]);

  function prevMonth() {
    if (month === 0) { setYear(y => y - 1); setMonth(11); }
    else setMonth(m => m - 1);
    setSelectedDay(null);
  }
  function nextMonth() {
    if (month === 11) { setYear(y => y + 1); setMonth(0); }
    else setMonth(m => m + 1);
    setSelectedDay(null);
  }
  function goToday() {
    setYear(now.getFullYear());
    setMonth(now.getMonth());
    setSelectedDay(now.getDate());
  }

  const calCells = useMemo(() => buildCalendarGrid(year, month), [year, month]);
  const isCurrentMonth = year === now.getFullYear() && month === now.getMonth();
  const todayNum = isCurrentMonth ? now.getDate() : null;

  const monthScheduled = useMemo(() => {
    const prefix = `${year}-${String(month + 1).padStart(2, '0')}-`;
    return Object.entries(byDate)
      .filter(([d]) => d.startsWith(prefix))
      .flatMap(([, ts]) => ts)
      .filter(t => t.status === 'SCHEDULED').length;
  }, [byDate, year, month]);

  const monthCompleted = useMemo(() => {
    const prefix = `${year}-${String(month + 1).padStart(2, '0')}-`;
    return Object.entries(byDate)
      .filter(([d]) => d.startsWith(prefix))
      .flatMap(([, ts]) => ts)
      .filter(t => t.status === 'COMPLETED').length;
  }, [byDate, year, month]);

  const selectedKey = selectedDay ? dateKey(year, month, selectedDay) : null;
  const selectedTrainings = selectedKey
    ? (byDate[selectedKey] ?? []).sort((a, b) => new Date(a.scheduledAt) - new Date(b.scheduledAt))
    : [];

  if (loading) return <div className="max-w-4xl mx-auto px-6 py-12 text-center text-muted">Ładowanie...</div>;

  return (
    <div className="max-w-4xl mx-auto px-6 py-8 space-y-6">

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Kalendarz</h1>
          <p className="text-sm text-muted mt-1">Twój plan treningowy</p>
        </div>
        {!isCurrentMonth && (
          <button onClick={goToday} className="text-sm text-accent hover:text-green-400 transition-colors">
            Dziś
          </button>
        )}
      </div>

      <div className="bg-surface border border-border rounded-xl overflow-hidden">
        {/* Month navigation */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-border">
          <button onClick={prevMonth}
            className="p-2 rounded-lg text-muted hover:text-white hover:bg-white/10 transition-colors">
            <ChevronLeft size={18} />
          </button>
          <span className="text-white font-semibold text-lg">
            {MONTHS_PL[month]} {year}
          </span>
          <button onClick={nextMonth}
            className="p-2 rounded-lg text-muted hover:text-white hover:bg-white/10 transition-colors">
            <ChevronRight size={18} />
          </button>
        </div>

        {/* Day headers */}
        <div className="grid grid-cols-7 border-b border-border">
          {DAYS_SHORT.map(d => (
            <div key={d} className="py-2 text-center text-xs font-medium text-muted uppercase tracking-wide">
              {d}
            </div>
          ))}
        </div>

        {/* Calendar grid */}
        <div className="grid grid-cols-7">
          {calCells.map((day, i) => {
            if (!day) return (
              <div key={`empty-${i}`} className="min-h-[64px] border-b border-r border-border/30 bg-base/30" />
            );

            const key = dateKey(year, month, day);
            const dayTrainings = byDate[key] ?? [];
            const isToday = day === todayNum;
            const isSelected = day === selectedDay;

            const scheduledCount = dayTrainings.filter(t => t.status === 'SCHEDULED').length;
            const completedCount = dayTrainings.filter(t => t.status === 'COMPLETED').length;

            return (
              <div
                key={key}
                onClick={() => setSelectedDay(day === selectedDay ? null : day)}
                className={`min-h-[44px] sm:min-h-[64px] p-1 sm:p-2 border-b border-r border-border/30 cursor-pointer flex flex-col gap-1 sm:gap-1.5 transition-colors ${
                  isSelected ? 'bg-accent/10 border-accent/20' : 'hover:bg-white/5'
                }`}
              >
                <span className={`text-xs sm:text-sm w-6 h-6 sm:w-7 sm:h-7 flex items-center justify-center rounded-full font-medium self-start ${
                  isToday
                    ? 'bg-accent text-white font-bold'
                    : isSelected
                    ? 'text-accent font-bold'
                    : 'text-muted'
                }`}>
                  {day}
                </span>

                {dayTrainings.length > 0 && (
                  <div className="flex flex-wrap gap-1">
                    {scheduledCount > 0 && (
                      <span className="text-xs bg-blue-500/20 text-blue-300 px-1.5 py-0.5 rounded font-medium leading-none">
                        {scheduledCount}
                      </span>
                    )}
                    {completedCount > 0 && (
                      <span className="text-xs bg-accent/20 text-accent px-1.5 py-0.5 rounded font-medium leading-none">
                        {completedCount}
                      </span>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* Legend */}
        <div className="px-6 py-3 border-t border-border flex items-center gap-5">
          <span className="text-xs text-muted flex items-center gap-1.5">
            <span className="min-w-[18px] h-[18px] rounded bg-blue-500/20 text-blue-300 text-xs flex items-center justify-center font-medium px-1">
              {monthScheduled}
            </span>
            Zaplanowane
          </span>
          <span className="text-xs text-muted flex items-center gap-1.5">
            <span className="min-w-[18px] h-[18px] rounded bg-accent/20 text-accent text-xs flex items-center justify-center font-medium px-1">
              {monthCompleted}
            </span>
            Zrealizowane
          </span>
        </div>
      </div>

      {/* Selected day detail */}
      {selectedDay && (
        <div className="bg-surface border border-border rounded-xl overflow-hidden">
          <div className="px-6 py-4 border-b border-border flex items-center justify-between">
            <div>
              <h2 className="text-white font-semibold">
                {selectedDay} {MONTHS_PL[month]} {year}
              </h2>
              <p className="text-xs text-muted mt-0.5">
                {selectedTrainings.length === 0
                  ? 'Brak treningów'
                  : `${selectedTrainings.length} ${selectedTrainings.length === 1 ? 'trening' : 'treningów'}`}
              </p>
            </div>
          </div>

          {selectedTrainings.length === 0 ? (
            <p className="text-muted text-sm py-8 text-center">Wolny dzień</p>
          ) : (
            <ul className="divide-y divide-border">
              {selectedTrainings.map(t => (
                <li key={t.id} className="px-4 py-3 flex items-center gap-3">
                  <div className="w-16 flex-shrink-0 text-center">
                    <p className="text-white font-bold text-xl leading-none">
                      {new Date(t.scheduledAt).toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit' })}
                    </p>
                    <p className="text-muted text-xs mt-1">{t.durationMinutes} min</p>
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-white font-medium">{t.name}</p>
                    <p className="text-muted text-sm">{t.playerFullName}</p>
                    {t.location && (
                      <p className="text-muted text-xs flex items-center gap-1 mt-0.5">
                        <MapPin size={10} className="shrink-0" />{t.location}
                      </p>
                    )}
                    {t.notes && (
                      <p className="text-muted text-xs mt-1 italic">"{t.notes}"</p>
                    )}
                  </div>
                  <div className="flex flex-col items-end gap-2 flex-shrink-0">
                    <StatusBadge status={t.status} />
                    <span className="text-white text-sm font-semibold">
                      {Number(t.totalPrice).toFixed(0)} zł
                    </span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
