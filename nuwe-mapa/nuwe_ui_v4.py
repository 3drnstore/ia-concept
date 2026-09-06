#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path("android").resolve()
APP = ROOT / "OsmAnd"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def bump_version() -> None:
    p = APP / "build.gradle"
    s = read(p)
    s = re.sub(r'versionName\s+"0\.3\.1"', 'versionName "0.4.0"', s)
    write(p, s)


def create_drawables() -> None:
    panel = '''<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <gradient android:angle="270" android:startColor="#E838C8E5" android:centerColor="#E31A7696" android:endColor="#EE082D47" />
            <corners android:radius="30dp" />
            <stroke android:width="1dp" android:color="#C6DDFBFF" />
            <padding android:left="18dp" android:top="16dp" android:right="18dp" android:bottom="16dp" />
        </shape>
    </item>
    <item android:gravity="top" android:height="18dp" android:left="8dp" android:right="8dp" android:top="2dp">
        <shape android:shape="rectangle">
            <gradient android:angle="270" android:startColor="#86FFFFFF" android:endColor="#12FFFFFF" />
            <corners android:radius="24dp" />
        </shape>
    </item>
</layer-list>
'''
    orb = '''<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="oval">
            <gradient android:angle="270" android:startColor="#F37EF6FF" android:centerColor="#EC19B8DB" android:endColor="#F0076B98" />
            <stroke android:width="2dp" android:color="#E9ECFFFF" />
            <size android:width="76dp" android:height="76dp" />
        </shape>
    </item>
    <item android:gravity="top|center_horizontal" android:width="54dp" android:height="25dp" android:top="5dp">
        <shape android:shape="oval"><solid android:color="#7AFFFFFF" /></shape>
    </item>
</layer-list>
'''
    fab = '''<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <gradient android:angle="270" android:startColor="#E45BE3F4" android:centerColor="#E02191B2" android:endColor="#E00B4E6D" />
            <corners android:radius="20dp" />
            <stroke android:width="1dp" android:color="#B8E7FBFF" />
        </shape>
    </item>
    <item android:gravity="top" android:height="12dp" android:left="5dp" android:right="5dp" android:top="3dp">
        <shape android:shape="rectangle"><solid android:color="#4CFFFFFF" /><corners android:radius="10dp" /></shape>
    </item>
</layer-list>
'''
    search = '''<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <gradient android:angle="0" android:startColor="#E926B6D0" android:centerColor="#E0137C9E" android:endColor="#E20B405E" />
            <corners android:radius="28dp" />
            <stroke android:width="1dp" android:color="#C8E9FDFF" />
        </shape>
    </item>
    <item android:gravity="top" android:height="17dp" android:left="10dp" android:right="10dp" android:top="2dp">
        <shape android:shape="rectangle"><gradient android:angle="270" android:startColor="#68FFFFFF" android:endColor="#08FFFFFF" /><corners android:radius="22dp" /></shape>
    </item>
</layer-list>
'''
    chip = '''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#4815A2BD" />
    <corners android:radius="999dp" />
    <stroke android:width="1dp" android:color="#78D7F8FF" />
    <padding android:left="12dp" android:top="8dp" android:right="12dp" android:bottom="8dp" />
</shape>
'''
    chip_active = '''<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <gradient android:angle="270" android:startColor="#FFE9FFFF" android:centerColor="#FF72EDF5" android:endColor="#FF16A9C7" />
            <corners android:radius="999dp" />
            <stroke android:width="1dp" android:color="#FFFFFFFF" />
            <padding android:left="12dp" android:top="8dp" android:right="12dp" android:bottom="8dp" />
        </shape>
    </item>
</layer-list>
'''
    tile = '''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient android:angle="270" android:startColor="#5AF7FFFF" android:endColor="#2514A0C1" />
    <corners android:radius="18dp" />
    <stroke android:width="1dp" android:color="#72DDF9FF" />
    <padding android:left="8dp" android:top="10dp" android:right="8dp" android:bottom="10dp" />
</shape>
'''
    write(APP / "res/drawable/nuwe2000_panel.xml", panel)
    write(APP / "res/drawable/nuwe2000_orb.xml", orb)
    write(APP / "res/drawable/nuwe2000_fab.xml", fab)
    write(APP / "res/drawable/nuwe2000_search.xml", search)
    write(APP / "res/drawable/nuwe2000_chip.xml", chip)
    write(APP / "res/drawable/nuwe2000_chip_active.xml", chip_active)
    write(APP / "res/drawable/nuwe2000_tile.xml", tile)

    icons = {
        "nuwe_ic_plus": "M11,5h2v6h6v2h-6v6h-2v-6H5v-2h6z",
        "nuwe_ic_minus": "M5,11h14v2H5z",
        "nuwe_ic_navigate": "M12,2l8,20l-8,-5l-8,5z",
    }
    for name, data in icons.items():
        write(APP / f"res/drawable/{name}.xml", f'''<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">\n    <path android:fillColor="#FFFFFFFF" android:pathData="{data}"/>\n</vector>\n''')


def patch_map_hud() -> None:
    p = APP / "res/layout/map_hud_layout.xml"
    s = read(p)
    if 'nuwe_launcher_orb' in s:
        return
    anchor = '\n\t</net.osmand.plus.views.controls.MapHudLayout>'
    overlay = '''

        <!-- Nuwe Mapa 0.4 central launcher. The OsmAnd engine stays underneath; this is the user-facing shell. -->
        <LinearLayout
            android:id="@+id/nuwe_search_bar"
            android:layout_width="match_parent"
            android:layout_height="54dp"
            android:layout_gravity="top|center_horizontal"
            android:layout_marginStart="18dp"
            android:layout_marginTop="14dp"
            android:layout_marginEnd="18dp"
            android:background="@drawable/nuwe2000_search"
            android:clickable="true"
            android:elevation="18dp"
            android:focusable="true"
            android:gravity="center_vertical"
            android:orientation="horizontal"
            android:paddingStart="16dp"
            android:paddingEnd="16dp">

            <ImageView
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:src="@drawable/nuwe_ic_search"
                android:tint="#FFFFFFFF" />

            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginStart="12dp"
                android:layout_weight="1"
                android:text="Onde vamos?"
                android:textColor="#FFFFFFFF"
                android:textSize="16sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/nuwe_mode_badge"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:background="@drawable/nuwe2000_chip"
                android:text="CARRO"
                android:textColor="#DFFFFFFF"
                android:textSize="10sp"
                android:textStyle="bold" />
        </LinearLayout>

        <LinearLayout
            android:id="@+id/nuwe_right_rail"
            android:layout_width="56dp"
            android:layout_height="wrap_content"
            android:layout_gravity="end|center_vertical"
            android:layout_marginEnd="14dp"
            android:elevation="16dp"
            android:gravity="center"
            android:orientation="vertical">

            <ImageButton
                android:id="@+id/nuwe_location_btn"
                android:layout_width="54dp"
                android:layout_height="54dp"
                android:background="@drawable/nuwe2000_fab"
                android:contentDescription="Minha localização"
                android:padding="14dp"
                android:src="@drawable/nuwe_ic_location"
                android:tint="#FFFFFFFF" />

            <ImageButton
                android:id="@+id/nuwe_layers_btn"
                android:layout_width="54dp"
                android:layout_height="54dp"
                android:layout_marginTop="8dp"
                android:background="@drawable/nuwe2000_fab"
                android:contentDescription="Camadas"
                android:padding="14dp"
                android:src="@drawable/nuwe_ic_layers"
                android:tint="#FFFFFFFF" />

            <ImageButton
                android:id="@+id/nuwe_zoom_in_btn"
                android:layout_width="54dp"
                android:layout_height="54dp"
                android:layout_marginTop="8dp"
                android:background="@drawable/nuwe2000_fab"
                android:contentDescription="Aproximar"
                android:padding="15dp"
                android:src="@drawable/nuwe_ic_plus"
                android:tint="#FFFFFFFF" />

            <ImageButton
                android:id="@+id/nuwe_zoom_out_btn"
                android:layout_width="54dp"
                android:layout_height="54dp"
                android:layout_marginTop="8dp"
                android:background="@drawable/nuwe2000_fab"
                android:contentDescription="Afastar"
                android:padding="15dp"
                android:src="@drawable/nuwe_ic_minus"
                android:tint="#FFFFFFFF" />
        </LinearLayout>

        <LinearLayout
            android:id="@+id/nuwe_launcher_panel"
            android:layout_width="336dp"
            android:layout_height="wrap_content"
            android:layout_gravity="bottom|center_horizontal"
            android:layout_marginBottom="112dp"
            android:background="@drawable/nuwe2000_panel"
            android:elevation="24dp"
            android:orientation="vertical"
            android:visibility="gone">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center"
                android:letterSpacing="0.12"
                android:text="NUWE MAPA"
                android:textColor="#FFFFFFFF"
                android:textSize="18sp"
                android:textStyle="bold" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="2dp"
                android:gravity="center"
                android:text="offline navigation console"
                android:textColor="#BDEBFFFF"
                android:textSize="11sp" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="14dp"
                android:gravity="center"
                android:orientation="horizontal">

                <TextView
                    android:id="@+id/nuwe_profile_car"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:background="@drawable/nuwe2000_chip_active"
                    android:clickable="true"
                    android:focusable="true"
                    android:text="CARRO"
                    android:textColor="#073A4B"
                    android:textSize="12sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/nuwe_profile_bike"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="8dp"
                    android:background="@drawable/nuwe2000_chip"
                    android:clickable="true"
                    android:focusable="true"
                    android:text="BIKE"
                    android:textColor="#FFFFFFFF"
                    android:textSize="12sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/nuwe_profile_trail"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="8dp"
                    android:background="@drawable/nuwe2000_chip"
                    android:clickable="true"
                    android:focusable="true"
                    android:text="TRILHA"
                    android:textColor="#FFFFFFFF"
                    android:textSize="12sp"
                    android:textStyle="bold" />
            </LinearLayout>

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="14dp"
                android:gravity="center"
                android:orientation="horizontal">

                <LinearLayout
                    android:id="@+id/nuwe_action_search"
                    android:layout_width="0dp"
                    android:layout_height="76dp"
                    android:layout_weight="1"
                    android:background="@drawable/nuwe2000_tile"
                    android:clickable="true"
                    android:focusable="true"
                    android:gravity="center"
                    android:orientation="vertical">
                    <ImageView android:layout_width="26dp" android:layout_height="26dp" android:src="@drawable/nuwe_ic_search" android:tint="#FFFFFFFF" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="5dp" android:text="Buscar" android:textColor="#FFFFFFFF" android:textSize="11sp" android:textStyle="bold" />
                </LinearLayout>

                <LinearLayout
                    android:id="@+id/nuwe_action_route"
                    android:layout_width="0dp"
                    android:layout_height="76dp"
                    android:layout_marginStart="7dp"
                    android:layout_weight="1"
                    android:background="@drawable/nuwe2000_tile"
                    android:clickable="true"
                    android:focusable="true"
                    android:gravity="center"
                    android:orientation="vertical">
                    <ImageView android:layout_width="26dp" android:layout_height="26dp" android:src="@drawable/nuwe_ic_navigate" android:tint="#FFFFFFFF" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="5dp" android:text="Rota" android:textColor="#FFFFFFFF" android:textSize="11sp" android:textStyle="bold" />
                </LinearLayout>

                <LinearLayout
                    android:id="@+id/nuwe_action_layers"
                    android:layout_width="0dp"
                    android:layout_height="76dp"
                    android:layout_marginStart="7dp"
                    android:layout_weight="1"
                    android:background="@drawable/nuwe2000_tile"
                    android:clickable="true"
                    android:focusable="true"
                    android:gravity="center"
                    android:orientation="vertical">
                    <ImageView android:layout_width="26dp" android:layout_height="26dp" android:src="@drawable/nuwe_ic_layers" android:tint="#FFFFFFFF" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="5dp" android:text="Camadas" android:textColor="#FFFFFFFF" android:textSize="11sp" android:textStyle="bold" />
                </LinearLayout>

                <LinearLayout
                    android:id="@+id/nuwe_action_settings"
                    android:layout_width="0dp"
                    android:layout_height="76dp"
                    android:layout_marginStart="7dp"
                    android:layout_weight="1"
                    android:background="@drawable/nuwe2000_tile"
                    android:clickable="true"
                    android:focusable="true"
                    android:gravity="center"
                    android:orientation="vertical">
                    <ImageView android:layout_width="26dp" android:layout_height="26dp" android:src="@drawable/nuwe_ic_settings" android:tint="#FFFFFFFF" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="5dp" android:text="Ajustes" android:textColor="#FFFFFFFF" android:textSize="11sp" android:textStyle="bold" />
                </LinearLayout>
            </LinearLayout>
        </LinearLayout>

        <ImageButton
            android:id="@+id/nuwe_launcher_orb"
            android:layout_width="78dp"
            android:layout_height="78dp"
            android:layout_gravity="bottom|center_horizontal"
            android:layout_marginBottom="22dp"
            android:background="@drawable/nuwe2000_orb"
            android:contentDescription="Abrir Nuwe"
            android:elevation="30dp"
            android:padding="10dp"
            android:scaleType="centerInside"
            android:src="@mipmap/nuwe_launcher" />
'''
    if anchor not in s:
        raise RuntimeError("MapHudLayout closing anchor not found")
    s = s.replace(anchor, overlay + anchor, 1)
    write(p, s)


def create_controller() -> None:
    code = r'''package net.osmand.plus.nuwe;

import android.graphics.Color;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.search.ShowQuickSearchMode;
import net.osmand.plus.settings.backend.ApplicationMode;

public final class NuweLauncherController {

    private NuweLauncherController() {
    }

    public static void bind(@NonNull MapActivity activity) {
        View orb = activity.findViewById(R.id.nuwe_launcher_orb);
        View panel = activity.findViewById(R.id.nuwe_launcher_panel);
        if (orb == null || panel == null) {
            return;
        }

        activity.disableDrawer();
        forceHideLegacyControls(activity);
        activity.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        forceHideLegacyControls(activity);
                    }
                });

        orb.setOnClickListener(v -> togglePanel(panel));
        activity.findViewById(R.id.nuwe_search_bar).setOnClickListener(v -> {
            closePanel(panel);
            activity.getFragmentsHelper().showQuickSearch(ShowQuickSearchMode.NEW_IF_EXPIRED, false);
        });

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
            ConfigureMapFragment.showInstance(activity.getSupportFragmentManager());
        });
        activity.findViewById(R.id.nuwe_action_settings).setOnClickListener(v -> {
            closePanel(panel);
            activity.getFragmentsHelper().showSettings();
        });

        activity.findViewById(R.id.nuwe_location_btn).setOnClickListener(v -> clickLegacy(activity, R.id.map_my_location_button));
        activity.findViewById(R.id.nuwe_layers_btn).setOnClickListener(v -> ConfigureMapFragment.showInstance(activity.getSupportFragmentManager()));
        activity.findViewById(R.id.nuwe_zoom_in_btn).setOnClickListener(v -> clickLegacy(activity, R.id.map_zoom_in_button));
        activity.findViewById(R.id.nuwe_zoom_out_btn).setOnClickListener(v -> clickLegacy(activity, R.id.map_zoom_out_button));

        activity.findViewById(R.id.nuwe_profile_car).setOnClickListener(v -> setMode(activity, ApplicationMode.CAR));
        activity.findViewById(R.id.nuwe_profile_bike).setOnClickListener(v -> setMode(activity, ApplicationMode.BICYCLE));
        activity.findViewById(R.id.nuwe_profile_trail).setOnClickListener(v -> setMode(activity, ApplicationMode.PEDESTRIAN));

        updateModeUi(activity);
    }

    private static void togglePanel(@NonNull View panel) {
        if (panel.getVisibility() == View.VISIBLE) {
            closePanel(panel);
        } else {
            panel.setAlpha(0f);
            panel.setScaleX(0.88f);
            panel.setScaleY(0.88f);
            panel.setTranslationY(28f);
            panel.setVisibility(View.VISIBLE);
            panel.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f).setDuration(190).start();
        }
    }

    private static void closePanel(@NonNull View panel) {
        if (panel.getVisibility() != View.VISIBLE) {
            return;
        }
        panel.animate().alpha(0f).scaleX(0.9f).scaleY(0.9f).translationY(20f).setDuration(140)
                .withEndAction(() -> panel.setVisibility(View.GONE)).start();
    }

    private static void clickLegacy(@NonNull MapActivity activity, int id) {
        View target = activity.findViewById(id);
        if (target != null) {
            target.performClick();
        }
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
        styleChip(car, isCar);
        styleChip(bike, isBike);
        styleChip(trail, isTrail);
        if (badge != null) {
            badge.setText(isCar ? "CARRO" : isBike ? "BIKE" : "TRILHA");
        }
    }

    private static void styleChip(TextView chip, boolean active) {
        if (chip == null) {
            return;
        }
        chip.setBackgroundResource(active ? R.drawable.nuwe2000_chip_active : R.drawable.nuwe2000_chip);
        chip.setTextColor(active ? Color.rgb(7, 58, 75) : Color.WHITE);
    }

    private static void forceHideLegacyControls(@NonNull MapActivity activity) {
        int[] legacy = new int[] {
                R.id.map_menu_button,
                R.id.map_search_button,
                R.id.map_layers_button,
                R.id.map_quick_actions_button,
                R.id.map_route_info_button,
                R.id.map_compass_button,
                R.id.map_zoom_in_button,
                R.id.map_zoom_out_button,
                R.id.map_my_location_button
        };
        for (int id : legacy) {
            View view = activity.findViewById(id);
            if (view != null && view.getVisibility() != View.GONE) {
                view.setVisibility(View.GONE);
            }
        }
    }
}
'''
    write(APP / "src/net/osmand/plus/nuwe/NuweLauncherController.java", code)


def patch_map_activity() -> None:
    p = APP / "src/net/osmand/plus/activities/MapActivity.java"
    s = read(p)
    marker = '\t\tgetMapLayers().createAdditionalLayers(this);\n'
    replacement = marker + '\n\t\t// Nuwe Mapa user-facing shell: central launcher + Fruit Aero 2000 HUD.\n\t\tnet.osmand.plus.nuwe.NuweLauncherController.bind(this);\n'
    if 'NuweLauncherController.bind(this)' not in s:
        if marker not in s:
            raise RuntimeError("MapActivity createAdditionalLayers anchor not found")
        s = s.replace(marker, replacement, 1)
    write(p, s)


def write_notice() -> None:
    write(ROOT / "NUWE_UI_V4_NOTICE.txt", """Nuwe Mapa 0.4.0 - Fruit Aero 2000 structural redesign\n\nImplemented:\n- new central launcher orb as the primary navigation control\n- new expandable glass console instead of the OsmAnd drawer/menu flow\n- new top search capsule\n- new right-side glass rail for location, layers and zoom\n- old OsmAnd map buttons are hidden persistently on the map screen\n- drawer is disabled on the main map screen\n- quick profile switcher limited to Carro, Bike and Trilha\n- central actions: Buscar, Rota, Camadas and Ajustes\n- glossy/translucent Fruit Aero 2000 surfaces and animations\n- OsmAnd remains only as the underlying offline map/routing engine\n\nApplication ID and stable signing policy from 0.3.1 are preserved.\n""")


def main() -> None:
    bump_version()
    create_drawables()
    patch_map_hud()
    create_controller()
    patch_map_activity()
    write_notice()
    print("Applied Nuwe Mapa 0.4.0 Fruit Aero 2000 central launcher redesign")


if __name__ == "__main__":
    main()
