const cacheName = 'flowday-v5';
const appFiles = [
  './',
  './index.html',
  './time-planner.html',
  './widget.html',
  './widget.webmanifest',
  './flowday.webmanifest',
  './flowday-icon.svg'
];

// ── Install & Activate ──────────────────────────────
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(cacheName).then(cache => cache.addAll(appFiles))
  );
  self.skipWaiting();
});

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(key => key !== cacheName).map(key => caches.delete(key)))
    )
  );
  self.clients.claim();
});

// ── Fetch (cache-first) ──────────────────────────────
self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET') return;
  event.respondWith(
    caches.match(event.request).then(response => response || fetch(event.request))
  );
});

// ── Message handler for notifications ────────────────
self.addEventListener('message', event => {
  if (event.data && event.data.type === 'showNotification') {
    const { title, options } = event.data;
    self.registration.showNotification(title, options);
  }
});

// ── Notification click ───────────────────────────────
self.addEventListener('notificationclick', event => {
  event.notification.close();
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clientList => {
      // Focus existing window or open new one
      for (const client of clientList) {
        if (client.url.includes('time-planner.html') && 'focus' in client) {
          return client.focus();
        }
      }
      if (clients.openWindow) {
        return clients.openWindow('./time-planner.html');
      }
    })
  );
});

// ── Periodic background sync (if supported) ──────────
self.addEventListener('periodicsync', event => {
  if (event.tag === 'flowday-check') {
    event.waitUntil(checkAndNotify());
  }
});

async function checkAndNotify() {
  // This runs in the background - can't access localStorage directly,
  // but can use IndexedDB or Cache storage to check pending tasks
  // For now, this is a placeholder that could be extended
  console.log('[Flowday SW] Periodic sync fired');
}
