package net.osmand.plus.nuwe;

import static net.osmand.plus.views.OsmandMapTileView.DEFAULT_ELEVATION_ANGLE;

import android.Manifest;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import net.osmand.Location;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.enums.CompassMode;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.configure.buttons.Map3DButtonState;
import net.osmand.util.MapUtils;

import java.lang.ref.WeakReference;
import java.util.Locale;

public final class NuweMapShell {
    private static WeakReference<MapActivity> activityRef = new WeakReference<>(null);
    private static FrameLayout shell;
    private static TextView navigationBanner;
    private static boolean measureMode;
    private static LatLon measureStart;
    private static final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());

    private NuweMapShell() {}

    public static void bind(@NonNull MapActivity activity) {
        activityRef = new WeakReference<>(activity);
        if (!"app.nuwe.mapa".equals(activity.getPackageName())) return;

        activity.disableDrawer();
        hideNativeHud(activity);

        View existing = activity.findViewById(android.R.id.custom);
        if (existing != null && existing.getTag() != null && "nuwe_v50_shell".equals(existing.getTag())) return;

        View hudView = activity.findViewById(R.id.map_hud_layout);
        if (!(hudView instanceof ViewGroup hud)) return;

        shell = new FrameLayout(activity);
        shell.setId(android.R.id.custom);
        shell.setTag("nuwe_v50_shell");
        shell.setClickable(false);
        shell.setFocusable(false);
        hud.addView(shell, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        addMapControls(activity);
        addLauncher(activity);
        addNavigationBanner(activity);
        scheduleNavigationRefresh();
    }

    private static void hideNativeHud(@NonNull MapActivity activity) {
        View top = activity.findViewById(R.id.MapHudButtonsOverlayTop);
        View bottom = activity.findViewById(R.id.MapHudButtonsOverlayBottom);
        View quick = activity.findViewById(R.id.quick_action_widget);
        View widgets = activity.findViewById(R.id.map_bottom_widgets_panel);
        if (top != null) top.setVisibility(View.GONE);
        if (bottom != null) bottom.setVisibility(View.GONE);
        if (quick != null) quick.setVisibility(View.GONE);
        if (widgets != null) widgets.setVisibility(View.GONE);
    }

    private static void addMapControls(@NonNull MapActivity activity) {
        int margin = NuweUi.dp(activity, 16);
        int top = NuweUi.dp(activity, 18);

        FrameLayout layers = NuweUi.iconButton(activity, NuweUi.Glyph.LAYERS, 54, false, v -> NuweSheets.showLayers(activity));
        FrameLayout.LayoutParams lpLayers = new FrameLayout.LayoutParams(NuweUi.dp(activity,54), NuweUi.dp(activity,54), Gravity.TOP | Gravity.START);
        lpLayers.leftMargin = margin;
        lpLayers.topMargin = top;
        shell.addView(layers, lpLayers);

        LinearLayout rightTop = new LinearLayout(activity);
        rightTop.setOrientation(LinearLayout.VERTICAL);
        rightTop.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout compass = NuweUi.iconButton(activity, NuweUi.Glyph.COMPASS, 50, false, v -> NuweSheets.showCompass(activity));
        FrameLayout ruler = NuweUi.iconButton(activity, NuweUi.Glyph.RULER, 50, false, v -> startMeasure(activity));
        FrameLayout cube = NuweUi.iconButton(activity, NuweUi.Glyph.CUBE, 50, false, v -> toggle3d(activity));
        rightTop.addView(compass);
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(NuweUi.dp(activity,50), NuweUi.dp(activity,50)); gap.topMargin = NuweUi.dp(activity,10); rightTop.addView(ruler, gap);
        LinearLayout.LayoutParams gap2 = new LinearLayout.LayoutParams(NuweUi.dp(activity,50), NuweUi.dp(activity,50)); gap2.topMargin = NuweUi.dp(activity,10); rightTop.addView(cube, gap2);
        FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(NuweUi.dp(activity,50), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        rp.rightMargin = margin;
        rp.topMargin = top;
        shell.addView(rightTop, rp);

        LinearLayout zoom = new LinearLayout(activity);
        zoom.setOrientation(LinearLayout.VERTICAL);
        zoom.setBackground(NuweUi.roundedStroke(NuweUi.CARD, 26, NuweUi.LINE, 1, activity));
        zoom.setElevation(NuweUi.dp(activity,3));
        FrameLayout plus = NuweUi.iconButton(activity, NuweUi.Glyph.PLUS, 50, false, v -> zoomIn(activity));
        FrameLayout minus = NuweUi.iconButton(activity, NuweUi.Glyph.MINUS, 50, false, v -> zoomOut(activity));
        plus.setBackgroundColor(android.graphics.Color.TRANSPARENT); plus.setElevation(0);
        minus.setBackgroundColor(android.graphics.Color.TRANSPARENT); minus.setElevation(0);
        zoom.addView(plus);
        zoom.addView(NuweUi.divider(activity), new LinearLayout.LayoutParams(NuweUi.dp(activity,36), NuweUi.dp(activity,1)));
        zoom.addView(minus);
        FrameLayout.LayoutParams zp = new FrameLayout.LayoutParams(NuweUi.dp(activity,50), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END | Gravity.CENTER_VERTICAL);
        zp.rightMargin = margin;
        shell.addView(zoom, zp);

        FrameLayout location = NuweUi.iconButton(activity, NuweUi.Glyph.LOCATION, 58, false, v -> goToLocation(activity));
        FrameLayout.LayoutParams locp = new FrameLayout.LayoutParams(NuweUi.dp(activity,58), NuweUi.dp(activity,58), Gravity.BOTTOM | Gravity.END);
        locp.rightMargin = margin;
        locp.bottomMargin = NuweUi.dp(activity,28);
        shell.addView(location, locp);
    }

    private static void addLauncher(@NonNull MapActivity activity) {
        FrameLayout launcher = NuweUi.iconButton(activity, NuweUi.Glyph.HUB, 66, true, v -> NuweSheets.showLauncher(activity));
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(NuweUi.dp(activity,66), NuweUi.dp(activity,66), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        p.bottomMargin = NuweUi.dp(activity,20);
        shell.addView(launcher, p);
    }

    private static void addNavigationBanner(@NonNull MapActivity activity) {
        navigationBanner = NuweUi.text(activity, "", 15, NuweUi.TEXT, true);
        navigationBanner.setGravity(Gravity.CENTER);
        navigationBanner.setPadding(NuweUi.dp(activity,18), NuweUi.dp(activity,11), NuweUi.dp(activity,18), NuweUi.dp(activity,11));
        navigationBanner.setBackground(NuweUi.roundedStroke(NuweUi.CARD, 22, NuweUi.LINE, 1, activity));
        navigationBanner.setElevation(NuweUi.dp(activity,4));
        navigationBanner.setVisibility(View.GONE);
        navigationBanner.setOnClickListener(v -> NuweSheets.showNavigation(activity));
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, NuweUi.dp(activity,46), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        p.topMargin = NuweUi.dp(activity,18);
        p.leftMargin = NuweUi.dp(activity,78);
        p.rightMargin = NuweUi.dp(activity,78);
        shell.addView(navigationBanner, p);
    }

    private static void scheduleNavigationRefresh() {
        handler.removeCallbacksAndMessages(null);
        handler.post(new Runnable() {
            @Override public void run() {
                MapActivity activity = activityRef.get();
                if (activity == null || activity.isFinishing()) return;
                updateNavigationBanner(activity);
                handler.postDelayed(this, 1000);
            }
        });
    }

    private static void updateNavigationBanner(@NonNull MapActivity activity) {
        if (navigationBanner == null) return;
        RoutingHelper rh = activity.getRoutingHelper();
        if (rh != null && rh.isFollowingMode() && rh.isRouteCalculated()) {
            int left = rh.getLeftDistance();
            int secs = rh.getLeftTime();
            String dist = OsmAndFormatter.getFormattedDistance(left, activity.getApp());
            String time = secs >= 3600 ? String.format(Locale.getDefault(), "%dh %02dmin", secs / 3600, (secs % 3600) / 60)
                    : String.format(Locale.getDefault(), "%d min", Math.max(1, secs / 60));
            navigationBanner.setText("Navegação  •  " + dist + "  •  " + time);
            navigationBanner.setVisibility(View.VISIBLE);
        } else {
            navigationBanner.setVisibility(View.GONE);
        }
    }

    public static void zoomIn(@NonNull MapActivity activity) {
        activity.getMapViewTrackingUtilities().resetBackToLocation();
        activity.getMapView().zoomInAndAdjustTiltAngle();
    }

    public static void zoomOut(@NonNull MapActivity activity) {
        activity.getMapViewTrackingUtilities().resetBackToLocation();
        activity.getMapView().zoomOutAndAdjustTiltAngle();
    }

    public static void goToLocation(@NonNull MapActivity activity) {
        if (OsmAndLocationProvider.isLocationPermissionAvailable(activity)) {
            activity.getMapViewTrackingUtilities().backToLocationImpl();
        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    OsmAndLocationProvider.REQUEST_LOCATION_PERMISSION);
        }
    }

    public static void setCompassMode(@NonNull MapActivity activity, @NonNull CompassMode mode) {
        activity.getApp().getMapViewTrackingUtilities().switchCompassModeTo(mode);
        activity.getMapView().refreshMap();
    }

    public static void toggle3d(@NonNull MapActivity activity) {
        OsmandMapTileView mapView = activity.getMapView();
        Map3DButtonState state = activity.getApp().getMapButtonsHelper().getMap3DButtonState();
        boolean flat = state.isFlatMapMode();
        float target = mapView.normalizeElevationAngle(flat ? mapView.getAdjustedTiltAngle(mapView.getZoom(), true) : DEFAULT_ELEVATION_ANGLE);
        activity.getSettings().setLastKnownMapElevation(target);
        mapView.getAnimatedDraggingThread().startTilting(target, 0f);
        mapView.refreshMap();
    }

    public static void startMeasure(@NonNull MapActivity activity) {
        measureMode = true;
        measureStart = null;
        Toast.makeText(activity, "Régua: toque no ponto inicial do mapa", Toast.LENGTH_SHORT).show();
    }

    public static boolean handleContextPoint(@NonNull MapActivity activity, @NonNull LatLon latLon,
                                             @Nullable PointDescription description, @Nullable Object object) {
        if (measureMode) {
            if (measureStart == null) {
                measureStart = latLon;
                Toast.makeText(activity, "Agora toque no ponto final", Toast.LENGTH_SHORT).show();
            } else {
                double meters = MapUtils.getDistance(measureStart, latLon);
                LatLon first = measureStart;
                measureStart = null;
                measureMode = false;
                NuweSheets.showMeasureResult(activity, first, latLon, meters);
            }
            return true;
        }
        NuweSheets.showPoint(activity, latLon, description, object);
        return true;
    }

    public static void cancelMeasure() {
        measureMode = false;
        measureStart = null;
    }

    public static void routeTo(@NonNull MapActivity activity, @NonNull LatLon point, @Nullable String name) {
        PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_TARGET, name == null ? "Destino" : name);
        activity.getApp().getTargetPointsHelper().removeAllWayPoints(false, true);
        activity.getApp().getTargetPointsHelper().navigateToPoint(point, true, -1, pd);
        activity.getMapActions().startNavigation();
        goToLocation(activity);
    }

    public static void stopNavigation(@NonNull MapActivity activity) {
        activity.getApp().stopNavigation();
        updateNavigationBanner(activity);
    }

    public static void centerOn(@NonNull MapActivity activity, @NonNull LatLon point) {
        activity.getMapViewTrackingUtilities().setMapLinkedToLocation(false);
        activity.getMapView().setLatLon(point.getLatitude(), point.getLongitude());
        if (activity.getMapView().getZoom() < 15) activity.getMapView().setIntZoom(15);
        activity.getMapView().refreshMap();
    }

    public static void addFavoriteAt(@NonNull MapActivity activity, @NonNull LatLon point, @Nullable String name) {
        String n = (name == null || name.trim().isEmpty()) ? "Marcador Nuwe" : name.trim();
        FavouritePoint fav = new FavouritePoint(point.getLatitude(), point.getLongitude(), n, "Nuwe");
        fav.setVisible(true);
        activity.getApp().getFavoritesHelper().addFavourite(fav);
        activity.getSettings().SHOW_FAVORITES.set(true);
        activity.getMapView().refreshMap();
        Toast.makeText(activity, "Marcador salvo", Toast.LENGTH_SHORT).show();
    }

    public static void addFavoriteAtCenter(@NonNull MapActivity activity, @Nullable String name) {
        addFavoriteAt(activity, activity.getMapView().getCurrentRotatedTileBox().getCenterLatLon(), name);
    }

    public static void shareLocation(@NonNull MapActivity activity, @Nullable LatLon explicit) {
        LatLon point = explicit;
        if (point == null) {
            Location l = activity.getApp().getLocationProvider().getLastKnownLocation();
            if (l != null) point = new LatLon(l.getLatitude(), l.getLongitude());
        }
        if (point == null) {
            Toast.makeText(activity, "Localização ainda não disponível", Toast.LENGTH_SHORT).show();
            return;
        }
        String text = String.format(Locale.US, "%.6f, %.6f", point.getLatitude(), point.getLongitude());
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        activity.startActivity(Intent.createChooser(intent, "Compartilhar localização"));
    }

    public static void refreshMap(@NonNull MapActivity activity) {
        activity.getMapView().refreshMap(true);
    }
}
