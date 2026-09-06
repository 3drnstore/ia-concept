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


def force_branding() -> None:
    # Force the visible Android label even if a flavor resource overrides app_name.
    p = APP / "AndroidManifest.xml"
    s = read(p)
    s = re.sub(r'android:icon="[^"]+"\s+android:label="[^"]+"',
               'android:icon="@mipmap/nuwe_launcher" android:roundIcon="@mipmap/nuwe_launcher" android:label="Nuwe Mapa"', s, count=1)
    write(p, s)

    p = APP / "build.gradle"
    s = read(p)
    # The build is personal and only the Android Full flavor is used, but forcing every
    # app_name resValue prevents the launcher name from falling back to OsmAnd.
    s = re.sub(r'resValue\s+"string",\s*"app_name",\s*"[^"]*"',
               'resValue "string", "app_name", "Nuwe Mapa"', s)
    s = re.sub(r'versionName\s+"0\.1\.0"', 'versionName "0.2.0"', s)
    write(p, s)


def patch_map_activity() -> None:
    p = APP / "src/net/osmand/plus/activities/MapActivity.java"
    s = read(p)
    # Remove the OsmAnd-branded What's New startup interruption from Nuwe Mapa.
    s = s.replace('if (WhatsNewDialogFragment.shouldShowDialog(app)) {',
                  'if (false && WhatsNewDialogFragment.shouldShowDialog(app)) {', 1)
    write(p, s)


def patch_colors() -> None:
    p = APP / "res/values/colors.xml"
    s = read(p)
    palette = {
        "map_button_icon_color_light": "#E9FDFF",
        "map_button_icon_color_dark": "#E9FDFF",
        "map_button_background_color_light": "#B51B8EA8",
        "map_button_background_color_dark": "#B50A2632",
        "activity_background_color_light": "#EAFBFF",
        "activity_background_color_dark": "#06151D",
        "card_and_list_background_light": "#DDF8FC",
        "card_and_list_background_dark": "#0B2732",
        "list_background_color_light": "#EAFBFF",
        "list_background_color_dark": "#071A23",
        "widget_background_color_light": "#D9F8FD",
        "widget_background_color_dark": "#0A2530",
        "divider_color_light": "#3A73D9E7",
        "divider_color_dark": "#3A5ED4E6",
        "active_color_primary_light": "#00A9C5",
        "active_color_primary_dark": "#5BE7F4",
        "icon_color_active_light": "#009DBA",
        "icon_color_active_dark": "#65EDF7",
    }
    for name, value in palette.items():
        s = re.sub(r'(<color\s+name="' + re.escape(name) + r'">).*?(</color>)',
                   r'\1' + value + r'\2', s)
    write(p, s)


def create_drawables() -> None:
    glass = '''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient android:angle="270" android:startColor="#D932C8DF" android:centerColor="#C51886A4" android:endColor="#D80A566F" />
    <corners android:radius="22dp" />
    <stroke android:width="1dp" android:color="#A8D9FBFF" />
    <padding android:left="14dp" android:top="10dp" android:right="14dp" android:bottom="10dp" />
</shape>
'''
    glass_dark = '''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient android:angle="270" android:startColor="#E0185368" android:centerColor="#E00B3446" android:endColor="#E0061A24" />
    <corners android:radius="26dp" />
    <stroke android:width="1dp" android:color="#855BE9F5" />
</shape>
'''
    chip = '''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient android:angle="0" android:startColor="#CC53E5F2" android:endColor="#CC0BAFC8" />
    <corners android:radius="999dp" />
    <stroke android:width="1dp" android:color="#C8E8FFFF" />
</shape>
'''
    line = '''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient android:angle="0" android:startColor="#0048E5F5" android:centerColor="#D948E5F5" android:endColor="#0048E5F5" />
    <size android:height="1dp" />
</shape>
'''
    write(APP / "res/drawable/nuwe_glass_panel.xml", glass)
    write(APP / "res/drawable/nuwe_glass_panel_dark.xml", glass_dark)
    write(APP / "res/drawable/nuwe_glass_chip.xml", chip)
    write(APP / "res/drawable/nuwe_glow_line.xml", line)


def patch_main_layout() -> None:
    p = APP / "res/layout/main.xml"
    s = read(p)
    s = s.replace('android:background="?attr/bg_color"\n\t\t\tandroid:clipToPadding="false"',
                  'android:background="@drawable/nuwe_glass_panel_dark"\n\t\t\tandroid:paddingTop="12dp"\n\t\t\tandroid:paddingBottom="12dp"\n\t\t\tandroid:divider="@android:color/transparent"\n\t\t\tandroid:dividerHeight="4dp"\n\t\t\tandroid:clipToPadding="false"', 1)
    s = s.replace('android:layout_width="280dp"', 'android:layout_width="300dp"', 1)
    write(p, s)


def patch_hud_layout() -> None:
    p = APP / "res/layout/map_hud_layout.xml"
    s = read(p)
    needle = '''\t\t<include\n\t\t\tandroid:id="@+id/MapHudButtonsOverlayTop"\n\t\t\tlayout="@layout/map_hud_top" />'''
    brand = '''\t\t<include\n\t\t\tandroid:id="@+id/MapHudButtonsOverlayTop"\n\t\t\tlayout="@layout/map_hud_top" />\n\n\t\t<!-- Nuwe Mapa Fruit Aero identity layer. Functional map widgets remain below. -->\n\t\t<LinearLayout\n\t\t\tandroid:layout_width="wrap_content"\n\t\t\tandroid:layout_height="wrap_content"\n\t\t\tandroid:layout_gravity="top|center_horizontal"\n\t\t\tandroid:layout_marginTop="12dp"\n\t\t\tandroid:background="@drawable/nuwe_glass_panel"\n\t\t\tandroid:elevation="10dp"\n\t\t\tandroid:gravity="center_vertical"\n\t\t\tandroid:orientation="horizontal">\n\n\t\t\t<TextView\n\t\t\t\tandroid:layout_width="wrap_content"\n\t\t\t\tandroid:layout_height="wrap_content"\n\t\t\t\tandroid:text="NUWE MAPA"\n\t\t\t\tandroid:textColor="#F3FEFF"\n\t\t\t\tandroid:textSize="15sp"\n\t\t\t\tandroid:textStyle="bold" />\n\n\t\t\t<TextView\n\t\t\t\tandroid:layout_width="wrap_content"\n\t\t\t\tandroid:layout_height="wrap_content"\n\t\t\t\tandroid:layout_marginStart="10dp"\n\t\t\t\tandroid:background="@drawable/nuwe_glass_chip"\n\t\t\t\tandroid:paddingHorizontal="10dp"\n\t\t\t\tandroid:paddingVertical="4dp"\n\t\t\t\tandroid:text="MULTI-GNSS"\n\t\t\t\tandroid:textColor="#043342"\n\t\t\t\tandroid:textSize="10sp"\n\t\t\t\tandroid:textStyle="bold" />\n\t\t</LinearLayout>'''
    if needle not in s:
        raise RuntimeError("map_hud_layout anchor not found")
    s = s.replace(needle, brand, 1)
    write(p, s)


def patch_top_toolbar() -> None:
    p = APP / "res/layout/widget_top_bar.xml"
    s = read(p)
    s = s.replace('android:clickable="true"\n\tandroid:visibility="gone"',
                  'android:clickable="true"\n\tandroid:background="@drawable/nuwe_glass_panel_dark"\n\tandroid:elevation="10dp"\n\tandroid:layout_margin="8dp"\n\tandroid:visibility="gone"', 1)
    s = s.replace('android:textColor="?android:textColorPrimary"', 'android:textColor="#F1FDFF"')
    write(p, s)


def patch_dashboard() -> None:
    # Apply glass treatment to the map dashboard if the upstream layout exposes the common dashboard container.
    p = APP / "res/layout/dashboard_over_map.xml"
    if not p.exists():
        return
    s = read(p)
    # Conservative: only restyle existing background attributes; keep IDs and hierarchy intact.
    s = s.replace('android:background="?attr/bg_color"', 'android:background="@drawable/nuwe_glass_panel_dark"')
    write(p, s)


def write_skin_notice() -> None:
    text = '''Nuwe Mapa 0.2.0 Fruit Aero\n\nThis build keeps OsmAnd's GPL navigation engine underneath but replaces visible launcher identity and applies a dedicated Nuwe Mapa Fruit Aero skin to the map HUD, map controls, toolbar, drawer and dashboard surfaces.\n\nVisible identity: Nuwe Mapa\nApplication ID: app.nuwe.mapa\nNavigation focus: car, bicycle and hiking/trails\nGNSS policy: Multi-GNSS monitoring, Galileo > BeiDou > GLONASS > GPS preference in Nuwe diagnostics, without excluding GPS.\n\nThe upstream license/copyright notices remain in the source tree.\n'''
    write(ROOT / "NUWE_FRUIT_AERO_NOTICE.txt", text)


def main() -> None:
    force_branding()
    patch_map_activity()
    patch_colors()
    create_drawables()
    patch_main_layout()
    patch_hud_layout()
    patch_top_toolbar()
    patch_dashboard()
    write_skin_notice()
    print("Applied Nuwe Mapa 0.2.0 Fruit Aero skin")


if __name__ == "__main__":
    main()
