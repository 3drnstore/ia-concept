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
    color: "#02070b"
    flags: Qt.Window | Qt.FramelessWindowHint
    title: "NYRA // NEXUS COMMAND CORE"

    property bool compactMode: false
    property real savedW: 1440
    property real savedH: 840
    property bool talking: false
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
    component Panel: Rectangle { color:root.panel; radius:12; border.width:1; border.color:root.line }
    component CButton: Button {
        id:b; property bool accent:false; property bool leftAligned:false; implicitHeight:42
        background: Rectangle { radius:8; color:b.down?"#a117414a":b.hovered?"#a6102c35":b.accent?"#b314303a":"#0010171d"; border.width:b.accent?1:0; border.color:b.accent?"#b919e7f3":"transparent"
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
            else if(kind==="moon"){c.beginPath();c.arc(w*.49,h*.49,w*.34,.35,5.95);c.arc(w*.62,h*.42,w*.29,5.1,1.45,true);c.stroke()}
            else if(kind==="wave"){for(var z=0;z<7;z++){var hh=h*(.18+.65*Math.abs(Math.sin(z*1.7)));line(w*(.15+z*.115),h*.5-hh*.5,w*(.15+z*.115),h*.5+hh*.5)}}
            else if(kind==="windows"){for(var n=0;n<4;n++){var xx=n%2,yy=Math.floor(n/2);c.fillRect(w*(.12+xx*.4),h*(.12+yy*.4),w*.3,h*.3)}}
            else if(kind==="cloud"){c.beginPath();c.moveTo(w*.18,h*.66);c.bezierCurveTo(w*.08,h*.45,w*.28,h*.36,w*.39,h*.43);c.bezierCurveTo(w*.48,h*.16,w*.78,h*.26,w*.78,h*.46);c.bezierCurveTo(w*.96,h*.5,w*.88,h*.7,w*.7,h*.7);c.lineTo(w*.28,h*.7);c.stroke()}
            else if(kind==="cube"){c.beginPath();c.moveTo(w*.5,h*.12);c.lineTo(w*.82,h*.3);c.lineTo(w*.82,h*.68);c.lineTo(w*.5,h*.88);c.lineTo(w*.18,h*.68);c.lineTo(w*.18,h*.3);c.closePath();c.stroke();line(w*.18,h*.3,w*.5,h*.5);line(w*.82,h*.3,w*.5,h*.5);line(w*.5,h*.5,w*.5,h*.88)}
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
        Layout.fillWidth:true; implicitHeight:58; radius:8; color:"#a80a1820"; border.color:"#3b31515b"
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
        Image { id:orbImage; anchors.centerIn:parent; width:Math.min(parent.width*.78,parent.height*.98); height:width; source:"../assets/nyra-orb.png"; fillMode:Image.PreserveAspectFit; opacity:.94; smooth:true
            NumberAnimation on rotation { from:0; to:360; duration:24000; loops:Animation.Infinite; running:true }
        }
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

    // frameless move / resize
    MouseArea { anchors.left:parent.left;anchors.top:parent.top;anchors.bottom:parent.bottom;width:5;cursorShape:Qt.SizeHorCursor;onPressed:root.startSystemResize(Qt.LeftEdge) }
    MouseArea { anchors.right:parent.right;anchors.top:parent.top;anchors.bottom:parent.bottom;width:5;cursorShape:Qt.SizeHorCursor;onPressed:root.startSystemResize(Qt.RightEdge) }
    MouseArea { anchors.left:parent.left;anchors.right:parent.right;anchors.bottom:parent.bottom;height:5;cursorShape:Qt.SizeVerCursor;onPressed:root.startSystemResize(Qt.BottomEdge) }

    Rectangle {
        id:topbar; z:50; height:38; anchors.left:parent.left;anchors.right:parent.right;anchors.top:parent.top
        color:"#03090d"; border.color:root.line
        RowLayout { anchors.fill:parent; anchors.leftMargin:14; anchors.rightMargin:10
            Mono { text:root.compactMode?"N.Y.R.A. // COMPACT":"N.Y.R.A.   NEURAL YIELDING REASONING ASSISTANT"; color:root.compactMode?root.cyan:root.ink }
            Item { Layout.fillWidth:true }
            CButton { Layout.preferredWidth:116; Layout.preferredHeight:26; text:root.compactMode?"EXPANDIR":"COMPACTAR"; onClicked:root.toggleCompact() }
            CButton { Layout.preferredWidth:64; Layout.preferredHeight:26; text:"SAIR"; onClicked:Qt.quit() }
        }
        DragHandler { target:null; onActiveChanged:if(active)root.startSystemMove() }
    }

    Item { anchors.left:parent.left;anchors.right:parent.right;anchors.top:topbar.bottom;anchors.bottom:parent.bottom
        // DESKTOP — proportions copied from approved concept
        Item { anchors.fill:parent; visible:!root.compactMode
            RowLayout { anchors.left:parent.left;anchors.right:parent.right;anchors.top:parent.top;anchors.bottom:commandDock.top;anchors.margins:12;anchors.bottomMargin:10;spacing:12
                // LEFT
                Rectangle { Layout.preferredWidth:265; Layout.fillHeight:true; color:"#061016"; radius:14; border.color:root.line
                    ColumnLayout { anchors.fill:parent; anchors.margins:16; spacing:9
                        RowLayout { spacing:10
                            Rectangle { width:52;height:52;radius:26;color:"#061a22";border.width:1;border.color:root.cyan
                                Canvas{anchors.fill:parent;onPaint:{var c=getContext("2d");c.reset();c.strokeStyle=root.cyan;c.lineWidth=1.5;c.beginPath();c.moveTo(width*.22,height*.26);c.lineTo(width*.78,height*.26);c.lineTo(width*.5,height*.78);c.closePath();c.stroke();c.beginPath();c.moveTo(width*.32,height*.34);c.lineTo(width*.68,height*.34);c.lineTo(width*.5,height*.64);c.closePath();c.stroke()}}
                            }
                            ColumnLayout{spacing:0;Label{text:"NEXUS //";color:root.ink;font.bold:true;font.pixelSize:17}Label{text:"COMMAND CORE";color:root.ink;font.pixelSize:16}}
                        }
                        Mono{text:"●  LOCAL AI ONLINE";color:root.green}
                        Item{Layout.preferredHeight:4}
                        Repeater{model:[{t:"Hoje",i:"home"},{t:"Demandas",i:"tasks"},{t:"Memória",i:"brain"},{t:"Arquivos",i:"folder"},{t:"Rotinas",i:"clock"},{t:"Configurações",i:"gear"}];Item{Layout.fillWidth:true;implicitHeight:42;Rectangle{anchors.fill:parent;radius:8;color:index===0?"#a6162b35":"transparent";Rectangle{visible:index===0;width:3;radius:2;color:root.cyan;anchors.left:parent.left;anchors.top:parent.top;anchors.bottom:parent.bottom}RowLayout{anchors.fill:parent;anchors.leftMargin:14;spacing:12;Glyph{kind:modelData.i;stroke:index===0?root.cyan:"#9db0ba";Layout.preferredWidth:18;Layout.preferredHeight:18}Label{text:modelData.t;color:index===0?root.ink:"#bdcbd1";font.pixelSize:12}Item{Layout.fillWidth:true}}MouseArea{anchors.fill:parent;hoverEnabled:true}}}}
                        Item{Layout.preferredHeight:4}
                        CButton{Layout.fillWidth:true;text:"＋  Nova demanda";accent:true}
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
                            RowLayout{Layout.fillWidth:true;Rectangle{width:38;height:38;radius:19;color:"#50152a34";Glyph{anchors.centerIn:parent;width:23;height:23;kind:"moon";stroke:"#9bb7c5"}}ColumnLayout{spacing:1;Label{text:"Boa noite.";color:root.ink;font.pixelSize:23;font.bold:true}Label{text:"Estas são as coisas que merecem sua atenção agora.";color:root.muted;font.pixelSize:11}}Item{Layout.fillWidth:true}Mono{text:"4 DEMANDAS"}}
                            RowLayout{Layout.fillWidth:true;Layout.fillHeight:true;spacing:10
                                Repeater{model:[
                                    {title:"Cotação Hospital",tag:"TRABALHO",pri:"ALTA",due:"Hoje, 23:59",sel:true},
                                    {title:"3DRN Store",tag:"PROJETO",pri:"MÉDIA",due:"Amanhã, 18:00",sel:false},
                                    {title:"PsicoGestão",tag:"CLIENTE",pri:"ALTA",due:"27 Mai, 09:00",sel:false},
                                    {title:"Organizar arquivos",tag:"PESSOAL",pri:"BAIXA",due:"Sem prazo",sel:false}
                                ];Rectangle{Layout.fillWidth:true;Layout.fillHeight:true;radius:9;color:modelData.sel?"#a8122d38":"#8a0d1c24";border.width:1;border.color:modelData.sel?root.cyan:"#362b4a54";ColumnLayout{anchors.fill:parent;anchors.margins:12;spacing:6;Glyph{kind:index===0?"folder":index===1?"cube":index===2?"brain":"folder";stroke:modelData.sel?root.cyan:"#a8bbc4";Layout.preferredWidth:22;Layout.preferredHeight:22}Label{text:modelData.title;color:root.ink;font.bold:true;font.pixelSize:13;elide:Text.ElideRight;Layout.fillWidth:true}Item{Layout.fillHeight:true}RowLayout{spacing:7;Rectangle{width:62;height:20;radius:10;color:"#8a17303a";Mono{anchors.centerIn:parent;text:modelData.tag;font.pixelSize:7}}Rectangle{width:50;height:20;radius:10;color:modelData.pri==="ALTA"?"#9a4b2029":modelData.pri==="BAIXA"?"#9a173d2a":"#9a4a3a17";Mono{anchors.centerIn:parent;text:modelData.pri;color:modelData.pri==="ALTA"?"#ff7f88":modelData.pri==="BAIXA"?"#6de790":"#e4be51";font.pixelSize:7}}}Mono{text:modelData.due;font.pixelSize:8}}}}
                            }
                        }
                    }

                    Panel { Layout.fillWidth:true; Layout.fillHeight:true
                        ColumnLayout{anchors.fill:parent;anchors.margins:18;spacing:12
                            RowLayout{Layout.fillWidth:true;ColumnLayout{spacing:2;Mono{text:"DEMANDA ATUAL"}Label{text:"Cotação Hospital";color:root.ink;font.pixelSize:22} }Item{Layout.fillWidth:true}Rectangle{width:92;height:28;radius:14;color:"#0b2b25";border.color:"#1a694d";Mono{anchors.centerIn:parent;text:"●  ATIVA";color:root.green}}}
                            RowLayout{Layout.fillWidth:true;spacing:10;Repeater{model:[{k:"PRAZO",v:"Hoje, 23:59",s:"Faltam 1h 16m",i:"clock"},{k:"ANEXOS",v:"4 arquivos",s:"3.2 MB",i:"tasks"},{k:"IA",v:"NYRA (Local)",s:"Modelo: Llama 3.1 8B",i:"brain"}];Rectangle{Layout.fillWidth:true;implicitHeight:75;radius:9;color:root.panel2;border.color:root.line;RowLayout{anchors.fill:parent;anchors.margins:10;Glyph{kind:modelData.i;stroke:"#a9bdc6";Layout.preferredWidth:22;Layout.preferredHeight:22}ColumnLayout{Mono{text:modelData.k}Label{text:modelData.v;color:root.ink;font.pixelSize:13;font.bold:true}Mono{text:modelData.s;font.pixelSize:8}}}}}}
                            Mono{text:"DESCRIÇÃO"}
                            Label{text:"Solicitação de cotação para materiais hospitalares descartáveis.\nComparar preços, prazos e condições de pagamento.";color:root.muted;font.pixelSize:11;wrapMode:Text.WordWrap;Layout.fillWidth:true}
                            Rectangle{Layout.fillWidth:true;height:1;color:root.line}
                            RowLayout{Layout.fillWidth:true;Mono{text:"✦  ASSISTANT INSIGHT";color:root.ink}Item{Layout.fillWidth:true}CButton{text:"Ver detalhes";Layout.preferredWidth:96;Layout.preferredHeight:30}}
                            Label{Layout.fillWidth:true;Layout.fillHeight:true;text:"Para concluir a cotação com mais eficiência, recomendo priorizar fornecedores\nque já passaram pelo processo de homologação e possuem histórico de entrega.\nPosso comparar automaticamente as propostas assim que os arquivos forem enviados.";color:root.muted;font.pixelSize:11;wrapMode:Text.WordWrap}
                            RowLayout{Layout.fillWidth:true;spacing:10;CButton{text:"        Analisar agora";accent:true;Layout.preferredWidth:210;Glyph{kind:"wave";stroke:root.cyan;width:20;height:20;anchors.left:parent.left;anchors.leftMargin:48;anchors.verticalCenter:parent.verticalCenter}}CButton{text:"Conversar";Layout.fillWidth:true;onClicked:root.say("Estou aqui. Pode falar comigo normalmente.")}CButton{text:"•••";Layout.preferredWidth:62}}
                        }
                    }
                }

                // RIGHT NYRA
                Rectangle { Layout.preferredWidth:390; Layout.fillHeight:true; radius:14; color:"#08141b"; border.color:root.line
                    ColumnLayout{anchors.fill:parent;anchors.margins:16;spacing:10
                        RowLayout{Layout.fillWidth:true;ColumnLayout{spacing:0;Label{text:"NYRA";color:root.ink;font.pixelSize:24;font.letterSpacing:2}Mono{text:"PERSONAL INTELLIGENCE"}}Item{Layout.fillWidth:true}CButton{text:"⚙";Layout.preferredWidth:34;Layout.preferredHeight:30}}
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
            Rectangle{id:commandDock;anchors.left:parent.left;anchors.right:parent.right;anchors.bottom:parent.bottom;height:76;color:"#b0061118";border.color:"#54204b58";radius:10
                RowLayout{anchors.fill:parent;spacing:0
                    Rectangle{Layout.preferredWidth:205;Layout.fillHeight:true;color:"#9a050d13";border.color:root.line;RowLayout{anchors.fill:parent;anchors.margins:14;spacing:10;Glyph{kind:"windows";stroke:root.ink;Layout.preferredWidth:22;Layout.preferredHeight:22}ColumnLayout{spacing:0;Mono{text:"22:43";color:root.ink;font.pixelSize:8}Mono{text:"25 MAI 2025";font.pixelSize:7}}Item{Layout.fillWidth:true}Glyph{kind:"cloud";stroke:"#b6c8cf";Layout.preferredWidth:22;Layout.preferredHeight:22}ColumnLayout{spacing:0;Mono{text:"19°C";color:root.ink;font.pixelSize:8}Mono{text:"NUBLADO";font.pixelSize:7}}}}
                    Rectangle{Layout.fillWidth:true;Layout.fillHeight:true;color:"#9f0a1d28";border.color:"#54265b68";radius:9;RowLayout{anchors.fill:parent;anchors.leftMargin:18;anchors.rightMargin:14;spacing:13;Wave{Layout.preferredWidth:28;Layout.preferredHeight:34;active:root.talking}Rectangle{Layout.preferredWidth:1;Layout.preferredHeight:40;color:root.cyan}Label{text:"> Fale ou digite um comando...";color:root.muted;font.pixelSize:14;Layout.fillWidth:true}Rectangle{Layout.preferredWidth:1;Layout.preferredHeight:28;color:root.line}Mono{text:"ESC para cancelar"}Rectangle{Layout.preferredWidth:56;Layout.preferredHeight:56;radius:28;color:"#99103542";border.width:2;border.color:root.cyan;Rectangle{anchors.centerIn:parent;width:36;height:36;radius:18;color:"#c00a5363";border.color:"#12859a";Glyph{anchors.centerIn:parent;width:19;height:19;kind:"mic";stroke:"#c7ffff"}}}CButton{text:"⋮";Layout.preferredWidth:40;Layout.preferredHeight:40}}}
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
                            CButton { width:34;height:30;text:"⚙" }
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
                            Rectangle { width:parent.width;height:34;radius:7;color:index===0?"#a6162b35":"transparent";border.width:index===0?1:0;border.color:index===0?root.cyan:"transparent"
                                Rectangle{visible:index===0;width:3;radius:2;color:root.cyan;anchors.left:parent.left;anchors.top:parent.top;anchors.bottom:parent.bottom}
                                Row{anchors.fill:parent;anchors.leftMargin:14;spacing:12;Glyph{width:17;height:17;anchors.verticalCenter:parent.verticalCenter;kind:modelData.i;stroke:index===0?root.cyan:"#9db0ba"}Label{anchors.verticalCenter:parent.verticalCenter;text:modelData.t;color:root.ink;font.pixelSize:11}}
                            }
                        }
                        CButton { width:parent.width;height:38;text:"＋  Nova demanda";accent:true }
                    }
                }
                Panel {
                    width:parent.width;height:335
                    Column { anchors.fill:parent;anchors.margins:14;spacing:9
                        Mono{text:"FOCUS // HOJE";font.pixelSize:8}
                        Label{text:"Boa noite.";color:root.ink;font.pixelSize:22;font.bold:true}
                        Label{width:parent.width;text:"Estas são as coisas que merecem sua atenção agora.";color:root.muted;font.pixelSize:12;wrapMode:Text.WordWrap}
                        Repeater { model:[{t:"Cotação Hospital",s:"▲ PRIORIDADE ALTA"},{t:"3DRN Store",s:"◉ EM ANDAMENTO"},{t:"PsicoGestão",s:"○ AGUARDANDO"}]
                            Rectangle { width:parent.width;height:65;radius:9;color:index===0?"#102a2c":"#08141a";border.color:index===0?root.cyan:root.line
                                Column{anchors.fill:parent;anchors.margins:10;spacing:4;Label{text:modelData.t;color:root.ink;font.pixelSize:14;font.bold:true}Mono{text:modelData.s;font.pixelSize:7}}
                            }
                        }
                    }
                }
                Panel {
                    width:parent.width;height:225
                    Column { anchors.fill:parent;anchors.margins:14;spacing:9
                        Mono{text:"ACTIVE OBJECT";font.pixelSize:8}
                        Label{text:"Cotação Hospital";color:root.ink;font.pixelSize:19;font.bold:true}
                        Label{width:parent.width;text:"Analisar se os materiais solicitados são compatíveis antes de decidir participação.";color:root.muted;font.pixelSize:12;wrapMode:Text.WordWrap}
                        Rectangle{width:parent.width;height:95;radius:9;color:"#08141a";border.color:root.line;Column{anchors.fill:parent;anchors.margins:10;spacing:6;Mono{text:"✦  ASSISTANT INSIGHT";color:root.cyan;font.pixelSize:8}Label{width:parent.width;text:"Eu começaria confirmando os materiais antes de você gastar tempo com o restante.";color:root.muted;font.pixelSize:11;wrapMode:Text.WordWrap}}}
                    }
                }
                Rectangle {
                    width:parent.width;height:64;radius:10;color:"#061118";border.color:root.line
                    Row{anchors.fill:parent;anchors.margins:9;spacing:9;Wave{width:25;height:30;anchors.verticalCenter:parent.verticalCenter}Label{width:parent.width-112;anchors.verticalCenter:parent.verticalCenter;text:"> Fale ou digite um comando...";elide:Text.ElideRight;color:root.muted;font.pixelSize:11}Rectangle{width:44;height:44;radius:22;color:"#073341";border.width:2;border.color:root.cyan;Label{anchors.centerIn:parent;text:"●";color:root.cyan}}}
                }
                Item{width:1;height:10}
            }
        }
    }
}
