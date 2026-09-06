#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path("android").resolve()
APP = ROOT / "OsmAnd"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def require_replace(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise RuntimeError(f"Expected text not found while patching {label}: {old[:100]!r}")
    return text.replace(old, new, 1)


def replace_method(text: str, signature: str, body: str, label: str) -> str:
    # Methods patched here contain no nested braces in their current implementation.
    pattern = re.compile(r"(\tpublic static boolean " + re.escape(signature) + r"\s*\{).*?\n\t\}", re.S)
    replacement = r"\1\n\t\t" + body + "\n\t}"
    result, count = pattern.subn(replacement, text, count=1)
    if count != 1:
        raise RuntimeError(f"Could not patch method {label}")
    return result


def patch_gradle() -> None:
    p = APP / "build.gradle"
    s = read(p)
    s = require_replace(
        s,
        'applicationId "net.osmand.plus"\n\t\t\tresValue "string", "app_name", "OsmAnd~"',
        'applicationId "app.nuwe.mapa"\n\t\t\tresValue "string", "app_name", "Nuwe Mapa"',
        "androidFull flavor",
    )
    s = require_replace(s, 'versionName "5.4.0"', 'versionName "0.1.0"', "versionName")
    write(p, s)


def patch_version() -> None:
    p = APP / "src/net/osmand/plus/Version.java"
    s = read(p)
    s = replace_method(s, "isGooglePlayEnabled()", "return false;", "isGooglePlayEnabled")
    s = replace_method(s, "isMarketEnabled()", "return false;", "isMarketEnabled")
    s = replace_method(s, "isInAppPurchaseSupported()", "return false;", "isInAppPurchaseSupported")
    s = replace_method(s, "isFreeVersion(@NonNull OsmandApplication app)", "return false;", "isFreeVersion")
    s = replace_method(s, "isFullVersion(@NonNull OsmandApplication app)", "return true;", "isFullVersion")
    # This method currently spans several entitlement checks; replace it explicitly.
    pattern = re.compile(
        r"\tpublic static boolean isPaidVersion\(@NonNull OsmandApplication app\) \{.*?\n\t\}",
        re.S,
    )
    s, count = pattern.subn(
        "\tpublic static boolean isPaidVersion(@NonNull OsmandApplication app) {\n\t\treturn true;\n\t}",
        s,
        count=1,
    )
    if count != 1:
        raise RuntimeError("Could not patch isPaidVersion")
    write(p, s)


def remove_permission(s: str, permission: str) -> str:
    pattern = re.compile(
        r"\n\s*<uses-permission\s+android:name=\"" + re.escape(permission) + r"\"[^>]*/>",
        re.S,
    )
    return pattern.sub("", s)


def patch_manifest() -> None:
    p = APP / "AndroidManifest.xml"
    s = read(p)

    # Nuwe Mapa itself is deliberately offline. Assisted GNSS may still be provided
    # by the Android location stack; that does not require this app to hold INTERNET.
    for permission in [
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE",
        "android.permission.ACCESS_WIFI_STATE",
        "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
        "androidx.car.app.MAP_TEMPLATES",
        "androidx.car.app.NAVIGATION_TEMPLATES",
        "androidx.car.app.ACCESS_SURFACE",
    ]:
        s = remove_permission(s, permission)

    s = s.replace('android:authorities="net.osmand.plus.fileprovider"', 'android:authorities="${applicationId}.fileprovider"')
    s = s.replace('android:allowBackup="true" android:backupAgent="net.osmand.plus.helpers.OsmandBackupAgent"', 'android:allowBackup="false"')
    s = s.replace('android:usesCleartextTraffic="true"', 'android:usesCleartextTraffic="false"')
    s = s.replace('android:icon="@mipmap/icon"', 'android:icon="@mipmap/nuwe_launcher" android:roundIcon="@mipmap/nuwe_launcher"', 1)

    s = re.sub(r"\n\s*<meta-data android:name=\"com\.google\.android\.backup\.api_key\"[^>]*/>", "", s)
    s = re.sub(r"\n\s*<receiver android:name=\"net\.osmand\.plus\.liveupdates\.LiveUpdatesAlarmReceiver\"[^>]*/>", "", s)
    s = re.sub(r"\n\s*<package android:name=\"net\.osmand\.nauticalPlugin\"\s*/>", "", s)
    s = re.sub(r"\n\s*<package android:name=\"com\.mapillary\.app\"\s*/>", "", s)

    # Remove Android Auto service declaration while leaving phone navigation intact.
    service_pattern = re.compile(
        r"\n\s*<service\b(?:(?!</service>).)*?android:name=\"net\.osmand\.plus\.auto\.NavigationCarAppService\"(?:(?!</service>).)*?</service>",
        re.S,
    )
    s = service_pattern.sub("", s)

    write(p, s)

    full = APP / "AndroidManifest-androidFull.xml"
    fs = read(full)
    fs = remove_permission(fs, "android.permission.REQUEST_INSTALL_PACKAGES")
    write(full, fs)


def patch_second_splash() -> None:
    p = APP / "src/net/osmand/SecondSplashScreenFragment.java"
    if not p.exists():
        return
    s = read(p)
    for old in [
        "R.drawable.ic_logo_splash_osmand_plus",
        "R.drawable.ic_logo_splash_osmand_dev",
        "R.drawable.ic_logo_splash_osmand",
    ]:
        s = s.replace(old, "R.drawable.nuwe_splash_logo")
    write(p, s)


def replace_color(text: str, name: str, value: str) -> str:
    pattern = re.compile(r'(<color\s+name="' + re.escape(name) + r'">).*?(</color>)')
    return pattern.sub(r"\1" + value + r"\2", text)


def patch_colors() -> None:
    # Cyan/teal glass palette: keeps OsmAnd's mature layouts, changes the visual language.
    values = APP / "res/values/colors.xml"
    s = read(values)
    palette = {
        "activity_background_color_light": "#EAF9FC",
        "activity_background_color_dark": "#06161E",
        "card_and_list_background_light": "#F4FDFF",
        "card_and_list_background_dark": "#0B222B",
        "list_background_color_light": "#F7FEFF",
        "list_background_color_dark": "#081B23",
        "widget_background_color_light": "#E8FAFE",
        "widget_background_color_dark": "#071A22",
        "icon_color_osmand_light": "#00A9C2",
        "icon_color_osmand_dark": "#55E7F4",
        "icon_color_active_light": "#009BB8",
        "icon_color_active_dark": "#52E2F0",
        "app_bar_main_light": "#35CBDC",
        "app_bar_main_dark": "#09232C",
        "app_bar_active_light": "#00A5C0",
        "app_bar_active_dark": "#0A627A",
        "status_bar_main_light": "#159AAF",
        "status_bar_main_dark": "#061820",
        "active_color_primary_light": "#00A7C4",
        "active_color_primary_dark": "#55E4F1",
        "active_color_secondary_light": "#CFF7FC",
        "active_color_secondary_dark": "#123E48",
        "active_color_primary_light_v2": "#00A7C4",
        "active_color_primary_dark_v2": "#55E4F1",
        "active_color_secondary_light_v2": "#D4F8FC",
        "active_color_secondary_dark_v2": "#123E48",
        "icon_color_active_light_v2": "#009AB8",
        "icon_color_active_dark_v2": "#55E4F1",
        "btn_bg_primary_light": "#00A7C4",
        "btn_bg_primary_dark": "#0B8198",
        "btn_bg_primary_pressed_light": "#008DA8",
        "btn_bg_primary_pressed_dark": "#086778",
        "btn_bg_accent_light": "#CFF7FC",
        "btn_bg_accent_dark": "#123E48",
        "splash_screen_background_color": "#DDF9FD",
    }
    for name, value in palette.items():
        s = replace_color(s, name, value)
    write(values, s)

    night = APP / "res/values-night/colors.xml"
    ns = read(night)
    ns = replace_color(ns, "splash_screen_background_color", "#061820")
    write(night, ns)


def build_icon(size: int) -> Image.Image:
    scale = size / 1024.0
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))

    # Soft shadow behind a glossy rounded glass tile.
    shadow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    m = int(72 * scale)
    radius = int(230 * scale)
    sd.rounded_rectangle((m, m + int(24 * scale), size - m, size - m + int(24 * scale)), radius=radius, fill=(0, 36, 55, 130))
    shadow = shadow.filter(ImageFilter.GaussianBlur(max(1, int(34 * scale))))
    img.alpha_composite(shadow)

    tile = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    mask = Image.new("L", (size, size), 0)
    md = ImageDraw.Draw(mask)
    md.rounded_rectangle((m, m, size - m, size - m), radius=radius, fill=255)

    gradient = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    gd = ImageDraw.Draw(gradient)
    top = (69, 230, 241, 255)
    bottom = (0, 93, 137, 255)
    for y in range(size):
        t = y / max(1, size - 1)
        c = tuple(int(top[i] * (1 - t) + bottom[i] * t) for i in range(4))
        gd.line((0, y, size, y), fill=c)
    gradient.putalpha(mask)
    tile.alpha_composite(gradient)

    td = ImageDraw.Draw(tile)
    inset = int(88 * scale)
    td.rounded_rectangle((inset, inset, size - inset, size - inset), radius=int(210 * scale), outline=(203, 255, 255, 180), width=max(1, int(12 * scale)))

    # Navigation arrow / stylised N path.
    arrow = [
        (int(512 * scale), int(178 * scale)),
        (int(736 * scale), int(746 * scale)),
        (int(520 * scale), int(636 * scale)),
        (int(374 * scale), int(796 * scale)),
        (int(430 * scale), int(548 * scale)),
        (int(286 * scale), int(408 * scale)),
    ]
    td.polygon(arrow, fill=(242, 255, 255, 250))
    td.line([arrow[0], arrow[1], arrow[2]], fill=(255, 255, 255, 255), width=max(1, int(10 * scale)))

    # Small green trail accent.
    td.arc(
        (int(270 * scale), int(560 * scale), int(760 * scale), int(850 * scale)),
        start=200,
        end=340,
        fill=(137, 255, 177, 230),
        width=max(2, int(26 * scale)),
    )

    # Fruit-Aero gloss: translucent highlight over upper half.
    gloss = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    g = ImageDraw.Draw(gloss)
    g.ellipse(
        (int(145 * scale), int(95 * scale), int(880 * scale), int(545 * scale)),
        fill=(255, 255, 255, 72),
    )
    gloss.putalpha(Image.composite(gloss.getchannel("A"), Image.new("L", (size, size), 0), mask))
    tile.alpha_composite(gloss)
    img.alpha_composite(tile)
    return img


def generate_branding() -> None:
    base = build_icon(1024)
    densities = {
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192,
    }
    for density, px in densities.items():
        out = APP / f"res/mipmap-{density}/nuwe_launcher.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        base.resize((px, px), Image.Resampling.LANCZOS).save(out)

    splash_sizes = {
        "mdpi": 104,
        "hdpi": 156,
        "xhdpi": 208,
        "xxhdpi": 312,
        "xxxhdpi": 416,
    }
    for density, px in splash_sizes.items():
        out = APP / f"res/drawable-{density}/nuwe_splash_logo.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        base.resize((px, px), Image.Resampling.LANCZOS).save(out)

    splash_xml = '''<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android" android:opacity="opaque">
    <item>
        <shape android:shape="rectangle">
            <gradient android:angle="270" android:startColor="#E9FCFF" android:centerColor="#D3F6FB" android:endColor="#A8E6F0" />
        </shape>
    </item>
    <item android:top="@dimen/splash_screen_logo_top">
        <bitmap android:gravity="center_horizontal|top" android:src="@drawable/nuwe_splash_logo" />
    </item>
</layer-list>
'''
    write(APP / "res/drawable/first_splash_screen_plus.xml", splash_xml)


def write_notice() -> None:
    text = '''Nuwe Mapa 0.1.0\n\nPersonal offline navigation build based on the GPL-licensed OsmAnd Android source.\nNuwe Mapa keeps OsmAnd copyright and license notices in the source tree.\nThis patch changes package/branding, disables store/in-app purchase paths, removes the app INTERNET permission and Android Auto manifest integration, and applies a cyan Fruit-Aero-inspired palette.\n\nCore use cases: car navigation, bicycle navigation and hiking/trails.\nVoice guidance: Android offline TTS / OsmAnd voice guidance infrastructure.\nMap data: local compatible OBF files, typically generated from OpenStreetMap data.\n'''
    write(ROOT / "NUWE_MAPA_BUILD_NOTICE.txt", text)


def main() -> None:
    if not APP.exists():
        raise SystemExit(f"OsmAnd app directory not found: {APP}")
    patch_gradle()
    patch_version()
    patch_manifest()
    patch_second_splash()
    patch_colors()
    generate_branding()
    write_notice()
    print("Nuwe Mapa patches applied successfully")


if __name__ == "__main__":
    main()
