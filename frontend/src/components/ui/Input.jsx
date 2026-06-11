import { forwardRef, useId } from 'react';

const Input = forwardRef(function Input(
  { label, error, hint, className = '', id, ...props },
  ref
) {
  const autoId = useId();
  const inputId = id || autoId;
  return (
    <div className="space-y-1.5">
      {label && (
        <label htmlFor={inputId} className="text-sm font-medium text-muted block">
          {label}
        </label>
      )}
      <input
        ref={ref}
        id={inputId}
        className={`w-full bg-base border rounded-lg px-4 py-2.5 text-white placeholder-muted focus:outline-none focus:ring-1 transition-colors ${
          error
            ? 'border-danger focus:border-danger focus:ring-danger'
            : 'border-border focus:border-accent focus:ring-accent'
        } ${className}`}
        {...props}
      />
      {error && <p className="text-xs text-danger">{error}</p>}
      {hint && !error && <p className="text-xs text-muted">{hint}</p>}
    </div>
  );
});

export default Input;
