package yt_music.parsers

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import yt_music.Navigation
import yt_music.utils.collectionToJsonArray
import yt_music.utils.getDotSeparatorIndex
import yt_music.utils.getFlexColumnItem
import yt_music.utils.update
import kotlin.reflect.KFunction1

class BrowsingParser : Navigation() {

    private val songsParser = SongsParser()

    fun parseMixedContent(rows: JsonArray): JsonArray {
        val items = JsonArray()
        var contents: JsonElement = JsonArray()
        var title: JsonElement? = null
        for (row in rows) {
            if (row.asJsonObject.has(DESCRIPTION_SHELF[0])) {
                val results = nav(row, DESCRIPTION_SHELF)!!
                title = nav(results, listOf("header") + RUN_TEXT)!!
                contents = nav(results, DESCRIPTION)!!
            } else {
                val results = row.asJsonObject.asMap().values.iterator().next()
                if (!results.asJsonObject.has("contents")) {
                    continue
                }

                val title = nav(results, CAROUSEL_TITLE + listOf("text"))!!
                contents = JsonArray()
                for (result in results.asJsonObject.getAsJsonArray("contents")) {
                    var data = nav(result, listOf(MTRIR), true)
                    var content: JsonObject? = null
                    if (data != null) {
                        val pageType = nav(data, TITLE + NAVIGATION_BROWSE + PAGE_TYPE, true)?.asString
                        if (pageType == null) {
                            // song or watch_playlist
                            if (nav(data, NAVIGATION_WATCH_PLAYLIST_ID, true) != null) {
                                content = parseWatchPlaylist(data)
                            } else {
                                content = parseSong(result)
                            }
                        } else if (pageType == "MUSIC_PAGE_TYPE_ALBUM") {
                            content = parseAlbum(data)
                        } else if (pageType == "MUSIC_PAGE_TYPE_ARTIST") {
                            content = parseRelatedArtist(data.asJsonObject)
                        } else if (pageType == "MUSIC_PAGE_TYPE_PLAYLIST") {
                            content = parsePlaylist(data.asJsonObject)
                        }
                    } else {
                        data = nav(result, listOf(MRLIR))!!
                        content = parseSongFlat(data.asJsonObject)
                    }

                    contents.add(content)
                }
            }

            items.add(JsonObject().also { json -> json.add("title", title); json.add("contents", contents) })
        }

        return items
    }

    fun parseContentList(
        results: JsonArray,
        parseFunc: KFunction1<JsonObject, JsonObject>,
        key: String = MTRIR,
    ): JsonArray {
        val contents = JsonArray()
        for (result in results) {
            parseFunc(result.asJsonObject.getAsJsonObject(key))
        }
        return contents
    }

    fun parseAlbum(result: JsonElement): JsonObject {
        return JsonObject().also { json ->
            json.add("title", nav(result, TITLE_TEXT)!!)
            json.add("year", nav(result, SUBTITLE2, true))
            json.add("browseId", nav(result, TITLE + NAVIGATION_BROWSE_ID)!!)
            json.add("thumbnails", nav(result, THUMBNAIL_RENDERER)!!)
            val isExplicit = nav(result, SUBTITLE_BADGE_LABEL, true)
            if (isExplicit != null) {
                json.add("isExplicit", isExplicit)
            }
        }
    }

    fun parseSingle(result: JsonElement): JsonObject {
        return JsonObject().also { json ->
            json.add("title", nav(result, TITLE_TEXT)!!)
            json.add("year", nav(result, SUBTITLE, true))
            json.add("browseId", nav(result, TITLE + NAVIGATION_BROWSE_ID)!!)
            json.add("thumbnails", nav(result, THUMBNAIL_RENDERER)!!)
        }
    }

    fun parseSong(result: JsonElement): JsonObject {
        val song = JsonObject().also { json ->
            json.add("title", nav(result, TITLE_TEXT)!!)
            json.add("videoId", nav(result, NAVIGATION_VIDEO_ID)!!)
            json.add("playlistId", nav(result, NAVIGATION_PLAYLIST_ID, true))
            json.add("thumbnails", nav(result, THUMBNAIL_RENDERER)!!)
        }

        song.update(songsParser.parseSongRuns(nav(result, SUBTITLE_RUNS)!!.asJsonArray))
        return song
    }

    fun parseSongFlat(data: JsonObject): JsonObject {
        val columns = JsonArray()
        for (i in 0 until data.getAsJsonArray("flexColumns").size()) {
            columns.add(getFlexColumnItem(data, i))
        }

        val song = JsonObject().also { json ->
            json.add("title", nav(columns[0], TEXT_RUN_TEXT)!!)
            json.add("videoId", nav(columns[0], TEXT_RUN + NAVIGATION_VIDEO_ID, true))
            json.add("artistId", songsParser.parseSongArtists(data, 1))
            json.add("thumbnails", nav(data, THUMBNAILS)!!)
            val isExplicit = nav(data, THUMBNAILS, true)
            json.add("isExplicit", isExplicit)
        }
        if ((columns.size() > 2) && (columns[2] != null) && nav(
                columns[2],
                TEXT_RUN
            )!!.asJsonObject.has("navigationEndpoint")
        ) {
            val album = JsonObject().also { json ->
                json.add("name", nav(columns[2], TEXT_RUN_TEXT)!!)
                json.add("id", nav(columns[2], TEXT_RUN + NAVIGATION_BROWSE_ID))
            }
            song.add("album", album)
        } else {
            song.addProperty("views", nav(columns[1], listOf("text", "runs", "-1", "text"))!!.asString.split(' ')[0])
        }

        return song
    }

    fun parseVideo(result: JsonElement): JsonObject {
        val runs = nav(result, SUBTITLE_RUNS)!!.asJsonArray
        val artistLen = getDotSeparatorIndex(runs)
        return JsonObject().also { json ->
            json.add("title", nav(result, TITLE_TEXT)!!)
            json.add("videoId", nav(result, NAVIGATION_VIDEO_ID)!!)
            val artistsRuns = JsonParser.parseString(
                runs.asList().subList(artistLen, runs.size()).joinToString(prefix = "[", postfix = "]")
            ).asJsonArray
            json.add("artists", songsParser.parseSongArtistsRuns(artistsRuns))
            json.add("playlistId", nav(result, NAVIGATION_PLAYLIST_ID, true))
            json.add("thumbnails", nav(result, THUMBNAIL_RENDERER, true))
            json.addProperty("views", runs.last().asJsonObject.getAsJsonPrimitive("text").asString.split(' ')[0])
        }
    }

    fun parsePlaylist(data: JsonObject): JsonObject {
        val playlist = JsonObject().also { json ->
            json.add("title", nav(data, TITLE_TEXT)!!)
            json.add("playlistId", nav(data, TITLE + NAVIGATION_BROWSE_ID)!!)
            json.add("thumbnails", nav(data, THUMBNAIL_RENDERER)!!)
        }
        val subtitle = data.getAsJsonObject("subtitle")
        if (subtitle.has("runs")) {
            val descriptionBuilder = StringBuilder()
            for (run in subtitle.getAsJsonArray("runs")) {
                descriptionBuilder.append(run.asJsonObject.getAsJsonPrimitive("text").asString)
                if (subtitle.getAsJsonArray("runs").size() == 3 && "\\d+".toRegex()
                        .containsMatchIn(nav(data, SUBTITLE2)!!.asString)
                ) {
                    playlist.addProperty("count", nav(data, SUBTITLE2)!!.asString.split(' ')[0])
                    val songArtistsRuns = collectionToJsonArray(
                        subtitle.getAsJsonArray("runs").asList().subList(1, subtitle.getAsJsonArray("runs").size())
                    )
                    playlist.add("author", songsParser.parseSongArtistsRuns(songArtistsRuns))
                }
            }
        }

        return playlist
    }

    fun parseRelatedArtist(data: JsonObject): JsonObject {
        var subscribers = nav(data, SUBTITLE, true)?.asString
        if (subscribers != null) {
            subscribers = subscribers.toString().split(' ')[0]
        }

        return JsonObject().also { json ->
            json.add("title", nav(data, TITLE_TEXT)!!)
            json.add("browseId", nav(data, TITLE + NAVIGATION_BROWSE_ID)!!)
            json.addProperty("subscribers", subscribers)
            json.add("thumbnails", nav(data, THUMBNAIL_RENDERER)!!)
        }
    }

    fun parseWatchPlaylist(data: JsonElement): JsonObject {
        return JsonObject().also { json ->
            json.add("title", nav(data, TITLE_TEXT))
            json.add("playlist", nav(data, NAVIGATION_WATCH_PLAYLIST_ID))
            json.add("thumbnails", nav(data, THUMBNAIL_RENDERER))
        }
    }
}