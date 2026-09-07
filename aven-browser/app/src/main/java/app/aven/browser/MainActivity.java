package app.aven.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.StorageController;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int THEME_SYSTEM = 0;
    private static final int THEME_LIGHT = 1;
    private static final int THEME_DARK = 2;

    private final List<Tab> tabs = new ArrayList<>();
    private FrameLayout root;
    private GeckoView geckoView;
    private EditText addressBar;
    private Tab currentTab;
    private SharedPreferences prefs;
    private boolean dark;

    private int bg, surface, surface2, text, muted, accent, outline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("aven", MODE_PRIVATE);
        applyThemePalette();
        configureWindow();
        handleLaunchIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleLaunchIntent(intent);
    }

    private void handleLaunchIntent(Intent intent) {
        Uri data = intent == null ? null : intent.getData();
        if (data != null && ("http".equals(data.getScheme()) || "https".equals(data.getScheme()))) {
            openUrl(data.toString(), false);
        } else {
            showHome();
        }
    }

    private void applyThemePalette() {
        int mode = prefs.getInt("theme", THEME_SYSTEM);
        boolean systemDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        dark = mode == THEME_DARK || (mode == THEME_SYSTEM && systemDark);
        bg = Color.parseColor(dark ? "#101516" : "#F6F5F1");
        surface = Color.parseColor(dark ? "#182022" : "#FFFFFF");
        surface2 = Color.parseColor(dark ? "#20292B" : "#F0F0ED");
        text = Color.parseColor(dark ? "#F5F6F4" : "#151A1B");
        muted = Color.parseColor(dark ? "#AAB3B1" : "#6D7472");
        accent = Color.parseColor("#63A995");
        outline = Color.parseColor(dark ? "#2A3537" : "#E4E4DF");
        AvenApplication app = AvenApplication.get();
        if (app != null) app.setPreferredWebTheme(mode);
    }

    private void configureWindow() {
        Window w = getWindow();
        w.setStatusBarColor(bg);
        w.setNavigationBarColor(bg);
        if (!dark) {
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        } else {
            w.getDecorView().setSystemUiVisibility(0);
        }
    }

    private FrameLayout newRoot() {
        root = new FrameLayout(this);
        root.setBackgroundColor(bg);
        setContentView(root);
        return root;
    }

    private void showHome() {
        if (geckoView != null && geckoView.getSession() != null) geckoView.releaseSession();
        FrameLayout frame = newRoot();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(20), dp(18), dp(20), dp(104));
        scroll.addView(col, new ScrollView.LayoutParams(-1, -2));
        frame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        TextView brand = text("Aven", 34, text, true);
        brand.setGravity(Gravity.CENTER);
        col.addView(brand, lp(-1, dp(50)));
        TextView tag = text("Explore freely.", 16, muted, false);
        tag.setGravity(Gravity.CENTER);
        col.addView(tag, lp(-1, dp(32)));

        MountainView mountains = new MountainView();
        col.addView(mountains, lp(-1, dp(145)));

        addSectionTitle(col, "Favorites", "See all");
        LinearLayout favs = new LinearLayout(this);
        favs.setOrientation(LinearLayout.HORIZONTAL);
        favs.setGravity(Gravity.CENTER);
        addFavorite(favs, "G", "Google", "https://www.google.com");
        addFavorite(favs, "▶", "YouTube", "https://www.youtube.com");
        addFavorite(favs, "r", "Reddit", "https://www.reddit.com");
        addFavorite(favs, "⌘", "GitHub", "https://github.com");
        col.addView(favs, lp(-1, dp(96)));

        addSectionTitle(col, "Recent", "See all");
        if (tabs.isEmpty()) {
            addRecent(col, "A better, more private web", "https://www.aven.app");
            addRecent(col, "Privacy starts here", "about:blank");
        } else {
            for (int i = Math.max(0, tabs.size() - 3); i < tabs.size(); i++) {
                Tab t = tabs.get(i);
                addRecent(col, t.title, t.url);
            }
        }

        addBottomBar(frame, 0);
        addSearchBar(frame);
    }

    private void addSearchBar(FrameLayout frame) {
        addressBar = new EditText(this);
        addressBar.setTextColor(text);
        addressBar.setHintTextColor(muted);
        addressBar.setHint("Search or enter address");
        addressBar.setSingleLine(true);
        addressBar.setTextSize(15);
        addressBar.setPadding(dp(18), 0, dp(18), 0);
        addressBar.setBackground(roundRect(surface, 30, outline, 1));
        addressBar.setImeOptions(EditorInfo.IME_ACTION_GO);
        addressBar.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                openInput(addressBar.getText().toString());
                return true;
            }
            return false;
        });
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(-1, dp(54), Gravity.BOTTOM);
        p.setMargins(dp(18), 0, dp(18), dp(62));
        frame.addView(addressBar, p);
    }

    private void showBrowser(Tab tab) {
        currentTab = tab;
        FrameLayout frame = newRoot();

        geckoView = new GeckoView(this);
        FrameLayout.LayoutParams gv = new FrameLayout.LayoutParams(-1, -1);
        gv.setMargins(0, 0, 0, dp(116));
        frame.addView(geckoView, gv);
        if (geckoView.getSession() != null) geckoView.releaseSession();
        geckoView.setSession(tab.session);
        tab.session.setActive(true);
        tab.session.setFocused(true);

        addressBar = new EditText(this);
        addressBar.setText(tab.url);
        addressBar.setTextColor(text);
        addressBar.setHintTextColor(muted);
        addressBar.setSingleLine(true);
        addressBar.setTextSize(14);
        addressBar.setPadding(dp(16), 0, dp(72), 0);
        addressBar.setBackground(roundRect(surface, 28, outline, 1));
        addressBar.setImeOptions(EditorInfo.IME_ACTION_GO);
        addressBar.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                openInput(addressBar.getText().toString());
                return true;
            }
            return false;
        });
        FrameLayout.LayoutParams ab = new FrameLayout.LayoutParams(-1, dp(52), Gravity.BOTTOM);
        ab.setMargins(dp(16), 0, dp(16), dp(60));
        frame.addView(addressBar, ab);

        TextView shield = text("◈", 22, accent, true);
        shield.setGravity(Gravity.CENTER);
        shield.setOnClickListener(v -> Toast.makeText(this, "Aven Privacy: proteção ativa", Toast.LENGTH_SHORT).show());
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(dp(46), dp(46), Gravity.BOTTOM | Gravity.RIGHT);
        sp.setMargins(0, 0, dp(22), dp(63));
        frame.addView(shield, sp);

        addBottomBar(frame, 4);
    }

    private void openInput(String raw) {
        String input = raw == null ? "" : raw.trim();
        if (input.isEmpty()) return;
        String url;
        if (input.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            url = input;
        } else if (input.contains(".") && !input.contains(" ")) {
            url = "https://" + input;
        } else {
            url = "https://duckduckgo.com/?q=" + Uri.encode(input);
        }
        if (currentTab == null) openUrl(url, false);
        else {
            currentTab.session.loadUri(url);
            currentTab.url = url;
            showBrowser(currentTab);
        }
    }

    private void openUrl(String url, boolean privateMode) {
        GeckoSessionSettings settings = new GeckoSessionSettings.Builder()
                .usePrivateMode(privateMode)
                .useTrackingProtection(true)
                .suspendMediaWhenInactive(true)
                .build();
        GeckoSession session = new GeckoSession(settings);
        Tab tab = new Tab(session, privateMode ? "Private tab" : "New tab", url, privateMode);
        tabs.add(tab);
        attachDelegates(tab);
        session.open(AvenApplication.get().runtime());
        session.loadUri(url);
        showBrowser(tab);
    }

    private void attachDelegates(Tab tab) {
        tab.session.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override public void onPageStart(GeckoSession session, String url) {
                tab.url = url;
                if (session == (currentTab == null ? null : currentTab.session) && addressBar != null) addressBar.setText(url);
            }
            @Override public void onPageStop(GeckoSession session, boolean success) {
                if (session == (currentTab == null ? null : currentTab.session) && addressBar != null) addressBar.setText(tab.url);
            }
        });
        tab.session.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override public void onTitleChange(GeckoSession session, String title) {
                if (title != null && !title.trim().isEmpty()) tab.title = title;
            }
        });
        tab.session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override public void onLocationChange(GeckoSession session, String url, java.util.List<GeckoSession.PermissionDelegate.ContentPermission> perms, Boolean hasUserGesture) {
                if (url != null) tab.url = url;
            }
            @Override public void onCanGoBack(GeckoSession session, boolean canGoBack) { tab.canGoBack = canGoBack; }
            @Override public void onCanGoForward(GeckoSession session, boolean canGoForward) { tab.canGoForward = canGoForward; }
        });
    }

    private void showTabs() {
        if (geckoView != null && geckoView.getSession() != null) geckoView.releaseSession();
        FrameLayout frame = newRoot();
        ScrollView scroll = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(18), dp(18), dp(18), dp(88));
        scroll.addView(col);
        frame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("Tabs", 28, text, true);
        head.addView(title, new LinearLayout.LayoutParams(0, dp(54), 1));
        TextView count = text(tabs.size() + " open", 13, muted, false);
        head.addView(count);
        col.addView(head);

        for (Tab t : new ArrayList<>(tabs)) addTabCard(col, t);

        Button add = button("＋   New tab");
        add.setOnClickListener(v -> showHome());
        col.addView(add, marginLp(-1, dp(52), 0, 12, 0, 0));
        addBottomBar(frame, 4);
    }

    private void addTabCard(LinearLayout col, Tab tab) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(roundRect(surface, 18, outline, 1));
        card.setOnClickListener(v -> showBrowser(tab));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(tab.privateMode ? "◐  " + tab.title : tab.title, 15, text, true);
        title.setSingleLine(true);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(30), 1));
        TextView close = text("×", 26, muted, false);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> {
            tab.session.close();
            tabs.remove(tab);
            if (currentTab == tab) currentTab = null;
            showTabs();
        });
        top.addView(close, lp(dp(40), dp(40)));
        card.addView(top);
        TextView url = text(tab.url, 12, muted, false);
        url.setSingleLine(true);
        card.addView(url, lp(-1, dp(24)));

        View preview = new View(this);
        preview.setBackground(roundRect(surface2, 14, 0, 0));
        card.addView(preview, marginLp(-1, dp(80), 0, 8, 0, 0));
        col.addView(card, marginLp(-1, -2, 0, 0, 0, 12));
    }

    private void showSettings() {
        if (geckoView != null && geckoView.getSession() != null) geckoView.releaseSession();
        FrameLayout frame = newRoot();
        ScrollView scroll = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(18), dp(18), dp(18), dp(48));
        scroll.addView(col);
        frame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 34, text, false);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> {
            if (currentTab == null) showHome(); else showBrowser(currentTab);
        });
        head.addView(back, lp(dp(46), dp(52)));
        head.addView(text("Settings", 23, text, true), lp(-2, dp(52)));
        col.addView(head);

        addHeader(col, "Privacy & Security");
        LinearLayout privacy = card();
        Switch ad = settingSwitch("Ad blocking", "Blocks ads and intrusive content", AvenApplication.get().isAdBlockEnabled());
        ad.setOnCheckedChangeListener((buttonView, checked) -> AvenApplication.get().setAdBlockEnabled(checked));
        privacy.addView(ad);
        privacy.addView(divider());
        privacy.addView(settingRow("DNS over HTTPS", "Cloudflare (1.1.1.1) • Maximum protection"));
        privacy.addView(divider());
        Switch at = settingSwitch("Anti-tracking protection", "Blocks trackers and fingerprinting", AvenApplication.get().isAntiTrackingEnabled());
        at.setOnCheckedChangeListener((buttonView, checked) -> AvenApplication.get().setAntiTrackingEnabled(checked));
        privacy.addView(at);
        privacy.addView(divider());
        TextView clear = settingRow("Clear browsing data", "Cookies, site data and caches");
        clear.setOnClickListener(v -> confirmClearData());
        privacy.addView(clear);
        col.addView(privacy, marginLp(-1, -2, 0, 6, 0, 18));

        addHeader(col, "Appearance");
        LinearLayout appearance = card();
        TextView themeLabel = text("Theme", 15, text, true);
        themeLabel.setPadding(dp(14), dp(8), dp(14), 0);
        appearance.addView(themeLabel, lp(-1, dp(40)));
        LinearLayout themes = new LinearLayout(this);
        themes.setOrientation(LinearLayout.HORIZONTAL);
        themes.setPadding(dp(10), 0, dp(10), dp(12));
        int selected = prefs.getInt("theme", THEME_SYSTEM);
        themes.addView(themeButton("☼\nLight", THEME_LIGHT, selected), new LinearLayout.LayoutParams(0, dp(66), 1));
        themes.addView(themeButton("☾\nDark", THEME_DARK, selected), new LinearLayout.LayoutParams(0, dp(66), 1));
        themes.addView(themeButton("▣\nSystem", THEME_SYSTEM, selected), new LinearLayout.LayoutParams(0, dp(66), 1));
        appearance.addView(themes);
        appearance.addView(divider());
        appearance.addView(settingRow("App icon", "Default"));
        col.addView(appearance, marginLp(-1, -2, 0, 6, 0, 18));

        addHeader(col, "General");
        LinearLayout general = card();
        general.addView(settingRow("Search engine", "DuckDuckGo"));
        general.addView(divider());
        general.addView(settingRow("Privacy signal", "Global Privacy Control enabled"));
        general.addView(divider());
        general.addView(settingRow("HTTPS only", "Enabled"));
        col.addView(general);
    }

    private void confirmClearData() {
        new AlertDialog.Builder(this)
                .setTitle("Clear browsing data?")
                .setMessage("This removes cookies, site data, caches and permissions stored by Aven.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear", (d, w) -> AvenApplication.get().runtime().getStorageController()
                        .clearData(StorageController.ClearFlags.ALL)
                        .accept(v -> Toast.makeText(this, "Browsing data cleared", Toast.LENGTH_SHORT).show(),
                                e -> Toast.makeText(this, "Could not clear all data", Toast.LENGTH_SHORT).show()))
                .show();
    }

    private void addBottomBar(FrameLayout frame, int selected) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setBackgroundColor(bg);
        String[] glyphs = {"⌂", "☆", "◷", "⋮"};
        for (int i = 0; i < glyphs.length; i++) {
            TextView item = text(glyphs[i], i == 3 ? 27 : 25, i == selected ? accent : text, false);
            item.setGravity(Gravity.CENTER);
            final int idx = i;
            item.setOnClickListener(v -> {
                if (idx == 0) showHome();
                else if (idx == 1) toggleBookmark();
                else if (idx == 2) showTabs();
                else showMenu();
            });
            bar.addView(item, new LinearLayout.LayoutParams(0, dp(56), 1));
        }
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(-1, dp(58), Gravity.BOTTOM);
        frame.addView(bar, p);
    }

    private void toggleBookmark() {
        if (currentTab == null || currentTab.url == null) {
            Toast.makeText(this, "Open a page first", Toast.LENGTH_SHORT).show();
            return;
        }
        String key = "bookmark_" + currentTab.url.hashCode();
        boolean value = !prefs.getBoolean(key, false);
        prefs.edit().putBoolean(key, value).apply();
        Toast.makeText(this, value ? "Saved to favorites" : "Removed from favorites", Toast.LENGTH_SHORT).show();
    }

    private void showMenu() {
        String[] options = {"New tab", "New private tab", "Tabs", "Reload", "Settings"};
        new AlertDialog.Builder(this)
                .setItems(options, (d, which) -> {
                    if (which == 0) showHome();
                    else if (which == 1) openUrl("https://duckduckgo.com", true);
                    else if (which == 2) showTabs();
                    else if (which == 3 && currentTab != null) currentTab.session.reload();
                    else if (which == 4) showSettings();
                }).show();
    }

    private void addFavorite(LinearLayout row, String glyph, String label, String url) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        TextView icon = text(glyph, 24, text, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(roundRect(surface, 14, outline, 1));
        item.addView(icon, lp(dp(56), dp(56)));
        TextView name = text(label, 11, text, false);
        name.setGravity(Gravity.CENTER);
        item.addView(name, lp(dp(72), dp(28)));
        item.setOnClickListener(v -> openUrl(url, false));
        row.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
    }

    private void addRecent(LinearLayout col, String title, String url) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(10), dp(14), dp(10));
        card.setBackground(roundRect(surface, 14, outline, 1));
        TextView t = text(title == null ? "Recent page" : title, 14, text, true);
        t.setSingleLine(true);
        card.addView(t);
        TextView u = text(url == null ? "" : url, 11, muted, false);
        u.setSingleLine(true);
        card.addView(u);
        if (url != null && url.startsWith("http")) card.setOnClickListener(v -> openUrl(url, false));
        col.addView(card, marginLp(-1, dp(62), 0, 0, 0, 8));
    }

    private void addSectionTitle(LinearLayout col, String left, String right) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(left, 17, text, true), new LinearLayout.LayoutParams(0, dp(42), 1));
        TextView r = text(right, 12, accent, false);
        row.addView(r);
        col.addView(row);
    }

    private void addHeader(LinearLayout col, String s) {
        TextView h = text(s, 15, text, true);
        col.addView(h, marginLp(-1, dp(40), 0, 8, 0, 4));
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(roundRect(surface, 16, outline, 1));
        return c;
    }

    private TextView settingRow(String title, String subtitle) {
        TextView v = text(title + "\n" + subtitle, 15, text, false);
        v.setLineSpacing(0, 1.25f);
        v.setPadding(dp(16), dp(12), dp(16), dp(12));
        return v;
    }

    private Switch settingSwitch(String title, String subtitle, boolean checked) {
        Switch s = new Switch(this);
        s.setText(title + "\n" + subtitle);
        s.setTextColor(text);
        s.setTextSize(15);
        s.setPadding(dp(16), dp(10), dp(12), dp(10));
        s.setChecked(checked);
        return s;
    }

    private Button themeButton(String label, int mode, int selected) {
        Button b = button(label);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setBackground(roundRect(mode == selected ? surface2 : Color.TRANSPARENT, 14, mode == selected ? accent : Color.TRANSPARENT, mode == selected ? 1 : 0));
        b.setOnClickListener(v -> {
            prefs.edit().putInt("theme", mode).apply();
            recreate();
        });
        return b;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(text);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setBackground(roundRect(surface, 14, outline, 1));
        return b;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(outline);
        v.setLayoutParams(lp(-1, dp(1)));
        return v;
    }

    private TextView text(String value, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setFontFeatureSettings("kern");
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return t;
    }

    private GradientDrawable roundRect(int fill, int radiusDp, int stroke, int strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) g.setStroke(dp(strokeDp), stroke);
        return g;
    }

    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }
    private LinearLayout.LayoutParams marginLp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override
    public void onBackPressed() {
        if (currentTab != null && currentTab.canGoBack) currentTab.session.goBack();
        else if (currentTab != null) showHome();
        else super.onBackPressed();
    }

    private static class Tab {
        final GeckoSession session;
        String title;
        String url;
        final boolean privateMode;
        boolean canGoBack;
        boolean canGoForward;
        Tab(GeckoSession session, String title, String url, boolean privateMode) {
            this.session = session; this.title = title; this.url = url; this.privateMode = privateMode;
        }
    }

    private class MountainView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        MountainView() { super(MainActivity.this); }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            int w = getWidth(), h = getHeight();
            p.setColor(dark ? Color.parseColor("#182022") : Color.parseColor("#DAE3E1"));
            c.drawCircle(w * 0.77f, h * 0.35f, dp(20), p);
            path.reset();
            path.moveTo(0, h * 0.78f);
            path.lineTo(w * 0.18f, h * 0.64f);
            path.lineTo(w * 0.37f, h * 0.30f);
            path.lineTo(w * 0.52f, h * 0.58f);
            path.lineTo(w * 0.66f, h * 0.47f);
            path.lineTo(w, h * 0.82f);
            path.lineTo(w, h);
            path.lineTo(0, h);
            path.close();
            p.setColor(dark ? Color.parseColor("#182123") : Color.parseColor("#C4D0D0"));
            c.drawPath(path, p);
            path.reset();
            path.moveTo(0, h * 0.88f);
            path.lineTo(w * 0.2f, h * 0.73f);
            path.lineTo(w * 0.45f, h * 0.82f);
            path.lineTo(w * 0.66f, h * 0.70f);
            path.lineTo(w, h * 0.90f);
            path.lineTo(w, h);
            path.lineTo(0, h);
            path.close();
            p.setColor(dark ? Color.parseColor("#11191A") : Color.parseColor("#AABABA"));
            c.drawPath(path, p);
        }
    }
}
