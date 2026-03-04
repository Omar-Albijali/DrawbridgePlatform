interface AnimatedBackgroundProps {
  animated?: boolean;
}

export default function AnimatedBackground({ animated = true }: AnimatedBackgroundProps): JSX.Element {
  return (
    <div className="pointer-events-none fixed inset-0 -z-10 overflow-hidden" aria-hidden="true">
      <svg
        className={`absolute -left-[7%] -top-[7%] h-[115%] w-[115%] ${animated ? 'animate-flow' : ''}`}
        viewBox="0 0 1200 800"
        role="presentation"
      >
        <defs>
          <linearGradient id="motion-wave-gradient" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#10b981" stopOpacity="0" />
            <stop offset="50%" stopColor="#10b981" stopOpacity="0.16" />
            <stop offset="100%" stopColor="#10b981" stopOpacity="0" />
          </linearGradient>
        </defs>
        <g fill="none" stroke="url(#motion-wave-gradient)" strokeWidth="1.5" strokeLinecap="round">
          <path d="M-100 400 C 200 100 800 700 1300 400" />
          <path d="M-100 450 C 300 150 900 750 1300 450" />
          <path d="M-100 350 C 100 50 700 650 1300 350" />
        </g>
      </svg>
      <div className="absolute inset-0 bg-slate-50/90 backdrop-blur-[2px] dark:bg-slate-950/88" />
    </div>
  );
}
