import React, { useState } from 'react';
import { useTiltConnector } from './hooks/useTiltConnector';
import { FloatingGlassCard } from './components/FloatingGlassCard';
import { TelemetryControls } from './components/TelemetryControls';
import { Layers, Box, Compass, Eye, Zap } from 'lucide-react';

export const App: React.FC = () => {
  const {
    tiltX,
    tiltY,
    inputSource,
    permissionState,
    isInteracting,
    requestOrientationPermission,
  } = useTiltConnector();

  const [themeColor, setThemeColor] = useState<'cyan' | 'purple' | 'emerald' | 'gold'>('cyan');

  return (
    <div className="relative min-h-screen bg-[#030712] text-white flex flex-col justify-between overflow-hidden selection:bg-cyan-500/30 selection:text-cyan-200">
      {/* 1. CINEMATIC ZERO-G DEEP SPACE AMBIENT BACKDROP */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden z-0" aria-hidden="true">
        {/* Top-Right Cyan Nebula */}
        <div className="absolute -top-[10%] -right-[10%] w-[550px] h-[550px] rounded-full bg-cyan-600/10 blur-[130px] animate-pulse-slow" />
        {/* Bottom-Left Indigo Nebula */}
        <div className="absolute -bottom-[10%] -left-[10%] w-[600px] h-[600px] rounded-full bg-indigo-600/12 blur-[140px] animate-pulse-slow" style={{ animationDelay: '2s' }} />
        {/* Center Floating Violet Core */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[400px] h-[400px] rounded-full bg-purple-600/8 blur-[120px]" />
        
        {/* Starry Dust Grid */}
        <div 
          className="absolute inset-0 opacity-[0.18]"
          style={{
            backgroundImage: `radial-gradient(rgba(255, 255, 255, 0.45) 1px, transparent 1px)`,
            backgroundSize: '36px 36px',
          }}
        />
      </div>

      {/* 2. TOP NAVIGATION & BADGE */}
      <header className="relative z-10 w-full max-w-5xl mx-auto px-4 sm:px-8 pt-6 sm:pt-8 flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-cyan-400 to-indigo-600 p-0.5 shadow-lg shadow-cyan-500/20">
            <div className="w-full h-full rounded-[10px] bg-slate-950 flex items-center justify-center">
              <Box className="w-5 h-5 text-cyan-400" />
            </div>
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-base sm:text-lg font-black tracking-tight text-white flex items-center gap-1.5 font-mono">
                ZERO-G // GLASS CARD
              </h1>
              <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-cyan-500/10 text-cyan-300 border border-cyan-400/30 font-bold uppercase">
                WebGL 2.0
              </span>
            </div>
            <p className="text-xs text-slate-400">
              Interactive 3D Kinetic Glassmorphism with Dual-Input Sensor Tracking
            </p>
          </div>
        </div>

        {/* Live Interaction Mode Pill */}
        <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-white/5 border border-white/10 backdrop-blur-md text-xs font-mono">
          <div className={`w-2 h-2 rounded-full ${isInteracting ? 'bg-cyan-400 animate-ping' : 'bg-slate-500'}`} />
          <span className="text-slate-300">
            {inputSource === 'gyro' ? 'Gyroscope Active' : inputSource === 'mouse' ? 'Mouse Tracking' : 'Zero-G Float'}
          </span>
        </div>
      </header>

      {/* 3. HERO SHOWCASE: THE ZERO-G FLOATING GLASS CARD */}
      <main className="relative z-10 w-full max-w-5xl mx-auto px-4 sm:px-8 py-6 sm:py-10 flex flex-col items-center justify-center flex-1">
        {/* Interaction Hint */}
        <div className="mb-4 sm:mb-6 text-center">
          <span className="inline-flex items-center gap-2 px-3.5 py-1 rounded-full bg-white/[0.04] border border-white/10 text-xs text-slate-300 font-mono tracking-wide backdrop-blur-md">
            <Compass className="w-3.5 h-3.5 text-cyan-400 animate-spin-slow" />
            <span>Move cursor or tilt device to manipulate 3D specular refraction</span>
          </span>
        </div>

        {/* The Star Component: Floating Glass UI Card */}
        <FloatingGlassCard
          tiltX={tiltX}
          tiltY={tiltY}
          themeColor={themeColor}
          holderName="SHEFEEQ K K"
          cardTier="QUANTUM // ZERO-G NOC MASTER"
          primaryMetric="$ 428,950.00"
          statusText="24/7 BIOMETRIC ACTIVE"
        />

        {/* Live Telemetry Readout & Sensor Controls */}
        <TelemetryControls
          tiltX={tiltX}
          tiltY={tiltY}
          inputSource={inputSource}
          permissionState={permissionState}
          isInteracting={isInteracting}
          onRequestPermission={requestOrientationPermission}
          themeColor={themeColor}
          onChangeTheme={setThemeColor}
        />

        {/* Technical Architecture Feature Cards */}
        <div className="w-full max-w-2xl grid grid-cols-1 sm:grid-cols-3 gap-3 mt-6 sm:mt-8">
          {/* Card 1: Kinetic Physics */}
          <div className="p-4 rounded-xl bg-white/[0.02] border border-white/5 backdrop-blur-md">
            <div className="w-7 h-7 rounded-lg bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center mb-2.5">
              <Zap className="w-4 h-4 text-cyan-400" />
            </div>
            <h2 className="text-xs font-bold text-white uppercase font-mono tracking-wider">
              1. Kinetic Physics
            </h2>
            <p className="text-[11px] text-slate-400 mt-1 leading-relaxed">
              Framer Motion spring dynamics (<span className="text-cyan-300 font-mono">stiffness: 90, damping: 22</span>) mapping 3D perspective pitch and yaw.
            </p>
          </div>

          {/* Card 2: Dual Sensor Connector */}
          <div className="p-4 rounded-xl bg-white/[0.02] border border-white/5 backdrop-blur-md">
            <div className="w-7 h-7 rounded-lg bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center mb-2.5">
              <Compass className="w-4 h-4 text-indigo-400" />
            </div>
            <h2 className="text-xs font-bold text-white uppercase font-mono tracking-wider">
              2. Dual-Sensor Connector
            </h2>
            <p className="text-[11px] text-slate-400 mt-1 leading-relaxed">
              Normalized <span className="text-indigo-300 font-mono">[-1, 1]</span> desktop viewport coordinates and mobile deviceorientation with iOS Safari permission gate.
            </p>
          </div>

          {/* Card 3: WebGL Refraction */}
          <div className="p-4 rounded-xl bg-white/[0.02] border border-white/5 backdrop-blur-md">
            <div className="w-7 h-7 rounded-lg bg-purple-500/10 border border-purple-500/20 flex items-center justify-center mb-2.5">
              <Eye className="w-4 h-4 text-purple-400" />
            </div>
            <h2 className="text-xs font-bold text-white uppercase font-mono tracking-wider">
              3. WebGL Refraction
            </h2>
            <p className="text-[11px] text-slate-400 mt-1 leading-relaxed">
              Three.js physical transmission (<span className="text-purple-300 font-mono">transmission: 0.92</span>) with dynamic point light casting specular glare.
            </p>
          </div>
        </div>
      </main>

      {/* 4. FOOTER */}
      <footer className="relative z-10 w-full max-w-5xl mx-auto px-4 sm:px-8 py-5 border-t border-white/5 flex flex-col sm:flex-row items-center justify-between gap-2 text-xs text-slate-500 font-mono">
        <div>RosterX Advanced Creative UI // Zero-G Floating Glass Component</div>
        <div className="flex items-center gap-1.5 text-slate-400">
          <Layers className="w-3.5 h-3.5 text-cyan-400" />
          <span>Real Parallax Depth at translateZ(30px)</span>
        </div>
      </footer>
    </div>
  );
};
