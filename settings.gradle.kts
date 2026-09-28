rootProject.name = "baby-phone"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

// `-PskipAndroid` lets the server build without an Android SDK (Docker image).
val androidModules = if (providers.gradleProperty("skipAndroid").isPresent) emptyList() else listOf("android")

(listOf("shared", "server") + androidModules).forEach { name ->
    include(":$name")
    project(":$name").projectDir = file("components/$name")
}
