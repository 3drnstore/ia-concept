# Aven Browser

Aven é um navegador Android baseado em GeckoView, com interface minimalista própria, modo claro/escuro/sistema e foco em privacidade.

## Privacidade padrão
- uBlock Origin integrado no APK
- DNS over HTTPS Cloudflare (`https://cloudflare-dns.com/dns-query`) em modo TRR-only
- Enhanced Tracking Protection em modo estrito
- Fingerprinting protection ativada
- Global Privacy Control ativado
- Cookies com isolamento de terceiros
- Query parameter stripping
- HTTPS-only
- Web content isolation
- Telemetria própria: nenhuma

## Build
O workflow `.github/workflows/aven-browser-build.yml` baixa o uBlock Origin oficial 1.72.2, incorpora-o em `assets/ublock`, compila o APK e publica o artefato `Aven-APK`.
