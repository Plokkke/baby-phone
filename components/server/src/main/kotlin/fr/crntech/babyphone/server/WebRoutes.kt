package fr.crntech.babyphone.server

import fr.crntech.babyphone.shared.Endpoints
import fr.crntech.babyphone.shared.PairingLink
import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

fun Route.webRoutes(config: ServerConfig) {
    val assetLinks = assetLinks(config)
    val pairPage = resourceText("pair.html")

    val health = buildJsonObject { put("status", "ok"); put("version", config.version) }.toString()

    get(Endpoints.HEALTH_PATH) { call.respondText(health, ContentType.Application.Json) }
    get("/.well-known/assetlinks.json") { call.respondText(assetLinks, ContentType.Application.Json) }
    get(PairingLink.PATH) { call.respondText(pairPage, ContentType.Text.Html) }
}

/** Android App Links verification: lets the system camera open pairing links straight in the app. */
private fun assetLinks(config: ServerConfig) = buildJsonArray {
    addJsonObject {
        putJsonArray("relation") { add("delegate_permission/common.handle_all_urls") }
        putJsonObject("target") {
            put("namespace", "android_app")
            put("package_name", config.androidPackage)
            putJsonArray("sha256_cert_fingerprints") { config.androidCertFingerprints.forEach(::add) }
        }
    }
}.toString()

private fun resourceText(name: String) =
    requireNotNull(object {}.javaClass.classLoader.getResource(name)) { "Missing resource $name" }.readText()
