package yt_music.parsers

import com.google.gson.JsonObject
import yt_music.utils.getDotSeparatorIndex
import yt_music.utils.getFlexColumnItem
import yt_music.utils.update
import yt_music.Navigation

class ExploreParser : Navigation() {

    private val browsingParser = BrowsingParser()
    private val songsParser = SongsParser()

    private val TRENDS = mapOf("ARROW_DROP_UP" to "up", "ARROW_DROP_DOWN" to "down", "ARROW_CHART_NEUTRAL" to "neutral")

    fun parseChartSong(data: JsonObject): JsonObject {
        val parsed = browsingParser.parseSongFlat(data)
        parsed.update(parseRanking(data))
        return parsed
    }

    fun parseChartArtist(data: JsonObject): JsonObject {
        val subscribers = getFlexColumnItem(data, 1)
        val subscribersStr = if (subscribers != null) {
            nav(subscribers, TEXT_RUN_TEXT)!!.asString.split(' ')[0]
        } else null

        val parsed = JsonObject().also { json ->
            json.add("title", nav(getFlexColumnItem(data, 0)!!, TEXT_RUN_TEXT))
            json.add("browseId", nav(data, NAVIGATION_BROWSE_ID))
            json.addProperty("subscribers", subscribersStr)
            json.add("thumbnails", nav(data, THUMBNAILS))
        }
        parsed.update(parseRanking(data))
        return parsed
    }

    fun parseChartTrending(data: JsonObject): JsonObject {
        val flex0 = getFlexColumnItem(data, 0)!!
        val artists = songsParser.parseSongArtists(data, 1)!!
        val index = getDotSeparatorIndex(artists)
        // last item is views for some reason
        val views =
            if (index == artists.size()) {
                null
            } else {
                artists.remove(artists.size() - 1).asJsonObject.getAsJsonPrimitive("name").asString.split(
                    ' '
                )[0]
            }

        return JsonObject().also { json ->
            json.add("title", nav(flex0, TEXT_RUN_TEXT))
            json.add("videoId", nav(flex0, TEXT_RUN + NAVIGATION_VIDEO_ID, true))
            json.add("playlistId", nav(flex0, TEXT_RUN + NAVIGATION_PLAYLIST_ID, true))
            json.add("artists", artists)
            json.add("thumbnails", nav(data, THUMBNAILS))
            json.addProperty("views", views)
        }
    }

    fun parseRanking(data: JsonObject): JsonObject {
        return JsonObject().also { json ->
            json.add("rank", nav(data, listOf("customIndexColumn", "musicCustomIndexColumnRenderer") + TEXT_RUN_TEXT))
            json.addProperty(
                "trend", TRENDS[nav(
                    data, listOf(
                        "customIndexColumn", "musicCustomIndexColumnRenderer", "icon",
                        "iconType"
                    )
                )!!.asString]
            )
        }
    }
}