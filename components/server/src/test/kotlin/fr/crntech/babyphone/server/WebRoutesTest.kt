package fr.crntech.babyphone.server

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assumptions.assumeTrue
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebRoutesTest {
    private val config = ServerConfig(playStoreUrl = "https://play.example/app")

    private fun webTest(block: suspend (HttpClient) -> Unit) = testApplication {
        application { module(config) }
        block(createClient { followRedirects = false })
    }

    private suspend fun HttpClient.visit(path: String, userAgent: String) = get(path) {
        header(HttpHeaders.UserAgent, userAgent)
        header("Sec-Fetch-Mode", "navigate")
    }

    private fun HttpResponse.assertRedirect(location: String) {
        assertEquals(HttpStatusCode.Found, status)
        assertEquals(location, headers[HttpHeaders.Location])
    }

    @Test
    fun `desktop pairing links open the web client`() = webTest { client ->
        client.visit("/pair", DESKTOP).assertRedirect("/web/")
    }

    @Test
    fun `mobile pairing links get the app hand-off page`() = webTest { client ->
        val page = client.visit("/pair", ANDROID)
        assertEquals(HttpStatusCode.OK, page.status)
        val html = page.bodyAsText()
        assertContains(html, "\"fr.crntech.babyphone\"")
        assertContains(html, "\"https://play.example/app\"")
    }

    @Test
    fun `mobile browsers are sent back to the app`() = webTest { client ->
        client.visit("/web/", ANDROID).assertRedirect("/pair")
        client.visit("/web", IPHONE).assertRedirect("/pair")
    }

    @Test
    fun `desktop browsers get the web client`() = webTest { client ->
        client.visit("/web", DESKTOP).assertRedirect("/web/")
        assumeTrue(javaClass.classLoader.getResource("web/index.html") != null, "web client not bundled")
        val index = client.visit("/web/", DESKTOP)
        assertEquals(ContentType.Text.Html, index.contentType()?.withoutParameters())
        assertContains(index.bodyAsText(), "babyphone.js")
    }

    @Test
    fun `wasm bundles are served compressed and cached for good`() = webTest { client ->
        val wasm = javaClass.classLoader.getResource("web")?.let { java.io.File(it.toURI()).list()?.firstOrNull { f -> f.endsWith(".wasm") } }
        assumeTrue(wasm != null, "web client not bundled")
        val response = client.get("/web/$wasm") { header(HttpHeaders.AcceptEncoding, "gzip") }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("gzip", response.headers[HttpHeaders.ContentEncoding])
        assertTrue(response.headers[HttpHeaders.CacheControl].orEmpty().contains("max-age=31536000"))
    }

    private companion object {
        const val DESKTOP = "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_0) AppleWebKit/537.36 Chrome/130.0 Safari/537.36"
        const val ANDROID = "Mozilla/5.0 (Linux; Android 12; GM1913) AppleWebKit/537.36 Chrome/130.0 Mobile Safari/537.36"
        const val IPHONE = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148"
    }
}
