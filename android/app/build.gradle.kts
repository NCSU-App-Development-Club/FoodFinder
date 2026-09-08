import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.kotlin.dsl.detekt
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "org.appdevncsu.foodfinder"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.appdevncsu.foodfinder"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing: reads android/keystore.properties first,
    // then falls back to env vars (for CI):
    //   RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD,
    //   RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD
    // If neither is present, the release build stays unsigned.
    val keystorePropsFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties()
    if (keystorePropsFile.exists()) {
        FileInputStream(keystorePropsFile).use { keystoreProps.load(it) }
    }
    val releaseStoreFileProp: String? =
        keystoreProps.getProperty("storeFile") ?: System.getenv("RELEASE_STORE_FILE")
    val releaseStorePassword: String? =
        keystoreProps.getProperty("storePassword") ?: System.getenv("RELEASE_STORE_PASSWORD")
    val releaseKeyAlias: String? =
        keystoreProps.getProperty("keyAlias") ?: System.getenv("RELEASE_KEY_ALIAS")
    val releaseKeyPassword: String? =
        keystoreProps.getProperty("keyPassword") ?: System.getenv("RELEASE_KEY_PASSWORD")
    val hasReleaseSigning = !releaseStoreFileProp.isNullOrBlank() &&
        !releaseStorePassword.isNullOrBlank() &&
        !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank()

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = rootProject.file(releaseStoreFileProp)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            // Enable R8 minification
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
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
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    testImplementation(libs.paparazzi)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.android.compiler)

    detektPlugins(libs.detekt.compose)
}

tasks.withType<Detekt>().configureEach {
    reports.sarif.required = true
    reports.sarif.outputLocation.set(rootProject.layout.projectDirectory.file("detekt-report.sarif"))
}

detekt {
    toolVersion = libs.plugins.detekt.get().version.toString()
    config.setFrom(file("../detekt.yml"))
    buildUponDefaultConfig = true
    basePath = rootProject.projectDir.parentFile.absolutePath
}

val reportMerge = tasks.register<io.gitlab.arturbosch.detekt.report.ReportMergeTask>("reportMerge") {
    output.set(rootProject.layout.buildDirectory.file("reports/detekt/merge.sarif"))
}

reportMerge {
    input.from(tasks.withType<Detekt>().map { it.reports.sarif.outputLocation })
}
