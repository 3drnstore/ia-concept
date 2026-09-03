from pathlib import Path

path = Path("src/nyra/ui/ConceptShell.qml")
text = path.read_text(encoding="utf-8")

replacements = [
    (
        'CButton{Layout.fillWidth:true;text:"＋ Nova demanda";accent:true}}\n                }\n                Rectangle{Layout.fillWidth:true;implicitHeight:545',
        'CButton{Layout.fillWidth:true;text:"＋ Nova demanda";accent:true}}\n                }\n                }\n                Rectangle{Layout.fillWidth:true;implicitHeight:545',
    ),
    (
        'Mono{text:modelData.s}}}}\n                }\n                Rectangle{Layout.fillWidth:true;implicitHeight:420',
        'Mono{text:modelData.s}}}}\n                }\n                }\n                Rectangle{Layout.fillWidth:true;implicitHeight:420',
    ),
    (
        'Label{text:modelData.v;color:root.ink;font.pixelSize:14}}}}}\n                Item{Layout.preferredHeight:12}',
        'Label{text:modelData.v;color:root.ink;font.pixelSize:14}}}}}\n                }\n                Item{Layout.preferredHeight:12}',
    ),
]

changed = False
for old, new in replacements:
    if old in text:
        text = text.replace(old, new, 1)
        changed = True

if not changed:
    print("ConceptShell.qml already patched or expected patterns not found")
else:
    path.write_text(text, encoding="utf-8")
    print("ConceptShell.qml compact block closures repaired")
