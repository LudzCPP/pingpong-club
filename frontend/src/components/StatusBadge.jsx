const config = {
  SCHEDULED:  { label: 'Zaplanowany', cls: 'bg-blue-500/20 text-blue-300 border-blue-500/30' },
  COMPLETED:  { label: 'Zrealizowany', cls: 'bg-green-500/20 text-green-300 border-green-500/30' },
  CANCELLED:  { label: 'Odwołany',    cls: 'bg-slate-500/20 text-slate-400 border-slate-500/30' },
};

export default function StatusBadge({ status }) {
  const { label, cls } = config[status] ?? { label: status, cls: 'bg-slate-500/20 text-slate-400' };
  return (
    <span className={`inline-block px-2.5 py-0.5 rounded-full text-xs font-semibold border ${cls}`}>
      {label}
    </span>
  );
}
