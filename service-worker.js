// One worker for both systems. It is registered from the shell at the app root
// and from each system's own page, all with the same scope, so a single
// registration covers the whole app.
//
// Bump CACHE with every upload, otherwise devices keep serving the copy they
// already have and never see the new files — including a raised
// ACCESS_VERSION, which is the only way to lock a device that is already in.
const CACHE = 'ncm-unified-v23';
const SHELL = [
  './',
  './index.html',
  './manifest.json',
  './employees/',
  './employees/index.html',
  './permits/',
  './permits/index.html',
  './supabase.min.js',
  './lib/papaparse.min.js',
  './icons/icon-192.png',
  './icons/icon-512.png'
];

self.addEventListener('install', (e) => {
  // One missing file must not fail the whole install, or the app never caches
  // anything and stops working offline entirely.
  e.waitUntil(
    caches.open(CACHE).then((c) =>
      Promise.all(SHELL.map((u) => c.add(u).catch(() => {})))
    )
  );
  self.skipWaiting();
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)))
    )
  );
  self.clients.claim();
});

// Network-first: always try for the latest version when online, and fall back
// to the cached copy only when there is no connection at all.
//
// GitHub Pages answers every file with `Cache-Control: max-age=600`, and a
// plain fetch() honours that — so for ten minutes after a page is loaded the
// phone serves its own stored copy and never asks the server, no matter how
// many times the app is closed and reopened. An upload appeared to not arrive
// at all. `no-cache` makes the request revalidate: the ETag goes up, and the
// server answers 304 (a few bytes) when nothing changed, or the new file when
// it did. Nothing is refetched needlessly and nothing is ever served stale.
function fromNetwork(req) {
  try {
    return fetch(req, { cache: 'no-cache' });
  } catch (err) {
    return fetch(req);          // an engine that rejects the option still works
  }
}

self.addEventListener('fetch', (e) => {
  if (e.request.method !== 'GET') return;
  const url = new URL(e.request.url);
  if (url.origin !== self.location.origin) return;   // fonts, Supabase, the sheet

  e.respondWith(
    fromNetwork(e.request)
      .then((response) => {
        if (response && response.ok && response.type === 'basic') {
          const copy = response.clone();
          caches.open(CACHE).then((c) => c.put(e.request, copy));
        }
        return response;
      })
      .catch(() => caches.match(e.request))
  );
});
