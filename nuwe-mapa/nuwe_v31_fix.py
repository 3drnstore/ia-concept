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


def restore_launcher_identity() -> None:
    # Keep the glossy PNG launcher produced by patch_osmand.py. The v3 adaptive
    # icon used a simplified vector and hid the intended Nuwe launcher artwork.
    for name in ("nuwe_launcher.xml", "nuwe_launcher_round.xml"):
        p = APP / "res/mipmap-anydpi-v26" / name
        if p.exists():
            p.unlink()

    manifest = APP / "AndroidManifest.xml"
    s = read(manifest)
    s = re.sub(r'android:icon="[^"]+"', 'android:icon="@mipmap/nuwe_launcher"', s, count=1)
    if 'android:roundIcon=' in s:
        s = re.sub(r'android:roundIcon="[^"]+"', 'android:roundIcon="@mipmap/nuwe_launcher"', s, count=1)
    else:
        s = s.replace('android:icon="@mipmap/nuwe_launcher"',
                      'android:icon="@mipmap/nuwe_launcher" android:roundIcon="@mipmap/nuwe_launcher"', 1)
    write(manifest, s)


def bump_version() -> None:
    p = APP / "build.gradle"
    s = read(p)
    s = re.sub(r'versionName\s+"0\.3\.0"', 'versionName "0.3.1"', s)
    write(p, s)


def write_notice() -> None:
    write(ROOT / "NUWE_V31_FIX_NOTICE.txt", """Nuwe Mapa 0.3.1\n\nFixes:\n- restores the glossy Nuwe launcher icon used by the previous build\n- removes the simplified adaptive launcher override from 0.3.0\n- keeps the v3 Nuwe UI scope and vector icon replacements\n- build is re-signed with a stable public Android development test key so future Nuwe debug builds can update one another\n\nImportant: builds before 0.3.1 were signed by ephemeral GitHub debug keys. The already-installed older Nuwe build must be uninstalled once before installing 0.3.1.\n""")


def main() -> None:
    restore_launcher_identity()
    bump_version()
    write_notice()
    print("Applied Nuwe Mapa 0.3.1 launcher/signing compatibility fix")


if __name__ == "__main__":
    main()
