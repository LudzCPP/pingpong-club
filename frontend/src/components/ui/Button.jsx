const base =
  'inline-flex items-center justify-center gap-2 font-semibold rounded-lg transition-colors ' +
  'disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none ' +
  'focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-offset-base';

const variants = {
  primary: 'bg-accent hover:bg-green-600 text-white focus-visible:ring-accent',
  secondary: 'bg-white/10 hover:bg-white/15 text-white focus-visible:ring-white/30',
  danger: 'bg-danger hover:bg-red-600 text-white focus-visible:ring-danger',
  outline: 'border border-border hover:border-accent text-white hover:text-accent focus-visible:ring-accent',
  ghost: 'text-muted hover:text-white hover:bg-white/10 focus-visible:ring-white/30',
};

const sizes = {
  sm: 'text-xs px-3 py-1.5',
  md: 'text-sm px-5 py-2.5',
  icon: 'p-1.5',
};

function Spinner() {
  return (
    <svg className="animate-spin h-4 w-4 shrink-0" viewBox="0 0 24 24" fill="none">
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
    </svg>
  );
}

export default function Button({
  as: Tag = 'button',
  variant = 'primary',
  size = 'md',
  className = '',
  loading = false,
  children,
  ...props
}) {
  return (
    <Tag
      className={`${base} ${variants[variant]} ${sizes[size]} ${className}`}
      disabled={loading || props.disabled}
      {...props}
    >
      {loading && <Spinner />}
      {children}
    </Tag>
  );
}
