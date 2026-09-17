plugins {
    kotlin("jvm") version "2.4.10"
    kotlin("plugin.serialization") version "2.4.10"
    id("com.gradleup.shadow") version "9.6.1"
    id("io.ktor.plugin") version "3.5.2"
}

group = "org.appdevncsu"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Scraper
    implementation("com.squareup.okhttp3:okhttp:5.5.0")
    implementation("com.squareup.okhttp3:okhttp-java-net-cookiejar:5.5.0")
    implementation("com.fleeksoft.ksoup:ksoup:0.2.6")

    // Server
    implementation("io.ktor:ktor-server-core:3.5.2")
    implementation("io.ktor:ktor-server-core-jvm:3.5.2")
    implementation("io.ktor:ktor-server-cio:3.5.2")
    implementation("io.ktor:ktor-server-content-negotiation:3.5.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.5.2")
    implementation("io.ktor:ktor-server-call-logging:3.5.2")
    implementation("io.ktor:ktor-server-compression:3.5.2")
    implementation("io.ktor:ktor-server-conditional-headers:3.5.2")
    implementation("ch.qos.logback:logback-classic:1.6.3")
    implementation("org.quartz-scheduler:quartz:2.5.0")

    // Shared
    implementation("net.sf.biweekly:biweekly:0.6.8") { // iCalendar ICS feed parsing
        // We don't need any code from biweekly that touches the Jackson dependency
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-core")
    }
    implementation("org.xerial:sqlite-jdbc:3.53.2.1")
    implementation("org.jetbrains.exposed:exposed-core:1.4.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:1.4.0")
    implementation("org.jetbrains.exposed:exposed-java-time:1.4.0")
    implementation("org.jetbrains.exposed:exposed-json:1.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.shadowJar {
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

application {
    mainClass = "org.appdevncsu.foodfinder.MainKt"
}

kotlin {
    jvmToolchain(24)
}
