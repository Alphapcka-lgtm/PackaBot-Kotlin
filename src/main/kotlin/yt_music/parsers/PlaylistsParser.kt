package yt_music.parsers

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import yt_music.utils.getFixedColumnItem
import yt_music.utils.getItemText
import yt_music.utils.parseDuration
import yt_music.Navigation

class PlaylistsParser : Navigation() {

    private val songsParser = SongsParser()

    fun parsePlaylistItems(results: JsonArray, menuEntries: List<List<String>>? = null): JsonArray {
        val songs = JsonArray()
        var count = 1
        for (res in results) {
            val result = res.asJsonObject
            count++
            if (!result.has(MRLIR)) {
                continue
            }

            val data = result.getAsJsonObject(MRLIR)

            var videoId: String? = null
            var setVideoId: String? = null
            var like: JsonElement? = null
            var feedbackTokens: JsonObject? = null

            // if the item has a menu, find its setVideoId
            if (data.has("menu")) {
                for (item in nav(data, MENU_ITEMS)!!.asJsonArray) {
                    if (item.asJsonObject.has("menuServiceItemRenderer")) {
                        val menuService = nav(item, MENU_SERVICE)!!.asJsonObject
                        if (menuService.has("playlistEditEndpoint")) {
                            setVideoId = menuService.getAsJsonObject("playlistEditEndpoint")
                                .getAsJsonArray("actions")[0].asJsonObject.getAsJsonPrimitive("setVideoId").asString
                            videoId = menuService.getAsJsonObject("playlistEditEndpoint")
                                .getAsJsonArray("actions")[0].asJsonObject.getAsJsonPrimitive("removedVideoId").asString
                        }
                    }

                    if (item.asJsonObject.has(TOGGLE_MENU)) {
                        feedbackTokens = songsParser.parseSongMenuTokens(item.asJsonObject)
                    }
                }
            }

            // if item is not playable, the videoId was retrieved above
            if (nav(data, PLAY_BUTTON, true) != null) {
                if (nav(data, PLAY_BUTTON)!!.asJsonObject.has("playNavigationEndpoint")) {
                    videoId = nav(data, PLAY_BUTTON)!!.asJsonObject.getAsJsonObject("playNavigationEndpoint")
                        .getAsJsonObject("watchEndpoint").getAsJsonPrimitive("videoId").asString

                    if (data.has("menu")) {
                        like = nav(data, MENU_LIKE_STATUS, true)
                    }
                }
            }

            val title = getItemText(data, 0)
            if (title == "Song deleted") {
                continue
            }

            val artists = songsParser.parseSongArtists(data, 1)
            val album = songsParser.parseSongAlbum(data, 2)

            var duration: String? = null
            if (data.has("fixedColumns")) {
                duration = if (getFixedColumnItem(data, 0)!!.getAsJsonObject("text").has("simpleText")) {
                    getFixedColumnItem(data, 0)!!.getAsJsonObject("text").getAsJsonPrimitive("simpleText").asString
                } else {
                    getFixedColumnItem(data, 0)!!.getAsJsonObject("text")
                        .getAsJsonArray("runs")[0].asJsonObject.getAsJsonPrimitive("text").asString
                }
            }

            var thumbnails: JsonElement? = null
            if (data.has("thumbnail")) {
                thumbnails = nav(data, THUMBNAILS)!!
            }

            var isAvailable = true
            if (data.has("musicItemRendererDisplayPolicy)")) {
                isAvailable =
                    data.getAsJsonPrimitive("musicItemRendererDisplayPolicy").asString != "MUSIC_ITEM_RENDERER_DISPLAY_POLICY_GREY_OUT"
            }

            val isExplicit = nav(data, BADGE_LABEL, true) != null

            val videoType = nav(
                data,
                MENU_ITEMS + listOf("0", "menuNavigationItemRenderer", "navigationEndpoint") + NAVIGATION_VIDEO_TYPE,
                true
            )

            val song = JsonObject().also { json ->
                json.addProperty("videoId", videoId)
                json.addProperty("title", title)
                json.add("artists", artists)
                json.add("album", album)
                json.add("likeStatus", like)
                json.add("thumbnails", thumbnails)
                json.addProperty("isAvailable", isAvailable)
                json.addProperty("isExplicit", isExplicit)
                json.add("videoType", videoType)
            }

            if (duration != null) {
                song.addProperty("duration", duration)
                song.addProperty("duration_seconds", parseDuration(duration))
            }
            if (setVideoId != null) {
                song.addProperty("setVideoId", setVideoId)
            }
            if (feedbackTokens != null) {
                song.add("feedbackTokens", feedbackTokens)
            }

            if (!menuEntries.isNullOrEmpty()) {
                for (menuEntry in menuEntries) {
                    song.add(menuEntry.last(), nav(data, MENU_ITEMS + menuEntry))
                }
            }

            songs.add(song)

        }

        return songs
    }

    fun validatePlaylistId(playlistId: String): String {
        return if (!playlistId.startsWith("VL")) {
            playlistId
        } else {
            playlistId.removeRange(0, 2)
        }
    }
}