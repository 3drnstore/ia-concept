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
    property color panel: "#0d151a"
    property color soft: "#0a1115"
    property color line: "#20353c"
    property color cyan: "#69d9dc"
    property color cyanDim: "#1b5055"
    property color textMain: "#e5f0f1"
    property color textMuted: "#83979c"
    property color danger: "#d47178"
    property string assistantText: "Estou aqui. Pode falar comigo normalmente."

    font.family: "Segoe UI"

    component Panel: Rectangle {
        radius: 16
        color: root.panel
        border.color: root.line
        border.width: 1
    }

    component Tiny: Label {
        color: root.textMuted
        font.family: "Consolas"
        font.pixelSize: 10
        font.letterSpacing: 1.0
    }

    component CoreOrb: Item {
        implicitWidth: 118
        implicitHeight: 118
        Rectangle {
            anchors.centerIn: parent
            width: 114; height: 114; radius: 57
            color: "#081216"; border.color: "#285860"
        }
        Rectangle {
            anchors.centerIn: parent
            width: 84; height: 84; radius: 42
            color: "transparent"; border.color: "#397d84"; opacity: .7
            RotationAnimator on rotation { from: 0; to: 360; duration: 9000; loops: Animation.Infinite }
        }
        Rectangle {
            anchors.centerIn: parent
            width: 50; height: 50; radius: 25
            gradient: Gradient {
                GradientStop { position: 0.0; color: "#cfffff" }
                GradientStop { position: 0.25; color: "#5fcfd4" }
                GradientStop { position: 0.62; color: "#174149" }
                GradientStop { position: 1.0; color: "#071014" }
            }
            SequentialAnimation on scale {
                loops: Animation.Infinite
                NumberAnimation { to: 1.07; duration: 1200; easing.type: Easing.InOutQuad }
                NumberAnimation { to: .96; duration: 1200; easing.type: Easing.InOutQuad }
            }
        }
        Rectangle { anchors.centerIn: parent; width: 6; height: 6; radius: 3; color: "#efffff" }
    }

    component TaskCard: Rectangle {
        id: taskCard
        required property var taskData
        property bool selected: taskStore.selectedTask && taskStore.selectedTask.id === taskData.id
        radius: 12
        color: selected ? "#13262b" : root.soft
        border.color: selected ? root.cyanDim : root.line
        border.width: 1
        implicitHeight: 88

        MouseArea {
            anchors.fill: parent
            cursorShape: Qt.PointingHandCursor
            onClicked: taskStore.selectTask(taskCard.taskData.id)
        }
        ColumnLayout {
            anchors.fill: parent
            anchors.margins: 11
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
                Tiny { text: String(taskCard.taskData.id).padStart(2, "0") }
            }
            RowLayout {
                Layout.fillWidth: true
                Label {
                    text: taskCard.taskData.priority === "ALTA" ? "▲ ALTA" : taskCard.taskData.status
                    color: taskCard.taskData.priority === "ALTA" ? root.danger : root.textMuted
                    font.family: "Consolas"; font.pixelSize: 9
                }
                Item { Layout.fillWidth: true }
                Tiny { text: taskCard.taskData.dueText || "Sem prazo" }
            }
        }
    }

    component LeftRail: Panel {
        implicitWidth: 210
        ColumnLayout {
            anchors.fill: parent
            anchors.margins: 14
            spacing: 8

            Label { text: "NEXUS //"; color: root.textMain; font.pixelSize: 16; font.bold: true }
            Label { text: "COMMAND CORE"; color: root.cyan; font.pixelSize: 15; font.bold: true }
            Tiny { text: "● LOCAL AI ONLINE"; color: root.cyan }
            Item { Layout.preferredHeight: 10 }

            Repeater {
                model: ["◉  Hoje", "▦  Demandas", "◇  Memória", "⌁  Arquivos", "↻  Rotinas", "⚙  Configurações"]
                Button {
                    Layout.fillWidth: true
                    implicitHeight: 40
                    text: modelData
                    background: Rectangle {
                        radius: 9
                        color: index === 0 ? "#14343a" : (parent.hovered ? "#101e23" : "transparent")
                        border.color: index === 0 ? root.cyanDim : "transparent"
                    }
                    contentItem: Label {
                        text: parent.text
                        color: index === 0 ? root.textMain : root.textMuted
                        font.pixelSize: 12
                        verticalAlignment: Text.AlignVCenter
                    }
                }
            }

            Item { Layout.fillHeight: true }

            Repeater {
                model: ["CPU // NORMAL", "VOICE // READY", "LOCAL DB // SYNC"]
                Rectangle {
                    Layout.fillWidth: true; implicitHeight: 34; radius: 8
                    color: root.soft; border.color: root.line
                    Tiny { anchors.centerIn: parent; text: modelData }
                }
            }

            Button {
                Layout.fillWidth: true; implicitHeight: 46
                text: "+ Nova demanda"
                onClicked: newTaskDialog.open()
                background: Rectangle { radius: 10; color: root.cyanDim; border.color: "#33747b" }
                contentItem: Label { text: parent.text; color: root.textMain; font.bold: true; horizontalAlignment: Text.AlignHCenter; verticalAlignment: Text.AlignVCenter }
            }
        }
    }

    component WorkArea: Item {
        ColumnLayout {
            anchors.fill: parent
            spacing: 12

            Panel {
                Layout.fillWidth: true
                implicitHeight: 215
                ColumnLayout {
                    anchors.fill: parent; anchors.margins: 16; spacing: 10
                    RowLayout {
                        Layout.fillWidth: true
                        ColumnLayout {
                            spacing: 2
                            Label { text: "Boa noite."; color: root.textMain; font.pixelSize: 23; font.bold: true }
                            Label { text: "Estas são as coisas que merecem sua atenção agora."; color: root.textMuted; font.pixelSize: 12 }
                        }
                        Item { Layout.fillWidth: true }
                        Tiny { text: taskStore.tasks.length + " DEMANDAS" }
                    }
                    ScrollView {
                        Layout.fillWidth: true; Layout.fillHeight: true; clip: true
                        ScrollBar.vertical.policy: ScrollBar.AlwaysOff
                        RowLayout {
                            spacing: 10
                            Repeater {
                                model: taskStore.tasks
                                TaskCard { taskData: modelData; Layout.preferredWidth: 210 }
                            }
                        }
                    }
                }
            }

            Panel {
                Layout.fillWidth: true; Layout.fillHeight: true
                ColumnLayout {
                    anchors.fill: parent; anchors.margins: 17; spacing: 12
                    RowLayout {
                        Layout.fillWidth: true
                        ColumnLayout {
                            spacing: 3
                            Tiny { text: "DEMANDA ATUAL" }
                            Label { text: taskStore.selectedTask.title || "Nenhuma demanda"; color: root.textMain; font.pixelSize: 20; font.bold: true }
                        }
                        Item { Layout.fillWidth: true }
                        Rectangle {
                            implicitWidth: 92; implicitHeight: 28; radius: 8
                            color: "#10251e"; border.color: "#235744"
                            Tiny { anchors.centerIn: parent; text: taskStore.selectedTask.status || "—"; color: "#84d7a9" }
                        }
                    }

                    RowLayout {
                        Layout.fillWidth: true; spacing: 10
                        Repeater {
                            model: [
                                {k: "PRAZO", v: taskStore.selectedTask.dueText || "Sem prazo"},
                                {k: "ANEXOS", v: "0 arquivos"},
                                {k: "IA", v: "NYRA (Local)"}
                            ]
                            Rectangle {
                                Layout.fillWidth: true; implicitHeight: 64; radius: 10
                                color: root.soft; border.color: root.line
                                ColumnLayout {
                                    anchors.fill: parent; anchors.margins: 10; spacing: 3
                                    Tiny { text: modelData.k }
                                    Label { text: modelData.v; color: root.textMain; font.pixelSize: 12 }
                                }
                            }
                        }
                    }

                    Rectangle {
                        Layout.fillWidth: true; implicitHeight: 88; radius: 10
                        color: root.soft; border.color: root.line
                        ColumnLayout {
                            anchors.fill: parent; anchors.margins: 12; spacing: 5
                            Tiny { text: "DESCRIÇÃO" }
                            Label { text: taskStore.selectedTask.description || "Sem descrição."; color: root.textMuted; wrapMode: Text.WordWrap; Layout.fillWidth: true; font.pixelSize: 12 }
                        }
                    }

                    Rectangle {
                        Layout.fillWidth: true; Layout.fillHeight: true; radius: 10
                        color: "#0d191d"; border.color: "#24434a"
                        ColumnLayout {
                            anchors.fill: parent; anchors.margins: 12; spacing: 8
                            RowLayout {
                                Tiny { text: "● ASSISTANT INSIGHT"; color: root.cyan }
                                Item { Layout.fillWidth: true }
                                Tiny { text: "LOCAL" }
                            }
                            Label {
                                Layout.fillWidth: true
                                text: "Eu já tenho o contexto desta demanda localmente. Posso organizar o que está pendente, apontar o próximo passo e conversar com você sem acessar a internet."
                                color: root.textMuted; wrapMode: Text.WordWrap; font.pixelSize: 12
                            }
                            Item { Layout.fillHeight: true }
                            RowLayout {
                                Button {
                                    text: "Analisar agora"
                                    onClicked: root.assistantText = "Eu começaria separando o que depende de você, o que depende de terceiros e qual é a próxima ação objetiva desta demanda."
                                }
                                Button {
                                    text: "Conversar"
                                    onClicked: root.assistantText = "Estou ouvindo. Pode falar comigo normalmente."
                                }
                                Item { Layout.fillWidth: true }
                                Button {
                                    text: taskStore.selectedTask.status === "CONCLUÍDA" ? "Reabrir" : "Concluir"
                                    enabled: taskStore.selectedTask.id !== undefined
                                    onClicked: taskStore.toggleDone(taskStore.selectedTask.id)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    component NyraPanel: Panel {
        implicitWidth: 270
        ColumnLayout {
            anchors.fill: parent; anchors.margins: 15; spacing: 10
            CoreOrb { Layout.alignment: Qt.AlignHCenter; Layout.preferredWidth: 118; Layout.preferredHeight: 118 }
            Label { text: "NYRA"; color: root.textMain; font.pixelSize: 20; font.bold: true; Layout.alignment: Qt.AlignHCenter }
            Tiny { text: "PERSONAL LOCAL INTELLIGENCE"; Layout.alignment: Qt.AlignHCenter }

            Rectangle {
                Layout.fillWidth: true; implicitHeight: 54; radius: 10
                color: root.soft; border.color: root.line
                RowLayout {
                    anchors.fill: parent; anchors.margins: 10
                    ColumnLayout { spacing: 2; Tiny { text: "ESTADO // DISPONÍVEL"; color: root.cyan }; Tiny { text: "NETWORK // LOCKED" } }
                    Item { Layout.fillWidth: true }
                    Label { text: "◉"; color: root.cyan; font.pixelSize: 18 }
                }
            }

            Rectangle {
                Layout.fillWidth: true; Layout.fillHeight: true; radius: 10
                color: root.soft; border.color: root.line
                ColumnLayout {
                    anchors.fill: parent; anchors.margins: 12; spacing: 8
                    Tiny { text: "CONVERSA" }
                    Label { Layout.fillWidth: true; text: root.assistantText; color: root.textMain; wrapMode: Text.WordWrap; font.pixelSize: 12 }
                    Item { Layout.fillHeight: true }
                    Rectangle {
                        Layout.fillWidth: true; implicitHeight: 92; radius: 9
                        color: "#0b171b"; border.color: "#1d3940"
                        ColumnLayout {
                            anchors.fill: parent; anchors.margins: 10; spacing: 4
                            Tiny { text: "MEMÓRIA RECENTE" }
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
        color: "#080d10"; border.color: root.line
        RowLayout {
            anchors.fill: parent; anchors.leftMargin: 18; anchors.rightMargin: 18
            Label { text: "N.Y.R.A."; color: root.textMain; font.pixelSize: 15; font.bold: true }
            Tiny { text: "NEURAL YIELDING REASONING ASSISTANT"; visible: !root.compact }
            Item { Layout.fillWidth: true }
            Tiny { text: root.compact ? "COMPACT MODE" : "DESKTOP MODE"; color: root.cyan }
        }
    }

    StackLayout {
        anchors.fill: parent
        anchors.margins: 12
        currentIndex: root.compact ? 1 : 0

        RowLayout {
            spacing: 12
            LeftRail { Layout.preferredWidth: 210; Layout.fillHeight: true }
            WorkArea { Layout.fillWidth: true; Layout.fillHeight: true }
            NyraPanel { Layout.preferredWidth: 270; Layout.fillHeight: true }
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
                    Layout.fillWidth: true; implicitHeight: 145
                    RowLayout {
                        anchors.fill: parent; anchors.margins: 14; spacing: 14
                        CoreOrb { Layout.preferredWidth: 110; Layout.preferredHeight: 110 }
                        ColumnLayout {
                            Layout.fillWidth: true
                            Label { text: "NYRA"; color: root.textMain; font.pixelSize: 21; font.bold: true }
                            Tiny { text: "PERSONAL LOCAL INTELLIGENCE" }
                            Label { text: root.assistantText; color: root.textMuted; wrapMode: Text.WordWrap; Layout.fillWidth: true; font.pixelSize: 12 }
                            Tiny { text: "● LOCAL CORE ONLINE  •  NETWORK LOCKED"; color: root.cyan }
                        }
                    }
                }

                Panel {
                    Layout.fillWidth: true
                    implicitHeight: Math.min(350, 110 + taskStore.tasks.length * 96)
                    ColumnLayout {
                        anchors.fill: parent; anchors.margins: 14; spacing: 8
                        RowLayout {
                            Layout.fillWidth: true
                            Label { text: "Hoje"; color: root.textMain; font.pixelSize: 18; font.bold: true }
                            Item { Layout.fillWidth: true }
                            Button { text: "+ Nova"; onClicked: newTaskDialog.open() }
                        }
                        ListView {
                            Layout.fillWidth: true; Layout.fillHeight: true; spacing: 8; clip: true
                            model: taskStore.tasks
                            delegate: TaskCard { width: ListView.view.width; taskData: modelData }
                        }
                    }
                }

                Panel {
                    Layout.fillWidth: true; implicitHeight: 300
                    ColumnLayout {
                        anchors.fill: parent; anchors.margins: 14; spacing: 10
                        Tiny { text: "DEMANDA ATUAL" }
                        Label { text: taskStore.selectedTask.title || "Nenhuma demanda"; color: root.textMain; font.pixelSize: 19; font.bold: true }
                        Label { text: taskStore.selectedTask.description || "Sem descrição."; color: root.textMuted; wrapMode: Text.WordWrap; Layout.fillWidth: true; font.pixelSize: 12 }
                        Rectangle {
                            Layout.fillWidth: true; Layout.fillHeight: true; radius: 10
                            color: root.soft; border.color: root.line
                            Label {
                                anchors.fill: parent; anchors.margins: 12
                                text: "NYRA // Posso analisar esta demanda usando apenas o contexto local e sugerir o próximo passo."
                                color: root.textMuted; wrapMode: Text.WordWrap; font.pixelSize: 12
                            }
                        }
                        RowLayout {
                            Button { text: "Concluir"; enabled: taskStore.selectedTask.id !== undefined; onClicked: taskStore.toggleDone(taskStore.selectedTask.id) }
                            Button { text: "Conversar"; onClicked: root.assistantText = "Estou ouvindo." }
                            Item { Layout.fillWidth: true }
                        }
                    }
                }
            }
        }
    }

    footer: Rectangle {
        height: 58
        color: "#080d10"; border.color: root.line
        RowLayout {
            anchors.fill: parent; anchors.leftMargin: 16; anchors.rightMargin: 16; spacing: 10
            Label { text: "⌁"; color: root.cyan; font.pixelSize: 18 }
            TextField {
                Layout.fillWidth: true
                placeholderText: "Fale ou digite um comando..."
                color: root.textMain; placeholderTextColor: root.textMuted
                background: Rectangle { radius: 10; color: root.soft; border.color: root.line }
                onAccepted: {
                    if (text.trim().length > 0) {
                        root.assistantText = "Recebi: “" + text.trim() + "”. O cérebro local será conectado na próxima etapa."
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
        width: Math.min(root.width - 40, 520)
        x: Math.max(20, (root.width - width) / 2)
        y: Math.max(20, (root.height - height) / 2)
        standardButtons: Dialog.Ok | Dialog.Cancel

        onAccepted: {
            taskStore.addTask(taskTitle.text, taskDescription.text, taskPriority.currentText, taskDue.text)
            taskTitle.clear(); taskDescription.clear(); taskDue.clear(); taskPriority.currentIndex = 0
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
