#!/usr/bin/env python3
from pathlib import Path
import re
import shutil
import sys

from PIL import Image
from patch_osmand import build_icon

ROOT = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path("android").resolve()
APP = ROOT / "OsmAnd"
HERE = Path(__file__).resolve().parent
SRC = HERE / "v50-src"


def read(p: Path) -> str:
    return p.read_text(encoding="utf-8")


def write(p: Path, s: str) -> None:
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(s, encoding="utf-8")


def patch_version() -> None:
    p = APP / "build.gradle"
    s = read(p)
    s = re.sub(r'versionCode\s+5399', 'versionCode 5500', s, count=1)
    s = re.sub(r'versionName\s+"0\.1\.0"', 'versionName "0.5.0"', s, count=1)
    if 'versionCode 5500' not in s or 'versionName "0.5.0"' not in s:
        raise RuntimeError("Could not set Nuwe 0.5.0 version")
    write(p, s)


def install_new_ui_sources() -> None:
    dst = APP / "src/net/osmand/plus/nuwe"
    dst.mkdir(parents=True, exist_ok=True)
    for name in ("NuweUi.java", "NuweMapShell.java", "NuweSheets.java"):
        src = SRC / name
        if not src.exists():
            raise RuntimeError(f"Missing v50 source: {src}")
        shutil.copyfile(src, dst / name)


def restore_original_nuwe_icon() -> None:
    # Use the original glossy Nuwe launcher artwork created by patch_osmand.py,
    # explicitly requested by the user. Do not generate the geometric-N v46 icon.
    base = build_icon(1024)
    densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for density, px in densities.items():
        d = APP / f"res/mipmap-{density}"
        d.mkdir(parents=True, exist_ok=True)
        out = d / "nuwe_launcher.png"
        base.resize((px, px), Image.Resampling.LANCZOS).save(out)
        # Defensive fallback: every historic OsmAnd launcher resource resolves to
        # the exact same Nuwe artwork even if a flavor manifest survives merging.
        for fallback in ("icon.png", "icon_free.png", "icon_nightly.png"):
            fp = d / fallback
            if fp.exists():
                shutil.copyfile(out, fp)

    # Never allow an adaptive resource from an older experiment to override PNGs.
    adaptive = APP / "res/mipmap-anydpi-v26"
    for name in ("nuwe_launcher.xml", "nuwe_launcher_round.xml"):
        p = adaptive / name
        if p.exists():
            p.unlink()

    # Patch the base manifest AND every flavor manifest. OsmAnd flavors can use
    # tools:replace on android:icon, which was the reason old builds could still
    # install with an OsmAnd icon even though the base manifest was branded.
    manifests = sorted(APP.glob("AndroidManifest*.xml"))
    for manifest in manifests:
        s = read(manifest)
        app_match = re.search(r'<application\b[^>]*>', s, flags=re.S)
        if not app_match:
            continue
        tag = app_match.group(0)
        if 'android:icon=' in tag:
            tag = re.sub(r'android:icon="[^"]+"', 'android:icon="@mipmap/nuwe_launcher"', tag)
        else:
            tag = tag[:-1] + ' android:icon="@mipmap/nuwe_launcher">'
        if 'android:roundIcon=' in tag:
            tag = re.sub(r'android:roundIcon="[^"]+"', 'android:roundIcon="@mipmap/nuwe_launcher"', tag)
        else:
            tag = tag[:-1] + ' android:roundIcon="@mipmap/nuwe_launcher">'
        if 'android:label=' in tag:
            tag = re.sub(r'android:label="[^"]+"', 'android:label="Nuwe Mapa"', tag)
        else:
            tag = tag[:-1] + ' android:label="Nuwe Mapa">'
        s = s[:app_match.start()] + tag + s[app_match.end():]
        write(manifest, s)


def reset_map_hud_layout() -> None:
    # Clean upstream-shaped HUD: old Fruit Aero search bar/orb/overlay are removed.
    # NuweMapShell is the only user-facing HUD added programmatically at runtime.
    xml = '''<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/map_hud_container"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fitsSystemWindows="true"
    android:orientation="vertical">

    <net.osmand.plus.views.controls.MapHudLayout
        android:id="@+id/map_hud_layout"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">

        <include android:id="@+id/MapHudButtonsOverlayTop" layout="@layout/map_hud_top" />

        <net.osmand.plus.widgets.FrameLayoutEx
            android:id="@+id/left_side_menu"
            android:layout_width="0dp"
            android:layout_height="0dp"
            android:visibility="gone" />

        <include android:id="@+id/MapHudButtonsOverlayBottom" layout="@layout/map_hud_bottom" />

        <net.osmand.plus.quickaction.QuickActionsWidget
            android:id="@+id/quick_action_widget"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:visibility="gone" />

        <net.osmand.plus.views.controls.VerticalWidgetPanel
            android:id="@+id/map_bottom_widgets_panel"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_gravity="bottom"
            android:orientation="vertical"
            app:topPanel="false" />

    </net.osmand.plus.views.controls.MapHudLayout>

    <net.osmand.plus.views.ObservableFrameLayout
        android:id="@+id/bottomFragmentContainer"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />

</LinearLayout>
'''
    write(APP / "res/layout/map_hud_layout.xml", xml)


def patch_map_activity() -> None:
    p = APP / "src/net/osmand/plus/activities/MapActivity.java"
    s = read(p)
    # Remove every legacy Nuwe shell bind left by old experimental branches.
    s = re.sub(r'\s*net\.osmand\.plus\.nuwe\.NuweLauncherController\.bind\(this\);', '', s)
    s = re.sub(r'\s*net\.osmand\.plus\.nuwe\.NuweMapShell\.bind\(this\);', '', s)
    anchor = '''\t\tdrawerLayout = findViewById(R.id.drawer_layout);\n\t\tmapViewWithLayers = findViewById(R.id.map_view_with_layers);'''
    if anchor not in s:
        raise RuntimeError("MapActivity bind anchor not found")
    replacement = anchor + '''\n\n\t\t// Nuwe owns the complete visible map UI; OsmAnd remains the engine.\n\t\tnet.osmand.plus.nuwe.NuweMapShell.bind(this);'''
    s = s.replace(anchor, replacement, 1)
    write(p, s)


def patch_native_hud_attachment() -> None:
    p = APP / "src/net/osmand/plus/views/controls/MapHudLayout.java"
    s = read(p)
    old_button = '''\t\tif (button.getParent() == null) {\n\t\t\taddView(button, params);\n\t\t}\n\t\tmapButtons.add(button);'''
    new_button = '''\t\t// Nuwe renders its own HUD. Keep native button objects alive for internal\n\t\t// state/controllers, but never attach them to the visible hierarchy.\n\t\tif ("app.nuwe.mapa".equals(app.getPackageName())) {\n\t\t\tmapButtons.add(button);\n\t\t\treturn;\n\t\t}\n\t\tif (button.getParent() == null) {\n\t\t\taddView(button, params);\n\t\t}\n\t\tmapButtons.add(button);'''
    if old_button not in s:
        raise RuntimeError("MapHudLayout.addMapButton anchor not found")
    s = s.replace(old_button, new_button, 1)

    old_widget = '''\t\tif (view.getParent() == null) {\n\t\t\taddView(view, params);\n\t\t}\n\t\tadditionalWidgetPositions.put(view, position);'''
    new_widget = '''\t\t// Same rule for runtime widgets: Nuwe supplies its own map chrome.\n\t\tif ("app.nuwe.mapa".equals(app.getPackageName())) {\n\t\t\tadditionalWidgetPositions.put(view, position);\n\t\t\treturn;\n\t\t}\n\t\tif (view.getParent() == null) {\n\t\t\taddView(view, params);\n\t\t}\n\t\tadditionalWidgetPositions.put(view, position);'''
    if old_widget not in s:
        raise RuntimeError("MapHudLayout.addWidget anchor not found")
    s = s.replace(old_widget, new_widget, 1)
    write(p, s)


def patch_context_menu() -> None:
    p = APP / "src/net/osmand/plus/mapcontextmenu/MapContextMenu.java"
    s = read(p)
    signature = '''\tpublic void show(@NonNull LatLon latLon,\n\t                 @Nullable PointDescription pointDescription,\n\t                 @Nullable Object object) {\n\t\tMapActivity mapActivity = getMapActivity();'''
    replacement = '''\tpublic void show(@NonNull LatLon latLon,\n\t                 @Nullable PointDescription pointDescription,\n\t                 @Nullable Object object) {\n\t\tMapActivity mapActivity = getMapActivity();\n\t\tif (mapActivity != null && "app.nuwe.mapa".equals(mapActivity.getPackageName())) {\n\t\t\tnet.osmand.plus.nuwe.NuweMapShell.handleContextPoint(mapActivity, latLon, pointDescription, object);\n\t\t\treturn;\n\t\t}'''
    if signature not in s:
        raise RuntimeError("MapContextMenu.show anchor not found")
    s = s.replace(signature, replacement, 1)
    write(p, s)


def write_notice() -> None:
    write(ROOT / "NUWE_UI_V50_NOTICE.txt", '''Nuwe Mapa 0.5.0\n\nComplete visible UI rebuild inspired by the interaction model of modern offline-map apps, without copying third-party branding/assets.\n\n- OsmAnd remains the offline map/routing engine only.\n- No persistent bottom navigation bar.\n- Circular Nuwe launcher opens an expandable bottom sheet.\n- Custom map controls: layers, ruler, compass/orientation, 2D/3D, zoom and triangular location arrow.\n- Custom search, route launcher, favorites, tracks, local maps and settings sheets.\n- Visible profiles limited to Carro, Bike and Trilha.\n- Native OsmAnd HUD buttons/widgets are not attached to the visible hierarchy for app.nuwe.mapa.\n- Native OsmAnd point context menu is redirected to the Nuwe point sheet.\n- Original glossy Nuwe launcher icon from the first branding pass is restored across base and flavor manifests.\n''')


def main() -> None:
    patch_version()
    install_new_ui_sources()
    restore_original_nuwe_icon()
    reset_map_hud_layout()
    patch_map_activity()
    patch_native_hud_attachment()
    patch_context_menu()
    write_notice()
    print("Applied Nuwe Mapa 0.5.0 full custom UI rebuild")


if __name__ == "__main__":
    main()
