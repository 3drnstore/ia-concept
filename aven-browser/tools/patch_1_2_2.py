from pathlib import Path

path = Path('aven-browser/app/src/main/java/app/aven/browser/MainActivity.java')
s = path.read_text(encoding='utf-8')


def add_import(line):
    global s
    if line not in s:
        anchor = 'import android.app.Activity;\n'
        s = s.replace(anchor, anchor + line + '\n', 1)


def replace_method(marker, replacement):
    global s
    start = s.find(marker)
    if start < 0:
        raise SystemExit(f'method marker not found: {marker}')
    brace = s.find('{', start)
    if brace < 0:
        raise SystemExit(f'opening brace not found: {marker}')
    depth = 0
    end = None
    for i in range(brace, len(s)):
        if s[i] == '{':
            depth += 1
        elif s[i] == '}':
            depth -= 1
            if depth == 0:
                end = i + 1
                break
    if end is None:
        raise SystemExit(f'closing brace not found: {marker}')
    s = s[:start] + replacement.rstrip() + s[end:]


def inject_once(anchor, addition):
    global s
    if addition.strip() in s:
        return
    if anchor not in s:
        raise SystemExit(f'anchor not found: {anchor}')
    s = s.replace(anchor, anchor + addition, 1)


for imp in [
    'import android.app.Dialog;',
    'import android.graphics.drawable.ColorDrawable;',
    'import android.os.Build;',
    'import android.view.WindowInsets;',
    'import android.view.WindowManager;',
    'import org.json.JSONArray;',
    'import org.json.JSONObject;'
]:
    add_import(imp)

# Keep Gecko sessions/tabs alive across Activity recreation in the same process.
s = s.replace('    private final List<Tab> tabs = new ArrayList<>();\n',
              '    private static final List<Tab> tabs = new ArrayList<>();\n', 1)
s = s.replace('    private Tab currentTab;\n', '    private static Tab currentTab;\n', 1)

inject_once(
    '    private static final int THEME_DARK = 2;\n',
    '''\n    private static final int SCREEN_HOME = 0;\n    private static final int SCREEN_BROWSER = 1;\n    private static final int SCREEN_TABS = 2;\n    private static final int SCREEN_HISTORY = 3;\n    private static final int SCREEN_SETTINGS = 4;\n''')

inject_once(
    '    private boolean dark;\n',
    '''    private final List<HistoryEntry> history = new ArrayList<>();\n    private int currentScreen = SCREEN_HOME;\n''')

inject_once('        prefs = getSharedPreferences("aven", MODE_PRIVATE);\n', '        loadHistory();\n')

replace_method(
    '    private void configureWindow() {',
    '''    private void configureWindow() {
        Window w = getWindow();
        w.setStatusBarColor(bg);
        w.setNavigationBarColor(bg);
        w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (!dark) {
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        } else {
            w.getDecorView().setSystemUiVisibility(0);
        }
    }''')

replace_method(
    '    private FrameLayout newRoot() {',
    '''    private FrameLayout newRoot() {
        root = new FrameLayout(this);
        root.setBackgroundColor(bg);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                android.graphics.Insets ime = insets.getInsets(WindowInsets.Type.ime());
                int bottom = Math.max(bars.bottom, ime.bottom);
                v.setPadding(bars.left, bars.top, bars.right, bottom);
            } else {
                v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        setContentView(root);
        root.requestApplyInsets();
        return root;
    }''')

# Track which surface is being shown so Android Back behaves like a browser.
for marker, line in [
    ('    private void showHome() {\n', '        currentScreen = SCREEN_HOME;\n'),
    ('    private void showBrowser(Tab tab) {\n', '        currentScreen = SCREEN_BROWSER;\n'),
    ('    private void showSettings() {\n', '        currentScreen = SCREEN_SETTINGS;\n')
]:
    if marker in s and line not in s[s.find(marker):s.find(marker)+180]:
        s = s.replace(marker, marker + line, 1)

replace_method(
    '    private void attachDelegates(Tab tab) {',
    '''    private void attachDelegates(Tab tab) {
        tab.session.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override public void onPageStart(GeckoSession session, String url) {
                tab.url = url;
                if (session == (currentTab == null ? null : currentTab.session) && addressBar != null) addressBar.setText(url);
            }
            @Override public void onPageStop(GeckoSession session, boolean success) {
                if (session == (currentTab == null ? null : currentTab.session) && addressBar != null) addressBar.setText(tab.url);
                if (success) recordHistory(tab);
            }
        });
        tab.session.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override public void onTitleChange(GeckoSession session, String title) {
                if (title != null && !title.trim().isEmpty()) {
                    tab.title = title;
                    recordHistory(tab);
                }
            }
        });
        tab.session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override public void onLocationChange(GeckoSession session, String url, java.util.List<GeckoSession.PermissionDelegate.ContentPermission> perms, Boolean hasUserGesture) {
                if (url != null) tab.url = url;
            }
            @Override public void onCanGoBack(GeckoSession session, boolean canGoBack) { tab.canGoBack = canGoBack; }
            @Override public void onCanGoForward(GeckoSession session, boolean canGoForward) { tab.canGoForward = canGoForward; }
        });
    }''')

replace_method(
    '    private void showTabs() {',
    '''    private void showTabs() {
        currentScreen = SCREEN_TABS;
        releaseVisibleSession();
        FrameLayout frame = newRoot();
        ScrollView scroll = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), dp(8), dp(16), dp(82));
        scroll.addView(col);
        frame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 34, text, false);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("Voltar");
        back.setOnClickListener(v -> returnFromPanel());
        head.addView(back, lp(dp(44), dp(58)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(text("Abas", 27, text, true), lp(-1, dp(34)));
        titles.addView(text(tabs.size() + (tabs.size() == 1 ? " aberta" : " abertas"), 12, muted, false), lp(-1, dp(20)));
        head.addView(titles, new LinearLayout.LayoutParams(0, dp(58), 1));

        SimpleIconView menu = new SimpleIconView("menu", text);
        menu.setOnClickListener(v -> showMenu());
        head.addView(menu, lp(dp(42), dp(52)));
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
    }''')

replace_method(
    '    private void addBottomBar(FrameLayout frame, int selected) {',
    '''    private void addBottomBar(FrameLayout frame, int selected) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setBackgroundColor(bg);

        SimpleIconView home = new SimpleIconView("home", selected == 0 ? accent : text);
        home.setContentDescription("Início");
        home.setOnClickListener(v -> showHome());
        bar.addView(home, new LinearLayout.LayoutParams(0, dp(56), 1));

        SimpleIconView star = new SimpleIconView("star", text);
        star.setContentDescription("Favorito");
        star.setOnClickListener(v -> toggleBookmark());
        bar.addView(star, new LinearLayout.LayoutParams(0, dp(56), 1));

        SimpleIconView clock = new SimpleIconView("clock", currentScreen == SCREEN_HISTORY ? accent : text);
        clock.setContentDescription("Histórico");
        clock.setOnClickListener(v -> showHistory());
        bar.addView(clock, new LinearLayout.LayoutParams(0, dp(56), 1));

        TabCounterView counter = new TabCounterView();
        counter.setContentDescription("Abas abertas: " + tabs.size());
        counter.setOnClickListener(v -> showTabs());
        bar.addView(counter, new LinearLayout.LayoutParams(0, dp(56), 1));

        SimpleIconView menu = new SimpleIconView("menu", text);
        menu.setContentDescription("Menu");
        menu.setOnClickListener(v -> showMenu());
        bar.addView(menu, new LinearLayout.LayoutParams(0, dp(56), 1));

        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(-1, dp(58), Gravity.BOTTOM);
        frame.addView(bar, p);
    }''')

replace_method(
    '    private void showMenu() {',
    '''    private void showMenu() {
        final Dialog dialog = new Dialog(this);
        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(14), dp(14), dp(14), dp(18));
        sheet.setBackground(roundRect(surface, 22, outline, 1));

        LinearLayout shortcuts = new LinearLayout(this);
        shortcuts.setOrientation(LinearLayout.HORIZONTAL);
        shortcuts.setGravity(Gravity.CENTER);
        shortcuts.addView(menuShortcut("‹", "Voltar", () -> {
            if (currentTab != null && currentTab.canGoBack) currentTab.session.goBack();
        }), new LinearLayout.LayoutParams(0, dp(70), 1));
        shortcuts.addView(menuShortcut("›", "Avançar", () -> {
            if (currentTab != null && currentTab.canGoForward) currentTab.session.goForward();
        }), new LinearLayout.LayoutParams(0, dp(70), 1));
        shortcuts.addView(menuShortcut("↻", "Recarregar", () -> {
            if (currentTab != null) currentTab.session.reload();
        }), new LinearLayout.LayoutParams(0, dp(70), 1));
        shortcuts.addView(menuShortcut("＋", "Nova aba", this::showHome), new LinearLayout.LayoutParams(0, dp(70), 1));
        sheet.addView(shortcuts);
        sheet.addView(divider());

        sheet.addView(menuRow("◐", "Nova aba privativa", () -> openUrl("https://duckduckgo.com", true)));
        sheet.addView(menuRow("▣", "Abas", this::showTabs));
        sheet.addView(menuRow("◷", "Histórico", this::showHistory));
        sheet.addView(menuRow("⚙", "Configurações", this::showSettings));

        dialog.setContentView(sheet);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.width = WindowManager.LayoutParams.MATCH_PARENT;
            attrs.height = WindowManager.LayoutParams.WRAP_CONTENT;
            attrs.gravity = Gravity.BOTTOM;
            attrs.dimAmount = 0.32f;
            window.setAttributes(attrs);
        }
        dialog.setOnShowListener(d -> {
            bindDismissToActions(sheet, dialog);
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setGravity(Gravity.BOTTOM);
        }
    }''')

replace_method(
    '    private Button themeButton(String label, int mode, int selected) {',
    '''    private Button themeButton(String label, int mode, int selected) {
        Button b = button(label);
        b.setTextSize(11);
        b.setAllCaps(false);
        b.setBackground(roundRect(mode == selected ? surface2 : Color.TRANSPARENT, 12, mode == selected ? accent : outline, mode == selected ? 1 : 0));
        b.setOnClickListener(v -> {
            prefs.edit().putInt("theme", mode).apply();
            applyThemePalette();
            configureWindow();
            showSettings();
        });
        return b;
    }''')

# Insert browser-like history and menu helpers before favorites.
helpers = r'''
    private void returnFromPanel() {
        if (currentTab != null) showBrowser(currentTab); else showHome();
    }

    private void loadHistory() {
        history.clear();
        try {
            JSONArray arr = new JSONArray(prefs.getString("history_json", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String url = o.optString("url", "");
                if (!url.startsWith("http://") && !url.startsWith("https://")) continue;
                history.add(new HistoryEntry(o.optString("title", hostFor(url)), url, o.optLong("time", 0L)));
            }
        } catch (Throwable ignored) {}
    }

    private void persistHistory() {
        try {
            JSONArray arr = new JSONArray();
            for (HistoryEntry h : history) {
                JSONObject o = new JSONObject();
                o.put("title", h.title);
                o.put("url", h.url);
                o.put("time", h.time);
                arr.put(o);
            }
            prefs.edit().putString("history_json", arr.toString()).apply();
        } catch (Throwable ignored) {}
    }

    private void recordHistory(Tab tab) {
        if (tab == null || tab.privateMode || tab.url == null) return;
        if (!tab.url.startsWith("http://") && !tab.url.startsWith("https://")) return;
        String titleValue = (tab.title == null || tab.title.trim().isEmpty() || "Nova aba".equals(tab.title)) ? hostFor(tab.url) : tab.title;
        for (int i = history.size() - 1; i >= 0; i--) {
            if (tab.url.equals(history.get(i).url)) history.remove(i);
        }
        history.add(0, new HistoryEntry(titleValue, tab.url, System.currentTimeMillis()));
        while (history.size() > 200) history.remove(history.size() - 1);
        persistHistory();
    }

    private void showHistory() {
        currentScreen = SCREEN_HISTORY;
        releaseVisibleSession();
        FrameLayout frame = newRoot();
        ScrollView scroll = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), dp(8), dp(16), dp(82));
        scroll.addView(col);
        frame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 34, text, false);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> returnFromPanel());
        head.addView(back, lp(dp(44), dp(58)));
        head.addView(text("Histórico", 27, text, true), new LinearLayout.LayoutParams(0, dp(58), 1));
        TextView clear = text("Limpar", 12, accent, true);
        clear.setGravity(Gravity.CENTER);
        clear.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Limpar histórico?")
                .setMessage("Isso remove a lista de páginas visitadas do Aven.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Limpar", (d, w) -> {
                    history.clear();
                    persistHistory();
                    showHistory();
                }).show());
        head.addView(clear, lp(dp(60), dp(52)));
        col.addView(head);

        if (history.isEmpty()) {
            TextView empty = text("Nenhuma página no histórico", 15, muted, false);
            empty.setGravity(Gravity.CENTER);
            col.addView(empty, marginLp(-1, dp(130), 0, 34, 0, 10));
        } else {
            for (HistoryEntry h : new ArrayList<>(history)) {
                LinearLayout row = new LinearLayout(this);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(11), dp(8), dp(10), dp(8));
                row.setBackground(roundRect(surface, 14, outline, 1));
                row.addView(new SiteIconView(siteKind(h.url)), lp(dp(38), dp(38)));
                LinearLayout labels = new LinearLayout(this);
                labels.setOrientation(LinearLayout.VERTICAL);
                TextView titleView = text(h.title, 13, text, true);
                titleView.setSingleLine(true);
                labels.addView(titleView, lp(-1, dp(22)));
                TextView urlView = text(h.url, 10, muted, false);
                urlView.setSingleLine(true);
                labels.addView(urlView, lp(-1, dp(18)));
                row.addView(labels, new LinearLayout.LayoutParams(0, dp(44), 1));
                row.setOnClickListener(v -> openUrl(h.url, false));
                col.addView(row, marginLp(-1, dp(58), 0, 0, 0, 7));
            }
        }
    }

    private LinearLayout menuShortcut(String glyph, String label, Runnable action) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        TextView icon = text(glyph, 25, text, false);
        icon.setGravity(Gravity.CENTER);
        item.addView(icon, lp(-1, dp(38)));
        TextView caption = text(label, 10, muted, false);
        caption.setGravity(Gravity.CENTER);
        item.addView(caption, lp(-1, dp(24)));
        item.setTag(action);
        item.setOnClickListener(v -> action.run());
        return item;
    }

    private LinearLayout menuRow(String glyph, String label, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(4), dp(8), dp(4));
        TextView icon = text(glyph, 20, text, false);
        icon.setGravity(Gravity.CENTER);
        row.addView(icon, lp(dp(42), dp(48)));
        row.addView(text(label, 14, text, false), new LinearLayout.LayoutParams(0, dp(48), 1));
        TextView arrow = text("›", 22, muted, false);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, lp(dp(30), dp(48)));
        row.setTag(action);
        row.setOnClickListener(v -> action.run());
        return row;
    }

    private void bindDismissToActions(View view, Dialog dialog) {
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) bindDismissToActions(group.getChildAt(i), dialog);
        }
        Object tag = view.getTag();
        if (tag instanceof Runnable) {
            Runnable action = (Runnable) tag;
            view.setOnClickListener(v -> {
                dialog.dismiss();
                action.run();
            });
        }
    }

'''
if '    private void showHistory() {' not in s:
    marker = '    private void addFavorite(LinearLayout row, String kind, String label, String url) {'
    if marker not in s:
        raise SystemExit('favorite insertion marker missing')
    s = s.replace(marker, helpers + marker, 1)

replace_method(
    '    public void onBackPressed() {',
    '''    public void onBackPressed() {
        if (currentScreen == SCREEN_TABS || currentScreen == SCREEN_HISTORY || currentScreen == SCREEN_SETTINGS) {
            returnFromPanel();
            return;
        }
        if (currentScreen == SCREEN_BROWSER && currentTab != null && currentTab.canGoBack) {
            currentTab.session.goBack();
        } else if (currentScreen == SCREEN_BROWSER && currentTab != null) {
            showHome();
        } else {
            super.onBackPressed();
        }
    }''')

# Add data model and tab counter before the existing Tab class.
extra_classes = r'''
    private static class HistoryEntry {
        final String title;
        final String url;
        final long time;
        HistoryEntry(String title, String url, long time) {
            this.title = title;
            this.url = url;
            this.time = time;
        }
    }

    private class TabCounterView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        TabCounterView() { super(MainActivity.this); }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(1.8f));
            p.setColor(currentScreen == SCREEN_TABS ? accent : text);
            RectF box = new RectF(cx - dp(9), cy - dp(9), cx + dp(9), cy + dp(9));
            c.drawRoundRect(box, dp(4), dp(4), p);
            p.setStyle(Paint.Style.FILL);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextSize(dp(10));
            String count = tabs.size() > 99 ? "∞" : String.valueOf(tabs.size());
            c.drawText(count, cx, cy + dp(3.5f), p);
        }
    }

'''
if '    private class TabCounterView extends View {' not in s:
    marker = '    private static class Tab {'
    if marker not in s:
        raise SystemExit('Tab class insertion marker missing')
    s = s.replace(marker, extra_classes + marker, 1)

path.write_text(s, encoding='utf-8')
print('Aven 1.2.2 UX patch applied')
