package net.osmand.plus.nuwe;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

public final class NuweUi {
    public static final int BG = Color.rgb(246, 247, 249);
    public static final int CARD = Color.WHITE;
    public static final int TEXT = Color.rgb(31, 35, 49);
    public static final int MUTED = Color.rgb(143, 147, 160);
    public static final int LINE = Color.rgb(230, 232, 238);
    public static final int ACCENT = Color.rgb(86, 103, 245);
    public static final int ACCENT_DARK = Color.rgb(68, 83, 218);
    public static final int RED = Color.rgb(230, 75, 83);
    public static final int GREEN = Color.rgb(33, 166, 113);

    private NuweUi() {}

    public static int dp(@NonNull Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable rounded(int color, float radiusDp, @NonNull Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static GradientDrawable roundedStroke(int color, float radiusDp, int strokeColor, float strokeDp, @NonNull Context c) {
        GradientDrawable d = rounded(color, radiusDp, c);
        d.setStroke(dp(c, strokeDp), strokeColor);
        return d;
    }

    public static StateListDrawable pressable(int normal, int pressed, float radiusDp, @NonNull Context c) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, rounded(pressed, radiusDp, c));
        states.addState(new int[]{}, rounded(normal, radiusDp, c));
        return states;
    }

    public static TextView text(@NonNull Context c, String value, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) {
            t.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        }
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    public static TextView section(@NonNull Context c, String value) {
        TextView t = text(c, value, 13, MUTED, true);
        t.setAllCaps(false);
        t.setPadding(dp(c, 4), dp(c, 16), dp(c, 4), dp(c, 8));
        return t;
    }

    public static View divider(@NonNull Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(c, 1)));
        return v;
    }

    public static LinearLayout card(@NonNull Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(rounded(CARD, 20, c));
        l.setPadding(dp(c, 14), dp(c, 8), dp(c, 14), dp(c, 8));
        l.setElevation(dp(c, 1));
        return l;
    }

    public static FrameLayout iconButton(@NonNull Context c, Glyph glyph, int sizeDp, boolean accent, View.OnClickListener listener) {
        FrameLayout root = new FrameLayout(c);
        int size = dp(c, sizeDp);
        root.setLayoutParams(new FrameLayout.LayoutParams(size, size));
        root.setBackground(pressable(accent ? ACCENT : CARD, accent ? ACCENT_DARK : Color.rgb(236, 238, 244), sizeDp / 2f, c));
        root.setElevation(dp(c, 3));
        root.setClickable(true);
        root.setFocusable(true);
        root.setOnClickListener(listener);

        GlyphView icon = new GlyphView(c, glyph, accent ? Color.WHITE : TEXT);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(dp(c, sizeDp * 0.48f), dp(c, sizeDp * 0.48f), Gravity.CENTER);
        root.addView(icon, ip);
        return root;
    }

    public static LinearLayout actionTile(@NonNull Context c, Glyph glyph, String label, View.OnClickListener listener) {
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(c, 6), dp(c, 8), dp(c, 6), dp(c, 8));
        root.setClickable(true);
        root.setFocusable(true);
        root.setBackground(pressable(Color.TRANSPARENT, Color.rgb(238, 240, 246), 16, c));
        root.setOnClickListener(listener);

        FrameLayout icon = iconButton(c, glyph, 48, false, v -> root.performClick());
        icon.setElevation(0);
        root.addView(icon);
        TextView t = text(c, label, 12, TEXT, true);
        t.setGravity(Gravity.CENTER);
        t.setMaxLines(2);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tp.topMargin = dp(c, 6);
        root.addView(t, tp);
        return root;
    }

    public static LinearLayout menuRow(@NonNull Context c, Glyph glyph, String title, String subtitle, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(c, 4), dp(c, 10), dp(c, 4), dp(c, 10));
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackground(pressable(Color.TRANSPARENT, Color.rgb(241, 242, 246), 14, c));
        row.setOnClickListener(listener);

        GlyphView icon = new GlyphView(c, glyph, TEXT);
        row.addView(icon, new LinearLayout.LayoutParams(dp(c, 26), dp(c, 26)));

        LinearLayout labels = new LinearLayout(c);
        labels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = dp(c, 14);
        TextView tt = text(c, title, 16, TEXT, false);
        labels.addView(tt);
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView st = text(c, subtitle, 12, MUTED, false);
            st.setPadding(0, dp(c, 2), 0, 0);
            labels.addView(st);
        }
        row.addView(labels, lp);
        TextView chevron = text(c, "›", 28, MUTED, false);
        chevron.setGravity(Gravity.CENTER);
        row.addView(chevron, new LinearLayout.LayoutParams(dp(c, 28), dp(c, 40)));
        return row;
    }

    public static TextView chip(@NonNull Context c, String label, boolean selected, View.OnClickListener listener) {
        TextView t = text(c, label, 13, selected ? Color.WHITE : TEXT, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 16), dp(c, 9), dp(c, 16), dp(c, 9));
        t.setBackground(rounded(selected ? ACCENT : Color.rgb(238, 240, 245), 999, c));
        t.setClickable(true);
        t.setOnClickListener(listener);
        return t;
    }

    public enum Glyph {
        HUB, LAYERS, RULER, COMPASS, LOCATION, PLUS, MINUS, CUBE,
        SEARCH, ROUTE, STAR, TRACK, MAP, PIN, SHARE, SETTINGS,
        SATELLITE, VOICE, INFO, CLOSE, MORE, SAVE, PLAY, PAUSE
    }

    public static final class GlyphView extends View {
        private final Glyph glyph;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        public GlyphView(@NonNull Context context, Glyph glyph, int color) {
            super(context);
            this.glyph = glyph;
            p.setColor(color);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setStyle(Paint.Style.STROKE);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float sx = getWidth() / 24f;
            float sy = getHeight() / 24f;
            canvas.save();
            canvas.scale(sx, sy);
            p.setStrokeWidth(2.15f);
            p.setStyle(Paint.Style.STROKE);
            path.reset();
            switch (glyph) {
                case HUB:
                    for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++) canvas.drawRoundRect(4 + x * 9, 4 + y * 9, 11 + x * 9, 11 + y * 9, 2, 2, p);
                    break;
                case LAYERS:
                    diamond(canvas, 12, 5, 8, 4); diamond(canvas, 12, 11, 8, 4); diamond(canvas, 12, 17, 8, 4); break;
                case RULER:
                    canvas.rotate(-35, 12, 12); canvas.drawRoundRect(5, 8, 19, 16, 1.5f, 1.5f, p); for (int i = 8; i <= 16; i += 3) canvas.drawLine(i, 8, i, 11, p); break;
                case COMPASS:
                    canvas.drawCircle(12, 12, 8, p); path.moveTo(14.8f, 6); path.lineTo(12.8f, 13); path.lineTo(6, 17); path.lineTo(10.7f, 10.8f); path.close(); canvas.drawPath(path, p); break;
                case LOCATION:
                    path.moveTo(12, 3); path.lineTo(19, 20); path.lineTo(12.2f, 16.6f); path.lineTo(7.4f, 21); path.close(); p.setStyle(Paint.Style.FILL); canvas.drawPath(path, p); break;
                case PLUS: canvas.drawLine(12, 5, 12, 19, p); canvas.drawLine(5, 12, 19, 12, p); break;
                case MINUS: canvas.drawLine(5, 12, 19, 12, p); break;
                case CUBE:
                    path.moveTo(12, 3); path.lineTo(20, 7); path.lineTo(12, 11); path.lineTo(4, 7); path.close(); canvas.drawPath(path, p); canvas.drawLine(4,7,4,16,p); canvas.drawLine(20,7,20,16,p); canvas.drawLine(4,16,12,21,p); canvas.drawLine(20,16,12,21,p); canvas.drawLine(12,11,12,21,p); break;
                case SEARCH: canvas.drawCircle(10.5f, 10.5f, 5.5f, p); canvas.drawLine(14.5f, 14.5f, 20, 20, p); break;
                case ROUTE:
                    canvas.drawCircle(6, 18, 2, p); canvas.drawCircle(18, 6, 2, p); path.moveTo(8,18); path.cubicTo(14,18,10,8,16,8); canvas.drawPath(path,p); break;
                case STAR:
                    path.moveTo(12,3); path.lineTo(14.8f,9); path.lineTo(21,9.6f); path.lineTo(16.2f,13.8f); path.lineTo(17.7f,20); path.lineTo(12,16.7f); path.lineTo(6.3f,20); path.lineTo(7.8f,13.8f); path.lineTo(3,9.6f); path.lineTo(9.2f,9); path.close(); canvas.drawPath(path,p); break;
                case TRACK:
                    canvas.drawCircle(5,16,2,p); canvas.drawCircle(19,7,2,p); path.moveTo(7,16); path.cubicTo(12,16,9,8,14,8); path.cubicTo(16,8,16,7,17,7); canvas.drawPath(path,p); break;
                case MAP:
                    path.moveTo(3,6); path.lineTo(9,3); path.lineTo(15,6); path.lineTo(21,3); path.lineTo(21,18); path.lineTo(15,21); path.lineTo(9,18); path.lineTo(3,21); path.close(); canvas.drawPath(path,p); canvas.drawLine(9,3,9,18,p); canvas.drawLine(15,6,15,21,p); break;
                case PIN:
                    canvas.drawCircle(12,9,3,p); path.moveTo(12,21); path.cubicTo(8,15,6,12,6,9); path.cubicTo(6,5.5f,8.7f,3,12,3); path.cubicTo(15.3f,3,18,5.5f,18,9); path.cubicTo(18,12,16,15,12,21); canvas.drawPath(path,p); break;
                case SHARE:
                    canvas.drawCircle(6,12,2,p); canvas.drawCircle(17,6,2,p); canvas.drawCircle(17,18,2,p); canvas.drawLine(8,11,15,7,p); canvas.drawLine(8,13,15,17,p); break;
                case SETTINGS:
                    canvas.drawCircle(12,12,3,p); for(int i=0;i<8;i++){ double a=i*Math.PI/4; float x1=(float)(12+6*Math.cos(a)); float y1=(float)(12+6*Math.sin(a)); float x2=(float)(12+9*Math.cos(a)); float y2=(float)(12+9*Math.sin(a)); canvas.drawLine(x1,y1,x2,y2,p);} break;
                case SATELLITE:
                    canvas.rotate(-35,12,12); canvas.drawRect(8,8,16,16,p); canvas.drawLine(5,5,8,8,p); canvas.drawLine(16,16,19,19,p); canvas.drawLine(8,12,4,12,p); canvas.drawLine(16,12,20,12,p); break;
                case VOICE:
                    canvas.drawRoundRect(9,4,15,14,3,3,p); canvas.drawLine(6,11,6,12,p); path.moveTo(6,12); path.cubicTo(6,18,18,18,18,12); canvas.drawPath(path,p); canvas.drawLine(12,18,12,21,p); break;
                case INFO: canvas.drawCircle(12,12,9,p); canvas.drawLine(12,10,12,17,p); p.setStyle(Paint.Style.FILL); canvas.drawCircle(12,6.8f,1.2f,p); break;
                case CLOSE: canvas.drawLine(6,6,18,18,p); canvas.drawLine(18,6,6,18,p); break;
                case MORE: p.setStyle(Paint.Style.FILL); canvas.drawCircle(6,12,1.6f,p); canvas.drawCircle(12,12,1.6f,p); canvas.drawCircle(18,12,1.6f,p); break;
                case SAVE: canvas.drawRect(5,4,19,20,p); canvas.drawRect(8,4,16,9,p); canvas.drawRect(8,13,16,20,p); break;
                case PLAY: path.moveTo(8,5); path.lineTo(19,12); path.lineTo(8,19); path.close(); p.setStyle(Paint.Style.FILL); canvas.drawPath(path,p); break;
                case PAUSE: p.setStyle(Paint.Style.FILL); canvas.drawRoundRect(7,5,10,19,1,1,p); canvas.drawRoundRect(14,5,17,19,1,1,p); break;
            }
            canvas.restore();
        }

        private void diamond(Canvas c, float cx, float cy, float rx, float ry) {
            path.reset(); path.moveTo(cx, cy-ry); path.lineTo(cx+rx,cy); path.lineTo(cx,cy+ry); path.lineTo(cx-rx,cy); path.close(); c.drawPath(path,p);
        }
    }
}
