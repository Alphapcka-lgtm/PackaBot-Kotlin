package yt_music.mixins.impls

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import yt_music.mixins.ExploreMixin
import yt_music.parsers.BrowsingParser
import yt_music.parsers.ExploreParser
import yt_music.utils.update
import yt_music.Navigation
import yt_music.Requester
import kotlin.reflect.KFunction1

class ExploreMixinImpl(private val requester: Requester) : ExploreMixin, Navigation() {

    private val browsingParser = BrowsingParser()
    private val exploreParser = ExploreParser()

    override fun getMoodCategories(): JsonObject {
        val sections = JsonObject()
        val endpoint = "browse"
        val body = JsonObject().also { json -> json.addProperty("browseId", "FEmusic_moods_and_genres") }
        val response = requester.sendRequest(endpoint, body).body
        for (section in nav(response, SINGLE_COLUMN_TAB + SECTION_LIST)!!.asJsonArray) {
            val title = nav(section, GRID + listOf("header", "gridHeaderRenderer") + TITLE_TEXT)!!.asString
            sections.add(title, JsonArray())
            for (category in nav(section, GRID_ITEMS)!!.asJsonArray) {
                sections.getAsJsonArray(title).add(JsonObject().also { json ->
                    json.add("title", nav(category, CATEGORY_TITLE))
                    json.add("params", nav(category, CATEGORY_PARAMS))
                })
            }
        }

        return sections
    }

    override fun getMoodPlaylists(params: String): JsonArray {
        val playlsits = JsonArray()
        val endpoint = "browse"
        val body = JsonObject().also { json ->
            json.addProperty("browseId", "FEmusic_moods_and_genres_category")
            json.addProperty("params", params)
        }
        val response = requester.sendRequest(endpoint, body).body
        for (section in nav(response, SINGLE_COLUMN_TAB + SECTION_LIST)!!.asJsonArray) {
            var path: List<String> = ArrayList()
            if (section.asJsonObject.has("gridRenderer")) {
                path = GRID_ITEMS
            } else if (section.asJsonObject.has("musicCarouselShelfRenderer")) {
                path = CAROUSEL_CONTENTS
            } else if (section.asJsonObject.has("musicImmersiveCarouselShelfRenderer")) {
                path = listOf("musicImmersiveCarouselShelfRenderer", "contents")
            }
            if (path.isNotEmpty()) {
                val results = nav(section, path)!!.asJsonArray
                playlsits.addAll(browsingParser.parseContentList(results, browsingParser::parsePlaylist))
            }
        }

        return playlsits
    }

    override fun getCharts(country: String): JsonObject {
        val body = JsonObject().also { json -> json.addProperty("browseId", "FEmusic_charts") }
        val formData =
            JsonObject().also { it.add("selectedValues", JsonArray().also { jsonArray -> jsonArray.add(country) }) }
        body.add("formData", formData)
        val endpoint = "browse"
        val response = requester.sendRequest(endpoint, body).body
        val results = nav(response, SINGLE_COLUMN_TAB + SECTION_LIST)!!.asJsonArray
        val charts = JsonObject().also { it.add("countries", JsonObject()) }
        val menu = nav(
            results[0], MUSIC_SHELF + listOf(
                "subheaders", "0", "musicSideAlignedItemRenderer", "startItems", "0",
                "musicSortFilterButtonRenderer"
            )
        )!!

        charts.getAsJsonObject("countries").add("selected", nav(menu, TITLE))
        val options = JsonArray()
        val optionsLst = nav(response, FRAMEWORK_MUTATIONS)!!.asJsonArray.asList().stream()
            .map { m -> nav(m, listOf("payload", "musicFormBooleanChoice", "opaqueToken"), true) }.filter { opt ->
                if (opt != null) {
                    return@filter !(opt.isJsonArray && opt.asJsonArray.isEmpty)
                }
                return@filter false
            }.toList()
        options.asList().addAll(optionsLst)
        charts.getAsJsonObject("countries").add("options", options)
        val chartsCategories = mutableListOf("videos", "artists")

        val hasSongs = false //later add possible auth check
        val hasGenres = country == "US"
        val hasTrending = country != "ZZ"
        if (hasSongs) chartsCategories.add(0, "songs")
        if (hasGenres) chartsCategories.add("genres")
        if (hasTrending) chartsCategories.add("trending")

        fun parseChart(i: Int, parseFunc: KFunction1<JsonObject, JsonObject>, key: String): JsonArray {
            val index = if (hasSongs) 1 else 0
            val res = nav(results[i + index], CAROUSEL_CONTENTS)!!.asJsonArray
            return browsingParser.parseContentList(res, parseFunc, key)
        }

        chartsCategories.forEachIndexed { i, c ->
            charts.add(c, JsonObject().also { json ->
                json.add("playlist", nav(results[1 + i], CAROUSEL + CAROUSEL_TITLE + NAVIGATION_BROWSE_ID, true))
            })
        }

        if (hasSongs) {
            val songs = JsonObject()
            songs.add("items", parseChart(0, exploreParser::parseChartSong, MRLIR))
            charts.getAsJsonObject("songs").update(songs)
        }
        charts.getAsJsonObject("videos").add("items", parseChart(1, browsingParser::parseVideo, MTRIR))
        charts.getAsJsonObject("artists").add("items", parseChart(2, exploreParser::parseChartArtist, MRLIR))

        if (hasGenres) charts.add("genres", parseChart(3, browsingParser::parsePlaylist, MTRIR))
        if (hasTrending) {
            val i = 3 + if (hasGenres) 1 else 0
            charts.getAsJsonObject("trending").add("items", parseChart(i, exploreParser::parseChartTrending, MRLIR))
        }

        return charts
    }
}