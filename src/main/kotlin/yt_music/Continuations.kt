package yt_music

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kong.unirest.HttpResponse
import kotlin.math.min
import kotlin.reflect.KFunction1

class Continuations : Navigation() {

    fun getContinuations(
        _results: JsonObject,
        continuationType: String,
        limit: Int?,
        requestFunc: KFunction1<String, HttpResponse<JsonElement>>,
        parseFunc: KFunction1<JsonElement, JsonElement>,
        ctokenPath: String = "",
        reloadable: Boolean = false,
    ): JsonArray {
        var results = _results
        val items = JsonArray()
        while (results.has("continuations") && (limit == null || items.size() < limit)) {
            val additionalParams = if (reloadable) {
                getReloadableContinuationParams(results)
            } else {
                getContinuationParams(results, ctokenPath)
            }
            val httpResponse = requestFunc(additionalParams)
            val response = JsonParser.parseString(httpResponse.body.toString()).asJsonObject
            if (response.has("continuationContents")) {
                results = response.getAsJsonObject("continuationContents").getAsJsonObject(continuationType)
            } else {
                break
            }

            // Possible that this should be json arrays!
            val contents = getContinuationContents(results, parseFunc)
            if (contents.isJsonObject && contents.asJsonObject.size() == 0) {
                break
            }
            if (contents.isJsonArray && contents.asJsonArray.size() == 0) {
                break
            }

            items.add(contents)
        }

        return items
    }

    fun getValidatedContinuations(
        results: JsonObject,
        continuationType: String,
        limit: Int,
        perPage: Int,
        requestFunc: KFunction1<String, HttpResponse<JsonElement>>,
        parseFunc: KFunction1<JsonElement, JsonElement>,
        ctokenPath: String = "",
    ): JsonArray {
        val items = JsonArray()
        var _results = results
        while (_results.has("continuations") && items.size() < limit) {
            val additionalParams = getContinuationParams(results, ctokenPath)
            fun wrappedParseFunc(rawResponse: JsonObject) =
                getParsedContinuationItems(rawResponse, parseFunc, continuationType)

            fun validateFunc(parsed: JsonObject) = validateResponse(parsed, perPage, limit, items.size())
            val response = resendRequestUntilParsedResponseIsValid(
                requestFunc,
                additionalParams,
                ::wrappedParseFunc,
                ::validateFunc,
                3
            )

            _results = response.getAsJsonObject("results")
            items.addAll(response.getAsJsonArray("parsed"))
        }
        return items
    }

    fun getParsedContinuationItems(
        response: JsonObject,
        parseFunc: KFunction1<JsonElement, JsonElement>,
        continuationType: String,
    ): JsonObject {
        val results = response.getAsJsonObject("continuationContents").getAsJsonObject(continuationType)
        return JsonObject().also { json ->
            json.add("results", results)
            json.add("parsed", getContinuationContents(results, parseFunc))
        }
    }

    fun getContinuationParams(results: JsonObject, ctokenPath: String = ""): String {
        val ctoken =
            nav(results, listOf("continuations", "0", "next" + ctokenPath + "ContinuationData", "continuation"))!!
        return getContinuationString(ctoken.asString)
    }

    fun getReloadableContinuationParams(results: JsonObject): String {
        val ctoken = nav(results, listOf("continuations", "0", "reloadContinuationData", "continuation"))!!
        return getContinuationString(ctoken.asString)
    }

    fun getContinuationString(ctoken: String): String {
        return "&ctoken=$ctoken&continuation=$ctoken"
    }

    fun getContinuationContents(
        continuation: JsonObject,
        parseFunc: KFunction1<JsonElement, JsonElement>,
    ): JsonElement {
        if (continuation.has("contents")) {
            return parseFunc(continuation.get("contents"))
        }

        if (continuation.has("items")) {
            return parseFunc(continuation.get("items"))
        }

        return JsonObject()
    }


    fun resendRequestUntilParsedResponseIsValid(
        requestFunc: KFunction1<String, HttpResponse<JsonElement>>,
        requestAdditionalParams: String,
        parseFunc: KFunction1<JsonObject, JsonObject>,
        validateFunc: KFunction1<JsonObject, Boolean>,
        maxRetries: Int,
    ): JsonObject {
        var response = requestFunc(requestAdditionalParams)
        var parsedObject = parseFunc(response.body.asJsonObject).asJsonObject
        var retryCounter = 0
        while (!validateFunc(parsedObject) && retryCounter < maxRetries) {
            response = requestFunc(requestAdditionalParams)
            val attempt = parseFunc(response.body.asJsonObject).asJsonObject
            if (attempt.getAsJsonArray("parsed").size() > parsedObject.getAsJsonArray("parsed").size()) {
                parsedObject = attempt
            }
            retryCounter++
        }

        return parsedObject
    }

    fun validateResponse(response: JsonObject, perPage: Int, limit: Int, currentCount: Int): Boolean {
        val remainingItemsCount = limit - currentCount
        val expectedItemsCount = min(perPage, remainingItemsCount)

        // response is valid, if it has lesse items then minimal expected count
        return response.getAsJsonArray("parsed").size() >= expectedItemsCount
    }
}