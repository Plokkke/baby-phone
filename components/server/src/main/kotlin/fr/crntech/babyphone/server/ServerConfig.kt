package fr.crntech.babyphone.server

data class ServerConfig(
    val version: String = "dev",
    val port: Int = 8080,
    val androidPackage: String = "fr.crntech.babyphone",
    val androidCertFingerprints: List<String> = emptyList(),
    val maxMembersPerRoom: Int = 8,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()) = ServerConfig().run {
            copy(
                version = env["APP_VERSION"] ?: version,
                port = env["PORT"]?.toIntOrNull() ?: port,
                androidPackage = env["ANDROID_PACKAGE"] ?: androidPackage,
                androidCertFingerprints = env["ANDROID_CERT_SHA256"]
                    ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)
                    ?: androidCertFingerprints,
                maxMembersPerRoom = env["MAX_MEMBERS_PER_ROOM"]?.toIntOrNull() ?: maxMembersPerRoom,
            )
        }
    }
}
