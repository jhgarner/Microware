import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
}

val appVersion = providers.fileContents(rootProject.layout.projectDirectory.file("version.txt")).asText.map { it.trim() }

val generateVersion by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/version")
    val version = appVersion
    inputs.property("version", version)
    outputs.dir(outputDir)
    doLast {
        outputDir.get().file("com/example/microware/Version.kt").asFile.apply {
            parentFile.mkdirs()
            writeText(
                """
                |package com.example.microware
                |
                |const val APP_VERSION = "${version.get()}"
                |
                """.trimMargin()
            )
        }
    }
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    android {
        namespace = "com.example.microware.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateVersion)
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}