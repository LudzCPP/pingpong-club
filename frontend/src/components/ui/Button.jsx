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

export default function Button({
  as: Tag = 'button',
  variant = 'primary',
  size = 'md',
  className = '',
  children,
  ...props
}) {
  return (
    <Tag className={`${base} ${variants[variant]} ${sizes[size]} ${className}`} {...props}>
      {children}
    </Tag>
  );
}
