# IA Concept

Repositório de desenvolvimento da assistente pessoal local para Windows.

## Direção atual

O projeto seguirá uma arquitetura **local-first**, com Python como linguagem principal, IA local, memória local, interação por voz e acesso à internet somente quando necessário e autorizado pelo usuário de forma natural durante a conversa.

A direção visual escolhida para o aplicativo principal é o **Conceito 03 — Central de Comando**. Os dois conceitos anteriores foram preservados neste repositório para possível reaproveitamento em outros projetos.

O Conceito 03 terá dois modos do mesmo aplicativo:

- **Expandido / desktop**: composição horizontal para monitor de PC e janela maximizada.
- **Compacto**: reorganização vertical para janelas menores.

A interface deverá alternar entre esses estados conforme a largura disponível, sem perder contexto, conversa ou estado da assistente.

## Arquivo de conceitos visuais

- `ui-concepts/concept-01/index.html` — primeira proposta: painel técnico/cyberpunk com foco em demandas e análise da IA.
- `ui-concepts/concept-02/index.html` — segunda proposta: composição inspirada no layout de referência enviado, com painel lateral e área de demanda detalhada.
- `ui-concepts/concept-03/README.md` — especificação visual oficial do conceito escolhido.
- `ui-concepts/concept-03/compact/index.html` — protótipo da versão compacta.
- `ui-concepts/concept-03/expanded/index.html` — protótipo horizontal para desktop.

Os arquivos HTML são protótipos visuais independentes e servem como referência de design e interação. A implementação final será integrada ao aplicativo Python.

## Princípios já definidos

- Windows desktop
- Python
- interface sci-fi/cyberpunk
- IA local e offline por padrão
- memória persistente local
- entrada e saída por voz
- acesso à internet bloqueado por padrão
- quando precisar de internet, a assistente explica por voz por que precisa e pede autorização de forma natural
- autorização simples vale, por padrão, apenas para a ação solicitada
- acesso a ferramentas externas, como GitHub, pode ser amplo, mantendo confirmação para ações destrutivas ou irreversíveis
