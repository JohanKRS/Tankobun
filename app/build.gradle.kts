import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use(::load)
    }
}

android {
    namespace = "com.tankobun.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.tankobun.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 2
        versionName = "0.1.1"

        val clientId = localProperties.getProperty("anilistClientId", "")
        buildConfigField("String", "ANILIST_CLIENT_ID", "\"$clientId\"")
        buildConfigField("String", "ANILIST_REDIRECT_URI", "\"tankobun://auth/anilist\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:anilist"))
    implementation(project(":core:database"))
    implementation(project(":core:extensions"))
    implementation(project(":core:reader"))
    implementation(project(":core:downloads"))
    implementation(project(":core:sync"))

    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.work.runtime)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}
