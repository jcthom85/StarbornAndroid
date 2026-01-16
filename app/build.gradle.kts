import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "com.example.starborn"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.starborn"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

tasks.register<Exec>("buildGameData") {
    description = "Aggregates JSON data from docs/story into app/src/main/assets"
    group = "starborn"
    workingDir = rootProject.projectDir
    commandLine("python", "tools/build_game_data.py")
}

tasks.named("preBuild") {
    dependsOn("buildGameData")
}

afterEvaluate {
    val testDebugUnitTest = tasks.named<Test>("testDebugUnitTest")
    tasks.register<Test>("runAssetIntegrity") {
        description = "Runs DataIntegrityTest to validate events, cinematics, and tutorial assets."
        group = "verification"
        testClassesDirs = testDebugUnitTest.get().testClassesDirs
        classpath = testDebugUnitTest.get().classpath
        include("**/DataIntegrityTest.class")
        shouldRunAfter(testDebugUnitTest)
    }
}
