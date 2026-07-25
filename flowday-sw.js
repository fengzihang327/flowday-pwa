const cacheName = 'flowday-v7';
const appFiles = [
  './',
  './index.html',
  './time-planner.html',
  './widget.html',
  './widget.webmanifest',
  './flowday.webmanifest',
  './flowday-icon.svg'
];

// ── Notification timers storage (IndexedDB) ──────────
let db = null;
function openDB() {
  return new Promise((resolve, reject) => {
    if (db) return resolve(db);
    const req = indexedDB.open('flowday-sw', 1);
    req.onupgradeneeded = e => {
      const d = e.target.result;
      if (!d.objectStoreNames.contains('notifications')) {
        d.createObjectStore('notifications', { keyPath: 'id' });
      }
    };
    req.onsuccess = e => { db = e.target.result; resolve(db); };
    req.onerror = e => reject(e.target.error);
  });
}

async function storeNotification(id, data) {
  const d = await openDB();
  const tx = d.transaction('notifications', 'readwrite');
  tx.objectStore('notifications').put({ id, ...data, stored: Date.now() });
  return tx.complete;
}

async function getNotifications() {
  const d = await openDB();
  return new Promise((resolve) => {
    const tx = d.transaction('notifications', 'readonly');
    const req = tx.objectStore('notifications').getAll();
    req.onsuccess = () => resolve(req.result || []);
  });
}

async function clearNotifications() {
  const d = await openDB();
  const tx = d.transaction('notifications', 'readwrite');
  tx.objectStore('notifications').clear();
  return tx.complete;
}

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
    const { title, options, id, delay } = data;

    // Store in IndexedDB for persistence
    if (id) {
      storeNotification(id, { title, options, delay, time: Date.now() });
    }

    // Show immediately if delay is 0 or not set
    if (!delay || delay <= 0) {
      self.registration.showNotification(title, {
        ...options,
        vibrate: options.vibrate || [300, 100, 300, 100, 300],
        silent: false,
        tag: options.tag || 'flowday',
        renotify: true,
        badge: 'flowday-icon.svg',
        icon: 'flowday-icon.svg'
      });
    } else {
      // Schedule with SW setTimeout (more reliable than page setTimeout)
      setTimeout(() => {
        self.registration.showNotification(title, {
          ...options,
          vibrate: options.vibrate || [300, 100, 300, 100, 300],
          silent: false,
          tag: options.tag || 'flowday',
          renotify: true,
          badge: 'flowday-icon.svg',
          icon: 'flowday-icon.svg'
        });
      }, delay);
    }
  }

  if (data.type === 'clearNotifications') {
    // Clear active notifications
    self.registration.getNotifications().then(notifications => {
      notifications.forEach(n => {
        if (n.tag && n.tag.startsWith('flowday-task')) n.close();
      });
    });
    // Clear stored schedule
    clearNotifications();
  }
});

// ── Notification click ───────────────────────────────
self.addEventListener('notificationclick', event => {
  event.notification.close();

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clientList => {
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

// ── Periodic background sync ─────────────────────────
self.addEventListener('periodicsync', event => {
  if (event.tag === 'flowday-check') {
    event.waitUntil(checkPending());
  }
});

async function checkPending() {
  const notifications = await getNotifications();
  const now = Date.now();

  for (const n of notifications) {
    if (n.delay && n.time && (n.time + n.delay) <= now) {
      // This notification should have fired — fire it now
      self.registration.showNotification(n.title, {
        ...n.options,
        vibrate: [300, 100, 300, 100, 300],
        silent: false,
        renotify: true
      });
      // Clean up
      const d = await openDB();
      d.transaction('notifications', 'readwrite').objectStore('notifications').delete(n.id);
    }
  }
}
