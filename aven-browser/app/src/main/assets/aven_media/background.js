"use strict";

const recentMediaByTab = new Map();
const drmTabs = new Set();
const MEDIA_NATIVE_APP = "aven_media";
const DIRECT_MEDIA_RE = /\.(?:mp4|webm|m4v|mov|ogv|mp3|m4a|aac)(?:[?#]|$)/i;
const HLS_RE = /\.m3u8(?:[?#]|$)/i;

function headerValue(headers, name) {
  if (!Array.isArray(headers)) return "";
  const wanted = name.toLowerCase();
  const found = headers.find(h => h && String(h.name || "").toLowerCase() === wanted);
  return found && found.value ? String(found.value) : "";
}

function classify(url, mime) {
  const cleanMime = String(mime || "").split(";", 1)[0].trim().toLowerCase();
  const value = String(url || "");
  if (cleanMime.includes("mpegurl") || HLS_RE.test(value)) return "hls";
  if (cleanMime.startsWith("video/") || cleanMime.startsWith("audio/") || DIRECT_MEDIA_RE.test(value)) return "direct";
  return "";
}

browser.webRequest.onHeadersReceived.addListener(details => {
  if (typeof details.tabId !== "number" || details.tabId < 0) return;
  const mime = headerValue(details.responseHeaders, "content-type");
  const kind = classify(details.url, mime);
  if (!kind) return;
  recentMediaByTab.set(details.tabId, {
    url: details.url,
    mime,
    kind,
    seenAt: Date.now()
  });
}, { urls: ["<all_urls>"] }, ["responseHeaders"]);

browser.runtime.onMessage.addListener(async (message, sender) => {
  if (!message || typeof message !== "object") return;
  const tabId = sender && sender.tab && typeof sender.tab.id === "number" ? sender.tab.id : -1;

  if (message.type === "pageReady") {
    if (tabId >= 0) {
      drmTabs.delete(tabId);
      recentMediaByTab.delete(tabId);
    }
    return;
  }

  if (message.type === "drmDetected") {
    if (tabId >= 0) drmTabs.add(tabId);
    return;
  }

  if (message.type === "contextActionRequest") {
    try {
      await browser.runtime.sendNativeMessage(MEDIA_NATIVE_APP, {
        type: "contextAction",
        pageUrl: String(message.pageUrl || ""),
        title: String(message.title || ""),
        linkUrl: String(message.linkUrl || ""),
        mediaUrl: String(message.mediaUrl || ""),
        mediaType: String(message.mediaType || ""),
        posterUrl: String(message.posterUrl || ""),
        text: String(message.text || ""),
        userAgent: String(message.userAgent || "")
      });
    } catch (_) {}
    return;
  }

  if (message.type !== "videoPlaying") return;
  if (tabId >= 0 && drmTabs.has(tabId)) return;

  const sources = Array.isArray(message.sources) ? message.sources : [];
  const directSource = [message.src, ...sources]
    .map(v => String(v || ""))
    .find(v => /^https?:\/\//i.test(v) && classify(v, "") === "direct") || "";

  const recent = tabId >= 0 ? recentMediaByTab.get(tabId) : null;
  const candidate = recent && Date.now() - recent.seenAt <= 120000 ? recent : null;

  let url = directSource;
  let mime = "";
  let kind = directSource ? "direct" : "";

  // If the media element exposes a blob: URL, the real MP4/HLS request usually
  // appeared in webRequest shortly before playback. Prefer that network URL.
  if (!url && candidate) {
    url = candidate.url;
    mime = candidate.mime;
    kind = candidate.kind;
  }

  if (!url || !kind) return;

  try {
    await browser.runtime.sendNativeMessage(MEDIA_NATIVE_APP, {
      type: "mediaDetected",
      url,
      mediaKind: kind,
      pageUrl: String(message.pageUrl || ""),
      title: String(message.title || "Vídeo"),
      duration: Number.isFinite(message.duration) ? message.duration : 0,
      mime,
      userAgent: String(message.userAgent || "")
    });
  } catch (_) {}
});
