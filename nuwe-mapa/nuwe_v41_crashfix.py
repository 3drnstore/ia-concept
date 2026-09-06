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


def fix_bind_order() -> None:
    p = APP / "src/net/osmand/plus/activities/MapActivity.java"
    s = read(p)

    # v0.4 bound the Nuwe controller before drawerLayout was assigned. The
    # controller calls disableDrawer(), which can dereference drawerLayout and
    # crash immediately at startup. Remove that early call and bind only after
    # the drawer/map root views are initialized.
    s = s.replace(
        '\n\t\t// Nuwe Mapa user-facing shell: central launcher + Fruit Aero 2000 HUD.\n\t\tnet.osmand.plus.nuwe.NuweLauncherController.bind(this);\n',
        '\n',
        1,
    )

    anchor = '\t\tdrawerLayout = findViewById(R.id.drawer_layout);\n\t\tmapViewWithLayers = findViewById(R.id.map_view_with_layers);\n'
    replacement = anchor + '\n\t\t// Bind Nuwe shell only after the drawer and map root views exist.\n\t\tnet.osmand.plus.nuwe.NuweLauncherController.bind(this);\n'
    if anchor not in s:
        raise RuntimeError("MapActivity drawer initialization anchor not found")
    if 'Bind Nuwe shell only after the drawer' not in s:
        s = s.replace(anchor, replacement, 1)
    write(p, s)


def bump_version() -> None:
    p = APP / "build.gradle"
    s = read(p)
    s = re.sub(r'versionName\s+"0\.4\.0"', 'versionName "0.4.1"', s)
    write(p, s)


def write_notice() -> None:
    write(ROOT / "NUWE_V41_CRASHFIX_NOTICE.txt", """Nuwe Mapa 0.4.1\n\nStartup crash fix:\n- NuweLauncherController is no longer bound before MapActivity drawerLayout exists\n- central Fruit Aero 2000 launcher is initialized after drawer and map root views\n- keeps the 0.4 structural UI redesign and stable signing\n""")


def main() -> None:
    fix_bind_order()
    bump_version()
    write_notice()
    print("Applied Nuwe Mapa 0.4.1 startup crash fix")


if __name__ == "__main__":
    main()
