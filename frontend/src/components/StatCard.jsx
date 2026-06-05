export default function StatCard({ icon: Icon, value, label, accent = false }) {
  return (
    <div className={`rounded-xl p-5 flex items-center gap-4 ${
      accent ? 'bg-accent/20 border border-accent/40' : 'bg-surface border border-border'
    }`}>
      <div className={`w-11 h-11 rounded-lg flex items-center justify-center flex-shrink-0 ${
        accent ? 'bg-accent/30' : 'bg-white/10'
      }`}>
        <Icon size={20} className={accent ? 'text-accent' : 'text-slate-400'} />
      </div>
      <div>
        <p className={`text-2xl font-bold ${accent ? 'text-accent' : 'text-white'}`}>{value}</p>
        <p className="text-sm text-muted leading-tight mt-0.5">{label}</p>
      </div>
    </div>
  );
}
