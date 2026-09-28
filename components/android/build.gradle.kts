plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.play.publisher)
}

val publicUrl = providers.gradleProperty("babyphone.publicUrl").get()
val appVersion = providers.gradleProperty("babyphone.version").get()

/** 1.2.3 → 1002003: monotonic as long as minor/patch stay below 1000. */
fun versionCodeOf(semver: String) = semver.substringBefore('-').split('.').map(String::toInt)
    .let { (major, minor, patch) -> major * 1_000_000 + minor * 1_000 + patch }
    .coerceAtLeast(1)

/** Release signing comes from CI secrets; without them release builds stay unsigned. */
val releaseKeystore = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
fun secret(name: String) = providers.environmentVariable(name).get()

android {
    namespace = "fr.crntech.babyphone"
    compileSdk = 37

    defaultConfig {
        applicationId = providers.gradleProperty("babyphone.applicationId").get()
        minSdk = 29
        targetSdk = 37
        versionCode = versionCodeOf(appVersion)
        versionName = appVersion
        buildConfigField("String", "PUBLIC_URL", "\"$publicUrl\"")
        manifestPlaceholders["publicHost"] = uri(publicUrl).host
    }

    signingConfigs {
        if (releaseKeystore != null) create("release") {
            storeFile = file(releaseKeystore)
            storePassword = secret("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = secret("ANDROID_KEY_ALIAS")
            keyPassword = secret("ANDROID_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
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

    packaging {
        resources.excludes += setOf("META-INF/INDEX.LIST", "META-INF/io.netty.versions.properties")
    }
}

play {
    serviceAccountCredentials = file(providers.environmentVariable("PLAY_CREDENTIALS_PATH").orElse("play-credentials.json"))
    track = "internal"
    defaultToAppBundles = true
}

dependencies {
    implementation(project(":client"))
    implementation(libs.coroutines.android)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
    implementation(libs.code.scanner)
}
