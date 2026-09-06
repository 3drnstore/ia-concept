#!/usr/bin/env python3
from pathlib import Path
import re
import shutil
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
    s = re.sub(r'versionCode\s+5399', 'versionCode 5405', s, count=1)
    s = re.sub(r'versionName\s+"0\.4\.4"', 'versionName "0.4.5"', s, count=1)
    write(p, s)


def enforce_png_launcher() -> None:
    manifest = APP / "AndroidManifest.xml"
    s = read(manifest)
    s = re.sub(r'android:icon="[^"]+"', 'android:icon="@mipmap/nuwe_launcher"', s, count=1)
    if 'android:roundIcon=' in s:
        s = re.sub(r'android:roundIcon="[^"]+"', 'android:roundIcon="@mipmap/nuwe_launcher"', s, count=1)
    else:
        s = s.replace('android:icon="@mipmap/nuwe_launcher"',
                      'android:icon="@mipmap/nuwe_launcher" android:roundIcon="@mipmap/nuwe_launcher"', 1)
    write(manifest, s)

    # Revert the 0.4.4 adaptive-icon experiment. The 0.4.2 build that installed
    # correctly used density PNGs. Keep the same proven launcher representation.
    anydpi = APP / "res/mipmap-anydpi-v26"
    for name in ("nuwe_launcher.xml", "nuwe_launcher_round.xml", "icon.xml", "icon_free.xml", "icon_nightly.xml"):
        p = anydpi / name
        if p.exists():
            p.unlink()

    # Keep every historical OsmAnd fallback name mapped to the Nuwe PNG artwork.
    for density in ("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"):
        d = APP / f"res/mipmap-{density}"
        src = d / "nuwe_launcher.png"
        if not src.exists():
            raise RuntimeError(f"Missing Nuwe launcher PNG: {src}")
        for name in ("icon.png", "icon_free.png", "icon_nightly.png"):
            shutil.copyfile(src, d / name)


def write_notice() -> None:
    write(ROOT / "NUWE_V45_INSTALL_ICON_NOTICE.txt", """Nuwe Mapa 0.4.5

Install/icon compatibility correction:
- versionCode increased from upstream 5399 to 5405 so Android treats this as a real update
- removes the 0.4.4 adaptive icon experiment
- returns to the density-PNG launcher method used by the installable 0.4.2 build
- all historical icon/icon_free/icon_nightly PNG fallbacks are replaced with Nuwe artwork
- application icon and roundIcon remain @mipmap/nuwe_launcher
- preserves the 0.4.3 custom Fruit Aero map and settings UI
""")


def main() -> None:
    bump_version()
    enforce_png_launcher()
    write_notice()
    print("Applied Nuwe Mapa 0.4.5 install/icon compatibility fix")


if __name__ == "__main__":
    main()
