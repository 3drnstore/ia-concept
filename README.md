# IA Concept

Repositório de desenvolvimento da assistente pessoal local para Windows.

## Identidade

- **Nome:** Nyra
- **Designação técnica:** N.Y.R.A. — Neural Yielding Reasoning Assistant
- **Tipo:** Personal Local Intelligence

NYRA é o nome usado na interação cotidiana. A forma `N.Y.R.A.` e sua expansão aparecem como designação técnica no boot, nas configurações, na documentação e em áreas internas do sistema.

## Direção atual

O projeto segue uma arquitetura **local-first**, com Python como linguagem principal, IA local, memória local, interação por voz e acesso à internet somente quando necessário e autorizado pelo usuário de forma natural durante a conversa.

A direção visual escolhida para o aplicativo principal é o **Conceito 03 — Central de Comando**.

O Conceito 03 possui dois modos do mesmo aplicativo:

- **Expandido / desktop**: composição horizontal para monitor de PC e janela maximizada.
- **Compacto**: reorganização vertical para janelas menores.

A interface alterna entre esses estados conforme a largura disponível, sem perder contexto ou estado da assistente.

## Desenvolvimento iniciado — Nyra 0.1

A primeira versão funcional já começou a ser implementada em `src/nyra/`.

Base atual:

- Python 3.11+
- PySide6 + QML
- SQLite local
- layout expandido e compacto responsivo
- criação, seleção e conclusão de demandas
- painel visual da Nyra
- barra de comando por texto preparada para receber o núcleo local de IA

O banco local é criado em `%LOCALAPPDATA%/NYRA/nyra.sqlite3` no Windows.

### Executar no Windows durante o desenvolvimento

No PowerShell, dentro da pasta do projeto:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/setup_windows.ps1
powershell -ExecutionPolicy Bypass -File scripts/run_windows.ps1
```

O primeiro comando cria um ambiente Python isolado e instala as dependências. O segundo abre a Nyra.

Veja `docs/roadmap.md` para os próximos marcos.

## Arquivo de conceitos visuais

- `ui-concepts/concept-01/index.html` — primeira proposta: painel técnico/cyberpunk com foco em demandas e análise da IA.
- `ui-concepts/concept-02/index.html` — segunda proposta: composição inspirada no layout de referência enviado, com painel lateral e área de demanda detalhada.
- `ui-concepts/concept-03/README.md` — especificação visual oficial do conceito escolhido.
- `ui-concepts/concept-03/compact/index.html` — protótipo da versão compacta.
- `ui-concepts/concept-03/expanded/index.html` — protótipo horizontal para desktop.

Os arquivos HTML são protótipos visuais independentes e servem como referência de design e interação. A implementação real está sendo construída em Python/QML.

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
