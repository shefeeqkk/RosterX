import React, { useEffect, useRef } from 'react';
import * as THREE from 'three';

export interface GlassCanvasProps {
  /** Normalized X tilt from sensor [-1, 1] */
  tiltX: number;
  /** Normalized Y tilt from sensor [-1, 1] */
  tiltY: number;
  /** Primary specular light highlight color (Hex or CSS color string) */
  specularColor?: string;
  /** Secondary edge fresnel accent color */
  fresnelColor?: string;
  /** Card aspect ratio width / height (default: 1.586 for credit-card ratio) */
  aspectRatio?: number;
  /** Custom className for positioning */
  className?: string;
}

/**
 * WebGL Backdrop Component powered by Three.js
 * Generates an optical glass plate with high transmission (~0.9),
 * low roughness (~0.15), clearcoat sheen, and a dynamic point light that
 * tracks cursor/device orientation to cast specular glare and edge fresnel.
 */
export const GlassCanvas: React.FC<GlassCanvasProps> = ({
  tiltX,
  tiltY,
  specularColor = '#38bdf8',
  fresnelColor = '#818cf8',
  className = '',
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const tiltRef = useRef<{ x: number; y: number }>({ x: tiltX, y: tiltY });
  const lightColorRef = useRef<string>(specularColor);
  const fresnelColorRef = useRef<string>(fresnelColor);

  // Keep refs in sync for the animation loop
  useEffect(() => {
    tiltRef.current = { x: tiltX, y: tiltY };
  }, [tiltX, tiltY]);

  useEffect(() => {
    lightColorRef.current = specularColor;
    fresnelColorRef.current = fresnelColor;
  }, [specularColor, fresnelColor]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container || typeof window === 'undefined') return;

    // 1. SCENE & CAMERA SETUP
    const scene = new THREE.Scene();
    const width = container.clientWidth || 380;
    const height = container.clientHeight || 240;

    const camera = new THREE.PerspectiveCamera(42, width / height, 0.1, 100);
    camera.position.z = 4.2;

    // 2. WEBGL RENDERER
    const renderer = new THREE.WebGLRenderer({
      alpha: true,
      antialias: true,
      powerPreference: 'high-performance',
    });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.setSize(width, height);
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.15;
    container.appendChild(renderer.domElement);

    // 3. CREATE ROUNDED RECTANGLE GLASS GEOMETRY
    const cardWidth = 3.6;
    const cardHeight = 2.25;
    const radius = 0.28;

    const shape = new THREE.Shape();
    const x0 = -cardWidth / 2;
    const y0 = -cardHeight / 2;
    const x1 = cardWidth / 2;
    const y1 = cardHeight / 2;

    shape.moveTo(x0 + radius, y0);
    shape.lineTo(x1 - radius, y0);
    shape.quadraticCurveTo(x1, y0, x1, y0 + radius);
    shape.lineTo(x1, y1 - radius);
    shape.quadraticCurveTo(x1, y1, x1 - radius, y1);
    shape.lineTo(x0 + radius, y1);
    shape.quadraticCurveTo(x0, y1, x0, y1 - radius);
    shape.lineTo(x0, y0 + radius);
    shape.quadraticCurveTo(x0, y0, x0 + radius, y0);

    const extrudeSettings: THREE.ExtrudeGeometryOptions = {
      depth: 0.14,
      bevelEnabled: true,
      bevelSegments: 6,
      steps: 1,
      bevelSize: 0.05,
      bevelThickness: 0.06,
    };

    const geometry = new THREE.ExtrudeGeometry(shape, extrudeSettings);
    geometry.center();

    // 4. GLASS PHYSICAL MATERIAL
    // Attributes: high transmission (~0.9), low roughness (~0.15), clearcoat sheen
    const glassMaterial = new THREE.MeshPhysicalMaterial({
      color: new THREE.Color('#dbeafe'),
      emissive: new THREE.Color('#0f172a'),
      emissiveIntensity: 0.15,
      metalness: 0.02,
      roughness: 0.14,
      transmission: 0.92, // Optical transmission through glass
      ior: 1.52,          // Crown glass index of refraction
      reflectivity: 0.85,
      clearcoat: 1.0,     // Polyurethane-like high gloss sheen
      clearcoatRoughness: 0.08,
      specularIntensity: 1.2,
      specularColor: new THREE.Color('#ffffff'),
      transparent: true,
      opacity: 0.92,
      side: THREE.DoubleSide,
    });

    const glassMesh = new THREE.Mesh(geometry, glassMaterial);
    scene.add(glassMesh);

    // 5. CAUSTICS / BACKGROUND REFRACTIVE ACCENTS (Subtle iridescent rings behind the glass)
    const ringGeo = new THREE.TorusGeometry(0.85, 0.025, 16, 64);
    const ringMat = new THREE.MeshBasicMaterial({
      color: new THREE.Color('#38bdf8'),
      transparent: true,
      opacity: 0.35,
    });
    const ringMesh = new THREE.Mesh(ringGeo, ringMat);
    ringMesh.position.set(0.9, -0.4, -0.4);
    scene.add(ringMesh);

    const ringGeo2 = new THREE.TorusGeometry(0.55, 0.02, 16, 48);
    const ringMat2 = new THREE.MeshBasicMaterial({
      color: new THREE.Color('#a855f7'),
      transparent: true,
      opacity: 0.3,
    });
    const ringMesh2 = new THREE.Mesh(ringGeo2, ringMat2);
    ringMesh2.position.set(-0.8, 0.5, -0.35);
    scene.add(ringMesh2);

    // 6. DYNAMIC LIGHTING PASS
    // Soft ambient light
    const ambientLight = new THREE.AmbientLight(0x0f172a, 1.2);
    scene.add(ambientLight);

    // Fixed key directional light
    const dirLight = new THREE.DirectionalLight(0xffffff, 1.4);
    dirLight.position.set(2, 4, 3);
    scene.add(dirLight);

    // Dynamic Tracking Point Light (Creates moving specular glare that tracks tilt)
    const trackingLight = new THREE.PointLight(new THREE.Color(lightColorRef.current), 4.5, 9, 1.6);
    trackingLight.position.set(0, 0, 2.2);
    scene.add(trackingLight);

    // Grazing Edge Fresnel Point Light
    const fresnelLight = new THREE.PointLight(new THREE.Color(fresnelColorRef.current), 3.0, 7, 2.0);
    fresnelLight.position.set(0, 0, -0.6);
    scene.add(fresnelLight);

    // 7. RENDER & ANIMATION LOOP
    let animationFrameId: number;
    let clock = new THREE.Clock();

    const animate = () => {
      animationFrameId = requestAnimationFrame(animate);

      const elapsedTime = clock.getElapsedTime();
      const targetX = tiltRef.current.x;
      const targetY = tiltRef.current.y;

      // Update light colors dynamically if changed
      trackingLight.color.set(lightColorRef.current);
      fresnelLight.color.set(fresnelColorRef.current);

      // Smoothly track specular light position based on tilt coordinates
      // X tilt shifts light horizontally, Y tilt shifts vertically
      trackingLight.position.x = THREE.MathUtils.lerp(trackingLight.position.x, targetX * 3.6, 0.12);
      trackingLight.position.y = THREE.MathUtils.lerp(trackingLight.position.y, -targetY * 2.8, 0.12);
      trackingLight.position.z = 2.0 + Math.sin(elapsedTime * 1.5) * 0.2;

      // Rim light moves opposite to emphasize edge fresnel sheen
      fresnelLight.position.x = THREE.MathUtils.lerp(fresnelLight.position.x, -targetX * 2.5, 0.08);
      fresnelLight.position.y = THREE.MathUtils.lerp(fresnelLight.position.y, targetY * 2.0, 0.08);

      // Micro internal rotation on glass plate
      glassMesh.rotation.y = THREE.MathUtils.lerp(glassMesh.rotation.y, targetX * 0.12, 0.1);
      glassMesh.rotation.x = THREE.MathUtils.lerp(glassMesh.rotation.x, targetY * 0.12, 0.1);

      // Slow orbital drift on background rings for refractive shimmer
      ringMesh.rotation.z = elapsedTime * 0.2;
      ringMesh2.rotation.z = -elapsedTime * 0.25;

      renderer.render(scene, camera);
    };

    animate();

    // 8. RESIZE HANDLING
    const handleResize = () => {
      if (!container) return;
      const newWidth = container.clientWidth;
      const newHeight = container.clientHeight;
      if (newWidth === 0 || newHeight === 0) return;

      camera.aspect = newWidth / newHeight;
      camera.updateProjectionMatrix();
      renderer.setSize(newWidth, newHeight);
    };

    const resizeObserver = new ResizeObserver(handleResize);
    resizeObserver.observe(container);

    // 9. CLEANUP
    return () => {
      cancelAnimationFrame(animationFrameId);
      resizeObserver.disconnect();

      geometry.dispose();
      glassMaterial.dispose();
      ringGeo.dispose();
      ringMat.dispose();
      ringGeo2.dispose();
      ringMat2.dispose();
      renderer.dispose();

      if (renderer.domElement.parentNode === container) {
        container.removeChild(renderer.domElement);
      }
    };
  }, []);

  return (
    <div
      ref={containerRef}
      className={`absolute inset-0 pointer-events-none rounded-3xl overflow-hidden ${className}`}
      style={{
        zIndex: 0,
      }}
      aria-hidden="true"
    />
  );
};

