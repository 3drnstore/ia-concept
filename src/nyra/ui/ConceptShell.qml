import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import QtQuick.Window

ApplicationWindow {
    id: root
    visible: true
    width: 1440; height: 840
    minimumWidth: compactMode ? 420 : 1050
    minimumHeight: compactMode ? 680 : 700
    color: "transparent"
    flags: Qt.Window | Qt.FramelessWindowHint
    title: "NYRA // NEXUS COMMAND CORE"

    property bool compactMode: false
    property real savedW: 1440
    property real savedH: 840
    property bool talking: false
    property string currentSection: "Hoje"
    property bool notificationsEnabled: true
    property bool startWithWindows: false
    property string assistantText: "Estou aqui.\nPode falar comigo normalmente."
    property color cyan: "#19e7f3"
    property color cyan2: "#0aa6b7"
    property color ink: "#e9f4f7"
    property color muted: "#91a7ae"
    property color panel: "#a0091821"
    property color panel2: "#9a0d1d27"
    property color line: "#542b5360"
    property color red: "#ff5867"
    property color green: "#35e788"

    Behavior on width { NumberAnimation { duration: 380; easing.type: Easing.InOutCubic } }
    Behavior on height { NumberAnimation { duration: 380; easing.type: Easing.InOutCubic } }

    function toggleCompact() {
        if (!compactMode) {
            savedW = width; savedH = height; compactMode = true
            width = 460; height = Math.min(900, Screen.height * .92)
        } else {
            compactMode = false; width = Math.max(1180, savedW); height = Math.max(760, savedH)
        }
    }
    function say(t) { assistantText=t; talking=true; systemMonitor.setSpeaking(true); talkTimer.restart() }
    Timer { id:talkTimer; interval:3200; onTriggered:{ talking=false; systemMonitor.setSpeaking(false) } }

    component Mono: Label { color:root.muted; font.family:"Consolas"; font.pixelSize:10; font.letterSpacing:1.05 }
    component Panel: Rectangle {
        radius:12; border.width:1; border.color:"#423b6672"
        gradient:Gradient{GradientStop{position:0;color:"#c6122732"}GradientStop{position:.5;color:"#ae091922"}GradientStop{position:1;color:"#cc06131b"}}
        Rectangle{anchors.fill:parent;anchors.margins:1;radius:parent.radius-1;color:"transparent";border.width:1;border.color:"#2427cddd"}
    }
    component CButton: Button {
        id:b; property bool accent:false; property bool leftAligned:false; implicitHeight:42
        background: Rectangle { radius:8; border.width:1; border.color:b.accent?"#7419e7f3":"#34395560"
            gradient:Gradient{GradientStop{position:0;color:b.down?"#d01a4652":b.hovered?"#ca153642":b.accent?"#cb174354":"#a5152933"}GradientStop{position:1;color:b.accent?"#a40a2530":"#8c0a171e"}}
            Rectangle{visible:b.accent;anchors.fill:parent;anchors.margins:2;radius:6;color:"transparent";border.width:3;border.color:"#1819e7f3"}
            Rectangle { visible:b.accent && b.leftAligned; width:3; radius:2; color:root.cyan; anchors.left:parent.left; anchors.top:parent.top; anchors.bottom:parent.bottom }
        }
        contentItem: Label { text:b.text; color:b.accent?"#d5fdff":root.ink; font.pixelSize:12; leftPadding:b.leftAligned?18:0; horizontalAlignment:b.leftAligned?Text.AlignLeft:Text.AlignHCenter; verticalAlignment:Text.AlignVCenter }
    }
    component Glyph: Canvas {
        id:gi; property string kind:"folder"; property color stroke:root.muted; implicitWidth:20; implicitHeight:20
        onKindChanged:requestPaint(); onStrokeChanged:requestPaint(); Component.onCompleted:requestPaint()
        onPaint:{var c=getContext("2d");c.reset();c.strokeStyle=stroke;c.fillStyle=stroke;c.lineWidth=1.45;c.lineCap="round";c.lineJoin="round";var w=width,h=height;
            function line(a,b,d,e){c.beginPath();c.moveTo(a,b);c.lineTo(d,e);c.stroke()}
            function box(x,y,bw,bh,r){c.beginPath();c.roundedRect(x,y,bw,bh,r,r);c.stroke()}
            if(kind==="home"){c.beginPath();c.moveTo(w*.18,h*.48);c.lineTo(w*.5,h*.2);c.lineTo(w*.82,h*.48);c.stroke();box(w*.28,h*.45,w*.44,h*.38,1)}
            else if(kind==="tasks"){box(w*.25,h*.16,w*.56,h*.68,2);for(var i=0;i<3;i++){box(w*.12,h*(.27+i*.22),w*.12,h*.1,1);line(w*.42,h*(.32+i*.22),w*.7,h*(.32+i*.22))}}
            else if(kind==="brain"){c.beginPath();c.arc(w*.37,h*.5,w*.2,1.4,4.9);c.arc(w*.63,h*.5,w*.2,4.5,1.8);c.stroke();line(w*.5,h*.23,w*.5,h*.77);line(w*.25,h*.48,w*.43,h*.58);line(w*.75,h*.42,w*.57,h*.54)}
            else if(kind==="folder"){c.beginPath();c.moveTo(w*.12,h*.3);c.lineTo(w*.4,h*.3);c.lineTo(w*.49,h*.4);c.lineTo(w*.88,h*.4);c.lineTo(w*.88,h*.82);c.lineTo(w*.12,h*.82);c.closePath();c.stroke()}
            else if(kind==="clock"){c.beginPath();c.arc(w*.5,h*.52,w*.34,0,6.283);c.stroke();line(w*.5,h*.52,w*.5,h*.3);line(w*.5,h*.52,w*.67,h*.61);line(w*.37,h*.1,w*.63,h*.1)}
            else if(kind==="gear"){c.beginPath();c.arc(w*.5,h*.5,w*.22,0,6.283);c.stroke();c.beginPath();c.arc(w*.5,h*.5,w*.08,0,6.283);c.stroke();for(var j=0;j<8;j++){var a=j*.785;line(w*.5+Math.cos(a)*w*.25,h*.5+Math.sin(a)*h*.25,w*.5+Math.cos(a)*w*.38,h*.5+Math.sin(a)*h*.38)}}
            else if(kind==="cpu"){box(w*.25,h*.25,w*.5,h*.5,2);box(w*.36,h*.36,w*.28,h*.28,1);for(var k=0;k<4;k++){var p=.31+k*.13;line(w*p,h*.08,w*p,h*.22);line(w*p,h*.78,w*p,h*.92);line(w*.08,h*p,w*.22,h*p);line(w*.78,h*p,w*.92,h*p)}}
            else if(kind==="mic"){box(w*.36,h*.13,w*.28,h*.48,5);c.beginPath();c.arc(w*.5,h*.49,w*.27,0,3.1416);c.stroke();line(w*.5,h*.76,w*.5,h*.9);line(w*.35,h*.9,w*.65,h*.9)}
            else if(kind==="db"){for(var q=0;q<3;q++){c.beginPath();c.ellipse(w*.5,h*(.25+q*.22),w*.31,h*.12,0,0,6.283);c.stroke()}line(w*.19,h*.25,w*.19,h*.69);line(w*.81,h*.25,w*.81,h*.69)}
            else if(kind==="moon"){c.beginPath();c.moveTo(w*.65,h*.15);c.bezierCurveTo(w*.29,h*.17,w*.17,h*.7,w*.52,h*.86);c.bezierCurveTo(w*.37,h*.66,w*.41,h*.34,w*.65,h*.15);c.closePath();c.stroke()}
            else if(kind==="wave"){for(var z=0;z<7;z++){var hh=h*(.18+.65*Math.abs(Math.sin(z*1.7)));line(w*(.15+z*.115),h*.5-hh*.5,w*(.15+z*.115),h*.5+hh*.5)}}
            else if(kind==="windows"){for(var n=0;n<4;n++){var xx=n%2,yy=Math.floor(n/2);c.fillRect(w*(.12+xx*.4),h*(.12+yy*.4),w*.3,h*.3)}}
            else if(kind==="cloud"){c.beginPath();c.moveTo(w*.18,h*.66);c.bezierCurveTo(w*.08,h*.45,w*.28,h*.36,w*.39,h*.43);c.bezierCurveTo(w*.48,h*.16,w*.78,h*.26,w*.78,h*.46);c.bezierCurveTo(w*.96,h*.5,w*.88,h*.7,w*.7,h*.7);c.lineTo(w*.28,h*.7);c.stroke()}
            else if(kind==="cube"){c.beginPath();c.moveTo(w*.5,h*.12);c.lineTo(w*.82,h*.3);c.lineTo(w*.82,h*.68);c.lineTo(w*.5,h*.88);c.lineTo(w*.18,h*.68);c.lineTo(w*.18,h*.3);c.closePath();c.stroke();line(w*.18,h*.3,w*.5,h*.5);line(w*.82,h*.3,w*.5,h*.5);line(w*.5,h*.5,w*.5,h*.88)}
            else if(kind==="chat"){box(w*.14,h*.18,w*.72,h*.52,4);c.beginPath();c.moveTo(w*.3,h*.7);c.lineTo(w*.24,h*.86);c.lineTo(w*.46,h*.7);c.stroke();for(var m=0;m<3;m++){c.beginPath();c.arc(w*(.36+m*.14),h*.44,w*.025,0,6.283);c.fill()}}
            else if(kind==="sliders"){for(var u=0;u<3;u++){var yy=h*(.25+u*.25),knob=u===0?.63:u===1?.38:.7;line(w*.18,yy,w*.82,yy);c.beginPath();c.arc(w*knob,yy,w*.08,0,6.283);c.fill()}}
        }
    }
    component Wave: Canvas {
        id:w; property bool active:root.talking; property real phase:0
        implicitHeight:32
        NumberAnimation on phase { from:0; to:6.283; duration:620; loops:Animation.Infinite; running:w.active }
        onPhaseChanged:requestPaint(); onActiveChanged:{ if(!active) phase=0; requestPaint() }
        onPaint:{
            var c=getContext("2d"); c.reset(); c.fillStyle=root.cyan; c.globalAlpha=active?.95:.68;
            var bars=Math.max(12,Math.floor(width/5)),mid=height/2;
            for(var i=0;i<bars;i++){var x=i*width/bars+1,env=.22+.78*Math.sin(i/(bars-1)*3.14159),pulse=Math.abs(Math.sin(i*.83+phase*2.1));var h=2+env*(active?(5+height*.42*pulse):(2+height*.18*pulse));c.fillRect(x,mid-h/2,1.4,h)}
        }
    }
    component Spark: Canvas {
        id:s; required property var values; implicitHeight:22; onValuesChanged:requestPaint()
        onPaint:{var c=getContext("2d"),d=values||[];c.reset();c.fillStyle=root.cyan;c.globalAlpha=.76;var n=Math.max(12,Math.min(32,d.length||20));for(var i=0;i<n;i++){var v=d.length?d[Math.floor(i*d.length/n)]:(35+25*Math.sin(i*.9));var h=2+Math.max(0,Math.min(100,v))/100*(height-4);c.fillRect(i*width/n+1,height/2-h/2,1.3,h)}}
    }
    component MetricCard: Rectangle {
        required property string metricLabel
        required property string metricValue
        required property string metricStatus
        required property string metricIcon
        required property var metricValues
        Layout.fillWidth:true; implicitHeight:58; radius:8; border.color:"#37345560"; gradient:Gradient{GradientStop{position:0;color:"#b7132a35"}GradientStop{position:1;color:"#a008171f"}}
        RowLayout { anchors.fill:parent; anchors.margins:8
            Rectangle { Layout.preferredWidth:30;Layout.preferredHeight:30;radius:6;color:"#85142c36"; Glyph{anchors.centerIn:parent;width:18;height:18;kind:metricIcon;stroke:"#9db5c1"} }
            ColumnLayout { Layout.preferredWidth:92; spacing:0
                Mono{text:metricLabel+" // "+metricValue;color:root.ink;font.pixelSize:8}
                Mono{text:metricStatus;color:root.green;font.pixelSize:7;elide:Text.ElideRight;Layout.maximumWidth:90}
            }
            Spark { Layout.fillWidth:true;values:metricValues }
        }
    }
    component Orb: Item {
        id:o; property real phase:0; implicitWidth:300; implicitHeight:225
        NumberAnimation on phase { from:0; to:6.283; duration:root.talking?820:6500; loops:Animation.Infinite }
        Image { id:orbImage; anchors.centerIn:parent; width:Math.min(parent.width*.78,parent.height*.98); height:width; source:"../assets/nyra-orb.png"; fillMode:Image.PreserveAspectFit; opacity:.94; smooth:true }
        Canvas { id:orbCanvas; anchors.fill:parent
            onPaint:{
                var c=getContext("2d"),cx=width/2,cy=height/2,t=o.phase;c.reset();
                var rad=Math.min(width*.27,height*.42);
                for(var r=rad*.48;r<=rad*1.16;r+=rad*.12){c.strokeStyle=(r>rad*.75&&r<rad*1.03)?"#57f4ff":"#15788c";c.globalAlpha=(r>rad*.75&&r<rad*1.03)?.68:.3;c.lineWidth=(r>rad*.75&&r<rad*1.03)?1.6:1;c.beginPath();c.arc(cx,cy,r,t*(r%4+1),t*(r%4+1)+4.45);c.stroke()}
                c.globalAlpha=.78;for(var i=0;i<86;i++){var a=i/86*6.283+t*.18,rr=rad*1.12+(i%7)*2.4;c.fillStyle=i%7===0?"#b6ffff":"#1594a7";c.fillRect(cx+Math.cos(a)*rr,cy+Math.sin(a)*rr,i%7===0?2:1,i%7===0?2:1)}
            }
            Connections { target:o; function onPhaseChanged(){ orbCanvas.requestPaint() } }
        }
        Wave { anchors.left:parent.left; anchors.right:parent.right; anchors.verticalCenter:parent.verticalCenter; height:86 }
    }

    Rectangle { anchors.fill:parent; radius:12; color:"#02070b"; z:-100 }

    // frameless move / resize
    MouseArea { anchors.left:parent.left;anchors.top:parent.top;anchors.bottom:parent.bottom;width:5;cursorShape:Qt.SizeHorCursor;onPressed:root.startSystemResize(Qt.LeftEdge) }
    MouseArea { anchors.right:parent.right;anchors.top:parent.top;anchors.bottom:parent.bottom;width:5;cursorShape:Qt.SizeHorCursor;onPressed:root.startSystemResize(Qt.RightEdge) }
    MouseArea { anchors.left:parent.left;anchors.right:parent.right;anchors.bottom:parent.bottom;height:5;cursorShape:Qt.SizeVerCursor;onPressed:root.startSystemResize(Qt.BottomEdge) }

    Rectangle {
        id:topbar; z:50; height:38; anchors.left:parent.left;anchors.right:parent.right;anchors.top:parent.top
        color:"#d0030a0f"; border.color:"#2f2b4b56"; radius:10
        RowLayout { anchors.fill:parent; anchors.leftMargin:14; anchors.rightMargin:10
            Mono { text:root.compactMode?"N.Y.R.A. // COMPACT":"N.Y.R.A.   NEURAL YIELDING REASONING ASSISTANT"; color:root.compactMode?root.cyan:root.ink }
            Item { Layout.fillWidth:true }
            Button{Layout.preferredWidth:34;Layout.preferredHeight:30;background:Rectangle{color:parent.hovered?"#4a18313b":"transparent";radius:5}contentItem:Label{text:"−";color:"#aebfc6";font.pixelSize:17;horizontalAlignment:Text.AlignHCenter;verticalAlignment:Text.AlignVCenter}onClicked:root.showMinimized()}
            Button{Layout.preferredWidth:34;Layout.preferredHeight:30;background:Rectangle{color:parent.hovered?"#4a18313b":"transparent";radius:5}contentItem:Label{text:"□";color:"#aebfc6";font.pixelSize:16;horizontalAlignment:Text.AlignHCenter;verticalAlignment:Text.AlignVCenter}onClicked:root.toggleCompact()}
            Button{Layout.preferredWidth:34;Layout.preferredHeight:30;background:Rectangle{color:parent.hovered?"#7cff4050":"transparent";radius:5}contentItem:Label{text:"×";color:"#aebfc6";font.pixelSize:19;horizontalAlignment:Text.AlignHCenter;verticalAlignment:Text.AlignVCenter}onClicked:Qt.quit()}
        }
        DragHandler { target:null; onActiveChanged:if(active)root.startSystemMove() }
    }

    Item { anchors.left:parent.left;anchors.right:parent.right;anchors.top:topbar.bottom;anchors.bottom:parent.bottom
        // DESKTOP — proportions copied from approved concept
        Item { anchors.fill:parent; visible:!root.compactMode
            RowLayout { anchors.left:parent.left;anchors.right:parent.right;anchors.top:parent.top;anchors.bottom:commandDock.top;anchors.margins:12;anchors.bottomMargin:10;spacing:12
                // LEFT
                Rectangle { Layout.preferredWidth:265; Layout.fillHeight:true; radius:14; border.color:"#3332525d";gradient:Gradient{GradientStop{position:0;color:"#cf081720"}GradientStop{position:.55;color:"#ba06141c"}GradientStop{position:1;color:"#d0081820"}}
                    ColumnLayout { anchors.fill:parent; anchors.margins:16; spacing:9
                        RowLayout { spacing:10
                            Rectangle { width:52;height:52;radius:26;color:"#061a22";border.width:1;border.color:root.cyan
                                Canvas{anchors.fill:parent;onPaint:{var c=getContext("2d");c.reset();c.strokeStyle=root.cyan;c.lineWidth=1.5;c.beginPath();c.moveTo(width*.22,height*.26);c.lineTo(width*.78,height*.26);c.lineTo(width*.5,height*.78);c.closePath();c.stroke();c.beginPath();c.moveTo(width*.32,height*.34);c.lineTo(width*.68,height*.34);c.lineTo(width*.5,height*.64);c.closePath();c.stroke()}}
                            }
                            ColumnLayout{spacing:0;Label{text:"NEXUS //";color:root.ink;font.bold:true;font.pixelSize:17}Label{text:"COMMAND CORE";color:root.ink;font.pixelSize:16}}
                        }
                        Mono{text:"●  LOCAL AI ONLINE";color:root.green}
                        Item{Layout.preferredHeight:4}
                        Repeater{model:[{t:"Hoje",i:"home"},{t:"Demandas",i:"tasks"},{t:"Memória",i:"brain"},{t:"Arquivos",i:"folder"},{t:"Rotinas",i:"clock"},{t:"Configurações",i:"gear"}];Item{Layout.fillWidth:true;implicitHeight:42;Rectangle{anchors.fill:parent;radius:8;color:root.currentSection===modelData.t?"#a6162b35":"transparent";Rectangle{visible:root.currentSection===modelData.t;anchors.fill:parent;anchors.margins:2;radius:6;color:"transparent";border.width:3;border.color:"#1819e7f3"}Rectangle{visible:root.currentSection===modelData.t;width:3;radius:2;color:root.cyan;anchors.left:parent.left;anchors.top:parent.top;anchors.bottom:parent.bottom}RowLayout{anchors.fill:parent;anchors.leftMargin:14;spacing:12;Glyph{kind:modelData.i;stroke:root.currentSection===modelData.t?root.cyan:"#9db0ba";Layout.preferredWidth:18;Layout.preferredHeight:18}Label{text:modelData.t;color:root.currentSection===modelData.t?root.ink:"#bdcbd1";font.pixelSize:12}Item{Layout.fillWidth:true}}MouseArea{anchors.fill:parent;cursorShape:Qt.PointingHandCursor;onClicked:root.currentSection=modelData.t}}}}
                        Item{Layout.preferredHeight:4}
                        CButton{Layout.fillWidth:true;text:"＋  Nova demanda";accent:true;onClicked:newTaskDialog.open()}
                        Item{Layout.fillHeight:true}
                        MetricCard{metricLabel:"CPU";metricValue:Math.round(systemMonitor.cpuPercent)+"%";metricStatus:"NORMAL";metricIcon:"cpu";metricValues:systemMonitor.cpuHistory}
                        MetricCard{metricLabel:"VOICE";metricValue:root.talking?"TALKING":"READY";metricStatus:"READY";metricIcon:"mic";metricValues:systemMonitor.voiceHistory}
                        MetricCard{metricLabel:"LOCAL DB";metricValue:systemMonitor.dbSize;metricStatus:"SYNC   ✓";metricIcon:"db";metricValues:[24,31,19,43,28,48,22,36,27,41]}
                    }
                }

                // CENTER
                ColumnLayout { Layout.fillWidth:true; Layout.fillHeight:true; spacing:12
                    Panel { Layout.fillWidth:true; Layout.preferredHeight:250; clip:true
                        Canvas { anchors.fill:parent; opacity:.44; onPaint:{var c=getContext("2d");c.reset();var base=height*.42;for(var i=0;i<58;i++){var x=width*.37+i*width*.012,h=18+(i*37%105);c.fillStyle=i%4===0?"#0b3140":"#0a2632";c.fillRect(x,base-h,8+(i%3)*3,h);if(i%5===0){c.fillStyle="#1baab9";c.fillRect(x+3,base-h+9,1,2)}}var grad=c.createLinearGradient(0,0,0,height);grad.addColorStop(0,"#07131b00");grad.addColorStop(1,"#07131bee");c.fillStyle=grad;c.fillRect(0,0,width,height)} }
                        ColumnLayout { anchors.fill:parent; anchors.margins:18; spacing:12
                            RowLayout{Layout.fillWidth:true;Rectangle{width:38;height:38;radius:19;color:"#50152a34";Glyph{anchors.centerIn:parent;width:23;height:23;kind:"moon";stroke:"#9bb7c5"}}ColumnLayout{spacing:1;Label{text:"Boa noite.";color:root.ink;font.pixelSize:23;font.bold:true}Label{text:"Estas são as coisas que merecem sua atenção agora.";color:root.muted;font.pixelSize:11}}Item{Layout.fillWidth:true}Mono{text:taskStore.tasks.length+" DEMANDAS"}}
                            RowLayout{Layout.fillWidth:true;Layout.fillHeight:true;spacing:10
                                Repeater{model:taskStore.tasks;Rectangle{property bool selected:taskStore.selectedTask&&taskStore.selectedTask.id===modelData.id;Layout.fillWidth:true;Layout.fillHeight:true;radius:9;color:selected?"#a8122d38":"#8a0d1c24";border.width:1;border.color:selected?root.cyan:"#362b4a54";Rectangle{visible:parent.selected;anchors.fill:parent;anchors.margins:2;radius:7;color:"transparent";border.width:4;border.color:"#1a19e7f3"}ColumnLayout{anchors.fill:parent;anchors.margins:12;spacing:6;Glyph{kind:"folder";stroke:parent.parent.selected?root.cyan:"#a8bbc4";Layout.preferredWidth:22;Layout.preferredHeight:22}Label{text:modelData.title;color:root.ink;font.bold:true;font.pixelSize:13;elide:Text.ElideRight;Layout.fillWidth:true}Item{Layout.fillHeight:true}RowLayout{spacing:7;Rectangle{width:68;height:20;radius:10;color:"#8a17303a";Mono{anchors.centerIn:parent;text:modelData.status;font.pixelSize:7}}Rectangle{width:54;height:20;radius:10;color:modelData.priority==="ALTA"?"#9a4b2029":modelData.priority==="BAIXA"?"#9a173d2a":"#9a4a3a17";Mono{anchors.centerIn:parent;text:modelData.priority;color:modelData.priority==="ALTA"?"#ff7f88":modelData.priority==="BAIXA"?"#6de790":"#e4be51";font.pixelSize:7}}}Mono{text:modelData.dueText||"Sem prazo";font.pixelSize:8}}MouseArea{anchors.fill:parent;cursorShape:Qt.PointingHandCursor;onClicked:taskStore.selectTask(modelData.id)}}}
                            }
                        }
                    }

                    Panel { Layout.fillWidth:true; Layout.fillHeight:true
                        ColumnLayout{anchors.fill:parent;anchors.margins:18;spacing:12
                            RowLayout{Layout.fillWidth:true;ColumnLayout{spacing:2;Mono{text:"DEMANDA ATUAL"}Label{text:taskStore.selectedTask.title||"Nenhuma demanda";color:root.ink;font.pixelSize:22} }Item{Layout.fillWidth:true}Rectangle{width:105;height:28;radius:14;color:"#0b2b25";border.color:"#1a694d";Mono{anchors.centerIn:parent;text:"●  "+(taskStore.selectedTask.status||"—");color:root.green}}}
                            RowLayout{Layout.fillWidth:true;spacing:10;Repeater{model:[{k:"PRAZO",v:taskStore.selectedTask.dueText||"Sem prazo",s:"Prazo informado",i:"clock"},{k:"STATUS",v:taskStore.selectedTask.status||"—",s:"Persistido localmente",i:"tasks"},{k:"PRIORIDADE",v:taskStore.selectedTask.priority||"NORMAL",s:"Banco SQLite",i:"brain"}];Rectangle{Layout.fillWidth:true;implicitHeight:75;radius:9;color:root.panel2;border.color:root.line;RowLayout{anchors.fill:parent;anchors.margins:10;Glyph{kind:modelData.i;stroke:"#a9bdc6";Layout.preferredWidth:22;Layout.preferredHeight:22}ColumnLayout{Mono{text:modelData.k}Label{text:modelData.v;color:root.ink;font.pixelSize:13;font.bold:true}Mono{text:modelData.s;font.pixelSize:8}}}}}}
                            Mono{text:"DESCRIÇÃO"}
                            Label{text:taskStore.selectedTask.description||"Sem descrição.";color:root.muted;font.pixelSize:11;wrapMode:Text.WordWrap;Layout.fillWidth:true}
                            Rectangle{Layout.fillWidth:true;height:1;color:root.line}
                            RowLayout{Layout.fillWidth:true;Mono{text:"✦  ASSISTANT INSIGHT";color:root.ink}Item{Layout.fillWidth:true}CButton{text:"Ver detalhes";Layout.preferredWidth:96;Layout.preferredHeight:30}}
                            Label{Layout.fillWidth:true;Layout.fillHeight:true;text:"Para concluir a cotação com mais eficiência, recomendo priorizar fornecedores\nque já passaram pelo processo de homologação e possuem histórico de entrega.\nPosso comparar automaticamente as propostas assim que os arquivos forem enviados.";color:root.muted;font.pixelSize:11;wrapMode:Text.WordWrap}
                            RowLayout{Layout.fillWidth:true;spacing:10;CButton{text:taskStore.selectedTask.status==="CONCLUÍDA"?"Reabrir demanda":"Concluir demanda";accent:true;Layout.preferredWidth:210;onClicked:if(taskStore.selectedTask.id)taskStore.toggleDone(taskStore.selectedTask.id)}CButton{text:"Editar";Layout.fillWidth:true;onClicked:editTaskDialog.prepare()}CButton{text:"Excluir";Layout.preferredWidth:82;onClicked:deleteDialog.open()}}
                        }
                    }
                }

                // RIGHT NYRA
                Rectangle { Layout.preferredWidth:390; Layout.fillHeight:true; radius:14; border.color:"#3332525d";gradient:Gradient{GradientStop{position:0;color:"#ce0b1d27"}GradientStop{position:.6;color:"#ad07151d"}GradientStop{position:1;color:"#ca091922"}}
                    ColumnLayout{anchors.fill:parent;anchors.margins:16;spacing:10
                        RowLayout{Layout.fillWidth:true;ColumnLayout{spacing:0;Label{text:"NYRA";color:root.ink;font.pixelSize:24;font.letterSpacing:2}Mono{text:"PERSONAL INTELLIGENCE"}}Item{Layout.fillWidth:true}CButton{Layout.preferredWidth:38;Layout.preferredHeight:34;Glyph{anchors.centerIn:parent;width:18;height:18;kind:"sliders";stroke:"#9fb5bf"}}}
                        Orb{Layout.fillWidth:true;Layout.preferredHeight:220}
                        Rectangle{Layout.fillWidth:true;implicitHeight:50;radius:8;color:"#8607131a";border.color:"#3b31515b";RowLayout{anchors.fill:parent;anchors.margins:10;Rectangle{width:7;height:7;radius:4;color:root.green}Mono{text:"ESTADO // ";color:"#a8bbc3"}Mono{text:"DISPONÍVEL";color:root.cyan}Item{Layout.fillWidth:true}Wave{width:108;active:root.talking}}}
                        Rectangle{Layout.fillWidth:true;implicitHeight:98;radius:9;color:"#8f0b1a22";border.color:"#3b31515b";RowLayout{anchors.fill:parent;anchors.margins:11;Rectangle{width:48;height:48;radius:24;color:"#06151c";Image{anchors.fill:parent;anchors.margins:2;source:"../assets/nyra-orb.png";fillMode:Image.PreserveAspectCrop;smooth:true}}Label{Layout.fillWidth:true;text:root.assistantText;color:root.ink;font.pixelSize:11;wrapMode:Text.WordWrap}Mono{text:"22:42";font.pixelSize:7}}}
                        Rectangle{Layout.fillWidth:true;implicitHeight:36;radius:7;color:"#2b1118";border.color:"#6b2632";RowLayout{anchors.fill:parent;anchors.margins:8;Mono{text:"▣  NETWORK // LOCKED";color:root.red}Item{Layout.fillWidth:true}Mono{text:"Internet bloqueada";color:"#c06671"}}}
                        RowLayout{Layout.fillWidth:true;Mono{text:"MEMÓRIA RECENTE";color:root.ink}Item{Layout.fillWidth:true}Mono{text:"Ver tudo"}}
                        Repeater{model:["Você pediu para priorizar fornecedores\ncom histórico de entrega.","Preferência: Respostas objetivas e com\ntabelas comparativas.","Projeto 3DRN Store em andamento.\nFoco em automação."];RowLayout{Layout.fillWidth:true;Rectangle{width:28;height:28;radius:5;color:"#0c222b";Mono{anchors.centerIn:parent;text:"□"}}Label{Layout.fillWidth:true;text:modelData;color:root.muted;font.pixelSize:9;wrapMode:Text.WordWrap}Mono{text:index===0?"22:10":index===1?"21:47":"Ontem";font.pixelSize:7}}}
                        Item{Layout.fillHeight:true}
                    }
                }
            }
            // footer command dock
            Rectangle{id:commandDock;anchors.left:parent.left;anchors.right:parent.right;anchors.bottom:parent.bottom;height:76;radius:12;border.width:1;border.color:"#5a34717e";gradient:Gradient{GradientStop{position:0;color:"#d0143543"}GradientStop{position:.18;color:"#c00c2632"}GradientStop{position:1;color:"#d0061821"}}
                Rectangle{anchors.fill:parent;anchors.margins:2;radius:10;color:"transparent";border.width:1;border.color:"#3024dbea"}
                RowLayout{anchors.fill:parent;spacing:0
                    RowLayout{Layout.fillWidth:true;Layout.fillHeight:true;Layout.leftMargin:26;Layout.rightMargin:18;spacing:15;Wave{Layout.preferredWidth:34;Layout.preferredHeight:36;active:root.talking}Rectangle{Layout.preferredWidth:1;Layout.preferredHeight:42;color:"#8119e7f3"}Label{text:"> Fale ou digite um comando...";color:"#9db1ba";font.pixelSize:14;Layout.fillWidth:true}Rectangle{Layout.preferredWidth:1;Layout.preferredHeight:30;color:"#303f6470"}Mono{text:"ESC para cancelar"}Item{Layout.preferredWidth:82;Layout.preferredHeight:66;Canvas{anchors.fill:parent;opacity:.65;onPaint:{var c=getContext("2d");c.reset();c.fillStyle=root.cyan;for(var i=0;i<12;i++){var a=i/12*6.283,r=30+(i%3)*3;c.globalAlpha=.12+(i%4)*.08;c.beginPath();c.arc(width/2+Math.cos(a)*r,height/2+Math.sin(a)*r,i%4===0?2:1,0,6.283);c.fill()}}Rectangle{anchors.centerIn:parent;width:60;height:60;radius:30;color:"#76103442";border.width:1;border.color:"#6e19ddea";Rectangle{anchors.centerIn:parent;width:40;height:40;radius:20;color:"#c20a5363";border.width:1;border.color:"#7429eaf4";Glyph{anchors.centerIn:parent;width:20;height:20;kind:"mic";stroke:"#d5ffff"}}}Wave{anchors.left:parent.left;anchors.right:parent.right;anchors.verticalCenter:parent.verticalCenter;height:22;active:root.talking;z:-1}}CButton{text:"⋮";Layout.preferredWidth:44;Layout.preferredHeight:44}}}
                }
            }
            Panel { z:30;visible:root.currentSection!=="Hoje";anchors.left:parent.left;anchors.leftMargin:289;anchors.right:parent.right;anchors.rightMargin:12;anchors.top:parent.top;anchors.topMargin:12;anchors.bottom:commandDock.top;anchors.bottomMargin:10
                Rectangle{anchors.fill:parent;anchors.margins:1;radius:11;color:"#f008171f"}
                ColumnLayout{anchors.fill:parent;anchors.margins:22;spacing:14
                    RowLayout{Layout.fillWidth:true;Glyph{kind:root.currentSection==="Demandas"?"tasks":root.currentSection==="Memória"?"brain":root.currentSection==="Arquivos"?"folder":root.currentSection==="Rotinas"?"clock":"gear";stroke:root.cyan;Layout.preferredWidth:28;Layout.preferredHeight:28}Label{text:root.currentSection;color:root.ink;font.pixelSize:25;font.bold:true}Item{Layout.fillWidth:true}CButton{visible:root.currentSection==="Demandas";text:"＋ Nova demanda";accent:true;Layout.preferredWidth:160;onClicked:newTaskDialog.open()}}
                    Rectangle{Layout.fillWidth:true;height:1;color:root.line}
                    ListView{visible:root.currentSection==="Demandas";Layout.fillWidth:true;Layout.fillHeight:true;clip:true;spacing:9;model:taskStore.tasks
                        delegate:Rectangle{required property var modelData;width:ListView.view.width;height:78;radius:9;color:taskStore.selectedTask.id===modelData.id?"#b012303a":"#8c091922";border.width:1;border.color:taskStore.selectedTask.id===modelData.id?root.cyan:root.line
                            MouseArea{anchors.fill:parent;cursorShape:Qt.PointingHandCursor;onClicked:taskStore.selectTask(modelData.id)}
                            RowLayout{anchors.fill:parent;anchors.margins:12;ColumnLayout{Layout.fillWidth:true;Label{text:modelData.title;color:root.ink;font.bold:true;font.pixelSize:14}Label{text:modelData.description||"Sem descrição";color:root.muted;font.pixelSize:10;elide:Text.ElideRight;Layout.fillWidth:true}}Mono{text:modelData.priority;color:modelData.priority==="ALTA"?root.red:root.cyan}Mono{text:modelData.status;color:modelData.status==="CONCLUÍDA"?root.muted:root.green}CButton{text:"Editar";Layout.preferredWidth:76;onClicked:{taskStore.selectTask(modelData.id);editTaskDialog.prepare()}}}
                        }
                    }
                    ColumnLayout{visible:root.currentSection!=="Demandas";Layout.fillWidth:true;Layout.fillHeight:true;spacing:12
                        Label{text:root.currentSection==="Memória"?"A memória local será organizada aqui.":root.currentSection==="Arquivos"?"Arquivos vinculados às demandas.":root.currentSection==="Rotinas"?"Rotinas e lembretes recorrentes.":"Preferências do aplicativo";color:root.ink;font.pixelSize:18}
                        Label{Layout.fillWidth:true;text:root.currentSection==="Memória"?"A seção já possui navegação real e está reservada para a memória persistente que será integrada junto ao núcleo de IA.":root.currentSection==="Arquivos"?"Nesta etapa, a página está pronta para receber anexos locais. A leitura inteligente dos arquivos será habilitada com a IA.":root.currentSection==="Rotinas"?"O painel está preparado para agendamentos. A execução automática será adicionada na etapa de ferramentas e permissões.":"As preferências abaixo já respondem na interface e serão persistidas em uma próxima atualização.";color:root.muted;font.pixelSize:13;wrapMode:Text.WordWrap}
                        Rectangle{visible:root.currentSection==="Configurações";Layout.fillWidth:true;implicitHeight:62;radius:9;color:root.panel2;border.color:root.line;RowLayout{anchors.fill:parent;anchors.margins:12;ColumnLayout{Label{text:"Notificações";color:root.ink;font.bold:true}Mono{text:"Exibir avisos da Nyra"}}Item{Layout.fillWidth:true}Switch{checked:root.notificationsEnabled;onToggled:root.notificationsEnabled=checked}}}
                        Rectangle{visible:root.currentSection==="Configurações";Layout.fillWidth:true;implicitHeight:62;radius:9;color:root.panel2;border.color:root.line;RowLayout{anchors.fill:parent;anchors.margins:12;ColumnLayout{Label{text:"Iniciar com o Windows";color:root.ink;font.bold:true}Mono{text:"Preferência visual nesta versão"}}Item{Layout.fillWidth:true}Switch{checked:root.startWithWindows;onToggled:root.startWithWindows=checked}}}
                        Item{Layout.fillHeight:true}
                    }
                }
            }
        }

        // COMPACT — independent single-column composition with explicit sizes.
        Flickable {
            anchors.fill:parent; visible:root.compactMode; clip:true
            contentWidth:width; contentHeight:compactCol.height+28
            ScrollBar.vertical:ScrollBar{}
            Column {
                id:compactCol; width:parent.width-28; x:14; y:14; spacing:12
                Item {
                    width:parent.width; height:78
                    Column { anchors.left:parent.left; anchors.verticalCenter:parent.verticalCenter; spacing:5
                        Mono{text:"PERSONAL OPERATIONS NODE // COMPACT";font.pixelSize:8}
                        Label{text:"NEXUS // COMMAND CORE";color:root.ink;font.pixelSize:22;font.bold:true}
                    }
                }
                Row {
                    width:parent.width; height:42; spacing:7
                    Repeater { model:["●  LOCAL AI","NETWORK // LOCKED","MEMORY // ACTIVE"]
                        Rectangle { width:(compactCol.width-14)/3;height:42;radius:8;color:"#0a171d";border.color:root.line
                            Mono{anchors.centerIn:parent;text:modelData;color:index===0?root.green:root.muted;font.pixelSize:7}
                        }
                    }
                }
                Panel {
                    width:parent.width; height:285
                    Column { anchors.fill:parent; anchors.margins:14; spacing:7
                        Row { width:parent.width; height:34
                            Column { width:parent.width-40; spacing:1;Label{text:"NYRA";color:root.ink;font.pixelSize:20;font.bold:true}Mono{text:"PERSONAL INTELLIGENCE";font.pixelSize:7} }
                            CButton { width:36;height:32;Glyph{anchors.centerIn:parent;width:18;height:18;kind:"sliders";stroke:"#9fb5bf"} }
                        }
                        Orb { width:parent.width; height:155 }
                        Rectangle { width:parent.width;height:55;radius:9;color:"#07131a";border.color:root.line
                            Row { anchors.fill:parent;anchors.margins:11;spacing:10;Mono{anchors.verticalCenter:parent.verticalCenter;text:"●  ESTADO // DISPONÍVEL";color:root.green;font.pixelSize:8}Item{width:Math.max(10,parent.width-245);height:1}Wave{width:100;height:30;active:root.talking} }
                        }
                    }
                }
                Panel {
                    width:parent.width;height:292
                    Column { anchors.fill:parent;anchors.margins:14;spacing:7
                        Mono{text:"NAVEGAÇÃO";color:root.ink;font.pixelSize:8}
                        Repeater { model:[{t:"Hoje",i:"home"},{t:"Demandas",i:"tasks"},{t:"Memória",i:"brain"},{t:"Arquivos",i:"folder"},{t:"Rotinas",i:"clock"},{t:"Configurações",i:"gear"}]
                            Rectangle { width:parent.width;height:34;radius:7;color:root.currentSection===modelData.t?"#a6162b35":"transparent";border.width:root.currentSection===modelData.t?1:0;border.color:root.currentSection===modelData.t?root.cyan:"transparent"
                                Rectangle{visible:root.currentSection===modelData.t;anchors.fill:parent;anchors.margins:2;radius:5;color:"transparent";border.width:3;border.color:"#1819e7f3"}
                                Rectangle{visible:root.currentSection===modelData.t;width:3;radius:2;color:root.cyan;anchors.left:parent.left;anchors.top:parent.top;anchors.bottom:parent.bottom}
                                Row{anchors.fill:parent;anchors.leftMargin:14;spacing:12;Glyph{width:17;height:17;anchors.verticalCenter:parent.verticalCenter;kind:modelData.i;stroke:root.currentSection===modelData.t?root.cyan:"#9db0ba"}Label{anchors.verticalCenter:parent.verticalCenter;text:modelData.t;color:root.ink;font.pixelSize:11}}
                                MouseArea{anchors.fill:parent;cursorShape:Qt.PointingHandCursor;onClicked:root.currentSection=modelData.t}
                            }
                        }
                        CButton { width:parent.width;height:38;text:"＋  Nova demanda";accent:true;onClicked:newTaskDialog.open() }
                    }
                }
                Panel {
                    width:parent.width;height:335
                    Column { anchors.fill:parent;anchors.margins:14;spacing:9
                        Mono{text:"FOCUS // HOJE";font.pixelSize:8}
                        Label{text:"Boa noite.";color:root.ink;font.pixelSize:22;font.bold:true}
                        Label{width:parent.width;text:"Estas são as coisas que merecem sua atenção agora.";color:root.muted;font.pixelSize:12;wrapMode:Text.WordWrap}
                        Repeater { model:taskStore.tasks.slice(0,3)
                            Rectangle { property bool selected:taskStore.selectedTask.id===modelData.id;width:parent.width;height:65;radius:9;color:selected?"#102a2c":"#08141a";border.color:selected?root.cyan:root.line
                                Rectangle{visible:parent.selected;anchors.fill:parent;anchors.margins:2;radius:7;color:"transparent";border.width:4;border.color:"#1a19e7f3"}
                                Column{anchors.fill:parent;anchors.margins:10;spacing:4;Label{text:modelData.title;color:root.ink;font.pixelSize:14;font.bold:true}Mono{text:modelData.priority==="ALTA"?"▲ PRIORIDADE ALTA":"◉ "+modelData.status;font.pixelSize:7}}
                                MouseArea{anchors.fill:parent;cursorShape:Qt.PointingHandCursor;onClicked:taskStore.selectTask(modelData.id)}
                            }
                        }
                    }
                }
                Panel {
                    width:parent.width;height:225
                    Column { anchors.fill:parent;anchors.margins:14;spacing:9
                        Mono{text:"ACTIVE OBJECT";font.pixelSize:8}
                        Label{text:taskStore.selectedTask.title||"Nenhuma demanda";color:root.ink;font.pixelSize:19;font.bold:true}
                        Label{width:parent.width;text:taskStore.selectedTask.description||"Sem descrição.";color:root.muted;font.pixelSize:12;wrapMode:Text.WordWrap}
                        Rectangle{width:parent.width;height:95;radius:9;color:"#08141a";border.color:root.line;Column{anchors.fill:parent;anchors.margins:10;spacing:6;Mono{text:"AÇÕES DA DEMANDA";color:root.cyan;font.pixelSize:8}Row{spacing:8;CButton{width:125;height:34;text:taskStore.selectedTask.status==="CONCLUÍDA"?"Reabrir":"Concluir";accent:true;onClicked:if(taskStore.selectedTask.id)taskStore.toggleDone(taskStore.selectedTask.id)}CButton{width:90;height:34;text:"Editar";onClicked:editTaskDialog.prepare()}CButton{width:90;height:34;text:"Excluir";onClicked:deleteDialog.open()}}}}
                    }
                }
                Rectangle {
                    width:parent.width;height:64;radius:10;color:"#061118";border.color:root.line
                    Row{anchors.fill:parent;anchors.margins:9;spacing:9;Wave{width:25;height:30;anchors.verticalCenter:parent.verticalCenter}Label{width:parent.width-112;anchors.verticalCenter:parent.verticalCenter;text:"> Fale ou digite um comando...";elide:Text.ElideRight;color:root.muted;font.pixelSize:11}Rectangle{width:44;height:44;radius:22;color:"#9a103845";border.width:1;border.color:"#6719e7f3";Glyph{anchors.centerIn:parent;width:18;height:18;kind:"mic";stroke:"#d5ffff"}}}
                }
                Item{width:1;height:10}
            }
        }
    }

    Dialog{
        id:newTaskDialog;title:"Nova demanda";modal:true;width:Math.min(root.width-40,520);anchors.centerIn:Overlay.overlay
        standardButtons:Dialog.Ok|Dialog.Cancel
        onAccepted:{taskStore.addTask(newTitle.text,newDescription.text,newPriority.currentText,newDue.text);newTitle.clear();newDescription.clear();newDue.clear();newPriority.currentIndex=0;root.currentSection="Hoje"}
        contentItem:ColumnLayout{spacing:10;TextField{id:newTitle;Layout.fillWidth:true;placeholderText:"Título da demanda"}TextArea{id:newDescription;Layout.fillWidth:true;Layout.preferredHeight:100;placeholderText:"Descrição";wrapMode:TextEdit.WordWrap}RowLayout{Layout.fillWidth:true;ComboBox{id:newPriority;model:["NORMAL","ALTA","BAIXA"];Layout.preferredWidth:140}TextField{id:newDue;Layout.fillWidth:true;placeholderText:"Prazo, por exemplo: Amanhã, 18:00"}}}
    }
    Dialog{
        id:editTaskDialog;title:"Editar demanda";modal:true;width:Math.min(root.width-40,520);anchors.centerIn:Overlay.overlay
        standardButtons:Dialog.Save|Dialog.Cancel
        function prepare(){if(!taskStore.selectedTask.id)return;editTitle.text=taskStore.selectedTask.title||"";editDescription.text=taskStore.selectedTask.description||"";editPriority.currentIndex=Math.max(0,["NORMAL","ALTA","BAIXA"].indexOf(taskStore.selectedTask.priority));editDue.text=taskStore.selectedTask.dueText||"";open()}
        onAccepted:taskStore.updateTask(taskStore.selectedTask.id,editTitle.text,editDescription.text,editPriority.currentText,editDue.text)
        contentItem:ColumnLayout{spacing:10;TextField{id:editTitle;Layout.fillWidth:true;placeholderText:"Título da demanda"}TextArea{id:editDescription;Layout.fillWidth:true;Layout.preferredHeight:100;placeholderText:"Descrição";wrapMode:TextEdit.WordWrap}RowLayout{Layout.fillWidth:true;ComboBox{id:editPriority;model:["NORMAL","ALTA","BAIXA"];Layout.preferredWidth:140}TextField{id:editDue;Layout.fillWidth:true;placeholderText:"Prazo"}}}
    }
    Dialog{
        id:deleteDialog;title:"Excluir demanda?";modal:true;width:380;anchors.centerIn:Overlay.overlay
        standardButtons:Dialog.Yes|Dialog.No
        onAccepted:if(taskStore.selectedTask.id)taskStore.deleteTask(taskStore.selectedTask.id)
        contentItem:Label{text:"Esta ação remove a demanda do banco local.";color:root.ink;padding:14}
    }
}
