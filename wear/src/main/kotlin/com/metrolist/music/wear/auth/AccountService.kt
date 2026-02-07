package com.metrolist.music.wear.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for fetching account information using TV client.
 * OAuth tokens from Device Code Flow only work with TV clients (TVHTML5).
 */
@Singleton
class AccountService @Inject constructor(
    private val httpClient: HttpClient,
    private val tokenManager: TokenManager
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Fetch account info using the TV client's accounts_list endpoint.
     * This endpoint works with OAuth bearer tokens (unlike account_menu which needs cookies).
     */
    suspend fun getAccountInfo(): Result<AccountInfo> = runCatching {
        val accessToken = tokenManager.getValidToken()
            ?: throw Exception("No valid token available")

        val requestBody = AccountsListRequest(
            context = TvClientContext(
                client = TvClientContext.Client(
                    clientName = "TVHTML5",
                    clientVersion = "7.20260124.00.00"
                )
            ),
            accountReadMask = AccountReadMask(
                returnOwner = true,
                returnBrandAccounts = true,
                returnPersonaAccounts = false
            )
        )

        val response = httpClient.post("https://www.youtube.com/youtubei/v1/account/accounts_list") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            header("X-Goog-Api-Format-Version", "1")
            header("X-YouTube-Client-Name", "7") // TVHTML5 client ID
            header("X-YouTube-Client-Version", "7.20260124.00.00")
            setBody(json.encodeToString(AccountsListRequest.serializer(), requestBody))
        }

        if (response.status.isSuccess()) {
            val body = response.bodyAsText()
            Timber.d("Account info response length: ${body.length}")

            // Parse manually due to varying response structure
            parseAccountInfoResponse(body)
        } else {
            val errorBody = response.bodyAsText()
            Timber.e("Account info request failed: ${response.status} - $errorBody")
            throw Exception("Failed to get account info: ${response.status}")
        }
    }

    private fun parseAccountInfoResponse(responseBody: String): AccountInfo {
        val jsonElement = json.parseToJsonElement(responseBody)
        val root = jsonElement.jsonObject

        Timber.d("Account response keys: ${root.keys}")

        // Try to find account info in various paths
        var name = "Unknown"
        var email: String? = null
        var thumbnailUrl: String? = null

        // The response structure can vary - contents might be an array or object
        val contentsElement = root["contents"]

        // Path 1: contents is an array (TV client response)
        if (contentsElement != null) {
            try {
                val contentsArray = contentsElement.jsonArray
                Timber.d("Contents is array with ${contentsArray.size} items")

                contentsArray.forEach { contentItem ->
                    val itemObj = contentItem.jsonObject
                    Timber.d("Content item keys: ${itemObj.keys}")

                    // Try accountSectionListRenderer
                    itemObj["accountSectionListRenderer"]?.jsonObject?.let { sectionList ->
                        parseAccountSectionList(sectionList)?.let { info ->
                            name = info.name
                            email = info.email
                            thumbnailUrl = info.thumbnailUrl
                        }
                    }

                    // Try accountItemRenderer directly
                    itemObj["accountItemRenderer"]?.jsonObject?.let { renderer ->
                        parseAccountItemRenderer(renderer)?.let { info ->
                            name = info.name
                            email = info.email
                            thumbnailUrl = info.thumbnailUrl
                        }
                    }
                }
            } catch (e: Exception) {
                // contents might be an object instead
                Timber.d("Contents is not array, trying as object")
                try {
                    val contentsObj = contentsElement.jsonObject
                    Timber.d("Contents keys: ${contentsObj.keys}")

                    contentsObj["accountSectionListRenderer"]?.jsonObject?.let { sectionList ->
                        parseAccountSectionList(sectionList)?.let { info ->
                            name = info.name
                            email = info.email
                            thumbnailUrl = info.thumbnailUrl
                        }
                    }
                } catch (e2: Exception) {
                    Timber.w(e2, "Could not parse contents")
                }
            }
        }

        // Path 2: Try header if available
        if (name == "Unknown") {
            val header = root["header"]?.jsonObject
            header?.get("c4TabbedHeaderRenderer")?.jsonObject?.let { renderer ->
                name = renderer["title"]?.jsonPrimitive?.contentOrNull ?: name
                thumbnailUrl = renderer["avatar"]?.jsonObject
                    ?.get("thumbnails")?.jsonArray
                    ?.lastOrNull()?.jsonObject
                    ?.get("url")?.jsonPrimitive?.contentOrNull
            }
        }

        // Path 3: Try selectText for account name
        if (name == "Unknown") {
            root["selectText"]?.jsonObject?.let { selectText ->
                name = selectText["simpleText"]?.jsonPrimitive?.contentOrNull
                    ?: selectText["runs"]?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.contentOrNull
                    ?: name
            }
        }

        Timber.d("Parsed account: name=$name, email=$email")

        if (name == "Unknown") {
            throw Exception("Could not parse account info from response")
        }

        return AccountInfo(
            name = name,
            email = email,
            thumbnailUrl = thumbnailUrl
        )
    }

    private fun parseAccountSectionList(sectionList: JsonObject): AccountInfo? {
        try {
            Timber.d("accountSectionListRenderer keys: ${sectionList.keys}")
            val sections = sectionList["contents"]?.jsonArray
            sections?.forEach { section ->
                val sectionObj = section.jsonObject
                val itemSection = sectionObj["accountItemSectionRenderer"]?.jsonObject
                val items = itemSection?.get("contents")?.jsonArray
                items?.forEach { item ->
                    val accountItem = item.jsonObject["accountItem"]?.jsonObject
                    if (accountItem != null) {
                        return parseAccountItem(accountItem)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error parsing accountSectionListRenderer")
        }
        return null
    }

    private fun parseAccountItemRenderer(renderer: JsonObject): AccountInfo? {
        try {
            Timber.d("accountItemRenderer keys: ${renderer.keys}")
            return parseAccountItem(renderer)
        } catch (e: Exception) {
            Timber.w(e, "Error parsing accountItemRenderer")
        }
        return null
    }

    private fun parseAccountItem(accountItem: JsonObject): AccountInfo? {
        try {
            Timber.d("Found accountItem: ${accountItem.keys}")

            var name = "Unknown"
            var email: String? = null
            var thumbnailUrl: String? = null

            // Get name
            accountItem["accountName"]?.jsonObject?.let { nameObj ->
                name = nameObj["simpleText"]?.jsonPrimitive?.contentOrNull
                    ?: nameObj["runs"]?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.contentOrNull
                    ?: name
            }

            // Get email/byline
            accountItem["accountByline"]?.jsonObject?.let { bylineObj ->
                email = bylineObj["simpleText"]?.jsonPrimitive?.contentOrNull
                    ?: bylineObj["runs"]?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.contentOrNull
            }

            // Get thumbnail
            accountItem["accountPhoto"]?.jsonObject?.let { photoObj ->
                thumbnailUrl = photoObj["thumbnails"]?.jsonArray
                    ?.lastOrNull()?.jsonObject
                    ?.get("url")?.jsonPrimitive?.contentOrNull
            }

            if (name != "Unknown") {
                return AccountInfo(name, email, thumbnailUrl)
            }
        } catch (e: Exception) {
            Timber.w(e, "Error parsing accountItem")
        }
        return null
    }
}

// Request models
@Serializable
data class AccountsListRequest(
    val context: TvClientContext,
    val accountReadMask: AccountReadMask
)

@Serializable
data class TvClientContext(
    val client: Client
) {
    @Serializable
    data class Client(
        val clientName: String,
        val clientVersion: String,
        val hl: String = "en",
        val gl: String = "US"
    )
}

@Serializable
data class AccountReadMask(
    val returnOwner: Boolean = true,
    val returnBrandAccounts: Boolean = true,
    val returnPersonaAccounts: Boolean = false
)

data class AccountInfo(
    val name: String,
    val email: String?,
    val thumbnailUrl: String?
)
