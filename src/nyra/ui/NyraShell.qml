import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import QtQuick.Window

ApplicationWindow {
    id: root
    visible: true
    width: 1440
    height: 860
    minimumWidth: compactMode ? 420 : 720
    minimumHeight: 620
    title: "NYRA // NEXUS COMMAND CORE"
    color: "#020609"
    flags: Qt.Window | Qt.FramelessWindowHint

    property bool compactMode: false
    readonly property bool compact: compactMode || width < 720
    property real desktopWidth: 1440
    property real desktopHeight: 860
    property bool talking: false
    property string assistantText: "Estou aqui.\nPode falar comigo normalmente."
    property color cyan: "#24dce9"
    property color green: "#52c96a"
    property color panel: "#08151b"
    property color soft: "#0a1a20"
    property color line: "#173640"
    property color ink: "#e7f3f5"
    property color muted: "#8aa0a6"

    Behavior on width { NumberAnimation { duration: 360; easing.type: Easing.InOutCubic } }
    Behavior on height { NumberAnimation { duration: 360; easing.type: Easing.InOutCubic } }

    function setCompact(value) {
        if (value === compactMode) return
        if (value) {
            desktopWidth = width
            desktopHeight = height
            compactMode = true
            width = 460
            height = Math.min(Screen.height * 0.90, 900)
        } else {
            compactMode = false
            width = Math.max(1100, desktopWidth)
            height = Math.max(720, desktopHeight)
        }
    }

    function say(message) {
        assistantText = message
        talking = true
        systemMonitor.setSpeaking(true)
        speakingTimer.restart()
    }

    Timer {
        id: speakingTimer
        interval: 3600
        onTriggered: {
            talking = false
            systemMonitor.setSpeaking(false)
        }
    }

    component Mono: Label {
        color: root.muted
        font.family: "Consolas"
        font.pixelSize: 10
        font.letterSpacing: 1.1
    }

    component Panel: Rectangle {
        color: root.panel
        radius: 14
        border.width: 1
        border.color: root.line
    }

    component GlowButton: Button {
        id: gb
        property bool accent: false
        implicitHeight: 42
        background: Rectangle {
            radius: 8
            color: gb.down ? "#15424a" : gb.hovered ? "#102b32" : (gb.accent ? "#0c2c34" : "#09171d")
            border.width: gb.accent ? 1.5 : 1
            border.color: gb.accent ? root.cyan : root.line
        }
        contentItem: Label {
            text: gb.text
            color: gb.accent ? "#c9fbff" : root.ink
            font.pixelSize: 12
            horizontalAlignment: Text.AlignHCenter
            verticalAlignment: Text.AlignVCenter
        }
    }

    component Wave: Canvas {
        id: wave
        property bool active: root.talking
        property real phase: 0
        implicitHeight: 34
        NumberAnimation on phase {
            from: 0; to: 6.283
            duration: active ? 620 : 3200
            loops: Animation.Infinite
        }
        onPhaseChanged: requestPaint()
        onActiveChanged: requestPaint()
        onPaint: {
            var c = getContext("2d")
            c.reset()
            c.strokeStyle = root.cyan
            c.lineWidth = 1.4
            c.globalAlpha = active ? 0.95 : 0.55
            c.beginPath()
            for (var i=0;i<64;i++) {
                var x = i * width / 63
                var envelope = Math.sin(i / 63 * 3.14159)
                var amp = active ? (7 + 8*Math.sin(i*.73 + phase*2.4)) : 2.2
                var y = height/2 + Math.sin(i*.66 + phase) * amp * envelope
                if (i===0) c.moveTo(x,y); else c.lineTo(x,y)
            }
            c.stroke()
        }
    }

    component Orb: Item {
        id: orb
        implicitWidth: 230
        implicitHeight: 190
        property real phase: 0
        NumberAnimation on phase {
            from: 0; to: 6.283
            duration: root.talking ? 850 : 6500
            loops: Animation.Infinite
        }
        Canvas {
            anchors.fill: parent
            onPaint: {
                var c=getContext("2d"), cx=width/2, cy=height/2, t=orb.phase
                c.reset()
                var g=c.createRadialGradient(cx,cy,2,cx,cy,78)
                g.addColorStop(0,"#f1ffff")
                g.addColorStop(.08,"#77faff")
                g.addColorStop(.22,"#13cddd")
                g.addColorStop(.48,"#075161")
                g.addColorStop(.76,"#071a23")
                g.addColorStop(1,"#02060900")
                c.fillStyle=g
                c.beginPath(); c.arc(cx,cy,78,0,6.283); c.fill()
                for(var r=46;r<=84;r+=9){
                    c.strokeStyle=(r===73)?"#55f5fb":"#168095"
                    c.globalAlpha=(r===73)?0.75:0.38
                    c.lineWidth=(r===73)?1.5:1
                    c.beginPath(); c.arc(cx,cy,r,t*(r%4+1),t*(r%4+1)+4.4); c.stroke()
                }
                c.globalAlpha=.85
                for(var i=0;i<48;i++){
                    var a=i/48*6.283+t*.25, rr=78+(i%6)*3
                    c.fillStyle=(i%5===0)?"#9effff":"#16899a"
                    c.fillRect(cx+Math.cos(a)*rr,cy+Math.sin(a)*rr,i%5===0?2:1,i%5===0?2:1)
                }
            }
            Connections { target: orb; function onPhaseChanged(){ parent.requestPaint() } }
        }
        Wave { anchors.left: parent.left; anchors.right: parent.right; anchors.verticalCenter: parent.verticalCenter }
    }

    component Spark: Canvas {
        id: spark
        required property var values
        implicitHeight: 20
        onValuesChanged: requestPaint()
        onPaint: {
            var c=getContext("2d"), d=values || []
            c.reset(); c.strokeStyle=root.cyan; c.lineWidth=1.1; c.beginPath()
            for(var i=0;i<d.length;i++){
                var x=i*width/Math.max(1,d.length-1)
                var y=height-2-Math.max(0,Math.min(100,d[i]))/100*(height-4)
                if(i===0)c.moveTo(x,y);else c.lineTo(x,y)
            }
            c.stroke()
        }
    }

    component MetricCard: Rectangle {
        id: metric
        required property string title
        required property string value
        required property var history
        property string subtitle: ""
        implicitHeight: 56
        radius: 9
        color: "#08151b"
        border.color: root.line
        RowLayout {
            anchors.fill: parent; anchors.margins: 9; spacing: 8
            Rectangle { width: 30; height: 30; radius: 6; color: "#0e2931"; Mono { anchors.centerIn: parent; text: metric.title.charAt(0); color: root.cyan } }
            ColumnLayout {
                Layout.preferredWidth: 90; spacing: 0
                Mono { text: metric.title + " // " + metric.value; color: root.ink; font.pixelSize: 8 }
                Mono { text: metric.subtitle; font.pixelSize: 7; elide: Text.ElideRight; Layout.maximumWidth: 88 }
            }
            Spark { Layout.fillWidth: true; values: metric.history }
        }
    }

    component TaskTile: Rectangle {
        id: tile
        required property var taskData
        property bool selected: taskStore.selectedTask && taskStore.selectedTask.id === taskData.id
        radius: 10
        color: selected ? "#102c34" : "#0d1c22"
        border.width: selected ? 2 : 1
        border.color: selected ? root.cyan : root.line
        implicitHeight: 120
        MouseArea { anchors.fill: parent; cursorShape: Qt.PointingHandCursor; onClicked: taskStore.selectTask(tile.taskData.id) }
        ColumnLayout {
            anchors.fill: parent; anchors.margins: 12; spacing: 5
            Label { text: tile.taskData.title; color: root.ink; font.bold: true; font.pixelSize: 13; elide: Text.ElideRight; Layout.fillWidth: true }
            Mono { text: tile.taskData.priority === "ALTA" ? "▲ PRIORIDADE ALTA" : "◉ " + tile.taskData.status; color: tile.taskData.priority === "ALTA" ? "#ff747d" : root.muted }
            Item { Layout.fillHeight: true }
            Mono { text: tile.taskData.dueText || "Sem prazo" }
        }
    }

    // Invisible resize zones: borderless visual, native resizing behavior.
    MouseArea { anchors.left: parent.left; anchors.top: parent.top; anchors.bottom: parent.bottom; width: 5; cursorShape: Qt.SizeHorCursor; onPressed: root.startSystemResize(Qt.LeftEdge) }
    MouseArea { anchors.right: parent.right; anchors.top: parent.top; anchors.bottom: parent.bottom; width: 5; cursorShape: Qt.SizeHorCursor; onPressed: root.startSystemResize(Qt.RightEdge) }
    MouseArea { anchors.left: parent.left; anchors.right: parent.right; anchors.bottom: parent.bottom; height: 5; cursorShape: Qt.SizeVerCursor; onPressed: root.startSystemResize(Qt.BottomEdge) }

    // Borderless drag strip. No Windows titlebar or native min/max/close buttons.
    Rectangle {
        id: dragStrip
        z: 50
        anchors.left: parent.left; anchors.right: parent.right; anchors.top: parent.top
        height: root.compact ? 34 : 38
        color: "#03090d"
        border.color: root.line
        RowLayout {
            anchors.fill: parent; anchors.leftMargin: 14; anchors.rightMargin: 14
            Mono { text: root.compact ? "N.Y.R.A. // COMPACT" : "N.Y.R.A.  NEURAL YIELDING REASONING ASSISTANT"; color: root.compact ? root.cyan : root.ink }
            Item { Layout.fillWidth: true }
            GlowButton {
                width: 122; height: 28
                text: root.compactMode ? "▣ Expandir" : "▦ Compactar"
                onClicked: root.setCompact(!root.compactMode)
            }
        }
        DragHandler { target: null; onActiveChanged: if (active) root.startSystemMove() }
    }

    Item {
        anchors.left: parent.left; anchors.right: parent.right; anchors.top: dragStrip.bottom; anchors.bottom: parent.bottom

        Item {
            id: desktopView
            anchors.fill: parent
            visible: !root.compact
            opacity: visible ? 1 : 0
            Behavior on opacity { NumberAnimation { duration: 220 } }

            RowLayout {
                anchors.fill: parent; anchors.margins: 10; spacing: 10

                Panel {
                    Layout.preferredWidth: 245; Layout.fillHeight: true
                    ColumnLayout {
                        anchors.fill: parent; anchors.margins: 14; spacing: 8
                        RowLayout {
                            Rectangle { width: 48; height: 48; radius: 24; color: "#06171d"; border.color: root.cyan; Label { anchors.centerIn: parent; text: "▽"; color: root.cyan; font.pixelSize: 30 } }
                            ColumnLayout { spacing: 0; Label { text: "NEXUS //"; color: root.ink; font.bold: true; font.pixelSize: 16 } Mono { text: "COMMAND CORE"; color: root.cyan } }
                        }
                        Mono { text: "●  LOCAL AI ONLINE"; color: "#2dff9b" }
                        Item { Layout.preferredHeight: 4 }
                        Repeater {
                            model: ["⌂  Hoje","▤  Demandas","◈  Memória","□  Arquivos","◴  Rotinas","⚙  Configurações"]
                            GlowButton { Layout.fillWidth: true; text: modelData; accent: index===0 }
                        }
                        GlowButton { Layout.fillWidth: true; text: "+  Nova demanda"; accent: true; onClicked: newTaskDialog.open() }
                        Item { Layout.fillHeight: true }
                        MetricCard { Layout.fillWidth: true; title:"CPU"; value:Math.round(systemMonitor.cpuPercent)+"%"; history:systemMonitor.cpuHistory; subtitle:"NORMAL" }
                        MetricCard { Layout.fillWidth: true; title:"GPU"; value:systemMonitor.gpuPercent<0?"N/D":Math.round(systemMonitor.gpuPercent)+"%"; history:systemMonitor.gpuHistory; subtitle:systemMonitor.gpuName }
                        MetricCard { Layout.fillWidth: true; title:"RAM"; value:Math.round(systemMonitor.ramPercent)+"%"; history:systemMonitor.ramHistory; subtitle:"WINDOWS" }
                        MetricCard { Layout.fillWidth: true; title:"VOICE"; value:root.talking?"TALKING":"READY"; history:systemMonitor.voiceHistory; subtitle:"NYRA OUTPUT" }
                        MetricCard { Layout.fillWidth: true; title:"LOCAL DB"; value:systemMonitor.dbSize; history:[42,42,43,43,44,44]; subtitle:"SQLITE // SYNC" }
                    }
                }

                ColumnLayout {
                    Layout.fillWidth: true; Layout.fillHeight: true; spacing: 10
                    Panel {
                        Layout.fillWidth: true; Layout.preferredHeight: 245; clip: true
                        Canvas {
                            anchors.fill: parent; opacity: .30
                            onPaint: {
                                var c=getContext("2d"); c.reset()
                                for(var i=0;i<45;i++){
                                    var h=20+(i*19%90), x=width*.38+i*width*.014
                                    c.fillStyle="#0d3540"; c.fillRect(x,height-h,10+(i%3)*4,h)
                                    if(i%4===0){c.fillStyle="#19a9b9";c.fillRect(x+4,height-h+10,1,2)}
                                }
                            }
                        }
                        ColumnLayout {
                            anchors.fill: parent; anchors.margins: 18; spacing: 10
                            RowLayout { Layout.fillWidth: true; ColumnLayout { spacing:1; Label { text:"☾  Boa noite."; color:root.ink; font.pixelSize:22; font.bold:true } Label { text:"Estas são as coisas que merecem sua atenção agora."; color:root.muted; font.pixelSize:11 } } Item { Layout.fillWidth:true } Mono { text:taskStore.tasks.length+" DEMANDAS" } }
                            ListView { Layout.fillWidth:true; Layout.fillHeight:true; orientation:ListView.Horizontal; spacing:10; clip:true; model:taskStore.tasks; delegate:TaskTile { width:205; taskData:modelData } }
                        }
                    }

                    Panel {
                        Layout.fillWidth:true; Layout.fillHeight:true
                        ColumnLayout {
                            anchors.fill:parent; anchors.margins:18; spacing:12
                            RowLayout { Layout.fillWidth:true; ColumnLayout { spacing:2; Mono { text:"DEMANDA ATUAL" } Label { text:taskStore.selectedTask.title||"Nenhuma demanda"; color:root.ink; font.pixelSize:22; font.bold:true } } Item { Layout.fillWidth:true } Rectangle { width:90;height:28;radius:14;color:"#0c3026";border.color:"#1e6d52";Mono{anchors.centerIn:parent;text:"● "+(taskStore.selectedTask.status||"—");color:"#65f2a5"} } }
                            RowLayout { Layout.fillWidth:true;spacing:9; Repeater { model:[{k:"PRAZO",v:taskStore.selectedTask.dueText||"Sem prazo"},{k:"ANEXOS",v:"0 arquivos"},{k:"IA",v:"NYRA (Local)"}]; Rectangle { Layout.fillWidth:true;implicitHeight:72;radius:9;color:root.soft;border.color:root.line;ColumnLayout{anchors.fill:parent;anchors.margins:10;Mono{text:modelData.k}Label{text:modelData.v;color:root.ink;font.bold:true;font.pixelSize:12}} } } }
                            Rectangle { Layout.fillWidth:true;implicitHeight:82;radius:9;color:"#071218";border.color:root.line;ColumnLayout{anchors.fill:parent;anchors.margins:10;Mono{text:"DESCRIÇÃO"}Label{Layout.fillWidth:true;text:taskStore.selectedTask.description||"Sem descrição.";color:root.muted;wrapMode:Text.WordWrap;font.pixelSize:11}} }
                            Rectangle { Layout.fillWidth:true;Layout.fillHeight:true;radius:9;color:"#0a191e";border.color:"#1d4650";ColumnLayout{anchors.fill:parent;anchors.margins:12;RowLayout{Layout.fillWidth:true;Mono{text:"✦  ASSISTANT INSIGHT";color:root.cyan}Item{Layout.fillWidth:true}Mono{text:"LOCAL"}}Label{Layout.fillWidth:true;Layout.fillHeight:true;text:"Para concluir esta demanda com mais eficiência, recomendo priorizar fornecedores que já passaram pelo processo de homologação e possuem histórico de entrega. Posso comparar automaticamente as propostas assim que os arquivos forem enviados.";color:root.muted;wrapMode:Text.WordWrap;font.pixelSize:11}RowLayout{Layout.fillWidth:true;GlowButton{text:"⌁  Analisar agora";accent:true;Layout.preferredWidth:190;onClicked:root.say("Vou analisar a demanda e separar as próximas ações objetivas.")}GlowButton{text:"◌  Conversar";Layout.fillWidth:true;onClicked:root.say("Estou ouvindo. Pode falar comigo normalmente.")}GlowButton{text:"•••";Layout.preferredWidth:58}}} }
                        }
                    }
                }

                Panel {
                    Layout.preferredWidth: 330; Layout.fillHeight:true
                    ColumnLayout {
                        anchors.fill:parent;anchors.margins:14;spacing:9
                        RowLayout{Layout.fillWidth:true;ColumnLayout{spacing:0;Label{text:"NYRA";color:root.ink;font.pixelSize:22}Mono{text:"PERSONAL INTELLIGENCE"}}Item{Layout.fillWidth:true}Mono{text:"☷";color:root.ink;font.pixelSize:16}}
                        Orb { Layout.fillWidth:true;Layout.preferredHeight:210 }
                        Rectangle{Layout.fillWidth:true;implicitHeight:48;radius:8;color:"#071319";border.color:root.line;RowLayout{anchors.fill:parent;anchors.margins:9;ColumnLayout{spacing:0;Mono{text:root.talking?"ESTADO // FALANDO":"ESTADO // DISPONÍVEL";color:root.cyan}Mono{text:"NETWORK // LOCKED"}}Item{Layout.fillWidth:true}Wave{width:105;active:root.talking}}}
                        Rectangle{Layout.fillWidth:true;implicitHeight:104;radius:9;color:"#0b1b22";border.color:root.line;RowLayout{anchors.fill:parent;anchors.margins:10;Rectangle{width:30;height:30;radius:15;color:"#0b313c";border.color:root.cyan;Label{anchors.centerIn:parent;text:"◉";color:root.cyan}}Label{Layout.fillWidth:true;text:root.assistantText;color:root.ink;wrapMode:Text.WordWrap;font.pixelSize:11}}}
                        Rectangle{Layout.fillWidth:true;implicitHeight:34;radius:7;color:"#2a1117";border.color:"#632631";RowLayout{anchors.fill:parent;anchors.margins:8;Mono{text:"▣  NETWORK // LOCKED";color:"#ff5d68"}Item{Layout.fillWidth:true}Mono{text:"Internet bloqueada";color:"#bd6570"}}}
                        RowLayout{Layout.fillWidth:true;Mono{text:"MEMÓRIA RECENTE";color:root.ink}Item{Layout.fillWidth:true}Mono{text:"Ver tudo"}}
                        Repeater{model:["Você pediu para priorizar fornecedores com histórico de entrega.","Preferência: respostas objetivas e com tabelas comparativas.","Projeto 3DRN Store em andamento."];RowLayout{Layout.fillWidth:true;Rectangle{width:26;height:26;radius:5;color:"#0c2229";Mono{anchors.centerIn:parent;text:"□"}}Label{Layout.fillWidth:true;text:modelData;color:root.muted;wrapMode:Text.WordWrap;font.pixelSize:9}}}
                        Item{Layout.fillHeight:true}
                        Mono{Layout.fillWidth:true;text:systemMonitor.gpuName+"\n"+systemMonitor.gpuStatus;wrapMode:Text.WordWrap;font.pixelSize:7}
                    }
                }
            }
        }

        // Compact mode is a separate composition, not a squeezed desktop layout.
        Flickable {
            id: compactView
            anchors.fill: parent
            visible: root.compact
            opacity: visible ? 1 : 0
            clip: true
            contentWidth: width
            contentHeight: compactColumn.implicitHeight + 30
            ScrollBar.vertical: ScrollBar { policy: ScrollBar.AsNeeded }
            Behavior on opacity { NumberAnimation { duration: 220 } }

            ColumnLayout {
                id: compactColumn
                width: parent.width
                spacing: 12
                anchors.left: parent.left; anchors.right: parent.right
                anchors.leftMargin: 14; anchors.rightMargin: 14

                Item { Layout.preferredHeight: 4 }

                ColumnLayout {
                    Layout.fillWidth:true; spacing:6
                    Mono { text:"PERSONAL OPERATIONS NODE // 03" }
                    Label { text:"NEXUS // COMMAND CORE";color:root.ink;font.pixelSize:23;font.bold:true }
                    RowLayout { Layout.fillWidth:true;spacing:7
                        Rectangle{Layout.fillWidth:true;height:38;radius:7;color:"#09171d";border.color:root.line;Mono{anchors.centerIn:parent;text:"● LOCAL AI";color:root.green}}
                        Rectangle{Layout.fillWidth:true;height:38;radius:7;color:"#09171d";border.color:root.line;Mono{anchors.centerIn:parent;text:"NETWORK // LOCKED"}}
                        Rectangle{Layout.fillWidth:true;height:38;radius:7;color:"#09171d";border.color:root.line;Mono{anchors.centerIn:parent;text:"MEMORY // ACTIVE"}}
                    }
                }

                Panel {
                    Layout.fillWidth:true; implicitHeight:520
                    ColumnLayout {
                        anchors.fill:parent;anchors.margins:18;spacing:10
                        Orb { Layout.alignment:Qt.AlignHCenter;Layout.preferredWidth:260;Layout.preferredHeight:180 }
                        Label{text:"NYRA";color:root.ink;font.pixelSize:22;font.bold:true;Layout.alignment:Qt.AlignHCenter}
                        Mono{text:"PERSONAL INTELLIGENCE";Layout.alignment:Qt.AlignHCenter}
                        Rectangle{Layout.fillWidth:true;implicitHeight:142;radius:10;color:"#071218";border.color:root.line;ColumnLayout{anchors.fill:parent;anchors.margins:14;Mono{text:root.talking?"ESTADO // FALANDO":"ESTADO // DISPONÍVEL"}Wave{Layout.fillWidth:true;active:root.talking}Label{Layout.fillWidth:true;text:root.assistantText;color:root.muted;wrapMode:Text.WordWrap;font.pixelSize:13;horizontalAlignment:Text.AlignHCenter}}}
                        GlowButton{Layout.fillWidth:true;text:"◉  Iniciar conversa por voz";onClicked:root.say("Estou ouvindo. Pode falar comigo normalmente.")}
                        GlowButton{Layout.fillWidth:true;text:"⌁  Simular necessidade de internet";onClicked:root.say("Eu preciso acessar a internet para confirmar uma informação atual. Posso acessar?")}
                        Rectangle{Layout.fillWidth:true;height:1;color:root.line}
                        Mono{text:"MEMÓRIA RECENTE";color:root.ink}
                        Repeater{model:["Você prefere que eu peça autorização de forma natural por voz.","A internet permanece bloqueada por padrão.","Sua interface deve ter identidade sci-fi/cyberpunk."];Rectangle{Layout.fillWidth:true;implicitHeight:52;radius:8;color:"#08151b";border.color:root.line;Label{anchors.fill:parent;anchors.margins:10;text:modelData;color:root.muted;wrapMode:Text.WordWrap;font.pixelSize:11}}}
                    }
                }

                Panel {
                    Layout.fillWidth:true; implicitHeight:460
                    ColumnLayout { anchors.fill:parent;anchors.margins:18;spacing:10
                        Mono{text:"NAVIGATION";color:root.ink}
                        Repeater{model:[{n:"◉ Hoje",c:"07"},{n:"▦ Demandas",c:String(taskStore.tasks.length).padStart(2,"0")},{n:"◇ Memória",c:""},{n:"⌁ Arquivos",c:""},{n:"↻ Rotinas",c:""}];Rectangle{Layout.fillWidth:true;implicitHeight:56;radius:9;color:index===0?"#10292c":"transparent";border.color:index===0?"#376b3c":"transparent";RowLayout{anchors.fill:parent;anchors.margins:12;Label{text:modelData.n;color:root.ink;font.pixelSize:14}Item{Layout.fillWidth:true}Mono{text:modelData.c;color:root.ink;font.pixelSize:13}}}}
                        Item{Layout.preferredHeight:5}
                        Mono{text:"SYSTEM";color:root.ink}
                        Rectangle{Layout.fillWidth:true;implicitHeight:48;radius:8;color:"#08151b";border.color:root.line;RowLayout{anchors.fill:parent;anchors.margins:11;Mono{text:"CPU // "+Math.round(systemMonitor.cpuPercent)+"%";color:root.ink}Item{Layout.fillWidth:true}Spark{width:100;values:systemMonitor.cpuHistory}}}
                        Rectangle{Layout.fillWidth:true;implicitHeight:48;radius:8;color:"#08151b";border.color:root.line;RowLayout{anchors.fill:parent;anchors.margins:11;Mono{text:"VOICE // "+(root.talking?"TALKING":"READY");color:root.ink}Item{Layout.fillWidth:true}Wave{width:100;active:root.talking}}}
                        Rectangle{Layout.fillWidth:true;implicitHeight:48;radius:8;color:"#08151b";border.color:root.line;Mono{anchors.centerIn:parent;text:"LOCAL DB // "+systemMonitor.dbSize;color:root.ink}}
                        GlowButton{Layout.fillWidth:true;text:"＋ Nova demanda";accent:true;onClicked:newTaskDialog.open()}
                    }
                }

                Panel {
                    Layout.fillWidth:true; implicitHeight:520
                    ColumnLayout { anchors.fill:parent;anchors.margins:18;spacing:10
                        Mono{text:"FOCUS // HOJE"}
                        Label{text:"Boa noite.";color:root.ink;font.pixelSize:24;font.bold:true}
                        Label{text:"Estas são as coisas que merecem sua atenção agora.";color:root.muted;font.pixelSize:14;wrapMode:Text.WordWrap;Layout.fillWidth:true}
                        Mono{text:"PRIORIDADE GERAL";Layout.alignment:Qt.AlignHCenter}
                        Label{text:"02 críticas · 03 normais";color:root.ink;font.pixelSize:15}
                        ListView{Layout.fillWidth:true;Layout.fillHeight:true;spacing:10;clip:true;model:taskStore.tasks;delegate:Rectangle{width:ListView.view.width;height:92;radius:10;color:modelData.id===taskStore.selectedTask.id?"#10282a":"#08151b";border.width:modelData.id===taskStore.selectedTask.id?1.5:1;border.color:modelData.id===taskStore.selectedTask.id?"#427d47":root.line;MouseArea{anchors.fill:parent;onClicked:taskStore.selectTask(modelData.id)}RowLayout{anchors.fill:parent;anchors.margins:14;ColumnLayout{Layout.fillWidth:true;Label{text:modelData.title;color:root.ink;font.bold:true;font.pixelSize:15}Mono{text:modelData.priority==="ALTA"?"▲ PRIORIDADE ALTA":"◉ "+modelData.status}}Mono{text:String(index+1).padStart(2,"0");color:root.ink}}}}
                    }
                }

                Panel {
                    Layout.fillWidth:true; implicitHeight:470
                    ColumnLayout { anchors.fill:parent;anchors.margins:18;spacing:12
                        RowLayout{Layout.fillWidth:true;ColumnLayout{Mono{text:"ACTIVE OBJECT"}Label{text:taskStore.selectedTask.title||"Nenhuma demanda";color:root.ink;font.pixelSize:21;font.bold:true}}Item{Layout.fillWidth:true}Rectangle{width:64;height:36;radius:9;color:"#08151b";border.color:root.line;Mono{anchors.centerIn:parent;text:"ATIVA"}}}
                        Label{Layout.fillWidth:true;text:taskStore.selectedTask.description||"Sem descrição.";color:root.muted;font.pixelSize:14;wrapMode:Text.WordWrap}
                        Repeater{model:[{k:"PRAZO",v:taskStore.selectedTask.dueText||"Hoje"},{k:"ANEXOS",v:"3 arquivos"},{k:"IA",v:"Contexto carregado"}];Rectangle{Layout.fillWidth:true;implicitHeight:86;radius:10;color:"#08151b";border.color:root.line;ColumnLayout{anchors.fill:parent;anchors.margins:12;Mono{text:modelData.k}Label{text:modelData.v;color:root.ink;font.pixelSize:14}}}}
                    }
                }

                Panel {
                    Layout.fillWidth:true; implicitHeight:300
                    ColumnLayout{anchors.fill:parent;anchors.margins:18;spacing:12
                        RowLayout{Layout.fillWidth:true;Mono{text:"●  SUGESTÃO DA IA";color:root.green}Item{Layout.fillWidth:true}Mono{text:"LOCAL"}}
                        Label{Layout.fillWidth:true;text:"Notei que esta demanda depende mais de uma decisão do que de execução. Posso comparar o que você já definiu antes e sugerir apenas as mudanças que realmente fazem diferença.";color:root.muted;font.pixelSize:14;wrapMode:Text.WordWrap}
                        RowLayout{GlowButton{text:"↗  MELHORAR";Layout.fillWidth:true}GlowButton{text:"◉  CONVERSAR";Layout.fillWidth:true;onClicked:root.say("Podemos conversar sobre esta demanda sem perder o contexto anterior.")}}
                    }
                }

                Panel {
                    Layout.fillWidth:true; implicitHeight:145
                    RowLayout{anchors.fill:parent;anchors.margins:18;spacing:14;Wave{width:85;active:true}ColumnLayout{Layout.fillWidth:true;Mono{text:"ESCUTANDO PASSIVAMENTE";color:root.ink}Label{text:"Você pode falar comigo\na qualquer momento.";color:root.muted;font.pixelSize:13}}GlowButton{text:"TESTAR INTERNET";onClicked:root.say("Preciso consultar a internet para verificar isso. Posso acessar?")}}
                }

                Item { Layout.preferredHeight: 18 }
            }
        }
    }

    footer: Rectangle {
        visible: !root.compact
        height: 72
        color: "#061017"
        border.color: root.line
        RowLayout {
            anchors.fill:parent;anchors.margins:12;spacing:10
            Label{text:"▥";color:root.cyan;font.pixelSize:20}
            TextField{Layout.fillWidth:true;placeholderText:"Fale ou digite um comando...";color:root.ink;placeholderTextColor:root.muted;background:Rectangle{radius:10;color:"#071218";border.color:root.line}onAccepted:{if(text.trim().length){root.say("Recebi: “"+text.trim()+"”.");text=""}}}
            Mono{text:"ESC para cancelar"}
            Rectangle{width:46;height:46;radius:23;color:"#0b3943";border.color:root.cyan;Label{anchors.centerIn:parent;text:"●";color:root.cyan;font.pixelSize:18}}
        }
    }

    Dialog {
        id:newTaskDialog
        modal:true
        title:"Nova demanda"
        width:Math.min(root.width-40,520)
        standardButtons:Dialog.Ok|Dialog.Cancel
        onAccepted:{taskStore.addTask(taskTitle.text,taskDescription.text,taskPriority.currentText,taskDue.text);taskTitle.clear();taskDescription.clear();taskDue.clear();taskPriority.currentIndex=0}
        contentItem:ColumnLayout{spacing:10;TextField{id:taskTitle;Layout.fillWidth:true;placeholderText:"Título"}TextArea{id:taskDescription;Layout.fillWidth:true;Layout.preferredHeight:100;placeholderText:"Descrição";wrapMode:TextEdit.WordWrap}RowLayout{Layout.fillWidth:true;ComboBox{id:taskPriority;model:["NORMAL","ALTA","BAIXA"];Layout.preferredWidth:140}TextField{id:taskDue;Layout.fillWidth:true;placeholderText:"Prazo"}}}
    }

    Shortcut { sequence:"Alt+F4"; onActivated: Qt.quit() }
    Shortcut { sequence:"Ctrl+Shift+C"; onActivated: root.setCompact(!root.compactMode) }
}
