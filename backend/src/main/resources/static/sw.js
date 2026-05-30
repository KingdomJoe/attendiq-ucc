/**
 * AttendIQ Service Worker
 * 
 * Caching strategy:
 * - Cache-first for static assets (CSS, JS, fonts, icons)
 * - Network-first for HTML pages and API calls
 * - Offline fallback page when both cache and network fail
 */

const CACHE_VERSION = 'attendiq-v1';
const STATIC_CACHE = CACHE_VERSION + '-static';
const DYNAMIC_CACHE = CACHE_VERSION + '-dynamic';

// Static assets to pre-cache on install
const PRECACHE_URLS = [
    '/offline.html',
    '/manifest.webmanifest',
    '/icons/icon-192.png',
    '/icons/icon-512.png',
    '/icons/apple-touch-icon.png'
];

// External CDN resources to cache on first use
const CDN_CACHE_PATTERNS = [
    'cdn.tailwindcss.com',
    'unpkg.com/htmx.org',
    'unpkg.com/html5-qrcode',
    'cdn.jsdelivr.net/npm/@tabler/icons-webfont',
    'openfpcdn.io/fingerprintjs'
];

// ─── Install ────────────────────────────────────────────────────────────────
self.addEventListener('install', (event) => {
    console.log('[SW] Install: pre-caching static assets');
    event.waitUntil(
        caches.open(STATIC_CACHE)
            .then(cache => cache.addAll(PRECACHE_URLS))
            .then(() => self.skipWaiting()) // Activate immediately
    );
});

// ─── Activate ───────────────────────────────────────────────────────────────
self.addEventListener('activate', (event) => {
    console.log('[SW] Activate: cleaning old caches');
    event.waitUntil(
        caches.keys()
            .then(keys => Promise.all(
                keys
                    .filter(key => key !== STATIC_CACHE && key !== DYNAMIC_CACHE)
                    .map(key => {
                        console.log('[SW] Deleting old cache:', key);
                        return caches.delete(key);
                    })
            ))
            .then(() => self.clients.claim()) // Take control of all pages
    );
});

// ─── Fetch ──────────────────────────────────────────────────────────────────
self.addEventListener('fetch', (event) => {
    const { request } = event;
    const url = new URL(request.url);

    // Skip non-GET requests (form submissions, etc.)
    if (request.method !== 'GET') return;

    // Skip WebSocket and browser-extension requests
    if (url.protocol === 'chrome-extension:' || url.protocol === 'ws:' || url.protocol === 'wss:') return;

    // Determine caching strategy based on request type
    if (isCdnResource(url)) {
        // CDN resources: Cache-first (they rarely change)
        event.respondWith(cacheFirst(request, STATIC_CACHE));
    } else if (isStaticAsset(url)) {
        // Local static assets: Cache-first
        event.respondWith(cacheFirst(request, STATIC_CACHE));
    } else if (isApiOrHtmxRequest(request, url)) {
        // API and HTMX requests: Network-only (realtime data is critical)
        // Don't cache these — attendance data must always be fresh
        return;
    } else if (isNavigationRequest(request)) {
        // HTML page navigation: Network-first with offline fallback
        event.respondWith(networkFirstWithFallback(request));
    }
    // All other requests: let the browser handle normally
});

// ─── Caching Strategies ─────────────────────────────────────────────────────

/**
 * Cache-first: try cache, fall back to network and cache the response.
 */
function cacheFirst(request, cacheName) {
    return caches.match(request)
        .then(cachedResponse => {
            if (cachedResponse) {
                return cachedResponse;
            }
            return fetch(request)
                .then(networkResponse => {
                    if (networkResponse && networkResponse.status === 200) {
                        const responseClone = networkResponse.clone();
                        caches.open(cacheName)
                            .then(cache => cache.put(request, responseClone));
                    }
                    return networkResponse;
                });
        });
}

/**
 * Network-first with offline fallback: try network, fall back to cache,
 * then fall back to offline page.
 */
function networkFirstWithFallback(request) {
    return fetch(request)
        .then(networkResponse => {
            // Cache successful page loads for offline use
            if (networkResponse && networkResponse.status === 200) {
                const responseClone = networkResponse.clone();
                caches.open(DYNAMIC_CACHE)
                    .then(cache => cache.put(request, responseClone));
            }
            return networkResponse;
        })
        .catch(() => {
            // Network failed — try cache
            return caches.match(request)
                .then(cachedResponse => {
                    if (cachedResponse) {
                        return cachedResponse;
                    }
                    // Nothing in cache — show offline page
                    return caches.match('/offline.html');
                });
        });
}

// ─── Request Classification Helpers ─────────────────────────────────────────

function isCdnResource(url) {
    return CDN_CACHE_PATTERNS.some(pattern => url.hostname.includes(pattern));
}

function isStaticAsset(url) {
    return url.pathname.startsWith('/icons/') ||
           url.pathname.startsWith('/css/') ||
           url.pathname.startsWith('/js/') ||
           url.pathname.startsWith('/images/') ||
           url.pathname === '/manifest.webmanifest' ||
           url.pathname === '/favicon.ico';
}

function isApiOrHtmxRequest(request, url) {
    // HTMX requests include HX-Request header
    if (request.headers.get('HX-Request')) return true;
    // API endpoints
    if (url.pathname.startsWith('/auth/') ||
        url.pathname.startsWith('/api/') ||
        url.pathname.startsWith('/sessions/') ||
        url.pathname.startsWith('/departments')) return true;
    // POST/PUT/DELETE handled above (method check), but HTMX GET polls too
    return false;
}

function isNavigationRequest(request) {
    return request.mode === 'navigate' ||
           (request.method === 'GET' && request.headers.get('accept')?.includes('text/html'));
}
