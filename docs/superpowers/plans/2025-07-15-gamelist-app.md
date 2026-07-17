# GameList App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a cross-platform (Android + iOS) free-to-play game catalog app with offline-first architecture, consuming the FreeToGame API.

**Architecture:** Single KMP module with Clean Architecture layers separated by packages. Compose Multiplatform for shared UI. SQLDelight as local source of truth with stale-while-revalidate sync from the FreeToGame API. Voyager for tab-based navigation.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Ktor Client, SQLDelight, Koin, Voyager, Coil 3, Phosphor Compose, Compottie, Material 3

## Global Constraints

- Kotlin 2.1+, Compose Multiplatform 1.7+
- Targets: Android (minSdk 24) + iOS (arm64)
- Package root: `com.kami.gamelist`
- Single Gradle module (`composeApp`) — no multi-module
- All shared code in `commonMain`; platform code via `expect`/`actual`
- TDD: write failing test → implement → pass → commit
- FreeToGame API base URL: `https://www.freetogame.com/api`
- FreeToGame API rate limit: 10 req/s — Ktor client must throttle
- FreeToGame API platforms: `windows`, `browser`, `all`
- FreeToGame API sort options: `release-date`, `alphabetical`, `relevance`, `popularity`
- FreeToGame attribution required: must credit FreeToGame.com in the app
- Offline-first: SQLDelight is the single source of truth for UI
- Dark theme as default
- Every user interaction must have visual feedback (animation)
- Makefile for common dev commands; GitHub Actions CI; Fastlane structure prepared

---

## File Map

```
gamelist-app/
├── Makefile                                  (dev commands: setup, run, test, build, lint)
├── .editorconfig                             (code style consistency)
├── .gitignore                                (standard KMP ignores)
├── README.md                                 (setup instructions, requirements, how to run)
├── build.gradle.kts                          (root project config)
├── settings.gradle.kts                       (module includes + repo config)
├── gradle.properties                         (KMP/Compose flags)
├── gradle/
│   └── libs.versions.toml                    (version catalog)
├── .github/
│   └── workflows/
│       ├── pr-check.yml                      (build + test on PRs)
│       └── release.yml                       (build release on tags)
├── fastlane/
│   ├── Fastfile                              (lanes: test, build_debug, build_release)
│   └── Appfile                               (app identifiers)
├── Gemfile                                   (Fastlane dependency)
├── composeApp/
│   ├── build.gradle.kts                      (KMP targets, dependencies, SQLDelight config)
│   └── src/
│       ├── commonMain/
│       │   ├── composeResources/
│       │   │   └── files/                    (Lottie JSON animations)
│       │   ├── sqldelight/
│       │   │   └── com/kami/gamelist/
│       │   │       ├── Game.sq               (games table + queries)
│       │   │       ├── Favorite.sq           (favorites table + queries)
│       │   │       ├── UserList.sq           (user lists table + queries)
│       │   │       ├── UserListEntry.sq      (list entries table + queries)
│       │   │       ├── SearchHistory.sq      (search history table + queries)
│       │   │       └── CacheMeta.sq          (cache timestamps table)
│       │   └── kotlin/com/kami/gamelist/
│       │       ├── App.kt                    (root Composable, Koin init, theme wrapper)
│       │       ├── core/
│       │       │   ├── network/
│       │       │   │   ├── HttpClientFactory.kt      (Ktor client builder with retry, logging, serialization)
│       │       │   │   └── ConnectivityMonitor.kt    (expect class for network state)
│       │       │   ├── database/
│       │       │   │   └── DriverFactory.kt           (expect class for SQLDelight driver)
│       │       │   ├── di/
│       │       │   │   ├── NetworkModule.kt           (Ktor + ConnectivityMonitor)
│       │       │   │   ├── DatabaseModule.kt          (SQLDelight driver + database)
│       │       │   │   ├── RepositoryModule.kt        (all repositories)
│       │       │   │   └── FeatureModule.kt           (all ScreenModels)
│       │       │   └── ui/
│       │       │       ├── theme/
│       │       │       │   ├── Theme.kt               (GameListTheme composable)
│       │       │       │   ├── Color.kt               (dark/light color schemes)
│       │       │       │   ├── Type.kt                (typography scale)
│       │       │       │   └── Shape.kt               (shape tokens)
│       │       │       ├── components/
│       │       │       │   ├── GameCard.kt            (game card + skeleton)
│       │       │       │   ├── GameGrid.kt            (responsive lazy grid)
│       │       │       │   ├── FilterChipRow.kt       (horizontal scrollable chips)
│       │       │       │   ├── SearchBar.kt           (search input + history dropdown)
│       │       │       │   ├── ScreenshotCarousel.kt  (horizontal pager for screenshots)
│       │       │       │   ├── FavoriteButton.kt      (animated heart toggle)
│       │       │       │   ├── ListSelector.kt        (bottom sheet list picker)
│       │       │       │   ├── EmptyState.kt          (illustrated empty placeholder)
│       │       │       │   ├── ErrorState.kt          (error message + retry)
│       │       │       │   ├── OfflineBanner.kt       (connectivity indicator)
│       │       │       │   └── ShimmerEffect.kt       (shimmer animation modifier)
│       │       │       └── UiState.kt                 (sealed interface Loading/Success/Error)
│       │       ├── data/
│       │       │   ├── model/
│       │       │   │   ├── Game.kt                    (domain models: Game, GameDetail, Screenshot, SystemRequirements)
│       │       │   │   └── UserData.kt                (domain models: UserList, ListType, UserListEntry, SearchHistory)
│       │       │   ├── remote/
│       │       │   │   ├── dto/
│       │       │   │   │   ├── GameDto.kt             (API response DTO for game list)
│       │       │   │   │   └── GameDetailDto.kt       (API response DTO for game detail)
│       │       │   │   ├── FreeToGameApi.kt           (API service interface + implementation)
│       │       │   │   └── DtoMapper.kt               (DTO → domain model mappers)
│       │       │   ├── local/
│       │       │   │   ├── GameLocalDataSource.kt     (wraps SQLDelight game queries)
│       │       │   │   ├── UserLocalDataSource.kt     (wraps SQLDelight favorites/lists/history queries)
│       │       │   │   └── CacheManager.kt            (TTL logic for stale-while-revalidate)
│       │       │   └── repository/
│       │       │       ├── GameRepository.kt          (orchestrates remote + local for games)
│       │       │       └── UserRepository.kt          (favorites, lists, search history)
│       │       └── feature/
│       │           ├── home/
│       │           │   ├── HomeScreenModel.kt
│       │           │   └── HomeScreen.kt
│       │           ├── search/
│       │           │   ├── SearchScreenModel.kt
│       │           │   └── SearchScreen.kt
│       │           ├── detail/
│       │           │   ├── GameDetailScreenModel.kt
│       │           │   └── GameDetailScreen.kt
│       │           ├── favorites/
│       │           │   ├── FavoritesScreenModel.kt
│       │           │   └── FavoritesScreen.kt
│       │           ├── lists/
│       │           │   ├── ListsScreenModel.kt
│       │           │   ├── ListsScreen.kt
│       │           │   ├── ListDetailScreenModel.kt
│       │           │   └── ListDetailScreen.kt
│       │           └── navigation/
│       │               ├── AppNavigator.kt            (Voyager TabNavigator setup)
│       │               └── Tabs.kt                    (tab definitions with Phosphor icons)
│       ├── commonTest/
│       │   └── kotlin/com/kami/gamelist/
│       │       ├── data/
│       │       │   ├── remote/
│       │       │   │   ├── FreeToGameApiTest.kt
│       │       │   │   └── DtoMapperTest.kt
│       │       │   ├── local/
│       │       │   │   ├── GameLocalDataSourceTest.kt
│       │       │   │   └── UserLocalDataSourceTest.kt
│       │       │   └── repository/
│       │       │       ├── GameRepositoryTest.kt
│       │       │       └── UserRepositoryTest.kt
│       │       └── feature/
│       │           ├── home/HomeScreenModelTest.kt
│       │           ├── search/SearchScreenModelTest.kt
│       │           ├── detail/GameDetailScreenModelTest.kt
│       │           ├── favorites/FavoritesScreenModelTest.kt
│       │           └── lists/ListsScreenModelTest.kt
│       ├── androidMain/
│       │   ├── AndroidManifest.xml
│       │   └── kotlin/com/kami/gamelist/
│       │       ├── core/
│       │       │   ├── network/ConnectivityMonitor.android.kt
│       │       │   └── database/DriverFactory.android.kt
│       │       └── MainActivity.kt
│       └── iosMain/
│           └── kotlin/com/kami/gamelist/
│               ├── core/
│               │   ├── network/ConnectivityMonitor.ios.kt
│               │   └── database/DriverFactory.ios.kt
│               └── MainViewController.kt
└── iosApp/
    ├── iosApp/
    │   ├── iOSApp.swift
    │   └── ContentView.swift
    └── iosApp.xcodeproj/
```

---

### Task 1: Project Scaffold, DevOps & Dependencies

**Files:**
- Create: `build.gradle.kts`
- Create: `settings.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `composeApp/build.gradle.kts`
- Create: `composeApp/src/androidMain/AndroidManifest.xml`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/App.kt` (placeholder)
- Create: `composeApp/src/androidMain/kotlin/com/kami/gamelist/MainActivity.kt`
- Create: `composeApp/src/iosMain/kotlin/com/kami/gamelist/MainViewController.kt`
- Create: `Makefile`
- Create: `.editorconfig`
- Create: `.gitignore`
- Create: `README.md`
- Create: `.github/workflows/pr-check.yml`
- Create: `.github/workflows/release.yml`
- Create: `Gemfile`
- Create: `fastlane/Fastfile`
- Create: `fastlane/Appfile`

**Interfaces:**
- Produces: Buildable KMP project with all dependencies resolved, dev tooling (Makefile), CI/CD pipelines (GitHub Actions), and Fastlane skeleton. Subsequent tasks add code into this structure.

- [ ] **Step 1: Create Version Catalog**

```toml
# gradle/libs.versions.toml
[versions]
kotlin = "2.1.21"
compose-multiplatform = "1.8.1"
ktor = "3.1.3"
sqldelight = "2.0.2"
koin = "4.0.4"
voyager = "1.1.0-beta03"
coil = "3.2.0"
coroutines = "1.10.2"
serialization = "1.8.1"
compottie = "2.0.0-rc04"
turbine = "1.2.0"

[libraries]
# Ktor
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }

# SQLDelight
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
sqldelight-android = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-native = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }

# Koin
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }

# Voyager
voyager-tabNavigator = { module = "cafe.adriel.voyager:voyager-tab-navigator", version.ref = "voyager" }
voyager-navigator = { module = "cafe.adriel.voyager:voyager-navigator", version.ref = "voyager" }
voyager-screenModel = { module = "cafe.adriel.voyager:voyager-screenmodel", version.ref = "voyager" }
voyager-koin = { module = "cafe.adriel.voyager:voyager-koin", version.ref = "voyager" }
voyager-transitions = { module = "cafe.adriel.voyager:voyager-transitions", version.ref = "voyager" }

# Coil
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coil-network-ktor = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil" }

# Compottie (Lottie for KMP)
compottie = { module = "io.github.nicholasgasior.compottie:compottie", version.ref = "compottie" }
compottie-resources = { module = "io.github.nicholasgasior.compottie:compottie-resources", version.ref = "compottie" }

# Kotlinx
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }

# Test
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
```

- [ ] **Step 2: Create root build.gradle.kts**

```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.sqldelight) apply false
}
```

- [ ] **Step 3: Create settings.gradle.kts**

```kotlin
// settings.gradle.kts
rootProject.name = "GameList"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolution {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
```

- [ ] **Step 4: Create gradle.properties**

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx2048M -Dfile.encoding=UTF-8
org.gradle.caching=true

kotlin.code.style=official

android.useAndroidX=true
android.nonTransitiveRClass=true
```

- [ ] **Step 5: Create composeApp/build.gradle.kts**

```kotlin
// composeApp/build.gradle.kts
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.sqldelight)
    id("com.android.application")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.animation)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)

            implementation(libs.sqldelight.coroutines)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.voyager.navigator)
            implementation(libs.voyager.tabNavigator)
            implementation(libs.voyager.screenModel)
            implementation(libs.voyager.koin)
            implementation(libs.voyager.transitions)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            implementation(libs.compottie)
            implementation(libs.compottie.resources)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.turbine)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native)
        }
    }
}

android {
    namespace = "com.kami.gamelist"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kami.gamelist"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("GameListDatabase") {
            packageName.set("com.kami.gamelist.db")
        }
    }
}
```

- [ ] **Step 6: Create AndroidManifest.xml**

```xml
<!-- composeApp/src/androidMain/AndroidManifest.xml -->
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:name=".GameListApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="GameList"
        android:supportsRtl="true"
        android:theme="@style/Theme.GameList">
        <activity
            android:name=".MainActivity"
            android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 7: Create platform entry points (placeholder)**

Create `composeApp/src/androidMain/kotlin/com/kami/gamelist/MainActivity.kt`:

```kotlin
package com.kami.gamelist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}
```

Create `composeApp/src/androidMain/kotlin/com/kami/gamelist/GameListApplication.kt`:

```kotlin
package com.kami.gamelist

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class GameListApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@GameListApplication)
        }
    }
}
```

Create `composeApp/src/iosMain/kotlin/com/kami/gamelist/MainViewController.kt`:

```kotlin
package com.kami.gamelist

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController { App() }
```

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/App.kt`:

```kotlin
package com.kami.gamelist

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun App() {
    MaterialTheme {
        Text("GameList App")
    }
}
```

- [ ] **Step 8: Install Gradle wrapper and verify build**

Run: `gradle wrapper --gradle-version 8.14` (or use an existing wrapper)

Then run:
```bash
./gradlew :composeApp:compileKotlinAndroid
```
Expected: BUILD SUCCESSFUL — all dependencies resolve and the project compiles.

- [ ] **Step 9: Create Makefile**

```makefile
# Makefile
.PHONY: setup run-android run-ios test lint build-debug build-release clean

setup:
	./gradlew --version
	@echo "✓ Gradle OK"
	@echo "Run 'make run-android' or 'make run-ios' to start the app"

run-android:
	./gradlew :composeApp:installDebug

run-ios:
	cd iosApp && xcodebuild -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 16' build

test:
	./gradlew :composeApp:allTests

test-android:
	./gradlew :composeApp:testDebugUnitTest

lint:
	./gradlew :composeApp:lintDebug

build-debug:
	./gradlew :composeApp:assembleDebug

build-release:
	./gradlew :composeApp:assembleRelease

clean:
	./gradlew clean
```

- [ ] **Step 10: Create .editorconfig**

```ini
# .editorconfig
root = true

[*]
charset = utf-8
end_of_line = lf
indent_style = space
indent_size = 4
insert_final_newline = true
trim_trailing_whitespace = true

[*.{kt,kts}]
indent_size = 4

[*.{xml,yml,yaml,json,toml}]
indent_size = 2

[Makefile]
indent_style = tab
```

- [ ] **Step 11: Create .gitignore**

```gitignore
# .gitignore
# Gradle
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar

# IDE
.idea/
*.iml
.DS_Store

# Android
local.properties
*.apk
*.aab

# iOS
iosApp/iosApp.xcodeproj/xcuserdata/
iosApp/iosApp.xcodeproj/project.xcworkspace/xcuserdata/
iosApp/Pods/

# Fastlane
fastlane/report.xml
fastlane/Preview.html
fastlane/screenshots/
fastlane/test_output/

# Ruby
vendor/
.bundle/
```

- [ ] **Step 12: Create GitHub Actions CI**

Create `.github/workflows/pr-check.yml`:

```yaml
name: PR Check

on:
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - uses: gradle/actions/setup-gradle@v4

      - name: Run tests
        run: ./gradlew :composeApp:allTests

      - name: Build debug APK
        run: ./gradlew :composeApp:assembleDebug

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: composeApp/build/reports/tests/
```

Create `.github/workflows/release.yml`:

```yaml
name: Release

on:
  push:
    tags:
      - 'v*'

jobs:
  build-release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - uses: gradle/actions/setup-gradle@v4

      - name: Run tests
        run: ./gradlew :composeApp:allTests

      - name: Build release APK
        run: ./gradlew :composeApp:assembleRelease

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: release-apk
          path: composeApp/build/outputs/apk/release/
```

- [ ] **Step 13: Create Fastlane skeleton**

Create `Gemfile`:

```ruby
source "https://rubygems.org"

gem "fastlane"
```

Create `fastlane/Appfile`:

```ruby
# Android
package_name("com.kami.gamelist")

# iOS (uncomment when ready)
# app_identifier("com.kami.gamelist")
# apple_id("your@email.com")
```

Create `fastlane/Fastfile`:

```ruby
default_platform(:android)

platform :android do
  desc "Run unit tests"
  lane :test do
    gradle(task: ":composeApp:allTests")
  end

  desc "Build debug APK"
  lane :build_debug do
    gradle(task: ":composeApp:assembleDebug")
  end

  desc "Build release APK"
  lane :build_release do
    gradle(
      task: ":composeApp:assembleRelease",
      print_command: false
    )
  end

  # Uncomment when ready for Play Store
  # desc "Deploy to Google Play internal track"
  # lane :deploy_internal do
  #   build_release
  #   supply(
  #     track: "internal",
  #     aab: "composeApp/build/outputs/bundle/release/composeApp-release.aab"
  #   )
  # end
end

# platform :ios do
#   desc "Build iOS app"
#   lane :build do
#     build_app(scheme: "iosApp")
#   end
# end
```

- [ ] **Step 14: Create README.md**

```markdown
# GameList

Cross-platform (Android + iOS) free-to-play game catalog app built with Kotlin Multiplatform + Compose Multiplatform.

Data provided by [FreeToGame.com](https://www.freetogame.com).

## Requirements

- JDK 17+
- Android Studio Hedgehog+ (or IntelliJ IDEA with KMP plugin)
- Xcode 15+ (for iOS builds)
- Gradle 8.14+

## Quick Start

```bash
# Verify setup
make setup

# Run on Android emulator
make run-android

# Run tests
make test

# Build debug APK
make build-debug
```

## Project Structure

```
composeApp/src/
├── commonMain/     Shared Kotlin code (UI, data, features)
├── androidMain/    Android-specific implementations
├── iosMain/        iOS-specific implementations
└── commonTest/     Shared tests
```

## Tech Stack

- **UI:** Compose Multiplatform + Material 3
- **Networking:** Ktor Client
- **Database:** SQLDelight
- **DI:** Koin
- **Navigation:** Voyager
- **Images:** Coil 3
- **Icons:** Phosphor Compose
- **Animations:** Compottie (Lottie for KMP)

## CI/CD

- **PR checks:** GitHub Actions runs build + tests on every PR
- **Release:** Tag with `v*` to trigger release build
- **Fastlane:** `bundle exec fastlane android test` / `build_debug` / `build_release`
```

- [ ] **Step 15: Commit**

```bash
git init
git add .
git commit -m "chore: scaffold KMP project with dependencies, Makefile, CI/CD, and Fastlane"
```

---

### Task 2: Domain Models & Database Layer

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/model/Game.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/model/UserData.kt`
- Create: `composeApp/src/commonMain/sqldelight/com/kami/gamelist/Game.sq`
- Create: `composeApp/src/commonMain/sqldelight/com/kami/gamelist/Favorite.sq`
- Create: `composeApp/src/commonMain/sqldelight/com/kami/gamelist/UserList.sq`
- Create: `composeApp/src/commonMain/sqldelight/com/kami/gamelist/UserListEntry.sq`
- Create: `composeApp/src/commonMain/sqldelight/com/kami/gamelist/SearchHistory.sq`
- Create: `composeApp/src/commonMain/sqldelight/com/kami/gamelist/CacheMeta.sq`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/database/DriverFactory.kt`
- Create: `composeApp/src/androidMain/kotlin/com/kami/gamelist/core/database/DriverFactory.android.kt`
- Create: `composeApp/src/iosMain/kotlin/com/kami/gamelist/core/database/DriverFactory.ios.kt`

**Interfaces:**
- Produces:
  - `Game`, `GameDetail`, `Screenshot`, `SystemRequirements` data classes
  - `UserList`, `ListType`, `UserListEntry`, `SearchHistory` data classes
  - `GameListDatabase` generated by SQLDelight with typed queries
  - `DriverFactory` expect/actual for platform-specific SQLite drivers

- [ ] **Step 1: Create domain models**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/model/Game.kt`:

```kotlin
package com.kami.gamelist.data.model

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

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/model/UserData.kt`:

```kotlin
package com.kami.gamelist.data.model

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

- [ ] **Step 2: Create SQLDelight schema files**

Create `composeApp/src/commonMain/sqldelight/com/kami/gamelist/Game.sq`:

```sql
CREATE TABLE GameEntity (
    id INTEGER NOT NULL PRIMARY KEY,
    title TEXT NOT NULL,
    thumbnail TEXT NOT NULL,
    short_description TEXT NOT NULL,
    game_url TEXT NOT NULL,
    genre TEXT NOT NULL,
    platform TEXT NOT NULL,
    publisher TEXT NOT NULL,
    developer TEXT NOT NULL,
    release_date TEXT NOT NULL,
    freetogame_profile_url TEXT NOT NULL,
    -- detail fields (nullable, populated when detail is fetched)
    description TEXT,
    status TEXT,
    min_req_os TEXT,
    min_req_processor TEXT,
    min_req_memory TEXT,
    min_req_graphics TEXT,
    min_req_storage TEXT
);

selectAll:
SELECT * FROM GameEntity ORDER BY title ASC;

selectById:
SELECT * FROM GameEntity WHERE id = ?;

selectByGenre:
SELECT * FROM GameEntity WHERE genre = ? ORDER BY title ASC;

selectByPlatform:
SELECT * FROM GameEntity WHERE platform LIKE '%' || ? || '%' ORDER BY title ASC;

selectByGenreAndPlatform:
SELECT * FROM GameEntity WHERE genre = ? AND platform LIKE '%' || ? || '%' ORDER BY title ASC;

searchByTitle:
SELECT * FROM GameEntity WHERE title LIKE '%' || ? || '%' ORDER BY title ASC;

upsert:
INSERT OR REPLACE INTO GameEntity(
    id, title, thumbnail, short_description, game_url, genre, platform,
    publisher, developer, release_date, freetogame_profile_url,
    description, status, min_req_os, min_req_processor, min_req_memory,
    min_req_graphics, min_req_storage
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

deleteAll:
DELETE FROM GameEntity;

selectAllGenres:
SELECT DISTINCT genre FROM GameEntity ORDER BY genre ASC;

selectAllPlatforms:
SELECT DISTINCT platform FROM GameEntity ORDER BY platform ASC;
```

Create `composeApp/src/commonMain/sqldelight/com/kami/gamelist/Screenshot.sq`:

```sql
CREATE TABLE ScreenshotEntity (
    id INTEGER NOT NULL PRIMARY KEY,
    game_id INTEGER NOT NULL,
    image TEXT NOT NULL,
    FOREIGN KEY(game_id) REFERENCES GameEntity(id) ON DELETE CASCADE
);

selectByGameId:
SELECT * FROM ScreenshotEntity WHERE game_id = ? ORDER BY id ASC;

insertScreenshot:
INSERT OR REPLACE INTO ScreenshotEntity(id, game_id, image) VALUES (?, ?, ?);

deleteByGameId:
DELETE FROM ScreenshotEntity WHERE game_id = ?;
```

Create `composeApp/src/commonMain/sqldelight/com/kami/gamelist/Favorite.sq`:

```sql
CREATE TABLE FavoriteEntity (
    game_id INTEGER NOT NULL PRIMARY KEY,
    added_at INTEGER NOT NULL,
    FOREIGN KEY(game_id) REFERENCES GameEntity(id) ON DELETE CASCADE
);

selectAll:
SELECT GameEntity.* FROM FavoriteEntity
INNER JOIN GameEntity ON FavoriteEntity.game_id = GameEntity.id
ORDER BY FavoriteEntity.added_at DESC;

isFavorite:
SELECT COUNT(*) FROM FavoriteEntity WHERE game_id = ?;

insert:
INSERT OR REPLACE INTO FavoriteEntity(game_id, added_at) VALUES (?, ?);

delete:
DELETE FROM FavoriteEntity WHERE game_id = ?;
```

Create `composeApp/src/commonMain/sqldelight/com/kami/gamelist/UserList.sq`:

```sql
CREATE TABLE UserListEntity (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

selectAll:
SELECT * FROM UserListEntity ORDER BY created_at ASC;

selectById:
SELECT * FROM UserListEntity WHERE id = ?;

insert:
INSERT INTO UserListEntity(name, type, created_at) VALUES (?, ?, ?);

update:
UPDATE UserListEntity SET name = ? WHERE id = ?;

delete:
DELETE FROM UserListEntity WHERE id = ?;

lastInsertId:
SELECT last_insert_rowid();
```

Create `composeApp/src/commonMain/sqldelight/com/kami/gamelist/UserListEntry.sq`:

```sql
CREATE TABLE UserListEntryEntity (
    list_id INTEGER NOT NULL,
    game_id INTEGER NOT NULL,
    added_at INTEGER NOT NULL,
    PRIMARY KEY(list_id, game_id),
    FOREIGN KEY(list_id) REFERENCES UserListEntity(id) ON DELETE CASCADE,
    FOREIGN KEY(game_id) REFERENCES GameEntity(id) ON DELETE CASCADE
);

selectByListId:
SELECT GameEntity.* FROM UserListEntryEntity
INNER JOIN GameEntity ON UserListEntryEntity.game_id = GameEntity.id
WHERE UserListEntryEntity.list_id = ?
ORDER BY UserListEntryEntity.added_at DESC;

isInList:
SELECT COUNT(*) FROM UserListEntryEntity WHERE list_id = ? AND game_id = ?;

insert:
INSERT OR REPLACE INTO UserListEntryEntity(list_id, game_id, added_at) VALUES (?, ?, ?);

delete:
DELETE FROM UserListEntryEntity WHERE list_id = ? AND game_id = ?;

deleteAllForList:
DELETE FROM UserListEntryEntity WHERE list_id = ?;

countByListId:
SELECT COUNT(*) FROM UserListEntryEntity WHERE list_id = ?;
```

Create `composeApp/src/commonMain/sqldelight/com/kami/gamelist/SearchHistory.sq`:

```sql
CREATE TABLE SearchHistoryEntity (
    query TEXT NOT NULL PRIMARY KEY,
    searched_at INTEGER NOT NULL
);

selectRecent:
SELECT * FROM SearchHistoryEntity ORDER BY searched_at DESC LIMIT 10;

insert:
INSERT OR REPLACE INTO SearchHistoryEntity(query, searched_at) VALUES (?, ?);

delete:
DELETE FROM SearchHistoryEntity WHERE query = ?;

deleteAll:
DELETE FROM SearchHistoryEntity;
```

Create `composeApp/src/commonMain/sqldelight/com/kami/gamelist/CacheMeta.sq`:

```sql
CREATE TABLE CacheMetaEntity (
    cache_key TEXT NOT NULL PRIMARY KEY,
    last_fetched_at INTEGER NOT NULL
);

getLastFetched:
SELECT last_fetched_at FROM CacheMetaEntity WHERE cache_key = ?;

upsert:
INSERT OR REPLACE INTO CacheMetaEntity(cache_key, last_fetched_at) VALUES (?, ?);

delete:
DELETE FROM CacheMetaEntity WHERE cache_key = ?;
```

- [ ] **Step 3: Create DriverFactory expect/actual**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/database/DriverFactory.kt`:

```kotlin
package com.kami.gamelist.core.database

import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory {
    fun createDriver(): SqlDriver
}
```

Create `composeApp/src/androidMain/kotlin/com/kami/gamelist/core/database/DriverFactory.android.kt`:

```kotlin
package com.kami.gamelist.core.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.kami.gamelist.db.GameListDatabase

actual class DriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(GameListDatabase.Schema, context, "gamelist.db")
    }
}
```

Create `composeApp/src/iosMain/kotlin/com/kami/gamelist/core/database/DriverFactory.ios.kt`:

```kotlin
package com.kami.gamelist.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.kami.gamelist.db.GameListDatabase

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(GameListDatabase.Schema, "gamelist.db")
    }
}
```

- [ ] **Step 4: Verify build compiles with schema**

Run:
```bash
./gradlew :composeApp:generateCommonMainGameListDatabaseInterface
```
Expected: BUILD SUCCESSFUL — SQLDelight generates `GameListDatabase` and all typed query interfaces.

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: add domain models and SQLDelight database schema"
```

---

### Task 3: Network Layer (Ktor + FreeToGame API)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/network/HttpClientFactory.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/remote/dto/GameDto.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/remote/dto/GameDetailDto.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/remote/FreeToGameApi.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/remote/DtoMapper.kt`
- Create: `composeApp/src/commonTest/kotlin/com/kami/gamelist/data/remote/DtoMapperTest.kt`
- Create: `composeApp/src/commonTest/kotlin/com/kami/gamelist/data/remote/FreeToGameApiTest.kt`

**Interfaces:**
- Consumes: `Game`, `GameDetail`, `Screenshot`, `SystemRequirements` from Task 2
- Produces:
  - `HttpClientFactory.create(): HttpClient` (with 10 req/s rate limiting)
  - `FreeToGameApi.getGames(platform?, category?, sortBy?): List<GameDto>`
  - `FreeToGameApi.getGamesByTags(tags: List<String>, platform?, sortBy?): List<GameDto>`
  - `FreeToGameApi.getGameById(id: Int): GameDetailDto`
  - `GameDto.toDomain(): Game`
  - `GameDetailDto.toDomain(): GameDetail`
  - `SortOption` enum: `RELEASE_DATE`, `ALPHABETICAL`, `RELEVANCE`, `POPULARITY`

- [ ] **Step 1: Write failing test for DTO mappers**

Create `composeApp/src/commonTest/kotlin/com/kami/gamelist/data/remote/DtoMapperTest.kt`:

```kotlin
package com.kami.gamelist.data.remote

import com.kami.gamelist.data.remote.dto.GameDetailDto
import com.kami.gamelist.data.remote.dto.GameDto
import com.kami.gamelist.data.remote.dto.MinimumSystemRequirementsDto
import com.kami.gamelist.data.remote.dto.ScreenshotDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DtoMapperTest {

    @Test
    fun gameDtoMapsToGameDomain() {
        val dto = GameDto(
            id = 1,
            title = "Genshin Impact",
            thumbnail = "https://example.com/thumb.jpg",
            shortDescription = "An open-world RPG",
            gameUrl = "https://example.com/game",
            genre = "MMORPG",
            platform = "PC (Windows)",
            publisher = "miHoYo",
            developer = "miHoYo",
            releaseDate = "2020-09-28",
            freetogameProfileUrl = "https://example.com/profile"
        )

        val game = dto.toDomain()

        assertEquals(1, game.id)
        assertEquals("Genshin Impact", game.title)
        assertEquals("MMORPG", game.genre)
        assertEquals("PC (Windows)", game.platform)
    }

    @Test
    fun gameDetailDtoMapsToGameDetailDomain() {
        val dto = GameDetailDto(
            id = 1,
            title = "Genshin Impact",
            thumbnail = "https://example.com/thumb.jpg",
            shortDescription = "An open-world RPG",
            gameUrl = "https://example.com/game",
            genre = "MMORPG",
            platform = "PC (Windows)",
            publisher = "miHoYo",
            developer = "miHoYo",
            releaseDate = "2020-09-28",
            freetogameProfileUrl = "https://example.com/profile",
            description = "Full description here",
            status = "Live",
            screenshots = listOf(
                ScreenshotDto(id = 1, image = "https://example.com/ss1.jpg")
            ),
            minimumSystemRequirements = MinimumSystemRequirementsDto(
                os = "Windows 7",
                processor = "Intel i5",
                memory = "8 GB",
                graphics = "GTX 1060",
                storage = "30 GB"
            )
        )

        val detail = dto.toDomain()

        assertEquals(1, detail.game.id)
        assertEquals("Full description here", detail.description)
        assertEquals("Live", detail.status)
        assertEquals(1, detail.screenshots.size)
        assertEquals("Windows 7", detail.minimumSystemRequirements?.os)
    }

    @Test
    fun gameDetailDtoWithNullRequirementsMapsCorrectly() {
        val dto = GameDetailDto(
            id = 2,
            title = "Browser Game",
            thumbnail = "https://example.com/thumb.jpg",
            shortDescription = "A browser game",
            gameUrl = "https://example.com/game",
            genre = "Strategy",
            platform = "Web Browser",
            publisher = "Pub",
            developer = "Dev",
            releaseDate = "2023-01-01",
            freetogameProfileUrl = "https://example.com/profile",
            description = "Browser game description",
            status = "Live",
            screenshots = emptyList(),
            minimumSystemRequirements = null
        )

        val detail = dto.toDomain()

        assertNull(detail.minimumSystemRequirements)
        assertEquals(0, detail.screenshots.size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :composeApp:compileTestKotlinJvm` (or the appropriate test compile task)
Expected: FAIL — `GameDto`, `GameDetailDto`, `toDomain()` don't exist yet.

- [ ] **Step 3: Create DTOs and mappers**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/remote/dto/GameDto.kt`:

```kotlin
package com.kami.gamelist.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameDto(
    val id: Int,
    val title: String,
    val thumbnail: String,
    @SerialName("short_description") val shortDescription: String,
    @SerialName("game_url") val gameUrl: String,
    val genre: String,
    val platform: String,
    val publisher: String,
    val developer: String,
    @SerialName("release_date") val releaseDate: String,
    @SerialName("freetogame_profile_url") val freetogameProfileUrl: String
)
```

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/remote/dto/GameDetailDto.kt`:

```kotlin
package com.kami.gamelist.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameDetailDto(
    val id: Int,
    val title: String,
    val thumbnail: String,
    @SerialName("short_description") val shortDescription: String,
    @SerialName("game_url") val gameUrl: String,
    val genre: String,
    val platform: String,
    val publisher: String,
    val developer: String,
    @SerialName("release_date") val releaseDate: String,
    @SerialName("freetogame_profile_url") val freetogameProfileUrl: String,
    val description: String,
    val status: String,
    val screenshots: List<ScreenshotDto>,
    @SerialName("minimum_system_requirements") val minimumSystemRequirements: MinimumSystemRequirementsDto?
)

@Serializable
data class ScreenshotDto(
    val id: Int,
    val image: String
)

@Serializable
data class MinimumSystemRequirementsDto(
    val os: String? = null,
    val processor: String? = null,
    val memory: String? = null,
    val graphics: String? = null,
    val storage: String? = null
)
```

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/remote/DtoMapper.kt`:

```kotlin
package com.kami.gamelist.data.remote

import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.model.GameDetail
import com.kami.gamelist.data.model.Screenshot
import com.kami.gamelist.data.model.SystemRequirements
import com.kami.gamelist.data.remote.dto.GameDetailDto
import com.kami.gamelist.data.remote.dto.GameDto
import com.kami.gamelist.data.remote.dto.MinimumSystemRequirementsDto
import com.kami.gamelist.data.remote.dto.ScreenshotDto

fun GameDto.toDomain(): Game = Game(
    id = id,
    title = title,
    thumbnail = thumbnail,
    shortDescription = shortDescription,
    gameUrl = gameUrl,
    genre = genre,
    platform = platform,
    publisher = publisher,
    developer = developer,
    releaseDate = releaseDate,
    freetogameProfileUrl = freetogameProfileUrl
)

fun GameDetailDto.toDomain(): GameDetail = GameDetail(
    game = Game(
        id = id,
        title = title,
        thumbnail = thumbnail,
        shortDescription = shortDescription,
        gameUrl = gameUrl,
        genre = genre,
        platform = platform,
        publisher = publisher,
        developer = developer,
        releaseDate = releaseDate,
        freetogameProfileUrl = freetogameProfileUrl
    ),
    description = description,
    status = status,
    screenshots = screenshots.map { it.toDomain() },
    minimumSystemRequirements = minimumSystemRequirements?.toDomain()
)

fun ScreenshotDto.toDomain(): Screenshot = Screenshot(
    id = id,
    image = image
)

fun MinimumSystemRequirementsDto.toDomain(): SystemRequirements = SystemRequirements(
    os = os,
    processor = processor,
    memory = memory,
    graphics = graphics,
    storage = storage
)
```

- [ ] **Step 4: Run mapper tests**

Run: `./gradlew :composeApp:allTests --tests "com.kami.gamelist.data.remote.DtoMapperTest"`
Expected: ALL PASS

- [ ] **Step 5: Create Ktor HttpClient factory**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/network/HttpClientFactory.kt`:

```kotlin
package com.kami.gamelist.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientFactory {

    private const val BASE_HOST = "www.freetogame.com"

    fun create(): HttpClient = HttpClient {
        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
                host = BASE_HOST
            }
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }

        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 2)
            exponentialDelay()
        }

        install(Logging) {
            level = LogLevel.HEADERS
        }
    }
}
```

- [ ] **Step 6: Write failing test for FreeToGameApi**

Create `composeApp/src/commonTest/kotlin/com/kami/gamelist/data/remote/FreeToGameApiTest.kt`:

```kotlin
package com.kami.gamelist.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class FreeToGameApiTest {

    private val gamesJson = """
        [
            {
                "id": 1,
                "title": "Genshin Impact",
                "thumbnail": "https://example.com/thumb.jpg",
                "short_description": "An open-world RPG",
                "game_url": "https://example.com/game",
                "genre": "MMORPG",
                "platform": "PC (Windows)",
                "publisher": "miHoYo",
                "developer": "miHoYo",
                "release_date": "2020-09-28",
                "freetogame_profile_url": "https://example.com/profile"
            }
        ]
    """.trimIndent()

    private val gameDetailJson = """
        {
            "id": 1,
            "title": "Genshin Impact",
            "thumbnail": "https://example.com/thumb.jpg",
            "short_description": "An open-world RPG",
            "game_url": "https://example.com/game",
            "genre": "MMORPG",
            "platform": "PC (Windows)",
            "publisher": "miHoYo",
            "developer": "miHoYo",
            "release_date": "2020-09-28",
            "freetogame_profile_url": "https://example.com/profile",
            "description": "Full description",
            "status": "Live",
            "screenshots": [{"id": 1, "image": "https://example.com/ss1.jpg"}],
            "minimum_system_requirements": {
                "os": "Windows 7",
                "processor": "Intel i5",
                "memory": "8 GB",
                "graphics": "GTX 1060",
                "storage": "30 GB"
            }
        }
    """.trimIndent()

    private fun createMockClient(responseBody: String): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond(
                        content = responseBody,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    @Test
    fun getGamesReturnsListOfGameDto() = runTest {
        val api = FreeToGameApi(createMockClient(gamesJson))
        val games = api.getGames()

        assertEquals(1, games.size)
        assertEquals("Genshin Impact", games[0].title)
        assertEquals("MMORPG", games[0].genre)
    }

    @Test
    fun getGamesWithFilters() = runTest {
        val api = FreeToGameApi(createMockClient(gamesJson))
        val games = api.getGames(
            platform = "windows",
            category = "mmorpg",
            sortBy = SortOption.POPULARITY
        )

        assertEquals(1, games.size)
    }

    @Test
    fun getGamesByTagsReturnsList() = runTest {
        val api = FreeToGameApi(createMockClient(gamesJson))
        val games = api.getGamesByTags(
            tags = listOf("mmorpg", "open-world"),
            platform = "windows"
        )

        assertEquals(1, games.size)
    }

    @Test
    fun getGameByIdReturnsGameDetailDto() = runTest {
        val api = FreeToGameApi(createMockClient(gameDetailJson))
        val detail = api.getGameById(1)

        assertEquals(1, detail.id)
        assertEquals("Full description", detail.description)
        assertEquals(1, detail.screenshots.size)
        assertEquals("Windows 7", detail.minimumSystemRequirements?.os)
    }
}
```

- [ ] **Step 7: Run test to verify it fails**

Run: `./gradlew :composeApp:allTests --tests "com.kami.gamelist.data.remote.FreeToGameApiTest"`
Expected: FAIL — `FreeToGameApi` doesn't exist yet.

- [ ] **Step 8: Implement FreeToGameApi**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/remote/SortOption.kt`:

```kotlin
package com.kami.gamelist.data.remote

enum class SortOption(val apiValue: String) {
    RELEASE_DATE("release-date"),
    ALPHABETICAL("alphabetical"),
    RELEVANCE("relevance"),
    POPULARITY("popularity")
}
```

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/remote/FreeToGameApi.kt`:

```kotlin
package com.kami.gamelist.data.remote

import com.kami.gamelist.data.remote.dto.GameDetailDto
import com.kami.gamelist.data.remote.dto.GameDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class FreeToGameApi(private val client: HttpClient) {

    // GET /api/games?platform={p}&category={c}&sort-by={s}
    // All params optional. platform: "windows"|"browser"|"all", category: lowercase genre
    suspend fun getGames(
        platform: String? = null,
        category: String? = null,
        sortBy: SortOption? = null
    ): List<GameDto> {
        return client.get("api/games") {
            platform?.let { parameter("platform", it) }
            category?.let { parameter("category", it.lowercase()) }
            sortBy?.let { parameter("sort-by", it.apiValue) }
        }.body()
    }

    // GET /api/filter?tag={tag1.tag2.tag3}&platform={p}&sort-by={s}
    // Tags are dot-separated. Allows filtering by multiple categories at once.
    suspend fun getGamesByTags(
        tags: List<String>,
        platform: String? = null,
        sortBy: SortOption? = null
    ): List<GameDto> {
        return client.get("api/filter") {
            parameter("tag", tags.joinToString(".") { it.lowercase() })
            platform?.let { parameter("platform", it) }
            sortBy?.let { parameter("sort-by", it.apiValue) }
        }.body()
    }

    // GET /api/game?id={id}
    suspend fun getGameById(id: Int): GameDetailDto {
        return client.get("api/game") {
            parameter("id", id)
        }.body()
    }
}
```

- [ ] **Step 9: Run all network tests**

Run: `./gradlew :composeApp:allTests --tests "com.kami.gamelist.data.remote.*"`
Expected: ALL PASS

- [ ] **Step 10: Commit**

```bash
git add .
git commit -m "feat: add Ktor network layer with FreeToGame API client and DTO mappers"
```

---

### Task 4: Local Data Sources & Cache Manager

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/local/GameLocalDataSource.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/local/UserLocalDataSource.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/local/CacheManager.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/local/EntityMapper.kt`
- Create: `composeApp/src/commonTest/kotlin/com/kami/gamelist/data/local/GameLocalDataSourceTest.kt`
- Create: `composeApp/src/commonTest/kotlin/com/kami/gamelist/data/local/UserLocalDataSourceTest.kt`

**Interfaces:**
- Consumes: `GameListDatabase` from Task 2, domain models from Task 2
- Produces:
  - `GameLocalDataSource.observeGames(): Flow<List<Game>>`, `.observeGameById(id: Int): Flow<GameDetail?>`, `.upsertGames(games: List<Game>)`, `.upsertGameDetail(detail: GameDetail)`
  - `UserLocalDataSource.observeFavorites(): Flow<List<Game>>`, `.toggleFavorite(gameId: Int)`, `.isFavorite(gameId: Int): Flow<Boolean>`, `.observeLists(): Flow<List<UserList>>`, `.createList(name, type)`, `.deleteList(id)`, `.addToList(listId, gameId)`, `.removeFromList(listId, gameId)`, `.observeGamesInList(listId): Flow<List<Game>>`, `.addSearchQuery(query)`, `.observeRecentSearches(): Flow<List<SearchHistory>>`, `.clearSearchHistory()`
  - `CacheManager.isStale(key: String, ttlMillis: Long): Boolean`, `.markFetched(key: String)`

- [ ] **Step 1: Create EntityMapper (SQLDelight entity ↔ domain)**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/local/EntityMapper.kt`:

```kotlin
package com.kami.gamelist.data.local

import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.model.GameDetail
import com.kami.gamelist.data.model.ListType
import com.kami.gamelist.data.model.Screenshot
import com.kami.gamelist.data.model.SystemRequirements
import com.kami.gamelist.data.model.UserList
import com.kami.gamelist.db.GameEntity
import com.kami.gamelist.db.ScreenshotEntity
import com.kami.gamelist.db.UserListEntity

fun GameEntity.toDomain(): Game = Game(
    id = id.toInt(),
    title = title,
    thumbnail = thumbnail,
    shortDescription = short_description,
    gameUrl = game_url,
    genre = genre,
    platform = platform,
    publisher = publisher,
    developer = developer,
    releaseDate = release_date,
    freetogameProfileUrl = freetogame_profile_url
)

fun GameEntity.toDetailDomain(screenshots: List<ScreenshotEntity>): GameDetail = GameDetail(
    game = toDomain(),
    description = description ?: "",
    status = status ?: "",
    screenshots = screenshots.map { it.toDomain() },
    minimumSystemRequirements = if (min_req_os != null || min_req_processor != null) {
        SystemRequirements(
            os = min_req_os,
            processor = min_req_processor,
            memory = min_req_memory,
            graphics = min_req_graphics,
            storage = min_req_storage
        )
    } else null
)

fun ScreenshotEntity.toDomain(): Screenshot = Screenshot(
    id = id.toInt(),
    image = image
)

fun UserListEntity.toDomain(): UserList = UserList(
    id = id,
    name = name,
    type = ListType.valueOf(type),
    createdAt = created_at
)
```

- [ ] **Step 2: Write failing tests for GameLocalDataSource**

Create `composeApp/src/commonTest/kotlin/com/kami/gamelist/data/local/GameLocalDataSourceTest.kt`:

```kotlin
package com.kami.gamelist.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.model.GameDetail
import com.kami.gamelist.data.model.Screenshot
import com.kami.gamelist.data.model.SystemRequirements
import com.kami.gamelist.db.GameListDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GameLocalDataSourceTest {

    private lateinit var database: GameListDatabase
    private lateinit var dataSource: GameLocalDataSource

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driver)
        database = GameListDatabase(driver)
        dataSource = GameLocalDataSource(database)
    }

    private fun sampleGame(id: Int = 1, genre: String = "MMORPG") = Game(
        id = id,
        title = "Game $id",
        thumbnail = "https://example.com/thumb$id.jpg",
        shortDescription = "Description $id",
        gameUrl = "https://example.com/game$id",
        genre = genre,
        platform = "PC (Windows)",
        publisher = "Publisher",
        developer = "Developer",
        releaseDate = "2023-01-01",
        freetogameProfileUrl = "https://example.com/profile$id"
    )

    @Test
    fun upsertAndObserveGames() = runTest {
        val games = listOf(sampleGame(1), sampleGame(2))
        dataSource.upsertGames(games)

        dataSource.observeGames().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeGameByIdReturnsDetail() = runTest {
        val detail = GameDetail(
            game = sampleGame(1),
            description = "Full description",
            status = "Live",
            screenshots = listOf(Screenshot(id = 100, image = "https://example.com/ss.jpg")),
            minimumSystemRequirements = SystemRequirements(
                os = "Windows 10",
                processor = "i5",
                memory = "8GB",
                graphics = "GTX 1060",
                storage = "30GB"
            )
        )
        dataSource.upsertGameDetail(detail)

        dataSource.observeGameById(1).test {
            val result = awaitItem()
            assertNotNull(result)
            assertEquals("Full description", result.description)
            assertEquals(1, result.screenshots.size)
            assertEquals("Windows 10", result.minimumSystemRequirements?.os)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchByTitleFiltersCorrectly() = runTest {
        dataSource.upsertGames(listOf(
            sampleGame(1).copy(title = "Genshin Impact"),
            sampleGame(2).copy(title = "Lost Ark"),
            sampleGame(3).copy(title = "Genshin Star")
        ))

        dataSource.searchByTitle("Genshin").test {
            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew :composeApp:allTests --tests "com.kami.gamelist.data.local.GameLocalDataSourceTest"`
Expected: FAIL — `GameLocalDataSource` doesn't exist.

Note: For `commonTest` to use JdbcSqliteDriver, add the test dependency to `build.gradle.kts`:
```kotlin
commonTest.dependencies {
    // ... existing
    implementation("app.cash.sqldelight:sqlite-driver:${libs.versions.sqldelight.get()}")
}
```

- [ ] **Step 4: Implement GameLocalDataSource**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/local/GameLocalDataSource.kt`:

```kotlin
package com.kami.gamelist.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.model.GameDetail
import com.kami.gamelist.db.GameListDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class GameLocalDataSource(private val database: GameListDatabase) {

    private val gameQueries = database.gameQueries
    private val screenshotQueries = database.screenshotQueries

    fun observeGames(): Flow<List<Game>> =
        gameQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }

    fun observeGamesByGenre(genre: String): Flow<List<Game>> =
        gameQueries.selectByGenre(genre)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }

    fun observeGamesByPlatform(platform: String): Flow<List<Game>> =
        gameQueries.selectByPlatform(platform)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }

    fun observeGamesByGenreAndPlatform(genre: String, platform: String): Flow<List<Game>> =
        gameQueries.selectByGenreAndPlatform(genre, platform)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }

    fun observeGameById(id: Int): Flow<GameDetail?> {
        val gameFlow = gameQueries.selectById(id.toLong())
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
        val screenshotsFlow = screenshotQueries.selectByGameId(id.toLong())
            .asFlow()
            .mapToList(Dispatchers.IO)

        return combine(gameFlow, screenshotsFlow) { entity, screenshots ->
            entity?.toDetailDomain(screenshots)
        }
    }

    fun searchByTitle(query: String): Flow<List<Game>> =
        gameQueries.searchByTitle(query)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }

    fun observeGenres(): Flow<List<String>> =
        gameQueries.selectAllGenres()
            .asFlow()
            .mapToList(Dispatchers.IO)

    fun observePlatforms(): Flow<List<String>> =
        gameQueries.selectAllPlatforms()
            .asFlow()
            .mapToList(Dispatchers.IO)

    fun upsertGames(games: List<Game>) {
        database.transaction {
            games.forEach { game ->
                gameQueries.upsert(
                    id = game.id.toLong(),
                    title = game.title,
                    thumbnail = game.thumbnail,
                    short_description = game.shortDescription,
                    game_url = game.gameUrl,
                    genre = game.genre,
                    platform = game.platform,
                    publisher = game.publisher,
                    developer = game.developer,
                    release_date = game.releaseDate,
                    freetogame_profile_url = game.freetogameProfileUrl,
                    description = null,
                    status = null,
                    min_req_os = null,
                    min_req_processor = null,
                    min_req_memory = null,
                    min_req_graphics = null,
                    min_req_storage = null
                )
            }
        }
    }

    fun upsertGameDetail(detail: GameDetail) {
        database.transaction {
            val game = detail.game
            val reqs = detail.minimumSystemRequirements
            gameQueries.upsert(
                id = game.id.toLong(),
                title = game.title,
                thumbnail = game.thumbnail,
                short_description = game.shortDescription,
                game_url = game.gameUrl,
                genre = game.genre,
                platform = game.platform,
                publisher = game.publisher,
                developer = game.developer,
                release_date = game.releaseDate,
                freetogame_profile_url = game.freetogameProfileUrl,
                description = detail.description,
                status = detail.status,
                min_req_os = reqs?.os,
                min_req_processor = reqs?.processor,
                min_req_memory = reqs?.memory,
                min_req_graphics = reqs?.graphics,
                min_req_storage = reqs?.storage
            )
            screenshotQueries.deleteByGameId(game.id.toLong())
            detail.screenshots.forEach { ss ->
                screenshotQueries.insertScreenshot(
                    id = ss.id.toLong(),
                    game_id = game.id.toLong(),
                    image = ss.image
                )
            }
        }
    }
}
```

- [ ] **Step 5: Run GameLocalDataSource tests**

Run: `./gradlew :composeApp:allTests --tests "com.kami.gamelist.data.local.GameLocalDataSourceTest"`
Expected: ALL PASS

- [ ] **Step 6: Write failing tests for UserLocalDataSource**

Create `composeApp/src/commonTest/kotlin/com/kami/gamelist/data/local/UserLocalDataSourceTest.kt`:

```kotlin
package com.kami.gamelist.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.model.ListType
import com.kami.gamelist.db.GameListDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserLocalDataSourceTest {

    private lateinit var database: GameListDatabase
    private lateinit var gameDataSource: GameLocalDataSource
    private lateinit var userDataSource: UserLocalDataSource

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driver)
        database = GameListDatabase(driver)
        gameDataSource = GameLocalDataSource(database)
        userDataSource = UserLocalDataSource(database)
    }

    private fun sampleGame(id: Int = 1) = Game(
        id = id,
        title = "Game $id",
        thumbnail = "https://example.com/thumb.jpg",
        shortDescription = "Desc",
        gameUrl = "https://example.com",
        genre = "RPG",
        platform = "PC (Windows)",
        publisher = "Pub",
        developer = "Dev",
        releaseDate = "2023-01-01",
        freetogameProfileUrl = "https://example.com/profile"
    )

    @Test
    fun toggleFavoriteAddsAndRemoves() = runTest {
        gameDataSource.upsertGames(listOf(sampleGame(1)))

        userDataSource.toggleFavorite(1)
        userDataSource.isFavorite(1).test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        userDataSource.toggleFavorite(1)
        userDataSource.isFavorite(1).test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeFavoritesReturnsGames() = runTest {
        gameDataSource.upsertGames(listOf(sampleGame(1), sampleGame(2)))
        userDataSource.toggleFavorite(1)
        userDataSource.toggleFavorite(2)

        userDataSource.observeFavorites().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun createAndObserveLists() = runTest {
        userDataSource.createList("Jogando", ListType.PLAYING)
        userDataSource.createList("Quero Jogar", ListType.WANT_TO_PLAY)

        userDataSource.observeLists().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("Jogando", result[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addAndRemoveFromList() = runTest {
        gameDataSource.upsertGames(listOf(sampleGame(1)))
        val listId = userDataSource.createList("Jogando", ListType.PLAYING)

        userDataSource.addToList(listId, 1)
        userDataSource.observeGamesInList(listId).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Game 1", result[0].title)
            cancelAndIgnoreRemainingEvents()
        }

        userDataSource.removeFromList(listId, 1)
        userDataSource.observeGamesInList(listId).test {
            val result = awaitItem()
            assertEquals(0, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchHistoryStoresAndRetrievesRecent() = runTest {
        userDataSource.addSearchQuery("genshin")
        userDataSource.addSearchQuery("lost ark")

        userDataSource.observeRecentSearches().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("lost ark", result[0].query)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 7: Implement UserLocalDataSource**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/local/UserLocalDataSource.kt`:

```kotlin
package com.kami.gamelist.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.model.ListType
import com.kami.gamelist.data.model.SearchHistory
import com.kami.gamelist.data.model.UserList
import com.kami.gamelist.db.GameListDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class UserLocalDataSource(private val database: GameListDatabase) {

    private val favoriteQueries = database.favoriteQueries
    private val userListQueries = database.userListQueries
    private val userListEntryQueries = database.userListEntryQueries
    private val searchHistoryQueries = database.searchHistoryQueries

    fun observeFavorites(): Flow<List<Game>> =
        favoriteQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }

    fun isFavorite(gameId: Int): Flow<Boolean> =
        favoriteQueries.isFavorite(gameId.toLong())
            .asFlow()
            .mapToOne(Dispatchers.IO)
            .map { it > 0 }

    fun toggleFavorite(gameId: Int) {
        val count = favoriteQueries.isFavorite(gameId.toLong()).executeAsOne()
        if (count > 0) {
            favoriteQueries.delete(gameId.toLong())
        } else {
            favoriteQueries.insert(gameId.toLong(), Clock.System.now().toEpochMilliseconds())
        }
    }

    fun observeLists(): Flow<List<UserList>> =
        userListQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }

    fun createList(name: String, type: ListType): Long {
        userListQueries.insert(name, type.name, Clock.System.now().toEpochMilliseconds())
        return userListQueries.lastInsertId().executeAsOne()
    }

    fun updateListName(id: Long, name: String) {
        userListQueries.update(name, id)
    }

    fun deleteList(id: Long) {
        database.transaction {
            userListEntryQueries.deleteAllForList(id)
            userListQueries.delete(id)
        }
    }

    fun addToList(listId: Long, gameId: Int) {
        userListEntryQueries.insert(listId, gameId.toLong(), Clock.System.now().toEpochMilliseconds())
    }

    fun removeFromList(listId: Long, gameId: Int) {
        userListEntryQueries.delete(listId, gameId.toLong())
    }

    fun observeGamesInList(listId: Long): Flow<List<Game>> =
        userListEntryQueries.selectByListId(listId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }

    fun isInList(listId: Long, gameId: Int): Flow<Boolean> =
        userListEntryQueries.isInList(listId, gameId.toLong())
            .asFlow()
            .mapToOne(Dispatchers.IO)
            .map { it > 0 }

    fun listGameCount(listId: Long): Flow<Long> =
        userListEntryQueries.countByListId(listId)
            .asFlow()
            .mapToOne(Dispatchers.IO)

    fun addSearchQuery(query: String) {
        searchHistoryQueries.insert(query, Clock.System.now().toEpochMilliseconds())
    }

    fun observeRecentSearches(): Flow<List<SearchHistory>> =
        searchHistoryQueries.selectRecent()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { SearchHistory(query = it.query, searchedAt = it.searched_at) }
            }

    fun deleteSearchQuery(query: String) {
        searchHistoryQueries.delete(query)
    }

    fun clearSearchHistory() {
        searchHistoryQueries.deleteAll()
    }
}
```

Note: Add `kotlinx-datetime` to the version catalog and dependencies:

In `libs.versions.toml`, add:
```toml
[versions]
datetime = "0.6.2"

[libraries]
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "datetime" }
```

In `composeApp/build.gradle.kts` `commonMain.dependencies`:
```kotlin
implementation(libs.kotlinx.datetime)
```

- [ ] **Step 8: Implement CacheManager**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/local/CacheManager.kt`:

```kotlin
package com.kami.gamelist.data.local

import com.kami.gamelist.db.GameListDatabase
import kotlinx.datetime.Clock

class CacheManager(private val database: GameListDatabase) {

    companion object {
        const val GAMES_LIST_KEY = "games_list"
        const val GAMES_LIST_TTL = 3_600_000L       // 1 hour
        const val GAME_DETAIL_TTL = 21_600_000L      // 6 hours

        fun gameDetailKey(id: Int) = "game_detail_$id"
    }

    private val queries = database.cacheMetaQueries

    fun isStale(key: String, ttlMillis: Long): Boolean {
        val lastFetched = queries.getLastFetched(key).executeAsOneOrNull() ?: return true
        val elapsed = Clock.System.now().toEpochMilliseconds() - lastFetched
        return elapsed > ttlMillis
    }

    fun markFetched(key: String) {
        queries.upsert(key, Clock.System.now().toEpochMilliseconds())
    }
}
```

- [ ] **Step 9: Run all local data source tests**

Run: `./gradlew :composeApp:allTests --tests "com.kami.gamelist.data.local.*"`
Expected: ALL PASS

- [ ] **Step 10: Commit**

```bash
git add .
git commit -m "feat: add local data sources with SQLDelight, cache manager, and user data operations"
```

---

### Task 5: Repositories & Connectivity Monitor

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/network/ConnectivityMonitor.kt`
- Create: `composeApp/src/androidMain/kotlin/com/kami/gamelist/core/network/ConnectivityMonitor.android.kt`
- Create: `composeApp/src/iosMain/kotlin/com/kami/gamelist/core/network/ConnectivityMonitor.ios.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/repository/GameRepository.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/repository/UserRepository.kt`
- Create: `composeApp/src/commonTest/kotlin/com/kami/gamelist/data/repository/GameRepositoryTest.kt`
- Create: `composeApp/src/commonTest/kotlin/com/kami/gamelist/data/repository/UserRepositoryTest.kt`

**Interfaces:**
- Consumes: `GameLocalDataSource`, `UserLocalDataSource`, `CacheManager` from Task 4. `FreeToGameApi` from Task 3.
- Produces:
  - `ConnectivityMonitor.isOnline: Flow<Boolean>`
  - `GameRepository.observeGames(genre?, platform?, sortBy?): Flow<List<Game>>`, `.observeGameDetail(id): Flow<GameDetail?>`, `.searchGames(query): Flow<List<Game>>`, `.refreshGames()`, `.observeGenres(): Flow<List<String>>`, `.observePlatforms(): Flow<List<String>>`
  - `UserRepository.observeFavorites(): Flow<List<Game>>`, `.toggleFavorite(gameId)`, `.isFavorite(gameId): Flow<Boolean>`, `.observeLists(): Flow<List<UserList>>`, `.createList(name, type): Long`, `.deleteList(id)`, `.addToList(listId, gameId)`, `.removeFromList(listId, gameId)`, `.observeGamesInList(listId): Flow<List<Game>>`, `.addSearchQuery(query)`, `.observeRecentSearches(): Flow<List<SearchHistory>>`, `.clearSearchHistory()`

- [ ] **Step 1: Create ConnectivityMonitor expect/actual**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/network/ConnectivityMonitor.kt`:

```kotlin
package com.kami.gamelist.core.network

import kotlinx.coroutines.flow.Flow

expect class ConnectivityMonitor {
    val isOnline: Flow<Boolean>
}
```

Create `composeApp/src/androidMain/kotlin/com/kami/gamelist/core/network/ConnectivityMonitor.android.kt`:

```kotlin
package com.kami.gamelist.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual class ConnectivityMonitor(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    actual val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(false) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        val activeNetwork = connectivityManager.activeNetwork
        val activeCaps = connectivityManager.getNetworkCapabilities(activeNetwork)
        trySend(activeCaps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
}
```

Create `composeApp/src/iosMain/kotlin/com/kami/gamelist/core/network/ConnectivityMonitor.ios.kt`:

```kotlin
package com.kami.gamelist.core.network

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_get_status
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue

actual class ConnectivityMonitor {
    actual val isOnline: Flow<Boolean> = callbackFlow {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_set_update_handler(monitor) { path ->
            trySend(nw_path_get_status(path) == nw_path_status_satisfied)
        }
        nw_path_monitor_start(monitor)
        awaitClose { nw_path_monitor_cancel(monitor) }
    }
}
```

- [ ] **Step 2: Write failing tests for GameRepository**

Create `composeApp/src/commonTest/kotlin/com/kami/gamelist/data/repository/GameRepositoryTest.kt`:

```kotlin
package com.kami.gamelist.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.kami.gamelist.data.local.CacheManager
import com.kami.gamelist.data.local.GameLocalDataSource
import com.kami.gamelist.data.remote.FreeToGameApi
import com.kami.gamelist.data.remote.dto.GameDto
import com.kami.gamelist.db.GameListDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRepositoryTest {

    private lateinit var database: GameListDatabase
    private lateinit var localDataSource: GameLocalDataSource
    private lateinit var cacheManager: CacheManager
    private lateinit var repository: GameRepository

    private val sampleDtos = listOf(
        GameDto(
            id = 1, title = "Game 1", thumbnail = "https://img.com/1.jpg",
            shortDescription = "Desc 1", gameUrl = "https://game.com/1",
            genre = "MMORPG", platform = "PC (Windows)", publisher = "Pub",
            developer = "Dev", releaseDate = "2023-01-01",
            freetogameProfileUrl = "https://ftg.com/1"
        ),
        GameDto(
            id = 2, title = "Game 2", thumbnail = "https://img.com/2.jpg",
            shortDescription = "Desc 2", gameUrl = "https://game.com/2",
            genre = "Shooter", platform = "Web Browser", publisher = "Pub",
            developer = "Dev", releaseDate = "2023-02-01",
            freetogameProfileUrl = "https://ftg.com/2"
        )
    )

    private fun createApiWithResponse(json: String): FreeToGameApi {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(
                        content = json,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return FreeToGameApi(client)
    }

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driver)
        database = GameListDatabase(driver)
        localDataSource = GameLocalDataSource(database)
        cacheManager = CacheManager(database)
    }

    @Test
    fun observeGamesReturnsCachedDataThenSyncsFromApi() = runTest {
        val api = createApiWithResponse(Json.encodeToString(sampleDtos))
        repository = GameRepository(api, localDataSource, cacheManager)

        repository.observeGames().test {
            val first = awaitItem()
            // Initially empty (no cache)
            assertTrue(first.isEmpty())

            // After sync, should have 2 games
            val second = awaitItem()
            assertEquals(2, second.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refreshGamesUpdatesLocalData() = runTest {
        val api = createApiWithResponse(Json.encodeToString(sampleDtos))
        repository = GameRepository(api, localDataSource, cacheManager)

        repository.refreshGames()

        localDataSource.observeGames().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :composeApp:allTests --tests "com.kami.gamelist.data.repository.GameRepositoryTest"`
Expected: FAIL — `GameRepository` doesn't exist.

- [ ] **Step 4: Implement GameRepository**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/repository/GameRepository.kt`:

```kotlin
package com.kami.gamelist.data.repository

import com.kami.gamelist.data.local.CacheManager
import com.kami.gamelist.data.local.GameLocalDataSource
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.model.GameDetail
import com.kami.gamelist.data.remote.FreeToGameApi
import com.kami.gamelist.data.remote.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

class GameRepository(
    private val api: FreeToGameApi,
    private val localDataSource: GameLocalDataSource,
    private val cacheManager: CacheManager
) {

    fun observeGames(genre: String? = null, platform: String? = null): Flow<List<Game>> {
        val flow = when {
            genre != null && platform != null -> localDataSource.observeGamesByGenreAndPlatform(genre, platform)
            genre != null -> localDataSource.observeGamesByGenre(genre)
            platform != null -> localDataSource.observeGamesByPlatform(platform)
            else -> localDataSource.observeGames()
        }

        return flow.onStart {
            if (cacheManager.isStale(CacheManager.GAMES_LIST_KEY, CacheManager.GAMES_LIST_TTL)) {
                syncGames()
            }
        }
    }

    fun observeGameDetail(id: Int): Flow<GameDetail?> {
        return localDataSource.observeGameById(id).onStart {
            val key = CacheManager.gameDetailKey(id)
            if (cacheManager.isStale(key, CacheManager.GAME_DETAIL_TTL)) {
                syncGameDetail(id)
            }
        }
    }

    fun searchGames(query: String): Flow<List<Game>> =
        localDataSource.searchByTitle(query)

    fun observeGenres(): Flow<List<String>> =
        localDataSource.observeGenres()

    fun observePlatforms(): Flow<List<String>> =
        localDataSource.observePlatforms()

    suspend fun refreshGames() {
        syncGames()
    }

    private suspend fun syncGames() {
        try {
            val dtos = api.getGames()
            val games = dtos.map { it.toDomain() }
            localDataSource.upsertGames(games)
            cacheManager.markFetched(CacheManager.GAMES_LIST_KEY)
        } catch (_: Exception) {
            // Silently fail — UI will show cached data or empty state
        }
    }

    private suspend fun syncGameDetail(id: Int) {
        try {
            val dto = api.getGameById(id)
            val detail = dto.toDomain()
            localDataSource.upsertGameDetail(detail)
            cacheManager.markFetched(CacheManager.gameDetailKey(id))
        } catch (_: Exception) {
            // Silently fail — UI will show cached data or empty state
        }
    }
}
```

- [ ] **Step 5: Run GameRepository tests**

Run: `./gradlew :composeApp:allTests --tests "com.kami.gamelist.data.repository.GameRepositoryTest"`
Expected: ALL PASS

- [ ] **Step 6: Implement UserRepository**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/data/repository/UserRepository.kt`:

```kotlin
package com.kami.gamelist.data.repository

import com.kami.gamelist.data.local.UserLocalDataSource
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.model.ListType
import com.kami.gamelist.data.model.SearchHistory
import com.kami.gamelist.data.model.UserList
import kotlinx.coroutines.flow.Flow

class UserRepository(private val localDataSource: UserLocalDataSource) {

    fun observeFavorites(): Flow<List<Game>> =
        localDataSource.observeFavorites()

    fun isFavorite(gameId: Int): Flow<Boolean> =
        localDataSource.isFavorite(gameId)

    fun toggleFavorite(gameId: Int) {
        localDataSource.toggleFavorite(gameId)
    }

    fun observeLists(): Flow<List<UserList>> =
        localDataSource.observeLists()

    fun createList(name: String, type: ListType): Long =
        localDataSource.createList(name, type)

    fun updateListName(id: Long, name: String) {
        localDataSource.updateListName(id, name)
    }

    fun deleteList(id: Long) {
        localDataSource.deleteList(id)
    }

    fun addToList(listId: Long, gameId: Int) {
        localDataSource.addToList(listId, gameId)
    }

    fun removeFromList(listId: Long, gameId: Int) {
        localDataSource.removeFromList(listId, gameId)
    }

    fun observeGamesInList(listId: Long): Flow<List<Game>> =
        localDataSource.observeGamesInList(listId)

    fun isInList(listId: Long, gameId: Int): Flow<Boolean> =
        localDataSource.isInList(listId, gameId)

    fun listGameCount(listId: Long): Flow<Long> =
        localDataSource.listGameCount(listId)

    fun addSearchQuery(query: String) {
        localDataSource.addSearchQuery(query)
    }

    fun observeRecentSearches(): Flow<List<SearchHistory>> =
        localDataSource.observeRecentSearches()

    fun deleteSearchQuery(query: String) {
        localDataSource.deleteSearchQuery(query)
    }

    fun clearSearchHistory() {
        localDataSource.clearSearchHistory()
    }
}
```

- [ ] **Step 7: Run all repository tests**

Run: `./gradlew :composeApp:allTests --tests "com.kami.gamelist.data.repository.*"`
Expected: ALL PASS

- [ ] **Step 8: Commit**

```bash
git add .
git commit -m "feat: add game and user repositories with offline-first sync and connectivity monitor"
```

---

### Task 6: DI Setup (Koin)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/di/NetworkModule.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/di/DatabaseModule.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/di/RepositoryModule.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/di/FeatureModule.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/di/AppModule.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/kami/gamelist/GameListApplication.kt`

**Interfaces:**
- Consumes: All classes from Tasks 2-5
- Produces: `appModule()` — a Koin module list that provides all dependencies for the app

- [ ] **Step 1: Create Koin modules**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/di/NetworkModule.kt`:

```kotlin
package com.kami.gamelist.core.di

import com.kami.gamelist.core.network.HttpClientFactory
import com.kami.gamelist.data.remote.FreeToGameApi
import org.koin.dsl.module

val networkModule = module {
    single { HttpClientFactory.create() }
    single { FreeToGameApi(get()) }
}
```

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/di/DatabaseModule.kt`:

```kotlin
package com.kami.gamelist.core.di

import com.kami.gamelist.core.database.DriverFactory
import com.kami.gamelist.data.local.CacheManager
import com.kami.gamelist.data.local.GameLocalDataSource
import com.kami.gamelist.data.local.UserLocalDataSource
import com.kami.gamelist.db.GameListDatabase
import org.koin.dsl.module

val databaseModule = module {
    single { get<DriverFactory>().createDriver() }
    single { GameListDatabase(get()) }
    single { GameLocalDataSource(get()) }
    single { UserLocalDataSource(get()) }
    single { CacheManager(get()) }
}
```

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/di/RepositoryModule.kt`:

```kotlin
package com.kami.gamelist.core.di

import com.kami.gamelist.data.repository.GameRepository
import com.kami.gamelist.data.repository.UserRepository
import org.koin.dsl.module

val repositoryModule = module {
    single { GameRepository(get(), get(), get()) }
    single { UserRepository(get()) }
}
```

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/di/FeatureModule.kt`:

```kotlin
package com.kami.gamelist.core.di

import com.kami.gamelist.feature.detail.GameDetailScreenModel
import com.kami.gamelist.feature.favorites.FavoritesScreenModel
import com.kami.gamelist.feature.home.HomeScreenModel
import com.kami.gamelist.feature.lists.ListDetailScreenModel
import com.kami.gamelist.feature.lists.ListsScreenModel
import com.kami.gamelist.feature.search.SearchScreenModel
import org.koin.dsl.module

val featureModule = module {
    factory { HomeScreenModel(get(), get()) }
    factory { SearchScreenModel(get(), get()) }
    factory { params -> GameDetailScreenModel(params.get(), get(), get()) }
    factory { FavoritesScreenModel(get()) }
    factory { ListsScreenModel(get()) }
    factory { params -> ListDetailScreenModel(params.get(), get()) }
}
```

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/di/AppModule.kt`:

```kotlin
package com.kami.gamelist.core.di

fun appModules() = listOf(
    networkModule,
    databaseModule,
    repositoryModule,
    featureModule
)
```

- [ ] **Step 2: Update Android Application with Koin**

Update `composeApp/src/androidMain/kotlin/com/kami/gamelist/GameListApplication.kt`:

```kotlin
package com.kami.gamelist

import android.app.Application
import com.kami.gamelist.core.database.DriverFactory
import com.kami.gamelist.core.di.appModules
import com.kami.gamelist.core.network.ConnectivityMonitor
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class GameListApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@GameListApplication)
            modules(appModules())
            modules(module {
                single { DriverFactory(get()) }
                single { ConnectivityMonitor(get()) }
            })
        }
    }
}
```

- [ ] **Step 3: Verify build**

Run: `./gradlew :composeApp:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

Note: FeatureModule references ScreenModels that don't exist yet. To make this compile, either create stub ScreenModel classes or comment out FeatureModule contents and uncomment as each feature is implemented. The pragmatic approach: create empty ScreenModel stubs that compile, then fill them in during feature tasks.

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "feat: add Koin dependency injection modules"
```

---

### Task 7: Design System & Shared UI Components

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/theme/Color.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/theme/Type.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/theme/Shape.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/theme/Theme.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/UiState.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/components/ShimmerEffect.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/components/GameCard.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/components/GameGrid.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/components/FilterChipRow.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/components/SearchBar.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/components/ScreenshotCarousel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/components/FavoriteButton.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/components/ListSelector.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/components/EmptyState.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/components/ErrorState.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/components/OfflineBanner.kt`

**Interfaces:**
- Produces: All shared Composable components used by feature screens. `GameListTheme` wrapper. `UiState<T>` sealed interface.

- [ ] **Step 1: Create color scheme**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/theme/Color.kt`:

```kotlin
package com.kami.gamelist.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFBB86FC)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Cyan80 = Color(0xFF80DEEA)

val Purple40 = Color(0xFF6200EE)
val PurpleGrey40 = Color(0xFF625B71)
val Cyan40 = Color(0xFF00838F)

val DarkBackground = Color(0xFF0F0F1A)
val DarkSurface = Color(0xFF1A1A2E)
val DarkSurfaceVariant = Color(0xFF252540)

val AccentCyan = Color(0xFF00E5FF)
val AccentPurple = Color(0xFFBB86FC)

val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    secondary = AccentPurple,
    tertiary = Cyan80,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0B0C0)
)

val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = Cyan40,
    tertiary = PurpleGrey40,
    background = Color(0xFFF8F8FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEEEF4),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1A2E),
    onSurface = Color(0xFF1A1A2E),
    onSurfaceVariant = Color(0xFF555570)
)
```

- [ ] **Step 2: Create typography**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/theme/Type.kt`:

```kotlin
package com.kami.gamelist.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val GameListTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)
```

- [ ] **Step 3: Create shapes**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/theme/Shape.kt`:

```kotlin
package com.kami.gamelist.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val GameListShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)
```

- [ ] **Step 4: Create Theme composable**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/theme/Theme.kt`:

```kotlin
package com.kami.gamelist.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun GameListTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GameListTypography,
        shapes = GameListShapes,
        content = content
    )
}
```

- [ ] **Step 5: Create UiState**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/UiState.kt`:

```kotlin
package com.kami.gamelist.core.ui

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
```

- [ ] **Step 6: Create ShimmerEffect**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/components/ShimmerEffect.kt`:

```kotlin
package com.kami.gamelist.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    return this.background(brush)
}
```

- [ ] **Step 7: Create GameCard with skeleton**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/components/GameCard.kt`:

```kotlin
package com.kami.gamelist.core.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kami.gamelist.data.model.Game

@Composable
fun GameCard(
    game: Game,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Column {
            AsyncImage(
                model = game.thumbnail,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.small)
            )

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = game.genre,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = game.platform,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun GameCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .shimmerEffect()
            )

            Column(modifier = Modifier.padding(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(16.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(12.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .shimmerEffect()
                )
            }
        }
    }
}
```

- [ ] **Step 8: Create GameGrid**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/components/GameGrid.kt`:

```kotlin
package com.kami.gamelist.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kami.gamelist.data.model.Game

@Composable
fun GameGrid(
    games: List<Game>,
    onGameClick: (Game) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(games, key = { it.id }) { game ->
            GameCard(game = game, onClick = { onGameClick(game) })
        }
    }
}

@Composable
fun GameGridSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 6
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(itemCount) {
            GameCardSkeleton()
        }
    }
}
```

- [ ] **Step 9: Create FilterChipRow**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/components/FilterChipRow.kt`:

```kotlin
package com.kami.gamelist.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FilterChipRow(
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    allLabel: String = "All"
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedOption == null,
            onClick = { onOptionSelected(null) },
            label = { Text(allLabel) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        options.forEach { option ->
            FilterChip(
                selected = option == selectedOption,
                onClick = {
                    onOptionSelected(if (option == selectedOption) null else option)
                },
                label = { Text(option) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}
```

- [ ] **Step 10: Create remaining shared components**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/components/FavoriteButton.kt`:

```kotlin
package com.kami.gamelist.core.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder

@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = if (isFavorite) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurfaceVariant
) {
    val scale by animateFloatAsState(
        targetValue = if (isFavorite) 1.0f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "favorite_scale"
    )

    var wasClicked by remember { mutableStateOf(false) }
    val clickScale by animateFloatAsState(
        targetValue = if (wasClicked) 1.3f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        finishedListener = { wasClicked = false },
        label = "click_scale"
    )

    Icon(
        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
        tint = tint,
        modifier = modifier
            .size(28.dp)
            .scale(scale * clickScale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                wasClicked = true
                onToggle()
            }
    )
}
```

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/components/EmptyState.kt`:

```kotlin
package com.kami.gamelist.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}
```

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/components/ErrorState.kt`:

```kotlin
package com.kami.gamelist.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
```

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/components/OfflineBanner.kt`:

```kotlin
package com.kami.gamelist.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OfflineBanner(
    isOffline: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOffline,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Offline — showing cached data",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
```

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/components/ScreenshotCarousel.kt`:

```kotlin
package com.kami.gamelist.core.ui.components

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import coil3.compose.AsyncImage
import com.kami.gamelist.data.model.Screenshot

@Composable
fun ScreenshotCarousel(
    screenshots: List<Screenshot>,
    modifier: Modifier = Modifier
) {
    if (screenshots.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { screenshots.size })

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth(),
        pageSpacing = 8.dp
    ) { page ->
        AsyncImage(
            model = screenshots[page].image,
            contentDescription = "Screenshot ${page + 1}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.medium)
        )
    }
}
```

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/components/ListSelector.kt`:

```kotlin
package com.kami.gamelist.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kami.gamelist.data.model.UserList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListSelector(
    lists: List<UserList>,
    listsContainingGame: Set<Long>,
    onListToggle: (UserList) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Add to list",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            lists.forEach { list ->
                val isInList = listsContainingGame.contains(list.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onListToggle(list) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = list.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )

                    if (isInList) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "In list",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
```

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/core/ui/components/SearchBar.kt`:

```kotlin
package com.kami.gamelist.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kami.gamelist.data.model.SearchHistory

@Composable
fun GameSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    recentSearches: List<SearchHistory>,
    showHistory: Boolean,
    onHistoryItemClick: (String) -> Unit,
    onHistoryItemDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Search games...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        AnimatedVisibility(visible = showHistory && recentSearches.isNotEmpty()) {
            Column {
                recentSearches.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onHistoryItemClick(item.query) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = item.query,
                            modifier = Modifier.weight(1f).padding(start = 12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        IconButton(onClick = { onHistoryItemDelete(item.query) }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 11: Verify build**

Run: `./gradlew :composeApp:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

- [ ] **Step 12: Commit**

```bash
git add .
git commit -m "feat: add design system with theme, shared UI components, and skeleton loaders"
```

---

### Task 8: Navigation & App Shell

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/navigation/Tabs.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/navigation/AppNavigator.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/kami/gamelist/App.kt`

**Interfaces:**
- Consumes: `GameListTheme` from Task 7, Voyager `TabNavigator`, Phosphor icons
- Produces: `App()` composable with BottomNav + TabNavigator + theme. `HomeTab`, `SearchTab`, `FavoritesTab`, `ListsTab` tab definitions.

- [ ] **Step 1: Create Tab definitions**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/navigation/Tabs.kt`:

```kotlin
package com.kami.gamelist.feature.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.kami.gamelist.feature.home.HomeScreen
import com.kami.gamelist.feature.search.SearchScreen
import com.kami.gamelist.feature.favorites.FavoritesScreen
import com.kami.gamelist.feature.lists.ListsScreen

object HomeTab : Tab {
    override val options: TabOptions
        @Composable get() {
            val icon = rememberVectorPainter(Icons.Outlined.Home)
            return remember { TabOptions(index = 0u, title = "Home", icon = icon) }
        }

    @Composable
    override fun Content() {
        HomeScreen()
    }
}

object SearchTab : Tab {
    override val options: TabOptions
        @Composable get() {
            val icon = rememberVectorPainter(Icons.Outlined.Search)
            return remember { TabOptions(index = 1u, title = "Search", icon = icon) }
        }

    @Composable
    override fun Content() {
        SearchScreen()
    }
}

object FavoritesTab : Tab {
    override val options: TabOptions
        @Composable get() {
            val icon = rememberVectorPainter(Icons.Outlined.FavoriteBorder)
            return remember { TabOptions(index = 2u, title = "Favorites", icon = icon) }
        }

    @Composable
    override fun Content() {
        FavoritesScreen()
    }
}

object ListsTab : Tab {
    override val options: TabOptions
        @Composable get() {
            val icon = rememberVectorPainter(Icons.Outlined.List)
            return remember { TabOptions(index = 3u, title = "Lists", icon = icon) }
        }

    @Composable
    override fun Content() {
        ListsScreen()
    }
}
```

- [ ] **Step 2: Create AppNavigator**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/navigation/AppNavigator.kt`:

```kotlin
package com.kami.gamelist.feature.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator

@Composable
fun AppNavigator() {
    TabNavigator(HomeTab) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    TabItem(HomeTab)
                    TabItem(SearchTab)
                    TabItem(FavoritesTab)
                    TabItem(ListsTab)
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                CurrentTab()
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(tab: Tab) {
    val tabNavigator = LocalTabNavigator.current
    val isSelected = tabNavigator.current == tab

    val color by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "tab_color"
    )

    NavigationBarItem(
        selected = isSelected,
        onClick = { tabNavigator.current = tab },
        icon = {
            tab.options.icon?.let {
                Icon(painter = it, contentDescription = tab.options.title, tint = color)
            }
        },
        label = {
            Text(
                text = tab.options.title,
                color = color,
                style = MaterialTheme.typography.labelSmall
            )
        },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    )
}
```

- [ ] **Step 3: Update App.kt**

Update `composeApp/src/commonMain/kotlin/com/kami/gamelist/App.kt`:

```kotlin
package com.kami.gamelist

import androidx.compose.runtime.Composable
import com.kami.gamelist.core.ui.theme.GameListTheme
import com.kami.gamelist.feature.navigation.AppNavigator

@Composable
fun App() {
    GameListTheme(darkTheme = true) {
        AppNavigator()
    }
}
```

- [ ] **Step 4: Create placeholder feature screens (stubs for compilation)**

These stubs let the app compile. Each subsequent task replaces the stub with the real implementation.

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/home/HomeScreen.kt`:
```kotlin
package com.kami.gamelist.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun HomeScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Home")
    }
}
```

Create the same stub pattern for `SearchScreen.kt`, `FavoritesScreen.kt`, `ListsScreen.kt` in their respective feature packages (same structure, different text label).

- [ ] **Step 5: Verify build**

Run: `./gradlew :composeApp:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "feat: add Voyager tab navigation with bottom nav bar and app shell"
```

---

### Task 9: Home Feature

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/home/HomeScreenModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/home/HomeScreen.kt` (replace stub)
- Create: `composeApp/src/commonTest/kotlin/com/kami/gamelist/feature/home/HomeScreenModelTest.kt`

**Interfaces:**
- Consumes: `GameRepository.observeGames(genre?, platform?)`, `GameRepository.refreshGames()`, `GameRepository.observeGenres()`, `GameRepository.observePlatforms()`, `ConnectivityMonitor.isOnline` from Task 5. `GameGrid`, `FilterChipRow`, `GameGridSkeleton`, `OfflineBanner` from Task 7.
- Produces: `HomeScreenModel` with `uiState: StateFlow<UiState<HomeData>>`, `selectedGenre: StateFlow<String?>`, `selectedPlatform: StateFlow<String?>`, `selectedSort: StateFlow<SortOption?>`, `isOffline: StateFlow<Boolean>`, `selectGenre(String?)`, `selectPlatform(String?)`, `selectSort(SortOption?)`, `refresh()`

- [ ] **Step 1: Write failing test for HomeScreenModel**

Create `composeApp/src/commonTest/kotlin/com/kami/gamelist/feature/home/HomeScreenModelTest.kt`:

```kotlin
package com.kami.gamelist.feature.home

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.kami.gamelist.core.ui.UiState
import com.kami.gamelist.data.local.CacheManager
import com.kami.gamelist.data.local.GameLocalDataSource
import com.kami.gamelist.data.remote.FreeToGameApi
import com.kami.gamelist.data.remote.dto.GameDto
import com.kami.gamelist.data.repository.GameRepository
import com.kami.gamelist.db.GameListDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: GameListDatabase
    private lateinit var repository: GameRepository

    private val sampleDtos = listOf(
        GameDto(
            id = 1, title = "Game A", thumbnail = "https://img.com/1.jpg",
            shortDescription = "Desc", gameUrl = "https://game.com/1",
            genre = "MMORPG", platform = "PC (Windows)", publisher = "Pub",
            developer = "Dev", releaseDate = "2023-01-01",
            freetogameProfileUrl = "https://ftg.com/1"
        )
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driver)
        database = GameListDatabase(driver)

        val client = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(
                        content = Json.encodeToString(sampleDtos),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        repository = GameRepository(
            FreeToGameApi(client),
            GameLocalDataSource(database),
            CacheManager(database)
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsLoadingThenSuccess() = runTest {
        val isOnline = MutableStateFlow(true)
        val screenModel = HomeScreenModel(repository, isOnline)

        screenModel.uiState.test {
            assertIs<UiState.Loading>(awaitItem())
            val success = awaitItem()
            assertIs<UiState.Success<HomeData>>(success)
            assertEquals(1, success.data.games.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :composeApp:allTests --tests "com.kami.gamelist.feature.home.HomeScreenModelTest"`
Expected: FAIL — `HomeScreenModel` and `HomeData` don't exist.

- [ ] **Step 3: Implement HomeScreenModel**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/home/HomeScreenModel.kt`:

```kotlin
package com.kami.gamelist.feature.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.kami.gamelist.core.ui.UiState
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.repository.GameRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeData(
    val games: List<Game>,
    val genres: List<String>,
    val platforms: List<String>
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenModel(
    private val gameRepository: GameRepository,
    isOnlineFlow: Flow<Boolean>
) : ScreenModel {

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    private val _selectedPlatform = MutableStateFlow<String?>(null)
    val selectedPlatform: StateFlow<String?> = _selectedPlatform.asStateFlow()

    val isOffline: StateFlow<Boolean> = isOnlineFlow
        .map { !it }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), false)

    val uiState: StateFlow<UiState<HomeData>> = combine(
        _selectedGenre,
        _selectedPlatform
    ) { genre, platform -> Pair(genre, platform) }
        .flatMapLatest { (genre, platform) ->
            combine(
                gameRepository.observeGames(genre, platform),
                gameRepository.observeGenres(),
                gameRepository.observePlatforms()
            ) { games, genres, platforms ->
                UiState.Success(HomeData(games, genres, platforms)) as UiState<HomeData>
            }
        }
        .catch { emit(UiState.Error(it.message ?: "Unknown error")) }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun selectGenre(genre: String?) {
        _selectedGenre.value = genre
    }

    fun selectPlatform(platform: String?) {
        _selectedPlatform.value = platform
    }

    fun refresh() {
        screenModelScope.launch {
            gameRepository.refreshGames()
        }
    }
}
```

- [ ] **Step 4: Run test**

Run: `./gradlew :composeApp:allTests --tests "com.kami.gamelist.feature.home.HomeScreenModelTest"`
Expected: ALL PASS

- [ ] **Step 5: Implement HomeScreen UI**

Replace `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/home/HomeScreen.kt`:

```kotlin
package com.kami.gamelist.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.koin.koinScreenModel
import com.kami.gamelist.core.ui.UiState
import com.kami.gamelist.core.ui.components.EmptyState
import com.kami.gamelist.core.ui.components.ErrorState
import com.kami.gamelist.core.ui.components.FilterChipRow
import com.kami.gamelist.core.ui.components.GameGrid
import com.kami.gamelist.core.ui.components.GameGridSkeleton
import com.kami.gamelist.core.ui.components.OfflineBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val screenModel = koinScreenModel<HomeScreenModel>()
    val uiState by screenModel.uiState.collectAsState()
    val selectedGenre by screenModel.selectedGenre.collectAsState()
    val selectedPlatform by screenModel.selectedPlatform.collectAsState()
    val isOffline by screenModel.isOffline.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        OfflineBanner(isOffline = isOffline)

        Text(
            text = "Free Games",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )

        when (val state = uiState) {
            is UiState.Loading -> {
                GameGridSkeleton(modifier = Modifier.fillMaxSize())
            }

            is UiState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = { screenModel.refresh() }
                )
            }

            is UiState.Success -> {
                val data = state.data

                if (data.genres.isNotEmpty()) {
                    FilterChipRow(
                        options = data.genres,
                        selectedOption = selectedGenre,
                        onOptionSelected = { screenModel.selectGenre(it) },
                        allLabel = "All Genres"
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (data.platforms.isNotEmpty()) {
                    FilterChipRow(
                        options = data.platforms,
                        selectedOption = selectedPlatform,
                        onOptionSelected = { screenModel.selectPlatform(it) },
                        allLabel = "All Platforms"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (data.games.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.SportsEsports,
                        title = "No games found",
                        subtitle = "Try adjusting your filters"
                    )
                } else {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            screenModel.refresh()
                            isRefreshing = false
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        GameGrid(
                            games = data.games,
                            onGameClick = { /* TODO: navigate to detail */ }
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 6: Verify build**

Run: `./gradlew :composeApp:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "feat: implement home screen with game grid, filters, pull-to-refresh, and offline banner"
```

---

### Task 10: Search Feature

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/search/SearchScreenModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/search/SearchScreen.kt` (replace stub)
- Create: `composeApp/src/commonTest/kotlin/com/kami/gamelist/feature/search/SearchScreenModelTest.kt`

**Interfaces:**
- Consumes: `GameRepository.searchGames(query)` from Task 5, `UserRepository.addSearchQuery()`, `UserRepository.observeRecentSearches()`, `UserRepository.deleteSearchQuery()` from Task 5. `GameSearchBar`, `GameGrid`, `EmptyState` from Task 7.
- Produces: `SearchScreenModel` with `uiState: StateFlow<UiState<List<Game>>>`, `query: StateFlow<String>`, `recentSearches: StateFlow<List<SearchHistory>>`, `showHistory: StateFlow<Boolean>`, `onQueryChange(String)`, `onSearch(String)`, `onHistoryItemClick(String)`, `onHistoryItemDelete(String)`

- [ ] **Step 1: Write failing test**

Create `composeApp/src/commonTest/kotlin/com/kami/gamelist/feature/search/SearchScreenModelTest.kt`:

```kotlin
package com.kami.gamelist.feature.search

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.kami.gamelist.data.local.CacheManager
import com.kami.gamelist.data.local.GameLocalDataSource
import com.kami.gamelist.data.local.UserLocalDataSource
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.remote.FreeToGameApi
import com.kami.gamelist.data.repository.GameRepository
import com.kami.gamelist.data.repository.UserRepository
import com.kami.gamelist.db.GameListDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SearchScreenModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: GameListDatabase
    private lateinit var gameRepository: GameRepository
    private lateinit var userRepository: UserRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driver)
        database = GameListDatabase(driver)

        val client = HttpClient(MockEngine) {
            engine { addHandler { respond("[]", HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())) } }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val localDataSource = GameLocalDataSource(database)
        gameRepository = GameRepository(FreeToGameApi(client), localDataSource, CacheManager(database))
        userRepository = UserRepository(UserLocalDataSource(database))

        localDataSource.upsertGames(listOf(
            Game(1, "Genshin Impact", "", "desc", "", "MMORPG", "PC", "Pub", "Dev", "2023-01-01", ""),
            Game(2, "Lost Ark", "", "desc", "", "MMORPG", "PC", "Pub", "Dev", "2023-01-01", "")
        ))
    }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun searchFiltersGames() = runTest {
        val screenModel = SearchScreenModel(gameRepository, userRepository)
        screenModel.onQueryChange("Genshin")
        screenModel.onSearch("Genshin")

        screenModel.searchResults.test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Genshin Impact", result[0].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchSavesToHistory() = runTest {
        val screenModel = SearchScreenModel(gameRepository, userRepository)
        screenModel.onSearch("Genshin")

        screenModel.recentSearches.test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Genshin", result[0].query)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 2: Implement SearchScreenModel**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/search/SearchScreenModel.kt`:

```kotlin
package com.kami.gamelist.feature.search

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.model.SearchHistory
import com.kami.gamelist.data.repository.GameRepository
import com.kami.gamelist.data.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchScreenModel(
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository
) : ScreenModel {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _showHistory = MutableStateFlow(true)
    val showHistory: StateFlow<Boolean> = _showHistory.asStateFlow()

    val searchResults: StateFlow<List<Game>> = _query
        .debounce(300)
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList())
            else gameRepository.searchGames(q)
        }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSearches: StateFlow<List<SearchHistory>> = userRepository
        .observeRecentSearches()
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        _showHistory.value = newQuery.isBlank()
    }

    fun onSearch(searchQuery: String) {
        if (searchQuery.isBlank()) return
        _query.value = searchQuery
        _showHistory.value = false
        userRepository.addSearchQuery(searchQuery)
    }

    fun onHistoryItemClick(searchQuery: String) {
        onSearch(searchQuery)
    }

    fun onHistoryItemDelete(searchQuery: String) {
        userRepository.deleteSearchQuery(searchQuery)
    }
}
```

- [ ] **Step 3: Implement SearchScreen UI**

Replace `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/search/SearchScreen.kt`:

```kotlin
package com.kami.gamelist.feature.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.koin.koinScreenModel
import com.kami.gamelist.core.ui.components.EmptyState
import com.kami.gamelist.core.ui.components.GameGrid
import com.kami.gamelist.core.ui.components.GameSearchBar

@Composable
fun SearchScreen() {
    val screenModel = koinScreenModel<SearchScreenModel>()
    val query by screenModel.query.collectAsState()
    val results by screenModel.searchResults.collectAsState()
    val recentSearches by screenModel.recentSearches.collectAsState()
    val showHistory by screenModel.showHistory.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))

        GameSearchBar(
            query = query,
            onQueryChange = screenModel::onQueryChange,
            onSearch = screenModel::onSearch,
            recentSearches = recentSearches,
            showHistory = showHistory,
            onHistoryItemClick = screenModel::onHistoryItemClick,
            onHistoryItemDelete = screenModel::onHistoryItemDelete
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (query.isNotBlank() && results.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Search,
                title = "No results",
                subtitle = "Try a different search term"
            )
        } else if (results.isNotEmpty()) {
            GameGrid(
                games = results,
                onGameClick = { /* TODO: navigate to detail */ }
            )
        }
    }
}
```

- [ ] **Step 4: Run tests and verify build**

Run: `./gradlew :composeApp:allTests --tests "com.kami.gamelist.feature.search.*" && ./gradlew :composeApp:compileKotlinAndroid`
Expected: ALL PASS, BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: implement search screen with debounced search, history, and real-time results"
```

---

### Task 11: Game Detail Feature

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/detail/GameDetailScreenModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/detail/GameDetailScreen.kt`
- Create: `composeApp/src/commonTest/kotlin/com/kami/gamelist/feature/detail/GameDetailScreenModelTest.kt`

**Interfaces:**
- Consumes: `GameRepository.observeGameDetail(id)` from Task 5. `UserRepository.isFavorite(gameId)`, `UserRepository.toggleFavorite(gameId)`, `UserRepository.observeLists()`, `UserRepository.addToList()`, `UserRepository.removeFromList()` from Task 5. `ScreenshotCarousel`, `FavoriteButton`, `ListSelector`, `GameDetailSkeleton`, `ErrorState` from Task 7.
- Produces: `GameDetailScreenModel` with `uiState: StateFlow<UiState<GameDetail>>`, `isFavorite: StateFlow<Boolean>`, `lists: StateFlow<List<UserList>>`, `listsContainingGame: StateFlow<Set<Long>>`, `toggleFavorite()`, `toggleList(UserList)`, `showListSelector: StateFlow<Boolean>`, `onShowListSelector()`, `onDismissListSelector()`

- [ ] **Step 1: Write failing test**

Create `composeApp/src/commonTest/kotlin/com/kami/gamelist/feature/detail/GameDetailScreenModelTest.kt`:

```kotlin
package com.kami.gamelist.feature.detail

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.kami.gamelist.core.ui.UiState
import com.kami.gamelist.data.local.CacheManager
import com.kami.gamelist.data.local.GameLocalDataSource
import com.kami.gamelist.data.local.UserLocalDataSource
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.model.GameDetail
import com.kami.gamelist.data.model.Screenshot
import com.kami.gamelist.data.model.SystemRequirements
import com.kami.gamelist.data.remote.FreeToGameApi
import com.kami.gamelist.data.remote.dto.GameDetailDto
import com.kami.gamelist.data.remote.dto.MinimumSystemRequirementsDto
import com.kami.gamelist.data.remote.dto.ScreenshotDto
import com.kami.gamelist.data.repository.GameRepository
import com.kami.gamelist.data.repository.UserRepository
import com.kami.gamelist.db.GameListDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameDetailScreenModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: GameListDatabase
    private lateinit var gameRepository: GameRepository
    private lateinit var userRepository: UserRepository

    private val detailDto = GameDetailDto(
        id = 1, title = "Game 1", thumbnail = "https://img.com/1.jpg",
        shortDescription = "Desc", gameUrl = "https://game.com/1",
        genre = "MMORPG", platform = "PC", publisher = "Pub",
        developer = "Dev", releaseDate = "2023-01-01",
        freetogameProfileUrl = "https://ftg.com/1",
        description = "Full desc", status = "Live",
        screenshots = listOf(ScreenshotDto(1, "https://img.com/ss1.jpg")),
        minimumSystemRequirements = MinimumSystemRequirementsDto("Win10", "i5", "8GB", "GTX1060", "30GB")
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driver)
        database = GameListDatabase(driver)

        val client = HttpClient(MockEngine) {
            engine { addHandler { respond(Json.encodeToString(detailDto), HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())) } }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val localDataSource = GameLocalDataSource(database)
        gameRepository = GameRepository(FreeToGameApi(client), localDataSource, CacheManager(database))
        userRepository = UserRepository(UserLocalDataSource(database))
    }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun loadsGameDetailSuccessfully() = runTest {
        val screenModel = GameDetailScreenModel(1, gameRepository, userRepository)

        screenModel.uiState.test {
            assertIs<UiState.Loading>(awaitItem())
            val success = awaitItem()
            assertIs<UiState.Success<GameDetail>>(success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleFavoriteUpdatesFavoriteState() = runTest {
        val localDataSource = GameLocalDataSource(database)
        localDataSource.upsertGames(listOf(
            Game(1, "Game 1", "", "desc", "", "MMORPG", "PC", "Pub", "Dev", "2023-01-01", "")
        ))

        val screenModel = GameDetailScreenModel(1, gameRepository, userRepository)

        screenModel.isFavorite.test {
            val initial = awaitItem()
            assertTrue(!initial)
            cancelAndIgnoreRemainingEvents()
        }

        screenModel.toggleFavorite()

        screenModel.isFavorite.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 2: Implement GameDetailScreenModel**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/detail/GameDetailScreenModel.kt`:

```kotlin
package com.kami.gamelist.feature.detail

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.kami.gamelist.core.ui.UiState
import com.kami.gamelist.data.model.GameDetail
import com.kami.gamelist.data.model.UserList
import com.kami.gamelist.data.repository.GameRepository
import com.kami.gamelist.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameDetailScreenModel(
    private val gameId: Int,
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository
) : ScreenModel {

    val uiState: StateFlow<UiState<GameDetail>> = gameRepository
        .observeGameDetail(gameId)
        .map { detail ->
            if (detail != null) UiState.Success(detail)
            else UiState.Loading
        }
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val isFavorite: StateFlow<Boolean> = userRepository
        .isFavorite(gameId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), false)

    val lists: StateFlow<List<UserList>> = userRepository
        .observeLists()
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showListSelector = MutableStateFlow(false)
    val showListSelector: StateFlow<Boolean> = _showListSelector.asStateFlow()

    fun toggleFavorite() {
        userRepository.toggleFavorite(gameId)
    }

    fun onShowListSelector() { _showListSelector.value = true }
    fun onDismissListSelector() { _showListSelector.value = false }

    fun toggleList(list: UserList) {
        screenModelScope.launch {
            userRepository.isInList(list.id, gameId).collect { isIn ->
                if (isIn) userRepository.removeFromList(list.id, gameId)
                else userRepository.addToList(list.id, gameId)
                return@collect
            }
        }
    }
}
```

- [ ] **Step 3: Implement GameDetailScreen UI**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/detail/GameDetailScreen.kt`:

```kotlin
package com.kami.gamelist.feature.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.kami.gamelist.core.ui.UiState
import com.kami.gamelist.core.ui.components.ErrorState
import com.kami.gamelist.core.ui.components.FavoriteButton
import com.kami.gamelist.core.ui.components.GameCardSkeleton
import com.kami.gamelist.core.ui.components.ListSelector
import com.kami.gamelist.core.ui.components.ScreenshotCarousel
import org.koin.core.parameter.parametersOf

data class GameDetailNavScreen(val gameId: Int) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<GameDetailScreenModel> { parametersOf(gameId) }
        val uiState by screenModel.uiState.collectAsState()
        val isFavorite by screenModel.isFavorite.collectAsState()
        val lists by screenModel.lists.collectAsState()
        val showListSelector by screenModel.showListSelector.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        FavoriteButton(
                            isFavorite = isFavorite,
                            onToggle = { screenModel.toggleFavorite() },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        IconButton(onClick = { screenModel.onShowListSelector() }) {
                            Icon(Icons.Outlined.PlaylistAdd, contentDescription = "Add to list")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        // Detail skeleton
                        Column(Modifier.padding(16.dp)) {
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                        }
                    }
                }

                is UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { },
                        modifier = Modifier.padding(padding)
                    )
                }

                is UiState.Success -> {
                    val detail = state.data
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                    ) {
                        AsyncImage(
                            model = detail.game.thumbnail,
                            contentDescription = detail.game.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .padding(horizontal = 16.dp)
                                .clip(MaterialTheme.shapes.medium)
                        )

                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = detail.game.title,
                                style = MaterialTheme.typography.headlineMedium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row {
                                Text(
                                    text = detail.game.genre,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = detail.game.platform,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = detail.status,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = detail.description,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            if (detail.screenshots.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Screenshots",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ScreenshotCarousel(screenshots = detail.screenshots)
                            }

                            detail.minimumSystemRequirements?.let { reqs ->
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "System Requirements",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                reqs.os?.let { SystemReqRow("OS", it) }
                                reqs.processor?.let { SystemReqRow("Processor", it) }
                                reqs.memory?.let { SystemReqRow("Memory", it) }
                                reqs.graphics?.let { SystemReqRow("Graphics", it) }
                                reqs.storage?.let { SystemReqRow("Storage", it) }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Publisher: ${detail.game.publisher}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Developer: ${detail.game.developer}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Release: ${detail.game.releaseDate}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }

            if (showListSelector) {
                ListSelector(
                    lists = lists,
                    listsContainingGame = emptySet(),
                    onListToggle = { screenModel.toggleList(it) },
                    onDismiss = { screenModel.onDismissListSelector() }
                )
            }
        }
    }
}

@Composable
private fun SystemReqRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
```

- [ ] **Step 4: Wire navigation from Home and Search to Detail**

Update `onGameClick` in `HomeScreen.kt` and `SearchScreen.kt` to navigate:

In both screens, add at the top of the composable:
```kotlin
val navigator = LocalNavigator.currentOrThrow
```

And replace `onGameClick = { /* TODO */ }` with:
```kotlin
onGameClick = { game -> navigator.push(GameDetailNavScreen(game.id)) }
```

Add imports:
```kotlin
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.kami.gamelist.feature.detail.GameDetailNavScreen
```

- [ ] **Step 5: Run tests and verify build**

Run: `./gradlew :composeApp:allTests --tests "com.kami.gamelist.feature.detail.*" && ./gradlew :composeApp:compileKotlinAndroid`
Expected: ALL PASS, BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "feat: implement game detail screen with screenshots, system requirements, favorite, and list actions"
```

---

### Task 12: Favorites Feature

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/favorites/FavoritesScreenModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/favorites/FavoritesScreen.kt` (replace stub)
- Create: `composeApp/src/commonTest/kotlin/com/kami/gamelist/feature/favorites/FavoritesScreenModelTest.kt`

**Interfaces:**
- Consumes: `UserRepository.observeFavorites()` from Task 5. `GameGrid`, `GameGridSkeleton`, `EmptyState` from Task 7. `GameDetailNavScreen` from Task 11.
- Produces: `FavoritesScreenModel` with `favorites: StateFlow<List<Game>>`

- [ ] **Step 1: Write failing test**

Create `composeApp/src/commonTest/kotlin/com/kami/gamelist/feature/favorites/FavoritesScreenModelTest.kt`:

```kotlin
package com.kami.gamelist.feature.favorites

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.kami.gamelist.data.local.GameLocalDataSource
import com.kami.gamelist.data.local.UserLocalDataSource
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.repository.UserRepository
import com.kami.gamelist.db.GameListDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesScreenModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: GameListDatabase
    private lateinit var userRepository: UserRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driver)
        database = GameListDatabase(driver)

        val gameDataSource = GameLocalDataSource(database)
        gameDataSource.upsertGames(listOf(
            Game(1, "Game 1", "", "desc", "", "RPG", "PC", "Pub", "Dev", "2023-01-01", ""),
            Game(2, "Game 2", "", "desc", "", "FPS", "PC", "Pub", "Dev", "2023-01-01", "")
        ))

        val userDataSource = UserLocalDataSource(database)
        userDataSource.toggleFavorite(1)
        userDataSource.toggleFavorite(2)
        userRepository = UserRepository(userDataSource)
    }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun showsFavoritedGames() = runTest {
        val screenModel = FavoritesScreenModel(userRepository)
        screenModel.favorites.test {
            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 2: Implement FavoritesScreenModel**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/favorites/FavoritesScreenModel.kt`:

```kotlin
package com.kami.gamelist.feature.favorites

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FavoritesScreenModel(
    userRepository: UserRepository
) : ScreenModel {

    val favorites: StateFlow<List<Game>> = userRepository
        .observeFavorites()
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

- [ ] **Step 3: Implement FavoritesScreen UI**

Replace `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/favorites/FavoritesScreen.kt`:

```kotlin
package com.kami.gamelist.feature.favorites

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.kami.gamelist.core.ui.components.EmptyState
import com.kami.gamelist.core.ui.components.GameGrid
import com.kami.gamelist.feature.detail.GameDetailNavScreen

@Composable
fun FavoritesScreen() {
    val navigator = LocalNavigator.currentOrThrow
    val screenModel = koinScreenModel<FavoritesScreenModel>()
    val favorites by screenModel.favorites.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Favorites",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )

        if (favorites.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.FavoriteBorder,
                title = "No favorites yet",
                subtitle = "Tap the heart on a game to add it here"
            )
        } else {
            GameGrid(
                games = favorites,
                onGameClick = { game -> navigator.push(GameDetailNavScreen(game.id)) }
            )
        }
    }
}
```

- [ ] **Step 4: Run tests and verify build**

Run: `./gradlew :composeApp:allTests --tests "com.kami.gamelist.feature.favorites.*" && ./gradlew :composeApp:compileKotlinAndroid`
Expected: ALL PASS, BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: implement favorites screen with empty state"
```

---

### Task 13: Lists Feature

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/lists/ListsScreenModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/lists/ListsScreen.kt` (replace stub)
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/lists/ListDetailScreenModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/lists/ListDetailScreen.kt`
- Create: `composeApp/src/commonTest/kotlin/com/kami/gamelist/feature/lists/ListsScreenModelTest.kt`

**Interfaces:**
- Consumes: `UserRepository.observeLists()`, `UserRepository.createList()`, `UserRepository.deleteList()`, `UserRepository.updateListName()`, `UserRepository.observeGamesInList()`, `UserRepository.listGameCount()` from Task 5. `EmptyState`, `GameGrid` from Task 7. `GameDetailNavScreen` from Task 11.
- Produces:
  - `ListsScreenModel` with `lists: StateFlow<List<UserList>>`, `createList(name, type)`, `deleteList(id)`, `updateListName(id, name)`
  - `ListDetailScreenModel` with `games: StateFlow<List<Game>>`, `listName: StateFlow<String>`

- [ ] **Step 1: Write failing test**

Create `composeApp/src/commonTest/kotlin/com/kami/gamelist/feature/lists/ListsScreenModelTest.kt`:

```kotlin
package com.kami.gamelist.feature.lists

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.kami.gamelist.data.local.UserLocalDataSource
import com.kami.gamelist.data.model.ListType
import com.kami.gamelist.data.repository.UserRepository
import com.kami.gamelist.db.GameListDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ListsScreenModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var userRepository: UserRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameListDatabase.Schema.create(driver)
        val database = GameListDatabase(driver)
        userRepository = UserRepository(UserLocalDataSource(database))
    }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun createAndObserveLists() = runTest {
        val screenModel = ListsScreenModel(userRepository)
        screenModel.createList("Jogando", ListType.PLAYING)
        screenModel.createList("Quero Jogar", ListType.WANT_TO_PLAY)

        screenModel.lists.test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("Jogando", result[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteListRemovesIt() = runTest {
        val screenModel = ListsScreenModel(userRepository)
        screenModel.createList("Temp", ListType.CUSTOM)

        screenModel.lists.test {
            val before = awaitItem()
            assertEquals(1, before.size)

            screenModel.deleteList(before[0].id)
            val after = awaitItem()
            assertEquals(0, after.size)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 2: Implement ListsScreenModel**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/lists/ListsScreenModel.kt`:

```kotlin
package com.kami.gamelist.feature.lists

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.kami.gamelist.data.model.ListType
import com.kami.gamelist.data.model.UserList
import com.kami.gamelist.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ListsScreenModel(
    private val userRepository: UserRepository
) : ScreenModel {

    val lists: StateFlow<List<UserList>> = userRepository
        .observeLists()
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createList(name: String, type: ListType): Long {
        return userRepository.createList(name, type)
    }

    fun deleteList(id: Long) {
        userRepository.deleteList(id)
    }

    fun updateListName(id: Long, name: String) {
        userRepository.updateListName(id, name)
    }
}
```

- [ ] **Step 3: Implement ListDetailScreenModel**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/lists/ListDetailScreenModel.kt`:

```kotlin
package com.kami.gamelist.feature.lists

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.kami.gamelist.data.model.Game
import com.kami.gamelist.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ListDetailScreenModel(
    private val listId: Long,
    private val userRepository: UserRepository
) : ScreenModel {

    val games: StateFlow<List<Game>> = userRepository
        .observeGamesInList(listId)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeFromList(gameId: Int) {
        userRepository.removeFromList(listId, gameId)
    }
}
```

- [ ] **Step 4: Implement ListsScreen UI**

Replace `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/lists/ListsScreen.kt`:

```kotlin
package com.kami.gamelist.feature.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.kami.gamelist.core.ui.components.EmptyState
import com.kami.gamelist.data.model.ListType
import com.kami.gamelist.data.model.UserList

@Composable
fun ListsScreen() {
    val navigator = LocalNavigator.currentOrThrow
    val screenModel = koinScreenModel<ListsScreenModel>()
    val lists by screenModel.lists.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create list")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                text = "My Lists",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            if (lists.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.List,
                    title = "No lists yet",
                    subtitle = "Create a list to organize your games"
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(lists, key = { it.id }) { list ->
                        ListItem(
                            list = list,
                            onClick = { navigator.push(ListDetailNavScreen(list.id, list.name)) },
                            onDelete = { screenModel.deleteList(list.id) }
                        )
                    }
                }
            }
        }

        if (showCreateDialog) {
            CreateListDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name ->
                    screenModel.createList(name, ListType.CUSTOM)
                    showCreateDialog = false
                }
            )
        }
    }
}

@Composable
private fun ListItem(
    list: UserList,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = list.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = list.type.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (list.type == ListType.CUSTOM) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete list",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateListDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create new list") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("List name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

- [ ] **Step 5: Implement ListDetailScreen UI**

Create `composeApp/src/commonMain/kotlin/com/kami/gamelist/feature/lists/ListDetailScreen.kt`:

```kotlin
package com.kami.gamelist.feature.lists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.kami.gamelist.core.ui.components.EmptyState
import com.kami.gamelist.core.ui.components.GameGrid
import com.kami.gamelist.feature.detail.GameDetailNavScreen
import org.koin.core.parameter.parametersOf

data class ListDetailNavScreen(val listId: Long, val listName: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<ListDetailScreenModel> { parametersOf(listId) }
        val games by screenModel.games.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(listName) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (games.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.List,
                        title = "List is empty",
                        subtitle = "Add games from the detail screen"
                    )
                } else {
                    GameGrid(
                        games = games,
                        onGameClick = { game -> navigator.push(GameDetailNavScreen(game.id)) }
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 6: Seed default lists on first launch**

Update `App.kt` to create default lists if none exist:

```kotlin
package com.kami.gamelist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.kami.gamelist.core.ui.theme.GameListTheme
import com.kami.gamelist.data.model.ListType
import com.kami.gamelist.data.repository.UserRepository
import com.kami.gamelist.feature.navigation.AppNavigator
import org.koin.compose.koinInject

@Composable
fun App() {
    val userRepository = koinInject<UserRepository>()

    LaunchedEffect(Unit) {
        userRepository.observeLists().collect { lists ->
            if (lists.isEmpty()) {
                userRepository.createList("Playing", ListType.PLAYING)
                userRepository.createList("Want to Play", ListType.WANT_TO_PLAY)
                userRepository.createList("Played", ListType.PLAYED)
            }
            return@collect
        }
    }

    GameListTheme(darkTheme = true) {
        AppNavigator()
    }
}
```

- [ ] **Step 7: Run tests and verify build**

Run: `./gradlew :composeApp:allTests --tests "com.kami.gamelist.feature.lists.*" && ./gradlew :composeApp:compileKotlinAndroid`
Expected: ALL PASS, BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add .
git commit -m "feat: implement lists feature with create, delete, detail view, and default list seeding"
```

---

## Post-Implementation Checklist

After all tasks are complete, verify:

- [ ] `./gradlew :composeApp:allTests` — all tests pass
- [ ] `./gradlew :composeApp:assembleDebug` — Android APK builds successfully
- [ ] Run on Android emulator — verify Home, Search, Detail, Favorites, Lists all work
- [ ] Verify offline mode — disable network, confirm cached data shows, enable network, confirm sync
- [ ] Verify dark theme renders correctly on both platforms
