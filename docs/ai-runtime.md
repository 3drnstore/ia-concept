# NYRA — AI Runtime

## Hardware-alvo inicial

- CPU: AMD Ryzen 5 5600
- RAM: 32 GB
- GPU: AMD Radeon RX 7600
- VRAM esperada: 8 GB
- Sistema-alvo: Windows

## Estratégia do cérebro local

NYRA não será acoplada permanentemente a um único modelo. A aplicação terá uma camada de runtime que permita trocar o modelo sem reescrever o restante do sistema.

Fluxo previsto:

`NYRA UI/Orquestração -> Runtime local -> Modelo GGUF`

## Runtime inicial

A primeira opção será **llama.cpp**, por oferecer execução local, suporte a GGUF, quantização, execução híbrida CPU+GPU e backends para GPU AMD, incluindo Vulkan e HIP.

### Backend inicial no Windows

Começar com **Vulkan** para a Radeon RX 7600, por ser uma rota simples e amplamente suportada pelo llama.cpp no Windows. HIP/ROCm poderá ser testado posteriormente como alternativa de desempenho, sem mudar a arquitetura da NYRA.

## Modelo inicial

Primeiro candidato:

- **Qwen3 8B GGUF**
- Quantização inicial: **Q5_K_M**
- Alternativa mais leve: **Q4_K_M**

O Qwen3 8B Q5_K_M tem cerca de 5,85 GB apenas em pesos, portanto é um ponto de partida compatível com a RX 7600 de 8 GB, deixando margem limitada para cache/contexto. A configuração real de offload e contexto será ajustada por benchmark no computador-alvo.

## Estratégia de desempenho

1. Testar Qwen3 8B Q5_K_M com offload máximo possível para a GPU.
2. Medir tempo até o primeiro token, tokens por segundo e uso de VRAM/RAM.
3. Ajustar contexto e cache para preservar fluidez de conversa por voz.
4. Comparar Q4_K_M se o Q5_K_M deixar pouca margem de VRAM.
5. Testar modelos maiores apenas se a experiência continuar responsiva usando execução híbrida CPU+GPU.

## Regra de arquitetura

A identidade da NYRA, memória, voz, ferramentas, permissões e personalidade não ficam dentro do modelo. O modelo é um componente substituível.

Isso permite futuramente trocar Qwen3 por outro modelo sem perder a memória, o comportamento, as ferramentas ou a interface da NYRA.
