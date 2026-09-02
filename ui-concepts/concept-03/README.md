# Conceito 03 — Central de Comando

Este é o conceito visual escolhido como direção principal da assistente local.

## Estratégia de layout

O mesmo aplicativo terá dois estados responsivos, sem criar dois programas separados:

- **Expandido / desktop**: composição horizontal para janela maximizada e monitores de PC.
- **Compacto**: reorganização vertical do mesmo sistema para janelas menores.

A troca deve acontecer automaticamente conforme a largura disponível, preservando a identidade visual, o contexto da demanda e o estado da assistente.

## Identidade visual

- ficção científica / cyberpunk sofisticado, sem excesso de elementos decorativos;
- base muito escura, painéis translúcidos e bordas finas;
- ciano/verde-água usado como sinal de atividade e interação, não como preenchimento excessivo;
- pequenos indicadores técnicos e tipografia monoespaçada para estados do sistema;
- animações discretas: waveform de voz, núcleo da assistente, status e transições de painéis;
- sensação de uma central de comando pessoal, e não de um chatbot tradicional.

## Estrutura — modo expandido

### Coluna esquerda — Navegação e sistema

Contém navegação principal (`Hoje`, `Demandas`, `Memória`, `Arquivos`, `Rotinas`, `Configurações`), botão `Nova demanda` e indicadores locais como CPU, voz e banco de dados.

### Centro — Área de trabalho

É a maior região da interface. Mostra resumo do dia, cards de demandas, demanda atual, prazo, anexos, IA associada, descrição, insight da assistente e ações relacionadas à tarefa.

### Coluna direita — Presença da assistente

Mantém a assistente permanentemente visível: núcleo visual, estado (`disponível`, `ouvindo`, `falando`, `processando`), conversa corrente, estado da rede e memória recente.

### Barra inferior — Comando universal

Campo único para voz ou texto. Deve permanecer acessível independentemente da tela aberta.

## Estrutura — modo compacto

As mesmas áreas são empilhadas e reduzidas. A assistente continua tendo presença própria, porém navegação, tarefas e conversa passam a ocupar a largura total em sequência.

## Regra de internet

A rede permanece bloqueada por padrão. Quando uma informação externa for necessária, a assistente pede autorização de forma natural por voz, por exemplo:

> “Fulano, eu preciso acessar a internet para confirmar uma informação atual dessa demanda. Posso acessar?”

Um simples “pode” autoriza somente a ação atual por padrão. A interface deve apenas refletir discretamente o estado da rede, sem pop-ups burocráticos.

## Arquivos

- `compact/index.html` — protótipo da composição compacta.
- `expanded/index.html` — protótipo horizontal para desktop.

Estes HTMLs são referências visuais e de interação. A implementação final será feita no aplicativo Python, com a camada de interface escolhida para o projeto.