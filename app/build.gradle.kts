import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.testing.Test
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.play.publisher)
}

android {
    namespace = "com.example.starborn"
    compileSdk = 35
    val keystoreProperties = Properties().apply {
        val propertiesFile = rootProject.file("keystore.properties")
        if (propertiesFile.exists()) {
            propertiesFile.inputStream().use(::load)
        }
    }

    defaultConfig {
        applicationId = "com.junewiregames.starborn.prealpha"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val configuredStoreFile = keystoreProperties.getProperty("storeFile")
            if (!configuredStoreFile.isNullOrBlank()) {
                storeFile = rootProject.file(configuredStoreFile)
            }
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    sourceSets {
        getByName("debug") {
            assets.srcDirs("src/debug/assets", "../world_assets/src/main/assets")
        }
    }
    assetPacks += listOf(":world_assets")
}

play {
    serviceAccountCredentials.set(rootProject.file("play-service-account.json"))
    track.set("internal")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.json:json:20240303")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.squareup.moshi:moshi:1.15.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.protobuf.javalite)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.datasource)
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")
}

tasks.withType<Test>().configureEach {
    val isolatedHome = layout.buildDirectory.dir("test-home")
    val isolatedTmp = layout.buildDirectory.dir("test-tmp")
    systemProperty("user.home", isolatedHome.get().asFile.absolutePath)
    systemProperty("java.io.tmpdir", isolatedTmp.get().asFile.absolutePath)
    jvmArgs("-Djava.io.tmpdir=${isolatedTmp.get().asFile.absolutePath}")
    environment("TMP", isolatedTmp.get().asFile.absolutePath)
    environment("TEMP", isolatedTmp.get().asFile.absolutePath)
    doFirst {
        isolatedHome.get().asFile.mkdirs()
        isolatedTmp.get().asFile.mkdirs()
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

val isWindowsHost = System.getProperty("os.name").lowercase().contains("windows")
val powerShellExecutable = if (isWindowsHost) "powershell" else "pwsh"
val powerShellBaseArgs = if (isWindowsHost) {
    listOf("-NoProfile", "-ExecutionPolicy", "Bypass", "-File")
} else {
    listOf("-NoProfile", "-File")
}
val pythonExecutable = if (isWindowsHost) "python" else "python3"

fun registerPowerShellValidationTask(
    name: String,
    descriptionText: String,
    scriptPath: String,
    vararg scriptArgs: String
) = tasks.register<Exec>(name) {
    description = descriptionText
    group = "verification"
    workingDir = rootProject.projectDir
    commandLine(
        listOf(powerShellExecutable) + powerShellBaseArgs + listOf(scriptPath) + scriptArgs.toList()
    )
}

val validateWorld1Content = registerPowerShellValidationTask(
    "validateWorld1Content",
    "Validates World 1 rooms, references, art, and audio coverage.",
    "scripts/validate_world1_content.ps1",
    "-StrictArt",
    "-StrictAudio",
    "-StrictInlineActions"
)

val validateRoomPresence = registerPowerShellValidationTask(
    "validateRoomPresence",
    "Validates room NPC presence rules and duplicate availability.",
    "scripts/validate_room_presence.ps1",
    "-StrictDuplicates"
)

val validateAudioReferences = registerPowerShellValidationTask(
    "validateAudioReferences",
    "Validates referenced audio cues against the audio catalog.",
    "scripts/validate_audio_references.ps1",
    "-StrictCatalog"
)

val validateProgressionReferences = registerPowerShellValidationTask(
    "validateProgressionReferences",
    "Validates World 1 progression, milestone, and dialogue references.",
    "scripts/validate_progression_references.ps1",
    "-StrictMilestones"
)

val validateWorld1Balance = registerPowerShellValidationTask(
    "validateWorld1Balance",
    "Validates World 1 encounter/reward balance guardrails.",
    "scripts/validate_world1_balance.ps1",
    "-Strict"
)

val validateEnemyMovement = registerPowerShellValidationTask(
    "validateEnemyMovement",
    "Validates enemy movement zones, routes, parties, and safety boundaries.",
    "scripts/validate_enemy_movement.ps1"
)

val validateDialogueEmotes = tasks.register<Exec>("validateDialogueEmotes") {
    description = "Validates used dialogue/cinematic/shop emote references and minimum emote coverage."
    group = "verification"
    workingDir = rootProject.projectDir
    commandLine(
        pythonExecutable,
        "scripts/validate_dialogue_emotes.py",
        "--fail-on-missing",
        "--min-uses",
        "12"
    )
}

val validateWorld1Assets = tasks.register("validateWorld1Assets") {
    description = "Runs all World 1 content, audio, progression, balance, and dialogue emote validators."
    group = "verification"
    dependsOn(
        validateWorld1Content,
        validateRoomPresence,
        validateAudioReferences,
        validateProgressionReferences,
        validateWorld1Balance,
        validateEnemyMovement,
        validateDialogueEmotes
    )
}

afterEvaluate {
    val testDebugUnitTest = tasks.named<Test>("testDebugUnitTest")
    tasks.register<Test>("runAssetIntegrity") {
        description = "Runs DataIntegrityTest to validate events, cinematics, and tutorial assets."
        group = "verification"
        dependsOn(validateWorld1Assets)
        testClassesDirs = testDebugUnitTest.get().testClassesDirs
        classpath = testDebugUnitTest.get().classpath
        include("**/DataIntegrityTest.class")
        shouldRunAfter(testDebugUnitTest)
    }
}
