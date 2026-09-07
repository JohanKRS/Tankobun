# Integração MangaBaka

Implementação nova na branch `mangabaka`, partindo de `6cec832`, em 7 de setembro de 2026. Não reutiliza a implementação da outra branch. A versão pública permanece 4.2.1; nenhuma publicação foi realizada.

## Comportamento no aplicativo

- **Busca e listas de Explorar:** consultam AniList e MangaBaka; AniList vem primeiro, com complementos do MangaBaka. Correspondências oficiais compartilham a identidade local. Não há nova etapa de seleção de catálogo.
- **Seções iniciais de Explorar:** as quatro seções existentes priorizam AniList. Se ele estiver indisponível, o MangaBaka pode fornecer as listas equivalentes. Todas as linhas mostram até dez obras e oferecem Ver todos.
- **Para você:** segunda linha, logo abaixo de Em alta agora, com prévia de dez obras e Ver todos para abrir a seleção completa de até 36 sugestões do Mix. Usa até três obras em leitura, releitura ou concluídas da biblioteca, priorizando leituras atuais, notas e alterações recentes. Pode usar obras dos dois catálogos, desde que tenham correspondência MangaBaka confirmada. Exclui obras já na biblioteca e respeita o filtro de conteúdo adulto. Não exige conta MangaBaka; não aparece sem sementes ou resultados. O cache de 24 horas é verificado ao abrir Explorar; mudar as sementes ou a opção de conteúdo adulto usa outra chave. Atualizar na lista completa renova a consulta, sem apagar outros caches. Uma renovação pode retornar as mesmas sugestões.
- **Trending por gênero:** acrescenta Artes marciais, Histórico e Tragédia, classificados como gêneros no MangaBaka e consultados como tags no AniList. Mantém as duas consultas agrupadas, os campos compactos e o cache de quatro horas da Home. Quando a lista tem quantidade ímpar, o último cartão ocupa a largura da linha no tablet.
- **Recomendações nos detalhes:** preservam ordem, votos e imagens do AniList; completam a primeira página com similares e Mix do MangaBaka. Os resultados são deduplicados por identidade. A paginação seguinte pertence ao AniList; a quantidade combinada nunca é usada para adivinhar o número da página.
- **Fluxo de leitura:** os cartões abrem a mesma tela de detalhes, seleção de fontes e capítulos. A integração fornece catálogo e tracking; a leitura continua nas extensões.

Mix e similares retornam listas limitadas, sem paginação. O Mix calcula afinidade pelas tags das obras de referência; o app escolhe essas obras usando a biblioteca local, sem enviar notas, progresso ou conta para essa consulta. O serviço também documenta cache de um dia, portanto uma atualização manual não garante uma seleção diferente. O contrato atual envolve cada obra em `data[].series`; o valor de afinidade não é tratado como votos AniList. Referência: [OpenAPI oficial](https://mangabaka.org/api.json).

## Metadados e apresentação

AniList preenchido tem preferência. MangaBaka complementa títulos alternativos, sinopse, autor/artista, gêneros, tags, país/tipo, status, anos de publicação, capítulos e volumes. Banners são aceitos da galeria quando têm proporção adequada. A resposta completa dos provedores só é solicitada nos detalhes quando faltam campos; aproveita dados estruturados de Kitsu, MangaUpdates, Shikimori e AniList quando presentes.

Não usa estimativas de capítulos do Kitsu, texto de “último capítulo” ou contagens zero dos provedores como total confirmado. Popularidade MangaBaka é posição em ranking, portanto não é exibida como quantidade de leitores AniList. O modelo de datas do aplicativo continua exibindo os anos já previstos na interface.

Capas e banners existentes são preservados. Se o banner falhar, a apresentação recorre à capa na composição existente. Personagens permanecem do AniList ou da resposta bruta AniList disponível; retratos Shikimori que apresentaram bloqueio não foram incorporados. Não inventa autores, totais, datas ou personagens quando o catálogo também não os conhece.

## Identidade, persistência e backup

O identificador numérico histórico continua sendo a chave local, para manter vínculos, progresso e downloads. IDs externos AniList/MangaBaka são campos separados. Obras exclusivas MangaBaka recebem chave local negativa; nunca são enviadas ao GraphQL como um ID AniList. Um registro persistente de identidades sobrevive à limpeza de cache de navegação.

A migração Room 13 → 14 acrescenta esses campos, identidades, marcadores de paginação/cache e fila MangaBaka. Migrações anteriores continuam encadeadas. Uma correspondência encontrada depois pode absorver um registro que só existia no cache, preservando a chave da obra da biblioteca. Quando já existem dois registros independentes com dados do usuário, a implementação preserva ambos e não funde automaticamente progressos/fontes conflitantes. Reconciliação interativa desse caso continua fora desta entrega.

Backup completo manual e agendado usa JSON nativo versão 2 em ambos os modos de biblioteca, incluindo IDs externos, metadados, biblioteca, fonte vinculada e progresso. A restauração lê versões 1 e 2 sem exigir API ou login. XML antigo continua aceito; a exportação XML compatível é uma ação adicional e contém somente obras representáveis. Compartilhamento de recomendações também evoluiu para versão 2, mantendo leitura de arquivos antigos. Uma restauração com MangaBaka conectado registra as alterações na fila dessa conta, usando a escala de notas do arquivo, para evitar que um snapshot remoto antigo desfaça o progresso restaurado. Tokens não fazem parte dos backups. Páginas baixadas não passam a ser incluídas no JSON.

## Tracking opcional

Em Configurações → Catálogos e contas, é possível conectar MangaBaka com token pessoal com `library.read` e `library.write`, criado em [API & apps](https://mangabaka.org/my/settings/api-and-apps). O token fica no armazenamento seguro. OAuth com cliente próprio não foi cadastrado.

Ações locais posteriores à conexão geram operações de status, progresso, nota, observação e privacidade. Não copia toda a biblioteca ao conectar. Atualizações de progresso preservam os demais campos remotos não suportados. A fila é persistente, separada por conta, confirma a revisão efetivamente enviada e mantém falhas para nova tentativa. O envio é retomado por ações/entrada no app ou sincronização; não depende de manter o processo Android permanentemente ativo.

Sincronizar importa a biblioteca MangaBaka; entradas já existentes com vínculo AniList mantêm a prioridade local/AniList e operações locais pendentes não são sobrescritas. Ausência em uma resposta remota não remove títulos locais. A sincronização AniList não remove títulos exclusivos MangaBaka nem tenta enviá-los sem correspondência.

O contrato de gravação e o uso de autorização foram testados com servidor simulado. Login e sincronização em uma conta real ainda precisam ser validados com um token do usuário. Não foram feitos envios a contas externas durante o desenvolvimento.

## Cache e falhas

As chamadas públicas têm cache limitado a 80 respostas e 4 MiB de payload agregado, coalescência e espaçamento de requisições. Cada resposta tem limite de leitura de 8 MiB; árvores JSON têm sobrecarga adicional de memória. O cache de navegação guarda inclusive respostas vazias e a informação real de próxima página. Limpar dados de navegação ou todos os caches também limpa as respostas MangaBaka em memória; requisições iniciadas antes da limpeza não repovoam esse cache. As chamadas pessoais não compartilham o cache público. Falha ou lentidão do MangaBaka não derruba resultados AniList já obtidos. O acesso principal tem limite de espera e intervalo de recuperação para permitir fallback.

## Validação

- Testes unitários: 166 do app, 29 AniList, 15 MangaBaka, 2 banco e 16 rede, todos sem falhas. A rodada de refinamento executou novamente app, AniList e MangaBaka (210 testes).
- Builds e lint debug/release concluídos. APK release assinado instalado como atualização no Galaxy Tab S9 Ultra via ADB Wi-Fi, mantendo a instalação e os dados existentes. Nenhuma release foi publicada no GitHub.
- Emulador Pixel_9: atualização de banco versão 11 para 14 sem erro de integridade; busca combinada; abertura e inclusão de NoTR/Notorare sem vínculo AniList; abertura da seleção de fonte no fluxo existente.
- Exportação pela interface e restauração de JSON com título MangaBaka: identidade, progresso 7, capítulo/página e vínculo de fonte preservados na comparação SQLite. Arquivo legado também coberto por teste unitário.
- API real de Mix: 36 obras retornadas; seção Para você renderizada nos cartões existentes e abertura de sugestão nos detalhes. A obra aberta exibiu autor, anos 2019–20, 22 capítulos e 2 volumes. Foram persistidas 37 recomendações: 32 com vínculo AniList e 5 sem vínculo AniList.
- Testes específicos cobrem wrappers de Mix/similares, conteúdo adulto, deduplicação, prioridade visual, paginação combinada, coalescência, correspondência ambígua, metadados estruturados e corpo da gravação de tracking.
- Refinamento de Explorar: navegação da segunda linha para Ver tudo, abertura de detalhes, retorno à lista/Explorar e atualização manual conferidos no emulador. Cenários controlados de layout com 21/22 categorias verificam largura total da última linha ímpar e duas colunas com conteúdo adulto habilitado. Esses cenários não validam a classificação editorial dos títulos usados na fixture.
- Testes comprovam que as 22 categorias usam duas consultas agrupadas e que atualizar o Mix ignora seu cache sem remover outras respostas. Build e lint passaram nas variantes debug e release.

A validação do leitor com uma extensão real e dos dois trackers autenticados não foi realizada neste emulador, que não tinha extensões instaladas nem contas conectadas. A estrutura de fontes e progresso foi conferida por restauração, sem afirmar teste completo de leitura remota ou de sincronização entre contas reais.

## Créditos e referências

README, NOTICE, Sobre e rodapé do site incluem MangaBaka e provedores de origem. O Sobre tem links para a licença CC BY-NC-SA 4.0, termos dos dados, serviço e privacidade; o código MIT continua separado dos dados e imagens de terceiros. Os textos do app e do site estão em inglês, português, espanhol e chinês simplificado. Backups e recomendações com títulos MangaBaka incluem atribuição no JSON, mantendo leitura dos formatos anteriores. A validação dos exports verifica que licença, IDs e progresso continuam presentes.
