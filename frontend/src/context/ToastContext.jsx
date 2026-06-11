import { createContext, useContext, useCallback, useState } from 'react';
import { createPortal } from 'react-dom';
import { CheckCircle2, XCircle, Info, AlertTriangle, X } from 'lucide-react';

const ToastContext = createContext(null);

const config = {
  success: { icon: CheckCircle2, cls: 'border-success/40 text-green-400' },
  error: { icon: XCircle, cls: 'border-danger/40 text-red-400' },
  warning: { icon: AlertTriangle, cls: 'border-warning/40 text-amber-400' },
  info: { icon: Info, cls: 'border-info/40 text-blue-400' },
};

let seq = 0;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const dismiss = useCallback((id) => {
    setToasts(ts => ts.filter(t => t.id !== id));
  }, []);

  const push = useCallback((type, message, duration = 4000) => {
    const id = ++seq;
    setToasts(ts => [...ts, { id, type, message }]);
    if (duration > 0) setTimeout(() => dismiss(id), duration);
    return id;
  }, [dismiss]);

  const toast = {
    success: (m, d) => push('success', m, d),
    error: (m, d) => push('error', m, d),
    warning: (m, d) => push('warning', m, d),
    info: (m, d) => push('info', m, d),
  };

  return (
    <ToastContext.Provider value={toast}>
      {children}
      {createPortal(
        <div className="fixed bottom-4 right-4 z-[100] flex flex-col gap-2 w-full max-w-xs pointer-events-none">
          {toasts.map(t => {
            const { icon: Icon, cls } = config[t.type];
            return (
              <div
                key={t.id}
                role="status"
                className={`pointer-events-auto flex items-start gap-3 bg-surface border ${cls} rounded-lg px-4 py-3 shadow-2xl animate-[toast-in_0.2s_ease-out]`}
              >
                <Icon size={18} className="shrink-0 mt-0.5" />
                <p className="text-sm text-white flex-1 leading-snug">{t.message}</p>
                <button
                  onClick={() => dismiss(t.id)}
                  className="text-muted hover:text-white transition-colors shrink-0"
                  aria-label="Zamknij"
                >
                  <X size={15} />
                </button>
              </div>
            );
          })}
        </div>,
        document.body
      )}
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}
