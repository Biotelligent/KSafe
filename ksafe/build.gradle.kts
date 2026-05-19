import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
//    alias(libs.plugins.kotlinMultiplatform)
//    alias(libs.plugins.android.kotlin.multiplatform.library)
//    alias(libs.plugins.vanniktech.mavenPublish)
//
//    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.androidKmpLibrary)
    id("com.airware.convention.kmm")
}

group = "eu.anifantakis"
version = "2.0.0"

kotlin {
    android {
        namespace = "eu.anifantakis"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
//    listOf(
//        iosX64(),
//        iosArm64(),
//        iosSimulatorArm64(),
//        macosX64(),
//        macosArm64(),
//    ).forEach {
//        it.binaries.framework {
//            baseName = "ksafe"
//            isStatic = true
//        }
//    }

    // Add a JVM target to support desktop platforms.
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    // Add a WASM/JS target for browser-based web apps.
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    // Add a plain JS/IR target for legacy JS projects and projects that
    // need both js + wasmJs artifacts.
    js(IR) {
        browser()
    }

    // Explicitly apply the default KMP hierarchy template. This creates
    // the intermediate source sets we use (nativeMain/appleMain/iosMain
    // for the Apple targets, and webMain/webTest shared between js and
    // wasmJs). Without this call, `webMain` does not exist.
    applyDefaultHierarchyTemplate()

    sourceSets {
        @Suppress("unused")
        val commonMain by getting {
            dependencies {
                // api (not implementation) — JSON is part of KSafe's public API (KSafeConfig.json),
                // so consumers get kotlinx-serialization-json transitively without declaring it themselves.
                api(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)

                implementation(libs.cryptography.core)
                implementation(libs.cryptography.provider.base)

                // compileOnly — used solely for the @Stable marker on the KSafe class so
                // Compose consumers (via :ksafe-compose) get accurate stability inference
                // and skip recompositions when passing a KSafe instance as a Composable
                // parameter. @Stable has BINARY retention and no runtime effect, so
                // non-Compose consumers (Ktor servers, CLI tools, plain JVM) do NOT
                // need compose-runtime on their classpath at runtime.
                compileOnly(libs.cmp.runtime)
            }
        }

        // Intermediate source set shared by Android, iOS and JVM — all three use
        // Jetpack DataStore Preferences as their on-disk backend, so the
        // DataStoreStorage adapter lives here instead of being duplicated.
        val datastoreMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.androidx.datastore.preferences.core)
            }
        }

        androidMain {
            dependsOn(datastoreMain)
            dependencies {
                implementation(libs.androidx.datastore.preferences)
                implementation(libs.cryptography.provider.jdk)
            }
        }
        // appleMain is shared by iosX64/iosArm64/iosSimulatorArm64 + macosX64/macosArm64.
        // The default hierarchy template (applyDefaultHierarchyTemplate above) wires it up.
        appleMain {
            dependsOn(datastoreMain)
            dependencies {
                implementation(libs.cryptography.provider.cryptokit)
            }
        }

        // Dependencies for the JVM target
        val jvmMain by getting {
            dependsOn(datastoreMain)
            dependencies {
                implementation(libs.androidx.datastore.preferences)
            }
        }

         // Dependencies shared by wasmJs + js targets.
        val webMain by getting {
            dependencies {
                implementation(libs.cryptography.provider.webcrypto)
            }
        }

        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }
}

ktlint {
    filter {
        exclude("**/*.kt")
    }
}
