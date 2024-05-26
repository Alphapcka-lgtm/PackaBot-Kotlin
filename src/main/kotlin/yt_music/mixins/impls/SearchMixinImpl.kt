package yt_music.mixins.impls

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kong.unirest.HttpResponse
import yt_music.Continuations
import yt_music.enums.SearchFilters
import yt_music.enums.SearchScopes
import yt_music.mixins.SearchMixin
import yt_music.parsers.SearchParser
import yt_music.pojos.search.SearchResult
import yt_music.Navigation
import yt_music.Requester

open class SearchMixinImpl(private val requester: Requester) : SearchMixin, Navigation() {

    private val searchParser = SearchParser()
    private val continuations = Continuations()

    private val searchResultsTypes = listOf("artist", "playlist", "song", "video", "station")

    override fun search(
        query: String,
        filter: SearchFilters?,
        scope: SearchScopes?,
        limit: Int,
        ignoreSpelling: Boolean,
    ): Collection<SearchResult> {
        val body = JsonObject().also { json -> json.addProperty("query", query) }
        val endpoint = "search"
        val searchResults = ArrayList<JsonObject>()

        if ((scope != null) && (scope == SearchScopes.UPLOADS) && (filter != null)) {
            throw Exception("No filter can be set when search uploads. Please unset the filter parameter when scope is set to \"${SearchScopes.UPLOADS}\"")
        }

        val params = searchParser.getSearchParams(filter, scope, ignoreSpelling)
        if (params != null) {
            body.addProperty("params", params)
        }

        val response = requester.sendRequest(endpoint, body)
        val json = JsonParser.parseString(response.body.toString()).asJsonObject

        if (!json.has("contents")) {
            return searchResults.stream().map { SearchResult.fromJson(it) }.toList()
        }

        var results: JsonElement = if (json.getAsJsonObject("contents").has("tabbedSearchResultsRenderer")) {
            val tabIndex = if (scope == null || filter != null) 0 else SearchScopes.values().asList().indexOf(scope) + 1
            json.getAsJsonObject("contents").getAsJsonObject("tabbedSearchResultsRenderer").getAsJsonArray("tabs")
                .get(tabIndex).asJsonObject.getAsJsonObject("tabRenderer").getAsJsonObject("content")
        } else {
            json.getAsJsonObject("contents")
        }

        results = nav(results, SECTION_LIST)!!

        if (results.asJsonArray.size() == 1 && results.toString().contains("itemSectionRenderer", true)) {
            return searchResults.stream().map { SearchResult.fromJson(it) }.toList()
        }

        val filterStr =
            if (filter != null && (filter == SearchFilters.PLAYLISTS || filter == SearchFilters.COMMUNITY_PLAYLISTS || filter == SearchFilters.FEATURED_PLAYLISTS)) {
                SearchFilters.PLAYLISTS.name
            } else if (scope == SearchScopes.UPLOADS) {
                SearchScopes.UPLOADS.name
            } else {
                filter?.name
            }

        var newResults: JsonArray?
        var type: String? = null
        for (_res in results.asJsonArray) {
            val res = _res.asJsonObject

            var category: JsonElement? = null

            if (res.has("musicCardShelfRenderer")) {
                val topResult = searchParser.parseTopResult(
                    res.get("musicCardShelfRenderer"),
                    searchResultsTypes
                )
                searchResults.add(topResult)

                newResults = nav(res, listOf("musicCardShelfRenderer", "contents"), true)?.asJsonArray
                if (newResults != null) {
                    if (results.asJsonArray.get(0).asJsonObject.has("messageRenderer")) {
                        category = nav(newResults.asJsonArray.get(0), listOf("messageRenderer") + TEXT_RUN_TEXT)
                    }
                    type = null
                } else {
                    newResults = results.asJsonArray
                    continue
                }

            } else if (res.has("musicShelfRenderer")) {
                newResults = res.getAsJsonObject("musicShelfRenderer").getAsJsonArray("contents")
                var typeFilter = filterStr
                category = nav(res, MUSIC_SHELF + TITLE_TEXT, true)
                if (typeFilter == null && scope == SearchScopes.LIBRARY) {
                    typeFilter = category?.asString
                }

                type = if (typeFilter == null) {
                    null
                } else {
                    if (category != null && category.isJsonArray) {
                        category.asJsonArray.remove(category.asJsonArray.size() - 1).asString
                    } else {
                        typeFilter.dropLast(1)
                    }
                }
            } else {
                newResults = results.asJsonArray
                continue
            }

            searchResults.addAll(
                searchParser.parseSearchResults(
                    newResults!!,
                    searchResultsTypes,
                    type,
                    category?.asString
                )
            )

            if (filter != null) {
                // if filter is set, there are continuations
                fun requestFunc(additionalParams: String): HttpResponse<JsonElement> {
                    return requester.sendRequest(endpoint, body, additionalParams)
                }

                fun parseFunc(contents: JsonElement): JsonObject {
                    return searchParser.parseSearchResult(
                        contents.asJsonObject,
                        searchResultsTypes,
                        type,
                        category?.asString
                    )
                }

                searchResults.addAll(
                    continuations.getContinuations(
                        res.getAsJsonObject("musicShelfRenderer"),
                        "musicShelfContinuation", limit - searchResults.size, ::requestFunc, ::parseFunc
                    ).asList().stream().map { it.asJsonObject }.toList()
                )
            }

        }

        return searchResults.stream().map { SearchResult.fromJson(it) }.toList()
    }

    override fun getSearchSuggestion(query: String, detailedRuns: Boolean): JsonArray {
        val body = JsonObject().also { jsonObject -> jsonObject.addProperty("input", query) }
        val endpoint = "music/get_search_suggestions"

        val response = requester.sendRequest(endpoint, body)
        val json = JsonParser.parseString(response.body.toString()).asJsonObject
        val searchResult = searchParser.parseSearchSuggestions(json, detailedRuns)
        return searchResult
    }

}
