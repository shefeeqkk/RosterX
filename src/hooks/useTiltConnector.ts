import { useState, useEffect, useCallback, useRef } from 'react';

export type InputSensorSource = 'mouse' | 'gyro' | 'idle';
export type DevicePermissionState = 'prompt' | 'granted' | 'denied' | 'not-supported';

export interface TiltConnectorState {
  /** Normalized X tilt coordinate in [-1, 1] range (horizontal) */
  tiltX: number;
  /** Normalized Y tilt coordinate in [-1, 1] range (vertical) */
  tiltY: number;
  /** Primary active input sensor currently driving the coordinates */
  inputSource: InputSensorSource;
  /** Gyroscope sensor permission state (critical for iOS Safari) */
  permissionState: DevicePermissionState;
  /** Whether the user is actively interacting (moving cursor or tilting device) */
  isInteracting: boolean;
  /** Trigger function to prompt iOS Safari for DeviceOrientation permissions */
  requestOrientationPermission: () => Promise<boolean>;
}

export interface UseTiltConnectorOptions {
  /** Maximum angle threshold in degrees for mobile gyroscope clamping (default: 35) */
  gyroMaxAngle?: number;
  /** Resting pitch angle in degrees when holding phone comfortably (default: 40) */
  gyroRestingPitch?: number;
  /** Idle auto-return timeout in milliseconds after mouse movement stops (default: 1500) */
  idleTimeoutMs?: number;
}

/**
 * Universal Dual-Input Sensor Connector Hook
 * Unifies Desktop mouse tracking with Mobile deviceorientation sensors into
 * a normalized [-1, 1] 2D coordinate space with iOS Safari permission handling.
 */
export function useTiltConnector(options: UseTiltConnectorOptions = {}): TiltConnectorState {
  const {
    gyroMaxAngle = 35,
    gyroRestingPitch = 40,
    idleTimeoutMs = 2000,
  } = options;

  const [tilt, setTilt] = useState<{ x: number; y: number }>({ x: 0, y: 0 });
  const [inputSource, setInputSource] = useState<InputSensorSource>('idle');
  const [isInteracting, setIsInteracting] = useState<boolean>(false);
  const [permissionState, setPermissionState] = useState<DevicePermissionState>('prompt');

  const idleTimerRef = useRef<number | null>(null);
  const isTouchDeviceRef = useRef<boolean>(false);

  // Mark activity and reset idle timer
  const registerActivity = useCallback(() => {
    setIsInteracting(true);
    if (idleTimerRef.current !== null) {
      window.clearTimeout(idleTimerRef.current);
    }
    idleTimerRef.current = window.setTimeout(() => {
      setIsInteracting(false);
      // Gently settle to center when idle
      setTilt((prev) => ({ x: prev.x * 0.9, y: prev.y * 0.9 }));
    }, idleTimeoutMs);
  }, [idleTimeoutMs]);

  // Request orientation permissions (iOS 13+ requirement)
  const requestOrientationPermission = useCallback(async (): Promise<boolean> => {
    if (typeof window === 'undefined') return false;

    const DeviceOrientation = window.DeviceOrientationEvent as unknown as {
      requestPermission?: () => Promise<'granted' | 'denied'>;
    };

    if (typeof DeviceOrientation?.requestPermission === 'function') {
      try {
        const response = await DeviceOrientation.requestPermission();
        if (response === 'granted') {
          setPermissionState('granted');
          return true;
        } else {
          setPermissionState('denied');
          return false;
        }
      } catch (err) {
        console.warn('[useTiltConnector] Error requesting deviceorientation permission:', err);
        setPermissionState('denied');
        return false;
      }
    } else {
      // Standard browsers with non-permission gate
      setPermissionState('granted');
      return true;
    }
  }, []);

  useEffect(() => {
    if (typeof window === 'undefined') return;

    // Detect if device has accelerometer/orientation support
    const hasOrientation = 'DeviceOrientationEvent' in window;
    const DeviceOrientation = window.DeviceOrientationEvent as unknown as {
      requestPermission?: () => Promise<'granted' | 'denied'>;
    };

    if (!hasOrientation) {
      setPermissionState('not-supported');
    } else if (typeof DeviceOrientation?.requestPermission === 'function') {
      setPermissionState('prompt');
    } else {
      setPermissionState('granted');
    }

    // 1. DESKTOP MOUSEMOVE LISTENER
    const handleMouseMove = (e: MouseEvent) => {
      // If mobile gyro is actively streaming, prioritize gyro
      if (inputSource === 'gyro' && isTouchDeviceRef.current) return;

      const width = window.innerWidth || 1;
      const height = window.innerHeight || 1;

      // Normalize client coordinates to [-1, 1] from viewport center
      const rawX = (e.clientX / width) * 2 - 1;
      const rawY = (e.clientY / height) * 2 - 1;

      // Clamp strictly within [-1, 1]
      const clampedX = Math.max(-1, Math.min(1, rawX));
      const clampedY = Math.max(-1, Math.min(1, rawY));

      setTilt({ x: clampedX, y: clampedY });
      setInputSource('mouse');
      registerActivity();
    };

    // 2. MOBILE GYROSCOPE ORIENTATION LISTENER
    const handleDeviceOrientation = (e: DeviceOrientationEvent) => {
      // e.gamma: Left-to-Right tilt [-90, 90]
      // e.beta: Front-to-Back tilt [-180, 180]
      if (e.gamma === null || e.beta === null) return;

      isTouchDeviceRef.current = true;

      // Normalize gamma (X-axis tilt)
      const clampedGamma = Math.max(-gyroMaxAngle, Math.min(gyroMaxAngle, e.gamma));
      const normX = clampedGamma / gyroMaxAngle;

      // Normalize beta (Y-axis tilt, adjusted for comfortable holding angle ~40deg)
      const adjustedBeta = e.beta - gyroRestingPitch;
      const clampedBeta = Math.max(-gyroMaxAngle, Math.min(gyroMaxAngle, adjustedBeta));
      const normY = clampedBeta / gyroMaxAngle;

      setTilt({
        x: Math.max(-1, Math.min(1, normX)),
        y: Math.max(-1, Math.min(1, normY)),
      });
      setInputSource('gyro');
      registerActivity();
    };

    // 3. MOUSE LEAVE (Settle to zero gravity resting center)
    const handleMouseLeave = () => {
      if (inputSource === 'mouse') {
        setTilt({ x: 0, y: 0 });
        setIsInteracting(false);
      }
    };

    window.addEventListener('mousemove', handleMouseMove, { passive: true });
    window.addEventListener('mouseleave', handleMouseLeave, { passive: true });
    window.addEventListener('deviceorientation', handleDeviceOrientation, { passive: true });

    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseleave', handleMouseLeave);
      window.removeEventListener('deviceorientation', handleDeviceOrientation);
      if (idleTimerRef.current !== null) {
        window.clearTimeout(idleTimerRef.current);
      }
    };
  }, [gyroMaxAngle, gyroRestingPitch, inputSource, registerActivity]);

  return {
    tiltX: tilt.x,
    tiltY: tilt.y,
    inputSource,
    permissionState,
    isInteracting,
    requestOrientationPermission,
  };
}
