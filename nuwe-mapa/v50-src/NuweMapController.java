package net.osmand.plus.nuwe;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.activities.MapActivity;

import java.lang.ref.WeakReference;

/**
 * Integration boundary between the OsmAnd map engine and Nuwe's own UI.
 *
 * MapActivity talks only to this controller. The controller then exposes the
 * engine-backed operations used by the Nuwe views/sheets. No OsmAnd menu,
 * drawer, settings screen or context-menu UI is launched from here.
 */
public final class NuweMapController {
    private static WeakReference<MapActivity> activityRef = new WeakReference<>(null);

    private NuweMapController() {}

    public static void attach(@NonNull MapActivity activity) {
        if (!isNuwe(activity)) {
            return;
        }
        activityRef = new WeakReference<>(activity);
        activity.disableDrawer();
        NuweMapShell.bind(activity);
    }

    public static boolean isNuwe(@NonNull MapActivity activity) {
        return "app.nuwe.mapa".equals(activity.getPackageName());
    }

    @Nullable
    public static MapActivity getActivity() {
        return activityRef.get();
    }

    public static boolean onMapPoint(@NonNull MapActivity activity,
                                     @NonNull LatLon latLon,
                                     @Nullable PointDescription description,
                                     @Nullable Object object) {
        if (!isNuwe(activity)) {
            return false;
        }
        return NuweMapShell.handleContextPoint(activity, latLon, description, object);
    }

    public static void openLauncher(@NonNull MapActivity activity) {
        if (isNuwe(activity)) {
            NuweLauncher.show(activity);
        }
    }
}
