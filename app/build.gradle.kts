import java.util.Properties
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations

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

fun signingValue(propertyName: String, envName: String): String? =
    localProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }
        ?: System.getenv(envName)?.takeIf { it.isNotBlank() }

fun configValue(propertyName: String, envName: String, defaultValue: String = ""): String =
    localProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }
        ?: System.getenv(envName)?.takeIf { it.isNotBlank() }
        ?: defaultValue

fun buildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val releaseStoreFile = signingValue("tankobunReleaseStoreFile", "TANKOBUN_RELEASE_STORE_FILE")
val releaseStorePassword = signingValue("tankobunReleaseStorePassword", "TANKOBUN_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = signingValue("tankobunReleaseKeyAlias", "TANKOBUN_RELEASE_KEY_ALIAS")
val releaseKeyPassword = signingValue("tankobunReleaseKeyPassword", "TANKOBUN_RELEASE_KEY_PASSWORD")
val updateManifestUrl = configValue(
    propertyName = "tankobunUpdateManifestUrl",
    envName = "TANKOBUN_UPDATE_MANIFEST_URL",
    defaultValue = "https://johankrs.github.io/Tankobun/updates.json",
)
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

abstract class BundleLicenseAssets : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val licenseFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:javax.inject.Inject
    abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun bundle() {
        fileSystemOperations.sync {
            from(licenseFiles)
            into(outputDirectory.dir("licenses"))
        }
    }
}

val bundleLicenseAssets = tasks.register<BundleLicenseAssets>("bundleLicenseAssets") {
    outputDirectory.set(layout.buildDirectory.dir("generated/licenseAssets"))
    licenseFiles.from(
        rootProject.file("LICENCE.md"),
        rootProject.file("NOTICE.md"),
        rootProject.file("docs/licenses/APACHE-2.0.txt"),
        rootProject.file("docs/licenses/OFL-1.1.txt"),
        rootProject.file("docs/licenses/TABLER-ICONS-MIT.txt"),
        rootProject.file("docs/licenses/COMPOSE-ICONS-MIT.txt"),
    )
}

android {
    namespace = "com.tankobun.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.tankobun.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 49
        versionName = "4.2.1"
        testInstrumentationRunner = "com.tankobun.app.NovelCompatibilityInstrumentation"

        val clientId = configValue("anilistClientId", "ANILIST_CLIENT_ID")
        buildConfigField("String", "ANILIST_CLIENT_ID", "\"$clientId\"")
        buildConfigField("String", "ANILIST_REDIRECT_URI", "\"tankobun://auth/anilist\"")
        buildConfigField("String", "UPDATE_MANIFEST_URL", buildConfigString(updateManifestUrl))
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(requireNotNull(releaseStoreFile))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        debug { applicationIdSuffix = providers.gradleProperty("qaApplicationIdSuffix").orNull }
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            isDebuggable = false
        }
    }
}

androidComponents.onVariants { variant ->
    variant.sources.assets?.addGeneratedSourceDirectory(bundleLicenseAssets, BundleLicenseAssets::outputDirectory)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:anilist"))
    implementation(project(":core:mangabaka"))
    implementation(project(":core:database"))
    implementation(project(":core:extensions"))
    implementation(project(":core:reader"))
    implementation(project(":core:downloads"))
    implementation(project(":core:sync"))

    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.tabler)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.network.cache.control)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.graphics.path)
    implementation(libs.haze)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.work.runtime)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
}
