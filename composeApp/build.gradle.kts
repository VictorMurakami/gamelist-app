import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.android.application)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm()

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
            implementation(compose.materialIconsExtended)
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
            implementation(libs.kotlinx.datetime)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.turbine)
            implementation(libs.sqldelight.jdbc)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android)
            implementation(libs.activity.compose)
            implementation(libs.koin.android)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.java)
            implementation(libs.sqldelight.jdbc)
        }

        jvmTest.dependencies {
            implementation(libs.sqldelight.jdbc)
        }
    }
}

// versionName eh enviado verbatim ao backend de app-config
// (AppInfo.android.kt) e comparado la com PEP 440. Um valor fora de
// MAJOR.MINOR.PATCH quebra dos dois lados: (a) qualquer sufixo/formato que o
// PEP 440 nao reconheca faz o resolver do backend responder "nao consegui
// parsear" e cair silenciosamente em status=none, desligando o gate de force
// update sem nenhum sinal; (b) um sufixo de pre-release que o PEP 440
// reconhece (ex.: "1.0.0-beta" -> "1.0.0b0") normaliza para ABAIXO da versao
// minima suportada, e o backend responde forced legitimamente — a
// ForceUpdateScreen nao tem saida e manda pra uma loja onde esse build nao
// existe, brickando qualquer instalacao de teste interno. Este check fecha as
// duas pontas na fonte, antes de qualquer build chegar perto de um device.
val versionNamePattern = Regex("""^\d+\.\d+\.\d+$""")

android {
    namespace = "com.kami.gamelist"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kami.gamelist"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        val currentVersionName = versionName
        require(currentVersionName != null && versionNamePattern.matches(currentVersionName)) {
            "versionName '$currentVersionName' precisa ser MAJOR.MINOR.PATCH " +
                "(ex.: 1.0.0), sem sufixo de pre-release. O backend de " +
                "app-config compara essa string com PEP 440: um sufixo " +
                "desconhecido cai em status=none (gate desligado em " +
                "silencio) e um sufixo de pre-release reconhecido normaliza " +
                "pra abaixo da versao minima e trava o app numa " +
                "ForceUpdateScreen sem saida, apontando pra uma loja onde " +
                "esse build nao existe."
        }
    }

    buildFeatures {
        buildConfig = true
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
            dialect(libs.sqldelight.dialect)
        }
    }
}
