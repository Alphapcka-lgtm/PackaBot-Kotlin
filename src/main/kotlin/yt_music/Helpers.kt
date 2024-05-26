package yt_music

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kong.unirest.HttpResponse
import kong.unirest.json.JSONObject
import kotlin.reflect.KFunction2

fun initializeHeaders(): HashMap<String, String> {
    val map = HashMap<String, String>()
    map.putAll(
        arrayOf(
            "user-agent" to USER_AGENT,
            "accept" to "*/*",
            "content-type" to "application/json",
            "origin" to YTM_DOMAIN
        )
    )
    return map
}

fun initializeContext(): JsonObject = JsonParser.parseString(
    "{context:{client:{clientName:WEB_REMIX,clientVersion:1.${
        String.format(
            "%1\$tY%1\$tm%1\$td",
            System.currentTimeMillis()
        )
    }.01.00},user:{}}}"
).asJsonObject

fun getVisitorId(requestFunction: KFunction2<String, Map<String, Any>?, HttpResponse<String>>): Pair<String, String> {
    val response = requestFunction(YTM_DOMAIN, null)
    val body = response.body
    val matches = Regex.fromLiteral("ytcfg\\.set\\s*\\(\\s*({.+?})\\s*\\)\\s*;").findAll(body)
    var visitorId = ""
    if (matches.count() > 0) {
        val ytcfg = JSONObject(body)
        visitorId = ytcfg.getString("VISITOR_DATA")
    }

    return "X-Goog-Visitor-Id" to visitorId
}

fun toInt(str: String): Int {
    val string = str
    val intStr = "\\D".toRegex().replace(string, "")
    val intValue: Int
    intValue = try {
        intStr.toInt()
    } catch (e: NumberFormatException) {
        intStr.replace(",", "").toInt()
    }

    return intValue
}

fun sumTotalDuration(item: JsonObject): Int {
    if (!item.has("tracks")) {
        return 0
    }

    return item.getAsJsonArray("tracks").asList().stream()
        .mapToInt { track -> if (track.asJsonObject.has("duration_seconds")) track.asJsonObject.getAsJsonPrimitive("duration_seconds").asInt else 0 }
        .sum()
}