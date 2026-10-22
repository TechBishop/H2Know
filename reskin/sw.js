/* Reskin shell only. Scope is the /reskin/ directory that contains this file.
   Cache names are prefixed so this worker never deletes the main app's caches.
   __H2KNOW_BUILD__ is replaced with the deploy SHA. */
const BUILD = "__H2KNOW_BUILD__";
const CACHE = "h2know-reskin-shell-" + BUILD;
const VERSIONED = ["styles.css", "app.js", "data.js", "config.js", "cloud.js", "counties.json"];

function withVersion(path) {
  const name = path.replace(/^\.\//, "");
  if (VERSIONED.indexOf(name) === -1) return path;
  return path + "?v=" + BUILD;
}

const SHELL = [
  "./",
  "./index.html",
  withVersion("./styles.css"),
  withVersion("./app.js"),
  withVersion("./data.js"),
  withVersion("./config.js"),
  withVersion("./cloud.js"),
  "./manifest.json",
  "./icons/icon-192.png",
  "./icons/icon-512.png"
];

function isVersionedPath(pathname) {
  return VERSIONED.some((name) => pathname.endsWith("/" + name));
}

function isStylesheet(request, url) {
  return request.destination === "style" || url.pathname.endsWith("/styles.css");
}

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE)
      .then((cache) => cache.addAll(SHELL))
      .catch(() => undefined)
      .then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(
        keys
          .filter((key) => key.indexOf("h2know-reskin-shell-") === 0 && key !== CACHE)
          .map((key) => caches.delete(key))
      ))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  const url = new URL(event.request.url);
  if (url.origin !== self.location.origin) return;
  const scopePath = new URL("./", self.location.href).pathname;
  if (url.pathname !== scopePath && url.pathname.indexOf(scopePath) !== 0) return;
  if (event.request.method !== "GET") return;

  const versioned = isVersionedPath(url.pathname);
  const style = isStylesheet(event.request, url);

  event.respondWith((async () => {
    try {
      const response = await fetch(event.request, { cache: versioned || style ? "reload" : "no-cache" });
      if (response && response.ok && !styleMismatched(response, event.request, url)) {
        const copy = response.clone();
        const cache = await caches.open(CACHE);
        await cache.put(event.request, copy);
      }
      if (style && styleMismatched(response, event.request, url)) {
        const exact = await caches.match(event.request, { ignoreSearch: false });
        return exact || Response.error();
      }
      return response;
    } catch (_) {
      const exact = await caches.match(event.request, { ignoreSearch: false });
      if (exact) return exact;
      if (style || versioned || event.request.destination === "script") {
        return Response.error();
      }
      return (await caches.match("./index.html", { ignoreSearch: false })) || Response.error();
    }
  })());
});

function styleMismatched(response, request, url) {
  if (!isStylesheet(request, url) || !response) return false;
  const type = (response.headers.get("content-type") || "").toLowerCase();
  if (!type) return false;
  return type.indexOf("text/css") === -1 && type.indexOf("text/plain") === -1;
}
