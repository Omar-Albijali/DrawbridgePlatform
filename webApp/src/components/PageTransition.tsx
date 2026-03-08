import { motion } from 'framer-motion';
import type { ReactNode } from 'react';

const pageVariants = {
  initial: { opacity: 0, y: 12 },
  enter: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.45,
      ease: [0.16, 1, 0.3, 1],
    },
  },
  exit: {
    opacity: 0,
    y: -8,
    transition: {
      duration: 0.3,
      ease: [0.7, 0, 0.84, 0],
    },
  },
} as const;

export default function PageTransition({ children }: { children: ReactNode }): JSX.Element {
  return (
    <motion.div variants={pageVariants} initial="initial" animate="enter" exit="exit">
      {children}
    </motion.div>
  );
}
