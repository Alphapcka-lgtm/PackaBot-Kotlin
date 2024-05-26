package yt_music

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kong.unirest.Cookie
import kong.unirest.HttpResponse
import kong.unirest.Proxy
import kong.unirest.UnirestInstance
import java.util.concurrent.TimeUnit

open class Requester(
    unirest: UnirestInstance,
    private val headers: Map<String, String>,
    private val context: JsonObject,
    private val proxy: Proxy? = null,
) {

    val unirest: UnirestInstance
    val cookies: MutableList<Cookie> = arrayListOf(Cookie("CONSENT", "YES+1"))

    init {
        if (unirest.config().connectionTimeout <= 30) {
            unirest.config().connectTimeout(TimeUnit.SECONDS.toMillis(30).toInt())
        }
        this.unirest = unirest
    }

    fun sendRequest(
        endpoint: String,
        body: JsonObject,
        additionalParams: String = "",
    ): HttpResponse<JsonElement> {
        body.add("context", context.getAsJsonObject("context"))
        val params = YTM_PARAMS

        //can add browser auth later

        val response =
            unirest.post(YTM_BASE_API + endpoint + params + additionalParams).body(body).headers(headers).proxy(proxy)
                .cookie(cookies).asObject(JsonElement::class.java)

        if (response.status >= 400) {
            val message = "Sever returned HTTP ${response.status}: ${response.statusText}.\n"
            val error = response.body.asJsonObject.getAsJsonObject("error").getAsJsonPrimitive("message").asString
            throw Exception(message + error)
        }

        return response
    }

    fun sendGetRequest(url: String, params: Map<String, Any>? = null): HttpResponse<String> {
        val request = unirest.get(url).headers(headers).cookie(cookies)
        if (params != null) {
            request.queryString(params)
        }
        if (proxy != null) {
            request.proxy(proxy)
        }
        val response = request.asString()
        return response
    }
}