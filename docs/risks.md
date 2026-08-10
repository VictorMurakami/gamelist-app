# Riscos Registrados

Riscos conhecidos que não justificam trabalho agora. Cada um tem o gatilho de
escala em que passa a justificar.

Achados críticos não entram aqui — são corrigidos antes do merge da fase e
ficam documentados no relatório da fase, em `docs/reviews/`.

---

## `BackendConfig.baseUrl` é `http://` e aponta para um host de desenvolvimento

- **Fase de origem:** 2
- **Descrição:** as três plataformas usam cleartext e endereço de dev —
  `http://10.0.2.2:8000` (`BackendConfig.android.kt:6`), `http://localhost:8000`
  (`BackendConfig.ios.kt:5` e `BackendConfig.jvm.kt:4`). Não há build type,
  flavor ou `buildConfigField` que troque esse valor no release: o literal é o
  mesmo em debug e em release. Duas consequências distintas. (a) **Funcional:**
  um APK publicado hoje tentaria falar com `10.0.2.2`, que não existe fora do
  NAT do emulador; o fetch falha, `load()` cai para `AppConfigState.EMPTY`
  (`AppConfigRepository.kt:63`) e o gate inteiro — force update e manutenção —
  fica permanentemente inerte, sem nenhum sinal. É exatamente o cenário que a
  seção 1 do design quer evitar ("um app publicado sem ele não tem como ser
  bloqueado depois"). (b) **Segurança:** no dia em que o literal virar um host
  real ainda em `http://`, um atacante na mesma rede injeta
  `{"update":{"status":"forced","store_url":"http://evil/app.apk"}}` e a
  `ForceUpdateScreen`, cuja única ação possível é o botão de atualizar
  (`ForceUpdateScreen.kt:123-138`), vira um canal de entrega de APK falso.
  O `network_security_config.xml` **não** é o problema: ele isenta apenas
  `10.0.2.2` e `localhost` (endereços de loopback/NAT que nenhum atacante
  alcança), não tem `base-config` e não usa `usesCleartextTraffic`, então
  qualquer host real continua exigindo TLS mesmo com o arquivo em
  `androidMain/res` — ou seja, ele *já* falha fechado. A avaliação da Task 2 se
  sustenta.
- **Gatilho:** o primeiro build de release / a primeira publicação em loja.
  Não é gradual: passa de irrelevante a bloqueante de uma vez.
- **Mitigação:** `buildConfigField("String", "BACKEND_BASE_URL", ...)` por build
  type no Android (release em `https://`), `xcconfig` equivalente no iOS, e o
  `network_security_config.xml` movido para um source set só de debug — assim um
  release que ainda aponte para `http://` falha alto em vez de trafegar em
  claro. Vale também um check no CI que rejeite `http://` em qualquer
  `BackendConfig.*.kt` fora de debug.

## `runCatching` em `load()` captura `CancellationException`

- **Fase de origem:** 2
- **Descrição:** `AppConfigRepository.kt:48` usa `runCatching`, que captura
  `Throwable` — inclusive `CancellationException`. **Verificado que hoje isso
  não quebra o cancelamento estruturado:** um teste que cancela o job chamador
  100 ms depois do início do fetch mostrou `load()` *não* retornando
  normalmente e `job.isCancelled == true`. A razão é indireta e frágil: o
  `withTimeoutOrNull` que envolve o `runCatching` (`AppConfigRepository.kt:47`)
  cria um `TimeoutCoroutine` filho do job do chamador; quando esse filho é
  completado com um valor enquanto já está em estado cancelado, o
  `JobSupport` descarta o valor proposto e reergue o cancelamento. Ou seja,
  a propriedade não vem do `runCatching` estar correto, vem de um efeito
  colateral do escopo que o envolve.
- **Gatilho:** qualquer refactor que tire o `withTimeoutOrNull`, mova o
  `runCatching` para fora dele, ou acrescente um segundo `runCatching`
  englobando o corpo inteiro de `load()`. A partir daí `load()` passa a
  retornar `EMPTY` em vez de propagar o cancelamento, e um `LaunchedEffect`
  que saiu de composição continuaria escrevendo em `textPrefs` e resolvendo
  estado que ninguém mais consome. A Fase 3 mexe justamente neste caminho.
- **Mitigação:** rethrow explícito —
  `runCatching { … }.onFailure { if (it is CancellationException) throw it }` ou
  um `catch (e: CancellationException) { throw e }` antes do `catch (e: Throwable)`.
  Trocar por `catch (e: Exception)` **não** resolve: no JVM
  `CancellationException` herda de `IllegalStateException`, portanto de
  `Exception`, e continuaria sendo engolida. Um teste de
  regressão que cancele o chamador e afirme `job.isCancelled` trava a
  propriedade independentemente de quem a garante.

## `else -> UpdateStatus.NONE` é sensível a caixa e espaço, e não loga

- **Fase de origem:** 2
- **Descrição:** `AppConfigRepository.kt:88-92` faz `when (this)` sobre a string
  crua. `"Forced"`, `" forced"` ou `"FORCED"` caem no `else` e viram `NONE` —
  indistinguível de "não há atualização" em qualquer log ou telemetria. Hoje o
  contrato do servidor fecha esse buraco dos dois lados: `resolver.py:13-16`
  emite apenas as constantes minúsculas e
  `serializers.py:20` valida a resposta com
  `ChoiceField(choices=["none","recommended","forced"])`. O `else` é fail-safe
  por escolha (status novo no servidor não pode bloquear app antigo — coberto
  por `unknownStatusFallsBackToNone`), e isso deve continuar assim.
- **Gatilho:** quando o endpoint deixar de ser servido só pelo Django desta
  base — um proxy/edge que reescreva o corpo, um mock de staging, um segundo
  serviço reimplementando o contrato — ou quando existir telemetria capaz de
  registrar a degradação. Enquanto o `ChoiceField` for o único produtor, o caso
  é inalcançável.
- **Mitigação:** `when (this.trim().lowercase())` mais um log de warning no
  `else` nomeando o valor recebido. Barato; fica fora agora porque não há
  infraestrutura de log no app (ver risco "Sem logging no cliente") e
  normalizar sem logar só esconde melhor o mesmo problema.

## `device_id` é um identificador estável de instalação enviado a cada abertura

- **Fase de origem:** 2
- **Descrição:** `DeviceIdProvider.kt:30-38` gera 16 bytes aleatórios na
  primeira execução e persiste em `TextPrefEntity`; `AppConfigApi.kt:19` o envia
  como query param em toda abertura do app. Não é identificador de hardware e
  não é compartilhado entre apps, então não é um identificador *de usuário* nem
  reidentificável entre reinstalações. Mas é, por construção, um identificador
  persistente do dispositivo do ponto de vista de política de loja: correlaciona
  todas as aberturas de uma instalação, e o servidor as recebe junto do IP. O
  propósito é legítimo e declarável (bucket de rollout — `rollout.py`), não
  publicidade. Dois pontos que ampliam o alcance: (a) o `Logging` do Ktor em
  `LogLevel.HEADERS` (`HttpClientFactory.kt:71-73`) escreve a URL completa,
  com o `device_id`, no logcat/NSLog **também em release**; (b)
  `android:allowBackup="true"` (`AndroidManifest.xml:8`, pré-existente) faz o
  banco — e portanto o `device_id` — subir para o Auto Backup do Google.
- **Gatilho:** a primeira submissão a uma loja (App Store Privacy Nutrition
  Label e Data Safety do Google Play exigem declarar "Device or other IDs"), ou
  o momento em que o `device_id` for correlacionado a uma conta — que é
  exatamente o que a Fase 3 faz ao usá-lo no fluxo de auth. Antes disso é um
  número aleatório sem sujeito.
- **Mitigação:** declarar o identificador nas duas lojas com a finalidade real
  (controle de versão e rollout, não tracking); reduzir o `LogLevel` para
  `NONE` em release; e reavaliar `allowBackup` na Fase 3, quando o banco passar
  a conviver com dados de conta.

## Cache do app-config é JSON em claro no SQLite, editável em device com root

- **Fase de origem:** 2
- **Descrição:** `AppConfigRepository.kt:59` grava a resposta como texto em
  `TextPrefEntity`. Num device com root/jailbreak dá para editar a linha
  `app_config_cache` e forçar `maintenance.active = false` ou rebaixar um
  `forced`. O alcance real é pequeno por três motivos: o cache só é consultado
  quando a rede falha (`AppConfigRepository.kt:63` — o fetch sempre é tentado
  primeiro), o atacante é o dono do device atacando a si mesmo, e quem tem root
  tem caminhos muito mais diretos (patch do APK, hook no `toUpdateStatus`,
  `version` forjado no query param — este último já registrado no `risks.md` do
  backend como "O cliente declara a própria versão"). O gate é cooperativo por
  construção. Quanto ao conteúdo: o bloco `auth` guardado no cache
  (`AppConfigDto.kt:29-32`) traz `issuer` e `client_id`, que são identificadores
  públicos de um client OIDC público com PKCE — não há segredo ali.
- **Gatilho:** quando o cache passar a guardar algo que valha proteger. Na
  Fase 3 isso é concreto: token de acesso/refresh **não** pode ir para esta
  tabela (o plano já manda Keystore/Keychain) — o gatilho é qualquer PR que
  encoste dado de sessão em `TextPrefDataSource`.
- **Mitigação:** nenhuma no cliente para o caso de root (é premissa perdida).
  O que fecha de verdade é do lado do servidor: decisão por endpoint autenticado
  e atestação de app (Play Integrity / DeviceCheck) na Fase 3. Para o dado de
  sessão, `EncryptedSharedPreferences`/Keystore e Keychain, nunca esta tabela.

## Manutenção cacheada bloqueia o app offline depois da janela terminar

- **Fase de origem:** 2
- **Descrição:** o envelope do cache agora é descartado quando a versão do app
  muda (achado C1 desta fase), mas não tem validade no tempo. Se o último fetch
  bem-sucedido trouxe `maintenance.active = true` e o usuário passa a abrir o
  app sem rede, `cachedState()` devolve manutenção ativa em toda abertura e a
  `MaintenanceScreen` — que não tem botão de tentar de novo
  (`MaintenanceScreen.kt`) — bloqueia um app que antes da Fase 2 funcionava
  offline a partir do banco local. Sai do estado na primeira abertura com rede.
  A inversão importa: o app diz "estamos em manutenção" quando o fato é "você
  está sem rede".
- **Gatilho:** a primeira janela de manutenção real em produção, ou a primeira
  vez que a janela durar mais que alguns minutos. Com base instalada relevante
  a cauda de usuários que pegaram `active=true` e ficaram offline deixa de ser
  hipotética.
- **Mitigação:** carimbar o instante do fetch no envelope do cache (já existe o
  `CachedConfig`) e ignorar o bloco `maintenance` cacheado depois de um TTL
  curto — algo como 1 h —, preservando `update.status`, que não expira. Um
  botão de "tentar de novo" na `MaintenanceScreen` resolve o sintoma sem
  resolver a causa, mas é mais barato.

## Trabalho de disco de `load()` roda na thread principal

- **Fase de origem:** 2
- **Descrição:** `App.kt:111-117` chama `appConfigRepository.load(lang)` de um
  `LaunchedEffect`, cujo contexto no Android é o `AndroidUiDispatcher.Main`.
  `load()` não troca de dispatcher — a Task 7 removeu de propósito um
  `withContext(Dispatchers.Default)` que existia só para acomodar teste. Rodam
  portanto na main thread, durante o splash: `deviceIdProvider.deviceId()`
  (`AppConfigRepository.kt:52` → leitura e possivelmente escrita no SQLite),
  `textPrefs.set` do JSON completo da config (`:59` → escrita) e `cachedState()`
  (`:67` → leitura mais decode do JSON). O `koinInject<AppConfigRepository>()`
  (`App.kt:66`) ainda pode disparar a abertura do banco e a migração dentro da
  composição. Medido: a migração `1.sqm` custou **1,23 ms** num banco com 5.000
  jogos e 20.000 linhas de `CacheMetaEntity` (`CREATE TABLE` não toca as tabelas
  existentes; o seed dos mesmos dados levou 142 ms, duas ordens de grandeza a
  mais). Nada disso é visível hoje, e nenhuma dessas operações lê mais de uma
  linha.
- **Gatilho:** quando `TextPrefEntity` deixar de ser "duas chaves pequenas" —
  a Fase 3 põe dado de sessão perto daqui — ou quando o volume gravado por
  `set()` crescer (catálogo de flags grande deixa o JSON da config maior; ver
  o risco de payload por flag no `risks.md` do backend). Também vira visível em
  device de gama baixa com armazenamento cheio, onde um `fsync` custa dezenas de
  ms. Sinal de alerta prático: StrictMode acusando disk I/O na main thread.
- **Mitigação:** `withContext` para um dispatcher de I/O em volta das partes
  não-suspensas de `load()`, mantendo o `withTimeoutOrNull` só em torno do
  fetch. `Dispatchers.IO` não existe em `commonMain`, então isso custa um
  `expect/actual` ou um `CoroutineDispatcher` injetado — motivo de não ser
  feito agora por um custo que hoje é de microssegundos.

## Os dois `HttpClient` nunca são fechados

- **Fase de origem:** 2
- **Descrição:** `NetworkModule.kt:10` e `:13` registram os dois clientes como
  `single` do Koin, e nenhum caminho chama `close()` nem `stopKoin()`. Ambos
  vivem enquanto o processo viver. O segundo cliente (`createBackend`) é usado
  uma vez por abertura e fica ocioso o resto do tempo, carregando um engine
  OkHttp/Darwin próprio com seu pool de conexões e dispatcher. Não é vazamento
  crescente — é um custo fixo duplicado — e no Android/iOS o processo morre
  levando tudo junto, então não há acúmulo entre execuções.
- **Gatilho:** um terceiro e um quarto cliente (a Fase 3 pode querer um cliente
  com interceptor de token), ou pressão de memória que faça o SO matar o app em
  background com mais frequência.
- **Mitigação:** um único `HttpClient` com `defaultRequest` por host, ou
  configuração de engine compartilhada entre os dois; e `close()` amarrado ao
  ciclo de vida do Koin se um dia houver `stopKoin()`.

## Resposta do app-config não tem limite de tamanho nem o `changelog` tem teto

- **Fase de origem:** 2
- **Descrição:** `AppConfigApi.kt:21` faz `.body()` sem limite de bytes, e o
  `changelog` (`AppConfigDto.kt:19`) vem de um `TextField` sem `max_length` no
  backend (`models.py:37`), sendo renderizado inteiro num único `Text`
  (`ForceUpdateScreen.kt:111-115`). Um backend comprometido — ou um MITM,
  enquanto o transporte for `http://` — pode devolver megabytes de texto. O
  `withTimeoutOrNull(2_500)` limita a *janela* de download, não o volume: em
  rede rápida cabem dezenas de MB nesse intervalo. Duas checagens deram
  resultado tranquilizador: JSON com 50.000 níveis de aninhamento num campo
  desconhecido foi pulado pelo `ignoreUnknownKeys` sem `StackOverflowError`
  (a config foi decodificada corretamente, `status=FORCED`), e um `OutOfMemory`
  ou `StackOverflow` seria capturado pelo `runCatching` de `load()`, que pega
  `Throwable`, caindo para o cache.
- **Gatilho:** o transporte deixar de ser confiável (ver o primeiro risco desta
  lista) ou o `changelog` passar a ser editado por mais gente que o time —
  qualquer um que possa escrever no Admin passa a poder travar a UI de todos os
  clientes bloqueados.
- **Mitigação:** `max_length` no `changelog` do backend, `maxLines` mais
  `overflow = TextOverflow.Ellipsis` na `ForceUpdateScreen`, e um teto de bytes
  no cliente lendo o corpo como `ByteArray` limitado antes de desserializar.

## `store_url` é aberto sem validar o esquema

- **Fase de origem:** 2
- **Descrição:** `App.kt:183` e `:207` passam `config.update.storeUrl` direto
  para `urlOpener.open`, que no Android faz
  `Intent(ACTION_VIEW, Uri.parse(url))` (`UrlOpener.kt:9`) e no iOS
  `UIApplication.openURL` (`UrlOpener.kt:9`). Nenhum dos dois checa o esquema.
  O único guard existente é `!storeUrl.isNullOrBlank()`
  (`ForceUpdateScreen.kt:123`). No backend o campo é `URLField`
  (`models.py:36`), cujo validador aceita `http`, `https`, `ftp` e `ftps` — e
  só roda no `full_clean()` do Admin; `bulk_update`/shell/SQL direto passam por
  fora, e o serializer de resposta é um `CharField` sem validação
  (`serializers.py:22`). Ou seja: um valor como `intent://…` ou um esquema
  custom chegaria intacto ao `openURL`. O impacto prático é limitado — o ponto
  de partida é já confiar no backend para tudo o mais no gate — mas o clique
  acontece na única tela da qual o usuário não consegue sair, o que maximiza a
  taxa de acerto de qualquer redirecionamento hostil.
- **Gatilho:** o primeiro caminho de escrita em `AppVersion` que não passe pelo
  Admin (importador de release, webhook, seed de deploy — já previsto no
  `risks.md` do backend), ou o transporte em claro chegar a produção.
- **Mitigação:** allowlist de esquema no `UrlOpener` de cada plataforma
  (`https`, mais `market://` no Android e `itms-apps://` no iOS), descartando o
  resto; e `URLValidator(schemes=["https"])` no campo do backend.

## Sem logging no cliente: toda degradação do gate é silenciosa

- **Fase de origem:** 2
- **Descrição:** não há abstração de log em `commonMain`. Todo caminho de falha
  do gate termina em `AppConfigState.EMPTY`, que é indistinguível de "o servidor
  disse que está tudo bem": fetch que falha (`AppConfigRepository.kt:55`), cache
  corrompido (`:68`), status desconhecido (`:91`), `startActivity` que não
  encontra app para o intent (`UrlOpener.kt:12`, `runCatching` descartado).
  Somam-se a isso duas degradações silenciosas de fora do app: o backend
  responde `status: none` quando não consegue parsear a `version` — verificado
  contra o stack, `?platform=android&version=v1.0.0` devolveu 200 com
  `"status":"none"` — e responde 400 quando a `version` passa de 20 caracteres
  (`serializers.py:12`), que no cliente também vira `EMPTY`. Um `versionName`
  publicado fora do formato semver desliga o force update sem que nada acuse.
- **Gatilho:** o primeiro incidente em que a pergunta for "quantos usuários
  deveriam ter visto a tela de bloqueio e não viram?". Na prática: a primeira
  vez que o force update precisar ser usado a sério.
- **Mitigação:** um `Logger` `expect/actual` fino (logcat/NSLog) com pontos nos
  caminhos de falha acima, e um crash/analytics reporter na Fase 3. No build,
  uma validação de que `versionName` casa com `^\d+\.\d+\.\d+$` (e que o
  `CFBundleShortVersionString` do `iosApp/Info.plist` — hoje `"1.0"`, contra
  `versionName = "1.0.0"` do Gradle — vem da mesma fonte) fecha a metade que é
  responsabilidade do cliente.

## Um `versionName` de pre-release trava o app numa tela sem saida (fail-closed)

- **Fase de origem:** 2
- **Descrição:** o risco acima ("Sem logging no cliente") registra a direção
  fail-*open*: `versionName` fora do formato semver desliga o force update em
  silêncio. Existe a direção oposta, mais grave, e ela não estava registrada.
  `AppInfo.android.kt:7` envia `BuildConfig.VERSION_NAME` verbatim; o backend
  normaliza essa string com PEP 440 antes de comparar com `min_supported`.
  Verificado contra o stack, com `min_supported = latest_version = 1.0.0` para
  `android`:
  ```
  version=1.0.0-beta  -> status=forced
  version=1.0.0-rc1   -> status=forced
  version=1.0.0_rc    -> status=forced
  version=1.0.0       -> status=none
  ```
  PEP 440 normaliza `1.0.0-beta` para `1.0.0b0`, que fica **abaixo** de
  `1.0.0` — o backend responde `forced` legitimamente, não é um bug do
  servidor. O cliente renderiza a `ForceUpdateScreen`, que não tem botão de
  voltar por construção, e cujo único botão abre uma loja onde esse build não
  existe. Qualquer tester interno rodando um `versionName` de pre-release fica
  brickado no primeiro launch, sem escape dentro do app.
- **Gatilho:** o primeiro build de beta/RC interno com um `versionName`
  contendo sufixo de pre-release — não precisa de release pública, um único
  APK de teste interno já reproduz.
- **Mitigação:** implementada nesta wave — um `require()` em
  `composeApp/build.gradle.kts` que falha o build quando `versionName` não
  casa com `^\d+\.\d+\.\d+$`. Esse único check fecha as duas direções ao mesmo
  tempo: a fail-open (formato que o PEP 440 não reconhece cai em `none`
  silencioso) e a fail-closed descrita aqui (sufixo de pre-release que o PEP
  440 reconhece normaliza para abaixo do mínimo e tranca o app). Verificado
  manualmente: com `versionName = "1.0.0-beta"` o build falha citando este
  motivo; restaurado para `"1.0.0"` o build volta a passar.

## Caminho iOS do app-config nunca foi verificado em runtime

- **Fase de origem:** 2
- **Descrição:** toda a verificação manual desta fase foi feita no emulador
  Android (ver o ledger da Task 7). No iOS, `BackendConfig.ios.kt:5` aponta para
  `http://localhost:8000` e `iosApp/iosApp/Info.plist` não tem chave
  `NSAppTransportSecurity` — nem `NSAllowsLocalNetworking`, nem exceção de
  domínio para `localhost`. Se a ATS bloquear esse carregamento, o fetch falha,
  `load()` devolve `EMPTY` e o gate está morto no iOS **sem nenhum sintoma
  visível**: o app abre normalmente, que é exatamente o comportamento esperado
  quando não há atualização pendente. Não foi possível determinar com confiança
  se a ATS isenta `localhost` — a documentação e os relatos divergem —, e o
  simulador não foi executado nesta revisão. Registrado como incerteza, não
  como conclusão.
- **Gatilho:** antes da Fase 3, que constrói o login em cima do mesmo cliente e
  do mesmo `issuer`/`client_id` vindos deste endpoint. Se o app-config não chega
  no iOS, o login também não sobe.
- **Mitigação:** rodar o app no simulador com o backend local e confirmar, com
  o `LogLevel` do Ktor, que a resposta chega; se a ATS bloquear, adicionar
  `NSAllowsLocalNetworking` apenas na configuração de debug. Em release o ponto
  desaparece junto com o primeiro risco desta lista (HTTPS).
