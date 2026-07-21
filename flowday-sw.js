const cacheName = 'flowday-v6';
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

// ── Message handler ──────────────────────────────────
self.addEventListener('message', event => {
  const data = event.data;
  if (!data) return;

  if (data.type === 'showNotification') {
    // Show notification with sound/vibration from SW context (more reliable)
    const { title, options } = data;
    self.registration.showNotification(title, options);
  }

  if (data.type === 'clearNotifications') {
    // Clear all pending flowday notifications
    self.registration.getNotifications().then(notifications => {
      notifications.forEach(n => {
        if (n.tag && n.tag.startsWith('flowday-task')) n.close();
      });
    });
  }
});

// ── Notification click ───────────────────────────────
self.addEventListener('notificationclick', event => {
  event.notification.close();

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clientList => {
      // Try to focus an existing window
      for (const client of clientList) {
        if (client.url.includes('time-planner.html') && 'focus' in client) {
          return client.focus();
        }
      }
      // Open new window
      if (clients.openWindow) {
        return clients.openWindow('./time-planner.html');
      }
    })
  );
});

// ── Periodic sync ────────────────────────────────────
self.addEventListener('periodicsync', event => {
  if (event.tag === 'flowday-check') {
    event.waitUntil(checkAndNotify());
  }
});

async function checkAndNotify() {
  // Background check - SW can't read localStorage directly
  // In the future, could use IndexedDB to store notification times
  console.log('[Flowday SW] Periodic sync fired');
}
