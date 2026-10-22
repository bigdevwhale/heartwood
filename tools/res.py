"""Writes the small hand-made resources: theme, colours, launcher icon, notification icon and the camera's file paths.

python tools/res.py
"""
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "app/src/main/res"


def rings(cx, cy, radii, color, width):
    return "\n".join(
        f'    <path android:fillColor="#00000000" android:strokeColor="{color}" android:strokeWidth="{width}" '
        f'android:pathData="M{cx - r},{cy}a{r},{r} 0,1 1,{2 * r},0a{r},{r} 0,1 1,{-2 * r},0z" />'
        for r in radii
    )


def disc(cx, cy, r, color):
    return f'    <path android:fillColor="{color}" android:pathData="M{cx - r},{cy}a{r},{r} 0,1 1,{2 * r},0a{r},{r} 0,1 1,{-2 * r},0z" />'


# A sawn cross-section: bark, sapwood, rings in the season colours, the pith a little off centre.
CUT = "\n".join([
    disc(54, 54, 31, "#4A3626"),
    disc(54, 54, 27.5, "#DCC395"),
    disc(53.4, 54.6, 24, "#E9D3A6"),
    rings(53.4, 54.6, [22.5], "#A8602A", 1.3),
    rings(53.2, 54.8, [19.5], "#5F7D3C", 1.2),
    rings(53, 55, [16.8], "#5F7488", 1.2),
    rings(52.9, 55.1, [14.2], "#7F9A55", 1.1),
    rings(52.8, 55.2, [11.6], "#A8602A", 1.1),
    rings(52.7, 55.3, [9.2], "#2F2A24", 0.9),
    rings(52.6, 55.4, [6.8], "#5F7D3C", 1.0),
    rings(52.5, 55.5, [4.4], "#A8602A", 1.0),
    disc(52.4, 55.6, 2.2, "#7A4F2D"),
    '    <path android:fillColor="#00000000" android:strokeColor="#2C2017" android:strokeWidth="1.2" android:strokeLineCap="round" android:pathData="M78,40l-5,3l-3,1" />',
])

MONO = "\n".join([
    rings(54, 54, [29.5], "#FFFFFF", 3),
    rings(53.4, 54.6, [22.5, 16.8, 11.6, 6.8], "#FFFFFF", 2),
    disc(52.6, 55.4, 2.4, "#FFFFFF"),
])

FILES = {
    "values/themes.xml": """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Heartwood" parent="Theme.AppCompat.Light.NoActionBar">
        <item name="android:windowBackground">@color/window</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="colorPrimary">@color/moss</item>
        <item name="colorAccent">@color/moss</item>
    </style>
</resources>
""",
    "values/colors.xml": """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="window">#FFD8C39D</color>
    <color name="moss">#FF55703A</color>
</resources>
""",
    "values/strings.xml": """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Heartwood</string>
    <string name="channel_calls">Calls from the trees</string>
</resources>
""",
    "values-ru/strings.xml": """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Сердцевина</string>
    <string name="channel_calls">Зов деревьев</string>
</resources>
""",
    "mipmap-anydpi-v26/ic_launcher.xml": """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_monochrome" />
</adaptive-icon>
""",
    "drawable/ic_launcher_background.xml": """<?xml version="1.0" encoding="utf-8"?>
<!-- Kraft paper with a few fibres. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#D8C39D" android:pathData="M0,0h108v108h-108z" />
    <path android:fillColor="#00000000" android:strokeColor="#22A98D60" android:strokeWidth="0.8" android:pathData="M10,20l14,1M70,12l12,-1M18,88l16,1M80,92l12,0M88,30l10,1M6,60l9,0" />
</vector>
""",
    "drawable/ic_launcher_foreground.xml": f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
{CUT}
</vector>
""",
    "drawable/ic_monochrome.xml": f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
{MONO}
</vector>
""",
    "drawable/ic_note.xml": """<?xml version="1.0" encoding="utf-8"?>
<!-- Notification icon: tree rings. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#00000000" android:strokeColor="#FFFFFFFF" android:strokeWidth="2" android:pathData="M3,12a9,9 0,1 1,18 0a9,9 0,1 1,-18 0z" />
    <path android:fillColor="#00000000" android:strokeColor="#FFFFFFFF" android:strokeWidth="1.6" android:pathData="M6.5,12.3a5.3,5.3 0,1 1,10.6 0a5.3,5.3 0,1 1,-10.6 0z" />
    <path android:fillColor="#FFFFFFFF" android:pathData="M10.3,12.6a1.5,1.5 0,1 1,3 0a1.5,1.5 0,1 1,-3 0z" />
</vector>
""",
    "xml/file_paths.xml": """<?xml version="1.0" encoding="utf-8"?>
<!-- The phone's camera writes the tree's photos, leaves and bark here. -->
<paths>
    <files-path name="photos" path="photos/" />
    <files-path name="leaves" path="leaves/" />
    <files-path name="tmp" path="tmp/" />
</paths>
""",
}


def main():
    for name, text in FILES.items():
        f = RES / name
        f.parent.mkdir(parents=True, exist_ok=True)
        f.write_text(text, encoding="utf-8", newline="\n")


if __name__ == "__main__":
    main()
