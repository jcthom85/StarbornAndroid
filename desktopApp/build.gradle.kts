plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.protobuf)
}

kotlin {
    jvmToolchain(21)
    sourceSets {
        main {
            kotlin.srcDirs(
                "src/main/kotlin",
                "../app/src/main/java"
            )
            kotlin.include(
                "com/example/starborn/desktop/**",
                "com/example/starborn/core/**",
                "com/example/starborn/data/**",
                "com/example/starborn/domain/**",
                "com/example/starborn/feature/arcade/domain/**",
                "com/example/starborn/feature/arcade/games/**",
                "com/example/starborn/feature/exploration/ui/menu/FieldMenuDesign.kt",
                "com/example/starborn/feature/mainmenu/DebugScenario*",
                "com/example/starborn/feature/enemy/**",
                "com/example/starborn/ui/events/**"
            )
            kotlin.exclude(
                "**/AndroidAssetProvider.kt",
                "**/AudioCuePlayer.kt",
                "**/UserSettingsStore.kt",
                "**/GameSaveRepository.kt",
                "**/*_temp.kt.ignored"
            )
        }
    }
}

sourceSets {
    named("main") {
        proto {
            srcDir("../app/src/main/proto")
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.1"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                maybeCreate("java").apply {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("com.squareup.moshi:moshi:1.15.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("org.json:json:20240303")
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.protobuf.javalite)
}

compose.desktop {
    application {
        mainClass = "com.example.starborn.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )
            packageName = "Starborn"
            packageVersion = "1.0.0"
            description = "Starborn - Sci-Fi Turn-Based RPG"
            vendor = "June Wire Games"
            windows {
                menuGroup = "Starborn"
                upgradeUuid = "a4c28bb0-7988-466d-8b01-f1190db981b2"
            }
        }
    }
}
