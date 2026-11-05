import React from 'react';
import { Smartphone, MousePointer, ShieldAlert, CheckCircle2, Activity, Palette } from 'lucide-react';
import { InputSensorSource, DevicePermissionState } from '../hooks/useTiltConnector';

export interface TelemetryControlsProps {
  tiltX: number;
  tiltY: number;
  inputSource: InputSensorSource;
  permissionState: DevicePermissionState;
  isInteracting: boolean;
  onRequestPermission: () => Promise<boolean>;
  themeColor: 'cyan' | 'purple' | 'emerald' | 'gold';
  onChangeTheme: (theme: 'cyan' | 'purple' | 'emerald' | 'gold') => void;
}

export const TelemetryControls: React.FC<TelemetryControlsProps> = ({
  tiltX,
  tiltY,
  inputSource,
  permissionState,
  isInteracting,
  onRequestPermission,
  themeColor,
  onChangeTheme,
}) => {
  // Approximate pitch and roll in degrees for telemetry display
  const pitchDeg = (-tiltY * 16).toFixed(1);
  const rollDeg = (tiltX * 18).toFixed(1);

  return (
    <div className="w-full max-w-2xl mx-auto mt-6 sm:mt-10 p-5 sm:p-6 rounded-2xl bg-white/[0.03] border border-white/10 backdrop-blur-xl shadow-2xl text-xs sm:text-sm text-slate-300">
      {/* Header */}
      <div className="flex items-center justify-between pb-4 border-b border-white/10">
        <div className="flex items-center gap-2 font-mono font-bold text-white tracking-wider">
          <Activity className="w-4 h-4 text-cyan-400 animate-pulse" />
          <span>ZERO-G KINETIC SENSOR TELEMETRY</span>
        </div>
        <div className="flex items-center gap-2 text-xs font-mono">
          <span className="text-slate-500">ENGINE:</span>
          <span className="px-2 py-0.5 rounded bg-cyan-500/10 text-cyan-300 border border-cyan-500/30 font-semibold">
            WebGL + Framer Motion
          </span>
        </div>
      </div>

      {/* Grid: Live Coordinates & Sensor State */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 my-4">
        {/* Normalized Tilt X */}
        <div className="p-3 rounded-xl bg-black/40 border border-white/5 font-mono">
          <div className="text-[10px] text-slate-400 uppercase tracking-widest">Tilt X (Norm)</div>
          <div className="text-base font-bold text-cyan-400 mt-1">
            {tiltX >= 0 ? `+${tiltX.toFixed(3)}` : tiltX.toFixed(3)}
          </div>
          <div className="text-[10px] text-slate-500 mt-0.5">Range: [-1.0, +1.0]</div>
        </div>

        {/* Normalized Tilt Y */}
        <div className="p-3 rounded-xl bg-black/40 border border-white/5 font-mono">
          <div className="text-[10px] text-slate-400 uppercase tracking-widest">Tilt Y (Norm)</div>
          <div className="text-base font-bold text-cyan-400 mt-1">
            {tiltY >= 0 ? `+${tiltY.toFixed(3)}` : tiltY.toFixed(3)}
          </div>
          <div className="text-[10px] text-slate-500 mt-0.5">Range: [-1.0, +1.0]</div>
        </div>

        {/* Calculated Pitch / Roll */}
        <div className="p-3 rounded-xl bg-black/40 border border-white/5 font-mono">
          <div className="text-[10px] text-slate-400 uppercase tracking-widest">3D Pitch / Roll</div>
          <div className="text-base font-bold text-indigo-400 mt-1">
            {pitchDeg}° / {rollDeg}°
          </div>
          <div className="text-[10px] text-slate-500 mt-0.5">Perspective: 1000px</div>
        </div>

        {/* Active Sensor Source */}
        <div className="p-3 rounded-xl bg-black/40 border border-white/5 font-mono">
          <div className="text-[10px] text-slate-400 uppercase tracking-widest">Input Sensor</div>
          <div className="text-sm font-bold text-white mt-1 flex items-center gap-1.5 truncate">
            {inputSource === 'gyro' ? (
              <>
                <Smartphone className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                <span className="text-emerald-300">Device Gyro</span>
              </>
            ) : inputSource === 'mouse' ? (
              <>
                <MousePointer className="w-3.5 h-3.5 text-cyan-400 shrink-0" />
                <span className="text-cyan-300">Desktop Mouse</span>
              </>
            ) : (
              <>
                <Activity className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                <span className="text-slate-400">Idle / Center</span>
              </>
            )}
          </div>
          <div className="text-[10px] text-slate-500 mt-0.5">
            {isInteracting ? 'Streaming Live' : 'Resting State'}
          </div>
        </div>
      </div>

      {/* Interactive Controls & Permission Button */}
      <div className="pt-3 border-t border-white/10 flex flex-wrap items-center justify-between gap-3">
        {/* Theme Picker */}
        <div className="flex items-center gap-2">
          <span className="text-xs text-slate-400 font-mono flex items-center gap-1">
            <Palette className="w-3.5 h-3.5" />
            <span>Theme:</span>
          </span>
          <div className="flex items-center gap-1.5">
            {(['cyan', 'purple', 'emerald', 'gold'] as const).map((color) => {
              const bgColors = {
                cyan: 'bg-cyan-500',
                purple: 'bg-purple-500',
                emerald: 'bg-emerald-500',
                gold: 'bg-amber-500',
              };
              const isSelected = themeColor === color;
              return (
                <button
                  key={color}
                  onClick={() => onChangeTheme(color)}
                  className={`w-6 h-6 rounded-full transition-all cursor-pointer ${bgColors[color]} ${
                    isSelected ? 'ring-2 ring-white scale-110 shadow-lg' : 'opacity-60 hover:opacity-100'
                  }`}
                  title={`Switch to ${color} theme`}
                />
              );
            })}
          </div>
        </div>

        {/* Mobile iOS Permission Trigger */}
        <div className="flex items-center gap-2">
          {permissionState === 'prompt' && (
            <button
              onClick={onRequestPermission}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-300 border border-emerald-400/40 text-xs font-mono font-bold transition cursor-pointer shadow-sm"
            >
              <Smartphone className="w-3.5 h-3.5" />
              <span>Enable Mobile Gyro (iOS)</span>
            </button>
          )}

          {permissionState === 'granted' && (
            <span className="inline-flex items-center gap-1 text-emerald-400 text-xs font-mono">
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>Gyro Active</span>
            </span>
          )}

          {permissionState === 'denied' && (
            <span className="inline-flex items-center gap-1 text-rose-400 text-xs font-mono">
              <ShieldAlert className="w-3.5 h-3.5" />
              <span>Gyro Blocked</span>
            </span>
          )}
        </div>
      </div>
    </div>
  );
};
