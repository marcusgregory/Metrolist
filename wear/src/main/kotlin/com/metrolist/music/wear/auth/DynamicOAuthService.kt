package com.metrolist.music.wear.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts OAuth credentials dynamically from YouTube's base.js file.
 * Based on SmartTube/MediaServiceCore approach.
 */
@Singleton
class DynamicOAuthService @Inject constructor(
    private val httpClient: HttpClient
) {
    companion object {
        // Fire TV User-Agent (works best according to SmartTube)
        private const val USER_AGENT_TV = "Mozilla/5.0 (Linux armeabi-v7a; Android 7.1.2; Fire OS 6.0) " +
            "Cobalt/22.lts.3.306369-gold (unlike Gecko) v8/8.8.278.8-jit gles Starboard/13, " +
            "Amazon_ATV_mediatek8695_2019/NS6294 (Amazon, AFTMM, Wireless) com.amazon.firetv.youtube/22.3.r2.v66.0"

        private const val YOUTUBE_TV_URL = "https://www.youtube.com/tv"

        // Regex to extract base.js URL from YouTube TV page
        private val BASE_JS_REGEX = listOf(
            """id="base-js" src="(.*?)"""".toRegex(),
            """\.src = '(.*?m=base)'""".toRegex(),
            """\.src = '(.*?)'; .\\.id = 'base-js'""".toRegex()
        )

        // Regex to extract client_id from base.js
        private val CLIENT_ID_REGEX = """clientId:"([-\w]+\.apps\.googleusercontent\.com)",\n?[$\w]+:"\w+"""".toRegex()

        // Regex to extract client_secret from base.js
        private val CLIENT_SECRET_REGEX = """clientId:"[-\w]+\.apps\.googleusercontent\.com",\n?[$\w]+:"(\w+)"""".toRegex()
    }

    data class OAuthCredentials(
        val clientId: String,
        val clientSecret: String
    )

    /**
     * Fetches OAuth credentials dynamically from YouTube.
     * This approach is used by SmartTube and is more reliable than hardcoded credentials.
     */
    suspend fun fetchCredentials(): Result<OAuthCredentials> = runCatching {
        Timber.d("Fetching OAuth credentials from YouTube...")

        // Step 1: Fetch YouTube TV page
        val tvPageHtml = httpClient.get(YOUTUBE_TV_URL) {
            header("User-Agent", USER_AGENT_TV)
        }.bodyAsText()

        Timber.d("Fetched YouTube TV page, length: ${tvPageHtml.length}")

        // Step 2: Extract base.js URL
        val baseJsPath = extractBaseJsUrl(tvPageHtml)
            ?: throw Exception("Could not find base.js URL in YouTube TV page")

        val baseJsUrl = if (baseJsPath.startsWith("http")) {
            baseJsPath
        } else {
            "https://www.youtube.com$baseJsPath"
        }

        Timber.d("Found base.js URL: $baseJsUrl")

        // Step 3: Fetch base.js
        val baseJsContent = httpClient.get(baseJsUrl) {
            header("User-Agent", USER_AGENT_TV)
        }.bodyAsText()

        Timber.d("Fetched base.js, length: ${baseJsContent.length}")

        // Step 4: Extract credentials
        val clientId = extractClientId(baseJsContent)
            ?: throw Exception("Could not extract client_id from base.js")

        val clientSecret = extractClientSecret(baseJsContent)
            ?: throw Exception("Could not extract client_secret from base.js")

        Timber.d("Extracted credentials - clientId: ${clientId.take(20)}...")

        OAuthCredentials(clientId, clientSecret)
    }

    private fun extractBaseJsUrl(html: String): String? {
        for (regex in BASE_JS_REGEX) {
            val match = regex.find(html)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    private fun extractClientId(jsContent: String): String? {
        val match = CLIENT_ID_REGEX.find(jsContent)
        return match?.groupValues?.get(1)
    }

    private fun extractClientSecret(jsContent: String): String? {
        val match = CLIENT_SECRET_REGEX.find(jsContent)
        return match?.groupValues?.get(1)
    }
}
