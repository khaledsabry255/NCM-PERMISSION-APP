// One worker for both systems. It is registered from the shell at the app root
// and from each system's own page, all with the same scope, so a single
// registration covers the whole app.
//
// Bump CACHE with every upload, otherwise devices keep serving the copy they
// already have and never see the new files — including a raised
// ACCESS_VERSION, which is the only way to lock a device that is already in.
const CACHE = 'ncm-unified-v53';
const SHELL = [
  './',
  './index.html',
  './manifest.json',
  './employees/',
  './employees/index.html',
  './permits/',
  './permits/index.html',
  './multi/',
  './multi/index.html',
  './supabase.min.js',
  './lib/papaparse.min.js',
  './icons/icon-192.png',
  './icons/icon-512.png',
  // the three pieces of the logo the opening animation is built from
  './icons/logo-word.png',
  './icons/logo-mark.png',
  './icons/logo-tag.png'
];

// Storing files ahead of time is a convenience, never a condition. A promise
// handed to waitUntil that rejects fails the whole step — and a failed install
// makes the worker redundant, so the app is left with no worker at all and
// stops working offline entirely, silently. Beside a missing file, the store
// itself can refuse to open: a full phone, a private window, cleared site
// data. So every one of these settles, whatever happened.
self.addEventListener('install', (e) => {
  e.waitUntil(
    caches.open(CACHE)
      .then((c) => Promise.all(SHELL.map((u) => c.add(u).catch(() => {}))))
      .catch(() => {})            // no store: carry on without one
  );
  self.skipWaiting();
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys()
      .then((keys) =>
        Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k).catch(() => {})))
      )
      .catch(() => {})
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
          // Failing to file a copy away must never spoil the answer itself.
          caches.open(CACHE)
            .then((c) => c.put(e.request, copy))
            .catch(() => {});
        }
        return response;
      })
      .catch(() => caches.match(e.request).catch(() => undefined))
  );
});
