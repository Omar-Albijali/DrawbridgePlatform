import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';

function canUseCustomCursor(): boolean {
  if (typeof window === 'undefined') {
    return false;
  }

  const finePointer = window.matchMedia('(pointer: fine)').matches;
  const hoverCapable = window.matchMedia('(hover: hover)').matches;
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  return finePointer && hoverCapable && !reduceMotion;
}

function isHoverTarget(element: Element | null): boolean {
  if (!element) {
    return false;
  }

  return Boolean(
    element.closest('a, button, input, select, textarea, label, [role="button"], [data-cursor-hover], .hover-target'),
  );
}

export default function CustomCursor(): JSX.Element | null {
  const dotRef = useRef<HTMLDivElement | null>(null);
  const outlineRef = useRef<HTMLDivElement | null>(null);
  const animationFrameRef = useRef<number | null>(null);
  const [enabled, setEnabled] = useState<boolean>(() => canUseCustomCursor());

  useEffect(() => {
    const mediaQueries = [
      window.matchMedia('(pointer: fine)'),
      window.matchMedia('(hover: hover)'),
      window.matchMedia('(prefers-reduced-motion: reduce)'),
    ];

    const updateEnabled = () => {
      setEnabled(canUseCustomCursor());
    };

    mediaQueries.forEach((query) => query.addEventListener('change', updateEnabled));
    return () => {
      mediaQueries.forEach((query) => query.removeEventListener('change', updateEnabled));
    };
  }, []);

  useEffect(() => {
    const root = document.documentElement;
    if (enabled) {
      root.classList.add('has-custom-cursor');
      return () => {
        root.classList.remove('has-custom-cursor');
      };
    }

    root.classList.remove('has-custom-cursor');

    return () => {
      root.classList.remove('has-custom-cursor');
    };
  }, [enabled]);

  useEffect(() => {
    if (!enabled) {
      return;
    }

    const dot = dotRef.current;
    const outline = outlineRef.current;
    if (!dot || !outline) {
      return;
    }

    let pointerX = window.innerWidth / 2;
    let pointerY = window.innerHeight / 2;
    let outlineX = pointerX;
    let outlineY = pointerY;

    const animate = () => {
      outlineX += (pointerX - outlineX) * 0.15;
      outlineY += (pointerY - outlineY) * 0.15;
      outline.style.left = `${outlineX}px`;
      outline.style.top = `${outlineY}px`;
      animationFrameRef.current = window.requestAnimationFrame(animate);
    };

    const setHoverState = (hovering: boolean) => {
      dot.classList.toggle('hovering', hovering);
      outline.classList.toggle('hovering', hovering);
    };

    const onPointerMove = (event: PointerEvent) => {
      pointerX = event.clientX;
      pointerY = event.clientY;
      dot.style.left = `${pointerX}px`;
      dot.style.top = `${pointerY}px`;
      dot.classList.add('is-visible');
      outline.classList.add('is-visible');
    };

    const onPointerLeave = () => {
      dot.classList.remove('is-visible');
      outline.classList.remove('is-visible');
      setHoverState(false);
    };

    const onPointerOver = (event: Event) => {
      setHoverState(isHoverTarget(event.target as Element | null));
    };

    window.addEventListener('pointermove', onPointerMove, { passive: true });
    window.addEventListener('pointerleave', onPointerLeave);
    document.addEventListener('pointerover', onPointerOver);

    animationFrameRef.current = window.requestAnimationFrame(animate);

    return () => {
      window.removeEventListener('pointermove', onPointerMove);
      window.removeEventListener('pointerleave', onPointerLeave);
      document.removeEventListener('pointerover', onPointerOver);

      if (animationFrameRef.current !== null) {
        window.cancelAnimationFrame(animationFrameRef.current);
        animationFrameRef.current = null;
      }

      setHoverState(false);
      dot.classList.remove('is-visible');
      outline.classList.remove('is-visible');
    };
  }, [enabled]);

  if (!enabled) {
    return null;
  }

  return createPortal(
    <>
      <div ref={dotRef} className="cursor-dot" />
      <div ref={outlineRef} className="cursor-outline" />
    </>,
    document.body,
  );
}
