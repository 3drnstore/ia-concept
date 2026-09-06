#!/usr/bin/env python3
from pathlib import Path
import re
import shutil
import sys

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path("android").resolve()
APP = ROOT / "OsmAnd"


def read(p: Path) -> str:
    return p.read_text(encoding="utf-8")


def write(p: Path, s: str) -> None:
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(s, encoding="utf-8")


def bump_version() -> None:
    p = APP / "build.gradle"
    s = read(p)
    s = re.sub(r'versionCode\s+5399', 'versionCode 5406', s, count=1)
    s = re.sub(r'versionName\s+"0\.4\.2"', 'versionName "0.4.6"', s, count=1)
    write(p, s)


def build_distinct_icon(size: int) -> Image.Image:
    scale = size / 1024.0
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    shadow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    m = int(70 * scale)
    r = int(225 * scale)
    sd.rounded_rectangle((m, m + int(28 * scale), size - m, size - m + int(28 * scale)), radius=r, fill=(0, 33, 50, 150))
    shadow = shadow.filter(ImageFilter.GaussianBlur(max(1, int(34 * scale))))
    img.alpha_composite(shadow)

    mask = Image.new("L", (size, size), 0)
    md = ImageDraw.Draw(mask)
    md.rounded_rectangle((m, m, size - m, size - m), radius=r, fill=255)

    tile = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    grad = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    gd = ImageDraw.Draw(grad)
    top = (89, 234, 244, 255)
    bottom = (5, 74, 120, 255)
    for y in range(size):
        t = y / max(1, size - 1)
        c = tuple(int(top[i] * (1 - t) + bottom[i] * t) for i in range(4))
        gd.line((0, y, size, y), fill=c)
    grad.putalpha(mask)
    tile.alpha_composite(grad)
    td = ImageDraw.Draw(tile)
    inset = int(92 * scale)
    td.rounded_rectangle((inset, inset, size - inset, size - inset), radius=int(205 * scale), outline=(220, 255, 255, 195), width=max(1, int(11 * scale)))

    # A geometric N, intentionally unlike OsmAnd's navigation arrow.
    w = int(82 * scale)
    x1, x2 = int(310 * scale), int(714 * scale)
    y1, y2 = int(285 * scale), int(748 * scale)
    td.rounded_rectangle((x1, y1, x1 + w, y2), radius=int(26 * scale), fill=(246, 255, 255, 255))
    td.rounded_rectangle((x2 - w, y1, x2, y2), radius=int(26 * scale), fill=(246, 255, 255, 255))
    poly = [
        (x1 + int(50 * scale), y1),
        (x1 + int(138 * scale), y1),
        (x2 - int(50 * scale), y2),
        (x2 - int(138 * scale), y2),
    ]
    td.polygon(poly, fill=(246, 255, 255, 255))
    td.ellipse((int(690 * scale), int(220 * scale), int(790 * scale), int(320 * scale)), fill=(133, 255, 180, 255))

    gloss = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    gg = ImageDraw.Draw(gloss)
    gg.ellipse((int(135 * scale), int(86 * scale), int(890 * scale), int(520 * scale)), fill=(255, 255, 255, 72))
    gloss.putalpha(Image.composite(gloss.getchannel("A"), Image.new("L", (size, size), 0), mask))
    tile.alpha_composite(gloss)
    img.alpha_composite(tile)
    return img


def replace_launcher_pngs() -> None:
    base = build_distinct_icon(1024)
    densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for density, px in densities.items():
        d = APP / f"res/mipmap-{density}"
        out = d / "nuwe_launcher.png"
        base.resize((px, px), Image.Resampling.LANCZOS).save(out)
        # Preserve upstream resource names but make every PNG fallback Nuwe.
        for name in ("icon.png", "icon_free.png", "icon_nightly.png"):
            dst = d / name
            if dst.exists():
                shutil.copyfile(out, dst)

    manifest = APP / "AndroidManifest.xml"
    s = read(manifest)
    s = re.sub(r'android:icon="[^"]+"', 'android:icon="@mipmap/nuwe_launcher"', s, count=1)
    if 'android:roundIcon=' in s:
        s = re.sub(r'android:roundIcon="[^"]+"', 'android:roundIcon="@mipmap/nuwe_launcher"', s, count=1)
    else:
        s = s.replace('android:icon="@mipmap/nuwe_launcher"', 'android:icon="@mipmap/nuwe_launcher" android:roundIcon="@mipmap/nuwe_launcher"', 1)
    write(manifest, s)


def patch_existing_icons() -> None:
    # Reuse existing Nuwe resource names so no new drawable IDs are introduced.
    paths = {
        "nuwe_ic_search": "M9,4a5,5 0,1 0,0 10a5,5 0,0 0,0 -10M13,13l7,7",
        "nuwe_ic_layers": "M12,3L4,7.5L12,12l8,-4.5zM4,12l8,4.5l8,-4.5M4,16l8,4.5l8,-4.5",
        "nuwe_ic_settings": "M12,8a4,4 0,1 0,0 8a4,4 0,0 0,0 -8M12,2v3M12,19v3M2,12h3M19,12h3M4.9,4.9l2.1,2.1M17,17l2.1,2.1M19.1,4.9L17,7M7,17l-2.1,2.1",
        "nuwe_ic_navigate": "M12,2L19,21L12,17L5,21Z",
        "nuwe_ic_location": "M12,3a7,7 0,1 0,0 14a7,7 0,0 0,0 -14M12,7v5l3,2M12,18l-2,4h4z",
    }
    for name, data in paths.items():
        p = APP / f"res/drawable/{name}.xml"
        if p.exists():
            write(p, f'''<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">\n    <path android:fillColor="@android:color/transparent" android:strokeColor="#FFFFFFFF" android:strokeWidth="1.8" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="{data}"/>\n</vector>\n''')


def write_settings_dialog() -> None:
    code = r'''package net.osmand.plus.nuwe;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public final class NuweSettingsDialog {
    private NuweSettingsDialog() {}

    public static void show(@NonNull MapActivity activity) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        GradientDrawable page = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] { Color.rgb(231, 251, 255), Color.rgb(197, 239, 247) });
        scroll.setBackground(page);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 20), dp(activity, 18), dp(activity, 20), dp(activity, 30));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout head = new LinearLayout(activity);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = chip(activity, "‹", true);
        back.setTextSize(34);
        back.setOnClickListener(v -> dialog.dismiss());
        head.addView(back, new LinearLayout.LayoutParams(dp(activity, 52), dp(activity, 52)));
        LinearLayout titles = new LinearLayout(activity);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(activity, 14), 0, 0, 0);
        TextView title = text(activity, "Ajustes Nuwe", 25, Color.rgb(5, 58, 76), true);
        TextView sub = text(activity, "navegação offline · Fruit Aero 2000", 12, Color.rgb(75, 112, 122), false);
        titles.addView(title);
        titles.addView(sub);
        head.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(head);

        TextView profileLabel = text(activity, "PERFIL DE NAVEGAÇÃO", 12, Color.rgb(12, 139, 166), true);
        LinearLayout.LayoutParams lpLabel = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpLabel.topMargin = dp(activity, 24);
        root.addView(profileLabel, lpLabel);

        LinearLayout profiles = new LinearLayout(activity);
        profiles.setGravity(Gravity.CENTER);
        String[] names = {"CARRO", "BIKE", "TRILHA"};
        int[] ids = {R.id.nuwe_profile_car, R.id.nuwe_profile_bike, R.id.nuwe_profile_trail};
        for (int i = 0; i < names.length; i++) {
            TextView b = chip(activity, names[i], false);
            final int targetId = ids[i];
            b.setOnClickListener(v -> {
                View target = activity.findViewById(targetId);
                if (target != null) target.performClick();
            });
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, dp(activity, 48), 1);
            if (i > 0) bp.leftMargin = dp(activity, 8);
            profiles.addView(b, bp);
        }
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.topMargin = dp(activity, 10);
        root.addView(profiles, plp);

        TextView systemLabel = text(activity, "MAPA E SISTEMA", 12, Color.rgb(12, 139, 166), true);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(activity, 24);
        root.addView(systemLabel, slp);

        root.addView(card(activity, Icon.MAP, "Mapa e camadas", "estilo, relevo e conteúdo visível", () -> {
            dialog.dismiss();
            View layers = activity.findViewById(R.id.nuwe_layers_btn);
            if (layers != null) layers.performClick();
        }));
        root.addView(card(activity, Icon.SAT, "GNSS e localização", "Galileo · BeiDou · GLONASS · GPS", () -> {
            try { activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)); }
            catch (Exception e) { activity.startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        }));
        root.addView(card(activity, Icon.VOICE, "Voz offline", "configurar o TTS instalado no aparelho", () -> {
            try { activity.startActivity(new Intent("com.android.settings.TTS_SETTINGS")); }
            catch (Exception e) { activity.startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        }));
        root.addView(card(activity, Icon.DATA, "Dados locais", "GPX, favoritos e mapas ficam no aparelho", () ->
                Toast.makeText(activity, "Dados locais do Nuwe Mapa", Toast.LENGTH_SHORT).show()));
        root.addView(card(activity, Icon.INFO, "Nuwe Mapa 0.4.6", "interface própria sobre o motor offline", null));

        dialog.setContentView(scroll);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setStatusBarColor(Color.rgb(12, 143, 168));
        }
        dialog.setOnShowListener(d -> {
            Window ww = dialog.getWindow();
            if (ww != null) ww.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        });
        dialog.show();
    }

    private static View card(MapActivity a, Icon icon, String title, String subtitle, Runnable action) {
        LinearLayout row = new LinearLayout(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(a, 16), dp(a, 12), dp(a, 16), dp(a, 12));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] { 0xEFFFFFFF, 0xC9D7F5FA });
        bg.setCornerRadius(dp(a, 22));
        bg.setStroke(dp(a, 1), 0xCFFFFFFF);
        row.setBackground(bg);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(a, 78));
        rlp.topMargin = dp(a, 10);
        row.setLayoutParams(rlp);
        Glyph glyph = new Glyph(a, icon);
        row.addView(glyph, new LinearLayout.LayoutParams(dp(a, 40), dp(a, 40)));
        LinearLayout t = new LinearLayout(a);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setPadding(dp(a, 14), 0, 0, 0);
        t.addView(text(a, title, 17, Color.rgb(12, 62, 76), true));
        t.addView(text(a, subtitle, 12, Color.rgb(91, 119, 127), false));
        row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        if (action != null) {
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v -> action.run());
        }
        return row;
    }

    private static TextView chip(MapActivity a, String label, boolean glass) {
        TextView v = text(a, label, 13, glass ? Color.WHITE : Color.rgb(7, 66, 82), true);
        v.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                glass ? new int[] { 0xFF55DCEB, 0xFF0B7799 } : new int[] { 0xEFFFFFFF, 0xBBD1F2F7 });
        bg.setCornerRadius(dp(a, 999));
        bg.setStroke(dp(a, 1), 0xBFFFFFFF);
        v.setBackground(bg);
        return v;
    }

    private static TextView text(MapActivity a, String value, int sp, int color, boolean bold) {
        TextView t = new TextView(a);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    private static int dp(MapActivity a, int v) {
        return Math.round(v * a.getResources().getDisplayMetrics().density);
    }

    private enum Icon { MAP, SAT, VOICE, DATA, INFO }

    private static final class Glyph extends View {
        private final Icon icon;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Glyph(MapActivity a, Icon icon) { super(a); this.icon = icon; p.setColor(Color.rgb(13, 145, 170)); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(a, 2)); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND); }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w = getWidth(), h = getHeight();
            Path q = new Path();
            switch (icon) {
                case MAP:
                    q.moveTo(.1f*w,.25f*h); q.lineTo(.36f*w,.12f*h); q.lineTo(.63f*w,.25f*h); q.lineTo(.9f*w,.12f*h); q.lineTo(.9f*w,.78f*h); q.lineTo(.63f*w,.9f*h); q.lineTo(.36f*w,.78f*h); q.lineTo(.1f*w,.9f*h); q.close(); c.drawPath(q,p); c.drawLine(.36f*w,.12f*h,.36f*w,.78f*h,p); c.drawLine(.63f*w,.25f*h,.63f*w,.9f*h,p); break;
                case SAT:
                    c.drawRect(.34f*w,.18f*h,.66f*w,.5f*h,p); c.rotate(45,w/2,h/2); c.drawLine(.18f*w,.5f*h,.82f*w,.5f*h,p); c.rotate(-45,w/2,h/2); c.drawArc(.12f*w,.55f*h,.72f*w,1.05f*h,205,105,false,p); break;
                case VOICE:
                    q.moveTo(.12f*w,.4f*h); q.lineTo(.32f*w,.4f*h); q.lineTo(.58f*w,.2f*h); q.lineTo(.58f*w,.8f*h); q.lineTo(.32f*w,.6f*h); q.lineTo(.12f*w,.6f*h); q.close(); c.drawPath(q,p); c.drawArc(.58f*w,.32f*h,.92f*w,.68f*h,-55,110,false,p); break;
                case DATA:
                    c.drawRoundRect(.2f*w,.12f*h,.8f*w,.88f*h,.08f*w,.08f*w,p); c.drawLine(.32f*w,.34f*h,.68f*w,.34f*h,p); c.drawLine(.32f*w,.5f*h,.68f*w,.5f*h,p); c.drawLine(.32f*w,.66f*h,.58f*w,.66f*h,p); break;
                case INFO:
                    c.drawCircle(.5f*w,.5f*h,.38f*w,p); c.drawLine(.5f*w,.42f*h,.5f*w,.7f*h,p); c.drawPoint(.5f*w,.28f*h,p); break;
            }
        }
    }
}
'''
    write(APP / "src/net/osmand/plus/nuwe/NuweSettingsDialog.java", code)


def rewrite_controller() -> None:
    code = r'''package net.osmand.plus.nuwe;

import android.graphics.Color;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.search.ShowQuickSearchMode;
import net.osmand.plus.settings.backend.ApplicationMode;

public final class NuweLauncherController {
    private NuweLauncherController() {}

    public static void bind(@NonNull MapActivity activity) {
        View orb = activity.findViewById(R.id.nuwe_launcher_orb);
        View panel = activity.findViewById(R.id.nuwe_launcher_panel);
        if (orb == null || panel == null) return;

        activity.disableDrawer();
        View searchBar = activity.findViewById(R.id.nuwe_search_bar);
        if (searchBar != null) searchBar.setVisibility(View.GONE);
        forceHideLegacyControls(activity);
        activity.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override public void onGlobalLayout() { forceHideLegacyControls(activity); }
                });
        activity.getWindow().getDecorView().getViewTreeObserver().addOnPreDrawListener(() -> {
            forceHideLegacyControls(activity);
            return true;
        });

        orb.setOnClickListener(v -> togglePanel(panel));
        activity.findViewById(R.id.nuwe_action_search).setOnClickListener(v -> {
            closePanel(panel);
            activity.getFragmentsHelper().showQuickSearch(ShowQuickSearchMode.NEW, false);
        });
        activity.findViewById(R.id.nuwe_action_route).setOnClickListener(v -> {
            closePanel(panel);
            activity.getMapActions().showRouteInfoControlDialog();
        });
        activity.findViewById(R.id.nuwe_action_layers).setOnClickListener(v -> {
            closePanel(panel);
            clickLegacy(activity, R.id.map_layers_button);
        });
        activity.findViewById(R.id.nuwe_action_settings).setOnClickListener(v -> {
            closePanel(panel);
            NuweSettingsDialog.show(activity);
        });

        activity.findViewById(R.id.nuwe_location_btn).setOnClickListener(v -> clickLegacy(activity, R.id.map_my_location_button));
        activity.findViewById(R.id.nuwe_layers_btn).setOnClickListener(v -> clickLegacy(activity, R.id.map_layers_button));
        activity.findViewById(R.id.nuwe_zoom_in_btn).setOnClickListener(v -> clickLegacy(activity, R.id.map_zoom_in_button));
        activity.findViewById(R.id.nuwe_zoom_out_btn).setOnClickListener(v -> clickLegacy(activity, R.id.map_zoom_out_button));

        activity.findViewById(R.id.nuwe_profile_car).setOnClickListener(v -> setMode(activity, ApplicationMode.CAR));
        activity.findViewById(R.id.nuwe_profile_bike).setOnClickListener(v -> setMode(activity, ApplicationMode.BICYCLE));
        activity.findViewById(R.id.nuwe_profile_trail).setOnClickListener(v -> setMode(activity, ApplicationMode.PEDESTRIAN));
        updateModeUi(activity);
    }

    private static void togglePanel(@NonNull View panel) {
        if (panel.getVisibility() == View.VISIBLE) closePanel(panel);
        else {
            panel.setAlpha(0f); panel.setScaleX(.88f); panel.setScaleY(.88f); panel.setTranslationY(28f);
            panel.setVisibility(View.VISIBLE);
            panel.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f).setDuration(190).start();
        }
    }

    private static void closePanel(@NonNull View panel) {
        if (panel.getVisibility() != View.VISIBLE) return;
        panel.animate().alpha(0f).scaleX(.9f).scaleY(.9f).translationY(20f).setDuration(140)
                .withEndAction(() -> panel.setVisibility(View.GONE)).start();
    }

    private static void clickLegacy(@NonNull MapActivity activity, int id) {
        View target = activity.findViewById(id);
        if (target != null) target.performClick();
    }

    private static void setMode(@NonNull MapActivity activity, @NonNull ApplicationMode mode) {
        activity.getApp().getSettings().setApplicationMode(mode);
        updateModeUi(activity);
        activity.refreshMapComplete();
    }

    private static void updateModeUi(@NonNull MapActivity activity) {
        ApplicationMode mode = activity.getApp().getSettings().getApplicationMode();
        TextView car = activity.findViewById(R.id.nuwe_profile_car);
        TextView bike = activity.findViewById(R.id.nuwe_profile_bike);
        TextView trail = activity.findViewById(R.id.nuwe_profile_trail);
        TextView badge = activity.findViewById(R.id.nuwe_mode_badge);
        boolean isCar = ApplicationMode.CAR.isDerivedRoutingFrom(mode);
        boolean isBike = ApplicationMode.BICYCLE.isDerivedRoutingFrom(mode);
        boolean isTrail = !isCar && !isBike;
        styleChip(car, isCar); styleChip(bike, isBike); styleChip(trail, isTrail);
        if (badge != null) badge.setText(isCar ? "CARRO" : isBike ? "BIKE" : "TRILHA");
    }

    private static void styleChip(TextView chip, boolean active) {
        if (chip == null) return;
        chip.setBackgroundResource(active ? R.drawable.nuwe2000_chip_active : R.drawable.nuwe2000_chip);
        chip.setTextColor(active ? Color.rgb(7, 58, 75) : Color.WHITE);
    }

    private static void forceHideLegacyControls(@NonNull MapActivity activity) {
        int[] legacy = new int[] {
                R.id.map_menu_button, R.id.map_search_button, R.id.map_layers_button,
                R.id.map_quick_actions_button, R.id.map_route_info_button, R.id.map_compass_button,
                R.id.map_zoom_in_button, R.id.map_zoom_out_button, R.id.map_my_location_button
        };
        for (int id : legacy) {
            View view = activity.findViewById(id);
            if (view != null && view.getVisibility() != View.INVISIBLE) view.setVisibility(View.INVISIBLE);
        }
    }
}
'''
    write(APP / "src/net/osmand/plus/nuwe/NuweLauncherController.java", code)


def write_notice() -> None:
    write(ROOT / "NUWE_V46_SAFE_REBUILD_NOTICE.txt", """Nuwe Mapa 0.4.6\n\nSafe rebuild strategy:\n- starts from the 0.4.2 package/resource baseline, the last build confirmed installable on-device\n- deliberately does NOT apply 0.4.3/0.4.4/0.4.5 resource-table experiments\n- removes the fixed search bar at runtime\n- stabilizes legacy OsmAnd controls as INVISIBLE on every layout/pre-draw while preserving their click handlers\n- layers delegates to the proven underlying map-layers button\n- replaces the OsmAnd settings home with a programmatic Fruit Aero Nuwe settings dialog\n- settings icons are Canvas/Path vector glyphs, not OsmAnd assets\n- retains only Carro, Bike and Trilha in the Nuwe settings UI\n- launcher artwork is a distinct glossy geometric N, not a navigation-arrow lookalike\n- versionCode 5406 / versionName 0.4.6\n""")


def main() -> None:
    bump_version()
    replace_launcher_pngs()
    patch_existing_icons()
    write_settings_dialog()
    rewrite_controller()
    write_notice()
    print("Applied Nuwe Mapa 0.4.6 safe rebuild from 0.4.2 baseline")


if __name__ == "__main__":
    main()
