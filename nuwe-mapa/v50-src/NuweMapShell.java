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
        View existing = activity.findViewById(android.R.id.custom);
        if (existing != null && existing.getTag() != null && "nuwe_v50_shell".equals(existing.getTag())) return;
        View hudView = activity.findViewById(net.osmand.plus.R.id.map_hud_layout);
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

    private static void addMapControls(@NonNull MapActivity activity) {
        int margin = NuweUi.dp(activity, 16), top = NuweUi.dp(activity, 18);
        FrameLayout layers = NuweUi.iconButton(activity, NuweUi.Glyph.LAYERS, 54, false, v -> NuweSheets.showLayers(activity));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(NuweUi.dp(activity,54), NuweUi.dp(activity,54), Gravity.TOP | Gravity.START); lp.leftMargin=margin; lp.topMargin=top; shell.addView(layers,lp);
        LinearLayout right = new LinearLayout(activity); right.setOrientation(LinearLayout.VERTICAL); right.setGravity(Gravity.CENTER_HORIZONTAL);
        right.addView(NuweUi.iconButton(activity,NuweUi.Glyph.COMPASS,50,false,v->NuweSheets.showCompass(activity)));
        right.addView(NuweUi.iconButton(activity,NuweUi.Glyph.RULER,50,false,v->startMeasure(activity)));
        right.addView(NuweUi.iconButton(activity,NuweUi.Glyph.CUBE,50,false,v->toggle3d(activity)));
        FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(NuweUi.dp(activity,50),ViewGroup.LayoutParams.WRAP_CONTENT,Gravity.TOP|Gravity.END); rp.rightMargin=margin;rp.topMargin=top;shell.addView(right,rp);
        LinearLayout zoom=new LinearLayout(activity);zoom.setOrientation(LinearLayout.VERTICAL);zoom.addView(NuweUi.iconButton(activity,NuweUi.Glyph.PLUS,50,false,v->zoomIn(activity)));zoom.addView(NuweUi.iconButton(activity,NuweUi.Glyph.MINUS,50,false,v->zoomOut(activity)));
        FrameLayout.LayoutParams zp=new FrameLayout.LayoutParams(NuweUi.dp(activity,50),ViewGroup.LayoutParams.WRAP_CONTENT,Gravity.END|Gravity.CENTER_VERTICAL);zp.rightMargin=margin;shell.addView(zoom,zp);
        FrameLayout location=NuweUi.iconButton(activity,NuweUi.Glyph.LOCATION,58,false,v->goToLocation(activity));FrameLayout.LayoutParams locp=new FrameLayout.LayoutParams(NuweUi.dp(activity,58),NuweUi.dp(activity,58),Gravity.BOTTOM|Gravity.END);locp.rightMargin=margin;locp.bottomMargin=NuweUi.dp(activity,28);shell.addView(location,locp);
    }

    private static void addLauncher(@NonNull MapActivity activity) {
        FrameLayout launcher = NuweUi.iconButton(activity, NuweUi.Glyph.HUB, 66, true, v -> NuweMapController.openLauncher(activity));
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(NuweUi.dp(activity,66), NuweUi.dp(activity,66), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL); p.bottomMargin=NuweUi.dp(activity,20); shell.addView(launcher,p);
    }

    private static void addNavigationBanner(@NonNull MapActivity activity) {
        navigationBanner=NuweUi.text(activity,"",15,NuweUi.TEXT,true);navigationBanner.setGravity(Gravity.CENTER);navigationBanner.setVisibility(View.GONE);navigationBanner.setOnClickListener(v->NuweSheets.showNavigation(activity));FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,NuweUi.dp(activity,46),Gravity.TOP|Gravity.CENTER_HORIZONTAL);p.topMargin=NuweUi.dp(activity,18);shell.addView(navigationBanner,p);
    }
    private static void scheduleNavigationRefresh(){handler.removeCallbacksAndMessages(null);handler.post(new Runnable(){@Override public void run(){MapActivity a=activityRef.get();if(a==null||a.isFinishing())return;updateNavigationBanner(a);handler.postDelayed(this,1000);}});}
    private static void updateNavigationBanner(@NonNull MapActivity activity){if(navigationBanner==null)return;RoutingHelper rh=activity.getRoutingHelper();if(rh!=null&&rh.isFollowingMode()&&rh.isRouteCalculated()){int left=rh.getLeftDistance(),secs=rh.getLeftTime();String dist=OsmAndFormatter.getFormattedDistance(left,activity.getApp());String time=secs>=3600?String.format(Locale.getDefault(),"%dh %02dmin",secs/3600,(secs%3600)/60):String.format(Locale.getDefault(),"%d min",Math.max(1,secs/60));navigationBanner.setText("Navegação  •  "+dist+"  •  "+time);navigationBanner.setVisibility(View.VISIBLE);}else navigationBanner.setVisibility(View.GONE);}
    public static void zoomIn(@NonNull MapActivity a){a.getMapViewTrackingUtilities().resetBackToLocation();a.getMapView().zoomInAndAdjustTiltAngle();}
    public static void zoomOut(@NonNull MapActivity a){a.getMapViewTrackingUtilities().resetBackToLocation();a.getMapView().zoomOutAndAdjustTiltAngle();}
    public static void goToLocation(@NonNull MapActivity a){if(OsmAndLocationProvider.isLocationPermissionAvailable(a))a.getMapViewTrackingUtilities().backToLocationImpl();else ActivityCompat.requestPermissions(a,new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},OsmAndLocationProvider.REQUEST_LOCATION_PERMISSION);}
    public static void setCompassMode(@NonNull MapActivity a,@NonNull CompassMode m){a.getApp().getMapViewTrackingUtilities().switchCompassModeTo(m);a.getMapView().refreshMap();}
    public static void toggle3d(@NonNull MapActivity a){OsmandMapTileView mv=a.getMapView();Map3DButtonState state=a.getApp().getMapButtonsHelper().getMap3DButtonState();float target=mv.normalizeElevationAngle(state.isFlatMapMode()?mv.getAdjustedTiltAngle(mv.getZoom(),true):DEFAULT_ELEVATION_ANGLE);a.getSettings().setLastKnownMapElevation(target);mv.getAnimatedDraggingThread().startTilting(target,0f);mv.refreshMap();}
    public static void startMeasure(@NonNull MapActivity a){measureMode=true;measureStart=null;Toast.makeText(a,"Régua: toque no ponto inicial do mapa",Toast.LENGTH_SHORT).show();}
    public static boolean handleContextPoint(@NonNull MapActivity a,@NonNull LatLon p,@Nullable PointDescription d,@Nullable Object o){if(measureMode){if(measureStart==null){measureStart=p;Toast.makeText(a,"Agora toque no ponto final",Toast.LENGTH_SHORT).show();}else{double m=MapUtils.getDistance(measureStart,p);LatLon first=measureStart;measureStart=null;measureMode=false;NuweSheets.showMeasureResult(a,first,p,m);}return true;}NuweSheets.showPoint(a,p,d,o);return true;}
    public static void cancelMeasure(){measureMode=false;measureStart=null;}
    public static void routeTo(@NonNull MapActivity a,@NonNull LatLon p,@Nullable String name){PointDescription pd=new PointDescription(PointDescription.POINT_TYPE_TARGET,name==null?"Destino":name);a.getApp().getTargetPointsHelper().removeAllWayPoints(false,true);a.getApp().getTargetPointsHelper().navigateToPoint(p,true,-1,pd);a.getMapActions().startNavigation();goToLocation(a);}
    public static void stopNavigation(@NonNull MapActivity a){a.getApp().stopNavigation();updateNavigationBanner(a);}
    public static void centerOn(@NonNull MapActivity a,@NonNull LatLon p){a.getMapViewTrackingUtilities().setMapLinkedToLocation(false);a.getMapView().setLatLon(p.getLatitude(),p.getLongitude());if(a.getMapView().getZoom()<15)a.getMapView().setIntZoom(15);a.getMapView().refreshMap();}
    public static void addFavoriteAt(@NonNull MapActivity a,@NonNull LatLon p,@Nullable String name){String n=(name==null||name.trim().isEmpty())?"Marcador Nuwe":name.trim();FavouritePoint f=new FavouritePoint(p.getLatitude(),p.getLongitude(),n,"Nuwe");f.setVisible(true);a.getApp().getFavoritesHelper().addFavourite(f);a.getSettings().SHOW_FAVORITES.set(true);a.getMapView().refreshMap();Toast.makeText(a,"Marcador salvo",Toast.LENGTH_SHORT).show();}
    public static void addFavoriteAtCenter(@NonNull MapActivity a,@Nullable String n){addFavoriteAt(a,a.getMapView().getCurrentRotatedTileBox().getCenterLatLon(),n);}
    public static void shareLocation(@NonNull MapActivity a,@Nullable LatLon explicit){LatLon p=explicit;if(p==null){Location l=a.getApp().getLocationProvider().getLastKnownLocation();if(l!=null)p=new LatLon(l.getLatitude(),l.getLongitude());}if(p==null){Toast.makeText(a,"Localização ainda não disponível",Toast.LENGTH_SHORT).show();return;}String text=String.format(Locale.US,"%.6f, %.6f",p.getLatitude(),p.getLongitude());Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,text);a.startActivity(Intent.createChooser(i,"Compartilhar localização"));}
    public static void refreshMap(@NonNull MapActivity a){a.getMapView().refreshMap(true);}
}
