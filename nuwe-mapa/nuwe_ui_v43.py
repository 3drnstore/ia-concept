#!/usr/bin/env python3
from pathlib import Path
import re
import sys

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
    s = re.sub(r'versionName\s+"0\.4\.2"', 'versionName "0.4.3"', s)
    write(p, s)


def add_drawables() -> None:
    vectors = {
        "nuwe43_search": "M9.5,3a6.5,6.5 0,1 0,0 13a6.5,6.5 0,0 0,0 -13M14.2,14.2L21,21",
        "nuwe43_route": "M5,19C7,13 10,13 12,10C14,7 14,5 19,5M16,2L20,5L16,8",
        "nuwe43_layers": "M12,3L3,8l9,5l9,-5zM3,12l9,5l9,-5M3,16l9,5l9,-5",
        "nuwe43_settings": "M12,8a4,4 0,1 0,0 8a4,4 0,0 0,0 -8M12,2v3M12,19v3M2,12h3M19,12h3M4.9,4.9l2.1,2.1M17,17l2.1,2.1M19.1,4.9L17,7M7,17l-2.1,2.1",
        "nuwe43_location": "M12,2l7,19l-7,-4l-7,4zM12,7v8",
        "nuwe43_back": "M20,11H7l5,-5l-1.4,-1.4L3.2,12l7.4,7.4L12,18l-5,-5h13z",
        "nuwe43_map": "M3,5l6,-2l6,2l6,-2v16l-6,2l-6,-2l-6,2zM9,3v16M15,5v16",
        "nuwe43_sat": "M12,3l3,3l-3,3l-3,-3zM8,10l6,6M15,11l4,-4M5,19c4,-1 8,-5 9,-9",
        "nuwe43_voice": "M5,10v4h4l5,4V6l-5,4zM17,9c2,2 2,4 0,6M19,7c4,4 4,8 0,12",
        "nuwe43_data": "M5,3h14v18H5zM8,7h8M8,11h8M8,15h5",
        "nuwe43_info": "M12,3a9,9 0,1 0,0 18a9,9 0,0 0,0 -18M12,10v6M12,7h.01",
    }
    for name, data in vectors.items():
        xml = f'''<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">\n    <path android:fillColor="@android:color/transparent" android:strokeColor="#FFFFFFFF" android:strokeWidth="1.8" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="{data}"/>\n</vector>\n'''
        write(APP / f"res/drawable/{name}.xml", xml)

    settings_bg = '''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient android:angle="270" android:startColor="#F2E8FBFF" android:centerColor="#F1D9F6FB" android:endColor="#F1C5EDF4" />
</shape>
'''
    settings_card = '''<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item><shape android:shape="rectangle"><gradient android:angle="270" android:startColor="#D9FFFFFF" android:endColor="#B8D7F4F8"/><corners android:radius="24dp"/><stroke android:width="1dp" android:color="#AFFFFFFF"/></shape></item>
    <item android:gravity="top" android:height="16dp" android:left="8dp" android:right="8dp" android:top="3dp"><shape android:shape="rectangle"><solid android:color="#55FFFFFF"/><corners android:radius="18dp"/></shape></item>
</layer-list>
'''
    write(APP / "res/drawable/nuwe43_settings_bg.xml", settings_bg)
    write(APP / "res/drawable/nuwe43_settings_card.xml", settings_card)


def patch_layout() -> None:
    p = APP / "res/layout/map_hud_layout.xml"
    s = read(p)
    # Search should be launcher-only, not permanently covering the map.
    s = s.replace('android:id="@+id/nuwe_search_bar"\n            android:layout_width="match_parent"', 'android:id="@+id/nuwe_search_bar"\n            android:visibility="gone"\n            android:layout_width="match_parent"', 1)

    # Replace visible launcher icons with a new custom vector set.
    replacements = {
        '@drawable/nuwe_ic_search': '@drawable/nuwe43_search',
        '@drawable/nuwe_ic_layers': '@drawable/nuwe43_layers',
        '@drawable/nuwe_ic_navigate': '@drawable/nuwe43_route',
        '@drawable/nuwe_ic_settings': '@drawable/nuwe43_settings',
        '@drawable/nuwe_ic_location': '@drawable/nuwe43_location',
    }
    for a, b in replacements.items():
        s = s.replace(a, b)

    if 'nuwe_settings_overlay' not in s:
        anchor = '''        <ImageButton\n            android:id="@+id/nuwe_launcher_orb"'''
        overlay = '''        <ScrollView
            android:id="@+id/nuwe_settings_overlay"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:layout_gravity="fill"
            android:background="@drawable/nuwe43_settings_bg"
            android:elevation="60dp"
            android:fillViewport="true"
            android:visibility="gone">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:paddingStart="22dp"
                android:paddingTop="18dp"
                android:paddingEnd="22dp"
                android:paddingBottom="32dp">

                <LinearLayout android:layout_width="match_parent" android:layout_height="56dp" android:gravity="center_vertical" android:orientation="horizontal">
                    <ImageButton android:id="@+id/nuwe_settings_back" android:layout_width="48dp" android:layout_height="48dp" android:background="@drawable/nuwe2000_fab" android:padding="12dp" android:src="@drawable/nuwe43_back" />
                    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_marginStart="14dp" android:layout_weight="1" android:orientation="vertical">
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Ajustes Nuwe" android:textColor="#073A4B" android:textSize="24sp" android:textStyle="bold" />
                        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="somente o que importa para navegar" android:textColor="#5A557780" android:textSize="12sp" />
                    </LinearLayout>
                </LinearLayout>

                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="22dp" android:text="PERFIL DE NAVEGAÇÃO" android:textColor="#1487A3" android:textSize="12sp" android:textStyle="bold" />
                <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="10dp" android:gravity="center" android:orientation="horizontal">
                    <TextView android:id="@+id/nuwe_settings_car" android:layout_width="0dp" android:layout_height="48dp" android:layout_weight="1" android:background="@drawable/nuwe2000_chip" android:gravity="center" android:text="CARRO" android:textColor="#073A4B" android:textStyle="bold" />
                    <TextView android:id="@+id/nuwe_settings_bike" android:layout_width="0dp" android:layout_height="48dp" android:layout_marginStart="8dp" android:layout_weight="1" android:background="@drawable/nuwe2000_chip" android:gravity="center" android:text="BIKE" android:textColor="#073A4B" android:textStyle="bold" />
                    <TextView android:id="@+id/nuwe_settings_trail" android:layout_width="0dp" android:layout_height="48dp" android:layout_marginStart="8dp" android:layout_weight="1" android:background="@drawable/nuwe2000_chip" android:gravity="center" android:text="TRILHA" android:textColor="#073A4B" android:textStyle="bold" />
                </LinearLayout>

                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="24dp" android:text="MAPA E SISTEMA" android:textColor="#1487A3" android:textSize="12sp" android:textStyle="bold" />

                <LinearLayout android:id="@+id/nuwe_settings_map" android:layout_width="match_parent" android:layout_height="78dp" android:layout_marginTop="10dp" android:background="@drawable/nuwe43_settings_card" android:clickable="true" android:focusable="true" android:gravity="center_vertical" android:orientation="horizontal" android:paddingStart="18dp" android:paddingEnd="18dp">
                    <ImageView android:layout_width="34dp" android:layout_height="34dp" android:src="@drawable/nuwe43_map" android:tint="#1487A3" />
                    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_marginStart="16dp" android:layout_weight="1" android:orientation="vertical"><TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Mapa e camadas" android:textColor="#123F4C" android:textSize="17sp" android:textStyle="bold"/><TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="estilo, relevo e conteúdo visível" android:textColor="#667A80" android:textSize="12sp"/></LinearLayout>
                </LinearLayout>

                <LinearLayout android:id="@+id/nuwe_settings_gnss" android:layout_width="match_parent" android:layout_height="78dp" android:layout_marginTop="10dp" android:background="@drawable/nuwe43_settings_card" android:clickable="true" android:focusable="true" android:gravity="center_vertical" android:orientation="horizontal" android:paddingStart="18dp" android:paddingEnd="18dp">
                    <ImageView android:layout_width="34dp" android:layout_height="34dp" android:src="@drawable/nuwe43_sat" android:tint="#1487A3" />
                    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_marginStart="16dp" android:layout_weight="1" android:orientation="vertical"><TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="GNSS e localização" android:textColor="#123F4C" android:textSize="17sp" android:textStyle="bold"/><TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Galileo, BeiDou, GLONASS e GPS" android:textColor="#667A80" android:textSize="12sp"/></LinearLayout>
                </LinearLayout>

                <LinearLayout android:id="@+id/nuwe_settings_voice" android:layout_width="match_parent" android:layout_height="78dp" android:layout_marginTop="10dp" android:background="@drawable/nuwe43_settings_card" android:clickable="true" android:focusable="true" android:gravity="center_vertical" android:orientation="horizontal" android:paddingStart="18dp" android:paddingEnd="18dp">
                    <ImageView android:layout_width="34dp" android:layout_height="34dp" android:src="@drawable/nuwe43_voice" android:tint="#1487A3" />
                    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_marginStart="16dp" android:layout_weight="1" android:orientation="vertical"><TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Voz offline" android:textColor="#123F4C" android:textSize="17sp" android:textStyle="bold"/><TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="configurar mecanismo TTS do aparelho" android:textColor="#667A80" android:textSize="12sp"/></LinearLayout>
                </LinearLayout>

                <LinearLayout android:id="@+id/nuwe_settings_data" android:layout_width="match_parent" android:layout_height="78dp" android:layout_marginTop="10dp" android:background="@drawable/nuwe43_settings_card" android:gravity="center_vertical" android:orientation="horizontal" android:paddingStart="18dp" android:paddingEnd="18dp">
                    <ImageView android:layout_width="34dp" android:layout_height="34dp" android:src="@drawable/nuwe43_data" android:tint="#1487A3" />
                    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_marginStart="16dp" android:layout_weight="1" android:orientation="vertical"><TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Dados locais" android:textColor="#123F4C" android:textSize="17sp" android:textStyle="bold"/><TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="GPX, favoritos e arquivos ficam no aparelho" android:textColor="#667A80" android:textSize="12sp"/></LinearLayout>
                </LinearLayout>

                <LinearLayout android:id="@+id/nuwe_settings_about" android:layout_width="match_parent" android:layout_height="78dp" android:layout_marginTop="10dp" android:background="@drawable/nuwe43_settings_card" android:gravity="center_vertical" android:orientation="horizontal" android:paddingStart="18dp" android:paddingEnd="18dp">
                    <ImageView android:layout_width="34dp" android:layout_height="34dp" android:src="@drawable/nuwe43_info" android:tint="#1487A3" />
                    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_marginStart="16dp" android:layout_weight="1" android:orientation="vertical"><TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Nuwe Mapa 0.4.3" android:textColor="#123F4C" android:textSize="17sp" android:textStyle="bold"/><TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="navegação offline · Fruit Aero 2000" android:textColor="#667A80" android:textSize="12sp"/></LinearLayout>
                </LinearLayout>
            </LinearLayout>
        </ScrollView>

'''
        if anchor not in s:
            raise RuntimeError("launcher orb anchor not found")
        s = s.replace(anchor, overlay + anchor, 1)
    write(p, s)


def patch_controller() -> None:
    p = APP / "src/net/osmand/plus/nuwe/NuweLauncherController.java"
    s = read(p)
    if 'android.content.Intent;' not in s:
        s = s.replace('import android.graphics.Color;\n', 'import android.graphics.Color;\nimport android.content.Intent;\nimport android.provider.Settings;\nimport android.widget.Toast;\n')

    # No persistent top search UI.
    s = s.replace('activity.findViewById(R.id.nuwe_search_bar).setOnClickListener(v -> {\n            closePanel(panel);\n            activity.getFragmentsHelper().showQuickSearch(ShowQuickSearchMode.NEW_IF_EXPIRED, false);\n        });', 'View searchBar = activity.findViewById(R.id.nuwe_search_bar);\n        if (searchBar != null) { searchBar.setVisibility(View.GONE); }')

    # Layers now delegate to the already wired native layers button rather than opening a fragment directly.
    s = s.replace('ConfigureMapFragment.showInstance(activity.getSupportFragmentManager());', 'clickLegacy(activity, R.id.map_layers_button);')

    old_settings = '''activity.findViewById(R.id.nuwe_action_settings).setOnClickListener(v -> {\n            closePanel(panel);\n            activity.getFragmentsHelper().showSettings();\n        });'''
    new_settings = '''activity.findViewById(R.id.nuwe_action_settings).setOnClickListener(v -> {\n            closePanel(panel);\n            View settingsOverlay = activity.findViewById(R.id.nuwe_settings_overlay);\n            if (settingsOverlay != null) settingsOverlay.setVisibility(View.VISIBLE);\n        });'''
    if old_settings in s:
        s = s.replace(old_settings, new_settings)

    insert_after = 'activity.findViewById(R.id.nuwe_profile_trail).setOnClickListener(v -> setMode(activity, ApplicationMode.PEDESTRIAN));\n'
    if 'nuwe_settings_back' not in s:
        extra = '''\n        activity.findViewById(R.id.nuwe_settings_back).setOnClickListener(v -> activity.findViewById(R.id.nuwe_settings_overlay).setVisibility(View.GONE));\n        activity.findViewById(R.id.nuwe_settings_car).setOnClickListener(v -> setMode(activity, ApplicationMode.CAR));\n        activity.findViewById(R.id.nuwe_settings_bike).setOnClickListener(v -> setMode(activity, ApplicationMode.BICYCLE));\n        activity.findViewById(R.id.nuwe_settings_trail).setOnClickListener(v -> setMode(activity, ApplicationMode.PEDESTRIAN));\n        activity.findViewById(R.id.nuwe_settings_map).setOnClickListener(v -> {\n            activity.findViewById(R.id.nuwe_settings_overlay).setVisibility(View.GONE);\n            clickLegacy(activity, R.id.map_layers_button);\n        });\n        activity.findViewById(R.id.nuwe_settings_gnss).setOnClickListener(v -> {\n            try { activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)); }\n            catch (Exception e) { activity.startActivity(new Intent(Settings.ACTION_SETTINGS)); }\n        });\n        activity.findViewById(R.id.nuwe_settings_voice).setOnClickListener(v -> {\n            try { activity.startActivity(new Intent("com.android.settings.TTS_SETTINGS")); }\n            catch (Exception e) { activity.startActivity(new Intent(Settings.ACTION_SETTINGS)); }\n        });\n'''
        if insert_after not in s:
            raise RuntimeError("profile listener anchor not found")
        s = s.replace(insert_after, insert_after + extra, 1)

    # Stop the OsmAnd buttons from flickering back in. Keep them logically alive at alpha 0
    # so the Nuwe controls can still delegate to their existing click handlers.
    old_force = '''if (view != null && view.getVisibility() != View.GONE) {\n                view.setVisibility(View.GONE);\n            }'''
    new_force = '''if (view != null) {\n                view.setAlpha(0f);\n            }'''
    s = s.replace(old_force, new_force)

    # Force zero alpha on every pre-draw; this prevents one-frame flashes while the map HUD refreshes.
    listener_anchor = '''activity.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(\n                new ViewTreeObserver.OnGlobalLayoutListener() {\n                    @Override\n                    public void onGlobalLayout() {\n                        forceHideLegacyControls(activity);\n                    }\n                });\n'''
    if 'addOnPreDrawListener' not in s and listener_anchor in s:
        pre = listener_anchor + '''        activity.getWindow().getDecorView().getViewTreeObserver().addOnPreDrawListener(() -> {\n            forceHideLegacyControls(activity);\n            return true;\n        });\n'''
        s = s.replace(listener_anchor, pre, 1)

    # Keep settings chips in sync too.
    update_anchor = 'styleChip(trail, isTrail);\n'
    if 'nuwe_settings_car' not in s[s.find('private static void updateModeUi'):]:
        add = '''styleChip(trail, isTrail);\n        styleChip(activity.findViewById(R.id.nuwe_settings_car), isCar);\n        styleChip(activity.findViewById(R.id.nuwe_settings_bike), isBike);\n        styleChip(activity.findViewById(R.id.nuwe_settings_trail), isTrail);\n'''
        s = s.replace(update_anchor, add, 1)

    write(p, s)


def write_notice() -> None:
    write(ROOT / "NUWE_UI_V43_NOTICE.txt", """Nuwe Mapa 0.4.3\n\nUI refinement based on device testing:\n- removed the permanent top search bar from the map\n- search remains available from the central Nuwe launcher\n- legacy OsmAnd HUD controls are forced transparent every frame to stop flashing/reappearing\n- layers buttons delegate to the proven native layers control\n- completely new Fruit Aero settings overlay replaces the OsmAnd settings home screen\n- settings only exposes Carro, Bike and Trilha plus map, GNSS, voice and local-data sections\n- removed Cloud, purchases, public transport, truck, motorcycle, train, boat, aircraft, ski and horse profiles from the visible settings UX\n- new custom vector icon family for search, route, layers, settings, location and settings categories\n""")


def main() -> None:
    bump_version()
    add_drawables()
    patch_layout()
    patch_controller()
    write_notice()
    print("Applied Nuwe Mapa 0.4.3 map/settings UI refinement")


if __name__ == "__main__":
    main()
