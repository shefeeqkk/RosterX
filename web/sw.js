/**
 * RosterX — Lightweight Installable PWA Service Worker (sw.js)
 * - Network-first (never cached stale) for /api/* and auth endpoints
 * - Stale-while-revalidate for static UI assets (CSS, JS, images)
 * - Safe instant activation on new deployments
 */
const CACHE_NAME = 'rosterx-static-v2.0.0';
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

  // HTML navigations: network-first with cache fallback
  if (req.mode === 'navigate' || url.pathname.endsWith('.html') || url.pathname === '/') {
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

  // Static assets: stale-while-revalidate
  event.respondWith(
    caches.match(req).then((cached) => {
      const networkFetch = fetch(req).then((resp) => {
        if (resp && resp.ok) {
          const copy = resp.clone();
          caches.open(CACHE_NAME).then((c) => c.put(req, copy)).catch(() => {});
        }
        return resp;
      }).catch(() => cached);
      return cached || networkFetch;
    })
  );
});
