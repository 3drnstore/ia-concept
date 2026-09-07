(() => {
  "use strict";

  const timers = new WeakMap();

  function send(message) {
    try {
      const result = browser.runtime.sendMessage(message);
      if (result && typeof result.catch === "function") result.catch(() => {});
    } catch (_) {}
  }

  send({ type: "pageReady", pageUrl: location.href });

  document.addEventListener("encrypted", event => {
    if (event.target && event.target.tagName === "VIDEO") {
      send({ type: "drmDetected", pageUrl: location.href });
    }
  }, true);

  document.addEventListener("play", event => {
    const video = event.target;
    if (!video || video.tagName !== "VIDEO") return;

    const oldTimer = timers.get(video);
    if (oldTimer) clearTimeout(oldTimer);

    const timer = setTimeout(() => {
      if (video.paused || video.ended) return;
      const duration = Number.isFinite(video.duration) ? video.duration : 0;
      const src = video.currentSrc || video.src || "";
      send({
        type: "videoPlaying",
        src,
        pageUrl: location.href,
        title: document.title || "Video",
        duration,
        userAgent: navigator.userAgent || ""
      });
    }, 2200);

    timers.set(video, timer);
  }, true);
})();
