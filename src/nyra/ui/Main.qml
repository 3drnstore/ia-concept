import QtQuick
import QtQuick.Controls
import QtQuick.Layouts

ApplicationWindow {
    id: root
    width: 1440
    height: 860
    minimumWidth: 760
    minimumHeight: 620
    visible: true
    title: "Nyra — Personal Local Intelligence"
    color: "#060a0d"

    property bool compact: width < 1120
    property color bg: "#060a0d"
    property color panel: "#0d151a"
    property color panel2: "#101b20"
    property color soft: "#0a1115"
    property color line: "#20353c"
    property color cyan: "#69d9dc"
    property color cyanDim: "#1b5055"
    property color textMain: "#e5f0f1"
    property color textMuted: "#83979c"
    property color danger: "#d47178"

    font.family: "Segoe UI"

    component Panel: Rectangle {
        radius: 16
        color: root.panel
        border.color: root.line
        border.width: 1
    }

    component SmallLabel: Label {
        color: root.textMuted
        font.family: "Consolas"
        font.pixelSize: 10
        font.letterSpacing: 1.0
    }

    component NavButton: Button {
        id: nav
        property bool active: false
        property string symbol: "◉"
        implicitHeight: 42
        leftPadding: 12
        rightPadding: 12
        background: Rectangle {
            radius: 10
            color: nav.active ? "#14343a" : (nav.hovered ? "#101e23" : "transparent")
            border.color: nav.active ? root.cyanDim : "transparent"
        }
        contentItem: RowLayout {
            spacing: 9
            Label { text: nav.symbol; color: nav.active ? root.cyan : root.textMuted; font.pixelSize: 13 }
            Label { text: nav.text; color: nav.active ? root.textMain : root.textMuted; font.pixelSize: 13; Layout.fillWidth: true }
        }
    }

    component TaskCard: Rectangle {
        id: taskCard
        required property var taskData
        property bool selected: taskStore.selectedTask && taskStore.selectedTask.id === taskData.id
        radius: 12
        color: selected ? "#13262b" : root.soft
        border.color: selected ? root.cyanDim : root.line
        border.width: 1
        implicitHeight: 92

        MouseArea {
            anchors.fill: parent
            cursorShape: Qt.PointingHandCursor
            onClicked: taskStore.selectTask(taskCard.taskData.id)
        }

        ColumnLayout {
            anchors.fill: parent
            anchors.margins: 12
            spacing: 6
            RowLayout {
                Layout.fillWidth: true
                Label {
                    text: taskCard.taskData.title
                    color: root.textMain
                    font.pixelSize: 13
                    font.bold: true
                    elide: Text.ElideRight
                    Layout.fillWidth: true
                }
                SmallLabel { text: String(taskCard.taskData.id).padStart(2, "0") }
            }
            RowLayout {
                Layout.fillWidth: true
                spacing: 8
                Label {
                    text: taskCard.taskData.priority === "ALTA" ? "▲ ALTA" : taskCard.taskData.status
                    color: taskCard.taskData.priority === "ALTA" ? root.danger : root.textMuted
                    font.family: "Consolas"
                    font.pixelSize: 9
                }
                Item { Layout.fillWidth: true }
                SmallLabel { text: taskCard.taskData.dueText || "Sem prazo" }
            }
        }
    }

    component AssistantCore: Item {
        implicitWidth: 120
        implicitHeight: 120
        Rectangle {
            anchors.centerIn: parent
            width: 116; height: 116; radius: 58
            color: "#081216"
            border.color: "#285860"
            border.width: 1
        }
        Rectangle {
            id: ring
            anchors.centerIn: parent
            width: 86; height: 86; radius: 43
            color: "transparent"
            border.color: "#397d84"
            border.width: 1
            opacity: 0.65
            RotationAnimator on rotation {
                from: 0; to: 360; duration: 9000; loops: Animation.Infinite
            }
        }
        Rectangle {
            anchors.centerIn: parent
            width: 50; height: 50; radius: 25
            gradient: Gradient {
                GradientStop { position: 0.0; color: "#b8ffff" }
                GradientStop { position: 0.2; color: "#5fcfd4" }
                GradientStop { position: 0.55; color: "#174149" }
                GradientStop { position: 1.0; color: "#071014" }
            }
            SequentialAnimation on scale {
                loops: Animation.Infinite
                NumberAnimation { to: 1.07; duration: 1200; easing.type: Easing.InOutQuad }
                NumberAnimation { to: 0.96; duration: 1200; easing.type: Easing.InOutQuad }
            }
        }
        Rectangle { anchors.centerIn: parent; width: 6; height: 6; radius: 3; color: "#e8ffff" }
    }

    component LeftPanel: Panel {
        implicitWidth: 210
        ColumnLayout {
            anchors.fill: parent
            anchors.margins: 14
            spacing: 8

            ColumnLayout {
                spacing: 1
                Label { text: "NEXUS //"; color: root.textMain; font.pixelSize: 16; font.bold: true }
                Label { text: "COMMAND CORE"; color: root.cyan; font.pixelSize: 15; font.bold: true }
                SmallLabel { text: "● LOCAL AI ONLINE"; color: root.cyan }
            }

            Item { Layout.preferredHeight: 10 }
            NavButton { text: "Hoje"; symbol: "◉"; active: true; Layout.fillWidth: true }
            NavButton { text: "Demandas"; symbol: "▦"; Layout.fillWidth: true }
            NavButton { text: "Memória"; symbol: "◇"; Layout.fillWidth: true }
            NavButton { text: "Arquivos"; symbol: "⌁"; Layout.fillWidth: true }
            NavButton { text: "Rotinas"; symbol: "↻"; Layout.fillWidth: true }
            NavButton { text: "Configurações"; symbol: "⚙"; Layout.fillWidth: true }

            Item { Layout.fillHeight: true }

            Repeater {
                model: ["CPU // NORMAL", "VOICE // READY", "LOCAL DB // SYNC"]
                Rectangle {
                    Layout.fillWidth: true
                    implicitHeight: 36
                    radius: 9
                    color: root.soft
                    border.color: root.line
                    SmallLabel { anchors.centerIn: parent; text: modelData }
                }
            }

            Button {
                text: "+ Nova demanda"
                Layout.fillWidth: true
                implicitHeight: 46
                onClicked: newTaskDialog.open()
                background: Rectangle { radius: 11; color: root.cyanDim; border.color: "#33747b" }
                contentItem: Label { text: parent.text; color: root.textMain; font.bold: true; horizontalAlignment: Text.AlignHCenter; verticalAlignment: Text.AlignVCenter }
            }
        }
    }

    component CenterPanel: Item {
        ColumnLayout {
            anchors.fill: parent
            spacing: 12

            Panel {
                Layout.fillWidth: true
                implicitHeight: 218
                ColumnLayout {
                    anchors.fill: parent
                    anchors.margins: 16
                    spacing: 10
                    RowLayout {
                        Layout.fillWidth: true
                        ColumnLayout {
                            spacing: 2
                            Label { text: "Boa noite."; color: root.textMain; font.pixelSize: 23; font.bold: true }
                            Label { text: "Estas são as coisas que merecem sua atenção agora."; color: root.textMuted; font.pixelSize: 12 }
                        }
                        Item { Layout.fillWidth: true }
                        SmallLabel { text: taskStore.tasks.length + " DEMANDAS" }
                    }

                    ScrollView {
                        Layout.fillWidth: true
                        Layout.fillHeight: true
                        clip: true
                        ScrollBar.vertical.policy: ScrollBar.AlwaysOff
                        ScrollBar.horizontal.policy: ScrollBar.AsNeeded
                        RowLayout {
                            spacing: 10
                            Repeater {
                                model: taskStore.tasks
                                TaskCard {
                                    taskData: modelData
                                    Layout.preferredWidth: 210
                                }
                            }
                        }
                    }
                }
            }

            Panel {
                Layout.fillWidth: true
                Layout.fillHeight: true
                ColumnLayout {
                    anchors.fill: parent
                    anchors.margins: 17
                    spacing: 12

                    RowLayout {
                        Layout.fillWidth: true
                        ColumnLayout {
                            spacing: 3
                            SmallLabel { text: "DEMANDA ATUAL" }
                            Label {
                                text: taskStore.selectedTask.title || "Nenhuma demanda"
                                color: root.textMain
                                font.pixelSize: 20
                                font.bold: true
                            }
                        }
                        Item { Layout.fillWidth: true }
                        Rectangle {
                            implicitWidth: 80; implicitHeight: 28; radius: 8
                            color: "#10251e"; border.color: "#235744"
                            SmallLabel { anchors.centerIn: parent; text: taskStore.selectedTask.status || "—"; color: "#84d7a9" }
                        }
                    }

                    RowLayout {
                        Layout.fillWidth: true
                        spacing: 10
                        Repeater {
                            model: [
                                {k: "PRAZO", v: taskStore.selectedTask.dueText || "Sem prazo"},
                                {k: "ANEXOS", v: "0 arquivos"},
                                {k: "IA", v: "NYRA (Local)"}
                            ]
                            Rectangle {
                                Layout.fillWidth: true
                                implicitHeight: 65
                                radius: 10
                                color: root.soft
                                border.color: root.line
                                ColumnLayout {
                                    anchors.fill: parent; anchors.margins: 10; spacing: 3
                                    SmallLabel { text: modelData.k }
                                    Label { text: modelData.v; color: root.textMain; font.pixelSize: 12 }
                                }
                            }
                        }
                    }

                    Rectangle {
                        Layout.fillWidth: true
                        implicitHeight: 90
                        radius: 11
                        color: root.soft
                        border.color: root.line
                        ColumnLayout {
                            anchors.fill: parent; anchors.margins: 12; spacing: 5
                            SmallLabel { text: "DESCRIÇÃO" }
                            Label {
                                text: taskStore.selectedTask.description || "Sem descrição."
                                color: root.textMuted
                                wrapMode: Text.WordWrap
                                Layout.fillWidth: true
                                font.pixelSize: 12
                            }
                        }
                    }

                    Rectangle {
                        Layout.fillWidth: true
                        Layout.fillHeight: true
                        radius: 11
                        color: "#0d191d"
                        border.color: "#24434a"
                        ColumnLayout {
                            anchors.fill: parent; anchors.margins: 12; spacing: 8
                            RowLayout {
                                SmallLabel { text: "● ASSISTANT INSIGHT"; color: root.cyan }
                                Item { Layout.fillWidth: true }
                                SmallLabel { text: "LOCAL" }
                            }
                            Label {
                                Layout.fillWidth: true
                                text: "Eu já tenho o contexto desta demanda localmente. Posso organizar o que está pendente, apontar o próximo passo e conversar com você sem acessar a internet."
                                color: root.textMuted
                                wrapMode: Text.WordWrap
                                font.pixelSize: 12
                            }
                            Item { Layout.fillHeight: true }
                            RowLayout {
                                Button {
                                    text: "Analisar agora"
                                    background: Rectangle { radius: 9; color: root.cyanDim; border.color: "#33747b" }
                                    contentItem: Label { text: parent.text; color: root.textMain; horizontalAlignment: Text.AlignHCenter; verticalAlignment: Text.AlignVCenter }
                                }
                                Button {
                                    text: "Conversar"
                                    onClicked: assistantMessage.text = "Estou ouvindo. Pode falar comigo normalmente."
                                    background: Rectangle { radius: 9; color: root.soft; border.color: root.line }
                                    contentItem: Label { text: parent.text; color: root.textMuted; horizontalAlignment: Text.AlignHCenter; verticalAlignment: Text.AlignVCenter }
                                }
                                Item { Layout.fillWidth: true }
                                Button {
                                    text: taskStore.selectedTask.status === "CONCLUÍDA" ? "Reabrir" : "Concluir"
                                    onClicked: taskStore.toggleDone(taskStore.selectedTask.id)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    component AssistantPanel: Panel {
        implicitWidth: 270
        ColumnLayout {
            anchors.fill: parent
            anchors.margins: 15
            spacing: 10
            Item { Layout.fillWidth: true; implicitHeight: 8 }
            AssistantCore { Layout.alignment: Qt.AlignHCenter; Layout.preferredWidth: 120; Layout.preferredHeight: 120 }
            Label { text: "NYRA"; color: root.textMain; font.pixelSize: 20; font.bold: true; Layout.alignment: Qt.AlignHCenter }
            SmallLabel { text: "PERSONAL LOCAL INTELLIGENCE"; Layout.alignment: Qt.AlignHCenter }

            Rectangle {
                Layout.fillWidth: true
                implicitHeight: 54
                radius: 10
                color: root.soft; border.color: root.line
                RowLayout {
                    anchors.fill: parent; anchors.margins: 10
                    ColumnLayout {
                        spacing: 2
                        SmallLabel { text: "ESTADO // DISPONÍVEL"; color: root.cyan }
                        SmallLabel { text: "NETWORK // LOCKED" }
                    }
                    Item { Layout.fillWidth: true }
                    Label { text: "◉"; color: root.cyan; font.pixelSize: 18 }
                }
            }

            Rectangle {
                Layout.fillWidth: true
                Layout.fillHeight: true
                radius: 11
                color: root.soft
                border.color: root.line
                ColumnLayout {
                    anchors.fill: parent; anchors.margins: 12; spacing: 8
                    SmallLabel { text: "CONVERSA" }
                    Label {
                        id: assistantMessage
                        Layout.fillWidth: true
                        text: "Estou aqui. Pode falar comigo normalmente."
                        color: root.textMain
                        wrapMode: Text.WordWrap
                        font.pixelSize: 12
                    }
                    Item { Layout.fillHeight: true }
                    Rectangle {
                        Layout.fillWidth: true
                        implicitHeight: 92
                        radius: 9
                        color: "#0b171b"; border.color: "#1d3940"
                        ColumnLayout {
                            anchors.fill: parent; anchors.margins: 10; spacing: 4
                            SmallLabel { text: "MEMÓRIA RECENTE" }
                            Label { text: "• Internet bloqueada por padrão"; color: root.textMuted; font.pixelSize: 10 }
                            Label { text: "• Pedir autorização por voz"; color: root.textMuted; font.pixelSize: 10 }
                            Label { text: "• Contexto local persistente"; color: root.textMuted; font.pixelSize: 10 }
                        }
                    }
                }
            }
        }
    }

    header: Rectangle {
        height: root.compact ? 58 : 50
        color: "#080d10"
        border.color: root.line
        RowLayout {
            anchors.fill: parent
            anchors.leftMargin: 18
            anchors.rightMargin: 18
            Label { text: "N.Y.R.A."; color: root.textMain; font.pixelSize: 15; font.bold: true }
            SmallLabel { text: "NEURAL YIELDING REASONING ASSISTANT"; visible: !root.compact }
            Item { Layout.fillWidth: true }
            SmallLabel { text: root.compact ? "COMPACT MODE" : "DESKTOP MODE"; color: root.cyan }
        }
    }

    StackLayout {
        anchors.fill: parent
        anchors.margins: 12
        currentIndex: root.compact ? 1 : 0

        RowLayout {
            spacing: 12
            LeftPanel { Layout.preferredWidth: 210; Layout.fillHeight: true }
            CenterPanel { Layout.fillWidth: true; Layout.fillHeight: true }
            AssistantPanel { Layout.preferredWidth: 270; Layout.fillHeight: true }
        }

        Flickable {
            clip: true
            contentWidth: width
            contentHeight: compactColumn.implicitHeight
            ScrollBar.vertical: ScrollBar {}

            ColumnLayout {
                id: compactColumn
                width: parent.width
                spacing: 12

                Panel {
                    Layout.fillWidth: true
                    implicitHeight: 145
                    RowLayout {
                        anchors.fill: parent; anchors.margins: 14; spacing: 14
                        AssistantCore { Layout.preferredWidth: 110; Layout.preferredHeight: 110 }
                        ColumnLayout {
                            Layout.fillWidth: true
                            Label { text: "NYRA"; color: root.textMain; font.pixelSize: 21; font.bold: true }
                            SmallLabel { text: "PERSONAL LOCAL INTELLIGENCE" }
                            Label { text: "Estou aqui. Pode falar comigo normalmente."; color: root.textMuted; wrapMode: Text.WordWrap; Layout.fillWidth: true; font.pixelSize: 12 }
                            SmallLabel { text: "● LOCAL CORE ONLINE  •  NETWORK LOCKED"; color: root.cyan }
                        }
                    }
                }

                Panel {
                    Layout.fillWidth: true
                    implicitHeight: Math.min(330, 120 + taskStore.tasks.length * 100)
                    ColumnLayout {
                        anchors.fill: parent; anchors.margins: 14; spacing: 8
                        RowLayout {
                            Layout.fillWidth: true
                            Label { text: "Hoje"; color: root.textMain; font.pixelSize: 18; font.bold: true }
                            Item { Layout.fillWidth: true }
                            Button { text: "+ Nova"; onClicked: newTaskDialog.open() }
                        }
                        ListView {
                            Layout.fillWidth: true
                            Layout.fillHeight: true
                            spacing: 8
                            clip: true
                            model: taskStore.tasks
                            delegate: TaskCard { width: ListView.view.width; taskData: modelData }
                        }
                    }
                }

                Panel {
                    Layout.fillWidth: true
                    implicitHeight: 310
                    ColumnLayout {
                        anchors.fill: parent; anchors.margins: 14; spacing: 10
                        SmallLabel { text: "DEMANDA ATUAL" }
                        Label { text: taskStore.selectedTask.title || "Nenhuma demanda"; color: root.textMain; font.pixelSize: 19; font.bold: true }
                        Label { text: taskStore.selectedTask.description || "Sem descrição."; color: root.textMuted; wrapMode: Text.WordWrap; Layout.fillWidth: true; font.pixelSize: 12 }
                        Rectangle {
                            Layout.fillWidth: true; Layout.fillHeight: true; radius: 10; color: root.soft; border.color: root.line
                            Label {
                                anchors.fill: parent; anchors.margins: 12
                                text: "NYRA // Eu posso analisar esta demanda usando apenas o contexto local e sugerir o próximo passo."
                                color: root.textMuted; wrapMode: Text.WordWrap; font.pixelSize: 12
                            }
                        }
                        RowLayout {
                            Button { text: "Concluir"; onClicked: taskStore.toggleDone(taskStore.selectedTask.id) }
                            Button { text: "Conversar"; onClicked: assistantMessage.text = "Estou ouvindo." }
                            Item { Layout.fillWidth: true }
                        }
                    }
                }
            }
        }
    }

    footer: Rectangle {
        height: 58
        color: "#080d10"
        border.color: root.line
        RowLayout {
            anchors.fill: parent; anchors.leftMargin: 16; anchors.rightMargin: 16; spacing: 10
            Label { text: "⌁"; color: root.cyan; font.pixelSize: 18 }
            TextField {
                Layout.fillWidth: true
                placeholderText: "Fale ou digite um comando..."
                color: root.textMain
                placeholderTextColor: root.textMuted
                background: Rectangle { radius: 10; color: root.soft; border.color: root.line }
                onAccepted: {
                    if (text.trim().length > 0) {
                        assistantMessage.text = "Recebi: “" + text.trim() + "”. O cérebro local será conectado na próxima etapa."
                        text = ""
                    }
                }
            }
            Rectangle {
                width: 40; height: 40; radius: 20
                color: root.cyanDim; border.color: "#33747b"
                Label { anchors.centerIn: parent; text: "●"; color: root.cyan; font.pixelSize: 17 }
            }
        }
    }

    Dialog {
        id: newTaskDialog
        title: "Nova demanda"
        modal: true
        anchors.centerIn: Overlay.overlay
        standardButtons: Dialog.Ok | Dialog.Cancel
        width: Math.min(root.width - 40, 520)

        onAccepted: {
            taskStore.addTask(taskTitle.text, taskDescription.text, taskPriority.currentText, taskDue.text)
            taskTitle.text = ""
            taskDescription.text = ""
            taskDue.text = ""
            taskPriority.currentIndex = 0
        }

        contentItem: ColumnLayout {
            spacing: 10
            TextField { id: taskTitle; Layout.fillWidth: true; placeholderText: "Título" }
            TextArea { id: taskDescription; Layout.fillWidth: true; Layout.preferredHeight: 100; placeholderText: "Descrição"; wrapMode: TextEdit.WordWrap }
            RowLayout {
                Layout.fillWidth: true
                ComboBox { id: taskPriority; model: ["NORMAL", "ALTA", "BAIXA"]; Layout.preferredWidth: 140 }
                TextField { id: taskDue; Layout.fillWidth: true; placeholderText: "Prazo (ex.: amanhã 09:00)" }
            }
        }
    }
}
