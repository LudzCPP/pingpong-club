import { useEffect } from 'react';

export function usePageTitle(title) {
  useEffect(() => {
    document.title = title ? `TTManager – ${title}` : 'TTManager';
    return () => { document.title = 'TTManager'; };
  }, [title]);
}
