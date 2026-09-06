package net.osmand.plus.nuwe;

import android.app.Dialog;
import android.graphics.Color;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;

/** Nuwe's central circular-launcher bottom sheet. */
public final class NuweLauncher {
    private NuweLauncher() {}

    public static void show(@NonNull MapActivity activity) {
        Dialog d = new Dialog(activity);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(NuweUi.dp(activity, 18), NuweUi.dp(activity, 8),
                NuweUi.dp(activity, 18), NuweUi.dp(activity, 20));
        content.setBackground(NuweUi.rounded(NuweUi.BG, 28, activity));

        View handle = new View(activity);
        handle.setBackground(NuweUi.rounded(Color.rgb(184, 188, 198), 999, activity));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                NuweUi.dp(activity, 46), NuweUi.dp(activity, 5));
        hp.gravity = Gravity.CENTER_HORIZONTAL;
        hp.bottomMargin = NuweUi.dp(activity, 10);
        content.addView(handle, hp);

        LinearLayout titleRow = new LinearLayout(activity);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = NuweUi.text(activity, "Nuwe Mapa", 23, NuweUi.TEXT, true);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, NuweUi.dp(activity, 46), 1f));
        FrameLayout close = NuweUi.iconButton(activity, NuweUi.Glyph.CLOSE, 42, false, v -> d.dismiss());
        close.setElevation(0);
        titleRow.addView(close);
        content.addView(titleRow);

        TextView sub = NuweUi.text(activity, "Navegação offline", 13, NuweUi.MUTED, false);
        sub.setPadding(0, 0, 0, NuweUi.dp(activity, 12));
        content.addView(sub);
        content.addView(profileRow(activity));

        ScrollView scroll = new ScrollView(activity);
        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        body.addView(NuweUi.section(activity, "Acesso rápido"));

        body.addView(actionRow(activity,
                action(activity, NuweUi.Glyph.SEARCH, "Buscar", () -> { d.dismiss(); NuweSheets.showSearch(activity, false); }),
                action(activity, NuweUi.Glyph.ROUTE, "Trajeto / Rota", () -> { d.dismiss(); NuweSheets.showSearch(activity, true); }),
                action(activity, NuweUi.Glyph.STAR, "Favoritos", () -> { d.dismiss(); NuweSheets.showFavorites(activity); })));

        body.addView(actionRow(activity,
                action(activity, NuweUi.Glyph.TRACK, "Trilhas / GPX", () -> { d.dismiss(); NuweSheets.showTracks(activity); }),
                action(activity, NuweUi.Glyph.PLAY, "Gravar trajeto", () -> { d.dismiss(); NuweSheets.showTracks(activity); }),
                action(activity, NuweUi.Glyph.MAP, "Mapas offline", () -> { d.dismiss(); NuweSheets.showMaps(activity); })));

        body.addView(actionRow(activity,
                action(activity, NuweUi.Glyph.SHARE, "Compartilhar localização", () -> { d.dismiss(); NuweMapShell.shareLocation(activity, null); }),
                action(activity, NuweUi.Glyph.PIN, "Adicionar marcador", () -> { d.dismiss(); NuweSheets.showAddMarker(activity, null); }),
                action(activity, NuweUi.Glyph.SETTINGS, "Configurações", () -> { d.dismiss(); NuweSheets.showSettings(activity); })));

        scroll.addView(body);
        content.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        d.setContentView(content);
        showBottom(d, activity, 58);
        makeDraggable(handle, d, activity);
    }

    private static LinearLayout profileRow(@NonNull MapActivity activity) {
        LinearLayout profiles = new LinearLayout(activity);
        profiles.setOrientation(LinearLayout.HORIZONTAL);
        profiles.setGravity(Gravity.CENTER);
        ApplicationMode current = activity.getSettings().getApplicationMode();
        profiles.addView(profileChip(activity, "CARRO", ApplicationMode.CAR, current == ApplicationMode.CAR));
        profiles.addView(profileChip(activity, "BIKE", ApplicationMode.BICYCLE, current == ApplicationMode.BICYCLE));
        profiles.addView(profileChip(activity, "TRILHA", ApplicationMode.PEDESTRIAN, current == ApplicationMode.PEDESTRIAN));
        return profiles;
    }

    private static TextView profileChip(@NonNull MapActivity activity, String label,
                                        ApplicationMode mode, boolean selected) {
        TextView chip = NuweUi.chip(activity, label, selected, v -> {
            activity.getSettings().setApplicationMode(mode);
            ViewGroup parent = (ViewGroup) v.getParent();
            if (parent != null) {
                ViewGroup grand = (ViewGroup) parent.getParent();
                if (grand != null) {
                    int index = grand.indexOfChild(parent);
                    grand.removeView(parent);
                    grand.addView(profileRow(activity), index);
                }
            }
            NuweMapShell.refreshMap(activity);
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.leftMargin = NuweUi.dp(activity, 4);
        p.rightMargin = NuweUi.dp(activity, 4);
        chip.setLayoutParams(p);
        return chip;
    }

    private static LinearLayout action(@NonNull MapActivity a, NuweUi.Glyph glyph,
                                       String text, Runnable runnable) {
        return NuweUi.actionTile(a, glyph, text, v -> runnable.run());
    }

    private static LinearLayout actionRow(@NonNull MapActivity c, View... actions) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (View v : actions) {
            row.addView(v, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        return row;
    }

    private static void showBottom(@NonNull Dialog d, @NonNull MapActivity activity, int percent) {
        d.setOnShowListener(x -> resizeDialog(d, activity, percent));
        d.show();
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            w.setGravity(Gravity.BOTTOM);
            w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            resizeDialog(d, activity, percent);
        }
    }

    private static void resizeDialog(@NonNull Dialog d, @NonNull MapActivity activity, int percent) {
        Window w = d.getWindow();
        if (w == null) return;
        int h = Math.round(activity.getResources().getDisplayMetrics().heightPixels * (percent / 100f));
        w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, h);
        w.setGravity(Gravity.BOTTOM);
    }

    private static void makeDraggable(@NonNull View handle, @NonNull Dialog dialog,
                                      @NonNull MapActivity activity) {
        final float[] downY = new float[1];
        final int[] startPct = new int[]{58};
        handle.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                downY[0] = event.getRawY();
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                float dy = downY[0] - event.getRawY();
                int delta = Math.round((dy / activity.getResources().getDisplayMetrics().heightPixels) * 100f);
                int pct = Math.max(45, Math.min(94, startPct[0] + delta));
                resizeDialog(dialog, activity, pct);
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                float dy = downY[0] - event.getRawY();
                if (dy > NuweUi.dp(activity, 55)) {
                    startPct[0] = 94;
                } else if (dy < -NuweUi.dp(activity, 70)) {
                    startPct[0] = 45;
                } else {
                    startPct[0] = 58;
                }
                resizeDialog(dialog, activity, startPct[0]);
                return true;
            }
            return false;
        });
    }
}
