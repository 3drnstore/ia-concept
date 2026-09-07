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
import android.graphics.RectF;
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

        bg = Color.parseColor(dark ? "#101516" : "#F7F6F2");
        surface = Color.parseColor(dark ? "#1A2224" : "#FFFFFF");
        surface2 = Color.parseColor(dark ? "#222B2D" : "#F1F1EE");
        text = Color.parseColor(dark ? "#F4F5F3" : "#171B1C");
        muted = Color.parseColor(dark ? "#AAB3B1" : "#69706E");
        accent = Color.parseColor("#63A995");
        outline = Color.parseColor(dark ? "#2B3537" : "#E5E4DF");

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

    private void releaseVisibleSession() {
        try {
            if (geckoView != null && geckoView.getSession() != null) geckoView.releaseSession();
        } catch (Throwable ignored) {}
        geckoView = null;
    }

    private void showHome() {
        releaseVisibleSession();
        FrameLayout frame = newRoot();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(18), dp(8), dp(18), dp(132));
        scroll.addView(col, new ScrollView.LayoutParams(-1, -2));
        frame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        TextView brand = text("Aven", 31, text, true);
        brand.setGravity(Gravity.CENTER);
        col.addView(brand, lp(-1, dp(48)));

        TextView tag = text("Navegue livremente.", 15, muted, false);
        tag.setGravity(Gravity.CENTER);
        col.addView(tag, lp(-1, dp(29)));

        MountainView mountains = new MountainView();
        col.addView(mountains, marginLp(-1, dp(160), 0, 4, 0, 2));

        addSectionTitle(col, "Favoritos", "Ver todos");
        LinearLayout favs = new LinearLayout(this);
        favs.setOrientation(LinearLayout.HORIZONTAL);
        favs.setGravity(Gravity.CENTER);
        addFavorite(favs, "google", "Google", "https://www.google.com");
        addFavorite(favs, "youtube", "YouTube", "https://www.youtube.com");
        addFavorite(favs, "reddit", "Reddit", "https://www.reddit.com");
        addFavorite(favs, "github", "GitHub", "https://github.com");
        col.addView(favs, lp(-1, dp(96)));

        addSectionTitle(col, "Recentes", "Ver todos");
        if (tabs.isEmpty()) {
            addRecent(col, "verge", "The Verge", "https://www.theverge.com");
            addRecent(col, "aven", "Uma web melhor e mais privada", "https://www.aven.app");
            addRecent(col, "unsplash", "Lugares incríveis na Terra", "https://unsplash.com");
        } else {
            for (int i = Math.max(0, tabs.size() - 3); i < tabs.size(); i++) {
                Tab t = tabs.get(i);
                addRecent(col, siteKind(t.url), t.title, t.url);
            }
        }

        addSearchBar(frame);
        addBottomBar(frame, 0);
    }

    private void addSearchBar(FrameLayout frame) {
        FrameLayout shell = new FrameLayout(this);
        shell.setBackground(roundRect(surface, 30, outline, 1));

        SimpleIconView searchIcon = new SimpleIconView("search", muted);
        FrameLayout.LayoutParams sip = new FrameLayout.LayoutParams(dp(44), dp(52), Gravity.LEFT | Gravity.CENTER_VERTICAL);
        shell.addView(searchIcon, sip);

        addressBar = new EditText(this);
        addressBar.setTextColor(text);
        addressBar.setHintTextColor(muted);
        addressBar.setHint("Pesquisar ou digitar endereço");
        addressBar.setSingleLine(true);
        addressBar.setTextSize(14);
        addressBar.setPadding(0, 0, 0, 0);
        addressBar.setBackgroundColor(Color.TRANSPARENT);
        addressBar.setImeOptions(EditorInfo.IME_ACTION_GO);
        addressBar.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                openInput(addressBar.getText().toString());
                return true;
            }
            return false;
        });
        FrameLayout.LayoutParams edit = new FrameLayout.LayoutParams(-1, dp(52));
        edit.setMargins(dp(44), 0, dp(44), 0);
        shell.addView(addressBar, edit);

        SimpleIconView scan = new SimpleIconView("scan", text);
        FrameLayout.LayoutParams scp = new FrameLayout.LayoutParams(dp(44), dp(52), Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        shell.addView(scan, scp);

        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(-1, dp(52), Gravity.BOTTOM);
        p.setMargins(dp(18), 0, dp(18), dp(62));
        frame.addView(shell, p);
    }

    private void showBrowser(Tab tab) {
        currentTab = tab;
        FrameLayout frame = newRoot();

        geckoView = new GeckoView(this);
        FrameLayout.LayoutParams gv = new FrameLayout.LayoutParams(-1, -1);
        gv.setMargins(0, 0, 0, dp(116));
        frame.addView(geckoView, gv);
        try {
            if (geckoView.getSession() != null) geckoView.releaseSession();
            geckoView.setSession(tab.session);
            tab.session.setActive(true);
            tab.session.setFocused(true);
        } catch (Throwable error) {
            Toast.makeText(this, "Não foi possível exibir esta página.", Toast.LENGTH_LONG).show();
            showHome();
            return;
        }

        FrameLayout shell = new FrameLayout(this);
        shell.setBackground(roundRect(surface, 28, outline, 1));

        addressBar = new EditText(this);
        addressBar.setText(tab.url);
        addressBar.setTextColor(text);
        addressBar.setHintTextColor(muted);
        addressBar.setSingleLine(true);
        addressBar.setTextSize(13);
        addressBar.setPadding(dp(14), 0, dp(54), 0);
        addressBar.setBackgroundColor(Color.TRANSPARENT);
        addressBar.setImeOptions(EditorInfo.IME_ACTION_GO);
        addressBar.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                openInput(addressBar.getText().toString());
                return true;
            }
            return false;
        });
        shell.addView(addressBar, new FrameLayout.LayoutParams(-1, dp(50)));

        SimpleIconView shield = new SimpleIconView("shield", accent);
        shield.setOnClickListener(v -> Toast.makeText(this, "Proteção do Aven ativa", Toast.LENGTH_SHORT).show());
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        shell.addView(shield, sp);

        FrameLayout.LayoutParams ab = new FrameLayout.LayoutParams(-1, dp(50), Gravity.BOTTOM);
        ab.setMargins(dp(16), 0, dp(16), dp(60));
        frame.addView(shell, ab);

        addBottomBar(frame, -1);
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
        if (currentTab == null) {
            openUrl(url, false);
        } else {
            currentTab.session.loadUri(url);
            currentTab.url = url;
            showBrowser(currentTab);
        }
    }

    private void openUrl(String url, boolean privateMode) {
        try {
            AvenApplication app = AvenApplication.get();
            if (app == null || app.runtime() == null) {
                Toast.makeText(this, "O mecanismo do navegador ainda não está pronto.", Toast.LENGTH_LONG).show();
                return;
            }

            app.ensureBrowserExtensions();

            GeckoSessionSettings settings = new GeckoSessionSettings.Builder()
                    .usePrivateMode(privateMode)
                    .useTrackingProtection(true)
                    .suspendMediaWhenInactive(true)
                    .build();
            GeckoSession session = new GeckoSession(settings);
            Tab tab = new Tab(session, privateMode ? "Aba privativa" : "Nova aba", url, privateMode);
            tabs.add(tab);
            attachDelegates(tab);
            session.open(app.runtime());
            session.loadUri(url);
            showBrowser(tab);
        } catch (Throwable error) {
            Toast.makeText(this, "Não foi possível abrir a página.", Toast.LENGTH_LONG).show();
        }
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
        releaseVisibleSession();
        FrameLayout frame = newRoot();
        ScrollView scroll = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), dp(10), dp(16), dp(82));
        scroll.addView(col);
        frame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(text("Abas", 27, text, true), lp(-1, dp(36)));
        titles.addView(text(tabs.size() + (tabs.size() == 1 ? " aberta" : " abertas"), 12, muted, false), lp(-1, dp(22)));
        head.addView(titles, new LinearLayout.LayoutParams(0, dp(62), 1));
        head.addView(new SimpleIconView("search", text), lp(dp(44), dp(52)));
        head.addView(new SimpleIconView("menu", text), lp(dp(38), dp(52)));
        col.addView(head);

        if (tabs.isEmpty()) {
            TextView empty = text("Nenhuma aba aberta", 15, muted, false);
            empty.setGravity(Gravity.CENTER);
            col.addView(empty, marginLp(-1, dp(120), 0, 30, 0, 10));
        } else {
            for (Tab t : new ArrayList<>(tabs)) addTabCard(col, t);
        }

        Button add = button("＋   Nova aba");
        add.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        add.setPadding(dp(12), 0, 0, 0);
        add.setOnClickListener(v -> showHome());
        col.addView(add, marginLp(-1, dp(52), 0, 10, 0, 0));
    }

    private void addTabCard(LinearLayout col, Tab tab) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(11), dp(10), dp(11), dp(10));
        card.setBackground(roundRect(surface, 16, outline, 1));
        card.setOnClickListener(v -> showBrowser(tab));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        SiteIconView icon = new SiteIconView(siteKind(tab.url));
        top.addView(icon, lp(dp(34), dp(34)));

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(tab.privateMode ? "◐  " + tab.title : tab.title, 14, text, true);
        title.setSingleLine(true);
        titleCol.addView(title, lp(-1, dp(21)));
        TextView url = text(hostFor(tab.url), 11, muted, false);
        url.setSingleLine(true);
        titleCol.addView(url, lp(-1, dp(18)));
        top.addView(titleCol, new LinearLayout.LayoutParams(0, dp(42), 1));

        TextView close = text("×", 23, muted, false);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> {
            try { tab.session.close(); } catch (Throwable ignored) {}
            tabs.remove(tab);
            if (currentTab == tab) currentTab = null;
            showTabs();
        });
        top.addView(close, lp(dp(38), dp(38)));
        card.addView(top);

        TabPreviewView preview = new TabPreviewView(siteKind(tab.url));
        card.addView(preview, marginLp(-1, dp(78), 0, 7, 0, 0));
        col.addView(card, marginLp(-1, -2, 0, 0, 0, 10));
    }

    private void showSettings() {
        releaseVisibleSession();
        FrameLayout frame = newRoot();
        ScrollView scroll = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(14), dp(8), dp(14), dp(48));
        scroll.addView(col);
        frame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 32, text, false);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> {
            if (currentTab == null) showHome(); else showBrowser(currentTab);
        });
        head.addView(back, lp(dp(44), dp(52)));
        head.addView(text("Configurações", 22, text, true), lp(-2, dp(52)));
        col.addView(head);

        addHeader(col, "Privacidade e segurança");
        LinearLayout privacy = card();
        LinearLayout ad = settingSwitchRow("shield", "Bloqueio de anúncios", "Bloqueia anúncios e conteúdo intrusivo", AvenApplication.get().isAdBlockEnabled());
        ((Switch) ad.getChildAt(2)).setOnCheckedChangeListener((buttonView, checked) -> AvenApplication.get().setAdBlockEnabled(checked));
        privacy.addView(ad);
        privacy.addView(divider());
        privacy.addView(settingRow("lock", "DNS sobre HTTPS", "Cloudflare (1.1.1.1) • Proteção máxima"));
        privacy.addView(divider());
        LinearLayout at = settingSwitchRow("eyeoff", "Proteção antirrastreamento", "Bloqueia rastreadores e fingerprinting", AvenApplication.get().isAntiTrackingEnabled());
        ((Switch) at.getChildAt(2)).setOnCheckedChangeListener((buttonView, checked) -> AvenApplication.get().setAntiTrackingEnabled(checked));
        privacy.addView(at);
        privacy.addView(divider());
        LinearLayout dl = settingSwitchRow("download", "Downloader de vídeos", "Pergunta quando detectar um vídeo compatível", AvenApplication.get().isVideoDownloaderEnabled());
        ((Switch) dl.getChildAt(2)).setOnCheckedChangeListener((buttonView, checked) -> AvenApplication.get().setVideoDownloaderEnabled(checked));
        privacy.addView(dl);
        privacy.addView(divider());
        LinearLayout clear = settingRow("trash", "Limpar dados de navegação", "Cookies, dados de sites e caches");
        clear.setOnClickListener(v -> confirmClearData());
        privacy.addView(clear);
        col.addView(privacy, marginLp(-1, -2, 0, 4, 0, 16));

        addHeader(col, "Aparência");
        LinearLayout appearance = card();
        TextView themeLabel = text("Tema", 14, text, true);
        themeLabel.setPadding(dp(14), dp(8), dp(14), 0);
        appearance.addView(themeLabel, lp(-1, dp(36)));
        LinearLayout themes = new LinearLayout(this);
        themes.setOrientation(LinearLayout.HORIZONTAL);
        themes.setPadding(dp(9), 0, dp(9), dp(11));
        int selected = prefs.getInt("theme", THEME_SYSTEM);
        themes.addView(themeButton("☼\nClaro", THEME_LIGHT, selected), new LinearLayout.LayoutParams(0, dp(66), 1));
        themes.addView(themeButton("☾\nEscuro", THEME_DARK, selected), new LinearLayout.LayoutParams(0, dp(66), 1));
        themes.addView(themeButton("▣\nSistema", THEME_SYSTEM, selected), new LinearLayout.LayoutParams(0, dp(66), 1));
        appearance.addView(themes);
        appearance.addView(divider());
        appearance.addView(settingRow("aven", "Ícone do aplicativo", "Padrão"));
        col.addView(appearance, marginLp(-1, -2, 0, 4, 0, 16));

        addHeader(col, "Geral");
        LinearLayout general = card();
        general.addView(settingRow("search", "Mecanismo de busca", "DuckDuckGo"));
        general.addView(divider());
        general.addView(settingRow("shield", "Sinal de privacidade", "Global Privacy Control ativado"));
        general.addView(divider());
        general.addView(settingRow("lock", "Somente HTTPS", "Ativado"));
        col.addView(general);
    }

    private void confirmClearData() {
        new AlertDialog.Builder(this)
                .setTitle("Limpar dados de navegação?")
                .setMessage("Isso remove cookies, dados de sites, caches e permissões armazenadas pelo Aven.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Limpar", (d, w) -> AvenApplication.get().runtime().getStorageController()
                        .clearData(StorageController.ClearFlags.ALL)
                        .accept(v -> Toast.makeText(this, "Dados de navegação limpos", Toast.LENGTH_SHORT).show(),
                                e -> Toast.makeText(this, "Não foi possível limpar todos os dados", Toast.LENGTH_SHORT).show()))
                .show();
    }

    private void addBottomBar(FrameLayout frame, int selected) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setBackgroundColor(bg);
        String[] types = {"home", "star", "clock", "menu"};
        for (int i = 0; i < types.length; i++) {
            SimpleIconView item = new SimpleIconView(types[i], i == selected ? accent : text);
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
            Toast.makeText(this, "Abra uma página primeiro", Toast.LENGTH_SHORT).show();
            return;
        }
        String key = "bookmark_" + currentTab.url.hashCode();
        boolean value = !prefs.getBoolean(key, false);
        prefs.edit().putBoolean(key, value).apply();
        Toast.makeText(this, value ? "Salvo nos favoritos" : "Removido dos favoritos", Toast.LENGTH_SHORT).show();
    }

    private void showMenu() {
        String[] options = {"Nova aba", "Nova aba privativa", "Abas", "Recarregar", "Configurações"};
        new AlertDialog.Builder(this)
                .setItems(options, (d, which) -> {
                    if (which == 0) showHome();
                    else if (which == 1) openUrl("https://duckduckgo.com", true);
                    else if (which == 2) showTabs();
                    else if (which == 3 && currentTab != null) currentTab.session.reload();
                    else if (which == 4) showSettings();
                }).show();
    }

    private void addFavorite(LinearLayout row, String kind, String label, String url) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);

        FrameLayout iconShell = new FrameLayout(this);
        iconShell.setBackground(roundRect(surface, 13, outline, 1));
        FavoriteIconView icon = new FavoriteIconView(kind);
        iconShell.addView(icon, new FrameLayout.LayoutParams(-1, -1));
        item.addView(iconShell, lp(dp(56), dp(56)));

        TextView name = text(label, 10, text, false);
        name.setGravity(Gravity.CENTER);
        item.addView(name, lp(dp(72), dp(28)));
        item.setOnClickListener(v -> openUrl(url, false));
        row.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
    }

    private void addRecent(LinearLayout col, String kind, String title, String url) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(11), dp(7), dp(9), dp(7));
        card.setBackground(roundRect(surface, 14, outline, 1));

        card.addView(new SiteIconView(kind), lp(dp(36), dp(36)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView t = text(title == null ? "Página recente" : title, 13, text, true);
        t.setSingleLine(true);
        labels.addView(t, lp(-1, dp(22)));
        TextView u = text(url == null ? "" : url, 10, muted, false);
        u.setSingleLine(true);
        labels.addView(u, lp(-1, dp(18)));
        card.addView(labels, new LinearLayout.LayoutParams(0, dp(42), 1));

        TextView arrow = text("›", 24, muted, false);
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow, lp(dp(28), dp(42)));

        if (url != null && url.startsWith("http")) card.setOnClickListener(v -> openUrl(url, false));
        col.addView(card, marginLp(-1, dp(58), 0, 0, 0, 7));
    }

    private void addSectionTitle(LinearLayout col, String left, String right) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(left, 16, text, true), new LinearLayout.LayoutParams(0, dp(40), 1));
        TextView r = text(right, 11, accent, false);
        row.addView(r);
        col.addView(row);
    }

    private void addHeader(LinearLayout col, String s) {
        TextView h = text(s, 14, text, true);
        col.addView(h, marginLp(-1, dp(38), 0, 7, 0, 3));
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(roundRect(surface, 15, outline, 1));
        return c;
    }

    private LinearLayout settingRow(String iconKind, String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.addView(new SimpleIconView(iconKind, text), lp(dp(38), dp(44)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title, 13, text, false), lp(-1, dp(21)));
        labels.addView(text(subtitle, 10, muted, false), lp(-1, dp(18)));
        row.addView(labels, new LinearLayout.LayoutParams(0, dp(44), 1));

        TextView arrow = text("›", 22, muted, false);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, lp(dp(26), dp(44)));
        return row;
    }

    private LinearLayout settingSwitchRow(String iconKind, String title, String subtitle, boolean checked) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(8), dp(8));
        row.addView(new SimpleIconView(iconKind, text), lp(dp(38), dp(44)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title, 13, text, false), lp(-1, dp(21)));
        labels.addView(text(subtitle, 10, muted, false), lp(-1, dp(18)));
        row.addView(labels, new LinearLayout.LayoutParams(0, dp(44), 1));

        Switch toggle = new Switch(this);
        toggle.setChecked(checked);
        row.addView(toggle, lp(dp(52), dp(44)));
        return row;
    }

    private Button themeButton(String label, int mode, int selected) {
        Button b = button(label);
        b.setTextSize(11);
        b.setAllCaps(false);
        b.setBackground(roundRect(mode == selected ? surface2 : Color.TRANSPARENT, 12, mode == selected ? accent : outline, mode == selected ? 1 : 0));
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
        b.setTextSize(13);
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

    private String hostFor(String url) {
        try {
            String host = Uri.parse(url).getHost();
            return host == null ? url : host.replaceFirst("^www\\.", "");
        } catch (Exception ignored) {
            return url == null ? "" : url;
        }
    }

    private String siteKind(String url) {
        String host = hostFor(url).toLowerCase();
        if (host.contains("theverge")) return "verge";
        if (host.contains("aven.app")) return "aven";
        if (host.contains("unsplash")) return "unsplash";
        if (host.contains("google")) return "google";
        if (host.contains("youtube")) return "youtube";
        if (host.contains("reddit")) return "reddit";
        if (host.contains("github")) return "github";
        return "web";
    }

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
            this.session = session;
            this.title = title;
            this.url = url;
            this.privateMode = privateMode;
        }
    }

    private class MountainView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        MountainView() { super(MainActivity.this); }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w = getWidth();
            float h = getHeight();

            p.setStyle(Paint.Style.FILL);
            p.setColor(dark ? Color.parseColor("#89979C") : Color.parseColor("#DDE7E1"));
            c.drawCircle(w * 0.78f, h * 0.33f, dp(19), p);

            drawMountain(c, new float[]{0f,.70f,.10f,.64f,.24f,.38f,.38f,.60f,.51f,.50f,.67f,.66f,.79f,.55f,1f,.73f},
                    dark ? "#172024" : "#D5DEDE");
            drawMountain(c, new float[]{0f,.78f,.13f,.67f,.27f,.49f,.42f,.72f,.55f,.59f,.67f,.76f,.82f,.65f,1f,.80f},
                    dark ? "#131C20" : "#C1CECF");
            drawMountain(c, new float[]{0f,.86f,.17f,.82f,.30f,.73f,.48f,.88f,.62f,.78f,.76f,.87f,1f,.83f},
                    dark ? "#0E1619" : "#A7BABA");

            p.setColor(dark ? Color.parseColor("#0B1113") : Color.parseColor("#819C99"));
            for (int i = 0; i < 18; i++) {
                float x = (i < 9 ? w * (0.01f + i * 0.025f) : w * (0.77f + (i - 9) * 0.025f));
                float base = h * (0.88f + (i % 3) * 0.015f);
                float th = dp(11 + (i % 4) * 3);
                drawPine(c, x, base, th);
            }

            p.setColor(dark ? Color.parseColor("#101516") : Color.parseColor("#F7F6F2"));
            c.drawRect(0, h * .93f, w, h, p);
        }

        private void drawMountain(Canvas c, float[] pts, String color) {
            float w = getWidth(), h = getHeight();
            path.reset();
            path.moveTo(pts[0] * w, pts[1] * h);
            for (int i = 2; i < pts.length; i += 2) path.lineTo(pts[i] * w, pts[i + 1] * h);
            path.lineTo(w, h);
            path.lineTo(0, h);
            path.close();
            p.setColor(Color.parseColor(color));
            c.drawPath(path, p);
        }

        private void drawPine(Canvas c, float x, float base, float height) {
            path.reset();
            path.moveTo(x, base - height);
            path.lineTo(x - height * .28f, base - height * .42f);
            path.lineTo(x - height * .12f, base - height * .42f);
            path.lineTo(x - height * .34f, base);
            path.lineTo(x + height * .34f, base);
            path.lineTo(x + height * .12f, base - height * .42f);
            path.lineTo(x + height * .28f, base - height * .42f);
            path.close();
            c.drawPath(path, p);
        }
    }

    private class FavoriteIconView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final String kind;

        FavoriteIconView(String kind) {
            super(MainActivity.this);
            this.kind = kind;
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            if ("youtube".equals(kind)) {
                p.setColor(Color.parseColor("#FF1F2E"));
                c.drawRoundRect(new RectF(cx-dp(15), cy-dp(10), cx+dp(15), cy+dp(10)), dp(5), dp(5), p);
                p.setColor(Color.WHITE);
                path.reset();
                path.moveTo(cx-dp(4), cy-dp(6)); path.lineTo(cx+dp(7), cy); path.lineTo(cx-dp(4), cy+dp(6)); path.close();
                c.drawPath(path, p);
                return;
            }
            if ("reddit".equals(kind)) {
                p.setColor(Color.parseColor("#FF4500"));
                c.drawCircle(cx, cy, dp(13), p);
                p.setColor(Color.WHITE);
                c.drawCircle(cx-dp(5), cy, dp(2), p); c.drawCircle(cx+dp(5), cy, dp(2), p);
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(1.7f));
                c.drawArc(new RectF(cx-dp(7), cy-dp(2), cx+dp(7), cy+dp(7)), 15, 150, false, p);
                p.setStyle(Paint.Style.FILL);
                return;
            }
            if ("github".equals(kind)) {
                p.setColor(dark ? Color.WHITE : Color.parseColor("#171B1C"));
                c.drawCircle(cx, cy, dp(13), p);
                p.setColor(dark ? Color.parseColor("#1A2224") : Color.WHITE);
                c.drawCircle(cx, cy+dp(1), dp(7), p);
                path.reset(); path.moveTo(cx-dp(7), cy-dp(4)); path.lineTo(cx-dp(5), cy-dp(10)); path.lineTo(cx-dp(1), cy-dp(6)); path.close(); c.drawPath(path,p);
                path.reset(); path.moveTo(cx+dp(7), cy-dp(4)); path.lineTo(cx+dp(5), cy-dp(10)); path.lineTo(cx+dp(1), cy-dp(6)); path.close(); c.drawPath(path,p);
                return;
            }

            // Google G built from four colored arcs.
            float r = dp(12);
            RectF box = new RectF(cx-r, cy-r, cx+r, cy+r);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(4.3f)); p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(Color.parseColor("#4285F4")); c.drawArc(box, -45, 105, false, p);
            p.setColor(Color.parseColor("#34A853")); c.drawArc(box, 45, 70, false, p);
            p.setColor(Color.parseColor("#FBBC05")); c.drawArc(box, 115, 63, false, p);
            p.setColor(Color.parseColor("#EA4335")); c.drawArc(box, 178, 137, false, p);
            p.setColor(Color.parseColor("#4285F4")); c.drawLine(cx+dp(2), cy, cx+dp(12), cy, p);
            p.setStyle(Paint.Style.FILL); p.setStrokeCap(Paint.Cap.BUTT);
        }
    }

    private class SiteIconView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final String kind;

        SiteIconView(String kind) {
            super(MainActivity.this);
            this.kind = kind;
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            float cx = getWidth()/2f, cy = getHeight()/2f;
            if ("aven".equals(kind)) {
                p.setColor(surface2); c.drawRoundRect(new RectF(dp(2),dp(2),getWidth()-dp(2),getHeight()-dp(2)),dp(7),dp(7),p);
                p.setColor(text); path.reset(); path.moveTo(cx-dp(8),cy+dp(8)); path.lineTo(cx-dp(1),cy-dp(10)); path.lineTo(cx+dp(4),cy-dp(7)); path.lineTo(cx-dp(3),cy+dp(9)); path.close(); c.drawPath(path,p);
                p.setColor(accent); c.drawCircle(cx+dp(8),cy+dp(6),dp(4),p);
            } else if ("verge".equals(kind)) {
                p.setColor(Color.parseColor("#6D20FF"));
                path.reset(); path.moveTo(cx-dp(10),cy-dp(8)); path.lineTo(cx+dp(10),cy-dp(8)); path.lineTo(cx+dp(2),cy+dp(10)); path.lineTo(cx-dp(3),cy+dp(10)); path.close(); c.drawPath(path,p);
                p.setColor(Color.WHITE); path.reset(); path.moveTo(cx-dp(4),cy-dp(4)); path.lineTo(cx+dp(4),cy-dp(4)); path.lineTo(cx,cy+dp(5)); path.close(); c.drawPath(path,p);
            } else if ("unsplash".equals(kind)) {
                p.setColor(text); c.drawRect(cx-dp(10),cy+dp(1),cx+dp(10),cy+dp(8),p); c.drawRect(cx-dp(4),cy-dp(8),cx+dp(4),cy+dp(3),p);
            } else if ("youtube".equals(kind) || "google".equals(kind) || "reddit".equals(kind) || "github".equals(kind)) {
                new FavoriteIconPainter(kind).draw(c, cx, cy);
            } else {
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2)); p.setColor(accent); c.drawCircle(cx,cy,dp(9),p); c.drawLine(cx-dp(9),cy,cx+dp(9),cy,p); c.drawArc(new RectF(cx-dp(5),cy-dp(9),cx+dp(5),cy+dp(9)),90,180,false,p); p.setStyle(Paint.Style.FILL);
            }
        }
    }

    private class FavoriteIconPainter {
        private final String kind;
        FavoriteIconPainter(String kind) { this.kind = kind; }
        void draw(Canvas c, float cx, float cy) {
            Paint q = new Paint(Paint.ANTI_ALIAS_FLAG);
            Path z = new Path();
            if ("youtube".equals(kind)) {
                q.setColor(Color.parseColor("#FF1F2E")); c.drawRoundRect(new RectF(cx-dp(11),cy-dp(7),cx+dp(11),cy+dp(7)),dp(4),dp(4),q);
                q.setColor(Color.WHITE); z.moveTo(cx-dp(3),cy-dp(4)); z.lineTo(cx+dp(5),cy); z.lineTo(cx-dp(3),cy+dp(4)); z.close(); c.drawPath(z,q);
            } else if ("reddit".equals(kind)) {
                q.setColor(Color.parseColor("#FF4500")); c.drawCircle(cx,cy,dp(10),q); q.setColor(Color.WHITE); c.drawCircle(cx-dp(4),cy,dp(1.6f),q); c.drawCircle(cx+dp(4),cy,dp(1.6f),q);
            } else if ("github".equals(kind)) {
                q.setColor(text); c.drawCircle(cx,cy,dp(10),q); q.setColor(surface); c.drawCircle(cx,cy+dp(1),dp(5),q);
            } else {
                q.setColor(Color.parseColor("#4285F4")); q.setTextSize(dp(19)); q.setTypeface(Typeface.DEFAULT_BOLD); q.setTextAlign(Paint.Align.CENTER); c.drawText("G",cx,cy+dp(7),q);
            }
        }
    }

    private class TabPreviewView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final String kind;
        TabPreviewView(String kind) { super(MainActivity.this); this.kind = kind; }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w=getWidth(), h=getHeight();
            int fill = dark ? Color.parseColor("#273235") : Color.parseColor("#EEF1F0");
            if ("verge".equals(kind)) fill = Color.parseColor("#30254B");
            if ("aven".equals(kind)) fill = dark ? Color.parseColor("#172022") : Color.parseColor("#D7E2E0");
            if ("unsplash".equals(kind)) fill = dark ? Color.parseColor("#243136") : Color.parseColor("#DDE5E7");
            p.setColor(fill); c.drawRoundRect(new RectF(0,0,w,h),dp(10),dp(10),p);
            p.setColor(dark ? Color.parseColor("#11191B") : Color.parseColor("#A9BCBB"));
            path.reset(); path.moveTo(0,h); path.lineTo(w*.25f,h*.48f); path.lineTo(w*.46f,h*.75f); path.lineTo(w*.67f,h*.36f); path.lineTo(w,h*.72f); path.lineTo(w,h); path.close(); c.drawPath(path,p);
        }
    }

    private class SimpleIconView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final String type;
        private final int color;

        SimpleIconView(String type, int color) {
            super(MainActivity.this);
            this.type = type;
            this.color = color;
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float cx=getWidth()/2f, cy=getHeight()/2f;
            p.setColor(color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(1.8f)); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);

            if ("search".equals(type)) {
                c.drawCircle(cx-dp(2),cy-dp(2),dp(6),p); c.drawLine(cx+dp(3),cy+dp(3),cx+dp(8),cy+dp(8),p);
            } else if ("scan".equals(type)) {
                float d=dp(8), s=dp(4); c.drawLine(cx-d,cy-d,cx-d+s,cy-d,p); c.drawLine(cx-d,cy-d,cx-d,cy-d+s,p); c.drawLine(cx+d,cy-d,cx+d-s,cy-d,p); c.drawLine(cx+d,cy-d,cx+d,cy-d+s,p); c.drawLine(cx-d,cy+d,cx-d+s,cy+d,p); c.drawLine(cx-d,cy+d,cx-d,cy+d-s,p); c.drawLine(cx+d,cy+d,cx+d-s,cy+d,p); c.drawLine(cx+d,cy+d,cx+d,cy+d-s,p);
            } else if ("home".equals(type)) {
                path.reset(); path.moveTo(cx-dp(8),cy); path.lineTo(cx,cy-dp(7)); path.lineTo(cx+dp(8),cy); path.lineTo(cx+dp(8),cy+dp(8)); path.lineTo(cx+dp(2),cy+dp(8)); path.lineTo(cx+dp(2),cy+dp(2)); path.lineTo(cx-dp(2),cy+dp(2)); path.lineTo(cx-dp(2),cy+dp(8)); path.lineTo(cx-dp(8),cy+dp(8)); path.close(); p.setStyle(Paint.Style.FILL); c.drawPath(path,p);
            } else if ("star".equals(type)) {
                path.reset(); for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5; float r=i%2==0?dp(9):dp(4); float x=cx+(float)Math.cos(a)*r, y=cy+(float)Math.sin(a)*r; if(i==0) path.moveTo(x,y); else path.lineTo(x,y);} path.close(); c.drawPath(path,p);
            } else if ("clock".equals(type)) {
                c.drawCircle(cx,cy,dp(8),p); c.drawLine(cx,cy,cx,cy-dp(5),p); c.drawLine(cx,cy,cx+dp(5),cy+dp(1),p);
            } else if ("menu".equals(type)) {
                p.setStyle(Paint.Style.FILL); c.drawCircle(cx,cy-dp(6),dp(1.6f),p); c.drawCircle(cx,cy,dp(1.6f),p); c.drawCircle(cx,cy+dp(6),dp(1.6f),p);
            } else if ("shield".equals(type)) {
                path.reset(); path.moveTo(cx,cy-dp(9)); path.lineTo(cx+dp(7),cy-dp(6)); path.lineTo(cx+dp(6),cy+dp(3)); path.quadTo(cx,cy+dp(10),cx,cy+dp(10)); path.quadTo(cx-dp(6),cy+dp(4),cx-dp(6),cy-dp(6)); path.close(); c.drawPath(path,p);
            } else if ("lock".equals(type)) {
                c.drawRoundRect(new RectF(cx-dp(7),cy-dp(1),cx+dp(7),cy+dp(9)),dp(2),dp(2),p); c.drawArc(new RectF(cx-dp(5),cy-dp(9),cx+dp(5),cy+dp(3)),180,-180,false,p);
            } else if ("eyeoff".equals(type)) {
                path.reset(); path.moveTo(cx-dp(10),cy); path.quadTo(cx,cy-dp(9),cx+dp(10),cy); path.quadTo(cx,cy+dp(9),cx-dp(10),cy); c.drawPath(path,p); c.drawLine(cx-dp(9),cy-dp(9),cx+dp(9),cy+dp(9),p);
            } else if ("trash".equals(type)) {
                c.drawRect(cx-dp(6),cy-dp(4),cx+dp(6),cy+dp(9),p); c.drawLine(cx-dp(8),cy-dp(7),cx+dp(8),cy-dp(7),p); c.drawLine(cx-dp(3),cy-dp(10),cx+dp(3),cy-dp(10),p);
            } else if ("download".equals(type)) {
                c.drawLine(cx,cy-dp(9),cx,cy+dp(4),p); c.drawLine(cx-dp(5),cy, cx,cy+dp(5),p); c.drawLine(cx+dp(5),cy,cx,cy+dp(5),p); c.drawLine(cx-dp(8),cy+dp(9),cx+dp(8),cy+dp(9),p);
            } else if ("aven".equals(type)) {
                p.setStyle(Paint.Style.FILL); path.reset(); path.moveTo(cx-dp(7),cy+dp(8)); path.lineTo(cx-dp(1),cy-dp(9)); path.lineTo(cx+dp(4),cy-dp(6)); path.lineTo(cx-dp(2),cy+dp(9)); path.close(); c.drawPath(path,p); p.setColor(accent); c.drawCircle(cx+dp(8),cy+dp(6),dp(3.5f),p);
            }
            p.setStyle(Paint.Style.FILL); p.setStrokeCap(Paint.Cap.BUTT);
        }
    }
}
