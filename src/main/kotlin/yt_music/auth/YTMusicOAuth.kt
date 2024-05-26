package yt_music.auth

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kong.unirest.HttpResponse
import kong.unirest.Proxy
import kong.unirest.UnirestInstance
import yt_music.utils.JsonFormatter
import yt_music.utils.update
import yt_music.*
import java.nio.file.Files
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path

/**
 * OAuth's implementation for YouTube Music based on YouTube TV
 *
 * @author sigma67, ported to kotlin by alphapacka
 */
class YTMusicOAuth(private val unirest: UnirestInstance, proxy: Proxy? = null) {

    init {
        if (proxy != null) unirest.config().proxy(proxy)
    }

    companion object {
        fun dumpToken(token: JsonObject, filepath: String) {
            token.addProperty(
                "expires_at",
                TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + token.get("expires_in").asLong
            )

            Files.writeString(Path(filepath), JsonFormatter.formatToPrettyString(token))
        }
    }

    fun sendRequest(url: String, data: JsonObject): HttpResponse<JsonElement> {
        data.addProperty("client", OAUTH_CLIENT_ID)
        val headers = mapOf<String, String>("User-Agent" to OAUTH_USER_AGENT)
        return unirest.post(url).body(data).headers(headers).asObject(JsonElement::class.java)
    }

    fun getCode(): JsonObject {
        val data = JsonObject().also { json -> json.addProperty("scope", OAUTH_SCOPE) }
        val codeResponse = sendRequest(OAUTH_CODE_URL, data)
        val responseJson = codeResponse.body.asJsonObject
        val url =
            "${responseJson.get("verification_url").asString}?user_code={${responseJson.get("user_code").asString}}"

        print("Go to $url, finish the login flow and press Enter when done, Ctrl-C to abort.")
        readln()
        return responseJson
    }

    fun getTokenFromCode(deviceCode: String): JsonObject {
        val data = JsonObject()
        data.asMap().putAll(
            mapOf(
                "client_secret" to JsonPrimitive(OAUTH_CLIENT_SECRET),
                "grant_type" to JsonPrimitive("http://oauth.net/grant_type/device/1.0"),
                "code" to JsonPrimitive(deviceCode)
            )
        )

        return sendRequest(OAUTH_TOKEN_URL, data).body.asJsonObject
    }

    fun refreshToken(refreshToken: String): JsonObject {
        val data = JsonObject()
        data.asMap().putAll(
            mapOf(
                "client_secret" to JsonPrimitive(
                    OAUTH_CLIENT_SECRET
                ),
                "grant_type" to JsonPrimitive("refresh_token"),
                "refresh_token" to JsonPrimitive(refreshToken)
            )
        )

        return sendRequest(OAUTH_TOKEN_URL, data).body.asJsonObject
    }

    fun setup(filepath: Optional<String>): JsonObject {
        val code = getCode()
        val token = getTokenFromCode(code.get("device_code").asString)
        if (filepath.isPresent) {
            dumpToken(token, filepath.get())
        }

        return token
    }

    fun loadHeaders(token: JsonObject, filepath: String): HashMap<String, String> {
        val headers = initializeHeaders()
        if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) > (token.get("expires_at").asLong - 3600L)) {
            token.update(refreshToken(token.get("refresh_token").asString))
            dumpToken(token, filepath)
        }

        headers["Authorization"] = "${token["token_type"].asString} ${token["access_token"]}"
        headers["Content-Type"] = "application/json"
        headers["X-Goog-Request-Time"] = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()).toString()
        return headers
    }

}