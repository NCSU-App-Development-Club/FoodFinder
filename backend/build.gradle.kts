plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("com.gradleup.shadow") version "9.1.0"
    id("io.ktor.plugin") version "3.4.0"
}

group = "org.appdevncsu"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Scraper
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("com.squareup.okhttp3:okhttp-java-net-cookiejar:5.1.0")
    implementation("com.fleeksoft.ksoup:ksoup:0.2.5")

    // Server
    implementation("io.ktor:ktor-server-core:3.4.0")
    implementation("io.ktor:ktor-server-core-jvm:3.4.0")
    implementation("io.ktor:ktor-server-cio:3.4.0")
    implementation("io.ktor:ktor-server-content-negotiation:3.4.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.0")

    // Shared
    implementation("com.h2database:h2:2.4.240")
    implementation("org.jetbrains.exposed:exposed-core:1.1.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:1.1.0")
    implementation("org.jetbrains.exposed:exposed-java-time:1.1.0")
    implementation("org.jetbrains.exposed:exposed-json:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.shadowJar {
    mergeServiceFiles()
}

application {
    mainClass = "org.appdevncsu.foodfinder.MainKt"
}

kotlin {
    jvmToolchain(24)
}
