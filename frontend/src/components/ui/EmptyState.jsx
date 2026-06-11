export default function EmptyState({ icon: Icon, title, description, action, className = '' }) {
  return (
    <div className={`flex flex-col items-center justify-center text-center py-8 px-4 ${className}`}>
      {Icon && (
        <div className="w-12 h-12 rounded-full bg-white/5 flex items-center justify-center mb-3">
          <Icon size={22} className="text-muted" />
        </div>
      )}
      <p className="text-white text-sm font-medium">{title}</p>
      {description && <p className="text-muted text-xs mt-1 max-w-xs">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}
