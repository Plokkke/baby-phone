package fr.crntech.babyphone.shared

/**
 * Pairing QR payload: `https://host/pair#s=<secret>`.
 * The secret lives in the fragment, which browsers never send to the server.
 */
object PairingLink {
    const val PATH = "/pair"
    private const val SECRET_PARAM = "s"

    fun build(publicUrl: String, secret: PairingSecret) =
        "${publicUrl.trimEnd('/')}$PATH#$SECRET_PARAM=${secret.encode()}"

    fun parse(link: String): PairingSecret? {
        if (!link.substringBefore('#').trimEnd('/').endsWith(PATH)) return null
        return link.substringAfter('#', missingDelimiterValue = "")
            .split('&')
            .map { it.split('=', limit = 2) }
            .firstOrNull { it.size == 2 && it[0] == SECRET_PARAM }
            ?.let { PairingSecret.decode(it[1]) }
    }
}
