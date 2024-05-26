package yt_music.i18n

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import yt_music.parsers.BrowsingParser
import yt_music.Navigation

// TODO: find out if this is necessary
class I18nParser : Navigation() {

    private val browsingParser = BrowsingParser()

    fun parseArtistContents(results: JsonArray): JsonObject {
        val categories = arrayOf("albums", "singles", "videos", "playlists", "related")
        val categoriesLocal = arrayOf("albums", "singles", "videos", "playlists", "related")
        val categoriesParser = arrayOf(
            browsingParser::parseAlbum,
            browsingParser::parseSingle,
            browsingParser::parseVideo,
            browsingParser::parsePlaylist,
            browsingParser::parseRelatedArtist
        )
        val artist = JsonObject()
        categories.asList().forEachIndexed { i, category ->
            val data = ArrayList<JsonElement>()
            for (result in results) {
                if (result.asJsonObject.has("musicCarouselShelfRenderer") && nav(
                        result,
                        CAROUSEL + CAROUSEL_TITLE
                    )!!.asJsonObject.getAsJsonPrimitive("text").asString.lowercase() == categoriesLocal[i]
                ) {
                    data.add(result.asJsonObject.get("musicCarouselShelfRenderer"))
                }
            }
            if (data.size > 0) {
                artist.add(category, JsonObject().also { json ->
                    json.add("browseId", null)
                    json.add("results", JsonArray())
                })
                if (nav(data[0], CAROUSEL_TITLE)!!.asJsonObject.has("navigationEndpoint")) {
                    artist.add(category, nav(data[0], CAROUSEL_TITLE + NAVIGATION_BROWSE_ID)!!)
                    if (arrayOf("albums", "singles", "playlists").contains(category)) {
                        artist.getAsJsonObject(category)
                            .add(
                                "params",
                                nav(data[0], CAROUSEL_TITLE)!!.asJsonObject.getAsJsonObject("navigationEndpoint")
                                    .getAsJsonObject("browseEndpoint").get("params")
                            )
                    }
                }
                artist.getAsJsonObject(category).add(
                    "results",
                    browsingParser.parseContentList(
                        data[0].asJsonObject.getAsJsonArray("contents"),
                        categoriesParser[i]
                    )
                )
            }
        }

        return artist
    }
}