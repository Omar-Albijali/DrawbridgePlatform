import { useEffect, useState, type ReactNode } from 'react';
import AnimatedBackground from './AnimatedBackground';

function shouldReduceMotion(): boolean {
  if (typeof window === 'undefined') {
    return false;
  }

  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

export default function MotionLayer({ children }: { children: ReactNode }): JSX.Element {
  const [reducedMotion, setReducedMotion] = useState<boolean>(() => shouldReduceMotion());

  useEffect(() => {
    const query = window.matchMedia('(prefers-reduced-motion: reduce)');
    const onChange = () => setReducedMotion(query.matches);
    query.addEventListener('change', onChange);

    return () => {
      query.removeEventListener('change', onChange);
    };
  }, []);

  return (
    <div className="relative isolate min-h-screen">
      <AnimatedBackground animated={!reducedMotion} />
      <div className="relative z-10">{children}</div>
    </div>
  );
}
