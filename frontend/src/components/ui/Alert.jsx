const variants = {
  success: 'bg-success/10 border-success/30 text-green-400',
  danger: 'bg-danger/10 border-danger/30 text-red-400',
  warning: 'bg-warning/10 border-warning/30 text-amber-400',
  info: 'bg-info/10 border-info/30 text-blue-400',
};

export default function Alert({ variant = 'info', className = '', children }) {
  return (
    <div className={`border text-sm px-4 py-3 rounded-lg ${variants[variant]} ${className}`}>
      {children}
    </div>
  );
}
