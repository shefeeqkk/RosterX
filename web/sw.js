/**
 * RosterX — Lightweight Installable PWA Service Worker (sw.js)
 * - Network-first (never cached stale) for /api/* and auth endpoints
 * - Network-first with offline cache fallback for HTML, JS, and CSS so iOS & Desktop PWAs pick up hotfixes immediately
 * - Stale-while-revalidate for static media assets (images, icons, manifest)
 * - Safe instant activation on new deployments
 */
const CACHE_NAME = 'rosterx-pwa-v2.3.17';
const STATIC_ASSETS = [
  '/manifest.webmanifest',
  '/css/tokens.css',
  '/css/neumorphic.css',
  '/css/mobile-shell.css',
  '/assets/logo.png',
  '/assets/favicon.png',
  '/assets/bot-3d-cutout.png'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(STATIC_ASSETS).catch(() => {}))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(
        keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k))
      ))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const req = event.request;
  if (req.method !== 'GET') return;

  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return;

  // Never cache API calls, well-known assetlinks, or /open/* redirects
  if (
    url.pathname.startsWith('/api/') ||
    url.pathname.startsWith('/.well-known/') ||
    url.pathname.startsWith('/open')
  ) {
    return;
  }

  // HTML navigations, JS modules & CSS stylesheets: network-first with cache fallback
  // Ensures installed iOS & Desktop PWAs receive hotfixes immediately on reload
  if (
    req.mode === 'navigate' ||
    url.pathname === '/' ||
    url.pathname.endsWith('.html') ||
    url.pathname.endsWith('.js') ||
    url.pathname.endsWith('.mjs') ||
    url.pathname.endsWith('.css')
  ) {
    event.respondWith(
      fetch(req)
        .then((resp) => {
          if (resp && resp.ok) {
            const copy = resp.clone();
            caches.open(CACHE_NAME).then((c) => c.put(req, copy)).catch(() => {});
          }
          return resp;
        })
        .catch(() => caches.match(req))
    );
    return;
  }

  // Static images, icons & manifest: stale-while-revalidate
  event.respondWith(
    caches.match(req).then((cached) => {
      const networkFetch = fetch(req)
        .then((resp) => {
          if (resp && resp.ok) {
            const copy = resp.clone();
            caches.open(CACHE_NAME).then((c) => c.put(req, copy)).catch(() => {});
          }
          return resp;
        })
        .catch(() => cached);
      return cached || networkFetch;
    })
  );
});
