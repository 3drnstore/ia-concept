# Retomada da Nyra — base Windows #33

Base aprovada: commit 36e1b8339645819996b51d2118fcee34e6efaa2d.
O usuário recuperou o último pedido da conversa do outro computador em 04/09/2026.
A interface dessa build fica aprovada como base; não retomar as observações antigas
de lua, traços brancos e cantos como requisitos novos.

## Implementado nesta continuação

- Microfone fixo nos modos desktop e compacto; dois anéis propagam de dentro para
  fora somente enquanto há sinal captado. Volume controla amplitude e opacidade.
- Decodificação UInt8, Int16, Int32 e Float, limiar de silêncio, decaimento e reset
  ao parar ou deixar de receber amostras.
- Prioridade de Nova demanda e Editar com fundo azul e destaque claro próprio.
- Comandos digitados conectados ao AiCore/llama.cpp; histórico de até 10 trocas na
  sessão, resposta legível em painel de conversa e erros explícitos de conexão.
- Piper como primeira voz local: síntese em processo separado e reprodução Qt.
  Estado FALANDO acompanha reprodução, sem temporizador de fala fictício.
- Configurações persistidas para voz, permissão de microfone, seleção de webcam
  e preferência de uso futuro da câmera; seção Companion com estado não conectado.

## Limites e próximos passos

- O microfone é um medidor nesta etapa. Falta integrar reconhecimento/transcrição
  de fala; Piper faz texto para voz, não voz para texto.
- Webcam: enumeração e preferência disponíveis; nenhuma captura ou análise visual.
- Companion: pareamento, aplicativo móvel e permissões por dispositivo pendentes.
- O modelo GGUF/llama.cpp deve estar rodando no PC. Não é incluído no executável.
- Piper e sua voz são instalados separadamente por setup-voice.ps1; após a
  instalação inicial, a síntese é offline. A conversa não persiste entre sessões.
- Não afirmar que arquivos ou conversas de outro PC sincronizaram sem verificar.
