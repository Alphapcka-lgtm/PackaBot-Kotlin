package yt_music.parsers

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import yt_music.enums.SearchFilters
import yt_music.enums.SearchScopes
import yt_music.utils.getFlexColumnItem
import yt_music.utils.getItemText
import yt_music.utils.parseMenuPlaylists
import yt_music.utils.update
import yt_music.Navigation

class SearchParser : Navigation() {

    private val songsParser = SongsParser()

    fun getSearchParams(
        filter: SearchFilters?,
        scope: SearchScopes?,
        ignoreSpelling: Boolean,
    ): String? {
        val filteredParam1 = "EgWKAQI"

        var params: String? = null
        var param1: String = ""
        var param2: String? = ""
        var param3: String = ""

        if (filter == null && scope == null && !ignoreSpelling) {
            return params
        }

        if (scope == SearchScopes.UPLOADS) {
            params = "agIYAw%3D%3D"
        }

        if (scope == SearchScopes.LIBRARY) {
            if (filter != null) {
                param1 = filteredParam1
                param2 = getParam2(filter)
                param3 = "AWoKEAUQCRADEAoYBA%3D%3D'"
            } else {
                params = "agIYBA%3D%3D"
            }
        }

        if (scope == null && filter != null) {
            if (filter == SearchFilters.PLAYLISTS) {
                params = "Eg-KAQwIABAAGAAgACgB"
                params += if (!ignoreSpelling) {
                    "MABqChAEEAMQCRAFEAo%3D"
                } else {
                    "MABCAggBagoQBBADEAkQBRAK"
                }
            } else if (filter == SearchFilters.FEATURED_PLAYLISTS || filter == SearchFilters.COMMUNITY_PLAYLISTS) {
                param1 = "EgeKAQQoA"
                param2 = if (filter == SearchFilters.FEATURED_PLAYLISTS) "Dg" else "EA"
                param3 = if (!ignoreSpelling) "BagwQDhAKEAMQBBAJEAU%3D" else "BQgIIAWoMEA4QChADEAQQCRAF"
            } else {
                param1 = filteredParam1
                param2 = getParam2(filter)
                param3 = if (!ignoreSpelling) "AWoMEA4QChADEAQQCRAF" else "AUICCAFqDBAOEAoQAxAEEAkQBQ%3D%3D"
            }
        }

        if (scope == null && filter == null && ignoreSpelling) {
            params = "EhGKAQ4IARABGAEgASgAOAFAAUICCAE%3D"
        }

        return params ?: (param1 + param2 + param3)
    }

    private fun getParam2(filter: SearchFilters): String? {
        return when (filter) {
            SearchFilters.SONGS -> "I"
            SearchFilters.VIDEOS -> "Q"
            SearchFilters.ALBUMS -> "Y"
            SearchFilters.ARTISTS -> "g"
            SearchFilters.PLAYLISTS -> "o"
            else -> null
        }
    }

    fun parseTopResult(data: JsonElement, searchResultTypes: List<String>): JsonObject {
        val resultType = getSearchResultType(nav(data, SUBTITLE)!!.asString, searchResultTypes)
        val searchResult =
            JsonParser.parseString("{category:${nav(data, CARD_SHELF_TITLE)}, resultType:$resultType}").asJsonObject

        if (resultType == "artist") {
            val subscribers = nav(data, SUBTITLE2, true)
            if (subscribers != null) {
                searchResult.add("subscribers", subscribers.asJsonArray[0])
            }

            val artistInfo = songsParser.parseSongRuns(nav(data, listOf("title", "runs"))!!.asJsonArray)
            searchResult.update(artistInfo)
        }

        if (arrayOf("song", "video").contains(resultType)) {
            val onTab = data.asJsonObject.getAsJsonObject("onTap")
            if (onTab != null) {
                searchResult.add("videoId", nav(onTab, WATCH_VIDEO_ID))
                searchResult.add("videoType", nav(onTab, NAVIGATION_VIDEO_TYPE))
            }
        }

        if (arrayOf("song", "type", "album").contains(resultType)) {
            searchResult.add("title", nav(data, TITLE_TEXT))
            val runs = nav(data, listOf("subtitle", "runs"))!!.asJsonArray
            // remove the first two elements, these are not needed here
            runs.remove(0)
            runs.remove(0)
            val songInfo = songsParser.parseSongRuns(runs)
            searchResult.update(songInfo)
        }

        searchResult.add("thumbnails", nav(data, THUMBNAILS, true))
        return searchResult
    }

    private fun getSearchResultType(resultTypeLocal: String?, resultTypesLocal: List<String>): String? {
        if (resultTypeLocal == null) {
            return null
        }
        val resultTypes = listOf("artist", "playlist", "song", "video", "station")
        //default to album since it's labeled with multiple values (Single, EP, etc)
        val resultType = if (!resultTypesLocal.contains(resultTypeLocal.lowercase())) {
            "album"
        } else {
            resultTypes.get(resultTypesLocal.indexOf(resultTypeLocal.lowercase()))
        }

        return resultType
    }

    fun parseSearchResult(
        data: JsonObject,
        searchResultTypes: List<String>,
        _resultType: String?,
        category: String?,
    ): JsonObject {
        var resultType = _resultType?.lowercase()
        val defaultOffset = if (resultType != null) 0 else 2
        val searchResult = JsonObject().also { json -> json.addProperty("category", category) }
        val videoType = nav(data, PLAY_BUTTON + listOf("playNavigationEndpoint") + NAVIGATION_VIDEO_TYPE, true)
        if (resultType == null && videoType != null) {
            resultType = if (videoType.asString == "MUSIC_VIDEO_TYPE_ATV") "song" else "video"
        }

        if (resultType == null) {
            resultType = getSearchResultType(getItemText(data, 1), searchResultTypes)
        }
        searchResult.addProperty("resultType", resultType)

        if (resultType != "artist") {
            searchResult.addProperty("title", getItemText(data, 0))
        }
        if (resultType == "artist") {
            searchResult.addProperty("artist", getItemText(data, 0))
            parseMenuPlaylists(data, searchResult)
        } else if (resultType == "album") {
            searchResult.addProperty("type", getItemText(data, 1))
        } else if (resultType == "playlist") {
            val flexItem = getFlexColumnItem(data, 1)!!.asJsonObject.getAsJsonObject("text").getAsJsonArray("runs")
            val hasAuthor = flexItem.size() == defaultOffset + 3
            val index = defaultOffset + (if (hasAuthor) 1 else 0) * 2
            searchResult.addProperty(
                "itemCount",
                nav(flexItem, listOf(index.toString(), "text"))!!.asString.split(' ').get(0)
            )
            val author = if (!hasAuthor) null else nav(flexItem, listOf(defaultOffset.toString(), "text"))
            searchResult.add("author", author)
        } else if (resultType == "station") {
            searchResult.add("videoId", nav(data, NAVIGATION_VIDEO_ID))
            searchResult.add("playlistId", nav(data, NAVIGATION_PLAYLIST_ID))
        } else if (resultType == "song") {
            searchResult.add("album", null)
            if (data.has("menu")) {
                val toggleMenu = findObjectByKey(nav(data, MENU_ITEMS)!!.asJsonArray, TOGGLE_MENU)
                if (toggleMenu != null) {
                    searchResult.add("feedbackTokens", parseSongMenuTokens(toggleMenu.asJsonObject))
                }
            }
        } else if (resultType == "upload") {
            val browseId = nav(data, NAVIGATION_BROWSE_ID, true)
            if (browseId == null) {
                // song result
                val flexItems = ArrayList<JsonElement?>(2)
                for (i in 0 until 2) {
                    flexItems.add(nav(getFlexColumnItem(data, i)!!, listOf("test", "runs"), true))
                }
                if (flexItems[0] != null) {
                    searchResult.add("videoId", nav(flexItems[0]!!.asJsonArray.get(0), NAVIGATION_VIDEO_ID, true))
                    searchResult.add("playlistId", nav(flexItems[0]!!.asJsonArray.get(0), NAVIGATION_PLAYLIST_ID, true))
                }
                if (flexItems[1] != null) {
                    searchResult.update(songsParser.parseSongRuns(flexItems[1]!!.asJsonArray))
                }
                searchResult.addProperty("resultType", "song")
            } else {
                // artist or album result
                searchResult.add("browseId", browseId)
                if (searchResult.getAsJsonObject("browseId").has("artist")) {
                    searchResult.addProperty("resultType", "artist")
                } else {
                    val flexItem = getFlexColumnItem(data, 1)!!
                    val runs = ArrayList<JsonElement>()
                    for (i in 0 until flexItem.getAsJsonObject("text").getAsJsonObject("runs").size()) {
                        if (i % 2 == 0) {
                            runs.add(flexItem.getAsJsonObject("text").getAsJsonObject("runs").get("text"))
                        }
                    }
                    if (runs.size > 1) {
                        searchResult.add("artist", runs[1])
                    }
                    if (runs.size > 2) {
                        // date may be missing
                        searchResult.add("releaseDate", runs[2])
                    }
                    searchResult.addProperty("resultType", "album")
                }
            }
        }

        if (arrayOf("song", "video").contains(resultType)) {
            searchResult.add(
                "video", nav(
                    data, PLAY_BUTTON + listOf(
                        "playNavigationEndpoint", "watchEndpoint",
                        "videoId"
                    ), true
                )
            )
            searchResult.add("videoType", videoType)
        }

        if (arrayOf("song", "video", "album").contains(resultType)) {
            searchResult.add("duration", null)
            searchResult.add("year", null)
            val flexItem = getFlexColumnItem(data, 1)!!
            val jsonArray = flexItem.getAsJsonObject("text").getAsJsonArray("runs")
            val runs = JsonParser.parseString(
                // get the sublist and convert it back to a json array
                jsonArray.asList().subList(defaultOffset, jsonArray.size()).toString()
            ).asJsonArray
            val songInfo = songsParser.parseSongRuns(runs)
            searchResult.update(songInfo)
        }

        if (arrayOf("artist", "album", "playlist").contains(resultType)) {
            searchResult.add("browseId", nav(data, NAVIGATION_BROWSE_ID, true))
            if (searchResult.getAsJsonPrimitive("browseId") == null) {
                return JsonObject()
            }
        }

        if (arrayOf("song", "album").contains(resultType)) {
            searchResult.addProperty("isExplicit", nav(data, BADGE_LABEL, true) != null)
        }

        searchResult.add("thumbnails", nav(data, THUMBNAILS, true))
        return searchResult
    }

    fun parseSearchResults(
        results: JsonArray,
        searchResultTypes: List<String>,
        resultType: String? = null,
        category: String? = null,
    ): ArrayList<JsonObject> {
        val ret = ArrayList<JsonObject>(results.size())
        results.asList().stream().forEach { result ->
            ret.add(
                parseSearchResult(
                    result.asJsonObject.getAsJsonObject(MRLIR),
                    searchResultTypes,
                    resultType,
                    category
                )
            )
        }

        return ret
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

        return JsonParser.parseString("{add:$libraryAddToken, remove:$libraryRemoveToken}").asJsonObject
    }

    fun parseSearchSuggestions(results: JsonObject, detailedRuns: Boolean): JsonArray {
        if (!results.has("contents") || !results.getAsJsonArray("contents")
                .get(0).asJsonObject.has("searchSuggestionsSectionRenderer") || !results.getAsJsonArray("contents")
                .get(0).asJsonObject.getAsJsonObject("searchSuggestionsSectionRenderer").has("contents")
        ) {
            return JsonArray()
        }

        val rawSuggestions =
            results.getAsJsonArray("contents").get(0).asJsonObject.getAsJsonObject("searchSuggestionsSectionRenderer")
                .getAsJsonArray("contents")

        val suggestions = JsonArray()

        for (rawSuggestion in rawSuggestions) {
            val suggestionContent = rawSuggestion.asJsonObject.getAsJsonObject("searchSuggestionRenderer")

            val text = suggestionContent.getAsJsonObject("navigationEndpoint").getAsJsonObject("searchEndpoint")
                .getAsJsonPrimitive("query")
            val runs = suggestionContent.getAsJsonObject("suggestion").getAsJsonPrimitive("runs")

            if (detailedRuns) {
                suggestions.add(JsonObject().also { json -> json.add("text", text); json.add("runs", runs) })
            } else {
                suggestions.add(text)
            }
        }

        return suggestions
    }
}