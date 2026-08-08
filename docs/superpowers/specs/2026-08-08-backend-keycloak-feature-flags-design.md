# GameList — Backend Django, Keycloak Auth e Feature Flags

**Data:** 2026-08-08
**Status:** Aprovado
**Repositórios afetados:** `gamelist-backend` (novo), `gamelist-app` (existente)

---

## 1. Visão Geral

Adicionar ao GameList um backend próprio que resolve três necessidades:

1. **Autenticação de usuário** via Keycloak (OpenID Connect, PKCE para mobile)
2. **Feature flags e controle de versão** — poder desligar funcionalidades e bloquear versões antigas do app sem republicar nas lojas
3. **Sincronização de dados do usuário** — favoritos e listas deixam de ser apenas locais e passam a acompanhar a conta

O ponto que motiva a ordem de execução: **o endpoint de configuração precisa existir antes do primeiro deploy oficial.** Um app publicado sem ele não tem como ser bloqueado depois — não há canal para dizer "esta versão não funciona mais". Por isso a Fase 1 é feature flags, não autenticação.

### Decisões travadas

| Decisão | Escolha |
|---|---|
| Framework backend | Django 5 + DRF |
| Auth server | Keycloak 25 (separado, Django só valida tokens) |
| Banco | PostgreSQL 16 |
| Dashboard admin | React 18 + Vite (SPA custom, não Django Admin) |
| Estrutura | Monorepo `gamelist-backend` (API + dashboard) |
| Login | Obrigatório — sem guest mode |
| Dados do usuário | Sincronizam com o backend |
| Ambiente dev | Docker Compose (Postgres + Keycloak + Django + Redis + Vite) |

---

## 2. Repositórios e Responsabilidades

### 2.1 `gamelist-backend` (novo)

**Localização:** `/Users/victormurakami/Documents/kami/gamelist-backend`

Responsável por tudo que roda no servidor: API, banco, configuração do Keycloak, dashboard administrativo e infraestrutura de desenvolvimento.

```
gamelist-backend/
├── docker-compose.yml
├── docker-compose.prod.yml
├── .env.example
├── Makefile                       # atalhos: up, migrate, test, seed
├── keycloak/
│   ├── realm-export.json          # realm versionado em git
│   └── themes/                    # tema de login com a identidade do app
├── gamelist_api/                  # projeto Django
│   ├── config/                    # settings, urls raiz, wsgi/asgi
│   ├── core/
│   │   ├── auth/                  # validação JWT, middleware, permissions
│   │   ├── events/                # event bus interno
│   │   ├── cache.py
│   │   └── pagination.py
│   ├── modules/
│   │   ├── accounts/
│   │   ├── app_config/
│   │   ├── sync/
│   │   ├── notifications/         # stub — estrutura pronta, sem implementação
│   │   └── analytics/             # stub — estrutura pronta, sem implementação
│   └── api/v1/urls.py
├── admin-dashboard/               # React + Vite
│   ├── src/
│   │   ├── shell/
│   │   ├── core/
│   │   └── modules/
│   └── vite.config.ts
├── tests/
└── docs/
```

**Não é responsabilidade deste repo:** qualquer código Kotlin, decisões de UI do app mobile, integração com a FreeToGame API.

### 2.2 `gamelist-app` (existente)

**Localização:** `/Users/victormurakami/Documents/kami/gamelist-app`

Responsável pelo consumo da API: autenticação no device, cache do app-config, telas de bloqueio, e o motor de sincronização local.

**Novos diretórios:**

```
composeApp/src/commonMain/kotlin/com/kami/gamelist/
├── core/auth/                     # AuthManager, TokenStorage, OAuthClient
├── core/config/                   # AppConfigRepository, FeatureFlags
├── data/sync/                     # SyncManager, SyncApi
├── feature/auth/                  # LoginScreen
└── feature/gate/                  # ForceUpdateScreen, MaintenanceScreen
```

**Não é responsabilidade deste repo:** definir schema de API (consome o contrato do backend), lógica de resolução de flags (recebe resolvido).

### 2.3 Contrato entre os repos

O acoplamento é a API v1. Para evitar drift silencioso:

- O backend publica um **OpenAPI schema** em `/api/v1/schema/` (drf-spectacular)
- O schema é commitado em `gamelist-backend/docs/openapi.json` a cada mudança
- O app tem um teste que valida seus DTOs contra uma cópia versionada desse schema

Sem isso, uma mudança de campo no backend só aparece como crash em produção.

---

## 3. Agentes Especializados

Cada repositório tem agentes com escopo definido. A execução usa Subagent-Driven Development: um subagente por task, com contexto limpo.

### 3.1 Agentes do `gamelist-backend`

| Agente | Escopo | Entregas |
|---|---|---|
| **backend-infra** | Docker Compose, Postgres, Redis, Keycloak, CI, Makefile | Ambiente `docker compose up` funcional, realm importado |
| **backend-auth** | Validação JWT, middleware, permissions, provisioning JIT | `core/auth/` + `modules/accounts/` |
| **backend-api** | Módulos de domínio: `app_config`, `sync` | Models, serializers, views, routers, migrations |
| **backend-modularity** | Event bus, interfaces públicas entre módulos, stubs de `notifications`/`analytics` | `core/events/`, `modules/*/api.py` |
| **dashboard-uiux** | Design system, tokens, layout, componentes base (via skill `ui-ux-pro-max`) | `admin-dashboard/src/core/design/` |
| **dashboard-shell** | AppShell, moduleRegistry, roteamento, auth do dashboard | `admin-dashboard/src/shell/` |
| **dashboard-modules** | Telas: feature-flags, app-versions, maintenance, stats | `admin-dashboard/src/modules/` |
| **backend-security-perf** | Revisão transversal contínua — ver 3.3 | Relatório por fase + correções |

### 3.2 Agentes do `gamelist-app`

| Agente | Escopo | Entregas |
|---|---|---|
| **app-config** | `AppConfigRepository`, `FeatureFlags`, cache local | `core/config/` |
| **app-gate** | `ForceUpdateScreen`, `MaintenanceScreen`, integração no startup | `feature/gate/` |
| **app-auth** | `AuthManager`, `TokenStorage` (expect/actual), PKCE (expect/actual), `LoginScreen` | `core/auth/`, `feature/auth/` |
| **app-sync** | Migration do schema local, `SyncManager`, delta pull/push, resolução de conflito | `data/sync/`, `.sq` migrations |
| **app-security-perf** | Revisão transversal contínua — ver 3.3 | Relatório por fase + correções |

### 3.3 Agente de Segurança e Performance (ambos os repos)

Presente nos dois repositórios, roda **ao final de cada fase**, antes do merge. Não é um code review genérico — tem um mandato específico e adversarial: procurar o que vai quebrar mais tarde.

**Mandato:**

Questionar ativamente, com evidência concreta (arquivo, linha, cenário de falha), nunca com apontamentos genéricos.

**Eixo — Cibersegurança:**

- Validação de token: assinatura, `exp`, `aud`, `iss`, `nbf` estão todos verificados? Algoritmo está fixado (rejeitar `alg: none` e troca HS/RS)?
- JWKS: há cache com TTL? Rotação de chave quebra o serviço? Um atacante consegue forçar refetch e causar DoS?
- Autorização: cada endpoint autenticado checa **ownership**, não só autenticação? Usuário A consegue ler/escrever dados do usuário B via `client_uuid` forjado?
- Endpoints admin: role `admin` é verificada no backend, não só escondida no frontend?
- Endpoint público `/app-config/`: enumerável? Vaza informação sobre features não lançadas? Tem rate limit?
- Secrets: algum credential no repo, no compose, ou logado? `.env.example` sem valores reais?
- Injection: querysets com `raw()`/`extra()`? Campos de busca concatenados?
- Storage de token no app: Keystore/Keychain de fato, com flags corretas? Token em log, crash report, ou backup automático?
- PKCE: `code_verifier` com entropia suficiente? `state` validado? Redirect URI restrita (sem wildcard)?
- CORS/CSRF no dashboard: origins restritas? Cookies com `SameSite`/`Secure`?
- Payload de sync: limite de tamanho? Um cliente malicioso consegue enviar 100k itens e derrubar o worker?

**Eixo — Performance de banco:**

- Índices existem nas colunas de filtro real? (`keycloak_id`, `updated_at`, `client_uuid`, `deleted_at`)
- N+1: serializers de lista com `select_related`/`prefetch_related`?
- Sync delta: a query de `pull` usa índice composto `(user_id, updated_at)`? Faz table scan quando o usuário tem muitos registros?
- Soft delete: linhas deletadas crescem sem limite? Há política de purga?
- Migrations: alguma adiciona coluna `NOT NULL` sem default em tabela grande (lock)? Cria índice sem `CONCURRENTLY`?
- Crescimento projetado: com 10k usuários × 200 favoritos, qual o tamanho da tabela e o tempo do `pull`?

**Eixo — Performance de servidor:**

- `/app-config/` é o endpoint de maior volume (todo app start). Está cacheado no Redis? Qual o TTL? Invalidação ao salvar no dashboard funciona?
- Validação JWT faz round-trip ao Keycloak por request, ou valida localmente com JWKS cacheado?
- Há rate limiting? Por IP no endpoint público, por usuário nos autenticados?
- Conexões de banco: pool configurado? `CONN_MAX_AGE`?
- Endpoints de lista têm paginação obrigatória?
- Sync em background no app: debounce funciona ou dispara request por tap?

**Formato do relatório:**

```markdown
## Segurança & Performance — Fase N

### Críticos (bloqueiam merge)
- [ ] <arquivo:linha> — <problema> — <cenário de exploração/falha> — <correção proposta>

### Riscos futuros (não bloqueiam, registrar)
- <problema> — <em que escala vira crítico> — <mitigação>

### Verificado e OK
- <o que foi checado e passou>
```

Achados **críticos** são corrigidos antes do merge da fase. **Riscos futuros** vão para `docs/risks.md` do repo com o gatilho de escala anotado — o valor está em saber quando revisitar, não em resolver tudo agora.

---

## 4. Fases de Implementação

Cinco fases. Cada uma entrega algo funcional e verificável de ponta a ponta. As fases 1–3 são sequenciais; a 4 pode correr em paralelo com a 3.

---

### Fase 1 — Fundação + Feature Flags (`gamelist-backend`)

**Objetivo:** o endpoint público de configuração no ar. É a peça que precisa existir antes do primeiro release.

**Escopo:**

1. Scaffold do projeto Django com estrutura modular (`core/`, `modules/`)
2. Docker Compose: Postgres (2 databases), Redis, Django
3. Módulo `app_config`: models `AppVersion`, `FeatureFlag`, `MaintenanceWindow`
4. `GET /api/v1/app-config/` — público, cacheado no Redis
5. Lógica de resolução: comparação de versão semântica, rollout determinístico por hash
6. Django Admin básico (temporário, até o dashboard existir)
7. drf-spectacular + OpenAPI schema
8. CI: lint (ruff), testes (pytest), build

**Agentes:** `backend-infra` → `backend-api` → `backend-security-perf`

**Critério de conclusão:**
- `docker compose up` sobe tudo
- `curl "localhost:8000/api/v1/app-config/?platform=android&version=1.0.0"` retorna JSON válido
- Testes cobrem: resolução de update (none/recommended/forced), rollout determinístico, cache hit/miss
- Relatório de segurança e performance sem críticos abertos

**Riscos desta fase:**
- Comparação de versão semântica ingênua (`"1.10.0" < "1.9.0"` com comparação de string) — usar biblioteca (`packaging`), não split manual
- Cache sem invalidação ao salvar → flag não propaga

---

### Fase 2 — Consumo do App Config (`gamelist-app`)

**Objetivo:** o app respeita bloqueio de versão e manutenção. A partir daqui é seguro publicar.

**Escopo:**

1. `AppConfigRepository` — fetch, cache local (SQLDelight ou preferences), fallback
2. `FeatureFlags` — acesso tipado às flags
3. `ForceUpdateScreen` — bloqueante, botão abre a store
4. `MaintenanceScreen` — bloqueante, mensagem localizada do servidor
5. Integração no startup, antes do `refreshGames()` atual
6. Dialog dispensável para `status: recommended`
7. Testes: fallback pro cache, parsing de status, transições de gate

**Agentes:** `app-config` → `app-gate` → `app-security-perf`

**Critério de conclusão:**
- App com versão abaixo de `min_supported` mostra ForceUpdateScreen e não passa dali
- Sem rede e sem cache, o app abre normalmente (não trava no splash)
- Testes passando, build Android e iOS OK

**Riscos desta fase:**
- Falha de rede no `/app-config/` travando o splash — o fallback precisa ter timeout curto (~3s) e seguir
- Kill switch acidental: `min_supported` mal configurado bloqueia todo mundo. Mitigado pelo guard-rail da Fase 4

---

### Fase 3 — Autenticação (`gamelist-backend` + `gamelist-app`)

**Objetivo:** login obrigatório funcionando nas duas plataformas.

**Escopo backend:**

1. Keycloak no Docker Compose, realm `gamelist` exportado e versionado
2. Clients `gamelist-mobile` (PKCE S256) e `gamelist-admin` (Auth Code)
3. Roles `user` (default) e `admin`
4. `core/auth/`: validação JWT via JWKS cacheado, middleware, permission classes
5. `modules/accounts/`: `UserProfile`, provisioning JIT no primeiro request
6. `GET/PATCH /api/v1/me/`

**Escopo app:**

7. `TokenStorage` — expect/actual: Android Keystore (EncryptedSharedPreferences), iOS Keychain
8. `OAuthClient` — expect/actual: Chrome Custom Tabs, `ASWebAuthenticationSession`
9. `AuthManager` — estado, refresh automático, expiração
10. `LoginScreen` + integração no fluxo de startup
11. Config do Keycloak vinda do bloco `auth` do `/app-config/`

**Agentes:** `backend-auth` → `app-auth` → ambos os `security-perf`

**Critério de conclusão:**
- Login completo Android e iOS, token persistido entre restarts
- Refresh automático antes da expiração
- 401 limpa tokens e leva ao login
- Request autenticado cria `UserProfile` na primeira vez
- Backend rejeita: token expirado, assinatura inválida, `aud` errada, `alg: none`

**Riscos desta fase:**
- Keycloak fora do ar torna o app inutilizável (login obrigatório). **Mitigação:** refresh token de 30 dias; falha de rede no refresh (distinta de 401) mantém a sessão local válida
- Redirect URI com wildcard permite roubo de código de autorização — restringir a `com.kami.gamelist://auth/callback`
- Validação de token com round-trip ao Keycloak por request mata a latência — JWKS cacheado, validação local

---

### Fase 4 — Admin Dashboard (`gamelist-backend`)

**Objetivo:** gerenciar flags, versões e manutenção por interface própria. Pode correr em paralelo com a Fase 3 depois que o auth do backend estiver pronto.

**Escopo:**

1. **UI/UX primeiro** — skill `ui-ux-pro-max` define design system, tokens, componentes base. Esta task precede as telas.
2. Scaffold Vite + React + TypeScript + Tailwind
3. `shell/`: `AppShell`, `moduleRegistry`, roteamento, sidebar dinâmica
4. `core/`: api client com Bearer + refresh, auth via `keycloak-js`, TanStack Query
5. Endpoints admin no Django: `/admin/flags/`, `/admin/versions/`, `/admin/maintenance/`, `/admin/stats/`, `/admin/modules/`
6. Módulo `feature-flags`: CRUD, toggle, slider de rollout, filtro por plataforma
7. Módulo `app-versions`: editar `min_supported`/`latest` por plataforma, **com guard-rail**
8. Módulo `maintenance`: ativar/desativar, mensagem PT/EN, janela agendada
9. Módulo `stats`: usuários ativos, distribuição de versões em uso
10. Stubs `notifications` e `analytics` registrados e desabilitados — provam o contrato de modularidade

**Guard-rail do `app-versions`:** salvar `min_supported` bloqueia usuários imediatamente. A tela mostra quantos usuários ativos ficarão abaixo do corte e exige confirmação explícita. É a única ação do dashboard capaz de quebrar o app de alguém.

**Agentes:** `dashboard-uiux` → `dashboard-shell` → `dashboard-modules` → `backend-security-perf`

**Critério de conclusão:**
- Login como admin funciona; usuário sem role `admin` é rejeitado **pelo backend**
- Alterar uma flag reflete no `/app-config/` (cache invalidado)
- Adicionar um módulo novo não requer alteração no shell — verificado pelos stubs
- Módulo desabilitado via `/admin/modules/` some da sidebar sem redeploy

**Riscos desta fase:**
- Autorização só no frontend — o backend precisa checar a role em toda rota admin
- CORS permissivo em dev vazando para produção

---

### Fase 5 — Sincronização de Dados (`gamelist-backend` + `gamelist-app`)

**Objetivo:** favoritos e listas acompanham a conta entre devices.

**Escopo backend:**

1. `modules/sync/`: `Favorite`, `UserList`, `ListItem` com soft delete e `client_uuid`
2. `POST /api/v1/sync/pull/` — delta desde `since`, devolve `server_time`
3. `POST /api/v1/sync/push/` — idempotente por `client_uuid`, LWW por `updated_at`
4. Índices compostos `(user_id, updated_at)`
5. Limite de tamanho de payload
6. Event bus: `favorite.added`, `list.created` publicados (consumidores virão depois)

**Escopo app:**

7. Migration SQLDelight: `updated_at`, `deleted_at`, `synced`, `client_uuid`
8. Backfill: registros existentes ganham UUID e `synced = 0`
9. `SyncManager`: pull → merge → push → salva `server_time`
10. Gatilhos: login, pull-to-refresh, pós-mutação (debounce ~5s)
11. `UserRepository` marca `synced = 0` nas mutações — sem mudar a assinatura pública
12. Backoff exponencial em falha, silencioso na UI

**Agentes:** `backend-api` → `app-sync` → ambos os `security-perf`

**Critério de conclusão:**
- Favoritar em um device aparece em outro após sync
- Deletar não ressuscita no próximo pull (soft delete funciona)
- Push duplicado não duplica registros (idempotência)
- Offline continua funcionando integralmente; sync recupera ao voltar
- Isolamento: usuário A não acessa dados de B mesmo forjando `client_uuid`
- Testes existentes do app continuam passando sem alteração

**Riscos desta fase:**
- Migration com perda de dados — testar com banco populado antes do merge
- `client_uuid` forjado acessando dados de outro usuário — a query **sempre** filtra por `user` primeiro
- Tabelas de soft delete crescendo sem purga

---

## 5. Design Técnico

### 5.1 Fluxo de Autenticação

```
┌─────────┐          ┌──────────┐          ┌──────────┐
│ KMP App │          │ Keycloak │          │  Django  │
└────┬────┘          └────┬─────┘          └────┬─────┘
     │  1. Login (PKCE S256)                    │
     │───────────────────>│                     │
     │  2. access + refresh token               │
     │<───────────────────│                     │
     │  3. API call + Bearer                    │
     │─────────────────────────────────────────>│
     │                    │  4. JWKS (cacheado) │
     │                    │<────────────────────│
     │  5. Response       │                     │
     │<─────────────────────────────────────────│
```

Django **não gerencia credenciais**. Valida a assinatura do JWT localmente contra as chaves públicas do Keycloak (JWKS cacheado com TTL), verifica `exp`/`aud`/`iss` e fixa o algoritmo esperado. Não há round-trip ao Keycloak por request.

**Provisioning JIT:** no primeiro request autenticado, o `UserProfile` é criado a partir das claims (`sub`, `email`, `preferred_username`).

### 5.2 Modelos

**`accounts/`**

```python
class UserProfile(models.Model):
    keycloak_id  = models.UUIDField(unique=True, db_index=True)   # claim `sub`
    email        = models.EmailField()
    username     = models.CharField(max_length=150)
    display_name = models.CharField(max_length=150, blank=True)
    created_at   = models.DateTimeField(auto_now_add=True)
    last_seen_at = models.DateTimeField(auto_now=True)
```

**`app_config/`**

```python
class AppVersion(models.Model):
    platform       = models.CharField(choices=["android", "ios"])
    min_supported  = models.CharField(max_length=20)   # abaixo disso → bloqueia
    latest_version = models.CharField(max_length=20)   # sugere update
    store_url      = models.URLField()
    changelog      = models.TextField(blank=True)
    updated_at     = models.DateTimeField(auto_now=True)


class FeatureFlag(models.Model):
    key         = models.SlugField(unique=True)
    description = models.TextField(blank=True)
    enabled     = models.BooleanField(default=False)
    platforms   = models.JSONField(default=list)       # [] = todas
    rollout_pct = models.PositiveSmallIntegerField(default=100)
    min_version = models.CharField(max_length=20, blank=True)
    updated_at  = models.DateTimeField(auto_now=True)


class MaintenanceWindow(models.Model):
    active     = models.BooleanField(default=False)
    message_en = models.TextField()
    message_pt = models.TextField()
    starts_at  = models.DateTimeField(null=True, blank=True)
    ends_at    = models.DateTimeField(null=True, blank=True)
```

**Rollout determinístico:** hash estável de `device_id` → bucket 0–99. O mesmo device sempre cai no mesmo bucket, então a flag não pisca entre requests.

O `device_id` é um UUID gerado pelo app na primeira execução e persistido localmente (não é o ID de hardware — esse é restrito nas duas plataformas e não sobrevive a reinstalação de forma confiável). Reinstalar o app gera um novo `device_id` e pode mudar o bucket; isso é aceitável para rollout gradual.

**`sync/`**

```python
class Favorite(models.Model):
    user       = models.ForeignKey(UserProfile, related_name="favorites")
    game_id    = models.IntegerField()             # FreeToGame ID
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    deleted_at = models.DateTimeField(null=True)
    class Meta:
        unique_together = ("user", "game_id")
        indexes = [models.Index(fields=["user", "updated_at"])]


class UserList(models.Model):
    user        = models.ForeignKey(UserProfile, related_name="lists")
    client_uuid = models.UUIDField(db_index=True)  # gerado no app → idempotência
    name        = models.CharField(max_length=100)
    list_type   = models.CharField(choices=["PLAYING","WANT_TO_PLAY","PLAYED","CUSTOM"])
    created_at  = models.DateTimeField(auto_now_add=True)
    updated_at  = models.DateTimeField(auto_now=True)
    deleted_at  = models.DateTimeField(null=True)
    class Meta:
        unique_together = ("user", "client_uuid")
        indexes = [models.Index(fields=["user", "updated_at"])]


class ListItem(models.Model):
    user_list  = models.ForeignKey(UserList, related_name="items")
    game_id    = models.IntegerField()
    added_at   = models.DateTimeField(auto_now_add=True)
    deleted_at = models.DateTimeField(null=True)
    class Meta:
        unique_together = ("user_list", "game_id")
```

Duas escolhas que sustentam o sync:

**Soft delete (`deleted_at`)** — sem ele o sync não distingue "registro novo no servidor" de "registro deletado no cliente". Uma exclusão local seria desfeita no próximo pull.

**`client_uuid` nas listas** — o app cria listas offline com ID local (autoincrement do SQLDelight). Se o servidor gerasse o ID, haveria colisão entre IDs locais e remotos. Com UUID gerado no cliente, o push é idempotente.

### 5.3 API

**Público (sem auth)**

```
GET /api/v1/app-config/?platform=android&version=1.4.0&device_id=<uuid>
```

```json
{
  "update": {
    "status": "forced",
    "latest_version": "1.5.0",
    "store_url": "https://play.google.com/...",
    "changelog": "Novo sistema de listas"
  },
  "maintenance": { "active": false, "message": null },
  "flags": {
    "enable_social_sharing": true,
    "enable_cloud_sync": true
  },
  "auth": {
    "issuer": "https://keycloak.local/realms/gamelist",
    "client_id": "gamelist-mobile"
  }
}
```

O bloco `auth` faz a config do Keycloak vir do servidor — mudar realm ou URL não exige republicar o app.

**Autenticado**

```
GET   /api/v1/me/
PATCH /api/v1/me/
POST  /api/v1/sync/pull/     # { since: "2026-08-08T10:00:00Z" }
POST  /api/v1/sync/push/     # { favorites: [...], lists: [...] }
```

**Admin (auth + role `admin`)**

```
GET/POST/PATCH/DELETE  /api/v1/admin/flags/
GET/POST/PATCH         /api/v1/admin/versions/
GET/PATCH              /api/v1/admin/maintenance/
GET                    /api/v1/admin/stats/
GET                    /api/v1/admin/modules/
```

### 5.4 Estratégia de Sync

Delta com Last-Write-Wins.

Sync completo (enviar toda a base local a cada vez) desperdiça banda e cresce linearmente com o uso. Delta envia só o que mudou desde o último sync.

**Push** — registros com `updated_at` local maior que o último sync bem-sucedido:

```json
{
  "favorites": [
    { "game_id": 540, "updated_at": "2026-08-08T12:00:00Z", "deleted": false }
  ],
  "lists": [
    {
      "client_uuid": "a3f2...",
      "name": "Zerados em 2026",
      "list_type": "CUSTOM",
      "updated_at": "2026-08-08T12:01:00Z",
      "deleted": false,
      "items": [{ "game_id": 540, "added_at": "...", "deleted": false }]
    }
  ]
}
```

**Pull** — tudo que mudou depois de `since`:

```json
{
  "server_time": "2026-08-08T12:05:00Z",
  "favorites": [...],
  "lists": [...]
}
```

**Conflito — LWW por `updated_at`, por registro.** Um usuário por device e concorrência multi-device é rara aqui. Merge semântico (CRDT, OT) seria correto em mais casos mas custa muito mais complexidade do que o problema justifica.

**`server_time` é a âncora do próximo `since`** — nunca o relógio do device. Clock skew no cliente causaria perda silenciosa de registros.

**Fluxo no app:**

```
Login OK
  → pull(since = último sync salvo)
  → merge no SQLDelight
  → push(mudanças pendentes)
  → salva server_time como novo "último sync"
```

Falha de rede não bloqueia nada — SQLDelight continua sendo a fonte de verdade da UI.

### 5.5 Modularidade

Nem o Django nem o dashboard são microserviços agora. Ambos são estruturados para que **extrair** um módulo depois seja mecânico.

**Backend — três regras:**

1. **Sem ForeignKey entre módulos.** `sync` referencia usuário por `keycloak_id` (UUID), não por FK para `accounts.UserProfile`. FK cross-module transforma extração em cirurgia de schema.
2. **Comunicação por eventos.** `sync` publica `favorite.added`; `analytics` e `notifications` escutam. Hoje o bus é in-process; trocar por Redis/RabbitMQ não toca o código dos módulos.
3. **Interface pública por módulo** (`modules/<nome>/api.py`). Outros módulos nunca importam models ou serviços internos.

**Frontend — shell + módulos auto-registrados:**

```ts
// modules/feature-flags/index.ts
export default defineModule({
  id: 'feature-flags',
  title: 'Feature Flags',
  icon: FlagIcon,
  requiredRole: 'admin',
  routes: [{ path: '/flags', element: <FeatureFlagsPage /> }],
  navItems: [{ label: 'Feature Flags', to: '/flags' }],
})
```

Adicionar push notifications = criar `modules/push-notifications/` e registrar. Zero alteração no shell, sidebar ou roteamento.

O `AppShell` consulta `/api/v1/admin/modules/` — o backend informa quais módulos estão habilitados, permitindo desligar um sem redeploy do frontend.

### 5.6 Infraestrutura

```yaml
services:
  postgres:          # databases: gamelist + keycloak
  keycloak:          # :8080 — realm de keycloak/realm-export.json
  redis:             # cache + event bus
  django:            # :8000
  admin-dashboard:   # :5173 (Vite dev, só em dev)
```

Em produção o React vira build estático servido pelo Django (WhiteNoise).

**Dois databases no mesmo Postgres:** Keycloak precisa do próprio schema; misturar as tabelas dele com as do Django complica migrations e backups. Separar custa nada.

**Realm versionado em git:** `docker compose up` num clone novo já sobe configurado. Configurar Keycloak pela UI sem exportar é a receita clássica de "funciona só na minha máquina".

**Redis desde a Fase 1:** além de cache do `/app-config/` (endpoint de maior volume), é o substrato natural do event bus quando os módulos virarem serviços.

**Keycloak clients:**

| Client | Tipo | Uso |
|---|---|---|
| `gamelist-mobile` | público, PKCE S256 | app KMP |
| `gamelist-admin` | público, Auth Code | dashboard |

Realm `gamelist`, roles `user` (default) e `admin`, redirect mobile `com.kami.gamelist://auth/callback`, access token 5min, refresh 30 dias.

### 5.7 Impacto no App

**Novo fluxo de startup:**

```
Splash
  ↓
GET /app-config/ ── falha ──→ cache local; sem cache → segue offline
  ↓
status == "forced"?  ──→ ForceUpdateScreen (dead end)
maintenance.active?  ──→ MaintenanceScreen (dead end)
  ↓
Token válido? ──não──→ LoginScreen ──→ PKCE ──→ ✓
  ↓ sim
SyncManager.sync()   (não bloqueia a UI)
  ↓
Home
```

O splash animado atual já espera `SyncState` — ganha só mais um estágio antes do `refreshGames()`.

**`expect/actual` obrigatórios:** PKCE (Chrome Custom Tabs vs `ASWebAuthenticationSession`) e storage de token (Keystore vs Keychain) não têm implementação em `commonMain`.

**Migration local:**

```sql
ALTER TABLE FavoriteEntity ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0;
ALTER TABLE FavoriteEntity ADD COLUMN deleted_at INTEGER;
ALTER TABLE FavoriteEntity ADD COLUMN synced INTEGER NOT NULL DEFAULT 0;
-- idem UserListEntity (+ client_uuid) e ListItemEntity
```

Dados existentes ganham `client_uuid` na migration e sobem no primeiro sync.

**O que não muda:** SQLDelight segue como única fonte de verdade da UI; nenhum ScreenModel observa a rede; repositories da FreeToGame intocados; todas as telas, tema e localização intocados; testes existentes continuam passando.

O sync fica **abaixo** dos repositories de usuário. `UserRepository.addFavorite()` grava local e marca `synced = 0`; o `SyncManager` cuida do resto em background.

### 5.8 Tratamento de Erros

| Cenário | Comportamento |
|---|---|
| `/app-config/` falha | Último config cacheado. Sem cache → segue como `status: none` |
| Refresh de token com 401 | Limpa tokens → LoginScreen |
| Refresh de token com falha de rede | Mantém sessão local (não desloga) |
| Sync falha | Silencioso, backoff exponencial. UI intacta |
| Push com conflito | LWW resolve no servidor; resposta traz o vencedor |
| Keycloak fora do ar | LoginScreen com erro + retry. Quem já estava logado continua |

**Risco estrutural:** login obrigatório + Keycloak fora = app inutilizável, inclusive offline. Mitigação: refresh token de 30 dias e distinção entre falha de rede (mantém sessão) e 401 (desloga).

### 5.9 Testes

**Backend — pytest + pytest-django**
- `app_config`: resolução de update por versão/plataforma, rollout determinístico, cache hit/invalidação
- `sync`: push idempotente, LWW, soft delete, isolamento entre usuários
- `auth`: JWT válido/expirado/assinatura inválida/`aud` errada, provisioning JIT
- Integração: fluxo completo com Keycloak em testcontainer

**Dashboard — Vitest + Testing Library**
- Registro de módulos, guard de role, confirmação do `min_supported`

**App — kotlin.test + Turbine**
- `AppConfigRepository`: fallback pro cache, parsing de status
- `SyncManager`: merge de pull, geração de delta, resolução de conflito
- `AuthManager`: refresh, expiração, transições de estado

---

## 6. Riscos Consolidados

| Risco | Fase | Impacto | Mitigação |
|---|---|---|---|
| Kill switch acidental (`min_supported` errado) | 1, 4 | Todos os usuários bloqueados | Guard-rail com contagem de afetados + confirmação explícita |
| Keycloak indisponível com login obrigatório | 3 | App inutilizável | Refresh de 30 dias; falha de rede ≠ 401 |
| Comparação de versão por string | 1 | Bloqueio incorreto | Biblioteca `packaging`, nunca split manual |
| Migration local com perda de dados | 5 | Favoritos/listas perdidos | Testar com banco populado antes do merge |
| `client_uuid` forjado | 5 | Vazamento entre contas | Query sempre filtra por `user` primeiro |
| `/app-config/` sem cache | 1 | Gargalo no app start | Redis com TTL + invalidação no save |
| Tabelas de soft delete sem purga | 5 | Crescimento indefinido | Registrar em `docs/risks.md` com gatilho de escala |
| Drift do contrato API | todas | Crash em produção | OpenAPI versionado + teste de contrato no app |

---

## 7. Ordem de Execução

```
Fase 1 (backend)  ──→ Fase 2 (app)  ──→ Fase 3 (backend + app)  ──→ Fase 5 (backend + app)
                                              └──→ Fase 4 (dashboard) ──┘
```

A Fase 4 depende apenas do auth do backend (metade da Fase 3) e pode correr em paralelo com o auth do app.

Cada fase termina com o relatório do agente de segurança e performance, e nenhum achado crítico segue para a fase seguinte.
