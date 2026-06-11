import { useCountUp } from '../hooks/useCountUp';

// Splits "1234 zł" -> { prefix: '', num: 1234, suffix: ' zł' }.
// Returns null when there is no numeric part to animate.
function parseValue(value) {
  if (typeof value === 'number') return { prefix: '', num: value, suffix: '' };
  const m = String(value).match(/^(\D*?)([\d.,\s]+)(.*)$/);
  if (!m) return null;
  const num = Number(m[2].replace(/\s/g, '').replace(',', '.'));
  if (Number.isNaN(num)) return null;
  return { prefix: m[1], num, suffix: m[3] };
}

export default function StatCard({ icon: Icon, value, label, accent = false }) {
  const parsed = parseValue(value);
  const animated = useCountUp(parsed ? parsed.num : 0);
  const display = parsed
    ? `${parsed.prefix}${Math.round(animated)}${parsed.suffix}`
    : value;

  return (
    <div className={`rounded-xl p-5 flex items-center gap-4 transition-transform duration-200 hover:-translate-y-0.5 hover:shadow-lg ${
      accent ? 'bg-accent/20 border border-accent/40' : 'bg-surface border border-border'
    }`}>
      <div className={`w-11 h-11 rounded-lg flex items-center justify-center flex-shrink-0 ${
        accent ? 'bg-accent/30' : 'bg-white/10'
      }`}>
        <Icon size={20} className={accent ? 'text-accent' : 'text-slate-400'} />
      </div>
      <div>
        <p className={`text-2xl font-bold ${accent ? 'text-accent' : 'text-white'}`}>{display}</p>
        <p className="text-sm text-muted leading-tight mt-0.5">{label}</p>
      </div>
    </div>
  );
}
