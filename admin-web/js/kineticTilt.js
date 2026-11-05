/**
 * RosterX — Kinetic Zero-G Physics Controller
 * 
 * Provides:
 * - Dual-input sensor tracking: Desktop mouse coordinates [-1, 1] + Mobile deviceorientation gyroscope
 * - Second-order spring dynamics (stiffness: 90, damping: 22, mass: 0.8)
 * - Infinite subtle Zero-G floating bobbing and micro-drift
 * - Parallax stereoscopic 3D depth (perspective: 1000px, preserve-3d, translateZ layers)
 */

export class KineticTiltController {
  constructor(containerSelector = '.kinetic-hero-card') {
    this.selector = containerSelector;
    this.elements = [];
    this.tiltX = 0;
    this.tiltY = 0;
    this.targetTiltX = 0;
    this.targetTiltY = 0;
    this.velocityX = 0;
    this.velocityY = 0;

    // Spring constants matching specification
    this.stiffness = 90;
    this.damping = 22;
    this.mass = 0.8;

    this.rafId = null;
    this.lastTime = performance.now();
    this.bobPhase = 0;

    this.init();
  }

  init() {
    if (typeof window === 'undefined') return;

    this.elements = Array.from(document.querySelectorAll(this.selector));
    if (this.elements.length === 0) return;

    // Apply 3D perspective styling to card containers
    this.elements.forEach(el => {
      el.style.transformStyle = 'preserve-3d';
      el.style.transition = 'box-shadow 0.3s ease, border-color 0.3s ease';
      const inner = el.querySelector('.kinetic-depth-layer');
      if (inner) {
        inner.style.transform = 'translateZ(25px)';
        inner.style.transformStyle = 'preserve-3d';
      }
    });

    this.bindEvents();
    this.startLoop();
  }

  refresh() {
    this.elements = Array.from(document.querySelectorAll(this.selector));
    this.elements.forEach(el => {
      el.style.transformStyle = 'preserve-3d';
      const inner = el.querySelector('.kinetic-depth-layer');
      if (inner) {
        inner.style.transform = 'translateZ(25px)';
        inner.style.transformStyle = 'preserve-3d';
      }
    });
  }

  bindEvents() {
    // 1. Desktop Mouse Tracking (Normalized [-1, 1])
    window.addEventListener('mousemove', (e) => {
      const w = window.innerWidth || 1;
      const h = window.innerHeight || 1;
      const rawX = (e.clientX / w) * 2 - 1;
      const rawY = (e.clientY / h) * 2 - 1;
      this.targetTiltX = Math.max(-1, Math.min(1, rawX));
      this.targetTiltY = Math.max(-1, Math.min(1, rawY));
    }, { passive: true });

    window.addEventListener('mouseleave', () => {
      this.targetTiltX = 0;
      this.targetTiltY = 0;
    }, { passive: true });

    // 2. Mobile Gyroscope Orientation Tracking
    window.addEventListener('deviceorientation', (e) => {
      if (e.gamma === null || e.beta === null) return;
      const gyroMax = 35;
      const restingPitch = 40;
      const clampedGamma = Math.max(-gyroMax, Math.min(gyroMax, e.gamma));
      const adjustedBeta = e.beta - restingPitch;
      const clampedBeta = Math.max(-gyroMax, Math.min(gyroMax, adjustedBeta));

      this.targetTiltX = clampedGamma / gyroMax;
      this.targetTiltY = clampedBeta / gyroMax;
    }, { passive: true });
  }

  startLoop() {
    const step = (now) => {
      const dt = Math.min((now - this.lastTime) / 1000, 0.05); // Cap delta time
      this.lastTime = now;

      // Spring dynamic simulation: F = -k*(x - target) - d*v
      const forceX = -this.stiffness * (this.tiltX - this.targetTiltX) - this.damping * this.velocityX;
      const forceY = -this.stiffness * (this.tiltY - this.targetTiltY) - this.damping * this.velocityY;

      const ax = forceX / this.mass;
      const ay = forceY / this.mass;

      this.velocityX += ax * dt;
      this.velocityY += ay * dt;

      this.tiltX += this.velocityX * dt;
      this.tiltY += this.velocityY * dt;

      // Continuous Zero-G subtle floating bob
      this.bobPhase += dt * 1.8;
      const bobY = Math.sin(this.bobPhase) * 3.5;
      const bobRot = Math.cos(this.bobPhase * 0.8) * 0.75;

      const rotX = (-this.tiltY * 12 + bobRot).toFixed(2);
      const rotY = (this.tiltX * 12).toFixed(2);
      const transX = (this.tiltX * 6).toFixed(2);
      const transY = (this.tiltY * 6 + bobY).toFixed(2);

      // Apply 3D transforms to active cards
      for (let i = 0; i < this.elements.length; i++) {
        const el = this.elements[i];
        if (!el || el.offsetParent === null) continue; // Skip hidden elements
        el.style.transform = `perspective(1000px) translate3d(${transX}px, ${transY}px, 0px) rotateX(${rotX}deg) rotateY(${rotY}deg)`;
      }

      this.rafId = requestAnimationFrame(step);
    };

    this.rafId = requestAnimationFrame(step);
  }

  destroy() {
    if (this.rafId) {
      cancelAnimationFrame(this.rafId);
    }
  }
}

