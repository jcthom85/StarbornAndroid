plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.protobuf)
}

kotlin {
    jvmToolchain(21)
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:3.25.1" }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins { maybeCreate("java").apply { option("lite") } }
        }
    }
}

dependencies {
    api(libs.kotlinx.serialization.json)
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    api("com.squareup.moshi:moshi:1.15.1")
    api("com.squareup.moshi:moshi-kotlin:1.15.1")
    api("org.json:json:20240303")
    api(libs.androidx.datastore.core)
    api(libs.protobuf.javalite)
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
