# GameList App — Design Spec

## Overview

App mobile multiplataforma (Android + iOS) que funciona como catálogo de jogos free-to-play. Consome a FreeToGame API para dados dos jogos e armazena dados do usuário localmente com estratégia offline-first. Backend (MySQL + auth) será implementado em fase futura.

**Atribuição:** O app deve exibir crédito ao FreeToGame.com conforme exigido pela API.

## Stack Tecnológica

| Camada | Tecnologia | Justificativa |
|---|---|---|
| UI | Compose Multiplatform + Material 3 | UI compartilhada Android/iOS, theming dinâmico |
| Networking | Ktor Client | HTTP client multiplataforma nativo em Kotlin, coroutines |
| Serialização | Kotlinx.serialization | Parsing JSON type-safe, integração nativa com Ktor |
| DB local | SQLDelight | SQL multiplataforma, código type-safe gerado, ideal para offline-first |
| DI | Koin | Leve, KMP-friendly, sem code generation |
| Navegação | Voyager | Navegação multiplataforma para Compose, API simples, type-safe |
| Imagens | Coil 3 | Carregamento async com cache, suporte Compose Multiplatform |
| Ícones | Phosphor Compose | 7.000+ ícones, 6 pesos (thin→fill), visual moderno e consistente |
| Animações | Compottie (Lottie for KMP) | Micro-animações via Lottie JSON, ícones animados, transições |
| Async | Kotlinx.coroutines + Flow | Reatividade nativa, StateFlow para UI state |
| Build | Gradle com Version Catalog | Centraliza versões de dependências |

### Justificativas de escolha

- **SQLDelight > Room**: Room não tem suporte KMP completo. SQLDelight gera código a partir de SQL puro, mais controle e multiplataforma real.
- **Voyager > Decompose**: API mais simples e menos boilerplate. Decompose é mais poderoso mas overengineered para este escopo.
- **Koin > Kodein/manual**: Setup mínimo, integração com Compose, sem annotation processing.
- **Coil > Kamel**: Coil 3 tem suporte oficial Compose Multiplatform e é mantido pelo Google.

## Arquitetura

**Padrão:** Clean Architecture em camadas, módulo único com separação por pacotes.

```
com.kami.gamelist
├── core/
│   ├── network/       → Ktor client, config, interceptors
│   ├── database/      → SQLDelight driver, schema
│   ├── di/            → Koin modules
│   └── ui/            → Design system (tema, componentes reutilizáveis)
│
├── feature/
│   ├── home/          → Tela inicial, listagem de jogos
│   ├── search/        → Busca com filtros e histórico
│   ├── detail/        → Detalhes do jogo
│   ├── favorites/     → Lista de favoritos
│   └── lists/         → Listas personalizadas
│
├── data/
│   ├── remote/        → API DTOs, FreeToGame API service
│   ├── local/         → SQLDelight queries, DAOs
│   ├── repository/    → Repositórios (orquestram remote + local)
│   └── model/         → Domain models
│
└── App.kt            → Entry point, setup de DI e navegação
```

**Fluxo de dados (offline-first):**

```
UI (Compose) → ViewModel → Repository → Local DB (source of truth)
                                      ↘ Remote API → sincroniza → Local DB
```

O Repository sempre retorna dados do banco local. Em paralelo, busca da API e atualiza o banco. A UI observa o banco via Flow/StateFlow e atualiza automaticamente.

## Modelo de Dados

### Domain models

```kotlin
data class Game(
    val id: Int,
    val title: String,
    val thumbnail: String,
    val shortDescription: String,
    val gameUrl: String,
    val genre: String,
    val platform: String,
    val publisher: String,
    val developer: String,
    val releaseDate: String,
    val freetogameProfileUrl: String
)

data class GameDetail(
    val game: Game,
    val description: String,
    val status: String,
    val screenshots: List<Screenshot>,
    val minimumSystemRequirements: SystemRequirements?
)

data class Screenshot(
    val id: Int,
    val image: String
)

data class SystemRequirements(
    val os: String?,
    val processor: String?,
    val memory: String?,
    val graphics: String?,
    val storage: String?
)
```

### Modelos locais do usuário

```kotlin
data class UserList(
    val id: Long,
    val name: String,
    val type: ListType,
    val createdAt: Long
)

enum class ListType {
    PLAYING, WANT_TO_PLAY, PLAYED, CUSTOM
}

data class UserListEntry(
    val listId: Long,
    val gameId: Int,
    val addedAt: Long
)

data class SearchHistory(
    val query: String,
    val searchedAt: Long
)
```

### Preparação para backend futuro

```kotlin
enum class OperationType {
    ADD_FAVORITE, REMOVE_FAVORITE,
    ADD_TO_LIST, REMOVE_FROM_LIST,
    CREATE_LIST, DELETE_LIST
}

data class PendingOperation(
    val id: Long,
    val type: OperationType,
    val payload: String,
    val createdAt: Long
)
```

Fila de operações pendentes criada no MVP mas consumida apenas quando o backend for implementado.

## Telas e Navegação

### Telas do MVP

| Tela | Descrição |
|---|---|
| HomeScreen | Grid de jogos com chips de filtro (gênero, plataforma). Pull-to-refresh. |
| SearchScreen | Campo de busca com histórico recente, resultados em tempo real, filtros. |
| GameDetailScreen | Thumbnail grande, descrição, screenshots carousel, requisitos de sistema, botão favoritar, botão adicionar a lista. |
| FavoritesScreen | Lista dos jogos favoritados, com opção de remover. |
| ListsScreen | Visualização das listas (Jogando, Quero jogar, Já joguei, custom). Criar/editar/deletar listas custom. |
| ListDetailScreen | Jogos dentro de uma lista específica. |

### Navegação

```
BottomNavBar
├── Home ─────────→ GameDetail
├── Search ───────→ GameDetail
├── Favorites ────→ GameDetail
└── Lists ────────→ ListDetail → GameDetail
```

- BottomNavigationBar com 4 abas: Home, Search, Favorites, Lists
- GameDetailScreen acessível de qualquer aba
- Voyager gerencia back stack por aba (cada aba mantém seu histórico)

### Estado da UI

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
```

Cada tela tem seu ScreenModel (Voyager) que expõe `StateFlow<UiState<T>>`.

## Estratégia Offline-First

### Fluxo para dados de jogos (leitura)

1. UI observa Flow do SQLDelight → exibe dados imediatamente (cache)
2. Repository dispara request à FreeToGame API em background
3. API responde → Repository faz upsert no SQLDelight
4. SQLDelight emite novo Flow → UI atualiza automaticamente

### Fluxo para dados do usuário (favoritos, listas)

1. Ação do usuário (favoritar, criar lista, etc.)
2. Grava direto no SQLDelight → UI reflete instantaneamente
3. (Futuro) Quando backend existir: enfileira operação para sync

### Cache e freshness

| Dado | Estratégia | TTL |
|---|---|---|
| Lista de jogos | Stale-while-revalidate | 1 hora |
| Detalhe do jogo | Stale-while-revalidate | 6 horas |
| Thumbnails/imagens | Cache do Coil (disco) | 7 dias |
| Favoritos/listas | Local-only (sem TTL) | — |
| Histórico de busca | Local-only (sem TTL) | — |

## Componentes de UI e Design System

### Theme

```kotlin
GameListTheme {
    colorScheme    → Paleta escura como base (gaming vibe), com accent vibrante
    typography     → Família moderna (Inter/Outfit), hierarquia clara
    shapes         → Rounded corners consistentes (8dp cards, 16dp modais)
}
```

- Dark theme como padrão (público gamer), com light theme disponível
- Imagens como protagonistas — thumbnails grandes, screenshots em destaque
- **Animações como pilar de UX** — experiência fluida e polida é prioridade:
  - Phosphor Icons com transição de peso (outline → fill) para estados ativos
  - Compottie (Lottie) para animações ricas: favoritar (coração), loading states, empty states, sucesso/erro
  - Compose animations API para: transições de tela (shared element transitions), shimmer skeletons, expand/collapse de cards, pull-to-refresh customizado
  - Princípio: toda interação do usuário deve ter feedback visual sutil e imediato

### Componentes reutilizáveis

| Componente | Uso |
|---|---|
| GameCard | Card do jogo com thumbnail, título, gênero, plataforma. |
| GameGrid | LazyVerticalGrid responsivo (2 colunas portrait, 3+ landscape). |
| FilterChipRow | Linha horizontal scrollável de chips para filtros. |
| SearchBar | Campo de busca com ícone, clear button, histórico em dropdown. |
| ScreenshotCarousel | HorizontalPager para screenshots na tela de detalhe. |
| FavoriteButton | Ícone de coração com animação de toggle. |
| ListSelector | Bottom sheet para escolher em qual lista adicionar um jogo. |
| EmptyState | Placeholder ilustrado para listas vazias. |
| ErrorState | Mensagem de erro com botão de retry. |

### Skeletons

| Skeleton | Onde |
|---|---|
| GameCardSkeleton | Home e Search (grid de cards shimmer) |
| GameDetailSkeleton | Tela de detalhe (thumbnail + texto + screenshots) |
| ListItemSkeleton | Favoritos e listas |

Cada skeleton espelha o layout real do componente correspondente com animação shimmer. Exibidos enquanto `UiState` está em `Loading`.

## Tratamento de Erros e Conectividade

### Cenários

| Cenário | Comportamento |
|---|---|
| Sem internet + tem cache | Exibe dados do cache, badge discreto "Offline" |
| Sem internet + sem cache | EmptyState com mensagem "Sem conexão" e botão retry |
| API retorna erro | Exibe cache se disponível + snackbar com erro. Sem cache → ErrorState com retry |
| Timeout | Retry automático 1x, depois fallback para cache ou ErrorState |

### Monitor de conectividade

```kotlin
expect class ConnectivityMonitor {
    val isOnline: Flow<Boolean>
}
```

- Android: `ConnectivityManager`
- iOS: `NWPathMonitor`
- Quando volta online, dispara re-sync automático dos dados stale
- UI reage ao `isOnline` para mostrar/esconder indicador offline

### Retry policy

```kotlin
install(HttpRequestRetry) {
    retryOnServerErrors(maxRetries = 2)
    exponentialDelay()
}
```

## Testes

| Tipo | Cobertura | Ferramenta |
|---|---|---|
| Unit tests | ViewModels, Repositories, mapeamento de DTOs, lógica de cache | kotlin.test + Turbine |
| Fake/mock | API responses, banco em memória | Ktor MockEngine + SQLDelight in-memory driver |
| UI tests | Componentes isolados, interações críticas | Compose UI Test |

### Prioridades para MVP

- Repositories — garantem que o fluxo offline-first funciona (cache hit, cache miss, sync, erro de rede)
- ViewModels — testados via StateFlow assertions com Turbine
- UI — componentes mais críticos (GameCard, FilterChipRow, navegação entre abas)

### Estrutura

```
src/commonTest/    → Unit tests compartilhados
src/androidTest/   → Testes específicos Android
src/iosTest/       → Testes específicos iOS
```

## FreeToGame API — Referência

Base URL: `https://www.freetogame.com/api`

### Endpoints

| Endpoint | Params | Descrição |
|---|---|---|
| `GET /games` | — | Lista todos os jogos |
| `GET /games?platform={p}` | `windows`, `browser`, `all` | Filtra por plataforma |
| `GET /games?category={c}` | ver lista abaixo | Filtra por categoria |
| `GET /games?sort-by={s}` | `release-date`, `alphabetical`, `relevance`, `popularity` | Ordena resultados |
| `GET /games?platform={p}&category={c}&sort-by={s}` | combinação | Filtros combinados |
| `GET /filter?tag={t1.t2}&platform={p}` | tags dot-separated | Filtro avançado por múltiplas tags |
| `GET /game?id={id}` | id numérico | Detalhes de um jogo |

### Categorias disponíveis

mmorpg, shooter, pvp, mmofps, strategy, moba, racing, sports, social, sandbox, open-world, survival, pve, pixel, voxel, zombie, turn-based, first-person, third-person, top-down, tank, space, sailing, side-scroller, superhero, permadeath, card, battle-royale, mmo, mmotps, mmorts, 3d, 2d, anime, fantasy, sci-fi, fighting, action-rpg, action, military, martial-arts, flight, low-spec, tower-defense, horror

### Constraints

- Rate limit: 10 req/s (requer throttle no client)
- Sem autenticação
- Atribuição obrigatória ao FreeToGame.com
- CORS indisponível (não impacta app nativo)

## DevOps & CI/CD

### Developer Experience

- `Makefile` na raiz com comandos comuns: `make setup`, `make run-android`, `make run-ios`, `make test`, `make lint`, `make build-debug`, `make build-release`
- `.editorconfig` para consistência de estilo entre IDEs
- `README.md` com instruções de setup, requisitos, e como rodar
- Git hooks via pre-commit: lint + format check

### CI/CD (GitHub Actions)

- **PR checks**: build + test (Android) em cada PR
- **Main merge**: build debug APK, run tests, upload artifact
- **Release tag**: build release, sign, upload artifacts

### Fastlane (preparação futura)

- Estrutura de diretórios `fastlane/` criada no scaffold
- `Gemfile` com dependência do Fastlane
- `Fastfile` com lanes básicas: `test`, `build_debug`, `build_release`
- Preparado para adição de `supply` (Google Play) e `deliver` (App Store) quando necessário

## Escopo Futuro (fora do MVP)

- Backend com MySQL (auth JWT + OAuth Google/Apple)
- Sincronização de favoritos/listas na nuvem (consumir fila PendingOperation)
- Perfil de usuário
- Reviews e ratings
- Notificações de novos jogos
- Modularização por feature (extração de módulos Gradle)
