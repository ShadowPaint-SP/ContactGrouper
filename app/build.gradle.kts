import org.gradle.api.GradleException
import org.gradle.api.tasks.Copy
import java.util.Properties

val releaseVersionName = "1.0.0"
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.isFile) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}
val hasReleaseSigningConfig = listOf(
    "storeFile",
    "storePassword",
    "keyAlias",
    "keyPassword",
).all(keystoreProperties::containsKey)
val releaseArtifactTasks = setOf(
    "assembleRelease",
    "bundleRelease",
    "copyReleaseBundleForPlay",
    "packageReleaseBundle",
    "signReleaseBundle",
)

gradle.taskGraph.whenReady {
    if (!hasReleaseSigningConfig && allTasks.any { it.name in releaseArtifactTasks }) {
        throw GradleException(
            "Release signing is not configured. Create keystore.properties from " +
                "keystore.properties.example before building a Play release."
        )
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

android {
    namespace = "de.drvlabs.contactgrouper"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.drvlabs.contactgrouper"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = releaseVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
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

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.compose.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.register<Copy>("copyReleaseBundleForPlay") {
    group = "distribution"
    description = "Copies the signed release App Bundle to a Play Console upload filename."
    dependsOn("bundleRelease")

    from(layout.buildDirectory.file("outputs/bundle/release/app-release.aab"))
    into(rootProject.layout.projectDirectory.dir("app/release"))
    rename { "contactgrouper-v$releaseVersionName.aab" }
}

afterEvaluate {
    tasks.named("bundleRelease") {
        finalizedBy("copyReleaseBundleForPlay")
    }
}
