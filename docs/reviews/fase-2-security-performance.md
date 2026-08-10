# Segurança & Performance — Fase 2 (`gamelist-app`)

**Data:** 2026-08-09
**Branch:** `feat/fase-2-app-config`
**HEAD revisado:** `64003f4`
**Escopo:** consumo do `GET /api/v1/app-config/` — store de texto e migração,
`device_id`, identidade de app e base URL por plataforma, DTOs, segundo
`HttpClient`, `AppConfigRepository`, telas de gate e o gate no `App.kt`.

Mandato da seção 3.3 do design, adaptado ao cliente móvel. Oito tasks e quinze
rodadas de correção antecederam esta revisão; achados já resolvidos por elas não
são repetidos aqui.

---

## Críticos (bloqueiam merge)

- [x] `core/config/AppConfigRepository.kt:66-69` — **o cache do app-config não
  era escopado à versão do app, e um `forced` obsoleto trancava o usuário
  depois que ele atualizava.** A resposta do backend é função da versão que
  perguntou (`resolver.py:33-59` compara `request.version` com `min_supported`),
  mas `cachedState()` reaplicava a resposta guardada sem checar para qual versão
  ela tinha sido obtida.

  *Cenário de falha, reproduzido:* app em 1.0.0, backend responde
  `status: forced`, o cliente cacheia (`:59`). O usuário faz exatamente o que a
  `ForceUpdateScreen` mandou e atualiza para 2.0.0. Na primeira abertura depois
  disso ele está sem rede — metrô, avião, roaming, Wi-Fi de portal cativo. O
  fetch falha, `load()` cai para o cache (`:63`) e devolve `FORCED` de novo. O
  usuário fica preso numa tela sem saída — a tela não tem botão de voltar por
  construção (`ForceUpdateScreen.kt:33-39`) — cujo único botão o manda para uma
  loja onde não há nada para baixar. Só sai disso quando conseguir rede numa
  abertura. É a inversão exata da propriedade que a tela existe para ter: em vez
  de bloquear quem está desatualizado, bloqueia quem acabou de se atualizar.
  Reproduzido em teste antes da correção:
  `### CACHE: status apos upgrade offline = FORCED`.

  *Correção (commit `02c664f`):* o cache passou a ser um envelope
  `CachedConfig { app_version, config }`; `cachedState()` descarta a entrada
  quando `cached.appVersion != appInfo.version`, devolvendo `null` — o mesmo
  caminho de "instalação nova", que resolve para `AppConfigState.EMPTY` e deixa
  o app abrir. Cache no formato antigo (sem envelope) falha a desserialização e
  cai no mesmo `null` pelo `runCatching` já existente, sem exceção. Dois testes
  novos: `discardsCacheFromAnotherAppVersion` (o caso acima, agora `NONE`) e
  `keepsCacheForTheSameAppVersion` (contraprova — o fallback offline continua
  servindo `FORCED` para a mesma versão, senão o "corrigir" teria sido
  desligar o cache).

---

## Riscos futuros (não bloqueiam, registrados em `docs/risks.md`)

Doze riscos registrados, cada um com gatilho de escala. Resumo:

| Risco | Gatilho |
|---|---|
| `baseUrl` em `http://` e apontando para host de dev nas 3 plataformas | primeiro build de release |
| `runCatching` captura `CancellationException` (hoje neutralizado pelo `withTimeoutOrNull`) | refactor que tire ou mova o `withTimeoutOrNull` |
| `else -> NONE` sensível a caixa/espaço e sem log | endpoint servido por algo que não seja este Django |
| `device_id` estável enviado a cada abertura (mais logcat e Auto Backup) | primeira submissão a loja; Fase 3 amarra a conta |
| Cache em JSON claro, editável com root | qualquer PR que encoste dado de sessão em `TextPrefDataSource` |
| Manutenção cacheada bloqueia offline depois da janela | primeira janela de manutenção real |
| I/O de disco de `load()` na thread principal | `TextPrefEntity` deixar de ser duas chaves pequenas (Fase 3) |
| Dois `HttpClient` nunca fechados | terceiro cliente, ou pressão de memória |
| Sem limite de tamanho na resposta nem teto no `changelog` | transporte deixar de ser confiável |
| `store_url` aberto sem validar esquema | escrita em `AppVersion` fora do Admin |
| Sem logging no cliente: toda degradação do gate é silenciosa | primeiro incidente que precise do force update a sério |
| Caminho iOS nunca verificado em runtime (ATS × `http://localhost`) | antes da Fase 3 |

---

## Decisão sobre os achados roteados das revisões anteriores

**R1 — `http://` em claro.** A avaliação da Task 2 se sustenta, mas por um
motivo mais forte do que o registrado: o `network_security_config.xml` não
tem `base-config` nem `usesCleartextTraffic`, isenta apenas `10.0.2.2` e
`localhost`, e portanto *já falha fechado* para qualquer host real, mesmo
embarcado no release. Deixá-lo em `androidMain/res` é subótimo, não perigoso.
O problema real é outro e é o próprio `baseUrl`: `http://10.0.2.2:8000` no
Android e `http://localhost:8000` no iOS/jvm, sem nenhum mecanismo por build
type que troque isso no release. Publicado hoje, o gate ficaria permanentemente
inerte (host inalcançável → `EMPTY`) — o cenário que a seção 1 do design existe
para impedir. **Decisão: não é crítico para este merge** (nenhum usuário exposto,
zero builds publicados, e o app sequer funcionaria contra esse endereço), **é
bloqueante para a primeira publicação**, registrado com esse gatilho e com a
mitigação (`buildConfigField` por build type, `xcconfig` no iOS,
`network_security_config` em source set de debug, check de CI recusando
`http://` fora de debug).

**R2 — `runCatching` engole `CancellationException`.** **Verificado
empiricamente que não quebra o cancelamento estruturado hoje.** Teste que
cancela o job chamador 100 ms depois do início do fetch: `load()` não retornou
normalmente e `job.isCancelled == true`. O motivo é indireto — o
`TimeoutCoroutine` criado pelo `withTimeoutOrNull` é filho do job do chamador e,
ao ser completado com um valor estando já cancelado, o `JobSupport` descarta o
valor e reergue o cancelamento. Ou seja, a propriedade não vem de o
`runCatching` estar correto. **Decisão: não corrigir agora, registrar como
risco latente** com o gatilho "qualquer refactor que tire o `withTimeoutOrNull`
ou acrescente um `runCatching` externo" — a Fase 3 mexe neste mesmo caminho.

**R3 — `else -> NONE` sensível a caixa e espaço, sem log.** O caso é hoje
inalcançável pelo produtor real: `resolver.py:13-16` só emite as constantes
minúsculas e `serializers.py:20` valida a resposta com
`ChoiceField(choices=["none","recommended","forced"])`. O `else` fail-safe é
correto e deve continuar (status novo no servidor não pode bloquear app antigo).
**Decisão: normalizar *e* logar, mas não agora** — normalizar sem logar apenas
esconde melhor a mesma degradação, e não há infraestrutura de log no cliente
(registrada como risco próprio). Registrado com o gatilho "o endpoint passar a
ser servido por algo que não seja este Django".

**R4 — `device_id` estável.** Sim, carrega implicação de privacidade, mas
declarativa, não técnica: é um valor aleatório por instalação, sem vínculo com
hardware, com finalidade legítima e verificável (bucket de rollout em
`rollout.py`). **Decisão: nada muda no código agora; vira obrigação de
declaração** nas Nutrition Labels da App Store e no Data Safety do Play, com
gatilho na primeira submissão — e reavaliação na Fase 3, quando ele passa a
identificar o device no fluxo de auth. Dois ampliadores registrados junto: o
`LogLevel.HEADERS` escreve a URL com o `device_id` no log **em release**, e
`allowBackup="true"` (pré-existente) copia o banco para o Auto Backup do Google.

**R5 — cache em JSON claro, adulterável com root.** Risco real, alcance
pequeno: o cache só é lido quando a rede falha, o atacante é o dono do device, e
com root existem caminhos mais diretos (patch do APK, hook no `toUpdateStatus`,
`version` forjada no query param — este já registrado no `risks.md` do backend).
Quanto ao conteúdo, o bloco `auth` guardado no cache traz `issuer` e `client_id`
de um client OIDC **público** com PKCE: identificadores públicos por definição,
sem exposição. **Decisão: aceitar, registrar.** O gate é cooperativo por
construção; o que fecha de verdade é do lado do servidor (endpoint autenticado
e atestação de app na Fase 3). Gatilho registrado: qualquer PR que encoste dado
de sessão em `TextPrefDataSource`.

---

## Verificado e OK

**Segredos e histórico.** `git log -p main..HEAD` inteiro varrido por
credenciais, chaves privadas, tokens e cabeçalhos de autorização: nada. Nenhum
arquivo sensível adicionado e depois removido. O `.gitignore` da branch passou a
cobrir `docs/superpowers/plans/` e `.superpowers/` — nada de produção entra
nesses caminhos. O `docs/` versionado contém apenas o plano e a
spec originais do app (`docs/superpowers/`), sem credenciais.

**Bloco `auth` armazenado sem uso até a Fase 3.** `issuer` e `client_id` são os
identificadores públicos de um client OIDC público com PKCE. Não há `client
secret` no DTO (`AppConfigDto.kt:29-32`) nem no payload do servidor
(`resolver.py:116`). Guardá-los no cache não expõe nada — e é o que permite
trocar de realm sem republicar o app, que é o objetivo declarado.

**Resposta hostil — aninhamento profundo.** JSON com 50.000 níveis de colchetes
num campo desconhecido: `ignoreUnknownKeys` pulou o valor sem
`StackOverflowError` e a config foi decodificada corretamente (`status=FORCED`).
Independentemente disso, o `runCatching` de `load()` captura `Throwable`, então
até `OutOfMemoryError`/`StackOverflowError` degradam para o cache em vez de
derrubar o app. (O tamanho da resposta em si não tem teto — registrado como
risco.)

**Portal cativo (HTML com 200).** Resposta `200 text/html` com corpo de página
de login: o `ContentNegotiation` não tem conversor registrado para `text/html`,
`body<AppConfigDto>()` lança, o `runCatching` captura, `load()` devolve `EMPTY`
e o app abre. Verificado também que **o cache não é envenenado** nesse caminho:
`app_config_cache` permaneceu `null`. Variante em que o portal responde
`application/json` com outro schema: `MissingFieldException` → mesmo caminho →
`NONE`.

**Airplane mode, DNS, reset de conexão, backend fora do ar.** Todos caem no
mesmo `runCatching` e resolvem rápido (falha de resolução/conexão é imediata,
não espera timeout). Com cache válido da mesma versão, o estado bloqueante é
preservado (`servesCacheWhenTheNetworkFails`, `timesOutAndFallsBackToCacheWhenBackendHangs`);
sem cache, `EMPTY` e o app abre (`returnsEmptyStateWhenNetworkFailsWithNoCache`,
`timesOutAndReturnsEmptyStateWhenBackendHangsWithNoCache`). A Task 7 já havia
verificado "backend fora do ar" no emulador.

**Teto de tempo até a Home.** Medido no relógio virtual do `runTest`: contra um
host que aceita a conexão e nunca responde, `load()` retorna em **2.500 ms**
exatos — o `withTimeoutOrNull(FETCH_TIMEOUT_MS)` envolve só o fetch, e o
fallback de cache fica fora do escopo cancelado, por construção da Task 7. O
teto do splash em `App.kt:59` é 3.000 ms e libera a UI independentemente. Logo o
pior caso até a Home é **3 s**, com ~500 ms de folga entre a resolução da config
e o teto — folga que a leitura de disco do `cachedState()` consome em
microssegundos. Os dois `LaunchedEffect(Unit)` partem da mesma composição, então
não somam latências. Sem `HttpRequestRetry` no cliente do backend, então não há
retry comendo o orçamento.

**Custo da migração `1.sqm`.** Medido num banco com 5.000 linhas em
`GameEntity` (com `short_description` de 200 chars) e 20.000 em
`CacheMetaEntity`: **1,23 ms**. O seed dos mesmos dados levou 142 ms — duas
ordens de grandeza a mais. `CREATE TABLE` grava uma linha em `sqlite_master` e
não toca tabelas existentes; não há `ALTER`, cópia de dados nem criação de
índice. O `TextPrefMigrationTest` já provava a correção (v1→v2 cria a tabela e
preserva os dados); esta medição fecha o lado do custo.

**Wiring de DI e isolamento entre os dois clientes.** `NetworkModuleTest` e
`RepositoryModuleTest` (permanentes desde as Tasks 3 e 4) provam por
comportamento que `AppConfigApi` recebe o cliente `named("backend")` e
`FreeToGameApi` o cliente original, e que `AppConfigRepository` resolve pelo
Koin. O cliente do backend não tem `defaultRequest` de host, então a URL absoluta
de `AppConfigApi.kt:16` não sofre interferência da configuração do outro.

**Contrato de query params contra o backend real.** Conferido com o stack de pé:
`platform=android&version=1.0.0&device_id=…&lang=pt` devolve 200 com todos os
blocos. `device_id` de 32 chars hex está bem abaixo do
`DEVICE_ID_MAX_LENGTH = 128`; `lang` só assume `"pt"`/`"en"` (`App.kt:112-115`),
ambos no `ChoiceField`; `platform` é literal `"android"`/`"ios"`.

**Comportamento do gate.** `App.kt:171-187` — ordem correta (force update vence
manutenção, porque é a única das duas que o usuário pode resolver); o splash só
é liberado com `appConfig != null` (`:146`), evitando o flash de Home antes da
tela de bloqueio; a `UpdateAvailableSheet` não pode renderizar sobre as telas de
bloqueio (`:204`); a dispensa é por versão (`CacheManager.kt:51-56`), então
dispensar 2.0.0 não silencia um 3.0.0. As telas de gate aplicam
`windowInsetsPadding(WindowInsets.safeDrawing)` e `verticalScroll`, corrigidos
nas Tasks 6 e 7.

**`FeatureFlags` falha fechado.** `FeatureFlags.kt:9` — `state.flags[key] == true`;
chave desconhecida é sempre `false`. O app nunca liga uma funcionalidade por não
reconhecer o nome dela. Coberto por `featureFlagsReadFromState`.

---

## Verificações executadas

```
72 testes, 0 falhas   (./gradlew :composeApp:jvmTest — 70 anteriores + 2 novos)
:composeApp:compileDebugKotlinAndroid  BUILD SUCCESSFUL
:composeApp:compileKotlinJvm           BUILD SUCCESSFUL
```

Backend usado nas verificações contra o stack real: `gamelist-backend` em
`http://localhost:8000` (`make up`).
