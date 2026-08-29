import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val anthropicApiKey = localProperties.getProperty("ANTHROPIC_API_KEY", "")
val anthropicFastModel = localProperties.getProperty(
    "ANTHROPIC_FAST_MODEL",
    "claude-haiku-4-5-20251001",
)
val anthropicReportModel = localProperties.getProperty(
    "ANTHROPIC_REPORT_MODEL",
    "claude-sonnet-5",
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
        buildConfigField("String", "ANTHROPIC_API_KEY", anthropicApiKey.asBuildConfigString())
        buildConfigField("String", "ANTHROPIC_FAST_MODEL", anthropicFastModel.asBuildConfigString())
        buildConfigField("String", "ANTHROPIC_REPORT_MODEL", anthropicReportModel.asBuildConfigString())
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
