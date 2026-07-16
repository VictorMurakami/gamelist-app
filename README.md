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
