# Nuwe Mapa

Nuwe Mapa é uma compilação pessoal de navegação offline para Android baseada no código aberto do OsmAnd.

## Objetivo desta versão

- navegação para carro, bicicleta e trilhas/caminhada;
- mapas vetoriais offline compatíveis com OBF, derivados de dados OpenStreetMap;
- busca, cálculo e recálculo de rotas offline;
- importação e gravação de GPX;
- orientação por voz usando TTS do Android;
- identidade própria **Nuwe Mapa**, com paleta ciano e inspiração Fruit Aero;
- sem anúncios, compras internas, assinatura, OsmAnd Cloud ou Android Auto;
- sem permissão `android.permission.INTERNET` no aplicativo.

O Android ainda pode usar seus próprios serviços de localização e dados de assistência GNSS para acelerar a obtenção da posição quando o sistema tiver conectividade. Isso não concede acesso de rede ao Nuwe Mapa.

## Base e licença

O código-fonte Android usado no build é obtido diretamente de `osmandapp/OsmAnd` e dos repositórios auxiliares oficiais. O código e as modificações permanecem sujeitos à GPL e às licenças aplicáveis aos componentes upstream. Os avisos de copyright/licença do OsmAnd não são removidos.

## Build

O workflow `Build Nuwe Mapa APK` faz checkout das fontes oficiais, aplica `patch_osmand.py` e compila a variante Android Full/OpenGL para `arm64-v8a` em modo debug, assinada pela chave de desenvolvimento do build.

O APK resultante é para instalação manual e uso pessoal, não para publicação em loja.
