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
    property color panel: "#09151c"
    property color panel2: "#0c1a22"
    property color line: "#1a3641"
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
        id:b; property bool accent:false; implicitHeight:42
        background: Rectangle { radius:8; color:b.down?"#17414a":b.hovered?"#102c35":b.accent?"#0d2f38":"#0a171d"; border.width:b.accent?1.4:1; border.color:b.accent?root.cyan:root.line }
        contentItem: Label { text:b.text; color:b.accent?"#d5fdff":root.ink; font.pixelSize:12; horizontalAlignment:Text.AlignHCenter; verticalAlignment:Text.AlignVCenter }
    }
    component Wave: Canvas {
        id:w; property bool active:root.talking; property real phase:0
        implicitHeight:32
        NumberAnimation on phase { from:0; to:6.283; duration:active?620:2600; loops:Animation.Infinite }
        onPhaseChanged:requestPaint(); onActiveChanged:requestPaint()
        onPaint:{
            var c=getContext("2d"); c.reset(); c.strokeStyle=root.cyan; c.lineWidth=1.25; c.globalAlpha=active?.95:.6; c.beginPath();
            for(var i=0;i<100;i++){ var x=i*width/99, env=Math.sin(i/99*3.14159), amp=active?(8+8*Math.sin(i*.72+phase*2.2)):2.5; var y=height/2+Math.sin(i*.62+phase)*amp*env; if(i===0)c.moveTo(x,y);else c.lineTo(x,y) } c.stroke()
        }
    }
    component Spark: Canvas {
        id:s; required property var values; implicitHeight:22; onValuesChanged:requestPaint()
        onPaint:{ var c=getContext("2d"),d=values||[];c.reset();c.strokeStyle=root.cyan;c.lineWidth=1;c.beginPath();for(var i=0;i<d.length;i++){var x=i*width/Math.max(1,d.length-1),y=height-2-Math.max(0,Math.min(100,d[i]))/100*(height-4);if(i===0)c.moveTo(x,y);else c.lineTo(x,y)}c.stroke() }
    }
    component Orb: Item {
        id:o; property real phase:0; implicitWidth:300; implicitHeight:225
        NumberAnimation on phase { from:0; to:6.283; duration:root.talking?820:6500; loops:Animation.Infinite }
        Canvas { id:orbCanvas; anchors.fill:parent
            onPaint:{
                var c=getContext("2d"),cx=width/2,cy=height/2,t=o.phase;c.reset();
                var g=c.createRadialGradient(cx,cy,2,cx,cy,92);g.addColorStop(0,"#ffffff");g.addColorStop(.05,"#bffcff");g.addColorStop(.13,"#34eff9");g.addColorStop(.28,"#0ab5c8");g.addColorStop(.5,"#0a5364");g.addColorStop(.72,"#071d27");g.addColorStop(1,"#02070b00");c.fillStyle=g;c.beginPath();c.arc(cx,cy,92,0,6.283);c.fill();
                for(var r=54;r<=100;r+=9){c.strokeStyle=(r===81||r===90)?"#5ef8ff":"#16778c";c.globalAlpha=(r===81||r===90)?.72:.32;c.lineWidth=(r===81||r===90)?1.7:1;c.beginPath();c.arc(cx,cy,r,t*(r%4+1),t*(r%4+1)+4.3);c.stroke()}
                c.globalAlpha=.8;for(var i=0;i<74;i++){var a=i/74*6.283+t*.2,rr=94+(i%7)*3;c.fillStyle=i%6===0?"#b6ffff":"#1594a7";c.fillRect(cx+Math.cos(a)*rr,cy+Math.sin(a)*rr,i%6===0?2:1,i%6===0?2:1)}
            }
            Connections { target:o; function onPhaseChanged(){ orbCanvas.requestPaint() } }
        }
        Wave { anchors.left:parent.left; anchors.right:parent.right; anchors.verticalCenter:parent.verticalCenter }
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
            RowLayout { anchors.fill:parent; anchors.margins:12; spacing:12
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
                        Repeater{model:["⌂   Hoje","▤   Demandas","◉   Memória","▭   Arquivos","◴   Rotinas","⚙   Configurações"];CButton{Layout.fillWidth:true;text:modelData;accent:index===0}}
                        Item{Layout.preferredHeight:4}
                        CButton{Layout.fillWidth:true;text:"＋  Nova demanda";accent:true}
                        Item{Layout.fillHeight:true}
                        Repeater{model:[
                            {t:"CPU",v:Math.round(systemMonitor.cpuPercent)+"%",h:systemMonitor.cpuHistory,s:"NORMAL"},
                            {t:"GPU",v:systemMonitor.gpuPercent<0?"N/D":Math.round(systemMonitor.gpuPercent)+"%",h:systemMonitor.gpuHistory,s:systemMonitor.gpuName},
                            {t:"RAM",v:Math.round(systemMonitor.ramPercent)+"%",h:systemMonitor.ramHistory,s:"WINDOWS"},
                            {t:"VOICE",v:root.talking?"TALKING":"READY",h:systemMonitor.voiceHistory,s:"NYRA OUTPUT"},
                            {t:"LOCAL DB",v:systemMonitor.dbSize,h:[40,42,42,43,44,44],s:"SQLITE // SYNC"}
                        ];Rectangle{Layout.fillWidth:true;implicitHeight:58;radius:8;color:"#0a1820";border.color:root.line;RowLayout{anchors.fill:parent;anchors.margins:8;Rectangle{width:30;height:30;radius:6;color:"#102933";Mono{anchors.centerIn:parent;text:modelData.t.charAt(0);color:root.cyan}}ColumnLayout{Layout.preferredWidth:92;spacing:0;Mono{text:modelData.t+" // "+modelData.v;color:root.ink;font.pixelSize:8}Mono{text:modelData.s;font.pixelSize:7;elide:Text.ElideRight;Layout.maximumWidth:90}}Spark{Layout.fillWidth:true;values:modelData.h}}}}
                    }
                }

                // CENTER
                ColumnLayout { Layout.fillWidth:true; Layout.fillHeight:true; spacing:12
                    Panel { Layout.fillWidth:true; Layout.preferredHeight:250; clip:true
                        Canvas { anchors.fill:parent; opacity:.44; onPaint:{var c=getContext("2d");c.reset();var base=height*.42;for(var i=0;i<58;i++){var x=width*.37+i*width*.012,h=18+(i*37%105);c.fillStyle=i%4===0?"#0b3140":"#0a2632";c.fillRect(x,base-h,8+(i%3)*3,h);if(i%5===0){c.fillStyle="#1baab9";c.fillRect(x+3,base-h+9,1,2)}}var grad=c.createLinearGradient(0,0,0,height);grad.addColorStop(0,"#07131b00");grad.addColorStop(1,"#07131bee");c.fillStyle=grad;c.fillRect(0,0,width,height)} }
                        ColumnLayout { anchors.fill:parent; anchors.margins:18; spacing:12
                            RowLayout{Layout.fillWidth:true;ColumnLayout{spacing:1;Label{text:"☾  Boa noite.";color:root.ink;font.pixelSize:23;font.bold:true}Label{text:"Estas são as coisas que merecem sua atenção agora.";color:root.muted;font.pixelSize:11}}Item{Layout.fillWidth:true}Mono{text:"4 DEMANDAS"}}
                            RowLayout{Layout.fillWidth:true;Layout.fillHeight:true;spacing:10
                                Repeater{model:[
                                    {title:"Cotação Hospital",tag:"TRABALHO",pri:"ALTA",due:"Hoje, 23:59",sel:true},
                                    {title:"3DRN Store",tag:"PROJETO",pri:"MÉDIA",due:"Amanhã, 18:00",sel:false},
                                    {title:"PsicoGestão",tag:"CLIENTE",pri:"ALTA",due:"27 Mai, 09:00",sel:false},
                                    {title:"Organizar arquivos",tag:"PESSOAL",pri:"BAIXA",due:"Sem prazo",sel:false}
                                ];Rectangle{Layout.fillWidth:true;Layout.fillHeight:true;radius:9;color:modelData.sel?"#102b34":"#0d1c24";border.width:modelData.sel?2:1;border.color:modelData.sel?root.cyan:root.line;ColumnLayout{anchors.fill:parent;anchors.margins:12;spacing:6;Label{text:modelData.title;color:root.ink;font.bold:true;font.pixelSize:13;elide:Text.ElideRight;Layout.fillWidth:true}Item{Layout.fillHeight:true}RowLayout{spacing:7;Rectangle{width:62;height:20;radius:10;color:"#17303a";Mono{anchors.centerIn:parent;text:modelData.tag;font.pixelSize:7}}Rectangle{width:50;height:20;radius:10;color:modelData.pri==="ALTA"?"#4b2029":modelData.pri==="BAIXA"?"#173d2a":"#4a3a17";Mono{anchors.centerIn:parent;text:modelData.pri;color:modelData.pri==="ALTA"?"#ff7f88":modelData.pri==="BAIXA"?"#6de790":"#e4be51";font.pixelSize:7}}}Mono{text:modelData.due;font.pixelSize:8}}}}
                            }
                        }
                    }

                    Panel { Layout.fillWidth:true; Layout.fillHeight:true
                        ColumnLayout{anchors.fill:parent;anchors.margins:18;spacing:12
                            RowLayout{Layout.fillWidth:true;ColumnLayout{spacing:2;Mono{text:"DEMANDA ATUAL"}Label{text:"Cotação Hospital";color:root.ink;font.pixelSize:22} }Item{Layout.fillWidth:true}Rectangle{width:92;height:28;radius:14;color:"#0b2b25";border.color:"#1a694d";Mono{anchors.centerIn:parent;text:"●  ATIVA";color:root.green}}}
                            RowLayout{Layout.fillWidth:true;spacing:10;Repeater{model:[{k:"PRAZO",v:"Hoje, 23:59",s:"Faltam 1h 16m"},{k:"ANEXOS",v:"4 arquivos",s:"3.2 MB"},{k:"IA",v:"NYRA (Local)",s:"Modelo: Llama 3.1 8B"}];Rectangle{Layout.fillWidth:true;implicitHeight:75;radius:9;color:root.panel2;border.color:root.line;ColumnLayout{anchors.fill:parent;anchors.margins:10;Mono{text:modelData.k}Label{text:modelData.v;color:root.ink;font.pixelSize:13;font.bold:true}Mono{text:modelData.s;font.pixelSize:8}}}}}
                            Mono{text:"DESCRIÇÃO"}
                            Label{text:"Solicitação de cotação para materiais hospitalares descartáveis.\nComparar preços, prazos e condições de pagamento.";color:root.muted;font.pixelSize:11;wrapMode:Text.WordWrap;Layout.fillWidth:true}
                            Rectangle{Layout.fillWidth:true;height:1;color:root.line}
                            RowLayout{Layout.fillWidth:true;Mono{text:"✦  ASSISTANT INSIGHT";color:root.ink}Item{Layout.fillWidth:true}CButton{text:"Ver detalhes";Layout.preferredWidth:96;Layout.preferredHeight:30}}
                            Label{Layout.fillWidth:true;Layout.fillHeight:true;text:"Para concluir a cotação com mais eficiência, recomendo priorizar fornecedores\nque já passaram pelo processo de homologação e possuem histórico de entrega.\nPosso comparar automaticamente as propostas assim que os arquivos forem enviados.";color:root.muted;font.pixelSize:11;wrapMode:Text.WordWrap}
                            RowLayout{Layout.fillWidth:true;spacing:10;CButton{text:"⌁  Analisar agora";accent:true;Layout.preferredWidth:210}CButton{text:"◌  Conversar";Layout.fillWidth:true;onClicked:root.say("Estou aqui. Pode falar comigo normalmente.")}CButton{text:"•••";Layout.preferredWidth:62}}
                        }
                    }
                }

                // RIGHT NYRA
                Rectangle { Layout.preferredWidth:390; Layout.fillHeight:true; radius:14; color:"#08141b"; border.color:root.line
                    ColumnLayout{anchors.fill:parent;anchors.margins:16;spacing:10
                        RowLayout{Layout.fillWidth:true;ColumnLayout{spacing:0;Label{text:"NYRA";color:root.ink;font.pixelSize:24;font.letterSpacing:2}Mono{text:"PERSONAL INTELLIGENCE"}}Item{Layout.fillWidth:true}CButton{text:"⚙";Layout.preferredWidth:34;Layout.preferredHeight:30}}
                        Orb{Layout.fillWidth:true;Layout.preferredHeight:220}
                        Rectangle{Layout.fillWidth:true;implicitHeight:50;radius:8;color:"#07131a";border.color:root.line;RowLayout{anchors.fill:parent;anchors.margins:10;Mono{text:"●  ESTADO // DISPONÍVEL";color:root.green}Item{Layout.fillWidth:true}Wave{width:108;active:root.talking}}}
                        Rectangle{Layout.fillWidth:true;implicitHeight:98;radius:9;color:"#0b1a22";border.color:root.line;RowLayout{anchors.fill:parent;anchors.margins:11;Rectangle{width:34;height:34;radius:17;color:"#0d3040";border.color:root.cyan;Mono{anchors.centerIn:parent;text:"◉";color:root.cyan}}Label{Layout.fillWidth:true;text:root.assistantText;color:root.ink;font.pixelSize:11;wrapMode:Text.WordWrap}Mono{text:"22:42";font.pixelSize:7}}}
                        Rectangle{Layout.fillWidth:true;implicitHeight:36;radius:7;color:"#2b1118";border.color:"#6b2632";RowLayout{anchors.fill:parent;anchors.margins:8;Mono{text:"▣  NETWORK // LOCKED";color:root.red}Item{Layout.fillWidth:true}Mono{text:"Internet bloqueada";color:"#c06671"}}}
                        RowLayout{Layout.fillWidth:true;Mono{text:"MEMÓRIA RECENTE";color:root.ink}Item{Layout.fillWidth:true}Mono{text:"Ver tudo"}}
                        Repeater{model:["Você pediu para priorizar fornecedores\ncom histórico de entrega.","Preferência: Respostas objetivas e com\ntabelas comparativas.","Projeto 3DRN Store em andamento.\nFoco em automação."];RowLayout{Layout.fillWidth:true;Rectangle{width:28;height:28;radius:5;color:"#0c222b";Mono{anchors.centerIn:parent;text:"□"}}Label{Layout.fillWidth:true;text:modelData;color:root.muted;font.pixelSize:9;wrapMode:Text.WordWrap}Mono{text:index===0?"22:10":index===1?"21:47":"Ontem";font.pixelSize:7}}}
                        Item{Layout.fillHeight:true}
                    }
                }
            }
            // footer command dock
            Rectangle{anchors.left:parent.left;anchors.right:parent.right;anchors.bottom:parent.bottom;height:74;color:"#07131a";border.color:root.line;radius:10
                RowLayout{anchors.fill:parent;anchors.leftMargin:22;anchors.rightMargin:18;spacing:12;Label{text:"▥";color:root.cyan;font.pixelSize:22}Rectangle{width:1;height:38;color:root.cyan}Label{text:"> Fale ou digite um comando...";color:root.muted;font.pixelSize:14;Layout.fillWidth:true}Mono{text:"ESC para cancelar"}Rectangle{width:52;height:52;radius:26;color:"#0b3440";border.width:2;border.color:root.cyan;Label{anchors.centerIn:parent;text:"●";color:root.cyan;font.pixelSize:18}}CButton{text:"⋮";Layout.preferredWidth:40;Layout.preferredHeight:40}}
            }
        }

        // COMPACT — separate Android/companion-style composition from approved simulation
        Flickable { anchors.fill:parent; visible:root.compactMode; clip:true; contentWidth:width; contentHeight:compactCol.implicitHeight+24; ScrollBar.vertical:ScrollBar{}
            ColumnLayout{id:compactCol;width:parent.width;anchors.left:parent.left;anchors.right:parent.right;anchors.leftMargin:16;anchors.rightMargin:16;spacing:14
                Item{Layout.preferredHeight:4}
                Mono{text:"PERSONAL OPERATIONS NODE // 03"}
                Label{text:"NEXUS // COMMAND CORE";color:root.ink;font.pixelSize:23;font.bold:true}
                RowLayout{Layout.fillWidth:true;spacing:7;Repeater{model:["● LOCAL AI","NETWORK // LOCKED","MEMORY // ACTIVE"];Rectangle{Layout.fillWidth:true;height:40;radius:8;color:"#0a171d";border.color:root.line;Mono{anchors.centerIn:parent;text:modelData;color:index===0?root.green:root.muted;font.pixelSize:8}}}}
                Rectangle{Layout.fillWidth:true;implicitHeight:560;radius:18;color:"#09151b";border.color:root.line
                    ColumnLayout{anchors.fill:parent;anchors.margins:20;spacing:11;Orb{Layout.alignment:Qt.AlignHCenter;Layout.preferredWidth:270;Layout.preferredHeight:190}Label{text:"NYRA";color:root.ink;font.pixelSize:23;font.bold:true;Layout.alignment:Qt.AlignHCenter}Mono{text:"PERSONAL INTELLIGENCE";Layout.alignment:Qt.AlignHCenter}
                        Rectangle{Layout.fillWidth:true;implicitHeight:145;radius:12;color:"#08141a";border.color:root.line;ColumnLayout{anchors.fill:parent;anchors.margins:14;Mono{text:"ESTADO // DISPONÍVEL";color:root.ink}Wave{Layout.fillWidth:true;active:root.talking}Label{text:root.assistantText;color:root.muted;font.pixelSize:14;wrapMode:Text.WordWrap;horizontalAlignment:Text.AlignHCenter;Layout.fillWidth:true}}}
                        CButton{Layout.fillWidth:true;text:"◉ Iniciar conversa por voz";onClicked:root.say("Estou aqui. Pode falar comigo normalmente.")}
                        CButton{Layout.fillWidth:true;text:"⌁ Simular necessidade de internet"}
                        Rectangle{Layout.fillWidth:true;height:1;color:root.line}
                        Mono{text:"MEMÓRIA RECENTE";color:root.ink}
                        Repeater{model:["Você prefere que eu peça autorização de forma natural por voz.","A internet permanece bloqueada por padrão.","Sua interface deve ter identidade sci-fi/cyberpunk."];Rectangle{Layout.fillWidth:true;implicitHeight:54;radius:9;color:"#08141a";border.color:root.line;Label{anchors.fill:parent;anchors.margins:10;text:modelData;color:root.muted;font.pixelSize:11;wrapMode:Text.WordWrap}}}
                    }
                }
                Rectangle{Layout.fillWidth:true;implicitHeight:530;radius:18;color:"#09151b";border.color:root.line;ColumnLayout{anchors.fill:parent;anchors.margins:20;spacing:12;Mono{text:"NAVIGATION";color:root.ink}Repeater{model:[{n:"◉ Hoje",c:"07"},{n:"▦ Demandas",c:"12"},{n:"◇ Memória",c:""},{n:"⌁ Arquivos",c:""},{n:"↻ Rotinas",c:""}];Rectangle{Layout.fillWidth:true;implicitHeight:58;radius:10;color:index===0?"#10292c":"transparent";border.color:index===0?"#396e40":"transparent";RowLayout{anchors.fill:parent;anchors.margins:12;Label{text:modelData.n;color:root.ink;font.pixelSize:15}Item{Layout.fillWidth:true}Mono{text:modelData.c;color:root.ink;font.pixelSize:13}}}}Item{Layout.preferredHeight:3}Mono{text:"SYSTEM";color:root.ink}Repeater{model:["CPU // NORMAL","VOICE // READY","LOCAL DB // SYNC"];Rectangle{Layout.fillWidth:true;implicitHeight:52;radius:9;color:"#08141a";border.color:root.line;Mono{anchors.verticalCenter:parent.verticalCenter;anchors.left:parent.left;anchors.leftMargin:12;text:modelData;color:root.ink}}}CButton{Layout.fillWidth:true;text:"＋ Nova demanda";accent:true}}
                }
                }
                Rectangle{Layout.fillWidth:true;implicitHeight:545;radius:18;color:"#09151b";border.color:root.line;ColumnLayout{anchors.fill:parent;anchors.margins:20;spacing:12;Mono{text:"FOCUS // HOJE"}Label{text:"Boa noite.";color:root.ink;font.pixelSize:26;font.bold:true}Label{text:"Estas são as coisas que merecem sua atenção agora.";color:root.muted;font.pixelSize:14;wrapMode:Text.WordWrap;Layout.fillWidth:true}Mono{text:"PRIORIDADE GERAL";Layout.alignment:Qt.AlignHCenter}Label{text:"02 críticas · 03 normais";color:root.ink;font.pixelSize:16}Repeater{model:[{t:"Cotação Hospital",s:"▲ PRIORIDADE ALTA",n:"01"},{t:"3DRN Store",s:"◉ EM ANDAMENTO",n:"02"},{t:"PsicoGestão",s:"○ AGUARDANDO",n:"03"},{t:"Organizar arquivos",s:"◇ SUGESTÃO DA IA",n:"04"}];Rectangle{Layout.fillWidth:true;implicitHeight:92;radius:11;color:index===0?"#102a2c":"#08141a";border.color:index===0?"#3b7744":root.line;ColumnLayout{anchors.fill:parent;anchors.margins:12;RowLayout{Layout.fillWidth:true;Label{text:modelData.t;color:root.ink;font.pixelSize:17;font.bold:true}Item{Layout.fillWidth:true}Mono{text:modelData.n}}Mono{text:modelData.s}}}}
                }
                }
                Rectangle{Layout.fillWidth:true;implicitHeight:420;radius:18;color:"#09151b";border.color:root.line;ColumnLayout{anchors.fill:parent;anchors.margins:20;spacing:12;Mono{text:"ACTIVE OBJECT"}RowLayout{Layout.fillWidth:true;Label{text:"Cotação Hospital";color:root.ink;font.pixelSize:22;font.bold:true}Item{Layout.fillWidth:true}Rectangle{width:58;height:36;radius:9;color:"#0a171d";border.color:root.line;Mono{anchors.centerIn:parent;text:"ATIVA"}}Rectangle{width:58;height:36;radius:9;color:"#0a171d";border.color:root.line;Mono{anchors.centerIn:parent;text:"ALTA"}}}Label{text:"Analisar se os materiais solicitados são\ncompatíveis antes de decidir participação.";color:root.muted;font.pixelSize:15;wrapMode:Text.WordWrap}Repeater{model:[{k:"PRAZO",v:"Hoje"},{k:"ANEXOS",v:"3 arquivos"},{k:"IA",v:"Contexto carregado"}];Rectangle{Layout.fillWidth:true;implicitHeight:78;radius:10;color:"#08141a";border.color:root.line;ColumnLayout{anchors.fill:parent;anchors.margins:12;Mono{text:modelData.k}Label{text:modelData.v;color:root.ink;font.pixelSize:14}}}}}
                Item{Layout.preferredHeight:12}
            }
        }
    }
}
