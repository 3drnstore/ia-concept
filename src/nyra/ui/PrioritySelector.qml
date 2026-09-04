pragma ComponentBehavior: Bound
import QtQuick
import QtQuick.Controls

ComboBox {
    id: control
    model: ["NORMAL", "ALTA", "BAIXA"]
    implicitHeight: 40
    palette.highlight: "#285d73"
    palette.highlightedText: "#e9faff"
    contentItem: Text {
        text: control.displayText; color: "#e9f4f7"; leftPadding: 12; rightPadding: 28
        verticalAlignment: Text.AlignVCenter; font.pixelSize: 13
    }
    background: Rectangle {
        radius: 7; color: control.down || control.activeFocus ? "#234f65" : "#102e40"
        border.color: control.activeFocus || control.hovered ? "#6ad4e7" : "#386375"
    }
    indicator: Text { x: control.width - 24; anchors.verticalCenter: parent.verticalCenter; text: "▾"; color: "#bdefff" }
    delegate: ItemDelegate {
        id: option
        required property int index
        required property string modelData
        width: control.width; height: 38
        highlighted: control.highlightedIndex === index
        contentItem: Text { text: option.modelData; color: "#e9f4f7"; verticalAlignment: Text.AlignVCenter }
        background: Rectangle { radius: 5; color: option.highlighted || option.hovered ? "#285d73" : control.currentIndex === option.index ? "#20475d" : "#102e40" }
    }
    popup: Popup {
        objectName: "priorityPopup"
        y: control.height + 4; width: control.width; padding: 4
        implicitHeight: contentItem.implicitHeight + 8
        background: Rectangle { radius: 7; color: "#102e40"; border.color: "#58899b" }
        contentItem: ListView { clip: true; implicitHeight: contentHeight; model: control.popup.visible ? control.delegateModel : null; currentIndex: control.highlightedIndex }
    }
}
