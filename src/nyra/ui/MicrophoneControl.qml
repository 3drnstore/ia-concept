pragma ComponentBehavior: Bound
import QtQuick

Item {
    id: control
    implicitWidth: 82
    implicitHeight: 66
    property real level: audioMonitor.listening ? audioMonitor.level : 0
    property color tint: audioMonitor.listening ? "#35e788" : "#19e7f3"
    property real phase: 0
    // Only captured sound advances the waves. Silence never drives a loop.
    Timer {
        interval: 16; repeat: true; running: control.level > 0
        onTriggered: control.phase = (control.phase + 0.016 / 1.15) % 1
    }
    onLevelChanged: if (level === 0) phase = 0
    Repeater {
        model: 2
        Rectangle {
            required property int index
            property real progress: (control.phase + index * 0.5) % 1
            anchors.centerIn: parent
            width: 38 + progress * (20 + control.level * 24)
            height: width; radius: width / 2
            color: "transparent"; border.width: 1 + control.level
            border.color: control.tint
            opacity: control.level > 0 ? (1 - progress) * control.level : 0
        }
    }
    Rectangle {
        anchors.centerIn: parent; width: 36; height: 36; radius: 18
        color: "#c20a5363"; border.color: control.tint
        // Neither the center nor its icon is scaled, rotated or translated.
        Rectangle { anchors.horizontalCenter: parent.horizontalCenter; y: 8; width: 7; height: 12; radius: 4; color: "#d5ffff" }
        Rectangle { x: 11; y: 14; width: 14; height: 11; radius: 7; color: "transparent"; border.color: "#d5ffff"; border.width: 1 }
        Rectangle { x: 13; y: 12; width: 10; height: 7; color: "#0a5363" }
        Rectangle { x: 17; y: 24; width: 2; height: 5; color: "#d5ffff" }
        Rectangle { x: 13; y: 28; width: 10; height: 1; color: "#d5ffff" }
    }
    MouseArea {
        anchors.fill: parent; cursorShape: Qt.PointingHandCursor
        onClicked: if (localVoice.microphoneAllowed) audioMonitor.setListening(!audioMonitor.listening)
    }
}
