package yt_music.parsers

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import yt_music.utils.getBrowseId
import yt_music.utils.getFlexColumnItem
import yt_music.utils.getItemText
import yt_music.utils.parseDuration
import yt_music.Navigation


class SongsParser : Navigation() {

    fun parseSongArtists(data: JsonObject, index: Int): JsonArray? {
        val flexItem = getFlexColumnItem(data, index) ?: return null

        val runs = flexItem.getAsJsonObject("text").getAsJsonArray("runs")
        return parseSongArtistsRuns(runs)
    }

    fun parseSongArtistsRuns(runs: JsonArray): JsonArray {
        val artists = JsonArray()
        for (i in 0 until (runs.size() / 2 + 1)) {
            artists.add(JsonObject().also { json ->
                json.add("name", runs[i * 2].asJsonObject.get("text"))
                json.add("id", nav(runs[i * 2], NAVIGATION_BROWSE_ID, true))
            })
        }

        return artists
    }

    fun parseSongRuns(runs: JsonArray): JsonObject {
        val parsed = JsonParser.parseString("{artists:[]}").asJsonObject
        for (i in 0 until runs.size()) {
            val run = runs.get(i).asJsonObject
            if (i % 2 != 0) {
                // uneven items are always separators
                continue
            }
            val text = run.getAsJsonPrimitive("text").asString
            if (run.has("navigationEndpoint")) {
                val item =
                    JsonParser.parseString(
                        "{\"name\":\"${text}\", \"id\":${
                            nav(
                                run,
                                NAVIGATION_BROWSE_ID,
                                true
                            )
                        }}"
                    ).asJsonObject

                if (item.has("id") && (item.getAsJsonPrimitive("id").asString.startsWith("MPRE") || item.getAsJsonPrimitive(
                        "id"
                    ).asString.contains("release_detail"))
                ) {
                    parsed.getAsJsonArray("artists").add(item)
                } else {
                    parsed.getAsJsonArray("artists").add(item)
                }
            } else {
                // note: YT uses non-breaking space \xa0 to separate number and magnitude
                if ("^\\d([^ ])* [^ ]*\$".toRegex().matches(text) && i > 0) {
                    parsed.addProperty("views", text.split(' ')[0])
                } else if ("^(\\d+:)*\\d+:\\d+$".toRegex().matches(text)) {
                    parsed.addProperty("duration", text)
                    parsed.addProperty("duration_seconds", parseDuration(text))
                } else if ("^\\d{4}$".toRegex().matches(text)) {
                    parsed.addProperty("year", text.toInt())
                } else {
                    // artist without id
                    parsed.getAsJsonArray("artists")
                        .add(JsonObject().also { json ->
                            json.addProperty("name", text)
                            json.add("id", null)
                        })
                }
            }
        }

        return parsed
    }

    fun parseSongAlbum(data: JsonObject, index: Int): JsonObject? {
        val flexItem = getFlexColumnItem(data, index) ?: return null

        return JsonObject().also { json ->
            json.addProperty("name", getItemText(data, index))
            json.add("id", getBrowseId(flexItem, 0))
        }
    }

    fun parseSongMenuTokens(item: JsonObject): JsonObject {
        val toggleMenu = item.getAsJsonObject(TOGGLE_MENU)
        val serviceType = toggleMenu.getAsJsonObject("defaultIcon").getAsJsonPrimitive("iconType").asString
        var libraryAddToken = nav(toggleMenu, listOf("defaultServiceEndpoint") + FEEDBACK_TOKEN, true)
        var libraryRemoveToken = nav(toggleMenu, listOf("toggledServiceEndpoint") + FEEDBACK_TOKEN, true)

        if (serviceType == "LIBRARY_REMOVE") {
            // swap if already in library
            val temp = libraryAddToken
            libraryAddToken = libraryRemoveToken
            libraryRemoveToken = temp
        }

        return JsonObject().also { json -> json.add("add", libraryAddToken); json.add("remove", libraryRemoveToken) }
    }

    fun parseLikeStatus(service: JsonObject): JsonElement? {
        val status = JsonArray().also { jsonArray -> jsonArray.add("LIKE"); jsonArray.add("INDIFFERENT") }
        return status[status.asList().indexOf(service.getAsJsonObject("likeEndpoint").get("status")) - 1]
    }
}