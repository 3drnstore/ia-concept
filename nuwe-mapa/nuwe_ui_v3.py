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


def replace_once(s: str, old: str, new: str, label: str) -> str:
    if old not in s:
        raise RuntimeError(f"Anchor not found: {label}")
    return s.replace(old, new, 1)


def force_branding() -> None:
    p = APP / "AndroidManifest.xml"
    s = read(p)
    s = re.sub(r'android:label="[^"]+"', 'android:label="Nuwe Mapa"', s, count=1)
    s = re.sub(r'android:icon="[^"]+"', 'android:icon="@mipmap/nuwe_launcher"', s, count=1)
    if 'android:roundIcon=' in s:
        s = re.sub(r'android:roundIcon="[^"]+"', 'android:roundIcon="@mipmap/nuwe_launcher_round"', s, count=1)
    else:
        s = s.replace('android:icon="@mipmap/nuwe_launcher"', 'android:icon="@mipmap/nuwe_launcher" android:roundIcon="@mipmap/nuwe_launcher_round"', 1)
    # Replace Samsung mini-window references too, otherwise some launchers still cache the upstream icon.
    s = s.replace('@mipmap/icon" android:value=""', '@mipmap/nuwe_launcher" android:value=""')
    write(p, s)

    p = APP / "build.gradle"
    s = read(p)
    s = re.sub(r'resValue\s+"string",\s*"app_name",\s*"[^"]*"', 'resValue "string", "app_name", "Nuwe Mapa"', s)
    s = re.sub(r'versionName\s+"0\.2\.0"', 'versionName "0.3.0"', s)
    write(p, s)


def create_adaptive_launcher() -> None:
    values = '''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="nuwe_launcher_bg">#0A6F8D</color>
</resources>
'''
    fg = '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#FFFFFF" android:pathData="M54,18 L79,78 L56,66 L41,84 L46,59 L29,43 Z"/>
    <path android:fillColor="#87F5B2" android:pathData="M31,72 C43,59 61,59 77,72 L73,78 C59,68 46,68 35,78 Z"/>
    <path android:fillColor="#66FFFFFF" android:pathData="M26,25 C40,13 68,13 83,25 C72,18 40,18 26,25 Z"/>
</vector>
'''
    icon = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/nuwe_launcher_bg" />
    <foreground android:drawable="@drawable/nuwe_launcher_foreground" />
</adaptive-icon>
'''
    write(APP / "res/values/nuwe_launcher_colors.xml", values)
    write(APP / "res/drawable/nuwe_launcher_foreground.xml", fg)
    for name in ["nuwe_launcher.xml", "nuwe_launcher_round.xml", "icon.xml", "icon_round.xml"]:
        write(APP / f"res/mipmap-anydpi-v26/{name}", icon)


def vector(path_data: str, fill: str = "#FFFFFFFF", size: int = 24) -> str:
    return f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="{size}dp" android:height="{size}dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="{fill}" android:pathData="{path_data}"/>
</vector>
'''


def create_nuwe_icons() -> None:
    icons = {
        "nuwe_ic_menu": "M4,6h16v2H4zM4,11h16v2H4zM4,16h16v2H4z",
        "nuwe_ic_search": "M10,4a6,6 0,1 0,0 12a6,6 0,0 0,0 -12M15,14l5,5l-1.4,1.4l-5,-5z",
        "nuwe_ic_car": "M5,11l2,-4h10l2,4l2,1v6h-2v2h-2v-2H7v2H5v-2H3v-6zM7.5,13a1.5,1.5 0,1 0,0 3a1.5,1.5 0,0 0,0 -3M16.5,13a1.5,1.5 0,1 0,0 3a1.5,1.5 0,0 0,0 -3M8,9l-1,2h10l-1,-2z",
        "nuwe_ic_bike": "M5,17a3,3 0,1 0,0 -6a3,3 0,0 0,0 6M19,17a3,3 0,1 0,0 -6a3,3 0,0 0,0 6M10,7h3l2,4h-4l-2,4M9,7l-2,4M13,7l3,-2",
        "nuwe_ic_hike": "M12,3a2,2 0,1 0,0 4a2,2 0,0 0,0 -4M10,8l3,1l2,4l-2,1l-1,-3l-2,3l-1,6H7l1,-7l2,-5M15,13l3,7h-2l-3,-6",
        "nuwe_ic_layers": "M12,3L3,8l9,5l9,-5zM3,12l9,5l9,-5v3l-9,5l-9,-5z",
        "nuwe_ic_location": "M12,2a7,7 0,1 0,0 14a7,7 0,0 0,0 -14M11,7h2v3h3v2h-5zM12,16l-3,6h6z",
        "nuwe_ic_compass": "M12,2a10,10 0,1 0,0 20a10,10 0,0 0,0 -20M15,7l-2,7l-4,3l2,-7z",
        "nuwe_ic_favorite": "M12,3l2.8,5.7l6.2,0.9l-4.5,4.4l1.1,6.2L12,17.3l-5.6,2.9l1.1,-6.2L3,9.6l6.2,-0.9z",
        "nuwe_ic_gpx": "M5,4h4l2,3h8v13H5zM8,10h8v2H8zM8,14h6v2H8z",
        "nuwe_ic_marker": "M12,2a6,6 0,0 0,-6 6c0,4.5 6,12 6,12s6,-7.5 6,-12a6,6 0,0 0,-6 -6M12,6a2,2 0,1 0,0 4a2,2 0,0 0,0 -4",
        "nuwe_ic_map": "M3,5l6,-2l6,2l6,-2v16l-6,2l-6,-2l-6,2zM9,5v12M15,7v12",
        "nuwe_ic_settings": "M12,8a4,4 0,1 0,0 8a4,4 0,0 0,0 -8M12,2l1,3l3,1l3,-1l1,2l-2,2l1,3l2,2l-1,2l-3,-1l-3,1l-1,3h-2l-1,-3l-3,-1l-3,1l-1,-2l2,-2l1,-3l-2,-2l1,-2l3,1l3,-1l1,-3z",
        "nuwe_ic_route": "M5,4a2,2 0,1 0,0 4a2,2 0,0 0,0 -4M19,16a2,2 0,1 0,0 4a2,2 0,0 0,0 -4M7,6c8,0 2,12 10,12v-2c-5,0 1,-12 -10,-12z",
        "nuwe_ic_globe": "M12,2a10,10 0,1 0,0 20a10,10 0,0 0,0 -20M4,12h16M12,4c3,3 3,13 0,16M12,4c-3,3 -3,13 0,16"
    }
    for name, data in icons.items():
        write(APP / f"res/drawable/{name}.xml", vector(data))


def patch_application_mode_icons() -> None:
    p = APP / "src/net/osmand/plus/settings/backend/ApplicationMode.java"
    s = read(p)
    repl = {
        "CAR": "nuwe_ic_car",
        "BICYCLE": "nuwe_ic_bike",
        "PEDESTRIAN": "nuwe_ic_hike",
        "DEFAULT": "nuwe_ic_globe",
    }
    for const, icon in repl.items():
        pat = re.compile(r'(public static final ApplicationMode\s+' + const + r'\s*=.*?\.icon\()R\.drawable\.[A-Za-z0-9_]+(\))', re.S)
        s, count = pat.subn(r'\1R.drawable.' + icon + r'\2', s, count=1)
        if count != 1:
            raise RuntimeError(f"Could not patch ApplicationMode {const}")
    write(p, s)


def restrict_profile_picker() -> None:
    p = APP / "src/net/osmand/plus/activities/actions/AppModeDialog.java"
    s = read(p)
    old = '''\t\tList<ApplicationMode> values = new ArrayList<>(ApplicationMode.values(app));\n\t\tselected.add(settings.getApplicationMode());'''
    new = '''\t\tList<ApplicationMode> values = new ArrayList<>();\n\t\tvalues.add(ApplicationMode.CAR);\n\t\tvalues.add(ApplicationMode.BICYCLE);\n\t\tvalues.add(ApplicationMode.PEDESTRIAN);\n\t\tApplicationMode current = settings.getApplicationMode();\n\t\tif (!values.contains(current)) {\n\t\t\tcurrent = ApplicationMode.CAR;\n\t\t\tsettings.setApplicationMode(current);\n\t\t}\n\t\tselected.add(current);'''
    s = replace_once(s, old, new, "AppModeDialog restricted modes")
    write(p, s)


def patch_configure_map_scope() -> None:
    p = APP / "src/net/osmand/plus/configmap/ConfigureMapMenu.java"
    s = read(p)
    if 'import static net.osmand.osm.OsmRouteType.BICYCLE;' not in s:
        s = s.replace('import static net.osmand.osm.OsmRouteType.ALPINE;\n', 'import static net.osmand.osm.OsmRouteType.ALPINE;\nimport static net.osmand.osm.OsmRouteType.BICYCLE;\nimport static net.osmand.osm.OsmRouteType.MTB;\n')

    loop = 'for (String attrName : RouteUtils.getRoutesAttrsNames(customRules)) {'
    filt = '''for (String attrName : RouteUtils.getRoutesAttrsNames(customRules)) {\n\t\t\tboolean nuweRoute = BICYCLE.getRenderingPropertyAttr().equals(attrName)\n\t\t\t\t\t|| MTB.getRenderingPropertyAttr().equals(attrName)\n\t\t\t\t\t|| HIKING.getRenderingPropertyAttr().equals(attrName)\n\t\t\t\t\t|| ALPINE.getRenderingPropertyAttr().equals(attrName);\n\t\t\tif (!nuweRoute) {\n\t\t\t\tRenderingRuleProperty skipped = getPropertyForAttr(customRules, attrName);\n\t\t\t\tcustomRules.remove(skipped);\n\t\t\t\tcontinue;\n\t\t\t}'''
    s = replace_once(s, loop, filt, "route scope filter")

    # Remove unsupported/online-only items after plugins have registered themselves.
    marker = '\t\tcreateRenderingAttributeItems(customRules, adapter, mapActivity, nightMode);\n\n\t\treturn adapter;'
    replacement = '''\t\tcreateRenderingAttributeItems(customRules, adapter, mapActivity, nightMode);\n\n\t\tadapter.getItems().removeIf(item -> {\n\t\t\tString id = item.getId();\n\t\t\treturn TRANSPORT_ID.equals(id)\n\t\t\t\t\t|| WEATHER_ID.equals(id)\n\t\t\t\t\t|| WIKIPEDIA_ID.equals(id)\n\t\t\t\t\t|| MAPILLARY.equals(id)\n\t\t\t\t\t|| OVERLAY_MAP.equals(id)\n\t\t\t\t\t|| UNDERLAY_MAP.equals(id);\n\t\t});\n\n\t\treturn adapter;'''
    s = replace_once(s, marker, replacement, "configure-map item cleanup")
    write(p, s)


def unlock_local_features_and_remove_pro_badges() -> None:
    p = APP / "src/net/osmand/plus/inapp/InAppPurchaseUtils.java"
    s = read(p)
    pat = re.compile(r'(public static boolean isOsmAndProAvailable\(@NonNull OsmandApplication app, boolean checkDevBuild\) \{).*?\n\t\}', re.S)
    s, count = pat.subn(r'\1\n\t\treturn true;\n\t}', s, count=1)
    if count != 1:
        raise RuntimeError("Could not force local Pro availability")
    write(p, s)


def patch_renderer_display_name() -> None:
    p = APP / "src/net/osmand/plus/render/RendererRegistry.java"
    s = read(p)
    needle = '''\tpublic static String getRendererName(@NonNull Context ctx, @NonNull String name) {\n\t\tString translation = getTranslatedRendererName(ctx, name);'''
    repl = '''\tpublic static String getRendererName(@NonNull Context ctx, @NonNull String name) {\n\t\tif (DEFAULT_RENDER.equals(name)) {\n\t\t\treturn "Nuwe";\n\t\t}\n\t\tString translation = getTranslatedRendererName(ctx, name);'''
    s = replace_once(s, needle, repl, "renderer display name")
    write(p, s)


def replace_visible_osmand_strings() -> None:
    # Keep legal/license/about strings intact; replace product branding in ordinary UI strings.
    blocked = ("license", "copyright", "privacy", "terms", "about_osmand", "source_code")
    entry = re.compile(r'(<string\s+name="([^"]+)"[^>]*>)(.*?)(</string>)', re.S)
    for p in APP.glob("res/values*/strings.xml"):
        s = read(p)
        def sub(m):
            key = m.group(2).lower()
            body = m.group(3)
            if any(b in key for b in blocked):
                return m.group(0)
            body = body.replace('OsmAnd+', 'Nuwe Mapa').replace('OsmAnd~', 'Nuwe Mapa').replace('OsmAnd', 'Nuwe Mapa')
            return m.group(1) + body + m.group(4)
        ns = entry.sub(sub, s)
        if ns != s:
            write(p, ns)


def remove_useless_hud_brand_bar() -> None:
    p = APP / "res/layout/map_hud_layout.xml"
    s = read(p)
    # Remove the 0.2 decorative Nuwe/MULTI-GNSS pill. Multi-GNSS stays functional in code.
    s = re.sub(r'\n\s*<!-- Nuwe Mapa Fruit Aero identity layer\..*?</LinearLayout>', '', s, count=1, flags=re.S)
    write(p, s)


def patch_core_icons_in_ui() -> None:
    replacements = {
        '@drawable/ic_action_search_dark': '@drawable/nuwe_ic_search',
        '@drawable/ic_action_drawer': '@drawable/nuwe_ic_menu',
        '@drawable/ic_action_layers': '@drawable/nuwe_ic_layers',
        '@drawable/ic_action_favorite': '@drawable/nuwe_ic_favorite',
        '@drawable/ic_action_flag': '@drawable/nuwe_ic_marker',
        '@drawable/ic_action_polygom_dark': '@drawable/nuwe_ic_gpx',
        '@drawable/ic_world_globe_dark': '@drawable/nuwe_ic_map',
        '@drawable/ic_action_car_dark': '@drawable/nuwe_ic_car',
        '@drawable/ic_action_bicycle_dark': '@drawable/nuwe_ic_bike',
        '@drawable/ic_action_trekking_dark': '@drawable/nuwe_ic_hike',
    }
    # Only user-facing Android XML resources; do not touch engine/source identifiers.
    for root in [APP / "res/layout", APP / "res/menu", APP / "res/xml"]:
        if not root.exists():
            continue
        for p in root.rglob("*.xml"):
            s = read(p)
            ns = s
            for old, new in replacements.items():
                ns = ns.replace(old, new)
            if ns != s:
                write(p, ns)


def patch_main_surface() -> None:
    # Full-screen map remains the primary surface; drawer and toolbar become Nuwe glass panels.
    p = APP / "res/layout/main.xml"
    s = read(p)
    s = s.replace('android:layout_width="300dp"', 'android:layout_width="292dp"')
    s = s.replace('android:background="@drawable/nuwe_glass_panel_dark"', 'android:background="@drawable/nuwe_glass_panel_dark"')
    write(p, s)

    p = APP / "res/layout/fragment_configure_map.xml"
    if p.exists():
        s = read(p)
        s = s.replace('android:background="?attr/activity_background_color"', 'android:background="#EAFBFF"')
        write(p, s)


def write_notice() -> None:
    write(ROOT / "NUWE_UI_V3_NOTICE.txt", '''Nuwe Mapa 0.3.0\n\nUI scope correction:\n- launcher/adaptive icon fully replaced\n- decorative Nuwe/Multi-GNSS map pill removed\n- profile picker restricted to Car, Bicycle and Hiking\n- map route layers restricted to bicycle, MTB, hiking and alpine hiking\n- transport, weather, Wikipedia, Mapillary and online overlay/underlay entries removed from Configure Map\n- default renderer shown as Nuwe instead of OsmAnd\n- common visible action/profile icons replaced with custom Nuwe vector drawables\n- local feature entitlement forced on so PRO purchase badges do not appear for supported local features\n- ordinary visible OsmAnd branding strings replaced while legal/license strings stay intact\n\nOsmAnd GPL navigation engine remains underneath.\n''')


def main() -> None:
    force_branding()
    create_adaptive_launcher()
    create_nuwe_icons()
    patch_application_mode_icons()
    restrict_profile_picker()
    patch_configure_map_scope()
    unlock_local_features_and_remove_pro_badges()
    patch_renderer_display_name()
    replace_visible_osmand_strings()
    remove_useless_hud_brand_bar()
    patch_core_icons_in_ui()
    patch_main_surface()
    write_notice()
    print("Applied Nuwe Mapa 0.3.0 complete UI scope patch")


if __name__ == "__main__":
    main()
