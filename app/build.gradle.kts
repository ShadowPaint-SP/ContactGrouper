import org.gradle.api.GradleException
import java.util.Properties

val releaseVersionCode = providers.gradleProperty("releaseVersionCode").orElse("2").get().toInt()
val releaseVersionName = providers.gradleProperty("releaseVersionName").orElse("1.0.1").get()
require(releaseVersionCode in 1..2_100_000_000) { "Invalid release version code" }
require(releaseVersionName.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+"))) {
    "Release version name must have the form 1.2.3"
}
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.isFile) {
        keystorePropertiesFile.inputStream().use(::load)
    }
    mapOf(
        "storeFile" to "ANDROID_KEYSTORE_FILE",
        "storePassword" to "ANDROID_KEYSTORE_PASSWORD",
        "keyAlias" to "ANDROID_KEY_ALIAS",
        "keyPassword" to "ANDROID_KEY_PASSWORD",
    ).forEach { (property, environmentVariable) ->
        providers.environmentVariable(environmentVariable).orNull?.let { setProperty(property, it) }
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
    "packageReleaseBundle",
    "signReleaseBundle",
)

gradle.taskGraph.whenReady {
    if (!hasReleaseSigningConfig && allTasks.any { it.name in releaseArtifactTasks }) {
        throw GradleException(
            "Release signing is not configured. Create keystore.properties from " +
                "keystore.properties.example or set the ANDROID_KEYSTORE_* and " +
                "ANDROID_KEY_* environment variables before building a Play release."
        )
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

base {
    archivesName = "contactgrouper-v$releaseVersionName"
}

android {
    namespace = "de.drvlabs.contactgrouper"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.drvlabs.contactgrouper"
        minSdk = 29
        targetSdk = 36
        versionCode = releaseVersionCode
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
