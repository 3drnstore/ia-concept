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
    s = re.sub(r'versionName\s+"0\.4\.3"', 'versionName "0.4.4"', s)
    write(p, s)


def enforce_manifest_icon() -> None:
    p = APP / "AndroidManifest.xml"
    s = read(p)
    s = re.sub(r'android:icon="[^"]+"', 'android:icon="@mipmap/nuwe_launcher"', s, count=1)
    if 'android:roundIcon=' in s:
        s = re.sub(r'android:roundIcon="[^"]+"', 'android:roundIcon="@mipmap/nuwe_launcher"', s, count=1)
    else:
        s = s.replace('android:icon="@mipmap/nuwe_launcher"',
                      'android:icon="@mipmap/nuwe_launcher" android:roundIcon="@mipmap/nuwe_launcher"', 1)
    write(p, s)


def replace_all_launcher_fallbacks() -> None:
    # Modern launchers can resolve adaptive resources while some OEM launchers/file
    # managers still fall back to the historical OsmAnd icon names. Make every
    # fallback point to Nuwe artwork so there is no OsmAnd logo left to resolve.
    for density in ("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"):
        d = APP / f"res/mipmap-{density}"
        src = d / "nuwe_launcher.png"
        if not src.exists():
            raise RuntimeError(f"Missing generated Nuwe launcher for {density}: {src}")
        for name in ("icon.png", "icon_free.png", "icon_nightly.png"):
            dst = d / name
            shutil.copyfile(src, dst)

    # A genuine adaptive Nuwe launcher for Android 8+. Keep the legacy PNGs too.
    colors = APP / "res/values/nuwe_launcher_colors.xml"
    write(colors, '''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="nuwe_launcher_bg">#087A9B</color>
</resources>
''')

    foreground = APP / "res/drawable/nuwe_launcher_foreground.xml"
    write(foreground, '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#55FFFFFF"
        android:pathData="M16,18C39,5 69,6 92,24C72,16 45,16 20,31Z" />
    <path
        android:fillColor="#F5FEFF"
        android:pathData="M54,20L76,82L55,69L39,87L45,60L29,46Z" />
    <path
        android:fillColor="#7CFFB8"
        android:pathData="M34,72C45,64 60,64 72,72C61,69 48,70 38,78Z" />
</vector>
''')

    monochrome = APP / "res/drawable/nuwe_launcher_monochrome.xml"
    write(monochrome, '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:fillColor="#FFFFFFFF" android:pathData="M54,20L76,82L55,69L39,87L45,60L29,46Z" />
</vector>
''')

    adaptive = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/nuwe_launcher_bg" />
    <foreground android:drawable="@drawable/nuwe_launcher_foreground" />
    <monochrome android:drawable="@drawable/nuwe_launcher_monochrome" />
</adaptive-icon>
'''
    anydpi = APP / "res/mipmap-anydpi-v26"
    anydpi.mkdir(parents=True, exist_ok=True)
    for name in ("nuwe_launcher.xml", "icon.xml", "icon_free.xml", "icon_nightly.xml"):
        write(anydpi / name, adaptive)


def write_notice() -> None:
    write(ROOT / "NUWE_V44_ICON_PACKAGE_NOTICE.txt", """Nuwe Mapa 0.4.4

Icon/package hardening after device testing:
- application and roundIcon are forced to @mipmap/nuwe_launcher
- adds a real Nuwe adaptive icon for Android 8+
- replaces OsmAnd icon/icon_free/icon_nightly fallbacks with Nuwe artwork in every density
- replaces adaptive OsmAnd icon fallbacks with the Nuwe adaptive icon
- keeps the custom 0.4.3 map/settings UI
- build pipeline validates the final APK icon resource and ZIP/signature before publishing
""")


def main() -> None:
    bump_version()
    enforce_manifest_icon()
    replace_all_launcher_fallbacks()
    write_notice()
    print("Applied Nuwe Mapa 0.4.4 icon/package hardening")


if __name__ == "__main__":
    main()
