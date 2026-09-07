package app.aven.browser;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import org.json.JSONObject;
import org.mozilla.geckoview.ContentBlocking;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.WebExtension;
import org.mozilla.geckoview.WebExtensionController;

import java.util.HashSet;
import java.util.Set;

public class AvenApplication extends Application {
    private static final String TAG = "Aven";
    private static final String MEDIA_EXTENSION_ID = "media-detector@aven.app";
    private static final String UBLOCK_EXTENSION_ID = "uBlock0@raymondhill.net";

    private static AvenApplication instance;

    private final Set<String> promptedMediaUrls = new HashSet<>();
    private GeckoRuntime runtime;
    private ContentBlocking.Settings contentBlocking;
    private WebExtension ublock;
    private WebExtension mediaDetector;
    private boolean extensionsInitializationStarted;

    public static AvenApplication get() { return instance; }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        // Gecko is intentionally lazy: the native Aven home remains usable even
        // if Gecko or an extension fails on a particular device.
    }

    public synchronized GeckoRuntime runtime() {
        ensureRuntime();
        return runtime;
    }

    private synchronized void ensureRuntime() {
        if (runtime != null) return;

        contentBlocking = new ContentBlocking.Settings.Builder()
                .antiTracking(ContentBlocking.AntiTracking.STRICT)
                .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS)
                .cookieBehaviorPrivateMode(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS)
                .cookiePurging(true)
                .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.STRICT)
                .enhancedTrackingProtectionCategory(ContentBlocking.EtpCategory.STRICT)
                .queryParameterStrippingEnabled(true)
                .queryParameterStrippingPrivateBrowsingEnabled(true)
                .build();

        GeckoRuntimeSettings settings = new GeckoRuntimeSettings.Builder()
                .contentBlocking(contentBlocking)
                .globalPrivacyControlEnabled(true)
                .remoteDebuggingEnabled(false)
                .allowInsecureConnections(GeckoRuntimeSettings.HTTPS_ONLY)
                .build();

        settings.setTrustedRecursiveResolverUri("https://cloudflare-dns.com/dns-query");
        settings.setDefaultRecursiveResolverUri("https://cloudflare-dns.com/dns-query");
        settings.setTrustedRecursiveResolverMode(GeckoRuntimeSettings.TRR_MODE_ONLY);
        settings.setDohAutoselectEnabled(false);
        settings.setBaselineFingerprintingProtection(true);
        settings.setFingerprintingProtection(true);
        settings.setFingerprintingProtectionPrivateBrowsing(true);
        settings.setWebContentIsolationStrategy(GeckoRuntimeSettings.STRATEGY_ISOLATE_EVERYTHING);

        if (!isAntiTrackingEnabled()) {
            contentBlocking.setAntiTracking(ContentBlocking.AntiTracking.NONE);
            contentBlocking.setEnhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.NONE);
        }

        runtime = GeckoRuntime.create(this, settings);
        Log.i(TAG, "GeckoRuntime inicializado sob demanda");
    }

    public synchronized void ensureBrowserExtensions() {
        ensureRuntime();
        if (extensionsInitializationStarted || runtime == null) return;
        extensionsInitializationStarted = true;
        installUblockSafely();
        installMediaDetectorSafely();
    }

    private void installUblockSafely() {
        try {
            runtime.getWebExtensionController()
                    .ensureBuiltIn("resource://android/assets/ublock/", UBLOCK_EXTENSION_ID)
                    .accept(extension -> {
                        try {
                            ublock = extension;
                            runtime.getWebExtensionController().setAllowedInPrivateBrowsing(extension, true);
                            applyAdBlockPreference();
                            Log.i(TAG, "uBlock Origin integrado e carregado: " + extension.id);
                        } catch (Throwable error) {
                            Log.e(TAG, "Falha ao finalizar inicialização do uBlock", error);
                        }
                    }, error -> Log.e(TAG, "Falha ao carregar uBlock Origin integrado", error));
        } catch (Throwable error) {
            Log.e(TAG, "Falha síncrona ao iniciar uBlock Origin", error);
        }
    }

    private void installMediaDetectorSafely() {
        try {
            runtime.getWebExtensionController()
                    .ensureBuiltIn("resource://android/assets/aven_media/", MEDIA_EXTENSION_ID)
                    .accept(extension -> {
                        try {
                            mediaDetector = extension;
                            runtime.getWebExtensionController().setAllowedInPrivateBrowsing(extension, true);
                            extension.setMessageDelegate(new WebExtension.MessageDelegate() {
                                @Override
                                public GeckoResult<Object> onMessage(String nativeApp, Object message, WebExtension.MessageSender sender) {
                                    try {
                                        if (!(message instanceof JSONObject)) return null;
                                        JSONObject data = (JSONObject) message;
                                        String type = data.optString("type", "");

                                        if ("contextAction".equals(type)) {
                                            showContextActions(data);
                                            return null;
                                        }

                                        if (!"mediaDetected".equals(type)) return null;
                                        String url = data.optString("url", "");
                                        if (!isHttp(url) || !isVideoDownloaderEnabled()) return null;

                                        synchronized (promptedMediaUrls) {
                                            if (!promptedMediaUrls.add(url)) return null;
                                        }

                                        Intent prompt = new Intent(AvenApplication.this, MediaPromptActivity.class);
                                        prompt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        prompt.putExtra(MediaPromptActivity.EXTRA_URL, url);
                                        prompt.putExtra(MediaPromptActivity.EXTRA_PAGE_URL, data.optString("pageUrl", ""));
                                        prompt.putExtra(MediaPromptActivity.EXTRA_TITLE, data.optString("title", "Vídeo"));
                                        prompt.putExtra(MediaPromptActivity.EXTRA_MIME, data.optString("mime", ""));
                                        prompt.putExtra(MediaPromptActivity.EXTRA_COOKIES, data.optString("cookies", ""));
                                        prompt.putExtra(MediaPromptActivity.EXTRA_USER_AGENT, data.optString("userAgent", ""));
                                        prompt.putExtra(MediaPromptActivity.EXTRA_DURATION, data.optDouble("duration", 0d));
                                        prompt.putExtra(MediaPromptActivity.EXTRA_MEDIA_KIND, data.optString("mediaKind", "direct"));
                                        startActivity(prompt);
                                    } catch (Throwable error) {
                                        Log.e(TAG, "Erro ao processar mensagem de mídia", error);
                                    }
                                    return null;
                                }
                            }, "aven_media");
                            Log.i(TAG, "Detector de mídia Aven carregado: " + extension.id);
                        } catch (Throwable error) {
                            Log.e(TAG, "Falha ao finalizar inicialização do detector de mídia", error);
                        }
                    }, error -> Log.e(TAG, "Falha ao carregar detector de mídia Aven", error));
        } catch (Throwable error) {
            Log.e(TAG, "Falha síncrona ao iniciar detector de mídia", error);
        }
    }

    private void showContextActions(JSONObject data) {
        Intent intent = new Intent(this, ContextActionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ContextActionActivity.EXTRA_PAGE_URL, data.optString("pageUrl", ""));
        intent.putExtra(ContextActionActivity.EXTRA_TITLE, data.optString("title", ""));
        intent.putExtra(ContextActionActivity.EXTRA_LINK_URL, data.optString("linkUrl", ""));
        intent.putExtra(ContextActionActivity.EXTRA_MEDIA_URL, data.optString("mediaUrl", ""));
        intent.putExtra(ContextActionActivity.EXTRA_MEDIA_TYPE, data.optString("mediaType", ""));
        intent.putExtra(ContextActionActivity.EXTRA_POSTER_URL, data.optString("posterUrl", ""));
        intent.putExtra(ContextActionActivity.EXTRA_TEXT, data.optString("text", ""));
        intent.putExtra(ContextActionActivity.EXTRA_USER_AGENT, data.optString("userAgent", ""));
        startActivity(intent);
    }

    private boolean isHttp(String value) {
        return value != null && (value.startsWith("https://") || value.startsWith("http://"));
    }

    public void setAdBlockEnabled(boolean enabled) {
        getSharedPreferences("aven", MODE_PRIVATE).edit().putBoolean("adblock", enabled).apply();
        applyAdBlockPreference();
    }

    public boolean isAdBlockEnabled() {
        return getSharedPreferences("aven", MODE_PRIVATE).getBoolean("adblock", true);
    }

    private void applyAdBlockPreference() {
        if (ublock == null || runtime == null) return;
        try {
            WebExtensionController controller = runtime.getWebExtensionController();
            if (isAdBlockEnabled()) {
                controller.enable(ublock, WebExtensionController.EnableSource.USER);
            } else {
                controller.disable(ublock, WebExtensionController.EnableSource.USER);
            }
        } catch (Throwable error) {
            Log.e(TAG, "Falha ao aplicar preferência de bloqueio de anúncios", error);
        }
    }

    public void setAntiTrackingEnabled(boolean enabled) {
        getSharedPreferences("aven", MODE_PRIVATE).edit().putBoolean("anti_tracking", enabled).apply();
        if (contentBlocking != null) {
            contentBlocking.setAntiTracking(enabled ? ContentBlocking.AntiTracking.STRICT : ContentBlocking.AntiTracking.NONE);
            contentBlocking.setEnhancedTrackingProtectionLevel(enabled ? ContentBlocking.EtpLevel.STRICT : ContentBlocking.EtpLevel.NONE);
        }
    }

    public boolean isAntiTrackingEnabled() {
        return getSharedPreferences("aven", MODE_PRIVATE).getBoolean("anti_tracking", true);
    }

    public void setVideoDownloaderEnabled(boolean enabled) {
        getSharedPreferences("aven", MODE_PRIVATE).edit().putBoolean("video_downloader", enabled).apply();
    }

    public boolean isVideoDownloaderEnabled() {
        return getSharedPreferences("aven", MODE_PRIVATE).getBoolean("video_downloader", true);
    }

    public void setPreferredWebTheme(int mode) {
        if (runtime == null) return;
        int scheme = GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM;
        if (mode == 1) scheme = GeckoRuntimeSettings.COLOR_SCHEME_LIGHT;
        if (mode == 2) scheme = GeckoRuntimeSettings.COLOR_SCHEME_DARK;
        runtime.getSettings().setPreferredColorScheme(scheme);
    }
}
