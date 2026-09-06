#!/usr/bin/env python3
from pathlib import Path
import re
import shutil
import sys

from PIL import Image

ROOT = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path("android").resolve()
APP = ROOT / "OsmAnd"
HERE = Path(__file__).resolve().parent
SRC = HERE / "v50-src"
ICON = HERE / "assets" / "nuwe_launcher.png"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def require_replace(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise RuntimeError(f"Expected {label} anchor not found")
    return text.replace(old, new, 1)


def install_integration_sources() -> None:
    dst = APP / "src/net/osmand/plus/nuwe"
    for name in ("NuweMapController.java", "NuweLauncher.java"):
        src = SRC / name
        if not src.exists():
            raise RuntimeError(f"Missing Nuwe integration source: {src}")
        shutil.copyfile(src, dst / name)

    shell = dst / "NuweMapShell.java"
    s = read(shell)
    s = s.replace("        hideNativeHud(activity);\n", "")
    s = s.replace("v -> NuweSheets.showLauncher(activity)",
                  "v -> NuweMapController.openLauncher(activity)")
    write(shell, s)


def install_canonical_launcher_icon() -> None:
    if not ICON.exists():
        raise RuntimeError(f"Canonical Nuwe launcher icon not found: {ICON}")

    base = Image.open(ICON).convert("RGBA")
    densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for density, px in densities.items():
        d = APP / f"res/mipmap-{density}"
        d.mkdir(parents=True, exist_ok=True)
        out = d / "nuwe_launcher.png"
        base.resize((px, px), Image.Resampling.LANCZOS).save(out)
        for fallback in ("icon.png", "icon_free.png", "icon_nightly.png"):
            fp = d / fallback
            if fp.exists():
                shutil.copyfile(out, fp)

    # Adaptive launcher uses the exact same existing Nuwe artwork. Density PNGs
    # remain as fallbacks for pre-Android-8 devices and flavor aliases.
    art = APP / "res/drawable-nodpi/nuwe_launcher_art.png"
    art.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(ICON, art)

    write(APP / "res/drawable/nuwe_launcher_background.xml", '''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#079DBD" />
</shape>
''')
    write(APP / "res/drawable/nuwe_launcher_foreground.xml", '''<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:gravity="fill">
        <bitmap android:src="@drawable/nuwe_launcher_art" android:gravity="fill" />
    </item>
</layer-list>
''')
    adaptive = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/nuwe_launcher_background" />
    <foreground android:drawable="@drawable/nuwe_launcher_foreground" />
</adaptive-icon>
'''
    write(APP / "res/mipmap-anydpi-v26/nuwe_launcher.xml", adaptive)
    write(APP / "res/mipmap-anydpi-v26/nuwe_launcher_round.xml", adaptive)

    # Manifest variants can override branding, so enforce the same resource in all.
    for manifest in sorted(APP.glob("AndroidManifest*.xml")):
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
        s = s[:app_match.start()] + tag + s[app_match.end():]
        write(manifest, s)


def patch_map_activity_boundary() -> None:
    p = APP / "src/net/osmand/plus/activities/MapActivity.java"
    s = read(p)
    s = require_replace(
        s,
        "net.osmand.plus.nuwe.NuweMapShell.bind(this);",
        "net.osmand.plus.nuwe.NuweMapController.attach(this);",
        "MapActivity -> Nuwe controller",
    )
    write(p, s)


def patch_context_boundary() -> None:
    p = APP / "src/net/osmand/plus/mapcontextmenu/MapContextMenu.java"
    s = read(p)
    s = s.replace("net.osmand.plus.nuwe.NuweMapShell.handleContextPoint(mapActivity, latLon, pointDescription, object);",
                  "net.osmand.plus.nuwe.NuweMapController.onMapPoint(mapActivity, latLon, pointDescription, object);")
    write(p, s)


def patch_version() -> None:
    p = APP / "build.gradle"
    s = read(p)
    s = re.sub(r"versionCode\s+5500", "versionCode 5510", s, count=1)
    s = re.sub(r'versionName\s+"0\.5\.0"', 'versionName "0.5.1"', s, count=1)
    if "versionCode 5510" not in s or 'versionName "0.5.1"' not in s:
        raise RuntimeError("Could not set Nuwe Mapa 0.5.1 version")
    write(p, s)


def write_notice() -> None:
    write(ROOT / "NUWE_UI_V51_NOTICE.txt", """Nuwe Mapa 0.5.1\n\nArchitecture pass:\n- OsmAnd engine -> NuweMapController -> Nuwe UI.\n- MapActivity no longer binds directly to NuweMapShell.\n- Native OsmAnd point context menu is redirected through NuweMapController.\n- Circular launcher is implemented by NuweLauncher, separate from the map shell.\n- Launcher exposes Buscar, Trajeto/Rota, Favoritos, Trilhas/GPX, Gravar trajeto, Mapas offline, Compartilhar localização, Adicionar marcador and Configurações.\n- Runtime hideNativeHud() call removed; Nuwe does not depend on repeatedly hiding OsmAnd controls.\n- Visible profiles remain only Carro, Bike and Trilha.\n- Launcher branding is sourced from the canonical pre-existing Nuwe icon asset, including density fallbacks and adaptive icon wrappers.\n""")


def main() -> None:
    install_integration_sources()
    install_canonical_launcher_icon()
    patch_map_activity_boundary()
    patch_context_boundary()
    patch_version()
    write_notice()
    print("Applied Nuwe Mapa 0.5.1 integration architecture")


if __name__ == "__main__":
    main()
