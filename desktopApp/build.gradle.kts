import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(project(":gameRuntime"))
    implementation(project(":shared"))
    implementation(libs.kotlinx.serialization.json)
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
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")
    implementation("com.googlecode.soundlibs:jlayer:1.0.1.4")
    implementation("com.googlecode.soundlibs:tritonus-share:0.3.7.4")
    testImplementation(libs.junit)
    testImplementation(compose.desktop.uiTestJUnit4)
}

compose.desktop {
    application {
        javaHome = providers.gradleProperty("starborn.packagingJavaHome").orNull ?: System.getenv("JAVA_HOME")
        jvmArgs += listOf("-Dstarborn.packagedAssets=true")
        mainClass = "com.example.starborn.desktop.MainKt"

        nativeDistributions {
            includeAllModules = true
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

// A complete asset tree is packaged; runtime access never depends on the checkout.
val stageGameAssets = tasks.register<Sync>("stageGameAssets") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(rootProject.file("app/src/main/assets")) { into("assets") }
    from(rootProject.file("world_assets/src/main/assets")) { into("assets") }
    from(rootProject.file("app/src/main/res")) { into("assets") }
    into(layout.buildDirectory.dir("game-assets"))
}
val generateAssetManifest = tasks.register("generateAssetManifest") {
    dependsOn(stageGameAssets)
    val assetRoot = layout.buildDirectory.dir("game-assets/assets")
    val manifestFile = layout.buildDirectory.file("generated/game-asset-manifest/asset-manifest.txt")
    inputs.dir(assetRoot)
    outputs.file(manifestFile)
    doLast {
        val root = assetRoot.get().asFile
        val files = root.walkTopDown().filter { it.isFile && it.name != "asset-manifest.txt" }
            .map { it.relativeTo(root).invariantSeparatorsPath }.sorted().toList()
        val manifest = manifestFile.get().asFile
        manifest.parentFile.mkdirs()
        manifest.writeText(files.joinToString("\n"))
    }
}
val gameAssetsJar = tasks.register<Jar>("gameAssetsJar") {
    dependsOn(generateAssetManifest)
    archiveBaseName.set("starborn-assets")
    destinationDirectory.set(layout.buildDirectory.dir("game-asset-library"))
    from(layout.buildDirectory.dir("game-assets")) { exclude("assets/asset-manifest.txt") }
    from(layout.buildDirectory.file("generated/game-asset-manifest/asset-manifest.txt")) { into("assets") }
    isZip64 = true
}
dependencies { runtimeOnly(files(gameAssetsJar.flatMap { it.archiveFile })) }
tasks.named<Jar>("jar") { exclude("assets/**") }

val verifyGameAssetParity = tasks.register("verifyGameAssetParity") {
    group = "verification"
    description = "Checks every desktop asset against the original Android source bytes."
    dependsOn(gameAssetsJar)
    doLast {
        val originals = linkedMapOf<String, ByteArray>()
        listOf("app/src/main/assets", "world_assets/src/main/assets", "app/src/main/res").forEach { path ->
            val root = rootProject.file(path)
            root.walkTopDown().filter { it.isFile }.forEach { file ->
                val name = file.relativeTo(root).invariantSeparatorsPath
                val bytes = file.readBytes()
                val existing = originals[name]
                check(existing == null || existing.contentEquals(bytes)) { "Conflicting Android asset: $name" }
                originals[name] = bytes
            }
        }
        val staged = layout.buildDirectory.dir("game-assets/assets").get().asFile
        ZipFile(gameAssetsJar.get().archiveFile.get().asFile).use { archive ->
            originals.forEach { (name, bytes) ->
                check(staged.resolve(name).readBytes().contentEquals(bytes)) { "Staged content drift: $name" }
                val entry = checkNotNull(archive.getEntry("assets/$name")) { "Missing packaged asset: $name" }
                check(archive.getInputStream(entry).use { it.readBytes() }.contentEquals(bytes)) { "Packaged content drift: $name" }
            }
        }
        logger.lifecycle("Verified ${originals.size} Android assets in desktop staging and asset archive.")
    }
}
tasks.named("check") { dependsOn(verifyGameAssetParity) }

tasks.register<Sync>("packagePortableWindows") {
    dependsOn("createDistributable", verifyGameAssetParity)
    from(layout.buildDirectory.dir("compose/binaries/main/app"))
    from(rootProject.file("docs/desktop/WINDOWS_BUILD.md")) { into("Starborn") }
    into(layout.buildDirectory.dir("distributions/Starborn-Windows-portable"))
}
