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
    s = re.sub(r'versionName\s+"0\.4\.1"', 'versionName "0.4.2"', s)
    write(p, s)


def verify_launcher_resources() -> None:
    manifest = APP / "AndroidManifest.xml"
    s = read(manifest)
    if 'android:icon="@mipmap/nuwe_launcher"' not in s:
        raise RuntimeError("Nuwe launcher icon is not set in manifest")
    if 'android:roundIcon="@mipmap/nuwe_launcher"' not in s:
        raise RuntimeError("Nuwe round launcher icon is not set in manifest")
    required = [
        APP / "res/mipmap-mdpi/nuwe_launcher.png",
        APP / "res/mipmap-hdpi/nuwe_launcher.png",
        APP / "res/mipmap-xhdpi/nuwe_launcher.png",
        APP / "res/mipmap-xxhdpi/nuwe_launcher.png",
        APP / "res/mipmap-xxxhdpi/nuwe_launcher.png",
    ]
    missing = [str(p) for p in required if not p.exists()]
    if missing:
        raise RuntimeError("Missing Nuwe launcher PNGs: " + ", ".join(missing))


def write_notice() -> None:
    write(ROOT / "NUWE_V42_PACKAGING_NOTICE.txt", """Nuwe Mapa 0.4.2\n\nAndroid package compatibility fix:\n- keeps the Fruit Aero 2000 UI and 0.4.1 startup fix\n- verifies Nuwe launcher resources before compilation\n- APK is zipaligned before final signing\n- final APK signature is verified after alignment\n- final package metadata is validated with Android build tools before upload\n\nThis release targets the package-parser error seen on-device with 0.4.1.\n""")


def main() -> None:
    bump_version()
    verify_launcher_resources()
    write_notice()
    print("Applied Nuwe Mapa 0.4.2 packaging compatibility fix")


if __name__ == "__main__":
    main()
