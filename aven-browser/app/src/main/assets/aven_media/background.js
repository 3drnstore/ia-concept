"use strict";

const recentVideoByTab = new Map();
const drmTabs = new Set();
const MEDIA_NATIVE_APP = "aven_media";
const MEDIA_EXT_RE = /\.(?:mp4|webm|m4v|mov|ogv)(?:[?#]|$)/i;

function headerValue(headers, name) {
  if (!Array.isArray(headers)) return "";
  const wanted = name.toLowerCase();
  const found = headers.find(h => h && String(h.name || "").toLowerCase() === wanted);
  return found && found.value ? String(found.value) : "";
}

function looksLikeDirectVideo(url, mime) {
  const cleanMime = String(mime || "").split(";", 1)[0].trim().toLowerCase();
  return cleanMime.startsWith("video/") || MEDIA_EXT_RE.test(String(url || ""));
}

browser.webRequest.onHeadersReceived.addListener(details => {
  if (typeof details.tabId !== "number" || details.tabId < 0) return;
  const mime = headerValue(details.responseHeaders, "content-type");
  if (!looksLikeDirectVideo(details.url, mime)) return;
  recentVideoByTab.set(details.tabId, { url: details.url, mime, seenAt: Date.now() });
}, { urls: ["<all_urls>"] }, ["responseHeaders"]);

browser.runtime.onMessage.addListener(async (message, sender) => {
  if (!message || typeof message !== "object") return;
  const tabId = sender && sender.tab && typeof sender.tab.id === "number" ? sender.tab.id : -1;
  if (message.type === "pageReady") {
    if (tabId >= 0) { drmTabs.delete(tabId); recentVideoByTab.delete(tabId); }
    return;
  }
  if (message.type === "drmDetected") {
    if (tabId >= 0) drmTabs.add(tabId);
    return;
  }
  if (message.type !== "videoPlaying") return;
  if (tabId >= 0 && drmTabs.has(tabId)) return;

  const direct = /^https?:\/\//i.test(String(message.src || "")) ? String(message.src) : "";
  const recent = tabId >= 0 ? recentVideoByTab.get(tabId) : null;
  const candidate = recent && Date.now() - recent.seenAt <= 120000 ? recent : null;
  const url = direct || (candidate ? candidate.url : "");
  const mime = candidate ? candidate.mime : "";
  if (!url || !looksLikeDirectVideo(url, mime)) return;

  try {
    await browser.runtime.sendNativeMessage(MEDIA_NATIVE_APP, {
      type: "mediaDetected",
      url,
      pageUrl: String(message.pageUrl || ""),
      title: String(message.title || "Video"),
      duration: Number.isFinite(message.duration) ? message.duration : 0,
      mime,
      userAgent: String(message.userAgent || "")
    });
  } catch (_) {}
});
