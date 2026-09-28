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
        // Toolchains the Kotlin/Wasm plugin downloads for browser builds and tests
        distribution("https://nodejs.org/dist", "v[revision]/[artifact](-v[revision]-[classifier]).[ext]", "org.nodejs")
        distribution("https://github.com/yarnpkg/yarn/releases/download", "v[revision]/[artifact](-v[revision]).[ext]", "com.yarnpkg")
        distribution(
            "https://github.com/WebAssembly/binaryen/releases/download",
            "version_[revision]/[module]-version_[revision]-[classifier].[ext]",
            "com.github.webassembly",
        )
    }
}

fun RepositoryHandler.distribution(url: String, pattern: String, group: String) = exclusiveContent {
    forRepository {
        ivy(url) {
            patternLayout { artifact(pattern) }
            metadataSources { artifact() }
        }
    }
    filter { includeGroup(group) }
}

// `-PskipAndroid` lets the server build without an Android SDK (Docker image).
val androidModules = if (providers.gradleProperty("skipAndroid").isPresent) emptyList() else listOf("client", "android", "web")

(listOf("shared", "server") + androidModules).forEach { name ->
    include(":$name")
    project(":$name").projectDir = file("components/$name")
}
