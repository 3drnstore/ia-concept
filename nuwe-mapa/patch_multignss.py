#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path("android").resolve()
APP = ROOT / "OsmAnd"

monitor = r'''package net.osmand.plus.nuwemap;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GnssMeasurementRequest;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

/**
 * Nuwe Mapa Multi-GNSS monitor.
 *
 * The Android public API does not allow an app to force the GNSS chipset to
 * acquire one constellation before another. This class therefore does not
 * reject GPS or modify the GNSS fix. It observes all available constellations,
 * reports a preferred active constellation using the Nuwe order
 * GALILEO > BEIDOU > GLONASS > GPS, and requests full GNSS tracking on
 * Android 12+ when the device supports it.
 */
public final class MultiGnssMonitor {

    public static final int PRIORITY_GALILEO = 400;
    public static final int PRIORITY_BEIDOU = 300;
    public static final int PRIORITY_GLONASS = 200;
    public static final int PRIORITY_GPS = 100;

    private final Context context;
    private final LocationManager locationManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile Snapshot snapshot = Snapshot.empty();
    private boolean started;
    private boolean measurementsRegistered;

    public MultiGnssMonitor(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
    }

    public static final class Snapshot {
        public final int visibleGalileo;
        public final int usedGalileo;
        public final int visibleBeidou;
        public final int usedBeidou;
        public final int visibleGlonass;
        public final int usedGlonass;
        public final int visibleGps;
        public final int usedGps;
        public final int visibleOther;
        public final int usedOther;
        public final int preferredConstellation;

        Snapshot(int visibleGalileo, int usedGalileo,
                 int visibleBeidou, int usedBeidou,
                 int visibleGlonass, int usedGlonass,
                 int visibleGps, int usedGps,
                 int visibleOther, int usedOther,
                 int preferredConstellation) {
            this.visibleGalileo = visibleGalileo;
            this.usedGalileo = usedGalileo;
            this.visibleBeidou = visibleBeidou;
            this.usedBeidou = usedBeidou;
            this.visibleGlonass = visibleGlonass;
            this.usedGlonass = usedGlonass;
            this.visibleGps = visibleGps;
            this.usedGps = usedGps;
            this.visibleOther = visibleOther;
            this.usedOther = usedOther;
            this.preferredConstellation = preferredConstellation;
        }

        static Snapshot empty() {
            return new Snapshot(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, GnssStatus.CONSTELLATION_UNKNOWN);
        }

        public int getVisibleTotal() {
            return visibleGalileo + visibleBeidou + visibleGlonass + visibleGps + visibleOther;
        }

        public int getUsedTotal() {
            return usedGalileo + usedBeidou + usedGlonass + usedGps + usedOther;
        }
    }

    private final GnssStatus.Callback statusCallback = new GnssStatus.Callback() {
        @Override
        public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
            int vg = 0, ug = 0, vb = 0, ub = 0, vr = 0, ur = 0, vp = 0, up = 0, vo = 0, uo = 0;
            for (int i = 0; i < status.getSatelliteCount(); i++) {
                final boolean used = status.usedInFix(i);
                switch (status.getConstellationType(i)) {
                    case GnssStatus.CONSTELLATION_GALILEO:
                        vg++; if (used) ug++; break;
                    case GnssStatus.CONSTELLATION_BEIDOU:
                        vb++; if (used) ub++; break;
                    case GnssStatus.CONSTELLATION_GLONASS:
                        vr++; if (used) ur++; break;
                    case GnssStatus.CONSTELLATION_GPS:
                        vp++; if (used) up++; break;
                    default:
                        vo++; if (used) uo++; break;
                }
            }
            int preferred = choosePreferred(ug, ub, ur, up);
            snapshot = new Snapshot(vg, ug, vb, ub, vr, ur, vp, up, vo, uo, preferred);
        }
    };

    private final GnssMeasurementsEvent.Callback measurementCallback = new GnssMeasurementsEvent.Callback() {
        @Override
        public void onGnssMeasurementsReceived(@NonNull GnssMeasurementsEvent eventArgs) {
            // Full tracking is requested for receiver stability/measurement visibility.
            // The navigation fix itself remains owned by Android's GNSS stack.
        }
    };

    public void start() {
        if (started || locationManager == null || !hasFineLocation()) {
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Executor executor = context.getMainExecutor();
                locationManager.registerGnssStatusCallback(executor, statusCallback);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                locationManager.registerGnssStatusCallback(statusCallback, mainHandler);
            }
            requestFullTrackingIfSupported();
            started = true;
        } catch (SecurityException | IllegalArgumentException ignored) {
            started = false;
        }
    }

    private void requestFullTrackingIfSupported() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || locationManager == null || !hasFineLocation()) {
            return;
        }
        try {
            GnssMeasurementRequest request = new GnssMeasurementRequest.Builder()
                    .setFullTracking(true)
                    .build();
            measurementsRegistered = locationManager.registerGnssMeasurementsCallback(
                    request,
                    context.getMainExecutor(),
                    measurementCallback
            );
        } catch (SecurityException | IllegalArgumentException | UnsupportedOperationException ignored) {
            measurementsRegistered = false;
        }
    }

    public void stop() {
        if (locationManager == null) {
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                locationManager.unregisterGnssStatusCallback(statusCallback);
            }
            if (measurementsRegistered && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                locationManager.unregisterGnssMeasurementsCallback(measurementCallback);
            }
        } catch (RuntimeException ignored) {
            // Defensive: some vendor GNSS stacks throw during teardown.
        } finally {
            measurementsRegistered = false;
            started = false;
        }
    }

    public Snapshot getSnapshot() {
        return snapshot;
    }

    public boolean isFullTrackingActive() {
        return measurementsRegistered;
    }

    private boolean hasFineLocation() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static int choosePreferred(int galileoUsed, int beidouUsed, int glonassUsed, int gpsUsed) {
        if (galileoUsed > 0) return GnssStatus.CONSTELLATION_GALILEO;
        if (beidouUsed > 0) return GnssStatus.CONSTELLATION_BEIDOU;
        if (glonassUsed > 0) return GnssStatus.CONSTELLATION_GLONASS;
        if (gpsUsed > 0) return GnssStatus.CONSTELLATION_GPS;
        return GnssStatus.CONSTELLATION_UNKNOWN;
    }
}
'''

out = APP / "src/net/osmand/plus/nuwemap/MultiGnssMonitor.java"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(monitor, encoding="utf-8")

nav = APP / "src/net/osmand/plus/NavigationService.java"
s = nav.read_text(encoding="utf-8")

import_anchor = "import net.osmand.plus.notifications.OsmandNotification.NotificationType;\n"
if "net.osmand.plus.nuwemap.MultiGnssMonitor" not in s:
    if import_anchor not in s:
        raise RuntimeError("NavigationService import anchor not found")
    s = s.replace(import_anchor, import_anchor + "import net.osmand.plus.nuwemap.MultiGnssMonitor;\n", 1)

field_anchor = "\tprivate long lastTimeGPSLocationFixed;\n"
if "private MultiGnssMonitor multiGnssMonitor;" not in s:
    if field_anchor not in s:
        raise RuntimeError("NavigationService field anchor not found")
    s = s.replace(field_anchor, field_anchor + "\tprivate MultiGnssMonitor multiGnssMonitor;\n", 1)

oncreate_old = "\tpublic void onCreate() {\n\t\tsuper.onCreate();\n\t\taddLocationSourceListener();\n\t}\n"
oncreate_new = "\tpublic void onCreate() {\n\t\tsuper.onCreate();\n\t\taddLocationSourceListener();\n\t\tmultiGnssMonitor = new MultiGnssMonitor(this);\n\t\tmultiGnssMonitor.start();\n\t}\n"
if oncreate_old in s:
    s = s.replace(oncreate_old, oncreate_new, 1)
elif "multiGnssMonitor = new MultiGnssMonitor(this);" not in s:
    raise RuntimeError("NavigationService onCreate anchor not found")

ondestroy_anchor = "\tpublic void onDestroy() {\n\t\tsuper.onDestroy();\n"
if "multiGnssMonitor.stop();" not in s:
    if ondestroy_anchor not in s:
        raise RuntimeError("NavigationService onDestroy anchor not found")
    s = s.replace(
        ondestroy_anchor,
        ondestroy_anchor + "\t\tif (multiGnssMonitor != null) {\n\t\t\tmultiGnssMonitor.stop();\n\t\t\tmultiGnssMonitor = null;\n\t\t}\n",
        1,
    )

nav.write_text(s, encoding="utf-8")

notice = ROOT / "NUWE_MAPA_MULTIGNSS.txt"
notice.write_text(
    "Nuwe Mapa Multi-GNSS\n"
    "Preference/reporting order: Galileo > BeiDou > GLONASS > GPS.\n"
    "GPS is never disabled. Android remains responsible for the actual GNSS fix.\n"
    "On Android 12+ the app requests Full GNSS Tracking when supported.\n",
    encoding="utf-8",
)

print("Applied Nuwe Mapa Multi-GNSS patch")
