#!/usr/bin/env python3
from pathlib import Path
import re
import shutil
import sys

ROOT = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path("android").resolve()
APP = ROOT / "OsmAnd"
HERE = Path(__file__).resolve().parent
SRC = HERE / "v50-src"


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

    # NuweMapShell remains the view/controller facade for map operations, but it
    # no longer owns the central launcher and no longer hides OsmAnd views at
    # runtime. Native HUD attachment is blocked structurally by the v50 patch.
    shell = dst / "NuweMapShell.java"
    s = read(shell)
    s = s.replace("        hideNativeHud(activity);\n", "")
    s = s.replace("v -> NuweSheets.showLauncher(activity)",
                  "v -> NuweMapController.openLauncher(activity)")
    write(shell, s)


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
    write(ROOT / "NUWE_UI_V51_NOTICE.txt", """Nuwe Mapa 0.5.1\n\nArchitecture pass:\n- OsmAnd engine -> NuweMapController -> Nuwe UI.\n- MapActivity no longer binds directly to NuweMapShell.\n- Native OsmAnd point context menu is redirected through NuweMapController.\n- Circular launcher is implemented by NuweLauncher, separate from the map shell.\n- Launcher exposes Buscar, Trajeto/Rota, Favoritos, Trilhas/GPX, Gravar trajeto, Mapas offline, Compartilhar localização, Adicionar marcador and Configurações.\n- Runtime hideNativeHud() call removed; Nuwe does not depend on repeatedly hiding OsmAnd controls.\n- Visible profiles remain only Carro, Bike and Trilha.\n""")


def main() -> None:
    install_integration_sources()
    patch_map_activity_boundary()
    patch_context_boundary()
    patch_version()
    write_notice()
    print("Applied Nuwe Mapa 0.5.1 integration architecture")


if __name__ == "__main__":
    main()
