import React, { useEffect } from 'react';
import { motion, useMotionValue, useSpring, useTransform } from 'framer-motion';
import { GlassCanvas } from './GlassCanvas';
import { Wifi, ShieldCheck, Sparkles } from 'lucide-react';

export interface FloatingGlassCardProps {
  /** Normalized tilt coordinates from sensor [-1, 1] */
  tiltX: number;
  /** Normalized tilt coordinates from sensor [-1, 1] */
  tiltY: number;
  /** Color theme for WebGL specular highlight and neon glow */
  themeColor?: 'cyan' | 'purple' | 'emerald' | 'gold';
  /** Cardholder display name */
  holderName?: string;
  /** Account title / Tier badge */
  cardTier?: string;
  /** Primary operational balance or ID metric */
  primaryMetric?: string;
  /** Secondary micro-tag or status text */
  statusText?: string;
  /** Custom CSS class names */
  className?: string;
}

const THEME_CONFIG = {
  cyan: {
    specular: '#38bdf8',
    fresnel: '#818cf8',
    glow: 'rgba(56, 189, 248, 0.25)',
    borderGlow: 'hover:border-cyan-400/40',
    accentText: 'text-cyan-300',
    tagBg: 'bg-cyan-500/15 border-cyan-400/30 text-cyan-200',
    chipGradient: 'from-amber-200 via-yellow-400 to-amber-600',
  },
  purple: {
    specular: '#c084fc',
    fresnel: '#e879f9',
    glow: 'rgba(192, 132, 252, 0.25)',
    borderGlow: 'hover:border-purple-400/40',
    accentText: 'text-purple-300',
    tagBg: 'bg-purple-500/15 border-purple-400/30 text-purple-200',
    chipGradient: 'from-purple-200 via-indigo-300 to-purple-500',
  },
  emerald: {
    specular: '#34d399',
    fresnel: '#38bdf8',
    glow: 'rgba(52, 211, 153, 0.25)',
    borderGlow: 'hover:border-emerald-400/40',
    accentText: 'text-emerald-300',
    tagBg: 'bg-emerald-500/15 border-emerald-400/30 text-emerald-200',
    chipGradient: 'from-emerald-200 via-teal-300 to-emerald-500',
  },
  gold: {
    specular: '#fbbf24',
    fresnel: '#f97316',
    glow: 'rgba(251, 191, 36, 0.25)',
    borderGlow: 'hover:border-amber-400/40',
    accentText: 'text-amber-300',
    tagBg: 'bg-amber-500/15 border-amber-400/30 text-amber-200',
    chipGradient: 'from-yellow-100 via-amber-300 to-yellow-600',
  },
};

/**
 * Zero-G Floating Glass UI Card Component
 * Integrates:
 * 1. 3D perspective viewport (perspective: 1000px)
 * 2. Framer Motion spring physics mapping tilt values to rotateX, rotateY, x, y
 * 3. Continuous subtle floating bob animation on Y-axis
 * 4. WebGL backdrop GlassCanvas with realistic transmission & dynamic point light
 * 5. Multi-depth parallax layering with translateZ(30px)
 */
export const FloatingGlassCard: React.FC<FloatingGlassCardProps> = ({
  tiltX,
  tiltY,
  themeColor = 'cyan',
  holderName = 'SHEFEEQ K K',
  cardTier = 'QUANTUM // ZERO-G EXCLUSIVE',
  primaryMetric = '$ 428,950.00',
  statusText = '24/7 BIOMETRIC ACTIVE',
  className = '',
}) => {
  const theme = THEME_CONFIG[themeColor] || THEME_CONFIG.cyan;

  // 1. Raw Framer Motion values for normalized sensor tilt coordinates
  const rawTiltX = useMotionValue(0);
  const rawTiltY = useMotionValue(0);

  // Sync incoming sensor props to motion values
  useEffect(() => {
    rawTiltX.set(tiltX);
    rawTiltY.set(tiltY);
  }, [tiltX, tiltY, rawTiltX, rawTiltY]);

  // 2. Kinetic Floating Physics: Spring dynamics (stiffness: 90, damping: 22, mass: 0.8)
  const springConfig = { stiffness: 90, damping: 22, mass: 0.8 };
  const smoothTiltX = useSpring(rawTiltX, springConfig);
  const smoothTiltY = useSpring(rawTiltY, springConfig);

  // 3. 3D Rotations and Micro-Translations
  // Invert Y tilt so moving cursor up tilts the top backwards
  const rotateX = useTransform(smoothTiltY, [-1, 1], [16, -16]);
  const rotateY = useTransform(smoothTiltX, [-1, 1], [-18, 18]);
  const translateX = useTransform(smoothTiltX, [-1, 1], [-14, 14]);
  const translateY = useTransform(smoothTiltY, [-1, 1], [-14, 14]);

  // 4. Glare highlight sheen position (dynamic CSS gradient tracker)
  const glareX = useTransform(smoothTiltX, [-1, 1], [15, 85]);
  const glareY = useTransform(smoothTiltY, [-1, 1], [15, 85]);

  return (
    // 3D Perspective Viewport
    <div
      className={`relative perspective-1000 flex items-center justify-center p-4 sm:p-8 select-none ${className}`}
      style={{ perspective: 1000 }}
    >
      {/* Subtle Zero-G Ambient Backlight Glow that moves with tilt */}
      <motion.div
        className="absolute w-[360px] h-[220px] sm:w-[460px] sm:h-[280px] rounded-[40px] pointer-events-none filter blur-3xl opacity-60 transition-colors duration-700"
        style={{
          background: `radial-gradient(circle, ${theme.glow} 0%, transparent 70%)`,
          x: translateX,
          y: translateY,
        }}
      />

      {/* Floating Bob Wrapper: Loop infinitely on Y-axis with easeInOut */}
      <motion.div
        animate={{ y: [0, -12, 0] }}
        transition={{
          duration: 5.5,
          repeat: Infinity,
          ease: 'easeInOut',
        }}
        className="preserve-3d"
      >
        {/* Kinetic Physics Card Body */}
        <motion.div
          style={{
            rotateX,
            rotateY,
            x: translateX,
            y: translateY,
            transformStyle: 'preserve-3d',
          }}
          whileHover={{ scale: 1.02 }}
          transition={{ type: 'spring', stiffness: 200, damping: 25 }}
          className="relative w-[340px] h-[215px] sm:w-[420px] sm:h-[265px] rounded-3xl p-6 sm:p-7 border border-white/20 backdrop-blur-xl shadow-2xl shadow-black/60 transition-all duration-300 overflow-hidden cursor-grab active:cursor-grabbing"
        >
          {/* LAYER 0: WebGL Refraction & Specular Pass Canvas */}
          <GlassCanvas
            tiltX={tiltX}
            tiltY={tiltY}
            specularColor={theme.specular}
            fresnelColor={theme.fresnel}
          />

          {/* LAYER 1: Optical Specular Glare Overlay tracking normalized coordinates */}
          <motion.div
            className="absolute inset-0 pointer-events-none rounded-3xl opacity-35 mix-blend-overlay transition-opacity duration-300"
            style={{
              background: useTransform(
                [glareX, glareY],
                ([x, y]) =>
                  `radial-gradient(circle at ${x}% ${y}%, rgba(255, 255, 255, 0.8) 0%, rgba(255, 255, 255, 0.1) 35%, transparent 65%)`
              ),
            }}
          />

          {/* LAYER 2: Fine Glass Edge Highlights & Micro-Grid Sheen */}
          <div
            className="absolute inset-0 pointer-events-none rounded-3xl border border-white/25 shadow-[inset_0_1px_2px_rgba(255,255,255,0.4),inset_0_-1px_2px_rgba(0,0,0,0.3)]"
            style={{ transform: 'translateZ(10px)' }}
          />

          {/* LAYER 3: FRONT UI LAYER with true Parallax Depth (translateZ: 30px) */}
          <div
            className="relative z-10 h-full flex flex-col justify-between preserve-3d"
            style={{ transform: 'translateZ(30px)' }}
          >
            {/* Top Bar: Holographic Header & Contactless Icon */}
            <div
              className="flex items-center justify-between preserve-3d"
              style={{ transform: 'translateZ(12px)' }}
            >
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-xl bg-white/10 border border-white/20 flex items-center justify-center backdrop-blur-md shadow-inner">
                  <Sparkles className={`w-4 h-4 ${theme.accentText} animate-pulse-slow`} />
                </div>
                <div>
                  <div className="text-[10px] font-mono tracking-widest uppercase text-white/60">
                    SECURE ENCLAVE
                  </div>
                  <div className="text-xs sm:text-sm font-bold tracking-wider text-white flex items-center gap-1.5">
                    {cardTier}
                  </div>
                </div>
              </div>

              {/* Contactless Waves & Chip Icon */}
              <div className="flex items-center gap-2 text-white/70">
                <Wifi className="w-5 h-5 rotate-90 text-white/80" />
                <div className="w-2.5 h-2.5 rounded-full bg-emerald-400 shadow-[0_0_8px_rgba(52,211,153,0.8)] animate-pulse" />
              </div>
            </div>

            {/* Middle Section: Security Microchip & Balance Telemetry */}
            <div
              className="flex items-center justify-between my-auto preserve-3d"
              style={{ transform: 'translateZ(20px)' }}
            >
              {/* EMV Gold Circuit Microchip */}
              <div
                className="relative w-12 h-9 sm:w-14 sm:h-10 rounded-lg p-1 bg-gradient-to-br from-amber-200 via-amber-400 to-amber-600 border border-amber-300/60 shadow-lg shadow-amber-950/40 preserve-3d"
                style={{ transform: 'translateZ(15px)' }}
              >
                {/* Circuit Grid Lines */}
                <div className="w-full h-full rounded border border-amber-800/40 grid grid-cols-3 grid-rows-2 gap-0.5 p-0.5">
                  <div className="border border-amber-900/30 rounded-xs bg-amber-300/30" />
                  <div className="border border-amber-900/30 rounded-xs bg-amber-300/30" />
                  <div className="border border-amber-900/30 rounded-xs bg-amber-300/30" />
                  <div className="border border-amber-900/30 rounded-xs bg-amber-300/30" />
                  <div className="border border-amber-900/30 rounded-xs bg-amber-300/30" />
                  <div className="border border-amber-900/30 rounded-xs bg-amber-300/30" />
                </div>
              </div>

              {/* Primary Balance / Telemetry Metric */}
              <div className="text-right preserve-3d" style={{ transform: 'translateZ(16px)' }}>
                <span className="text-[10px] sm:text-xs font-mono uppercase text-white/60 tracking-wider">
                  NOC LIQUIDITY INDEX
                </span>
                <div className="text-xl sm:text-2xl font-black font-mono tracking-tight text-white drop-shadow-[0_2px_10px_rgba(0,0,0,0.8)]">
                  {primaryMetric}
                </div>
              </div>
            </div>

            {/* Card Number / Identity Code */}
            <div
              className="font-mono text-xs sm:text-sm tracking-[0.25em] text-white/85 font-medium flex items-center justify-between preserve-3d"
              style={{ transform: 'translateZ(14px)' }}
            >
              <span>4096</span>
              <span>8820</span>
              <span>1024</span>
              <span>7769</span>
            </div>

            {/* Bottom Bar: Holder Name, Expiry, & Micro-Tag */}
            <div
              className="flex items-end justify-between pt-2 border-t border-white/10 preserve-3d"
              style={{ transform: 'translateZ(10px)' }}
            >
              <div>
                <div className="text-[9px] font-mono tracking-wider uppercase text-white/50">
                  AUTHORIZED OFFICER
                </div>
                <div className="text-xs sm:text-sm font-extrabold uppercase tracking-wide text-white drop-shadow-sm">
                  {holderName}
                </div>
              </div>

              <div className="flex items-center gap-3">
                <div className="text-right">
                  <div className="text-[8px] font-mono uppercase text-white/50">VALID THRU</div>
                  <div className="text-[11px] font-mono font-bold text-white/90">11 / 30</div>
                </div>

                {/* Biometric Status Micro-Tag */}
                <div
                  className={`flex items-center gap-1 text-[9px] sm:text-[10px] font-mono font-bold px-2.5 py-1 rounded-full border backdrop-blur-md shadow-xs ${theme.tagBg}`}
                >
                  <ShieldCheck className="w-3 h-3" />
                  <span>{statusText}</span>
                </div>
              </div>
            </div>
          </div>
        </motion.div>
      </motion.div>
    </div>
  );
};
