package fr.crntech.babyphone.server

import fr.crntech.babyphone.shared.Endpoints
import fr.crntech.babyphone.shared.PairingLink
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.header
import io.ktor.server.request.userAgent
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

const val WEB_PATH = "/web"
private const val WEB_RESOURCES = "web"

fun Route.webRoutes(config: ServerConfig) {
    val assetLinks = assetLinks(config)
    val health = buildJsonObject { put("status", "ok"); put("version", config.version) }.toString()
    val pairPage = resourceText("pair.html")!!
        .replace("{{ANDROID_PACKAGE}}", JsonPrimitive(config.androidPackage).toString())
        .replace("{{PLAY_STORE_URL}}", (config.playStoreUrl?.let(::JsonPrimitive) ?: JsonNull).toString())
    val webIndex = resourceText("$WEB_RESOURCES/index.html")

    get(Endpoints.HEALTH_PATH) { call.respondText(health, ContentType.Application.Json) }
    get("/.well-known/assetlinks.json") { call.respondText(assetLinks, ContentType.Application.Json) }

    // Browsers keep the #fragment across redirects, so the pairing secret follows without reaching us.
    get(PairingLink.PATH) {
        if (call.request.isDesktopNavigation()) call.respondRedirect("$WEB_PATH/")
        else call.respondText(pairPage, ContentType.Text.Html)
    }

    route(WEB_PATH) {
        get { call.respondRedirect(if (call.request.isMobile()) PairingLink.PATH else "$WEB_PATH/") }
        get("/") {
            when {
                call.request.isMobile() -> call.respondRedirect(PairingLink.PATH)
                webIndex == null -> call.respondText("Web client not bundled", status = HttpStatusCode.NotFound)
                else -> call.respondRevalidated(webIndex)
            }
        }
        staticResources("/", WEB_RESOURCES, index = null) {
            // Wasm bundles have content-hashed names: cache them for good, revalidate the rest.
            cacheControl { resource ->
                if (resource.path.endsWith(".wasm")) listOf(IMMUTABLE) else listOf(CacheControl.NoCache(null))
            }
        }
    }
}

private suspend fun RoutingCall.respondRevalidated(html: String) {
    response.headers.append(HttpHeaders.CacheControl, "no-cache")
    respondText(html, ContentType.Text.Html)
}

private val IMMUTABLE = CacheControl.MaxAge(maxAgeSeconds = 365 * 24 * 3600, visibility = CacheControl.Visibility.Public)

private fun ApplicationRequest.isMobile(): Boolean {
    val agent = userAgent().orEmpty()
    return header("Sec-CH-UA-Mobile") == "?1" || MOBILE_AGENT.containsMatchIn(agent)
}

/** Only top-level navigations are redirected; `Sec-Fetch-Mode` is absent on older browsers. */
private fun ApplicationRequest.isDesktopNavigation() =
    !isMobile() && (header("Sec-Fetch-Mode") ?: "navigate") == "navigate"

private val MOBILE_AGENT = Regex("Android|iPhone|iPad|iPod|Mobi")

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

private fun resourceText(name: String) = object {}.javaClass.classLoader.getResource(name)?.readText()
