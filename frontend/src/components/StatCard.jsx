export default function StatCard({ icon, value, label, accent = false }) {
  return (
    <div className={`rounded-xl p-5 flex items-center gap-4 ${accent ? 'bg-accent/20 border border-accent/40' : 'bg-surface border border-border'}`}>
      <span className="text-3xl">{icon}</span>
      <div>
        <p className={`text-2xl font-bold ${accent ? 'text-accent' : 'text-white'}`}>{value}</p>
        <p className="text-sm text-muted">{label}</p>
      </div>
    </div>
  );
}
