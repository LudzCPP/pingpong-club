import { useEffect, useRef, useState } from 'react';

// Animates from the previous value (0 on first mount) to `target`.
// Respects prefers-reduced-motion.
export function useCountUp(target, duration = 800) {
  const [val, setVal] = useState(0);
  const fromRef = useRef(0);

  useEffect(() => {
    const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    const from = fromRef.current;
    fromRef.current = target;

    if (reduce || from === target) {
      setVal(target);
      return;
    }

    let raf;
    const start = performance.now();
    const tick = (now) => {
      const p = Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - p, 3);
      setVal(from + (target - from) * eased);
      if (p < 1) raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [target, duration]);

  return val;
}
