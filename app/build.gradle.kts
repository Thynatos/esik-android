import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { input -> load(input) }
    }
}

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val geminiApiKey = localProperties.getProperty(
    "GEMINI_API_KEY",
    localProperties.getProperty("GOOGLE_API_KEY", ""),
)
val geminiFastModel = localProperties.getProperty(
    "GEMINI_FAST_MODEL",
    "gemini-2.5-flash-lite",
)
val geminiProfileModel = localProperties.getProperty(
    "GEMINI_PROFILE_MODEL",
    geminiFastModel,
)
val geminiCardModel = localProperties.getProperty(
    "GEMINI_CARD_MODEL",
    geminiFastModel,
)
val geminiReportModel = localProperties.getProperty(
    "GEMINI_REPORT_MODEL",
    "gemini-3.6-flash",
)

android {
    namespace = "com.thynatos.esik"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.thynatos.esik"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GEMINI_API_KEY", geminiApiKey.asBuildConfigString())
        buildConfigField("String", "GEMINI_FAST_MODEL", geminiFastModel.asBuildConfigString())
        buildConfigField("String", "GEMINI_PROFILE_MODEL", geminiProfileModel.asBuildConfigString())
        buildConfigField("String", "GEMINI_CARD_MODEL", geminiCardModel.asBuildConfigString())
        buildConfigField("String", "GEMINI_REPORT_MODEL", geminiReportModel.asBuildConfigString())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    testImplementation(libs.junit)
}
