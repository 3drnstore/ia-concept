# Voz e IA local

1. Instale Python 3.11 ou 3.12, com Python disponível no PATH.
2. Execute `powershell -File .\setup-voice.ps1` na pasta do pacote Windows.
   A instalação inicial requer internet e baixa Piper 1.8.0 e a voz pt_BR-faber-medium.
3. Reinicie Nyra. Em Configurações, use **Testar voz**. Os caminhos padrão são
   detectados em `%LOCALAPPDATA%\NYRA\voice`. Uma instalação própria também pode
   ser configurada com o executável `piper.exe`, modelo `.onnx` e `.onnx.json`.
4. Inicie seu servidor llama.cpp em `http://127.0.0.1:8080`, conforme ai-runtime.md.
   A Nyra envia comandos digitados a `/v1/chat/completions`. O nome do modelo
   pode ser definido em NYRA_MODEL antes de abrir o aplicativo.
5. Digite na barra inferior. Clique na resposta ou em **⋮** para ler o texto
   completo. No modo compacto, use **Conversa com Nyra**.

Desative **Ler respostas em voz alta** para usar somente texto. **Parar** interrompe
síntese ou reprodução. Desativar permissão de microfone encerra a captura.
O microfone ainda não transcreve fala. Webcam e Companion não capturam nem
transmitem dados nesta versão.

O runtime Piper não é embutido no executável. Documentação e licença do projeto:
https://github.com/OHF-Voice/piper1-gpl (GPL-3.0).
Consulte também a licença/model card da voz escolhida antes de redistribuí-la.
