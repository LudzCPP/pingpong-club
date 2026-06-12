import { useEffect } from 'react';

export default function ConfirmDialog({ message, confirmLabel = 'Potwierdź', confirmClass, onConfirm, onCancel }) {
  useEffect(() => {
    function handler(e) { if (e.key === 'Escape') onCancel(); }
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [onCancel]);

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4 animate-[modal-backdrop-in_0.2s_ease-out]">
      <div className="bg-surface border border-border rounded-xl p-6 w-full max-w-sm shadow-2xl animate-[modal-content-in_0.2s_ease-out]">
        <p className="text-white text-sm mb-6">{message}</p>
        <div className="flex gap-3 justify-end">
          <button
            onClick={onCancel}
            className="text-sm text-muted hover:text-white px-4 py-2 rounded-lg transition-colors"
          >
            Anuluj
          </button>
          <button
            onClick={onConfirm}
            className={confirmClass ?? 'bg-red-600 hover:bg-red-700 text-white font-semibold px-4 py-2 rounded-lg transition-colors text-sm'}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
