package app.aven.browser;

import android.app.Application;
import android.util.Log;

import org.mozilla.geckoview.ContentBlocking;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.WebExtension;
import org.mozilla.geckoview.WebExtensionController;

public class AvenApplication extends Application {
    private static final String TAG = "Aven";
    private static AvenApplication instance;
    private GeckoRuntime runtime;
    private ContentBlocking.Settings contentBlocking;
    private WebExtension ublock;

    public static AvenApplication get() { return instance; }
    public GeckoRuntime runtime() { return runtime; }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

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
        installUblock();
    }

    private void installUblock() {
        runtime.getWebExtensionController()
                .installBuiltIn("resource://android/assets/ublock/")
                .accept(extension -> {
                    ublock = extension;
                    runtime.getWebExtensionController().setAllowedInPrivateBrowsing(extension, true);
                    applyAdBlockPreference();
                    Log.i(TAG, "uBlock Origin integrado e carregado: " + extension.id);
                }, error -> Log.e(TAG, "Falha ao carregar uBlock Origin integrado", error));
    }

    public void setAdBlockEnabled(boolean enabled) {
        getSharedPreferences("aven", MODE_PRIVATE).edit().putBoolean("adblock", enabled).apply();
        applyAdBlockPreference();
    }

    public boolean isAdBlockEnabled() {
        return getSharedPreferences("aven", MODE_PRIVATE).getBoolean("adblock", true);
    }

    private void applyAdBlockPreference() {
        if (ublock == null) return;
        WebExtensionController controller = runtime.getWebExtensionController();
        if (isAdBlockEnabled()) {
            controller.enable(ublock, WebExtensionController.EnableSource.USER);
        } else {
            controller.disable(ublock, WebExtensionController.EnableSource.USER);
        }
    }

    public void setAntiTrackingEnabled(boolean enabled) {
        getSharedPreferences("aven", MODE_PRIVATE).edit().putBoolean("anti_tracking", enabled).apply();
        contentBlocking.setAntiTracking(enabled ? ContentBlocking.AntiTracking.STRICT : ContentBlocking.AntiTracking.NONE);
        contentBlocking.setEnhancedTrackingProtectionLevel(enabled ? ContentBlocking.EtpLevel.STRICT : ContentBlocking.EtpLevel.NONE);
    }

    public boolean isAntiTrackingEnabled() {
        return getSharedPreferences("aven", MODE_PRIVATE).getBoolean("anti_tracking", true);
    }

    public void setPreferredWebTheme(int mode) {
        int scheme = GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM;
        if (mode == 1) scheme = GeckoRuntimeSettings.COLOR_SCHEME_LIGHT;
        if (mode == 2) scheme = GeckoRuntimeSettings.COLOR_SCHEME_DARK;
        runtime.getSettings().setPreferredColorScheme(scheme);
    }
}
