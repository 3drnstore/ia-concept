# Roadmap da Nyra

## Marco 0.1 — Core Shell

Objetivo: ter um aplicativo Windows funcional antes de conectar o modelo de IA.

Estado atual:

- [x] Projeto Python criado
- [x] PySide6/QML definido como base da interface
- [x] Layout expandido para desktop
- [x] Layout compacto responsivo
- [x] Banco SQLite local para demandas
- [x] Criar demanda
- [x] Selecionar demanda
- [x] Concluir/reabrir demanda
- [x] Estado visual da Nyra simulado
- [x] Scripts simples de instalação e execução no Windows
- [ ] Testar a primeira execução no PC do usuário
- [ ] Ajustar a interface com base no teste real

## Marco 0.2 — Local AI Core

- integrar llama.cpp
- começar com Qwen3 8B GGUF
- testar Q5_K_M e Q4_K_M
- usar GPU AMD RX 7600 via Vulkan como primeira opção
- medir VRAM, RAM, latência inicial e tokens por segundo
- manter o modelo substituível

## Marco 0.3 — Voice Core

- reconhecimento de voz local
- síntese de voz local
- conversa contínua
- interrupção da fala da Nyra pelo usuário
- estados visuais: ouvindo, pensando, falando

## Marco 0.4 — Memory Core

- memória curta de conversa
- memória persistente
- recuperação de contexto relevante
- preferências e fatos pessoais armazenados localmente
- modelo interno de identidade e capacidades da Nyra

## Marco 0.5 — Tools & Permissions

- arquivos locais
- ações no Windows
- GitHub
- acesso à internet por ferramenta intermediária
- autorização natural por voz antes de uso externo
- confirmação reforçada para ações destrutivas

## Futuro — Android Companion

O Android será um companheiro da Nyra do PC. A IA principal, memória e ferramentas permanecem no Windows; o celular oferece voz, câmera, notificações e acesso remoto à mesma assistente.
