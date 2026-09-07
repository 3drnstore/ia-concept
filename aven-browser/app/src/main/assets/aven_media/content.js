(() => {
  "use strict";

  const timers = new WeakMap();
  let hoverTimer = null;
  let touchStartX = 0;
  let touchStartY = 0;

  function send(message) {
    try {
      const result = browser.runtime.sendMessage(message);
      if (result && typeof result.catch === "function") result.catch(() => {});
    } catch (_) {}
  }

  function abs(value) {
    if (!value) return "";
    try { return new URL(String(value), location.href).href; } catch (_) { return String(value); }
  }

  function closestOrChild(target, selector) {
    if (!target || target.nodeType !== 1) return null;
    try {
      const closest = target.closest(selector);
      if (closest) return closest;
      return target.querySelector(selector);
    } catch (_) { return null; }
  }

  function mediaSources(media) {
    if (!media) return [];
    const values = [];
    const current = media.currentSrc || media.src || "";
    if (current) values.push(abs(current));
    try {
      media.querySelectorAll("source[src]").forEach(source => {
        const value = abs(source.src || source.getAttribute("src") || "");
        if (value && !values.includes(value)) values.push(value);
      });
    } catch (_) {}
    return values;
  }

  function previewTarget(target) {
    if (!target || target.nodeType !== 1) return;
    try {
      for (const type of ["pointerover", "mouseover", "mouseenter"]) {
        target.dispatchEvent(new MouseEvent(type, { bubbles: true, cancelable: true, view: window }));
      }
    } catch (_) {}

    // Many video sites implement thumbnail previews with a muted <video> inside
    // the hovered card. If one already exists, ask it to play; sites that use
    // their own hover handler continue to work through the events above.
    const video = closestOrChild(target, "video");
    if (video) {
      try {
        video.muted = true;
        video.playsInline = true;
        const result = video.play();
        if (result && typeof result.catch === "function") result.catch(() => {});
      } catch (_) {}
    }
  }

  function contextPayload(target) {
    if (!target || target.nodeType !== 1) return null;
    const link = closestOrChild(target, "a[href]");
    const image = closestOrChild(target, "img");
    const video = closestOrChild(target, "video");
    const audio = closestOrChild(target, "audio");
    const selection = String(window.getSelection ? window.getSelection() : "").trim();

    let mediaType = "";
    let mediaUrl = "";
    let posterUrl = "";
    if (video) {
      mediaType = "video";
      mediaUrl = mediaSources(video)[0] || "";
      posterUrl = abs(video.poster || "");
    } else if (audio) {
      mediaType = "audio";
      mediaUrl = mediaSources(audio)[0] || "";
    } else if (image) {
      mediaType = "image";
      mediaUrl = abs(image.currentSrc || image.src || image.getAttribute("src") || "");
    }

    let text = selection;
    if (!text && !mediaUrl) {
      try { text = String(target.innerText || target.textContent || "").trim().slice(0, 2000); } catch (_) {}
    }

    const linkUrl = link ? abs(link.href || link.getAttribute("href") || "") : "";
    if (!text && !mediaUrl && !linkUrl) return null;

    return {
      type: "contextActionRequest",
      pageUrl: location.href,
      title: document.title || "",
      linkUrl,
      mediaUrl,
      mediaType,
      posterUrl,
      text,
      userAgent: navigator.userAgent || ""
    };
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
      const sources = mediaSources(video);
      send({
        type: "videoPlaying",
        src: sources[0] || "",
        sources,
        pageUrl: location.href,
        title: document.title || "Vídeo",
        duration,
        userAgent: navigator.userAgent || ""
      });
    }, 1600);

    timers.set(video, timer);
  }, true);

  // Native-like long press. The actual menu is rendered by Android so it stays
  // consistent with the rest of Aven instead of relying on each site's menu.
  document.addEventListener("contextmenu", event => {
    const payload = contextPayload(event.target);
    if (!payload) return;
    event.preventDefault();
    event.stopPropagation();
    previewTarget(event.target);
    send(payload);
  }, true);

  document.addEventListener("touchstart", event => {
    if (!event.touches || event.touches.length !== 1) return;
    const touch = event.touches[0];
    touchStartX = touch.clientX;
    touchStartY = touch.clientY;
    if (hoverTimer) clearTimeout(hoverTimer);
    const target = event.target;
    hoverTimer = setTimeout(() => previewTarget(target), 360);
  }, { capture: true, passive: true });

  document.addEventListener("touchmove", event => {
    if (!hoverTimer || !event.touches || event.touches.length !== 1) return;
    const touch = event.touches[0];
    if (Math.abs(touch.clientX - touchStartX) > 14 || Math.abs(touch.clientY - touchStartY) > 14) {
      clearTimeout(hoverTimer);
      hoverTimer = null;
    }
  }, { capture: true, passive: true });

  document.addEventListener("touchend", () => {
    if (hoverTimer) {
      clearTimeout(hoverTimer);
      hoverTimer = null;
    }
  }, { capture: true, passive: true });
})();
